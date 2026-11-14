const DB_NAME = "hisabati_pwa_db";
const DB_VERSION = 1;

export const DEFAULT_PREFS = {
  id: "user_prefs",
  themeMode: "LIGHT",
  themePalette: "royal",
  defaultCurrency: "YER",
  workspaceName: "حسابي",
  whatsappStyle: "FORMAL",
  whatsappIncludeWorkspaceHeader: true,
  whatsappIncludeFinancialBreakdown: true,
  whatsappIncludeDates: true,
  whatsappIncludeReasonAndNotes: true,
  whatsappIncludeFooterSignature: true,
  whatsappCustomSignature: "شكراً لتعاملكم الكريم • حساباتي",
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
  printDebtColorHex: "#0B1B32",
  printReceiptColorHex: "#065F46",
  printStatementColorHex: "#1E3A8A",
  printReportColorHex: "#115E59"
};

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains("contacts")) db.createObjectStore("contacts", { keyPath: "id" });
      if (!db.objectStoreNames.contains("debts")) db.createObjectStore("debts", { keyPath: "id" });
      if (!db.objectStoreNames.contains("payments")) db.createObjectStore("payments", { keyPath: "id" });
      if (!db.objectStoreNames.contains("outbox")) db.createObjectStore("outbox", { keyPath: "id" });
      if (!db.objectStoreNames.contains("preferences")) db.createObjectStore("preferences", { keyPath: "id" });
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
  const [contacts, debts, payments, outbox, prefsArr] = await Promise.all([
    getAll("contacts"),
    getAll("debts"),
    getAll("payments"),
    getAll("outbox"),
    getAll("preferences")
  ]);
  const prefs = { ...DEFAULT_PREFS, ...(prefsArr[0] || {}) };
  return {
    contacts: contacts.sort((a, b) => a.fullName.localeCompare(b.fullName, "ar")),
    debts: debts.sort((a, b) => (b.recordedAtMillis || 0) - (a.recordedAtMillis || 0)),
    payments: payments.sort((a, b) => (b.paidAtMillis || 0) - (a.paidAtMillis || 0)),
    outbox,
    preferences: prefs
  };
}

export async function saveContactLocal(contact) {
  return putItem("contacts", contact);
}

export async function saveDebtLocal(debt) {
  return putItem("debts", debt);
}

export async function savePaymentAndDebtLocal(payment, updatedDebt) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(["payments", "debts"], "readwrite");
    tx.objectStore("payments").put(payment);
    tx.objectStore("debts").put(updatedDebt);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function queueOutboxLocal(entry) {
  return putItem("outbox", entry);
}

export async function clearOutboxItemLocal(id) {
  return deleteItem("outbox", id);
}

export async function savePreferencesLocal(prefs) {
  return putItem("preferences", { ...DEFAULT_PREFS, ...prefs, id: "user_prefs" });
}

export async function exportFullBackupJson() {
  const state = await loadAllLocalState();
  return JSON.stringify(
    {
      app: "HISABATI_PWA",
      version: 1,
      exportedAt: new Date().toISOString(),
      contacts: state.contacts,
      debts: state.debts,
      payments: state.payments,
      preferences: state.preferences
    },
    null,
    2
  );
}

export async function importFullBackupJson(jsonText) {
  const parsed = JSON.parse(jsonText);
  if (!parsed || !Array.isArray(parsed.contacts) || !Array.isArray(parsed.debts)) {
    throw new Error("ملف النسخة الاحتياطية غير صالح");
  }
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(["contacts", "debts", "payments", "preferences"], "readwrite");
    for (const c of parsed.contacts) tx.objectStore("contacts").put(c);
    for (const d of parsed.debts) tx.objectStore("debts").put(d);
    for (const p of parsed.payments || []) tx.objectStore("payments").put(p);
    if (parsed.preferences) {
      tx.objectStore("preferences").put({ ...DEFAULT_PREFS, ...parsed.preferences, id: "user_prefs" });
    }
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}
