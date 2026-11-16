import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import {
  buildCsvContent,
  convertMinor,
  formatMinor,
  parseMajorToMinor
} from "../public/js/money.js";
import {
  buildFormattedWhatsAppMessage,
  openSmsComposer,
  renderTemplateText
} from "../public/js/whatsapp.js";
import { buildInvoiceHtml, PDF_TEMPLATES } from "../public/js/print.js";
import {
  allocatePaymentToInstallments,
  buildInstallmentSchedule,
  evaluateDueNotifications
} from "../public/js/notifications.js";
import {
  buildMonthlySeries,
  renderMonthlyCashflowChartHtml,
  renderReceivablePayableDonutHtml
} from "../public/js/charts.js";
import { AI_QUICK_PROMPTS, answerFinancialQuestionLocally } from "../public/js/ai.js";
import { buildFirestorePayload } from "../public/js/cloud.js";
import {
  buildContactStatementData,
  buildReportDocument,
  getActiveMemberPermissions
} from "../public/js/reports.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.resolve(__dirname, "../public");
const rootDir = path.resolve(__dirname, "../..");

test("PWA manifest, icons, Apple meta tags, notificationclick SW, and silent registration exist", () => {
  const manifestRaw = fs.readFileSync(path.join(publicDir, "manifest.json"), "utf8");
  const manifest = JSON.parse(manifestRaw);
  assert.equal(manifest.short_name, "حساباتي");
  assert.equal(manifest.dir, "rtl");
  assert.equal(manifest.display, "standalone");
  assert.ok(manifest.icons.some((i) => i.sizes === "192x192"));
  assert.ok(manifest.icons.some((i) => i.sizes === "512x512"));

  assert.ok(fs.existsSync(path.join(publicDir, "icons/icon-192.png")));
  assert.ok(fs.existsSync(path.join(publicDir, "icons/icon-512.png")));
  assert.ok(fs.existsSync(path.join(publicDir, "icons/apple-touch-icon.png")));

  const indexHtml = fs.readFileSync(path.join(publicDir, "index.html"), "utf8");
  assert.ok(indexHtml.includes('rel="manifest"'));
  assert.ok(indexHtml.includes('name="apple-mobile-web-app-capable"'));
  assert.ok(indexHtml.includes('rel="apple-touch-icon"'));

  const swJs = fs.readFileSync(path.join(publicDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("networkFirst"));
  assert.ok(swJs.includes("staleWhileRevalidate"));
  assert.ok(swJs.includes("notificationclick"));
  assert.ok(swJs.includes("./js/notifications.js"));
  assert.ok(swJs.includes("./js/charts.js"));
  assert.ok(swJs.includes("./js/ai.js"));

  // Every JS file in public/js/ must be precached by sw.js
  for (const file of fs.readdirSync(path.join(publicDir, "js"))) {
    if (file.endsWith(".js")) {
      assert.ok(swJs.includes(`./js/${file}`), `Missing ./js/${file} in sw.js SHELL_ASSETS`);
    }
  }

  const appJs = fs.readFileSync(path.join(publicDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes('navigator.serviceWorker.register("./sw.js"'));
  assert.ok(appJs.includes("renderAiAssistantPage"));

  const readme = fs.readFileSync(path.join(rootDir, "README.md"), "utf8");
  assert.ok(readme.includes("تحميل واستخدام التطبيق على الآيفون (بدون إنترنت)"));
  assert.ok(readme.includes("[ضع رابط الاستضافة هنا]"));
});

test("Yemeni Rial minor-unit math, custom currency conversion, WhatsApp/SMS formatter, and 4 invoice templates work", () => {
  const customCurrencies = [{ code: "KWD", nameAr: "دينار كويتي", symbol: "د.ك", rateToUsdMicros: 3_250_000, isCustom: true }];
  assert.equal(parseMajorToMinor("1,250.50"), 125050);
  assert.equal(parseMajorToMinor("١٠٠"), 10000);
  assert.equal(formatMinor(500000, "YER"), "5,000 ر.ي");
  assert.equal(formatMinor(10000, "KWD", customCurrencies), "100 د.ك");
  assert.ok(convertMinor(10000, "USD", "YER") > 10000);
  assert.equal(convertMinor(10000, "KWD", "USD", customCurrencies), 32500);

  const msg = buildFormattedWhatsAppMessage(
    { workspaceName: "متجر اليمن", whatsappStyle: "FORMAL" },
    {
      title: "سند مطالبة",
      contactName: "خالد علي",
      remainingFormatted: "5,000 ر.ي",
      receiptNumber: "DBT-101"
    }
  );
  assert.ok(msg.includes("متجر اليمن"));
  assert.ok(msg.includes("خالد علي"));
  assert.ok(msg.includes("5,000 ر.ي"));

  const tplText = renderTemplateText("مرحباً {الاسم}، المتبقي {المتبقي}", { name: "ياسر", remaining: "200 $" });
  assert.equal(tplText, "مرحباً ياسر، المتبقي 200 $");
  assert.ok(openSmsComposer("772317950", "مرحباً").startsWith("sms:967772317950?body="));

  for (const tpl of PDF_TEMPLATES) {
    const html = buildInvoiceHtml(
      { printTemplate: tpl.id, printCompanyName: "مؤسسة الأمل", printDebtColorHex: "#0B1B32", printShowWatermarkBadge: true },
      {
        docKind: "DEBT_VOUCHER",
        title: "سند قيد مالي",
        subtitle: "العميل: خالد علي",
        summary: [["المبلغ", "5,000 ر.ي"]],
        rows: [{ date: "2026/11/14", title: "فاتورة", amount: "5,000 ر.ي", status: "مفتوح" }]
      }
    );
    assert.ok(html.includes("مؤسسة الأمل"));
    assert.ok(html.includes("772317950"));
  }
});

test("Installment scheduling, payment allocation, local notification evaluation, and SVG charts work offline", () => {
  const now = 1_760_000_000_000;
  const DAY = 86_400_000;

  const schedule = buildInstallmentSchedule({
    debtId: "dbt_1",
    userId: "u1",
    principalMinor: 100_000,
    installmentCount: 3,
    recordedAtMillis: now - 10 * DAY,
    dueAtMillis: now + 20 * DAY,
    nowMillis: now
  });
  assert.equal(schedule.length, 3);
  assert.equal(schedule.reduce((s, i) => s + i.amountMinor, 0), 100_000);

  const modified = allocatePaymentToInstallments(schedule, 40_000, now);
  assert.equal(modified[0].status, "PAID");
  assert.equal(modified[1].paidMinor, 40_000 - schedule[0].amountMinor);

  const debt = {
    id: "dbt_over",
    contactName: "طارق سعيد",
    direction: "RECEIVABLE",
    principalMinor: 100_000,
    remainingMinor: 60_000,
    currencyCode: "YER",
    recordedAtMillis: now - 20 * DAY,
    dueAtMillis: now - 2 * DAY,
    reason: "فاتورة",
    status: "PARTIAL"
  };

  const disabledEval = evaluateDueNotifications({
    debts: [debt],
    installments: schedule,
    reminders: [],
    existingNotifications: [],
    prefs: { notificationsEnabled: false },
    nowMillis: now
  });
  assert.equal(disabledEval.newNotifications.length, 0);

  const eval1 = evaluateDueNotifications({
    debts: [debt],
    installments: [
      { id: "ins_due_soon", debtId: "dbt_over", installmentNumber: 2, amountMinor: 30_000, paidMinor: 0, dueAtMillis: now + 12 * 3_600_000, status: "PENDING" }
    ],
    reminders: [
      { id: "rem_1", debtId: "dbt_over", title: "متابعة قسط", message: "رسالة", triggerAtMillis: now - 1000, repeatType: "DAILY", isEnabled: true, isSent: false }
    ],
    existingNotifications: [],
    prefs: { notificationsEnabled: true, defaultCurrency: "YER" },
    nowMillis: now
  });
  assert.equal(eval1.newNotifications.length, 3);
  assert.ok(eval1.newNotifications.some((n) => n.category === "OVERDUE"));
  assert.ok(eval1.newNotifications.some((n) => n.category === "INSTALLMENT_DUE_SOON"));
  assert.ok(eval1.newNotifications.some((n) => n.category === "CUSTOM_REMINDER"));
  assert.ok(eval1.updatedReminders[0].triggerAtMillis > now);

  // Deduplicates same day
  const eval2 = evaluateDueNotifications({
    debts: [debt],
    installments: [
      { id: "ins_due_soon", debtId: "dbt_over", installmentNumber: 2, amountMinor: 30_000, paidMinor: 0, dueAtMillis: now + 12 * 3_600_000, status: "PENDING" }
    ],
    reminders: eval1.updatedReminders,
    existingNotifications: eval1.newNotifications,
    prefs: { notificationsEnabled: true, defaultCurrency: "YER" },
    nowMillis: now + 60_000
  });
  assert.equal(eval2.newNotifications.length, 0);

  const series = buildMonthlySeries([debt], [], "YER", now);
  assert.equal(series.length, 6);
  const cashflowHtml = renderMonthlyCashflowChartHtml(series);
  assert.ok(cashflowHtml.includes("<svg"));
  const donutHtml = renderReceivablePayableDonutHtml(60_000, 20_000, "YER");
  assert.ok(donutHtml.includes("75%"));
});

test("Web/iPhone PWA Smart Financial AI answers receivables, overdue debts, installments, and specific contact lookups offline", () => {
  const now = 1_760_000_000_000;
  const DAY = 86_400_000;
  const state = {
    preferences: { defaultCurrency: "YER", aiConsentGranted: true },
    contacts: [{ id: "c1", fullName: "أحمد السقاف", phone: "771122334" }],
    debts: [
      {
        id: "d1",
        contactId: "c1",
        contactName: "أحمد السقاف",
        direction: "RECEIVABLE",
        principalMinor: 150_000,
        remainingMinor: 100_000,
        currencyCode: "YER",
        recordedAtMillis: now - 15 * DAY,
        dueAtMillis: now - 3 * DAY,
        status: "PARTIAL"
      },
      {
        id: "d2",
        contactId: "c2",
        contactName: "مؤسسة البناء",
        direction: "PAYABLE",
        principalMinor: 40_000,
        remainingMinor: 40_000,
        currencyCode: "YER",
        recordedAtMillis: now - 5 * DAY,
        dueAtMillis: now + 10 * DAY,
        status: "OPEN"
      }
    ],
    payments: [{ id: "p1", debtId: "d1", amountMinor: 50_000, currencyCode: "YER", paidAtMillis: now - DAY }],
    installments: [
      { id: "i1", debtId: "d1", installmentNumber: 2, amountMinor: 50_000, paidMinor: 0, dueAtMillis: now + 2 * DAY, status: "PENDING" }
    ]
  };

  assert.equal(AI_QUICK_PROMPTS.length, 7);
  const recAnswer = answerFinancialQuestionLocally({ question: "ما إجمالي المستحق لي؟", state, nowMillis: now });
  assert.ok(recAnswer.includes("1,000 ر.ي"));
  assert.ok(recAnswer.includes("أحمد السقاف"));

  const overdueAnswer = answerFinancialQuestionLocally({ question: "ما الديون المتأخرة؟", state, nowMillis: now });
  assert.ok(overdueAnswer.includes("أحمد السقاف"));

  const instAnswer = answerFinancialQuestionLocally({ question: "ما الأقساط المستحقة هذا الأسبوع؟", state, nowMillis: now });
  assert.ok(instAnswer.includes("#2"));

  const contactAnswer = answerFinancialQuestionLocally({ question: "كم حساب أحمد السقاف؟", state, nowMillis: now });
  assert.ok(contactAnswer.includes("أحمد السقاف"));
  assert.ok(contactAnswer.includes("1,000 ر.ي"));

  const summaryAnswer = answerFinancialQuestionLocally({ question: "اعطني ملخصاً عن هذا الشهر", state, nowMillis: now });
  assert.ok(summaryAnswer.includes("صافي المركز المالي"));
});

test("Firestore payload builder enforces firestore.rules schema, CSV UTF-8 BOM, Reports, Statements, and RBAC parity", () => {
  const uid = "user_abc123";
  const ts = () => "SERVER_TS";

  // 1) Contacts: strip >490 char data URI so firestore.rules <=500 never fails; update excludes createdAt
  const bigAvatar = "data:image/jpeg;base64," + "a".repeat(600);
  const cCreate = buildFirestorePayload("contacts", { fullName: "صالح", phone: "770001112", avatarUri: bigAvatar }, uid, true, ts);
  assert.equal(cCreate.userId, uid);
  assert.equal(cCreate.avatarUri, null);
  assert.equal(cCreate.createdAt, "SERVER_TS");

  const cUpdate = buildFirestorePayload("contacts", { fullName: "صالح", phone: "770001112", avatarUri: "https://ex.com/a.jpg" }, uid, false, ts);
  assert.equal("createdAt" in cUpdate, false);
  assert.equal("userId" in cUpdate, false);
  assert.equal(cUpdate.avatarUri, "https://ex.com/a.jpg");

  // 2) Debts: update omits immutable fields (principalMinor, direction, currencyCode, createdAt)
  const dUpdate = buildFirestorePayload("debts", { contactId: "c1", contactName: "صالح", principalMinor: 50000, remainingMinor: 20000, direction: "RECEIVABLE", currencyCode: "YER" }, uid, false, ts);
  assert.equal("principalMinor" in dUpdate, false);
  assert.equal("direction" in dUpdate, false);
  assert.equal("createdAt" in dUpdate, false);
  assert.equal(dUpdate.remainingMinor, 20000);

  // 3) Payments & Audit Logs: create-only (update returns null)
  assert.equal(buildFirestorePayload("payments", { debtId: "d1", contactId: "c1", amountMinor: 1000 }, uid, false, ts), null);
  assert.equal(buildFirestorePayload("audit_logs", { action: "edit" }, uid, false, ts), null);
  const pCreate = buildFirestorePayload("payments", { debtId: "d1", contactId: "c1", amountMinor: 1000, receiptNumber: "R-1" }, uid, true, ts);
  assert.equal("updatedAt" in pCreate, false);
  assert.ok(pCreate.idempotencyKey.length >= 8);

  // 4) Reminders: invalid empty debtId/contactId fallback to valid alphanumeric IDs
  const rCreate = buildFirestorePayload("reminders", { debtId: "", contactId: "", title: "تذكير", message: "نص" }, uid, true, ts);
  assert.equal(rCreate.debtId, "general");
  assert.equal(rCreate.contactId, "general");

  // 5) CSV BOM & Reports & Statement & RBAC
  const csv = buildCsvContent([{ id: "d1", contactName: "أحمد", direction: "RECEIVABLE", principalMinor: 10000, remainingMinor: 5000, currencyCode: "YER", recordedAtMillis: 1_760_000_000_000, dueAtMillis: 1_760_000_000_000, status: "PARTIAL", reason: "سلفة" }]);
  assert.ok(csv.startsWith("\uFEFFالمعرف,الشخص,الاتجاه"));

  const sampleState = {
    preferences: { defaultCurrency: "YER", activeMemberId: "staff_1" },
    members: [{ id: "staff_1", displayName: "موظف", role: "STAFF", canAddDebt: true, canEditDebt: false, canRecordPayment: true, canViewReports: false }],
    contacts: [{ id: "c1", fullName: "أحمد", phone: "771122334" }],
    debts: [{ id: "d1", contactId: "c1", contactName: "أحمد", direction: "RECEIVABLE", principalMinor: 10000, remainingMinor: 5000, currencyCode: "YER", recordedAtMillis: 1_760_000_000_000, dueAtMillis: 1_760_000_000_000, status: "PARTIAL", reason: "سلفة" }],
    payments: [],
    currencies: []
  };
  const perms = getActiveMemberPermissions(sampleState);
  assert.equal(perms.canAddDebt, true);
  assert.equal(perms.canEditDebt, false);
  assert.equal(perms.canViewReports, false);

  const report = buildReportDocument(sampleState, "RECEIVABLES");
  assert.equal(report.rows.length, 1);
  const st = buildContactStatementData(sampleState, "c1", 0);
  assert.equal(st.currencySummaries[0].recRemaining, 5000);
});

