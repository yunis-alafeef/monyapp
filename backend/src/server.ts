import "dotenv/config";
import cors from "cors";
import express, { NextFunction, Request, Response } from "express";
import helmet from "helmet";
import admin from "firebase-admin";
import { Pool } from "pg";
import { z } from "zod";

if (!admin.apps.length) {
  admin.initializeApp({
    projectId: process.env.FIREBASE_PROJECT_ID || undefined,
  });
}

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
});

const app = express();
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: "2mb" }));

interface AuthenticatedRequest extends Request {
  uid?: string;
  email?: string;
}

async function requireFirebaseAuth(
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    res.status(401).json({ error: "رمز المصادقة مفقود أو غير صالح" });
    return;
  }
  const token = authHeader.slice(7).trim();
  try {
    const decoded = await admin.auth().verifyIdToken(token);
    req.uid = decoded.uid;
    req.email = decoded.email;
    next();
  } catch {
    res.status(401).json({ error: "فشل التحقق من جلسة المستخدم" });
  }
}

app.get("/health", async (_req: Request, res: Response) => {
  try {
    await pool.query("SELECT 1");
    res.json({ ok: true, service: "hisabati-backend", database: "connected" });
  } catch {
    res.json({ ok: true, service: "hisabati-backend", database: "not_configured" });
  }
});

const SyncBatchSchema = z.object({
  idempotencyKey: z.string().min(6),
  entityType: z.enum(["CONTACT", "DEBT", "PAYMENT", "INSTALLMENT", "AUDIT_LOG"]),
  entityId: z.string().min(1),
  operation: z.enum(["UPSERT", "CREATE", "DELETE"]),
  payload: z.record(z.unknown()).optional(),
});

app.post("/api/v1/sync", requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
  const parsed = SyncBatchSchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "بيانات المزامنة غير مكتملة", issues: parsed.error.issues });
    return;
  }
  const { idempotencyKey, entityType, entityId, operation } = parsed.data;
  try {
    const client = await pool.connect();
    try {
      await client.query("BEGIN");
      const userResult = await client.query(
        `INSERT INTO users (firebase_uid, email)
         VALUES ($1, $2)
         ON CONFLICT (firebase_uid) DO UPDATE SET updated_at = NOW()
         RETURNING id`,
        [req.uid, req.email || "user@hisabati.app"]
      );
      const userId = userResult.rows[0].id;
      const wsResult = await client.query(
        `SELECT id FROM workspaces WHERE owner_user_id = $1 LIMIT 1`,
        [userId]
      );
      const workspaceId =
        wsResult.rows[0]?.id ||
        (
          await client.query(
            `INSERT INTO workspaces (owner_user_id, name, account_type)
             VALUES ($1, 'حسابي', 'PERSONAL') RETURNING id`,
            [userId]
          )
        ).rows[0].id;

      const existing = await client.query(
        `SELECT id FROM sync_records WHERE workspace_id = $1 AND idempotency_key = $2`,
        [workspaceId, idempotencyKey]
      );
      if (existing.rows.length > 0) {
        await client.query("COMMIT");
        res.json({ accepted: true, deduplicated: true, idempotencyKey });
        return;
      }

      await client.query(
        `INSERT INTO sync_records (workspace_id, idempotency_key, entity_type, entity_id, operation)
         VALUES ($1, $2, $3, $4, $5)`,
        [workspaceId, idempotencyKey, entityType, entityId, operation]
      );
      await client.query("COMMIT");
      res.status(201).json({ accepted: true, deduplicated: false, idempotencyKey });
    } catch (err) {
      await client.query("ROLLBACK");
      throw err;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error("Sync error:", err);
    res.status(500).json({ error: "خطأ في الخادم أثناء المزامنة" });
  }
});

const MessagingSchema = z.object({
  toPhone: z.string().min(6),
  messageBody: z.string().min(1).max(2000),
  templateType: z.string().optional(),
  phoneNumberId: z.string().optional(),
  senderId: z.string().optional(),
});

app.post(
  "/api/v1/messaging/whatsapp/send",
  requireFirebaseAuth,
  async (req: AuthenticatedRequest, res: Response) => {
    const parsed = MessagingSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ error: "رقم الهاتف أو نص الرسالة غير صالح" });
      return;
    }
    const token = process.env.WHATSAPP_ACCESS_TOKEN;
    const phoneNumberId = parsed.data.phoneNumberId || process.env.WHATSAPP_PHONE_NUMBER_ID;
    if (!token || !phoneNumberId) {
      res.status(503).json({
        error: "خدمة WhatsApp Cloud API غير مهيأة على الخادم (يرجى ضبط WHATSAPP_ACCESS_TOKEN)",
      });
      return;
    }

    const digits = parsed.data.toPhone.replace(/\D/g, "");
    try {
      const response = await fetch(
        `https://graph.facebook.com/v20.0/${encodeURIComponent(phoneNumberId)}/messages`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            messaging_product: "whatsapp",
            to: digits,
            type: "text",
            text: { body: parsed.data.messageBody },
          }),
        }
      );
      const data = await response.json();
      if (!response.ok) {
        res.status(response.status).json({ error: "فشل الإرسال عبر WhatsApp Cloud API", details: data });
        return;
      }
      res.json({ sent: true, provider: "whatsapp_cloud_api", response: data });
    } catch (err) {
      console.error("WhatsApp relay error:", err);
      res.status(502).json({ error: "تعذر الاتصال بخادم WhatsApp Cloud API" });
    }
  }
);

app.post(
  "/api/v1/messaging/sms/send",
  requireFirebaseAuth,
  async (req: AuthenticatedRequest, res: Response) => {
    const parsed = MessagingSchema.safeParse(req.body);
    if (!parsed.success) {
      res.status(400).json({ error: "رقم الهاتف أو نص الرسالة غير صالح" });
      return;
    }
    const endpoint = process.env.SMS_PROVIDER_ENDPOINT;
    const apiKey = process.env.SMS_API_KEY;
    const senderId = parsed.data.senderId || process.env.SMS_SENDER_ID || "HISABATI";
    if (!endpoint || !apiKey) {
      res.status(503).json({
        error: "بوابة SMS غير مهيأة على الخادم (يرجى ضبط SMS_PROVIDER_ENDPOINT و SMS_API_KEY)",
      });
      return;
    }

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          to: parsed.data.toPhone,
          sender: senderId,
          body: parsed.data.messageBody,
        }),
      });
      if (!response.ok) {
        const text = await response.text();
        res.status(response.status).json({ error: "رفض مزود SMS إرسال الرسالة", details: text });
        return;
      }
      res.json({ sent: true, provider: "sms_gateway" });
    } catch (err) {
      console.error("SMS relay error:", err);
      res.status(502).json({ error: "تعذر الاتصال بمزود خدمة SMS" });
    }
  }
);

const port = Number(process.env.PORT || 8080);
app.listen(port, () => {
  console.log(`Hisabati backend server listening on port ${port}`);
});
