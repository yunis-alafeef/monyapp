import { DEFAULT_CURRENCIES } from "./money.js";

const DB_NAME = "hisabati_pwa_db";
const DB_VERSION = 3;

export const DEFAULT_PREFS = {
  id: "user_prefs",
  onboardingCompleted: false,
  themeMode: "SYSTEM", // SYSTEM | LIGHT | DARK
  themePalette: "royal", // royal | azure | teal | amethyst | ruby | gold | custom
  customPrimaryHex: "#0B1B32",
  customAccentHex: "#10B981",
  customThemes: [], // [{ id, name, primaryHex, accentHex }]
  defaultCurrency: "YER",
  accountType: "PERSONAL", // PERSONAL | BUSINESS
  workspaceName: "محفظتي الشخصية",
  activeMemberId: "owner",
  aiConsentGranted: false,
  pinEnabled: false,
  pinHash: "",
  pinSalt: "",
  notificationsEnabled: true,
  defaultReminderHour: 9,
  defaultReminderMinute: 0,
  reminderPriorityHigh: true,
  whatsappStyle: "FORMAL",
  whatsappIncludeWorkspaceHeader: true,
  whatsappIncludeFinancialBreakdown: true,
  whatsappIncludeDates: true,
  whatsappIncludeReasonAndNotes: true,
  whatsappIncludeFooterSignature: true,
  whatsappCustomSignature: "شكراً لتعاملكم الكريم • حساباتي",
  whatsappCloudEnabled: false,
  smsGatewayEnabled: false,
  backendBaseUrl: "",
  whatsappPhoneNumberId: "",
  smsSenderId: "HISABATI",
  printTemplate: "CLASSIC_FORMAL",
  printCompanyName: "",
  printCompanyPhone: "",
  printCompanyAddress: "",
  printTaxOrCrNumber: "",
  printLogoDataUrl: "",
  printFooterNote: "شكراً لتعاملكم معنا • هذا المستند صادر آلياً من تطبيق حساباتي",
  printShowLogoInHeader: true,
  printShowInmaSoftInHeader: true,
  printShowInmaSoftInFooter: true,
  printShowSignatureBox: true,
  printShowWatermarkBadge: true,
  printDebtColorHex: "#0B1B32",
  printReceiptColorHex: "#065F46",
  printStatementColorHex: "#1E3A8A",
  printReportColorHex: "#115E59"
};

export const DEFAULT_ARABIC_TEMPLATES = [
  {
    id: "tpl_due_soon",
    type: "DUE_SOON",
    titleAr: "تذكير بقرب موعد الاستحقاق",
    bodyTemplate:
      "*تذكير بموعد استحقاق مالي*\n──────────────\nالأخ الكريم / *{الاسم}*،\nنود تذكيركم بلطف بموعد استحقاق القيد المالي المسجل لدينا:\n▪️ *المبلغ المستحق:* {المبلغ}\n▪️ *البيان:* {السبب}\n⏰ *تاريخ الاستحقاق:* {التاريخ}\n──────────────\nشكراً لتعاونكم وثقتكم.",
    channel: "BOTH"
  },
  {
    id: "tpl_overdue",
    type: "OVERDUE",
    titleAr: "إشعار بتأخر السداد",
    bodyTemplate:
      "*تنبيه باستحقاق متأخر*\n──────────────\nالأخ الكريم / *{الاسم}*،\nنرجو التكرم بمراجعة المبلغ المستحق والمتأخر عن موعد سداده:\n▪️ *المتبقي:* {المتبقي}\n▪️ *البيان:* {السبب}\n⏰ *كان مستحقاً بتاريخ:* {التاريخ}\n──────────────\nنأمل التواصل لتنسيق السداد في أقرب فرصة.",
    channel: "BOTH"
  },
  {
    id: "tpl_payment_receipt",
    type: "PAYMENT_RECEIPT",
    titleAr: "سند استلام دفعة مالية",
    bodyTemplate:
      "*سند إشعار باستلام دفعة*\n──────────────\nالأخ الكريم / *{الاسم}*،\nتم بحمد الله استلام دفعتكم بنجاح:\n▪️ *رقم الإيصال:* `{رقم_الإيصال}`\n▪️ *المبلغ المدفوع:* {المبلغ}\n▪️ *الرصيد المتبقي:* {المتبقي}\n📅 *التاريخ:* {التاريخ}\n──────────────\nشكراً لالتزامكم وتعاملنا الدائم.",
    channel: "WHATSAPP"
  },
  {
    id: "tpl_statement",
    type: "STATEMENT",
    titleAr: "كشف حساب مختصر",
    bodyTemplate:
      "*ملخص كشف الحساب*\n──────────────\nالأخ الكريم / *{الاسم}*،\n▪️ *إجمالي المستحقات:* {الإجمالي}\n▪️ *إجمالي المسدد:* {المسدد}\n▪️ *الرصيد المتبقي:* *{المتبقي}*\n──────────────\nتطبيق حساباتي للإدارة المالية.",
    channel: "WHATSAPP"
  },
  {
    id: "tpl_thank_you",
    type: "THANK_YOU",
    titleAr: "رسالة شكر بعد التسديد الكامل",
    bodyTemplate:
      "*شهادة سداد وشكر*\n──────────────\nالأخ الكريم / *{الاسم}*،\nشكراً جزيلاً على التزامكم وسداد كامل المبلغ ({المبلغ}).\nنسعد دائماً بالتعامل معكم.",
    channel: "WHATSAPP"
  }
];

