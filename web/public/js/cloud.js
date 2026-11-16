// Firebase Auth + Firestore Cloud Sync for Hisabati Web PWA
// Matches the exact Firebase project, Firestore database ID, and /firestore.rules schema used by Android.
export const FIREBASE_CONFIG = {
  apiKey: "AIzaSyCi7giVSyL1CJmu780_Qnc7fQHrpg9TuxA",
  authDomain: "gen-lang-client-0821343243.firebaseapp.com",
  projectId: "gen-lang-client-0821343243",
  storageBucket: "gen-lang-client-0821343243.firebasestorage.app",
  messagingSenderId: "485118474969",
  appId: "1:485118474969:web:hisabati_pwa_client"
};

export const FIRESTORE_DATABASE_ID = "ai-studio-android-cc8646e8-3c99-467c-8184-88976fe831ca";

let firebaseContext = null;

function isValidFirestoreId(val) {
  return typeof val === "string" && val.length >= 1 && val.length <= 128 && /^[a-zA-Z0-9_-]+$/.test(val);
}

function safeOptionalUri(uri) {
  if (typeof uri !== "string" || !uri.trim()) return null;
  const trimmed = uri.trim();
  // firestore.rules enforces avatarUri/attachmentUri <= 500 chars; large base64 data URLs stay local in IndexedDB
  return trimmed.length <= 490 ? trimmed : null;
}

function safeStr(val, maxLen, fallback = "") {
  if (val == null) return fallback;
  const s = String(val).trim();
  return s.slice(0, maxLen);
}

/**
 * Pure schema mapper strictly matching /firestore.rules for create vs update across /users/{uid} and all 10 subcollections.
 * Returns null if the collection is create-only and doc already exists.
 */