export const DEFAULT_OWNER_MEMBER = {
  id: "owner",
  displayName: "المالك",
  email: "owner@hisabati.local",
  role: "OWNER",
  canAddDebt: true,
  canEditDebt: true,
  canRecordPayment: true,
  canViewReports: true
};

const ALL_STORES = [
  "contacts",
  "debts",
  "payments",
  "installments",
  "currencies",
  "reminders",
  "notifications",
  "templates",
  "auditLogs",
  "members",
  "outbox",
  "preferences"
];

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      for (const store of ALL_STORES) {
        if (!db.objectStoreNames.contains(store)) {
          db.createObjectStore(store, { keyPath: "id" });
        }
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function getAll(storeName) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, "readonly");
    const req = tx.objectStore(storeName).getAll();
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

async function putItem(storeName, item) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, "readwrite");
    tx.objectStore(storeName).put(item);
    tx.oncomplete = () => resolve(item);
    tx.onerror = () => reject(tx.error);
  });
}

async function putMany(storeName, items) {
  if (!items || !items.length) return;
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, "readwrite");
    const store = tx.objectStore(storeName);
    for (const item of items) store.put(item);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function deleteItem(storeName, id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, "readwrite");
    tx.objectStore(storeName).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function loadAllLocalState() {
  let [
    contacts,
    debts,
    payments,
    installments,
    currencies,
    reminders,
    notifications,
    templates,
    auditLogs,
    members,
    outbox,
    prefsArr
  ] = await Promise.all([
    getAll("contacts"),
    getAll("debts"),
    getAll("payments"),
    getAll("installments"),
    getAll("currencies"),
    getAll("reminders"),
    getAll("notifications"),
    getAll("templates"),
    getAll("auditLogs"),
    getAll("members"),
    getAll("outbox"),
    getAll("preferences")
  ]);

  const now = Date.now();
  if (!currencies.length) {
    currencies = DEFAULT_CURRENCIES.map((c) => ({
      id: `cur_${c.code}`,
      userId: "local_device",
      ...c,
      updatedAtMillis: now
    }));
    await putMany("currencies", currencies);
  }
  if (!templates.length) {
    templates = DEFAULT_ARABIC_TEMPLATES.map((t) => ({
      ...t,
      userId: "local_device",
      updatedAtMillis: now
    }));
    await putMany("templates", templates);
  }
  if (!members.length) {
    members = [{
      ...DEFAULT_OWNER_MEMBER,
      userId: "local_device",
      invitedAtMillis: now
    }];
    await putMany("members", members);
  }

  const prefs = { ...DEFAULT_PREFS, ...(prefsArr[0] || {}) };
  return {
    contacts: contacts.sort((a, b) => (a.fullName || "").localeCompare(b.fullName || "", "ar")),
    debts: debts.sort((a, b) => (b.recordedAtMillis || 0) - (a.recordedAtMillis || 0)),
    payments: payments.sort((a, b) => (b.paidAtMillis || 0) - (a.paidAtMillis || 0)),
    installments: installments.sort((a, b) => (a.dueAtMillis || 0) - (b.dueAtMillis || 0)),
    currencies: currencies.sort((a, b) => (a.isCustom === b.isCustom ? (a.code || "").localeCompare(b.code || "") : a.isCustom ? 1 : -1)),
    reminders: reminders.sort((a, b) => (a.triggerAtMillis || 0) - (b.triggerAtMillis || 0)),
    notifications: notifications.sort((a, b) => (b.createdAtMillis || 0) - (a.createdAtMillis || 0)),
    templates: templates.sort((a, b) => (a.id || "").localeCompare(b.id || "")),
    auditLogs: auditLogs.sort((a, b) => (b.createdAtMillis || 0) - (a.createdAtMillis || 0)),
    members: members.sort((a, b) => (a.id === "owner" ? -1 : b.id === "owner" ? 1 : (a.displayName || "").localeCompare(b.displayName || "", "ar"))),
    outbox,
    preferences: prefs
  };
}

export async function saveContactLocal(contact) { return putItem("contacts", contact); }
export async function deleteContactLocal(id) { return deleteItem("contacts", id); }

export async function saveDebtLocal(debt) { return putItem("debts", debt); }
export async function deleteDebtLocal(id) { return deleteItem("debts", id); }

export async function saveInstallmentsLocal(items) { return putMany("installments", items); }
export async function saveInstallmentLocal(item) { return putItem("installments", item); }

export async function saveCurrencyLocal(currency) { return putItem("currencies", currency); }
export async function deleteCurrencyLocal(id) { return deleteItem("currencies", id); }

export async function saveReminderLocal(reminder) { return putItem("reminders", reminder); }
export async function deleteReminderLocal(id) { return deleteItem("reminders", id); }

export async function saveNotificationLocal(notification) { return putItem("notifications", notification); }
export async function saveNotificationsBatchLocal(list) { return putMany("notifications", list); }

export async function saveTemplateLocal(tpl) { return putItem("templates", tpl); }
export async function saveAuditLogLocal(log) { return putItem("auditLogs", log); }
export async function saveMemberLocal(member) { return putItem("members", member); }
export async function deleteMemberLocal(id) { return deleteItem("members", id); }

export async function markAllNotificationsReadLocal(notifications) {
  const updated = (notifications || []).map((n) => ({ ...n, isRead: true }));
  await putMany("notifications", updated);
  return updated;
}

export async function savePaymentAndDebtLocal(payment, updatedDebt, updatedInstallments = []) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(["payments", "debts", "installments"], "readwrite");
    tx.objectStore("payments").put(payment);
    tx.objectStore("debts").put(updatedDebt);
    const instStore = tx.objectStore("installments");
    for (const inst of updatedInstallments) instStore.put(inst);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function mergeCloudCollectionLocal(storeName, docs) {
  if (!Array.isArray(docs) || !docs.length) return;
  return putMany(storeName, docs);
}

export async function queueOutboxLocal(entry) { return putItem("outbox", entry); }
export async function clearOutboxItemLocal(id) { return deleteItem("outbox", id); }

export async function savePreferencesLocal(prefs) {
  return putItem("preferences", { ...DEFAULT_PREFS, ...prefs, id: "user_prefs" });
}