export function buildFirestorePayload(collection, rawItem, uid, isCreate, serverTimestampFn) {
  const item = rawItem || {};
  const ts = () => (typeof serverTimestampFn === "function" ? serverTimestampFn() : "__SERVER_TS__");
  const now = Date.now();

  switch (collection) {
    case "user_profile": {
      const accountType = item.accountType === "BUSINESS" ? "BUSINESS" : "PERSONAL";
      const displayName = safeStr(item.displayName, 120, "مستخدم حساباتي") || "مستخدم حساباتي";
      const base = {
        displayName,
        email: item.email ? safeStr(item.email, 200) : null,
        accountType,
        workspaceName: item.workspaceName ? safeStr(item.workspaceName, 120) : null,
        defaultCurrency: safeStr(item.defaultCurrency, 10, "YER") || "YER",
        aiConsentGranted: Boolean(item.aiConsentGranted),
        updatedAt: ts()
      };
      if (isCreate) {
        return { userId: uid, ...base, createdAt: ts() };
      }
      return base;
    }

    case "contacts": {
      const fullName = safeStr(item.fullName, 120, "عميل") || "عميل";
      const phone = safeStr(item.phone, 40, "");
      const updatePayload = {
        fullName,
        phone,
        address: item.address ? safeStr(item.address, 250) : null,
        notes: item.notes ? safeStr(item.notes, 1000) : null,
        avatarUri: safeOptionalUri(item.avatarUri),
        updatedAtMillis: Math.max(0, Math.trunc(Number(item.updatedAtMillis) || now)),
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          ...updatePayload,
          createdAtMillis: Math.max(0, Math.trunc(Number(item.createdAtMillis) || now)),
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    case "debts": {
      const principalMinor = Math.max(1, Math.trunc(Number(item.principalMinor) || 1));
      const remainingMinor = Math.min(principalMinor, Math.max(0, Math.trunc(Number(item.remainingMinor) ?? principalMinor)));
      const allowedStatuses = new Set(["OPEN", "PARTIAL", "PAID", "OVERDUE", "SCHEDULED", "SETTLED"]);
      const status = allowedStatuses.has(item.status) ? item.status : "OPEN";
      const updatePayload = {
        contactName: safeStr(item.contactName, 120, "عميل") || "عميل",
        remainingMinor,
        dueAtMillis: Math.max(0, Math.trunc(Number(item.dueAtMillis) || now)),
        reason: safeStr(item.reason, 300, "معاملة مالية"),
        paymentMethod: item.paymentMethod ? safeStr(item.paymentMethod, 60) : null,
        attachmentUri: safeOptionalUri(item.attachmentUri),
        notes: item.notes ? safeStr(item.notes, 1000) : null,
        status,
        version: Math.max(1, Math.trunc(Number(item.version) || 1)),
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          contactId: isValidFirestoreId(item.contactId) ? item.contactId : "unknown_contact",
          ...updatePayload,
          direction: item.direction === "PAYABLE" ? "PAYABLE" : "RECEIVABLE",
          principalMinor,
          currencyCode: safeStr(item.currencyCode, 10, "YER") || "YER",
          exchangeRateMicros: Math.max(1, Math.trunc(Number(item.exchangeRateMicros) || 1_000_000)),
          recordedAtMillis: Math.max(0, Math.trunc(Number(item.recordedAtMillis) || now)),
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    case "payments": {
      // Immutable ledger in firestore.rules: allow update: if false
      if (!isCreate) return null;
      const amountMinor = Math.max(0, Math.trunc(Number(item.amountMinor) || 0));
      const discountMinor = Math.max(0, Math.trunc(Number(item.discountMinor) || 0));
      const allowedMethods = new Set(["CASH", "TRANSFER", "CARD", "OTHER"]);
      const balanceBeforeMinor = Math.max(0, Math.trunc(Number(item.balanceBeforeMinor) || (amountMinor + discountMinor)));
      const balanceAfterMinor = Math.min(balanceBeforeMinor, Math.max(0, Math.trunc(Number(item.balanceAfterMinor) || 0)));
      const idem = safeStr(item.idempotencyKey, 128, `idem_${uid.slice(0, 6)}_${now}`);
      return {
        userId: uid,
        debtId: isValidFirestoreId(item.debtId) ? item.debtId : "unknown_debt",
        contactId: isValidFirestoreId(item.contactId) ? item.contactId : "unknown_contact",
        receiptNumber: safeStr(item.receiptNumber, 64, `HSB-${now.toString().slice(-6)}`) || `HSB-${now.toString().slice(-6)}`,
        amountMinor: amountMinor + discountMinor > 0 ? amountMinor : 1,
        discountMinor,
        currencyCode: safeStr(item.currencyCode, 10, "YER") || "YER",
        method: allowedMethods.has(item.method) ? item.method : "CASH",
        paidAtMillis: Math.max(0, Math.trunc(Number(item.paidAtMillis) || now)),
        balanceBeforeMinor,
        balanceAfterMinor,
        notes: item.notes ? safeStr(item.notes, 1000) : null,
        idempotencyKey: idem.length >= 8 ? idem : `idem_${now}_${idem}`,
        createdAt: ts()
      };
    }

    case "installments": {
      const amountMinor = Math.max(1, Math.trunc(Number(item.amountMinor) || 1));
      const paidMinor = Math.min(amountMinor, Math.max(0, Math.trunc(Number(item.paidMinor) || 0)));
      const allowedStatus = new Set(["PENDING", "PAID", "OVERDUE"]);
      const updatePayload = {
        amountMinor,
        paidMinor,
        dueAtMillis: Math.max(0, Math.trunc(Number(item.dueAtMillis) || now)),
        status: allowedStatus.has(item.status) ? item.status : "PENDING",
        paidAtMillis: item.paidAtMillis != null ? Math.max(0, Math.trunc(Number(item.paidAtMillis))) : null,
        notes: item.notes ? safeStr(item.notes, 500) : null,
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          debtId: isValidFirestoreId(item.debtId) ? item.debtId : "unknown_debt",
          installmentNumber: Math.max(1, Math.trunc(Number(item.installmentNumber) || 1)),
          ...updatePayload,
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    case "currencies": {
      const updatePayload = {
        nameAr: safeStr(item.nameAr, 60, "عملة") || "عملة",
        symbol: safeStr(item.symbol, 10, "$") || "$",
        rateToUsdMicros: Math.max(1, Math.trunc(Number(item.rateToUsdMicros) || 1_000_000)),
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          code: safeStr(item.code, 10, "USD").toUpperCase() || "USD",
          ...updatePayload,
          isCustom: Boolean(item.isCustom),
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    case "reminders": {
      const allowedRepeat = new Set(["ONCE", "DAILY", "WEEKLY", "MONTHLY"]);
      const allowedPriority = new Set(["HIGH", "NORMAL", "LOW"]);
      const updatePayload = {
        title: safeStr(item.title, 120, "تذكير مالي") || "تذكير مالي",
        message: safeStr(item.message, 500, ""),
        triggerAtMillis: Math.max(0, Math.trunc(Number(item.triggerAtMillis) || now)),
        repeatType: allowedRepeat.has(item.repeatType) ? item.repeatType : "ONCE",
        priority: allowedPriority.has(item.priority) ? item.priority : "NORMAL",
        isEnabled: item.isEnabled !== false,
        isSent: Boolean(item.isSent),
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          debtId: isValidFirestoreId(item.debtId) ? item.debtId : "general",
          contactId: isValidFirestoreId(item.contactId) ? item.contactId : "general",
          ...updatePayload,
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    case "notification_logs": {
      if (!isCreate) {
        return {
          isRead: Boolean(item.isRead),
          updatedAt: ts()
        };
      }
      return {
        userId: uid,
        title: safeStr(item.title, 150, "تنبيه") || "تنبيه",
        body: safeStr(item.body, 600, ""),
        category: safeStr(item.category, 50, "GENERAL") || "GENERAL",
        relatedDebtId: item.relatedDebtId && isValidFirestoreId(item.relatedDebtId) ? item.relatedDebtId : null,
        isRead: Boolean(item.isRead),
        createdAtMillis: Math.max(0, Math.trunc(Number(item.createdAtMillis) || now)),
        createdAt: ts(),
        updatedAt: ts()
      };
    }

    case "message_templates": {
      const allowedChannels = new Set(["WHATSAPP", "SMS", "BOTH"]);
      const updatePayload = {
        titleAr: safeStr(item.titleAr, 120, "قالب رسالة") || "قالب رسالة",
        bodyTemplate: safeStr(item.bodyTemplate, 1500, "{الاسم}") || "{الاسم}",
        channel: allowedChannels.has(item.channel) ? item.channel : "WHATSAPP",
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          type: safeStr(item.type, 50, "CUSTOM") || "CUSTOM",
          ...updatePayload,
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    case "audit_logs": {
      // Immutable audit trail in firestore.rules: allow update: if false
      if (!isCreate) return null;
      return {
        userId: uid,
        actorName: safeStr(item.actorName, 120, "صاحب الحساب") || "صاحب الحساب",
        action: safeStr(item.action, 80, "عملية") || "عملية",
        entityType: safeStr(item.entityType, 50, "DEBT") || "DEBT",
        entityId: safeStr(item.entityId, 128, "unknown") || "unknown",
        details: safeStr(item.details, 1000, ""),
        reason: item.reason ? safeStr(item.reason, 500) : null,
        createdAtMillis: Math.max(0, Math.trunc(Number(item.createdAtMillis) || now)),
        createdAt: ts()
      };
    }

    case "workspace_members": {
      const allowedRoles = new Set(["OWNER", "MANAGER", "STAFF", "VIEWER"]);
      const email = safeStr(item.email, 200, "member@hisabati.local");
      const updatePayload = {
        displayName: safeStr(item.displayName, 120, "عضو") || "عضو",
        role: allowedRoles.has(item.role) ? item.role : "STAFF",
        canAddDebt: Boolean(item.canAddDebt),
        canEditDebt: Boolean(item.canEditDebt),
        canRecordPayment: Boolean(item.canRecordPayment),
        canViewReports: Boolean(item.canViewReports),
        updatedAt: ts()
      };
      if (isCreate) {
        return {
          userId: uid,
          email: email.length >= 3 ? email : "member@hisabati.local",
          ...updatePayload,
          createdAt: ts()
        };
      }
      return updatePayload;
    }

    default:
      return null;
  }
}

export function translateAuthErrorAr(err) {
  const code = err?.code || "";
  if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") {
    return "تم إغلاق نافذة تسجيل الدخول قبل الإكمال.";
  }
  if (code === "auth/unauthorized-domain") {
    return `النطاق الحالي (${typeof window !== "undefined" ? window.location.hostname : ""}) غير مضاف ضمن النطاقات المصرح بها في Firebase Auth، أو يمكنك استخدام البريد الإلكتروني وكلمة المرور.`;
  }
  if (code === "auth/operation-not-allowed") {
    return "طريقة تسجيل الدخول هذه غير مفعّلة حالياً في إعدادات Firebase Authentication.";
  }
  if (code === "auth/invalid-credential" || code === "auth/wrong-password" || code === "auth/user-not-found") {
    return "البريد الإلكتروني أو كلمة المرور غير صحيحة.";
  }
  if (code === "auth/email-already-in-use") {
    return "هذا البريد الإلكتروني مسجل مسبقاً — استخدم تسجيل الدخول بدلاً من إنشاء حساب.";
  }
  if (code === "auth/weak-password") {
    return "كلمة المرور ضعيفة: يجب أن تتكون من 6 أحرف على الأقل.";
  }
  if (code === "auth/invalid-email") {
    return "صيغة البريد الإلكتروني غير صحيحة.";
  }
  if (code === "auth/network-request-failed") {
    return "تعذر الاتصال بخوادم المصادقة — تحقق من اتصالك بالإنترنت.";
  }
  if (code === "auth/requires-recent-login") {
    return "لأسباب أمنية، يرجى إعادة تسجيل الدخول ثم المحاولة مرة أخرى لحذف الحساب.";
  }
  return err?.message || "تعذر إتمام العملية السحابية.";
}

export async function initFirebaseSafely() {
  if (typeof navigator !== "undefined" && !navigator.onLine) return null;
  if (firebaseContext) return firebaseContext;
  try {
    const [appMod, authMod, fsMod] = await Promise.all([
      import("https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js"),
      import("https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js"),
      import("https://www.gstatic.com/firebasejs/11.0.2/firebase-firestore.js")
    ]);
    const app = appMod.getApps().length ? appMod.getApp() : appMod.initializeApp(FIREBASE_CONFIG);
    const auth = authMod.getAuth(app);
    auth.languageCode = "ar";
    let db;
    try {
      db = fsMod.initializeFirestore(
        app,
        { localCache: fsMod.persistentLocalCache() },
        FIRESTORE_DATABASE_ID
      );
    } catch {
      db = fsMod.getFirestore(app, FIRESTORE_DATABASE_ID);
    }
    firebaseContext = { app, auth, db, authMod, fsMod };
    return firebaseContext;
  } catch {
    return null;
  }
}

export async function handleAuthRedirectResult() {
  const ctx = await initFirebaseSafely();
  if (!ctx) return null;
  try {
    const res = await ctx.authMod.getRedirectResult(ctx.auth);
    return res?.user || null;
  } catch {
    return null;
  }
}

export async function subscribeAuthState(callback) {
  const ctx = await initFirebaseSafely();
  if (!ctx) return () => {};
  return ctx.authMod.onAuthStateChanged(ctx.auth, (user) => {
    callback(user || null);
  });
}

export async function signInWithGoogleCloud() {
  const ctx = await initFirebaseSafely();
  if (!ctx) throw new Error("يتطلب تسجيل الدخول اتصالاً بالإنترنت.");
  const provider = new ctx.authMod.GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  try {
    const result = await ctx.authMod.signInWithPopup(ctx.auth, provider);
    return result.user;
  } catch (err) {
    if (err?.code === "auth/popup-blocked" || err?.code === "auth/operation-not-supported-in-this-environment") {
      await ctx.authMod.signInWithRedirect(ctx.auth, provider);
      return null;
    }
    throw new Error(translateAuthErrorAr(err));
  }
}

export async function signInWithEmailCloud(email, password) {
  const ctx = await initFirebaseSafely();
  if (!ctx) throw new Error("يتطلب تسجيل الدخول اتصالاً بالإنترنت.");
  try {
    const result = await ctx.authMod.signInWithEmailAndPassword(ctx.auth, String(email || "").trim(), String(password || ""));
    return result.user;
  } catch (err) {
    throw new Error(translateAuthErrorAr(err));
  }
}

export async function signUpWithEmailCloud(email, password, displayName) {
  const ctx = await initFirebaseSafely();
  if (!ctx) throw new Error("يتطلب إنشاء الحساب اتصالاً بالإنترنت.");
  try {
    const result = await ctx.authMod.createUserWithEmailAndPassword(ctx.auth, String(email || "").trim(), String(password || ""));
    if (displayName && result.user) {
      await ctx.authMod.updateProfile(result.user, { displayName: String(displayName).trim() });
    }
    return result.user;
  } catch (err) {
    throw new Error(translateAuthErrorAr(err));
  }
}

export async function sendPasswordResetCloud(email) {
  const ctx = await initFirebaseSafely();
  if (!ctx) throw new Error("يتطلب استعادة كلمة المرور اتصالاً بالإنترنت.");
  try {
    await ctx.authMod.sendPasswordResetEmail(ctx.auth, String(email || "").trim());
  } catch (err) {
    throw new Error(translateAuthErrorAr(err));
  }
}

export async function signOutCloud() {
  const ctx = await initFirebaseSafely();
  if (!ctx) return;
  await ctx.authMod.signOut(ctx.auth);
}

export async function deleteCurrentAuthUserSafely() {
  const ctx = await initFirebaseSafely();
  if (!ctx?.auth?.currentUser) return;
  try {
    await ctx.authMod.deleteUser(ctx.auth.currentUser);
  } catch (err) {
    throw new Error(translateAuthErrorAr(err));
  }
}

export async function ensureCloudUserProfile(user, prefs = {}) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !user?.uid) return null;
  const { db, fsMod } = ctx;
  const ref = fsMod.doc(db, "users", user.uid);
  const snap = await fsMod.getDoc(ref);
  const isCreate = !snap.exists();
  const payload = buildFirestorePayload(
    "user_profile",
    {
      displayName: user.displayName || prefs.workspaceName || "مستخدم حساباتي",
      email: user.email || null,
      accountType: prefs.accountType || "PERSONAL",
      workspaceName: prefs.workspaceName || null,
      defaultCurrency: prefs.defaultCurrency || "YER",
      aiConsentGranted: Boolean(prefs.aiConsentGranted)
    },
    user.uid,
    isCreate,
    fsMod.serverTimestamp
  );
  if (isCreate) {
    await fsMod.setDoc(ref, payload);
  } else {
    await fsMod.updateDoc(ref, payload);
  }
  return payload;
}

export async function pullCloudSnapshot(uid) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !uid) return null;
  const { db, fsMod } = ctx;
  const collectionNames = [
    "contacts",
    "debts",
    "payments",
    "installments",
    "currencies",
    "reminders",
    "notification_logs",
    "message_templates",
    "audit_logs",
    "workspace_members"
  ];
  const snaps = await Promise.all(
    collectionNames.map((col) => fsMod.getDocs(fsMod.collection(db, "users", uid, col)))
  );
  const out = {};
  collectionNames.forEach((col, i) => {
    out[col] = snaps[i].docs.map((d) => ({ id: d.id, ...d.data() }));
  });
  return out;
}

export async function subscribeCloudRealtime(uid, onCollectionData) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !uid) return () => {};
  const { db, fsMod } = ctx;
  const mapping = [
    ["contacts", "contacts"],
    ["debts", "debts"],
    ["payments", "payments"],
    ["installments", "installments"],
    ["currencies", "currencies"],
    ["reminders", "reminders"],
    ["notification_logs", "notifications"],
    ["message_templates", "templates"],
    ["audit_logs", "auditLogs"],
    ["workspace_members", "members"]
  ];

  const unsubs = mapping.map(([col, storeName]) =>
    fsMod.onSnapshot(
      fsMod.collection(db, "users", uid, col),
      (snap) => {
        const docs = snap.docs.map((d) => {
          const data = { ...d.data() };
          delete data.createdAt;
          delete data.updatedAt;
          return { id: d.id, ...data };
        });
        onCollectionData(storeName, docs);
      },
      () => {
        // Ignore transient permission/offline listener drops; reconnects automatically
      }
    )
  );

  return () => {
    for (const u of unsubs) {
      try { u(); } catch {}
    }
  };
}

export async function pushOutboxToCloud(uid, outboxItems) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !uid || !outboxItems?.length) return [];
  const { db, fsMod } = ctx;
  const syncedIds = [];

  for (const item of outboxItems) {
    try {
      const ref = fsMod.doc(db, "users", uid, item.collection, item.docId);
      if (item.operation === "DELETE") {
        await fsMod.deleteDoc(ref);
        syncedIds.push(item.id);
        continue;
      }

      const existingSnap = await fsMod.getDoc(ref);
      const isCreate = !existingSnap.exists();
      const payload = buildFirestorePayload(
        item.collection,
        item.payload,
        uid,
        isCreate,
        fsMod.serverTimestamp
      );

      if (payload === null) {
        // Immutable collection (payments, audit_logs) already exists in Firestore
        syncedIds.push(item.id);
        continue;
      }

      if (isCreate) {
        await fsMod.setDoc(ref, payload);
      } else {
        await fsMod.updateDoc(ref, payload);
      }
      syncedIds.push(item.id);
    } catch {
      // Leave in IndexedDB outbox for automatic retry
    }
  }
  return syncedIds;
}

export async function deleteAllCloudUserData(uid) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !uid) return;
  const { db, fsMod } = ctx;
  const cols = [
    "contacts",
    "debts",
    "payments",
    "installments",
    "currencies",
    "reminders",
    "notification_logs",
    "message_templates",
    "audit_logs",
    "workspace_members"
  ];
  for (const col of cols) {
    const snap = await fsMod.getDocs(fsMod.collection(db, "users", uid, col));
    for (const d of snap.docs) {
      await fsMod.deleteDoc(d.ref);
    }
  }
  await fsMod.deleteDoc(fsMod.doc(db, "users", uid));
}

export async function sendViaBackendRelay({
  baseUrl,
  channel,
  toPhone,
  messageBody,
  templateType = "CUSTOM",
  phoneNumberId = "",
  senderId = "HISABATI"
}) {
  const cleanBase = String(baseUrl || "").trim().replace(/\/+$/, "");
  if (!cleanBase || !/^https?:\/\//i.test(cleanBase)) {
    throw new Error("يرجى إدخال رابط خادم Backend صالح (HTTPS) في إعدادات الإرسال الآلي.");
  }
  const ctx = await initFirebaseSafely();
  const user = ctx?.auth?.currentUser;
  if (!user) {
    throw new Error("يرجى تسجيل الدخول بالحساب السحابي للإرسال عبر الخادم الآمن.");
  }
  const idToken = await user.getIdToken(false);
  const path = channel === "SMS" ? "/api/v1/messaging/sms/send" : "/api/v1/messaging/whatsapp/send";
  const resp = await fetch(`${cleanBase}${path}`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${idToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      toPhone,
      messageBody,
      templateType,
      phoneNumberId: phoneNumberId || undefined,
      senderId: senderId || undefined
    })
  });
  const data = await resp.json().catch(() => ({}));
  if (!resp.ok) {
    throw new Error(data.error || `رفض الخادم الطلب (${resp.status})`);
  }
  return data;
}