export async function clearAllLocalData() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(ALL_STORES, "readwrite");
    for (const s of ALL_STORES) tx.objectStore(s).clear();
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function migrateLocalUserIdToCloud(uid) {
  if (!uid) return 0;
  const storeCollectionMap = [
    ["contacts", "contacts"],
    ["debts", "debts"],
    ["payments", "payments"],
    ["installments", "installments"],
    ["currencies", "currencies"],
    ["reminders", "reminders"],
    ["notifications", "notification_logs"],
    ["templates", "message_templates"],
    ["auditLogs", "audit_logs"],
    ["members", "workspace_members"]
  ];

  const existingOutbox = await getAll("outbox");
  const queuedKeys = new Set(existingOutbox.map((o) => `${o.collection}:${o.docId}`));
  let migrated = 0;

  for (const [storeName, collectionName] of storeCollectionMap) {
    const rows = await getAll(storeName);
    const toUpdate = [];
    for (const r of rows) {
      if (!r.userId || r.userId === "local_device") {
        const updated = { ...r, userId: uid };
        toUpdate.push(updated);
        const key = `${collectionName}:${r.id}`;
        if (!queuedKeys.has(key)) {
          queuedKeys.add(key);
          await queueOutboxLocal({
            id: `ob_mig_${collectionName}_${r.id}`,
            collection: collectionName,
            docId: r.id,
            operation: "UPSERT",
            payload: updated
          });
        }
        migrated++;
      }
    }
    if (toUpdate.length) await putMany(storeName, toUpdate);
  }
  return migrated;
}

export async function requestPersistentStorage() {
  try {
    if (typeof navigator !== "undefined" && navigator.storage?.persist) {
      return await navigator.storage.persist();
    }
  } catch {
    // Ignore unsupported environments
  }
  return false;
}

export async function getStorageQuotaEstimate() {
  try {
    if (typeof navigator !== "undefined" && navigator.storage?.estimate) {
      const est = await navigator.storage.estimate();
      return {
        usage: est.usage || 0,
        quota: est.quota || 0,
        persisted: navigator.storage.persisted ? await navigator.storage.persisted() : false
      };
    }
  } catch {
    // Ignore
  }
  return { usage: 0, quota: 0, persisted: false };
}

// PIN SHA-256 + random salt helpers via Web Crypto API
export function generatePinSalt() {
  const bytes = new Uint8Array(12);
  if (typeof crypto !== "undefined" && crypto.getRandomValues) {
    crypto.getRandomValues(bytes);
  } else {
    for (let i = 0; i < bytes.length; i++) bytes[i] = Math.floor(Math.random() * 256);
  }
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

export async function hashPinWeb(pin, salt) {
  const text = `${salt}:${String(pin || "").trim()}`;
  const data = new TextEncoder().encode(text);
  if (typeof crypto !== "undefined" && crypto.subtle) {
    const buf = await crypto.subtle.digest("SHA-256", data);
    return Array.from(new Uint8Array(buf), (b) => b.toString(16).padStart(2, "0")).join("");
  }
  // Deterministic fallback for restricted non-secure test contexts
  let hash = 0;
  for (let i = 0; i < text.length; i++) hash = ((hash << 5) - hash + text.charCodeAt(i)) | 0;
  return `fb_${Math.abs(hash).toString(16)}`;
}

export async function verifyPinWeb(pin, pinHash, pinSalt) {
  if (!pinHash || !pinSalt) return false;
  const candidate = await hashPinWeb(pin, pinSalt);
  return candidate === pinHash;
}

export async function exportWhatsAppConfigJson() {
  const state = await loadAllLocalState();
  const p = state.preferences;
  return JSON.stringify(
    {
      app: "HISABATI_WHATSAPP_CONFIG",
      version: 1,
      whatsappSettings: {
        style: p.whatsappStyle,
        includeWorkspaceHeader: p.whatsappIncludeWorkspaceHeader,
        includeFinancialBreakdown: p.whatsappIncludeFinancialBreakdown,
        includeDates: p.whatsappIncludeDates,
        includeReasonAndNotes: p.whatsappIncludeReasonAndNotes,
        includeFooterSignature: p.whatsappIncludeFooterSignature,
        customSignature: p.whatsappCustomSignature
      },
      templates: state.templates.map((t) => ({
        id: t.id,
        type: t.type,
        titleAr: t.titleAr,
        bodyTemplate: t.bodyTemplate,
        channel: t.channel
      }))
    },
    null,
    2
  );
}

export async function importWhatsAppConfigJson(jsonText, userId = "local_device") {
  const parsed = JSON.parse(jsonText);
  if (!parsed || parsed.app !== "HISABATI_WHATSAPP_CONFIG") {
    throw new Error("ملف إعدادات واتساب غير صالح");
  }
  const state = await loadAllLocalState();
  const ws = parsed.whatsappSettings;
  let updatedPrefs = state.preferences;
  if (ws) {
    updatedPrefs = {
      ...state.preferences,
      whatsappStyle: ws.style || state.preferences.whatsappStyle,
      whatsappIncludeWorkspaceHeader: ws.includeWorkspaceHeader ?? state.preferences.whatsappIncludeWorkspaceHeader,
      whatsappIncludeFinancialBreakdown: ws.includeFinancialBreakdown ?? state.preferences.whatsappIncludeFinancialBreakdown,
      whatsappIncludeDates: ws.includeDates ?? state.preferences.whatsappIncludeDates,
      whatsappIncludeReasonAndNotes: ws.includeReasonAndNotes ?? state.preferences.whatsappIncludeReasonAndNotes,
      whatsappIncludeFooterSignature: ws.includeFooterSignature ?? state.preferences.whatsappIncludeFooterSignature,
      whatsappCustomSignature: ws.customSignature || state.preferences.whatsappCustomSignature
    };
    await savePreferencesLocal(updatedPrefs);
  }
  const now = Date.now();
  const importedTemplates = [];
  if (Array.isArray(parsed.templates)) {
    for (const t of parsed.templates) {
      if (!t?.id || !t?.titleAr || !t?.bodyTemplate) continue;
      const tpl = {
        id: String(t.id),
        userId,
        type: String(t.type || "CUSTOM"),
        titleAr: String(t.titleAr),
        bodyTemplate: String(t.bodyTemplate),
        channel: String(t.channel || "WHATSAPP"),
        updatedAtMillis: now
      };
      await saveTemplateLocal(tpl);
      importedTemplates.push(tpl);
    }
  }
  return { updatedPrefs, importedTemplates };
}

export async function exportFullBackupJson() {
  const state = await loadAllLocalState();
  return JSON.stringify(
    {
      app: "HISABATI",
      pwaSource: "HISABATI_PWA",
      version: 1,
      exportedAt: new Date().toISOString(),
      exportedAtMillis: Date.now(),
      contacts: state.contacts,
      debts: state.debts,
      payments: state.payments,
      installments: state.installments,
      currencies: state.currencies,
      reminders: state.reminders,
      notifications: state.notifications,
      templates: state.templates,
      auditLogs: state.auditLogs,
      members: state.members,
      preferences: state.preferences
    },
    null,
    2
  );
}

export async function importFullBackupJson(jsonText) {
  const parsed = JSON.parse(jsonText);
  const validApp = parsed?.app === "HISABATI" || parsed?.app === "HISABATI_PWA";
  if (!parsed || !validApp || !Array.isArray(parsed.contacts) || !Array.isArray(parsed.debts)) {
    throw new Error("ملف النسخة الاحتياطية غير صالح لتطبيق حساباتي");
  }
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(
      [
        "contacts",
        "debts",
        "payments",
        "installments",
        "currencies",
        "reminders",
        "notifications",
        "templates",
        "auditLogs",
        "members",
        "preferences"
      ],
      "readwrite"
    );
    for (const c of parsed.contacts) tx.objectStore("contacts").put(c);
    for (const d of parsed.debts) tx.objectStore("debts").put(d);
    for (const p of parsed.payments || []) tx.objectStore("payments").put(p);
    for (const ins of parsed.installments || []) tx.objectStore("installments").put(ins);
    for (const cur of parsed.currencies || []) tx.objectStore("currencies").put(cur);
    for (const rem of parsed.reminders || []) tx.objectStore("reminders").put(rem);
    for (const ntf of parsed.notifications || []) tx.objectStore("notifications").put(ntf);
    for (const tpl of parsed.templates || []) tx.objectStore("templates").put(tpl);
    for (const aud of parsed.auditLogs || []) tx.objectStore("auditLogs").put(aud);
    for (const mem of parsed.members || []) tx.objectStore("members").put(mem);
    if (parsed.preferences) {
      tx.objectStore("preferences").put({ ...DEFAULT_PREFS, ...parsed.preferences, id: "user_prefs" });
    }
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}
