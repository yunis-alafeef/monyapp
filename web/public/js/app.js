import {
  convertMinor,
  escapeHtml,
  formatDate,
  formatDateTime,
  formatMinor,
  formatSignedMinor,
  getCurrencyRateMicros,
  getDirectionLabelAr,
  getPaymentMethodLabelAr,
  getStatusLabelAr,
  mergeAllCurrencies,
  minorToPlain,
  parseMajorToMinor
} from "./money.js";
import {
  clearAllLocalData,
  clearOutboxItemLocal,
  DEFAULT_PREFS,
  deleteContactLocal,
  deleteDebtLocal,
  deleteMemberLocal,
  deleteReminderLocal,
  exportFullBackupJson,
  exportWhatsAppConfigJson,
  generatePinSalt,
  getStorageQuotaEstimate,
  hashPinWeb,
  importFullBackupJson,
  importWhatsAppConfigJson,
  loadAllLocalState,
  markAllNotificationsReadLocal,
  mergeCloudCollectionLocal,
  migrateLocalUserIdToCloud,
  queueOutboxLocal,
  requestPersistentStorage,
  saveAuditLogLocal,
  saveContactLocal,
  saveCurrencyLocal,
  saveDebtLocal,
  saveInstallmentLocal,
  saveInstallmentsLocal,
  saveMemberLocal,
  saveNotificationLocal,
  saveNotificationsBatchLocal,
  savePaymentAndDebtLocal,
  savePreferencesLocal,
  saveReminderLocal,
  saveTemplateLocal,
  verifyPinWeb
} from "./storage.js";
import {
  allocatePaymentToInstallments,
  buildInstallmentSchedule,
  dispatchSystemNotification,
  evaluateDueNotifications,
  getNotificationPermissionStatus,
  isIosDevice,
  isStandalonePwa,
  requestNotificationPermission
} from "./notifications.js";
import {
  buildMonthlySeries,
  renderMonthlyCashflowChartHtml,
  renderReceivablePayableDonutHtml
} from "./charts.js";
import { AI_QUICK_PROMPTS, answerFinancialQuestionLocally } from "./ai.js";
import {
  buildFormattedWhatsAppMessage,
  openSmsComposer,
  openWhatsAppShare,
  renderTemplateText
} from "./whatsapp.js";
import {
  buildLiveInvoicePreviewHtml,
  PDF_COLOR_SWATCHES,
  PDF_DOC_KINDS,
  PDF_TEMPLATES,
  printInvoiceSheet
} from "./print.js";
import {
  deleteAllCloudUserData,
  deleteCurrentAuthUserSafely,
  ensureCloudUserProfile,
  handleAuthRedirectResult,
  initFirebaseSafely,
  pullCloudSnapshot,
  pushOutboxToCloud,
  sendPasswordResetCloud,
  sendViaBackendRelay,
  signInWithEmailCloud,
  signInWithGoogleCloud,
  signOutCloud,
  signUpWithEmailCloud,
  subscribeAuthState,
  subscribeCloudRealtime
} from "./cloud.js";
import {
  buildContactStatementData,
  buildReportDocument,
  downloadCsvFile,
  getActiveMemberPermissions,
  renderReportsPage,
  renderStatementPage
} from "./reports.js";
import {
  renderAccountAndSecurityPage,
  renderAuthPage,
  renderHelpPage,
  renderOnboardingOverlay,
  renderPinLockOverlay,
  renderSettingsPageFull,
  renderWhatsAppAndSmsPage,
  renderWorkspacePage
} from "./pages_admin.js";

// Silent Service Worker Registration in App Entry Point
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./sw.js", { scope: "./" }).catch(() => {});
  });
  navigator.serviceWorker.addEventListener("message", (event) => {
    if (event.data?.type === "HISABATI_OPEN_NOTIFICATIONS") {
      state.tab = "notifications";
      render();
    }
  });
}

const ICONS = {
  home: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/></svg>`,
  wallet: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M21 7H5c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V9c0-1.1-.9-2-2-2zm-1 9c-.83 0-1.5-.67-1.5-1.5S19.17 13 20 13s1.5.67 1.5 1.5S20.83 16 20 16zM4 5h14V3H4c-1.1 0-2 .9-2 2v12h2V5z"/></svg>`,
  groups: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z"/></svg>`,
  grid: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M4 4h7v7H4V4zm9 0h7v7h-7V4zM4 13h7v7H4v-7zm9 0h7v7h-7v-7z"/></svg>`,
  palette: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3a9 9 0 0 0 0 18c.83 0 1.5-.67 1.5-1.5 0-.39-.15-.74-.39-1.01-.23-.26-.38-.61-.38-.99 0-.83.67-1.5 1.5-1.5H16c2.76 0 5-2.24 5-5 0-4.42-4.03-8-9-8zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 9 6.5 9 8 9.67 8 10.5 7.33 12 6.5 12zm3-4C8.67 8 8 7.33 8 6.5S8.67 5 9.5 5s1.5.67 1.5 1.5S10.33 8 9.5 8zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 5 14.5 5s1.5.67 1.5 1.5S15.33 8 14.5 8zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 9 17.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/></svg>`,
  cloudOk: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM10 17l-3.5-3.5 1.41-1.41L10 14.17 15.18 9l1.41 1.41L10 17z"/></svg>`,
  cloudWait: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/></svg>`,
  search: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"/></svg>`,
  bell: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 22c1.1 0 2-.9 2-2h-4a2 2 0 0 0 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5S10.5 3.17 10.5 4v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"/></svg>`,
  add: `<svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor"><path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>`,
  pay: `<svg width="17" height="17" viewBox="0 0 24 24" fill="currentColor"><path d="M19 14V6c0-1.1-.9-2-2-2H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zm-9-1c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm13-6v11c0 1.1-.9 2-2 2H4v-2h17V7h2z"/></svg>`,
  warn: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>`,
  clock: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/></svg>`,
  receipt: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M19.5 3.5 18 2l-1.5 1.5L15 2l-1.5 1.5L12 2l-1.5 1.5L9 2 7.5 3.5 6 2v14H3v3c0 1.66 1.34 3 3 3h12c1.66 0 3-1.34 3-3V2l-1.5 1.5zM15 11H9V9h6v2zm0 4H9v-2h6v2z"/></svg>`,
  print: `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M19 8H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zm-3 11H8v-5h8v5zm3-7c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1zm-1-9H6v4h12V3z"/></svg>`,
  chat: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>`,
  phone: `<svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor"><path d="M6.62 10.79a15.05 15.05 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.01-.24c1.12.37 2.33.57 3.58.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1C10.61 21 3 13.39 3 4c0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.46.57 3.58.11.35.03.74-.25 1.02l-2.2 2.19z"/></svg>`,
  verified: `<svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg>`,
  sparkle: `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="m19 9 1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z"/></svg>`,
  send: `<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M2.01 21 23 12 2.01 3 2 10l15 2-15 2z"/></svg>`,
  shield: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>`,
  report: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z"/></svg>`
};

const state = {
  tab: "dashboard", // dashboard | debts | contacts | more | ai | notifications | reminders | whatsapp | settings | search | reports | statement | auth | account | workspace | help
  debtFilter: "ALL",
  debtCurrencyFilter: "ALL",
  contactFilter: "ALL",
  contactSort: "BALANCE", // BALANCE | NAME
  searchQuery: "",
  reportKind: "POSITION",
  statementContactId: "",
  statementPeriodDays: 0,
  editingTemplateId: "tpl_due_soon",
  authMode: "signin",
  onboardingSlide: 0,
  isPinLocked: false,
  pinError: "",
  pendingAvatarDataUrl: "",
  pendingDebtAttachmentUrl: "",
  contacts: [],
  debts: [],
  payments: [],
  installments: [],
  currencies: [],
  reminders: [],
  notifications: [],
  templates: [],
  auditLogs: [],
  members: [],
  outbox: [],
  aiMessages: [],
  aiPendingQuestion: "",
  preferences: { ...DEFAULT_PREFS },
  cloudUser: null,
  storageQuota: { usage: 0, quota: 0, persisted: false },
  modal: null,
  isSaving: false,
  toast: null
};

let realtimeUnsub = null;
let toastTimer = null;

function showToast(msg) {
  state.toast = msg;
  render();
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    state.toast = null;
    render();
  }, 3200);
}

function uid() {
  return (crypto?.randomUUID?.() || `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`).replace(/-/g, "");
}

function activeUid() {
  return state.cloudUser?.uid || "local_device";
}

function resolveDarkMode() {
  const mode = state.preferences.themeMode || "SYSTEM";
  if (mode === "DARK") return true;
  if (mode === "LIGHT") return false;
  return Boolean(window.matchMedia?.("(prefers-color-scheme: dark)")?.matches);
}

function applyThemeAttributes() {
  const root = document.documentElement;
  const isDark = resolveDarkMode();
  root.setAttribute("data-theme", isDark ? "dark" : "light");
  const palette = state.preferences.themePalette || "royal";
  root.setAttribute("data-palette", palette);
  if (palette === "custom") {
    const pHex = state.preferences.customPrimaryHex || "#0B1B32";
    const aHex = state.preferences.customAccentHex || "#10B981";
    root.style.setProperty("--primary", pHex);
    root.style.setProperty("--accent", aHex);
    root.style.setProperty("--hero-start", pHex);
    root.style.setProperty("--hero-mid", pHex);
    root.style.setProperty("--hero-end", aHex);
  } else {
    root.style.removeProperty("--primary");
    root.style.removeProperty("--accent");
    root.style.removeProperty("--hero-start");
    root.style.removeProperty("--hero-mid");
    root.style.removeProperty("--hero-end");
  }
}

async function compressImageFile(file, maxSize = 420) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("تعذر قراءة الصورة"));
    reader.onload = () => {
      const img = new Image();
      img.onerror = () => resolve(String(reader.result || ""));
      img.onload = () => {
        const scale = Math.min(1, maxSize / Math.max(img.width, img.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.max(1, Math.round(img.width * scale));
        canvas.height = Math.max(1, Math.round(img.height * scale));
        const ctx = canvas.getContext("2d");
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL("image/jpeg", 0.72));
      };
      img.src = String(reader.result || "");
    };
    reader.readAsDataURL(file);
  });
}

async function recordAudit(action, entityType, entityId, details, reason = null) {
  const perms = getActiveMemberPermissions(state);
  const log = {
    id: `aud_${uid()}`,
    userId: activeUid(),
    actorName: state.cloudUser?.displayName || perms.member?.displayName || "صاحب الحساب",
    action,
    entityType,
    entityId,
    details,
    reason: reason || null,
    createdAtMillis: Date.now()
  };
  await saveAuditLogLocal(log);
  await queueOutboxLocal({ id: `ob_${uid()}`, collection: "audit_logs", docId: log.id, operation: "CREATE", payload: log });
}

async function emitNotification(title, body, category, relatedDebtId = null) {
  const notif = {
    id: `ntf_${uid()}`,
    userId: activeUid(),
    title,
    body,
    category,
    relatedDebtId,
    isRead: false,
    createdAtMillis: Date.now()
  };
  await saveNotificationLocal(notif);
  await queueOutboxLocal({ id: `ob_${uid()}`, collection: "notification_logs", docId: notif.id, operation: "UPSERT", payload: notif });
}

function computeMetrics() {
  const cur = state.preferences.defaultCurrency || "YER";
  const currencies = state.currencies || [];
  const now = Date.now();
  const soonBoundary = now + 7 * 86_400_000;
  const debtMap = new Map(state.debts.map((d) => [d.id, d]));

  let totalReceivable = 0;
  let totalPayable = 0;
  let overdueCount = 0;
  let overdueAmount = 0;
  let dueSoonCount = 0;
  let dueSoonAmount = 0;

  for (const d of state.debts) {
    const conv = convertMinor(d.remainingMinor || 0, d.currencyCode || cur, cur, currencies);
    if (d.direction === "RECEIVABLE") totalReceivable += conv;
    else totalPayable += conv;

    const isOpen = (d.remainingMinor || 0) > 0 && d.status !== "PAID" && d.status !== "SETTLED";
    if (isOpen) {
      if (d.dueAtMillis < now) {
        overdueCount++;
        overdueAmount += conv;
      } else if (d.dueAtMillis <= soonBoundary) {
        dueSoonCount++;
        dueSoonAmount += conv;
      }
    }
  }

  let totalCollected = 0;
  let totalPaidOut = 0;
  for (const p of state.payments) {
    const conv = convertMinor(p.amountMinor || 0, p.currencyCode || cur, cur, currencies);
    const parent = debtMap.get(p.debtId);
    if (parent?.direction === "PAYABLE") totalPaidOut += conv;
    else totalCollected += conv;
  }

  return {
    currency: cur,
    totalReceivable,
    totalPayable,
    netPosition: totalReceivable - totalPayable,
    totalCollected,
    totalPaidOut,
    overdueCount,
    overdueAmount,
    dueSoonCount,
    dueSoonAmount,
    contactsCount: state.contacts.length,
    transactionsCount: state.debts.length + state.payments.length,
    monthlySeries: buildMonthlySeries(state.debts, state.payments, cur, now, currencies)
  };
}

async function runDueNotificationCheck({ dispatchOs = true, manualFeedback = false } = {}) {
  const result = evaluateDueNotifications({
    debts: state.debts,
    installments: state.installments,
    reminders: state.reminders,
    existingNotifications: state.notifications,
    currencies: state.currencies,
    prefs: state.preferences,
    nowMillis: Date.now()
  });

  if (result.updatedInstallments.length) {
    await saveInstallmentsLocal(result.updatedInstallments);
    for (const ins of result.updatedInstallments) {
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "installments", docId: ins.id, operation: "UPSERT", payload: ins });
    }
  }
  for (const rem of result.updatedReminders) {
    await saveReminderLocal(rem);
    await queueOutboxLocal({ id: `ob_${uid()}`, collection: "reminders", docId: rem.id, operation: "UPSERT", payload: rem });
  }
  if (result.newNotifications.length) {
    const tagged = result.newNotifications.map((n) => ({ ...n, userId: activeUid() }));
    await saveNotificationsBatchLocal(tagged);
    for (const n of tagged) {
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "notification_logs", docId: n.id, operation: "UPSERT", payload: n });
    }
    if (dispatchOs) {
      for (const n of tagged.slice(0, 3)) {
        await dispatchSystemNotification({
          title: n.title,
          body: n.body,
          tag: n.id,
          data: { debtId: n.relatedDebtId }
        });
      }
    }
  }

  if (result.newNotifications.length || result.updatedInstallments.length || result.updatedReminders.length) {
    Object.assign(state, await loadAllLocalState());
    render();
  }

  if (manualFeedback) {
    showToast(
      result.newNotifications.length
        ? `تم فحص المستحقات وإصدار ${result.newNotifications.length} تنبيه`
        : "تم الفحص: لا توجد أقساط أو ديون جديدة مستحقة"
    );
  }
  return result.newNotifications.length;
}

function runAiPrompt(question) {
  const q = String(question || "").trim();
  if (!q) return;
  const answer = answerFinancialQuestionLocally({ question: q, state, nowMillis: Date.now() });
  state.aiMessages = [
    ...state.aiMessages,
    { id: `u_${uid()}`, isUser: true, text: q },
    { id: `a_${uid()}`, isUser: false, text: answer }
  ];
  render();
}

function handleAiSubmission(question) {
  const q = String(question || "").trim();
  if (!q) return;
  if (!state.preferences.aiConsentGranted) {
    state.aiPendingQuestion = q;
    render();
    return;
  }
  state.aiPendingQuestion = "";
  runAiPrompt(q);
}

function renderTopBar() {
  const unread = state.notifications.filter((n) => !n.isRead).length;
  const perms = getActiveMemberPermissions(state);
  const userName = state.cloudUser?.displayName || perms.member?.displayName || "بك";
  const accountLabel = state.preferences.accountType === "BUSINESS" ? "تجاري" : "شخصي";
  const workspace = state.preferences.printCompanyName || state.preferences.workspaceName || "محفظتي الشخصية";
  const pendingCount = state.outbox.length;

  return `
    <header class="top-bar">
      <div class="brand-group card-clickable" data-action="go-tab" data-val="auth" title="إدارة الحساب والمزامنة">
        <img src="./icons/icon-192.png" alt="شعار حساباتي" class="brand-logo" />
        <div style="min-width:0;">
          <h1 class="brand-title">مرحباً، ${escapeHtml(userName)}</h1>
          <p class="brand-sub">
            ${escapeHtml(workspace)} • ${accountLabel} •
            ${state.cloudUser ? (navigator.onLine ? "سحابي متزامن" : "سحابي (أوفلاين)") : "حساب محلي"}
          </p>
        </div>
      </div>
      <div class="top-actions">
        <button class="icon-btn" data-action="go-tab" data-val="settings" title="الثيمات والإعدادات" style="color:var(--primary);">${ICONS.palette}</button>
        <button class="icon-btn" data-action="${state.cloudUser ? "sync-cloud" : "go-tab"}" ${!state.cloudUser ? 'data-val="auth"' : ""} title="${state.cloudUser ? "مزامنة السحابة" : "تسجيل الدخول السحابي"}" style="color:${state.cloudUser ? (pendingCount > 0 ? "var(--warning)" : "var(--positive)") : "var(--muted)"};">
          ${pendingCount > 0 || !state.cloudUser ? ICONS.cloudWait : ICONS.cloudOk}
        </button>
        <button class="icon-btn" data-action="go-tab" data-val="search" title="بحث ذكي">${ICONS.search}</button>
        <button class="icon-btn" data-action="go-tab" data-val="notifications" title="الإشعارات والتنبيهات">
          ${ICONS.bell}
          ${unread > 0 ? `<span class="notif-badge">${unread > 99 ? "99+" : unread}</span>` : ""}
        </button>
      </div>
    </header>
  `;
}

function renderDashboard() {
  const m = computeMetrics();
  const currencies = state.currencies || [];
  const now = Date.now();
  const upcomingDebts = state.debts
    .filter((d) => (d.remainingMinor || 0) > 0 && d.status !== "PAID" && d.status !== "SETTLED")
    .sort((a, b) => (a.dueAtMillis || 0) - (b.dueAtMillis || 0))
    .slice(0, 5);
  const recentPayments = state.payments.slice(0, 4);
  const overdueInstCount = state.installments.filter(
    (ins) => (ins.amountMinor - ins.paidMinor) > 0 && ins.status !== "PAID" && ins.dueAtMillis <= now + 48 * 3600_000
  ).length;
  const iosNotInstalledBanner = isIosDevice() && !isStandalonePwa();

  return `
    <section class="hero-card">
      <div class="row-between">
        <span class="hero-label">صافي المركز المالي</span>
        <button class="currency-pill" data-action="open-currency-picker">C • ${escapeHtml(m.currency)}</button>
      </div>
      <div class="hero-value" style="color:${m.netPosition >= 0 ? "#6EE7B7" : "#FFA5B4"};">
        ${formatSignedMinor(m.netPosition, m.currency, currencies)}
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <div class="hero-label">↓ مستحق لي</div>
          <div style="font-weight:800;font-size:14.5px;">${formatMinor(m.totalReceivable, m.currency, currencies)}</div>
        </div>
        <div class="hero-stat">
          <div class="hero-label">↑ مستحق عليّ</div>
          <div style="font-weight:800;font-size:14.5px;">${formatMinor(m.totalPayable, m.currency, currencies)}</div>
        </div>
      </div>
      <div class="hero-actions">
        <button class="btn btn-hero-emerald" data-action="open-add-debt">${ICONS.add}<span>إضافة دين</span></button>
        <button class="btn btn-hero-glass" data-action="quick-payment">${ICONS.pay}<span>تسجيل دفعة</span></button>
      </div>
    </section>

    ${!state.cloudUser ? `
      <div class="card card-clickable" data-action="go-tab" data-val="auth" style="border-color:rgba(59,130,246,0.4);">
        <div class="row-between">
          <div>
            <strong style="font-size:13px;">الربط السحابي وحماية بياناتك (اختياري)</strong>
            <div class="text-muted">بياناتك الآن محفوظة محلياً داخل المتصفح. سجّل دخولك لمزامنتها وحفظها في السحابة.</div>
          </div>
          <span class="chip active">دخول</span>
        </div>
      </div>
    ` : ""}

    <div class="card ai-banner-card card-clickable" data-action="go-tab" data-val="ai">
      <span class="tile-icon-box" style="background:rgba(16,185,129,0.16);color:var(--accent);width:36px;height:36px;">
        ${ICONS.sparkle}
      </span>
      <div style="min-width:0;flex:1;">
        <strong style="font-size:13.5px;display:block;">المساعد المالي الذكي</strong>
        <span class="text-muted">تحليل المتأخرات، كبار العملاء، وملخص السيولة الفوري</span>
      </div>
      <span class="chip active">اسأل الآن</span>
    </div>

    ${overdueInstCount > 0 || m.overdueCount > 0 ? `
      <div class="alert-banner card-clickable" data-action="go-tab" data-val="notifications">
        <div style="display:flex;align-items:center;gap:8px;">
          <span style="color:var(--warning);display:inline-flex;">${ICONS.warn}</span>
          <div>
            <div style="font-weight:800;font-size:12.5px;">تنبيهات السداد والتحصيل المحلية</div>
            <div class="text-muted">${m.overdueCount} دين متأخر • ${overdueInstCount} قسط مستحق خلال 48 ساعة</div>
          </div>
        </div>
        <span style="font-weight:800;color:var(--warning);font-size:12px;">عرض</span>
      </div>
    ` : ""}

    ${iosNotInstalledBanner ? `
      <div class="card card-clickable" data-action="open-ios-guide" style="border-color:rgba(16,185,129,0.4);">
        <div class="row-between">
          <div>
            <strong style="font-size:13px;">تثبيت حساباتي على الآيفون بدون إنترنت</strong>
            <div class="text-muted">اضغط مشاركة ⬆ ثم «إضافة إلى الشاشة الرئيسية» لتفعيل وضع التطبيق والإشعارات.</div>
          </div>
          <span class="chip active">الخطوات</span>
        </div>
      </div>
    ` : ""}

    <div class="kpi-grid">
      <div class="kpi-tile">
        <div class="kpi-top"><span class="tile-icon-box" style="background:rgba(14,159,110,0.14);color:var(--positive);">${ICONS.wallet}</span></div>
        <div class="kpi-title">إجمالي المحصّل</div>
        <div class="kpi-val">${formatMinor(m.totalCollected, m.currency, currencies)}</div>
      </div>
      <div class="kpi-tile">
        <div class="kpi-top"><span class="tile-icon-box" style="background:rgba(59,130,246,0.14);color:var(--info);">${ICONS.pay}</span></div>
        <div class="kpi-title">إجمالي المسدّد</div>
        <div class="kpi-val">${formatMinor(m.totalPaidOut, m.currency, currencies)}</div>
      </div>
      <div class="kpi-tile card-clickable" data-action="open-overdue-debts">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(225,29,72,0.14);color:var(--negative);">${ICONS.warn}</span>
          <span class="kpi-sub">${m.overdueCount} دين</span>
        </div>
        <div class="kpi-title">الديون المتأخرة</div>
        <div class="kpi-val text-neg">${formatMinor(m.overdueAmount, m.currency, currencies)}</div>
      </div>
      <div class="kpi-tile">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(217,119,6,0.14);color:var(--warning);">${ICONS.clock}</span>
          <span class="kpi-sub">${m.dueSoonCount} خلال 7 أيام</span>
        </div>
        <div class="kpi-title">اقترب استحقاقها</div>
        <div class="kpi-val">${formatMinor(m.dueSoonAmount, m.currency, currencies)}</div>
      </div>
      <div class="kpi-tile card-clickable" data-action="switch-tab" data-val="contacts">
        <div class="kpi-top"><span class="tile-icon-box" style="background:var(--primary-soft);color:var(--primary);">${ICONS.groups}</span></div>
        <div class="kpi-title">عدد الأشخاص</div>
        <div class="kpi-val">${m.contactsCount}</div>
      </div>
      <div class="kpi-tile card-clickable" data-action="switch-tab" data-val="debts">
        <div class="kpi-top"><span class="tile-icon-box" style="background:rgba(16,185,129,0.14);color:var(--accent);">${ICONS.receipt}</span></div>
        <div class="kpi-title">عدد المعاملات</div>
        <div class="kpi-val">${m.transactionsCount}</div>
      </div>
    </div>

    ${renderMonthlyCashflowChartHtml(m.monthlySeries)}
    ${renderReceivablePayableDonutHtml(m.totalReceivable, m.totalPayable, m.currency)}

    <div class="row-between" style="margin-top:4px;">
      <strong>المستحقات القادمة والمتأخرة</strong>
      <button class="btn btn-outline btn-sm" data-action="open-overdue-debts">عرض الكل</button>
    </div>

    <div class="list-stack">
      ${upcomingDebts.length ? upcomingDebts.map(renderDebtCard).join("") : `
        <div class="card text-muted" style="text-align:center;padding:20px;">
          لا توجد مستحقات مفتوحة. ابدأ بإضافة شخص ثم تسجيل دين جديد — يعمل بالكامل بدون إنترنت.
        </div>
      `}
    </div>

    <div class="row-between" style="margin-top:4px;">
      <strong>العمليات والدفعات الأخيرة</strong>
      <button class="btn btn-outline btn-sm" data-action="go-tab" data-val="reports">${ICONS.report}<span>التقارير</span></button>
    </div>

    <div class="list-stack">
      ${recentPayments.length ? recentPayments.map((p) => {
        const d = state.debts.find((x) => x.id === p.debtId);
        return `
          <div class="card card-clickable" data-action="open-receipt-detail" data-id="${p.id}">
            <div class="row-between">
              <div style="display:flex;align-items:center;gap:10px;">
                <span class="tile-icon-box" style="border-radius:50%;background:rgba(14,159,110,0.14);color:var(--positive);">${ICONS.receipt}</span>
                <div>
                  <strong>${escapeHtml(d?.contactName || "دفعة مالية")}</strong>
                  <div class="text-muted">إيصال ${escapeHtml(p.receiptNumber)} • ${formatDate(p.paidAtMillis)}</div>
                </div>
              </div>
              <strong class="text-pos">${formatMinor(p.amountMinor, p.currencyCode, currencies)}</strong>
            </div>
          </div>
        `;
      }).join("") : `
        <div class="card text-muted">لم تُسجل دفعات بعد. عند تسجيل أي دفعة سيظهر سجل الإيصال هنا فوراً.</div>
      `}
    </div>
  `;
}

function renderDebtCard(d) {
  const currencies = state.currencies || [];
  const dirAr = getDirectionLabelAr(d.direction);
  const dirColor = d.direction === "RECEIVABLE" ? "var(--positive)" : "var(--negative)";
  const now = Date.now();
  const effectiveStatus = (d.remainingMinor || 0) > 0 && d.dueAtMillis < now && d.status !== "PAID" && d.status !== "SETTLED"
    ? "OVERDUE" : d.status;
  const pct = d.principalMinor > 0
    ? Math.min(100, Math.max(0, Math.round(((d.principalMinor - d.remainingMinor) * 100) / d.principalMinor)))
    : 0;
  const instList = state.installments.filter((ins) => ins.debtId === d.id);

  return `
    <div class="card card-clickable" data-action="open-debt-detail" data-id="${d.id}">
      <div class="row-between">
        <div style="min-width:0;">
          <strong>${escapeHtml(d.contactName)}</strong>
          <div class="text-muted">${escapeHtml(d.reason)} ${instList.length > 1 ? `• ${instList.length} أقساط` : ""} ${d.attachmentUri ? "• 📎 مرفق" : ""}</div>
        </div>
        <span class="status-badge st-${effectiveStatus}">${getStatusLabelAr(effectiveStatus)}</span>
      </div>
      <div class="row-between" style="margin-top:8px;">
        <div>
          <div class="text-muted">المتبقي</div>
          <strong style="font-size:15px;color:${dirColor};">${formatMinor(d.remainingMinor, d.currencyCode, currencies)}</strong>
        </div>
        <div style="text-align:left;">
          <div style="font-size:11.5px;font-weight:700;color:${dirColor};">${dirAr}</div>
          <div class="text-muted">الاستحقاق: ${formatDate(d.dueAtMillis)}</div>
        </div>
      </div>
      <div class="progress-track">
        <div class="progress-fill" style="width:${pct}%;background:${dirColor};"></div>
      </div>
      <div style="display:flex;gap:6px;margin-top:9px;flex-wrap:wrap;" data-stop-card="1">
        ${d.remainingMinor > 0 ? `<button class="btn btn-primary btn-sm" data-action="open-pay-debt" data-id="${d.id}">تسجيل دفعة</button>` : ""}
        <button class="btn btn-outline btn-sm" data-action="send-debt-wa" data-id="${d.id}">${ICONS.chat}<span>واتساب</span></button>
        <button class="btn btn-outline btn-sm" data-action="send-debt-sms" data-id="${d.id}">SMS</button>
        <button class="btn btn-outline btn-sm" data-action="print-debt-voucher" data-id="${d.id}">${ICONS.print}<span>PDF</span></button>
      </div>
    </div>
  `;
}

function renderDebtsTab() {
  const now = Date.now();
  const q = state.searchQuery.trim().toLowerCase();
  const currencies = mergeAllCurrencies(state.currencies);
  const filtered = state.debts.filter((d) => {
    const matchTab =
      state.debtFilter === "ALL" ||
      (state.debtFilter === "RECEIVABLE" && d.direction === "RECEIVABLE") ||
      (state.debtFilter === "PAYABLE" && d.direction === "PAYABLE") ||
      (state.debtFilter === "OVERDUE" && d.remainingMinor > 0 && d.dueAtMillis < now && d.status !== "PAID" && d.status !== "SETTLED") ||
      (state.debtFilter === "INSTALLMENTS" && state.installments.some((ins) => ins.debtId === d.id)) ||
      (state.debtFilter === "SETTLED" && (d.remainingMinor === 0 || d.status === "PAID" || d.status === "SETTLED"));
    const matchCur = state.debtCurrencyFilter === "ALL" || d.currencyCode === state.debtCurrencyFilter;
    const matchSearch = !q || (d.contactName || "").toLowerCase().includes(q) || (d.reason || "").toLowerCase().includes(q) || (d.notes || "").toLowerCase().includes(q);
    return matchTab && matchCur && matchSearch;
  });

  return `
    <div class="row-between">
      <strong>سجل الديون والأقساط (${filtered.length})</strong>
      <button class="btn btn-primary btn-sm" data-action="open-add-debt">${ICONS.add}<span>دين جديد</span></button>
    </div>
    <input class="input" type="search" placeholder="ابحث باسم الشخص أو البيان أو الملاحظات..." value="${escapeHtml(state.searchQuery)}" data-input="search-query" />
    <div class="tabs-row">
      ${[["ALL", "الكل"], ["RECEIVABLE", "مستحق لي"], ["PAYABLE", "مستحق عليّ"], ["OVERDUE", "متأخر"], ["INSTALLMENTS", "مقسط"], ["SETTLED", "مسدد"]]
        .map(([k, lbl]) => `<button class="chip ${state.debtFilter === k ? "active" : ""}" data-action="set-debt-filter" data-val="${k}">${lbl}</button>`)
        .join("")}
    </div>
    <div class="tabs-row">
      <button class="chip ${state.debtCurrencyFilter === "ALL" ? "active" : ""}" data-action="set-debt-cur-filter" data-val="ALL">كل العملات</button>
      ${currencies.map((c) => `<button class="chip ${state.debtCurrencyFilter === c.code ? "active" : ""}" data-action="set-debt-cur-filter" data-val="${c.code}">${escapeHtml(c.code)}</button>`).join("")}
    </div>
    <div class="list-stack">
      ${filtered.length ? filtered.map(renderDebtCard).join("") : `<div class="card text-muted" style="text-align:center;">لا توجد قيود مطابقة</div>`}
    </div>
  `;
}

function renderContactsTab() {
  const cur = state.preferences.defaultCurrency || "YER";
  const currencies = state.currencies || [];
  const q = state.searchQuery.trim().toLowerCase();
  const now = Date.now();

  const enriched = state.contacts.map((c) => {
    const cDebts = state.debts.filter((d) => d.contactId === c.id);
    const rec = cDebts.filter((d) => d.direction === "RECEIVABLE")
      .reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode, cur, currencies), 0);
    const pay = cDebts.filter((d) => d.direction === "PAYABLE")
      .reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode, cur, currencies), 0);
    const hasOverdue = cDebts.some((d) => d.remainingMinor > 0 && d.dueAtMillis < now && d.status !== "PAID" && d.status !== "SETTLED");
    return { contact: c, rec, pay, net: rec - pay, hasOverdue };
  }).filter((row) => {
    const c = row.contact;
    const matchQ = !q || (c.fullName || "").toLowerCase().includes(q) || (c.phone || "").includes(q);
    const matchF =
      state.contactFilter === "ALL" ||
      (state.contactFilter === "REC" && row.rec > 0) ||
      (state.contactFilter === "PAY" && row.pay > 0) ||
      (state.contactFilter === "OVERDUE" && row.hasOverdue);
    return matchQ && matchF;
  });

  if (state.contactSort === "NAME") {
    enriched.sort((a, b) => (a.contact.fullName || "").localeCompare(b.contact.fullName || "", "ar"));
  } else {
    enriched.sort((a, b) => Math.abs(b.net) - Math.abs(a.net));
  }

  return `
    <div class="row-between">
      <strong>الأشخاص والعملاء (${enriched.length})</strong>
      <div style="display:flex;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="toggle-contact-sort">${state.contactSort === "NAME" ? "فرز: أبجدي" : "فرز: الرصيد"}</button>
        <button class="btn btn-primary btn-sm" data-action="open-add-contact">${ICONS.add}<span>إضافة شخص</span></button>
      </div>
    </div>
    <input class="input" type="search" placeholder="ابحث بالاسم أو رقم الهاتف..." value="${escapeHtml(state.searchQuery)}" data-input="search-query" />
    <div class="tabs-row">
      ${[["ALL", "الكل"], ["REC", "له عليك / مستحق لي"], ["PAY", "مستحق عليّ"], ["OVERDUE", "متأخرون"]]
        .map(([k, lbl]) => `<button class="chip ${state.contactFilter === k ? "active" : ""}" data-action="set-contact-filter" data-val="${k}">${lbl}</button>`)
        .join("")}
    </div>
    <div class="list-stack">
      ${enriched.length ? enriched.map(({ contact: c, rec, pay }) => `
        <div class="card card-clickable" data-action="open-contact-detail" data-id="${c.id}">
          <div class="row-between">
            <div style="display:flex;align-items:center;gap:10px;min-width:0;">
              ${c.avatarUri
                ? `<img src="${escapeHtml(c.avatarUri)}" class="avatar-circle" alt="${escapeHtml(c.fullName)}" />`
                : `<span class="avatar-circle">${escapeHtml((c.fullName || "ع").slice(0, 1))}</span>`}
              <div style="min-width:0;">
                <strong>${escapeHtml(c.fullName)}</strong>
                <div class="text-muted">${escapeHtml(c.phone || "بدون هاتف")} ${c.address ? `• ${escapeHtml(c.address)}` : ""}</div>
              </div>
            </div>
            <div style="display:flex;gap:6px;" data-stop-card="1">
              <button class="btn btn-outline btn-sm" data-action="open-add-debt" data-id="${c.id}">+ دين</button>
              <button class="btn btn-outline btn-sm" data-action="open-statement" data-id="${c.id}">كشف حساب</button>
            </div>
          </div>
          <div class="row-between" style="margin-top:8px;">
            <span class="text-pos" style="font-weight:700;">مستحق لي: ${formatMinor(rec, cur, currencies)}</span>
            <span class="text-neg" style="font-weight:700;">مستحق عليّ: ${formatMinor(pay, cur, currencies)}</span>
          </div>
        </div>
      `).join("") : `<div class="card text-muted" style="text-align:center;padding:20px;">لا توجد جهات مطابقة.</div>`}
    </div>
  `;
}

function renderMoreHub() {
  const unread = state.notifications.filter((n) => !n.isRead).length;
  const items = [
    { tab: "auth", title: state.cloudUser ? "الحساب المتصل ✓" : "تسجيل الدخول السحابي", sub: state.cloudUser ? state.cloudUser.email : "مزامنة Firebase والنسخ", color: "var(--info)", icon: ICONS.cloudOk },
    { tab: "ai", title: "المساعد الذكي", sub: "تحليلات وإجابات فورية", color: "var(--accent)", icon: ICONS.sparkle },
    { tab: "reports", title: "التقارير و Excel CSV", sub: "6 تقارير + طباعة PDF", color: "var(--primary)", icon: ICONS.report },
    { tab: "statement", title: "كشف حساب شخص", sub: "كشف تفصيلي متعدد العملات", color: "var(--positive)", icon: ICONS.receipt },
    { tab: "notifications", title: `الإشعارات (${unread})`, sub: "تنبيهات الأقساط والديون", color: "var(--warning)", icon: ICONS.bell },
    { tab: "reminders", title: "التذكيرات المحلية", sub: "جدولة وإعداد الإشعارات", color: "var(--negative)", icon: ICONS.clock },
    { action: "open-print-settings", title: "الطباعة والفواتير PDF", sub: "قوالب والشعار والمعاينة", color: "var(--accent)", icon: ICONS.print },
    { tab: "whatsapp", title: "واتساب والقوالب و SMS", sub: "5 قوالب + خادم الإرسال", color: "var(--positive)", icon: ICONS.chat },
    { tab: "settings", title: "الثيمات والعملات", sub: "ثيم مخصص + عملات متعددة", color: "var(--primary)", icon: ICONS.palette },
    { tab: "workspace", title: "فريق العمل (RBAC)", sub: "الأعضاء والصلاحيات", color: "var(--info)", icon: ICONS.groups },
    { tab: "account", title: "الأمان و PIN والتدقيق", sub: "رمز القفل + سجل التدقيق", color: "var(--negative)", icon: ICONS.shield },
    { tab: "help", title: "المساعدة والدعم", sub: "دليل التخزين و iPhone PWA", color: "var(--muted)", icon: ICONS.verified }
  ];

  return `
    <div class="row-between">
      <strong>الأدوات والإعدادات المتقدمة</strong>
    </div>
    <div class="hub-grid">
      ${items.map((it) => `
        <div class="card hub-card card-clickable" ${it.tab ? `data-action="go-tab" data-val="${it.tab}"` : `data-action="${it.action}"`}>
          <span class="tile-icon-box" style="background:rgba(16,185,129,0.12);color:${it.color};">${it.icon}</span>
          <strong style="margin-top:4px;font-size:13.5px;">${escapeHtml(it.title)}</strong>
          <span class="text-muted">${escapeHtml(it.sub)}</span>
        </div>
      `).join("")}
    </div>
  `;
}

function renderNotificationsPage() {
  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="dashboard">رجوع</button>
        <strong>الإشعارات والتنبيهات (${state.notifications.length})</strong>
      </div>
      <div style="display:flex;gap:6px;">
        <button class="btn btn-primary btn-sm" data-action="check-due-now">فحص الآن</button>
        <button class="btn btn-outline btn-sm" data-action="mark-all-read">مقروء</button>
        <button class="btn btn-outline btn-sm" data-action="go-tab" data-val="reminders">الإعدادات</button>
      </div>
    </div>
    <div class="list-stack">
      ${state.notifications.length ? state.notifications.map((n) => `
        <div class="card ${n.relatedDebtId ? "card-clickable" : ""}" ${n.relatedDebtId ? `data-action="open-debt-detail" data-id="${n.relatedDebtId}"` : ""}>
          <div class="row-between">
            <strong style="color:${n.isRead ? "var(--text)" : "var(--positive)"};">${escapeHtml(n.title)}</strong>
            <span class="text-muted">${formatDate(n.createdAtMillis)}</span>
          </div>
          <div class="text-muted" style="margin-top:4px;">${escapeHtml(n.body)}</div>
        </div>
      `).join("") : `
        <div class="card" style="text-align:center;padding:22px;">
          <strong>لا توجد إشعارات بعد</strong>
          <p class="text-muted">اضغط «فحص الآن» لفحص الأقساط والديون المتأخرة أو القريبة من الاستحقاق.</p>
          <button class="btn btn-primary btn-sm" data-action="check-due-now">فحص المستحقات الآن</button>
        </div>
      `}
    </div>
  `;
}

function renderRemindersPage() {
  const perm = getNotificationPermissionStatus();
  const ios = isIosDevice();
  const standalone = isStandalonePwa();
  const p = state.preferences;
  const permAr =
    perm === "granted" ? "ممنوح ✓" :
    perm === "denied" ? "مرفوض من إعدادات المتصفح/النظام" :
    perm === "unsupported" ? "غير مدعوم في هذا المتصفح" : "بانتظار التفعيل";

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>إعدادات التذكيرات والإشعارات</strong>
      </div>
      <button class="btn btn-primary btn-sm" data-action="open-add-reminder">+ تذكير</button>
    </div>

    <div class="card">
      <strong>حالة إشعارات الجهاز: <span class="${perm === "granted" ? "text-pos" : "text-warn"}">${permAr}</span></strong>
      <p class="text-muted" style="margin-top:6px;">
        ${ios && !standalone
          ? "⚠️ على الآيفون (iOS 16.4+): متصفح Safari العادي لا يدعم الإشعارات إلا بعد تثبيت التطبيق: اضغط زر المشاركة ⬆ ثم «إضافة إلى الشاشة الرئيسية»."
          : "✅ إشعارات الأقساط والديون تعمل محلياً فور الفحص وأثناء تشغيل التطبيق."}
      </p>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:8px;">
        ${perm !== "granted" && perm !== "unsupported" ? `<button class="btn btn-primary btn-sm" data-action="request-notif-perm">تفعيل إشعارات الجهاز</button>` : ""}
        <button class="btn btn-outline btn-sm" data-action="check-due-now">فحص الأقساط والديون الآن</button>
        <button class="btn btn-positive btn-sm" data-action="send-test-notif">إرسال إشعار تجريبي</button>
      </div>
    </div>

    <form class="card" data-form="reminder-prefs">
      <div class="switch-row">
        <div>
          <strong>تفعيل الفحص التلقائي للاستحقاقات</strong>
          <div class="text-muted">فحص الأقساط والديون المتأخرة عند تشغيل التطبيق</div>
        </div>
        <input type="checkbox" name="notificationsEnabled" ${p.notificationsEnabled !== false ? "checked" : ""} />
      </div>
      <div class="switch-row">
        <span>إشعارات بأولوية عالية</span>
        <input type="checkbox" name="reminderPriorityHigh" ${p.reminderPriorityHigh !== false ? "checked" : ""} />
      </div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
        <div><label class="field-label">ساعة التنبيه الافتراضية</label><input class="input" type="number" min="0" max="23" name="defaultReminderHour" value="${Number(p.defaultReminderHour ?? 9)}" /></div>
        <div><label class="field-label">الدقيقة</label><input class="input" type="number" min="0" max="59" name="defaultReminderMinute" value="${Number(p.defaultReminderMinute ?? 0)}" /></div>
      </div>
      <button type="submit" class="btn btn-primary btn-sm" style="width:100%;margin-top:8px;">حفظ إعدادات التذكيرات</button>
    </form>

    <strong>التذكيرات المخصصة المجدولة (${state.reminders.length})</strong>
    <div class="list-stack">
      ${state.reminders.length ? state.reminders.map((r) => `
        <div class="card">
          <div class="row-between">
            <div>
              <strong>${escapeHtml(r.title)}</strong>
              <div class="text-muted">${escapeHtml(r.message)}</div>
              <div style="font-size:11px;color:var(--positive);margin-top:3px;">
                موعد التنبيه: ${formatDate(r.triggerAtMillis)} • الأولوية: ${escapeHtml(r.priority || "NORMAL")} • ${r.isSent ? "تم التنبيه" : "نشط"}
              </div>
            </div>
            <button class="btn btn-danger btn-sm" data-action="delete-reminder" data-id="${r.id}">حذف</button>
          </div>
        </div>
      `).join("") : `<div class="card text-muted">لا توجد تذكيرات مخصصة حالياً.</div>`}
    </div>
  `;
}

function renderSearchPage() {
  const q = state.searchQuery.trim().toLowerCase();
  const currencies = state.currencies || [];
  const matchedDebts = !q ? state.debts.slice(0, 8) : state.debts.filter((d) =>
    (d.contactName || "").toLowerCase().includes(q) || (d.reason || "").toLowerCase().includes(q) || (d.notes || "").toLowerCase().includes(q)
  );
  const matchedContacts = !q ? [] : state.contacts.filter((c) =>
    (c.fullName || "").toLowerCase().includes(q) || (c.phone || "").includes(q)
  );
  const matchedPayments = !q ? [] : state.payments.filter((p) =>
    (p.receiptNumber || "").toLowerCase().includes(q) || (p.notes || "").toLowerCase().includes(q)
  );

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="dashboard">رجوع</button>
        <strong>البحث الشامل</strong>
      </div>
    </div>
    <input class="input" type="search" placeholder="ابحث عن عميل، هاتف، دين، بيان، أو رقم إيصال..." value="${escapeHtml(state.searchQuery)}" data-input="search-query" autofocus />
    ${matchedContacts.length ? `
      <strong>الأشخاص (${matchedContacts.length})</strong>
      <div class="list-stack">
        ${matchedContacts.map((c) => `<div class="card card-clickable" data-action="open-contact-detail" data-id="${c.id}"><strong>${escapeHtml(c.fullName)}</strong><div class="text-muted">${escapeHtml(c.phone || "")}</div></div>`).join("")}
      </div>
    ` : ""}
    ${matchedPayments.length ? `
      <strong>الإيصالات (${matchedPayments.length})</strong>
      <div class="list-stack">
        ${matchedPayments.map((p) => `<div class="card card-clickable" data-action="open-receipt-detail" data-id="${p.id}"><strong>إيصال ${escapeHtml(p.receiptNumber)}</strong><div class="text-muted">${formatMinor(p.amountMinor, p.currencyCode, currencies)} • ${formatDate(p.paidAtMillis)}</div></div>`).join("")}
      </div>
    ` : ""}
    <strong>القيود المالية (${matchedDebts.length})</strong>
    <div class="list-stack">
      ${matchedDebts.length ? matchedDebts.map(renderDebtCard).join("") : `<div class="card text-muted">لا توجد نتائج مطابقة</div>`}
    </div>
  `;
}

function renderAiAssistantPage() {
  const active = Boolean(state.preferences.aiConsentGranted);
  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="dashboard">رجوع</button>
        <div>
          <strong>المساعد المالي الذكي</strong>
          <div style="font-size:11px;color:${active ? "var(--positive)" : "var(--warning)"};font-weight:700;">
            ${active ? "مفعّل وجاهز • تحليل مباشر لبياناتك (يعمل أوفلاين)" : "بانتظار التفعيل"}
          </div>
        </div>
      </div>
      ${state.aiMessages.length ? `<button class="btn btn-outline btn-sm" data-action="clear-ai-chat">محادثة جديدة</button>` : ""}
    </div>

    <div class="card">
      <div class="switch-row" style="padding:0;">
        <div>
          <strong>تفعيل التحليل المالي الذكي</strong>
          <div class="text-muted">تحليل أرصدتك، الأقساط، كبار العملاء، والمتأخرات فوراً من بياناتك الفعلية.</div>
        </div>
        <button class="chip ${active ? "active" : ""}" data-action="toggle-ai-consent">${active ? "مفعّل ✓" : "غير مفعّل"}</button>
      </div>
      ${!active ? `
        <button class="btn btn-positive" style="width:100%;margin-top:10px;" data-action="activate-ai-now">
          ${ICONS.sparkle}
          <span>${state.aiPendingQuestion ? "موافق — تفعيل وإرسال سؤالك الآن" : "تفعيل المساعد الذكي الآن"}</span>
        </button>
      ` : ""}
    </div>

    ${state.aiPendingQuestion && !active ? `
      <div class="alert-banner"><span>السؤال الجاهز: «${escapeHtml(state.aiPendingQuestion)}» — اضغط التفعيل أعلاه لعرض الإجابة.</span></div>
    ` : ""}

    <div class="tabs-row" style="margin-top:2px;">
      ${AI_QUICK_PROMPTS.map((q) => `<button type="button" class="chip" data-action="ask-ai-prompt" data-val="${escapeHtml(q)}">✨ ${escapeHtml(q)}</button>`).join("")}
    </div>

    <div class="ai-chat-stack">
      ${state.aiMessages.length ? state.aiMessages.map((m) => `
        <div class="ai-msg ${m.isUser ? "ai-msg-user" : "ai-msg-bot"}">${escapeHtml(m.text)}</div>
      `).join("") : `
        <div class="card" style="text-align:center;padding:24px;">
          <div style="color:var(--accent);display:inline-flex;margin-bottom:6px;">${ICONS.sparkle}</div>
          <strong style="display:block;">اسأل عن وضعك المالي</strong>
          <p class="text-muted" style="margin-top:4px;">اختر سؤالاً سريعاً أعلاه أو اكتب سؤالك عن الديون والتحصيل والأقساط أو اسم أي عميل.</p>
        </div>
      `}
    </div>

    <form class="ai-input-row" data-form="ask-ai-form">
      <input class="input" name="question" placeholder="اكتب سؤالك المالي هنا (مثال: كم حساب أحمد؟)..." required />
      <button type="submit" class="btn btn-positive" style="min-height:42px;padding:0 14px;" title="إرسال">${ICONS.send}</button>
    </form>
  `;
}

function renderModal() {
  if (!state.modal) return "";
  const { type, data } = state.modal;
  const currencies = state.currencies || [];
  const allCurrencies = mergeAllCurrencies(currencies);

  if (type === "currency-picker") {
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between"><strong>تغيير العملة الافتراضية (C)</strong><button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <div class="list-stack" style="margin-top:10px;">
            ${allCurrencies.map((c) => {
              const sel = c.code === state.preferences.defaultCurrency;
              return `
                <button class="btn ${sel ? "btn-primary" : "btn-outline"}" style="justify-content:space-between;" data-action="set-currency-and-close" data-val="${c.code}">
                  <span>${escapeHtml(c.nameAr)} (${escapeHtml(c.code)} - ${escapeHtml(c.symbol)})</span>
                  <span>${sel ? "✓" : ""}</span>
                </button>
              `;
            }).join("")}
          </div>
        </div>
      </div>
    `;
  }

  if (type === "ios-guide") {
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between"><strong>التثبيت والإشعارات على الآيفون</strong><button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <ol style="padding-right:18px;font-size:13px;line-height:1.7;">
            <li>افتح الرابط في متصفح <strong>Safari</strong> على الآيفون.</li>
            <li>اضغط زر <strong>المشاركة ⬆</strong> في الأسفل ثم اختر <strong>إضافة إلى الشاشة الرئيسية</strong>.</li>
            <li>افتح أيقونة <strong>حساباتي</strong> من الشاشة الرئيسية (سيعمل بكامل شاشته وبدون إنترنت).</li>
            <li>من <strong>المزيد ← التذكيرات المحلية</strong> اضغط <strong>تفعيل إشعارات الجهاز</strong>.</li>
          </ol>
        </div>
      </div>
    `;
  }

  if (type === "add-contact" || type === "edit-contact") {
    const existing = type === "edit-contact" ? state.contacts.find((c) => c.id === data.contactId) : null;
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="save-contact" data-contact-id="${escapeHtml(existing?.id || "")}">
          <div class="row-between"><strong>${existing ? "تعديل بيانات الشخص" : "إضافة شخص / عميل جديد"}</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <label class="field-label">الاسم الكامل *</label>
          <input class="input" name="fullName" required value="${escapeHtml(existing?.fullName || "")}" placeholder="مثال: أحمد محمد علي" />
          <label class="field-label">رقم الهاتف (واتساب / SMS)</label>
          <input class="input" name="phone" type="tel" value="${escapeHtml(existing?.phone || "")}" placeholder="77xxxxxxx" />
          <label class="field-label">العنوان</label>
          <input class="input" name="address" value="${escapeHtml(existing?.address || "")}" placeholder="المدينة / الحي" />
          <label class="field-label">ملاحظات</label>
          <input class="input" name="notes" value="${escapeHtml(existing?.notes || "")}" placeholder="ملاحظة اختيارية" />
          <label class="field-label">صورة شخصية / شعار العميل (اختياري)</label>
          <input type="file" accept="image/*" class="input" data-input="contact-avatar" />
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
            ${state.isSaving ? "جاري الحفظ..." : existing ? "تحديث بيانات الشخص" : "حفظ بيانات الشخص"}
          </button>
        </form>
      </div>
    `;
  }

  if (type === "contact-detail") {
    const c = state.contacts.find((x) => x.id === data.contactId);
    if (!c) return "";
    const cur = state.preferences.defaultCurrency || "YER";
    const cDebts = state.debts.filter((d) => d.contactId === c.id);
    const rec = cDebts.filter((d) => d.direction === "RECEIVABLE").reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode, cur, currencies), 0);
    const pay = cDebts.filter((d) => d.direction === "PAYABLE").reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode, cur, currencies), 0);
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between">
            <div style="display:flex;align-items:center;gap:10px;">
              ${c.avatarUri ? `<img src="${escapeHtml(c.avatarUri)}" class="avatar-circle" alt="${escapeHtml(c.fullName)}" />` : `<span class="avatar-circle">${escapeHtml(c.fullName.slice(0, 1))}</span>`}
              <div><strong>${escapeHtml(c.fullName)}</strong><div class="text-muted">${escapeHtml(c.phone || "بدون هاتف")}</div></div>
            </div>
            <button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button>
          </div>
          <div class="card" style="background:var(--surface-alt);margin-top:10px;">
            <div class="row-between"><span>مستحق لي عليه</span><strong class="text-pos">${formatMinor(rec, cur, currencies)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>مستحق له عليّ</span><strong class="text-neg">${formatMinor(pay, cur, currencies)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>صافي الرصيد</span><strong>${formatSignedMinor(rec - pay, cur, currencies)}</strong></div>
            ${c.address ? `<div class="text-muted" style="margin-top:4px;">العنوان: ${escapeHtml(c.address)}</div>` : ""}
            ${c.notes ? `<div class="text-muted">ملاحظات: ${escapeHtml(c.notes)}</div>` : ""}
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:10px;">
            <button class="btn btn-primary btn-sm" data-action="open-add-debt" data-id="${c.id}">+ تسجيل دين</button>
            <button class="btn btn-outline btn-sm" data-action="open-statement" data-id="${c.id}">كشف الحساب</button>
            ${c.phone ? `<a class="btn btn-outline btn-sm" href="tel:${escapeHtml(c.phone)}">اتصال</a>` : ""}
            <button class="btn btn-positive btn-sm" data-action="share-statement-wa" data-id="${c.id}">واتساب</button>
            <button class="btn btn-outline btn-sm" data-action="share-statement-sms" data-id="${c.id}">SMS</button>
            <button class="btn btn-outline btn-sm" data-action="open-edit-contact" data-id="${c.id}">تعديل</button>
            <button class="btn btn-danger btn-sm" data-action="delete-contact" data-id="${c.id}">حذف</button>
          </div>
          <label class="field-label" style="margin-top:12px;">القيود المرتبطة (${cDebts.length})</label>
          <div class="list-stack">
            ${cDebts.length ? cDebts.map((d) => `
              <div class="card card-clickable" data-action="open-debt-detail" data-id="${d.id}" style="padding:8px 10px;">
                <div class="row-between"><strong>${escapeHtml(d.reason)}</strong><strong>${formatMinor(d.remainingMinor, d.currencyCode, currencies)}</strong></div>
                <div class="text-muted">${getDirectionLabelAr(d.direction)} • ${getStatusLabelAr(d.status)}</div>
              </div>
            `).join("") : `<div class="text-muted">لا توجد ديون مسجلة لهذا الشخص</div>`}
          </div>
        </div>
      </div>
    `;
  }

  if (type === "add-debt") {
    const preContactId = data?.contactId || "";
    const contactOptions = state.contacts.map((c) => `<option value="${c.id}" ${c.id === preContactId ? "selected" : ""}>${escapeHtml(c.fullName)}</option>`).join("");
    const today = new Date().toISOString().slice(0, 10);
    const nextMonth = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
    const defCur = state.preferences.defaultCurrency || "YER";
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="add-debt">
          <div class="row-between"><strong>تسجيل دين جديد</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          ${!state.contacts.length ? `
            <p class="text-neg">يرجى إضافة شخص أولاً قبل تسجيل الدين.</p>
            <button type="button" class="btn btn-primary" style="width:100%;" data-action="open-add-contact">+ إضافة شخص الآن</button>
          ` : `
            <label class="field-label">الشخص *</label>
            <select class="select" name="contactId" required>${contactOptions}</select>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
              <div>
                <label class="field-label">نوع الدين</label>
                <select class="select" name="direction">
                  <option value="RECEIVABLE">مستحق لي عند الآخرين</option>
                  <option value="PAYABLE">مستحق عليّ للآخرين</option>
                </select>
              </div>
              <div>
                <label class="field-label">العملة</label>
                <select class="select" name="currencyCode">
                  ${allCurrencies.map((c) => `<option value="${c.code}" ${c.code === defCur ? "selected" : ""}>${escapeHtml(c.code)} (${escapeHtml(c.symbol)})</option>`).join("")}
                </select>
              </div>
            </div>
            <label class="field-label">المبلغ *</label>
            <input class="input" name="amount" type="number" step="0.01" min="0.01" required placeholder="0" />
            <label class="field-label">البيان / السبب *</label>
            <input class="input" name="reason" required placeholder="مثال: فاتورة بضاعة أو سلفة" />
            <label class="field-label">طريقة الصرف</label>
            <select class="select" name="paymentMethod">
              <option value="CASH">نقدي</option>
              <option value="TRANSFER">تحويل / صرافة</option>
              <option value="CARD">بطاقة</option>
              <option value="OTHER">أخرى / بضاعة آجلة</option>
            </select>
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
              <div><label class="field-label">تاريخ التسجيل</label><input class="input" name="recordedDate" type="date" value="${today}" /></div>
              <div><label class="field-label">تاريخ الاستحقاق</label><input class="input" name="dueDate" type="date" value="${nextMonth}" /></div>
            </div>
            <label class="field-label">تقسيط المبلغ (0 بدون تقسيط أو 2 - 36 قسطاً)</label>
            <input class="input" name="installmentCount" type="number" min="0" max="36" value="0" />
            <label class="field-label">ملاحظات إضافية</label>
            <input class="input" name="notes" placeholder="تفاصيل إضافية (اختياري)" />
            <label class="field-label">صورة مرفقة للسند / الفاتورة (اختياري)</label>
            <input type="file" accept="image/*" class="input" data-input="debt-attachment" />
            <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
              ${state.isSaving ? "جاري الحفظ..." : "حفظ الدين وإصدار سند الإشعار"}
            </button>
          `}
        </form>
      </div>
    `;
  }

  if (type === "edit-debt") {
    const debt = state.debts.find((d) => d.id === data.debtId);
    if (!debt) return "";
    const dueStr = new Date(debt.dueAtMillis || Date.now()).toISOString().slice(0, 10);
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="edit-debt" data-debt-id="${debt.id}">
          <div class="row-between"><strong>تعديل القيد — ${escapeHtml(debt.contactName)}</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <p class="text-muted">أصل المبلغ ثابت محاسبياً (${formatMinor(debt.principalMinor, debt.currencyCode, currencies)}) لحماية السجل، ويمكنك تعديل الاستحقاق والبيان مع توثيق السبب.</p>
          <label class="field-label">تاريخ الاستحقاق</label>
          <input class="input" name="dueDate" type="date" value="${dueStr}" required />
          <label class="field-label">البيان</label>
          <input class="input" name="reason" value="${escapeHtml(debt.reason)}" required />
          <label class="field-label">الملاحظات</label>
          <input class="input" name="notes" value="${escapeHtml(debt.notes || "")}" />
          <label class="field-label">سبب التعديل (إلزامي لسجل التدقيق) *</label>
          <input class="input" name="editReason" required placeholder="مثال: تمديد موعد السداد بالاتفاق مع العميل" />
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;">حفظ التعديل وتوثيقه</button>
        </form>
      </div>
    `;
  }

  if (type === "pay-debt") {
    const debt = state.debts.find((d) => d.id === data.debtId);
    if (!debt) return "";
    const today = new Date().toISOString().slice(0, 10);
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="pay-debt" data-debt-id="${debt.id}">
          <div class="row-between"><strong>تسجيل دفعة — ${escapeHtml(debt.contactName)}</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <p class="text-muted">المتبقي الحالي: <strong>${formatMinor(debt.remainingMinor, debt.currencyCode, currencies)}</strong></p>
          <div style="display:flex;gap:6px;margin-bottom:6px;">
            <button type="button" class="chip active" data-action="fill-pay-amount" data-val="${minorToPlain(debt.remainingMinor)}">سداد كامل</button>
            <button type="button" class="chip" data-action="fill-pay-amount" data-val="${minorToPlain(Math.floor(debt.remainingMinor / 2))}">نصف المبلغ</button>
          </div>
          <label class="field-label">المبلغ المدفوع (${escapeHtml(debt.currencyCode)}) *</label>
          <input class="input" id="pay-amount-input" name="amount" type="number" step="0.01" min="0" value="${minorToPlain(debt.remainingMinor)}" required />
          <label class="field-label">خصم / تنازل متفق عليه (اختياري)</label>
          <input class="input" name="discount" type="number" step="0.01" min="0" value="0" />
          <label class="switch-row">
            <span>تسوية نهائية وإغلاق الدين</span>
            <input type="checkbox" name="isFinalSettlement" />
          </label>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <div>
              <label class="field-label">طريقة الدفع</label>
              <select class="select" name="method">
                <option value="CASH">نقدي</option>
                <option value="TRANSFER">تحويل بنكي / صرافة</option>
                <option value="CARD">بطاقة</option>
                <option value="OTHER">أخرى</option>
              </select>
            </div>
            <div>
              <label class="field-label">تاريخ الدفع</label>
              <input class="input" name="paidDate" type="date" value="${today}" />
            </div>
          </div>
          <label class="field-label">ملاحظات الإيصال</label>
          <input class="input" name="notes" placeholder="رقم الحوالة أو ملاحظة (اختياري)" />
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
            ${state.isSaving ? "جاري الحفظ..." : "تأكيد الدفعة وإصدار الإيصال"}
          </button>
        </form>
      </div>
    `;
  }

  if (type === "reschedule-installment") {
    const ins = state.installments.find((i) => i.id === data.installmentId);
    if (!ins) return "";
    const dueStr = new Date(ins.dueAtMillis || Date.now()).toISOString().slice(0, 10);
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="reschedule-installment" data-installment-id="${ins.id}">
          <div class="row-between"><strong>إعادة جدولة قسط #${ins.installmentNumber}</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <label class="field-label">مبلغ القسط</label>
          <input class="input" name="amount" type="number" step="0.01" min="0.01" value="${minorToPlain(ins.amountMinor)}" required />
          <label class="field-label">تاريخ الاستحقاق الجديد</label>
          <input class="input" name="dueDate" type="date" value="${dueStr}" required />
          <label class="field-label">سبب إعادة الجدولة *</label>
          <input class="input" name="reason" required placeholder="مثال: تأجيل بالاتفاق" />
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;">حفظ الجدولة الجديدة</button>
        </form>
      </div>
    `;
  }

  if (type === "receipt-detail") {
    const p = state.payments.find((x) => x.id === data.paymentId);
    if (!p) return "";
    const debt = state.debts.find((d) => d.id === p.debtId);
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between"><strong>تفاصيل إيصال ${escapeHtml(p.receiptNumber)}</strong><button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <div class="card" style="background:var(--surface-alt);margin-top:8px;">
            <div class="row-between"><span>العميل</span><strong>${escapeHtml(debt?.contactName || "عميل")}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>المبلغ المدفوع</span><strong class="text-pos">${formatMinor(p.amountMinor, p.currencyCode, currencies)}</strong></div>
            ${p.discountMinor > 0 ? `<div class="row-between" style="margin-top:4px;"><span>الخصم</span><strong>${formatMinor(p.discountMinor, p.currencyCode, currencies)}</strong></div>` : ""}
            <div class="row-between" style="margin-top:4px;"><span>الرصيد قبل</span><strong>${formatMinor(p.balanceBeforeMinor, p.currencyCode, currencies)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>الرصيد بعد</span><strong>${formatMinor(p.balanceAfterMinor, p.currencyCode, currencies)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>الطريقة والتاريخ</span><strong>${escapeHtml(getPaymentMethodLabelAr(p.method))} • ${formatDate(p.paidAtMillis)}</strong></div>
            ${p.notes ? `<div class="text-muted" style="margin-top:4px;">ملاحظات: ${escapeHtml(p.notes)}</div>` : ""}
          </div>
          <div style="display:flex;gap:8px;margin-top:10px;">
            <button class="btn btn-primary btn-sm" style="flex:1;" data-action="print-payment-receipt" data-id="${p.id}">طباعة الإيصال PDF</button>
            <button class="btn btn-positive btn-sm" style="flex:1;" data-action="share-payment-wa" data-id="${p.id}">إرسال واتساب</button>
          </div>
        </div>
      </div>
    `;
  }

  if (type === "debt-detail") {
    const debt = state.debts.find((d) => d.id === data.debtId);
    if (!debt) return "";
    const instList = state.installments.filter((ins) => ins.debtId === debt.id);
    const payList = state.payments.filter((p) => p.debtId === debt.id);
    const debtAudits = (state.auditLogs || []).filter((a) => a.entityId === debt.id);
    const relayReady = Boolean((state.preferences.whatsappCloudEnabled || state.preferences.smsGatewayEnabled) && state.preferences.backendBaseUrl);
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between"><strong>تفاصيل القيد — ${escapeHtml(debt.contactName)}</strong><button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <div class="card" style="background:var(--surface-alt);margin-top:8px;">
            <div class="row-between"><span>أصل الدين</span><strong>${formatMinor(debt.principalMinor, debt.currencyCode, currencies)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>المتبقي</span><strong class="text-neg">${formatMinor(debt.remainingMinor, debt.currencyCode, currencies)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>الاستحقاق</span><strong>${formatDate(debt.dueAtMillis)}</strong></div>
            <div class="text-muted" style="margin-top:4px;">البيان: ${escapeHtml(debt.reason)}</div>
            ${debt.notes ? `<div class="text-muted">ملاحظات: ${escapeHtml(debt.notes)}</div>` : ""}
            ${debt.attachmentUri ? `<img src="${escapeHtml(debt.attachmentUri)}" class="attachment-thumb" alt="مرفق الفاتورة" />` : ""}
          </div>

          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:10px;">
            ${debt.remainingMinor > 0 ? `<button class="btn btn-primary btn-sm" data-action="open-pay-debt" data-id="${debt.id}">تسجيل دفعة</button>` : ""}
            <button class="btn btn-outline btn-sm" data-action="open-edit-debt" data-id="${debt.id}">تعديل القيد</button>
            <button class="btn btn-positive btn-sm" data-action="send-debt-wa" data-id="${debt.id}">واتساب</button>
            <button class="btn btn-outline btn-sm" data-action="send-debt-sms" data-id="${debt.id}">SMS</button>
            ${relayReady ? `<button class="btn btn-accent btn-sm" data-action="relay-send-debt" data-id="${debt.id}">إرسال آلي (خادم)</button>` : ""}
            <button class="btn btn-outline btn-sm" data-action="print-debt-voucher" data-id="${debt.id}">طباعة PDF</button>
            <button class="btn btn-outline btn-sm" data-action="open-add-reminder" data-id="${debt.id}">+ تذكير</button>
            <button class="btn btn-danger btn-sm" data-action="delete-debt" data-id="${debt.id}">حذف</button>
          </div>

          ${instList.length ? `
            <label class="field-label" style="margin-top:12px;">جدول الأقساط (${instList.length})</label>
            <div class="list-stack">
              ${instList.map((ins) => {
                const rem = Math.max(0, ins.amountMinor - ins.paidMinor);
                return `
                  <div class="card" style="padding:8px 10px;">
                    <div class="row-between">
                      <strong>قسط #${ins.installmentNumber}</strong>
                      <div style="display:flex;gap:6px;align-items:center;">
                        <span class="status-badge st-${ins.status}">${getStatusLabelAr(ins.status)}</span>
                        ${ins.status !== "PAID" ? `<button class="btn btn-outline btn-sm" style="min-height:28px;padding:2px 8px;" data-action="open-reschedule-inst" data-id="${ins.id}">جدولة</button>` : ""}
                      </div>
                    </div>
                    <div class="row-between text-muted" style="margin-top:4px;">
                      <span>المتبقي: ${formatMinor(rem, debt.currencyCode, currencies)} / ${formatMinor(ins.amountMinor, debt.currencyCode, currencies)}</span>
                      <span>${formatDate(ins.dueAtMillis)}</span>
                    </div>
                  </div>
                `;
              }).join("")}
            </div>
          ` : ""}

          ${payList.length ? `
            <label class="field-label" style="margin-top:12px;">الدفعات المسجلة (${payList.length})</label>
            <div class="list-stack">
              ${payList.map((p) => `
                <div class="card card-clickable" data-action="open-receipt-detail" data-id="${p.id}" style="padding:8px 10px;">
                  <div class="row-between">
                    <strong>إيصال ${escapeHtml(p.receiptNumber)}</strong>
                    <strong class="text-pos">${formatMinor(p.amountMinor, p.currencyCode, currencies)}</strong>
                  </div>
                  <div class="text-muted">${formatDate(p.paidAtMillis)} • المتبقي بعدها ${formatMinor(p.balanceAfterMinor, p.currencyCode, currencies)}</div>
                </div>
              `).join("")}
            </div>
          ` : ""}

          ${debtAudits.length ? `
            <label class="field-label" style="margin-top:12px;">سجل التدقيق لهذا القيد (${debtAudits.length})</label>
            <div class="list-stack">
              ${debtAudits.map((a) => `
                <div class="text-muted" style="border-right:2px solid var(--border);padding-right:8px;">
                  <strong>${escapeHtml(a.action)}</strong> — ${escapeHtml(a.details)} (${formatDateTime(a.createdAtMillis)})
                </div>
              `).join("")}
            </div>
          ` : ""}
        </div>
      </div>
    `;
  }

  if (type === "add-reminder") {
    const selectedDebt = state.debts.find((d) => d.id === data?.debtId) || state.debts[0] || null;
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="add-reminder">
          <div class="row-between"><strong>إنشاء تذكير محلي مخصص</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <label class="field-label">الربط بدين (اختياري)</label>
          <select class="select" name="debtId">
            <option value="general">تذكير عام</option>
            ${state.debts.map((d) => `<option value="${d.id}" ${selectedDebt?.id === d.id ? "selected" : ""}>${escapeHtml(d.contactName)} — ${formatMinor(d.remainingMinor, d.currencyCode, currencies)}</option>`).join("")}
          </select>
          <label class="field-label">عنوان التذكير</label>
          <input class="input" name="title" required value="${escapeHtml(selectedDebt ? `تذكير سداد: ${selectedDebt.contactName}` : "تذكير بمتابعة الأقساط")}" />
          <label class="field-label">نص التذكير</label>
          <input class="input" name="message" required value="${escapeHtml(selectedDebt ? `متابعة تحصيل/سداد ${formatMinor(selectedDebt.remainingMinor, selectedDebt.currencyCode, currencies)} (${selectedDebt.reason})` : "يرجى متابعة مواعيد السداد المستحقة")}" />
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
            <div><label class="field-label">التذكير بعد (أيام)</label><input class="input" name="daysAhead" type="number" min="0" value="0" /></div>
            <div>
              <label class="field-label">الأولوية</label>
              <select class="select" name="priority">
                <option value="HIGH" ${state.preferences.reminderPriorityHigh !== false ? "selected" : ""}>عالية</option>
                <option value="NORMAL">عادية</option>
                <option value="LOW">منخفضة</option>
              </select>
            </div>
          </div>
          <label class="field-label">التكرار</label>
          <select class="select" name="repeatType">
            <option value="ONCE">مرة واحدة</option>
            <option value="DAILY">يومياً</option>
            <option value="WEEKLY">أسبوعياً</option>
            <option value="MONTHLY">شهرياً</option>
          </select>
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;">حفظ التذكير</button>
        </form>
      </div>
    `;
  }

  if (type === "voucher") {
    return `
      <div class="modal-backdrop">
        <div class="modal-card" style="text-align:center;">
          <div style="font-size:28px;color:var(--positive);">✓</div>
          <h3 style="margin:6px 0 2px;">${escapeHtml(data.title)}</h3>
          <div class="text-muted">رقم السند: ${escapeHtml(data.voucherNumber)}</div>
          <div class="card" style="background:var(--surface-alt);text-align:right;margin:12px 0;">
            <div class="row-between"><span>الطرف</span><strong>${escapeHtml(data.partyName)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>المبلغ</span><strong class="text-pos">${escapeHtml(data.amountFormatted)}</strong></div>
            ${data.remainingFormatted ? `<div class="row-between" style="margin-top:4px;"><span>المتبقي</span><strong>${escapeHtml(data.remainingFormatted)}</strong></div>` : ""}
          </div>
          <div style="display:flex;gap:8px;">
            <button class="btn btn-positive" style="flex:1;" data-action="share-voucher-wa">${ICONS.chat}<span>إرسال واتساب</span></button>
            <button class="btn btn-outline" style="flex:1;" data-action="share-voucher-sms">إرسال SMS</button>
          </div>
          <button class="btn btn-outline" style="width:100%;margin-top:8px;" data-action="close-modal">تم والعودة</button>
        </div>
      </div>
    `;
  }

  if (type === "print-settings") {
    const p = state.preferences;
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="print-settings">
          <div class="row-between"><strong>إعدادات الفواتير والطباعة PDF</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <label class="field-label">قالب الفاتورة</label>
          <select class="select" name="printTemplate">
            ${PDF_TEMPLATES.map((t) => `<option value="${t.id}" ${p.printTemplate === t.id ? "selected" : ""}>${escapeHtml(t.titleAr)} — ${escapeHtml(t.subtitleAr)}</option>`).join("")}
          </select>
          <label class="field-label">اسم المؤسسة (يمين الهيدر)</label>
          <input class="input" name="printCompanyName" value="${escapeHtml(p.printCompanyName)}" placeholder="مثال: مؤسسة الأمل التجارية" />
          <label class="field-label">هاتف المؤسسة</label>
          <input class="input" name="printCompanyPhone" value="${escapeHtml(p.printCompanyPhone)}" />
          <label class="field-label">عنوان المؤسسة</label>
          <input class="input" name="printCompanyAddress" value="${escapeHtml(p.printCompanyAddress)}" />
          <label class="field-label">الرقم الضريبي / السجل التجاري</label>
          <input class="input" name="printTaxOrCrNumber" value="${escapeHtml(p.printTaxOrCrNumber)}" />
          <label class="field-label">شعار المؤسسة (منتصف الهيدر)</label>
          <div style="display:flex;gap:8px;align-items:center;">
            <label class="btn btn-outline btn-sm" style="cursor:pointer;">
              رفع الشعار
              <input type="file" accept="image/*" style="display:none;" data-input="upload-logo" />
            </label>
            ${p.printLogoDataUrl ? `<button type="button" class="btn btn-danger btn-sm" data-action="clear-logo">إزالة</button>` : `<span class="text-muted">شعار حرفي تلقائي</span>`}
          </div>
          <label class="switch-row"><span>إظهار الشعار في الترويسة</span><input type="checkbox" name="printShowLogoInHeader" ${p.printShowLogoInHeader !== false ? "checked" : ""} /></label>
          <label class="switch-row"><span>إظهار ترويسة انما سوفت بالرأس</span><input type="checkbox" name="printShowInmaSoftInHeader" ${p.printShowInmaSoftInHeader !== false ? "checked" : ""} /></label>
          <label class="switch-row"><span>إظهار انما سوفت بالتذييل</span><input type="checkbox" name="printShowInmaSoftInFooter" ${p.printShowInmaSoftInFooter !== false ? "checked" : ""} /></label>
          <label class="switch-row"><span>إظهار مربعات التوقيع والختم</span><input type="checkbox" name="printShowSignatureBox" ${p.printShowSignatureBox !== false ? "checked" : ""} /></label>
          <label class="switch-row"><span>إظهار العلامة المائية في الخلفية</span><input type="checkbox" name="printShowWatermarkBadge" ${p.printShowWatermarkBadge !== false ? "checked" : ""} /></label>

          <div class="row-between" style="margin-top:6px;">
            <label class="field-label" style="margin:0;">ألوان كل مستند</label>
            <button type="button" class="btn btn-outline btn-sm" data-action="apply-unified-print-color">توحيد للجميع</button>
          </div>
          ${PDF_DOC_KINDS.map((k) => `
            <div class="row-between" style="margin:6px 0;">
              <span style="font-size:12px;">${escapeHtml(k.titleAr)}</span>
              <select class="select" style="width:auto;min-height:34px;" name="${k.key}">
                ${PDF_COLOR_SWATCHES.map((s) => `<option value="${s.hex}" ${p[k.key] === s.hex ? "selected" : ""}>${escapeHtml(s.nameAr)}</option>`).join("")}
              </select>
            </div>
          `).join("")}

          <label class="field-label">عبارة الفوتر أسفل الفاتورة</label>
          <input class="input" name="printFooterNote" value="${escapeHtml(p.printFooterNote)}" />

          <label class="field-label">معاينة حية للفاتورة:</label>
          <div class="live-preview-box">${buildLiveInvoicePreviewHtml(p, "DEBT_VOUCHER")}</div>

          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;">حفظ إعدادات الطباعة</button>
        </form>
      </div>
    `;
  }

  return "";
}

function render() {
  applyThemeAttributes();
  const root = document.getElementById("app-root");
  if (!root) return;

  const waPreview = buildFormattedWhatsAppMessage(state.preferences, {
    title: "سند إشعار مالي",
    contactName: state.contacts[0]?.fullName || "أحمد محمد",
    directionLabel: "مستحق لي",
    principalFormatted: `150,000 ${state.preferences.defaultCurrency}`,
    paidFormatted: `50,000 ${state.preferences.defaultCurrency}`,
    remainingFormatted: `100,000 ${state.preferences.defaultCurrency}`,
    recordedDateLabel: formatDate(Date.now()),
    dueDateLabel: formatDate(Date.now() + 15 * 86_400_000),
    reason: "فاتورة مبيعات رقم 104",
    receiptNumber: "DBT-202601"
  });

  const pageHtml =
    state.tab === "dashboard" ? renderDashboard() :
    state.tab === "debts" ? renderDebtsTab() :
    state.tab === "contacts" ? renderContactsTab() :
    state.tab === "ai" ? renderAiAssistantPage() :
    state.tab === "notifications" ? renderNotificationsPage() :
    state.tab === "reminders" ? renderRemindersPage() :
    state.tab === "whatsapp" ? renderWhatsAppAndSmsPage(state, waPreview) :
    state.tab === "settings" ? renderSettingsPageFull(state) :
    state.tab === "reports" ? renderReportsPage(state) :
    state.tab === "statement" ? renderStatementPage(state) :
    state.tab === "auth" ? renderAuthPage(state) :
    state.tab === "account" ? renderAccountAndSecurityPage(state) :
    state.tab === "workspace" ? renderWorkspacePage(state) :
    state.tab === "help" ? renderHelpPage() :
    state.tab === "search" ? renderSearchPage() :
    renderMoreHub();

  const activeNavTab =
    state.tab === "dashboard" || state.tab === "debts" || state.tab === "contacts"
      ? state.tab
      : "more";

  root.innerHTML = `
    <div class="app-shell">
      ${renderTopBar()}
      <main class="main-content">${pageHtml}</main>
      <div class="bottom-chrome">
        <nav class="bottom-nav">
          ${[
            ["dashboard", ICONS.home, "الرئيسية"],
            ["debts", ICONS.wallet, "الديون"],
            ["contacts", ICONS.groups, "الأشخاص"],
            ["more", ICONS.grid, "المزيد"]
          ].map(([id, icon, lbl]) => `
            <button class="nav-item ${activeNavTab === id ? "active" : ""}" data-action="switch-tab" data-val="${id}">
              <span class="nav-pill">${icon}</span>
              <span>${lbl}</span>
            </button>
          `).join("")}
        </nav>
        <footer class="anmasoft-footer">
          <span style="display:inline-flex;align-items:center;gap:4px;">
            <span style="color:var(--accent);display:inline-flex;">${ICONS.verified}</span>
            <span>عمل انما سوفت</span>
          </span>
          <a href="tel:772317950">${ICONS.phone}<span>772317950</span></a>
        </footer>
      </div>
      ${renderModal()}
      ${renderOnboardingOverlay(state)}
      ${renderPinLockOverlay(state)}
      ${state.toast ? `<div class="toast">${escapeHtml(state.toast)}</div>` : ""}
    </div>
  `;
}

async function handleCloudUserAuthenticated(user) {
  if (!user?.uid) return;
  state.cloudUser = user;

  // Migrate local offline records created before login so they upload automatically
  await migrateLocalUserIdToCloud(user.uid);
  await ensureCloudUserProfile(user, state.preferences).catch(() => {});

  // Start real-time Firestore listeners for all 10 subcollections
  if (realtimeUnsub) {
    try { realtimeUnsub(); } catch {}
    realtimeUnsub = null;
  }
  realtimeUnsub = await subscribeCloudRealtime(user.uid, async (storeName, docs) => {
    if (docs.length) {
      await mergeCloudCollectionLocal(storeName, docs);
      Object.assign(state, await loadAllLocalState());
      render();
    }
  });

  await syncAllWithCloud({ silent: true });
}

async function syncAllWithCloud({ silent = false } = {}) {
  if (!state.cloudUser) {
    if (!silent) {
      state.tab = "auth";
      render();
      showToast("يرجى تسجيل الدخول لربط حسابك بالمزامنة السحابية");
    }
    return;
  }
  if (!navigator.onLine) {
    if (!silent) showToast("أنت تعمل حالياً بدون إنترنت — البيانات محفوظة محلياً وسيتم رفعها تلقائياً عند الاتصال");
    return;
  }
  try {
    const freshLocal = await loadAllLocalState();
    const syncedIds = await pushOutboxToCloud(state.cloudUser.uid, freshLocal.outbox);
    for (const id of syncedIds) await clearOutboxItemLocal(id);

    const snap = await pullCloudSnapshot(state.cloudUser.uid);
    if (snap) {
      await mergeCloudCollectionLocal("contacts", snap.contacts);
      await mergeCloudCollectionLocal("debts", snap.debts);
      await mergeCloudCollectionLocal("payments", snap.payments);
      await mergeCloudCollectionLocal("installments", snap.installments);
      await mergeCloudCollectionLocal("currencies", snap.currencies);
      await mergeCloudCollectionLocal("reminders", snap.reminders);
      await mergeCloudCollectionLocal("notifications", snap.notification_logs);
      await mergeCloudCollectionLocal("templates", snap.message_templates);
      await mergeCloudCollectionLocal("auditLogs", snap.audit_logs);
      await mergeCloudCollectionLocal("members", snap.workspace_members);
    }

    Object.assign(state, await loadAllLocalState());
    render();
    if (!silent) showToast("تمت المزامنة السحابية بنجاح ✓");
  } catch {
    if (!silent) showToast("تعذرت المزامنة — تم حفظ البيانات محلياً في جهازك");
  }
}

document.addEventListener("click", async (e) => {
  const target = e.target.closest("[data-action]");
  if (!target) return;
  if (target.closest("[data-stop-card]")) e.stopPropagation();

  const action = target.dataset.action;
  const val = target.dataset.val;
  const id = target.dataset.id;
  const perms = getActiveMemberPermissions(state);
  const currencies = state.currencies || [];

  if (action === "switch-tab" || action === "go-tab") {
    state.tab = val;
    if (val === "account") state.storageQuota = await getStorageQuotaEstimate();
    render();
  }
  else if (action === "open-overdue-debts") { state.tab = "debts"; state.debtFilter = "OVERDUE"; render(); }
  else if (action === "set-debt-filter") { state.debtFilter = val; render(); }
  else if (action === "set-debt-cur-filter") { state.debtCurrencyFilter = val; render(); }
  else if (action === "set-contact-filter") { state.contactFilter = val; render(); }
  else if (action === "toggle-contact-sort") { state.contactSort = state.contactSort === "NAME" ? "BALANCE" : "NAME"; render(); }
  else if (action === "set-report-kind") { state.reportKind = val; render(); }
  else if (action === "set-statement-period") { state.statementPeriodDays = Number(val || 0); render(); }
  else if (action === "set-auth-mode") { state.authMode = val; render(); }
  else if (action === "open-statement") { state.modal = null; state.statementContactId = id || ""; state.tab = "statement"; render(); }
  else if (action === "close-modal") { state.modal = null; state.pendingAvatarDataUrl = ""; state.pendingDebtAttachmentUrl = ""; render(); }
  else if (action === "next-onboarding") { state.onboardingSlide = Math.min(2, (state.onboardingSlide || 0) + 1); render(); }
  else if (action === "finish-onboarding") {
    state.preferences = { ...state.preferences, onboardingCompleted: true };
    await savePreferencesLocal(state.preferences);
    render();
  }
  else if (action === "open-currency-picker") { state.modal = { type: "currency-picker", data: {} }; render(); }
  else if (action === "open-ios-guide") { state.modal = { type: "ios-guide", data: {} }; render(); }
  else if (action === "open-add-contact") { state.pendingAvatarDataUrl = ""; state.modal = { type: "add-contact", data: {} }; render(); }
  else if (action === "open-edit-contact") { state.pendingAvatarDataUrl = ""; state.modal = { type: "edit-contact", data: { contactId: id } }; render(); }
  else if (action === "open-contact-detail") { state.modal = { type: "contact-detail", data: { contactId: id } }; render(); }
  else if (action === "open-add-debt") {
    if (!perms.canAddDebt) return showToast("لا تملك صلاحية إضافة ديون جديدة");
    state.pendingDebtAttachmentUrl = "";
    state.modal = { type: "add-debt", data: { contactId: id || "" } };
    render();
  }
  else if (action === "open-edit-debt") {
    if (!perms.canEditDebt) return showToast("لا تملك صلاحية تعديل الديون");
    state.modal = { type: "edit-debt", data: { debtId: id } };
    render();
  }
  else if (action === "open-pay-debt") {
    if (!perms.canRecordPayment) return showToast("لا تملك صلاحية تسجيل الدفعات");
    state.modal = { type: "pay-debt", data: { debtId: id } };
    render();
  }
  else if (action === "open-debt-detail") { state.modal = { type: "debt-detail", data: { debtId: id } }; render(); }
  else if (action === "open-receipt-detail") { state.modal = { type: "receipt-detail", data: { paymentId: id } }; render(); }
  else if (action === "open-reschedule-inst") {
    if (!perms.canEditDebt) return showToast("لا تملك صلاحية تعديل الأقساط");
    state.modal = { type: "reschedule-installment", data: { installmentId: id } };
    render();
  }
  else if (action === "open-add-reminder") { state.modal = { type: "add-reminder", data: { debtId: id || "" } }; render(); }
  else if (action === "open-print-settings") { state.modal = { type: "print-settings", data: {} }; render(); }
  else if (action === "fill-pay-amount") {
    const inp = document.getElementById("pay-amount-input");
    if (inp) inp.value = val;
  }
  else if (action === "quick-payment") {
    if (!perms.canRecordPayment) return showToast("لا تملك صلاحية تسجيل الدفعات");
    const firstOpen = state.debts.find((d) => d.remainingMinor > 0 && d.status !== "PAID" && d.status !== "SETTLED");
    state.modal = firstOpen ? { type: "pay-debt", data: { debtId: firstOpen.id } } : { type: "add-debt", data: {} };
    render();
  }
  else if (action === "set-currency-and-close" || action === "set-currency") {
    state.preferences = { ...state.preferences, defaultCurrency: val };
    await savePreferencesLocal(state.preferences);
    if (action === "set-currency-and-close") state.modal = null;
    if (state.cloudUser) ensureCloudUserProfile(state.cloudUser, state.preferences).catch(() => {});
    render();
    showToast(`العملة الافتراضية: ${val}`);
  }
  else if (action === "set-theme-mode") {
    state.preferences = { ...state.preferences, themeMode: val };
    await savePreferencesLocal(state.preferences);
    render();
  }
  else if (action === "set-palette") {
    state.preferences = { ...state.preferences, themePalette: val };
    await savePreferencesLocal(state.preferences);
    render();
  }
  else if (action === "apply-custom-theme") {
    const t = (state.preferences.customThemes || []).find((x) => x.id === id);
    if (!t) return;
    state.preferences = { ...state.preferences, themePalette: "custom", customPrimaryHex: t.primaryHex, customAccentHex: t.accentHex };
    await savePreferencesLocal(state.preferences);
    render();
    showToast(`تم تطبيق ثيم ${t.name}`);
  }
  else if (action === "delete-custom-theme") {
    const next = (state.preferences.customThemes || []).filter((x) => x.id !== id);
    state.preferences = { ...state.preferences, customThemes: next };
    await savePreferencesLocal(state.preferences);
    render();
  }
  else if (action === "apply-unified-print-color") {
    const color = state.preferences.printDebtColorHex || "#0B1B32";
    state.preferences = {
      ...state.preferences,
      printDebtColorHex: color,
      printReceiptColorHex: color,
      printStatementColorHex: color,
      printReportColorHex: color
    };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم توحيد اللون لجميع المستندات");
  }
  else if (action === "set-wa-style") {
    state.preferences = { ...state.preferences, whatsappStyle: val };
    await savePreferencesLocal(state.preferences);
    render();
  }
  else if (action === "request-notif-perm") {
    const perm = await requestNotificationPermission();
    render();
    if (perm === "granted") {
      showToast("تم تفعيل إشعارات الجهاز بنجاح ✓");
      await dispatchSystemNotification({
        title: "حساباتي — تفعيل التنبيهات",
        body: "سيقوم التطبيق بتنبيهك بمواعيد سداد الأقساط وتحصيل الديون المستحقة.",
        tag: "perm_granted"
      });
      await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
    } else {
      showToast("لم يُمنح الإذن — تأكد من تثبيت التطبيق للشاشة الرئيسية في الآيفون");
    }
  }
  else if (action === "check-due-now") {
    await runDueNotificationCheck({ dispatchOs: true, manualFeedback: true });
  }
  else if (action === "send-test-notif") {
    const perm = await requestNotificationPermission();
    const sent = perm === "granted" && await dispatchSystemNotification({
      title: "إشعار تجريبي — حساباتي",
      body: "تذكير محلي بمواعيد الأقساط وتحصيل الديون المستحقة يعمل بنجاح ✓",
      tag: `test_${Date.now()}`
    });
    showToast(sent ? "تم إرسال الإشعار التجريبي للجهاز ✓" : "يرجى السماح بالإشعارات أولاً");
  }
  else if (action === "mark-all-read") {
    const updated = await markAllNotificationsReadLocal(state.notifications);
    for (const n of updated) {
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "notification_logs", docId: n.id, operation: "UPSERT", payload: n });
    }
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم تعليم جميع الإشعارات كمقروءة");
  }
  else if (action === "delete-reminder") {
    await deleteReminderLocal(id);
    await queueOutboxLocal({ id: `ob_${uid()}`, collection: "reminders", docId: id, operation: "DELETE" });
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم حذف التذكير");
  }
  else if (action === "delete-contact") {
    await deleteContactLocal(id);
    await queueOutboxLocal({ id: `ob_${uid()}`, collection: "contacts", docId: id, operation: "DELETE" });
    await recordAudit("حذف شخص", "CONTACT", id, "تم حذف سجل الشخص من دفتر العناوين");
    state.modal = null;
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم حذف الشخص");
  }
  else if (action === "delete-debt") {
    if (!perms.canEditDebt) return showToast("لا تملك صلاحية حذف الديون");
    await deleteDebtLocal(id);
    await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: id, operation: "DELETE" });
    await recordAudit("حذف قيد دين", "DEBT", id, "تم حذف القيد المالي");
    state.modal = null;
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم حذف قيد الدين");
  }
  else if (action === "delete-member") {
    if (id === "owner") return;
    await deleteMemberLocal(id);
    await queueOutboxLocal({ id: `ob_${uid()}`, collection: "workspace_members", docId: id, operation: "DELETE" });
    if (state.preferences.activeMemberId === id) {
      state.preferences = { ...state.preferences, activeMemberId: "owner" };
      await savePreferencesLocal(state.preferences);
    }
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم حذف عضو الفريق");
  }
  else if (action === "lock-app-now") { state.isPinLocked = true; state.pinError = ""; render(); }
  else if (action === "disable-pin") {
    state.preferences = { ...state.preferences, pinEnabled: false, pinHash: "", pinSalt: "" };
    await savePreferencesLocal(state.preferences);
    state.isPinLocked = false;
    render();
    showToast("تم تعطيل رمز الحماية PIN");
  }
  else if (action === "request-persistent-storage") {
    const granted = await requestPersistentStorage();
    state.storageQuota = await getStorageQuotaEstimate();
    render();
    showToast(granted ? "تم تفعيل حماية التخزين الدائم ✓" : "المتصفح يدير التخزين تلقائياً حسب المساحة");
  }
  else if (action === "wipe-all-data") {
    if (!confirm("تحذير: سيتم مسح جميع الديون والعملاء والدفعات نهائياً! هل أنت متأكد؟")) return;
    const curUser = state.cloudUser;
    if (curUser?.uid) {
      await deleteAllCloudUserData(curUser.uid).catch(() => {});
      await deleteCurrentAuthUserSafely().catch(() => {});
      await signOutCloud().catch(() => {});
      state.cloudUser = null;
    }
    await clearAllLocalData();
    Object.assign(state, await loadAllLocalState());
    state.tab = "dashboard";
    render();
    showToast("تم مسح جميع البيانات المحلية والسحابية");
  }
  else if (action === "toggle-ai-consent" || action === "activate-ai-now") {
    const next = action === "activate-ai-now" ? true : !state.preferences.aiConsentGranted;
    state.preferences = { ...state.preferences, aiConsentGranted: next };
    await savePreferencesLocal(state.preferences);
    if (state.cloudUser) ensureCloudUserProfile(state.cloudUser, state.preferences).catch(() => {});
    if (next && state.aiPendingQuestion) {
      const q = state.aiPendingQuestion;
      state.aiPendingQuestion = "";
      runAiPrompt(q);
    } else {
      render();
      if (action === "activate-ai-now") showToast("تم تفعيل المساعد المالي الذكي ✓");
    }
  }
  else if (action === "ask-ai-prompt") { handleAiSubmission(val); }
  else if (action === "clear-ai-chat") { state.aiMessages = []; state.aiPendingQuestion = ""; render(); }
  else if (action === "share-voucher-wa") { const d = state.modal?.data; if (d) openWhatsAppShare(d.partyPhone || "", d.whatsappMessage || ""); }
  else if (action === "share-voucher-sms") { const d = state.modal?.data; if (d) openSmsComposer(d.partyPhone || "", d.whatsappMessage || ""); }
  else if (action === "test-wa-preview") {
    const p = state.preferences;
    const msg = buildFormattedWhatsAppMessage(p, {
      title: "سند إشعار مالي",
      contactName: state.contacts[0]?.fullName || "أحمد محمد",
      remainingFormatted: `100,000 ${p.defaultCurrency}`,
      receiptNumber: "DBT-202601"
    });
    openWhatsAppShare("", msg);
  }
  else if (action === "clear-logo") { state.preferences = { ...state.preferences, printLogoDataUrl: "" }; await savePreferencesLocal(state.preferences); render(); }
  else if (action === "send-debt-wa" || action === "send-debt-sms" || action === "relay-send-debt") {
    const d = state.debts.find((x) => x.id === id);
    const c = state.contacts.find((x) => x.id === d?.contactId);
    if (!d) return;
    const tpl = state.templates.find((t) => t.type === (d.dueAtMillis < Date.now() ? "OVERDUE" : "DUE_SOON"));
    const msg = tpl
      ? renderTemplateText(tpl.bodyTemplate, {
          name: d.contactName,
          amount: formatMinor(d.principalMinor, d.currencyCode, currencies),
          remaining: formatMinor(d.remainingMinor, d.currencyCode, currencies),
          reason: d.reason,
          date: formatDate(d.dueAtMillis)
        })
      : buildFormattedWhatsAppMessage(state.preferences, {
          title: "سند مطالبة وتذكير مالي",
          contactName: d.contactName,
          directionLabel: getDirectionLabelAr(d.direction),
          principalFormatted: formatMinor(d.principalMinor, d.currencyCode, currencies),
          paidFormatted: formatMinor(d.principalMinor - d.remainingMinor, d.currencyCode, currencies),
          remainingFormatted: formatMinor(d.remainingMinor, d.currencyCode, currencies),
          recordedDateLabel: formatDate(d.recordedAtMillis),
          dueDateLabel: formatDate(d.dueAtMillis),
          reason: d.reason,
          receiptNumber: `DBT-${d.id.slice(-6).toUpperCase()}`
        });
    if (action === "send-debt-wa") openWhatsAppShare(c?.phone || "", msg);
    else if (action === "send-debt-sms") openSmsComposer(c?.phone || "", msg);
    else {
      if (!c?.phone) return showToast("رقم الهاتف غير مسجل لهذا الشخص");
      try {
        const channel = state.preferences.whatsappCloudEnabled ? "WHATSAPP" : "SMS";
        await sendViaBackendRelay({
          baseUrl: state.preferences.backendBaseUrl,
          channel,
          toPhone: c.phone,
          messageBody: msg,
          templateType: "DEBT_REMINDER",
          phoneNumberId: state.preferences.whatsappPhoneNumberId,
          senderId: state.preferences.smsSenderId
        });
        showToast("تم إرسال الإشعار عبر الخادم الآمن بنجاح ✓");
      } catch (err) {
        showToast(err.message || "فشل الإرسال الآلي");
      }
    }
  }
  else if (action === "share-statement-wa" || action === "share-statement-sms") {
    const data = buildContactStatementData(state, id, state.statementPeriodDays || 0);
    if (!data) return;
    const summaryText = data.currencySummaries.map((cs) => `${cs.code}: مستحق لي ${formatMinor(cs.recRemaining, cs.code, currencies)} | مستحق عليّ ${formatMinor(cs.payRemaining, cs.code, currencies)}`).join("\n");
    const msg = `كشف حساب: *${data.contact.fullName}*\n${summaryText || "لا توجد أرصدة مفتوحة"}`;
    if (action === "share-statement-wa") openWhatsAppShare(data.contact.phone || "", msg);
    else openSmsComposer(data.contact.phone || "", msg);
  }
  else if (action === "print-debt-voucher") {
    const d = state.debts.find((x) => x.id === id);
    if (!d) return;
    printInvoiceSheet(state.preferences, {
      docKind: "DEBT_VOUCHER",
      title: `سند قيد دين #${d.id.slice(-6).toUpperCase()}`,
      subtitle: `الطرف: ${d.contactName}`,
      summary: [
        ["العميل", d.contactName],
        ["أصل المبلغ", formatMinor(d.principalMinor, d.currencyCode, currencies)],
        ["المتبقي", formatMinor(d.remainingMinor, d.currencyCode, currencies)],
        ["الاستحقاق", formatDate(d.dueAtMillis)]
      ],
      rows: [{ date: formatDate(d.recordedAtMillis), title: d.reason, amount: formatMinor(d.principalMinor, d.currencyCode, currencies), status: getStatusLabelAr(d.status) }]
    });
  }
  else if (action === "print-payment-receipt" || action === "share-payment-wa") {
    const p = state.payments.find((x) => x.id === id);
    if (!p) return;
    const d = state.debts.find((x) => x.id === p.debtId);
    const c = state.contacts.find((x) => x.id === p.contactId);
    if (action === "share-payment-wa") {
      const msg = buildFormattedWhatsAppMessage(state.preferences, {
        title: "سند إيصال استلام دفعة",
        contactName: d?.contactName || c?.fullName || "عميل",
        paidFormatted: formatMinor(p.amountMinor, p.currencyCode, currencies),
        remainingFormatted: formatMinor(p.balanceAfterMinor, p.currencyCode, currencies),
        recordedDateLabel: formatDate(p.paidAtMillis),
        receiptNumber: p.receiptNumber
      });
      openWhatsAppShare(c?.phone || "", msg);
    } else {
      printInvoiceSheet(state.preferences, {
        docKind: "PAYMENT_RECEIPT",
        title: `إيصال استلام دفعة ${p.receiptNumber}`,
        subtitle: `العميل: ${d?.contactName || c?.fullName || "عميل"}`,
        summary: [
          ["المبلغ المدفوع", formatMinor(p.amountMinor, p.currencyCode, currencies)],
          ["الخصم", formatMinor(p.discountMinor || 0, p.currencyCode, currencies)],
          ["الرصيد قبل", formatMinor(p.balanceBeforeMinor, p.currencyCode, currencies)],
          ["الرصيد بعد", formatMinor(p.balanceAfterMinor, p.currencyCode, currencies)]
        ],
        rows: [{ date: formatDate(p.paidAtMillis), title: `دفعة (${getPaymentMethodLabelAr(p.method)})`, amount: formatMinor(p.amountMinor, p.currencyCode, currencies), status: p.balanceAfterMinor === 0 ? "مسدد" : "جزئي" }]
      });
    }
  }
  else if (action === "print-contact-statement") {
    const stData = buildContactStatementData(state, id, state.statementPeriodDays || 0);
    if (!stData) return;
    printInvoiceSheet(state.preferences, {
      docKind: "STATEMENT",
      title: `كشف حساب: ${stData.contact.fullName}`,
      subtitle: `الهاتف: ${stData.contact.phone || "غير مسجل"}`,
      summary: [["عدد الحركات", String(stData.timeline.length)]],
      rows: stData.timeline
    });
  }
  else if (action === "print-active-report") {
    const doc = buildReportDocument(state, state.reportKind || "POSITION");
    printInvoiceSheet(state.preferences, doc);
  }
  else if (action === "export-report-csv") {
    const doc = buildReportDocument(state, state.reportKind || "POSITION");
    downloadCsvFile(`hisabati-report-${Date.now()}.csv`, doc.exportDebts);
    showToast("تم تصدير ملف Excel CSV بنجاح");
  }
  else if (action === "export-statement-csv") {
    const stData = buildContactStatementData(state, id, state.statementPeriodDays || 0);
    if (stData) {
      downloadCsvFile(`hisabati-statement-${Date.now()}.csv`, stData.debts);
      showToast("تم تصدير كشف الحساب CSV");
    }
  }
  else if (action === "export-backup") {
    const json = await exportFullBackupJson();
    const blob = new Blob([json], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `hisabati-backup-${Date.now()}.json`;
    a.click();
    showToast("تم تصدير النسخة الاحتياطية بنجاح");
  }
  else if (action === "export-wa-config") {
    const json = await exportWhatsAppConfigJson();
    const blob = new Blob([json], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `hisabati-whatsapp-templates-${Date.now()}.json`;
    a.click();
    showToast("تم تصدير القوالب بنجاح");
  }
  else if (action === "sign-in-cloud") {
    try {
      const user = await signInWithGoogleCloud();
      if (user) {
        await handleCloudUserAuthenticated(user);
        showToast(`أهلاً ${user.displayName || ""} — تم ربط الحساب والمزامنة ✓`);
      }
    } catch (err) {
      showToast(err.message || "تعذر تسجيل الدخول بحساب Google");
    }
  }
  else if (action === "sync-cloud") {
    await syncAllWithCloud({ silent: false });
  }
  else if (action === "sign-out-cloud") {
    if (realtimeUnsub) {
      try { realtimeUnsub(); } catch {}
      realtimeUnsub = null;
    }
    await signOutCloud();
    state.cloudUser = null;
    render();
    showToast("تم تسجيل الخروج من الحساب السحابي");
  }
});

document.addEventListener("input", (e) => {
  if (e.target?.dataset?.input === "search-query") {
    state.searchQuery = e.target.value;
    render();
  }
});

document.addEventListener("change", async (e) => {
  const inputType = e.target?.dataset?.input;
  const changeType = e.target?.dataset?.change;

  if (changeType === "statement-contact") { state.statementContactId = e.target.value; render(); }
  else if (changeType === "select-template") { state.editingTemplateId = e.target.value; render(); }
  else if (changeType === "switch-active-member") {
    state.preferences = { ...state.preferences, activeMemberId: e.target.value };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم تبديل المشغّل النشط");
  }
  else if (inputType === "upload-logo" && e.target.files?.[0]) {
    state.preferences = { ...state.preferences, printLogoDataUrl: await compressImageFile(e.target.files[0], 320) };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم حفظ الشعار");
  }
  else if (inputType === "contact-avatar" && e.target.files?.[0]) {
    state.pendingAvatarDataUrl = await compressImageFile(e.target.files[0], 260);
    showToast("تم تجهيز صورة الشخص ✓");
  }
  else if (inputType === "debt-attachment" && e.target.files?.[0]) {
    state.pendingDebtAttachmentUrl = await compressImageFile(e.target.files[0], 720);
    showToast("تم إرفاق صورة السند ✓");
  }
  else if (inputType === "import-backup" && e.target.files?.[0]) {
    try {
      const text = await e.target.files[0].text();
      await importFullBackupJson(text);
      Object.assign(state, await loadAllLocalState());
      render();
      showToast("تم استيراد النسخة الاحتياطية بنجاح ✓");
    } catch {
      showToast("ملف النسخة الاحتياطية غير صالح");
    }
  }
  else if (inputType === "import-wa-config" && e.target.files?.[0]) {
    try {
      const text = await e.target.files[0].text();
      const res = await importWhatsAppConfigJson(text, activeUid());
      for (const tpl of res.importedTemplates) {
        await queueOutboxLocal({ id: `ob_${uid()}`, collection: "message_templates", docId: tpl.id, operation: "UPSERT", payload: tpl });
      }
      Object.assign(state, await loadAllLocalState());
      render();
      showToast("تم استيراد قوالب وإعدادات واتساب ✓");
    } catch {
      showToast("ملف القوالب غير صالح");
    }
  }
});

document.addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target;
  const kind = form?.dataset?.form;
  if (!kind || state.isSaving) return;
  const fd = new FormData(form);
  const currencies = state.currencies || [];

  if (kind === "unlock-pin") {
    const ok = await verifyPinWeb(String(fd.get("pin") || ""), state.preferences.pinHash, state.preferences.pinSalt);
    if (ok) {
      state.isPinLocked = false;
      state.pinError = "";
      render();
    } else {
      state.pinError = "رمز الحماية غير صحيح";
      render();
    }
    return;
  }

  if (kind === "set-pin") {
    const pin = String(fd.get("pin") || "").trim();
    if (!/^\d{4,6}$/.test(pin)) return showToast("أدخل 4 إلى 6 أرقام فقط");
    const salt = generatePinSalt();
    const hash = await hashPinWeb(pin, salt);
    state.preferences = { ...state.preferences, pinEnabled: true, pinHash: hash, pinSalt: salt };
    await savePreferencesLocal(state.preferences);
    form.reset();
    render();
    showToast("تم حفظ رمز الحماية PIN بنجاح ✓");
    return;
  }

  if (kind === "ask-ai-form") {
    const q = String(fd.get("question") || "").trim();
    if (q) { form.reset(); handleAiSubmission(q); }
    return;
  }

  if (kind === "print-settings") {
    state.preferences = {
      ...state.preferences,
      printTemplate: String(fd.get("printTemplate") || "CLASSIC_FORMAL"),
      printCompanyName: String(fd.get("printCompanyName") || "").trim(),
      printCompanyPhone: String(fd.get("printCompanyPhone") || "").trim(),
      printCompanyAddress: String(fd.get("printCompanyAddress") || "").trim(),
      printTaxOrCrNumber: String(fd.get("printTaxOrCrNumber") || "").trim(),
      printFooterNote: String(fd.get("printFooterNote") || "").trim() || DEFAULT_PREFS.printFooterNote,
      printShowLogoInHeader: fd.has("printShowLogoInHeader"),
      printShowInmaSoftInHeader: fd.has("printShowInmaSoftInHeader"),
      printShowInmaSoftInFooter: fd.has("printShowInmaSoftInFooter"),
      printShowSignatureBox: fd.has("printShowSignatureBox"),
      printShowWatermarkBadge: fd.has("printShowWatermarkBadge"),
      printDebtColorHex: String(fd.get("printDebtColorHex") || "#0B1B32"),
      printReceiptColorHex: String(fd.get("printReceiptColorHex") || "#065F46"),
      printStatementColorHex: String(fd.get("printStatementColorHex") || "#1E3A8A"),
      printReportColorHex: String(fd.get("printReportColorHex") || "#115E59")
    };
    await savePreferencesLocal(state.preferences);
    state.modal = null;
    render();
    showToast("تم حفظ إعدادات الفواتير والطباعة");
    return;
  }

  if (kind === "workspace-form") {
    state.preferences = {
      ...state.preferences,
      workspaceName: String(fd.get("workspaceName") || "").trim() || DEFAULT_PREFS.workspaceName,
      accountType: String(fd.get("accountType") || "PERSONAL")
    };
    await savePreferencesLocal(state.preferences);
    if (state.cloudUser) ensureCloudUserProfile(state.cloudUser, state.preferences).catch(() => {});
    render();
    showToast("تم تحديث مساحة العمل");
    return;
  }

  if (kind === "custom-theme") {
    const name = String(fd.get("themeName") || "").trim();
    const primaryHex = String(fd.get("primaryHex") || "#0B1B32");
    const accentHex = String(fd.get("accentHex") || "#10B981");
    if (!name) return;
    const nextThemes = [...(state.preferences.customThemes || []), { id: `thm_${uid()}`, name, primaryHex, accentHex }];
    state.preferences = { ...state.preferences, themePalette: "custom", customPrimaryHex: primaryHex, customAccentHex: accentHex, customThemes: nextThemes };
    await savePreferencesLocal(state.preferences);
    render();
    showToast(`تم حفظ وتطبيق ثيم ${name}`);
    return;
  }

  if (kind === "wa-settings") {
    state.preferences = {
      ...state.preferences,
      whatsappIncludeWorkspaceHeader: fd.has("whatsappIncludeWorkspaceHeader"),
      whatsappIncludeFinancialBreakdown: fd.has("whatsappIncludeFinancialBreakdown"),
      whatsappIncludeDates: fd.has("whatsappIncludeDates"),
      whatsappIncludeReasonAndNotes: fd.has("whatsappIncludeReasonAndNotes"),
      whatsappIncludeFooterSignature: fd.has("whatsappIncludeFooterSignature"),
      whatsappCustomSignature: String(fd.get("whatsappCustomSignature") || "").trim() || DEFAULT_PREFS.whatsappCustomSignature
    };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم حفظ تنسيق رسائل واتساب");
    return;
  }

  if (kind === "messaging-relay") {
    state.preferences = {
      ...state.preferences,
      whatsappCloudEnabled: fd.has("whatsappCloudEnabled"),
      smsGatewayEnabled: fd.has("smsGatewayEnabled"),
      backendBaseUrl: String(fd.get("backendBaseUrl") || "").trim(),
      whatsappPhoneNumberId: String(fd.get("whatsappPhoneNumberId") || "").trim(),
      smsSenderId: String(fd.get("smsSenderId") || "HISABATI").trim() || "HISABATI"
    };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم حفظ إعدادات بوابة الإرسال الآلي");
    return;
  }

  if (kind === "reminder-prefs") {
    state.preferences = {
      ...state.preferences,
      notificationsEnabled: fd.has("notificationsEnabled"),
      reminderPriorityHigh: fd.has("reminderPriorityHigh"),
      defaultReminderHour: Math.max(0, Math.min(23, Math.trunc(Number(fd.get("defaultReminderHour")) || 9))),
      defaultReminderMinute: Math.max(0, Math.min(59, Math.trunc(Number(fd.get("defaultReminderMinute")) || 0)))
    };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم حفظ تفضيلات التذكيرات");
    return;
  }

  state.isSaving = true;
  render();
  try {
    if (kind === "email-signin") {
      const user = await signInWithEmailCloud(fd.get("email"), fd.get("password"));
      await handleCloudUserAuthenticated(user);
      showToast("تم تسجيل الدخول والمزامنة السحابية ✓");
    } else if (kind === "email-signup") {
      const user = await signUpWithEmailCloud(fd.get("email"), fd.get("password"), fd.get("displayName"));
      await handleCloudUserAuthenticated(user);
      showToast("تم إنشاء الحساب وربط المزامنة بنجاح ✓");
    } else if (kind === "email-reset") {
      await sendPasswordResetCloud(fd.get("email"));
      state.authMode = "signin";
      showToast("تم إرسال رابط استعادة كلمة المرور لبريدك الإلكتروني ✓");
    } else if (kind === "save-contact") {
      const existingId = form.dataset.contactId;
      const existing = existingId ? state.contacts.find((c) => c.id === existingId) : null;
      const now = Date.now();
      const contact = {
        id: existing?.id || `cnt_${uid()}`,
        userId: activeUid(),
        fullName: String(fd.get("fullName") || "").trim(),
        phone: String(fd.get("phone") || "").trim(),
        address: String(fd.get("address") || "").trim() || null,
        notes: String(fd.get("notes") || "").trim() || null,
        avatarUri: state.pendingAvatarDataUrl || existing?.avatarUri || null,
        createdAtMillis: existing?.createdAtMillis || now,
        updatedAtMillis: now
      };
      await saveContactLocal(contact);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "contacts", docId: contact.id, operation: "UPSERT", payload: contact });
      await recordAudit(existing ? "تعديل بيانات شخص" : "إضافة شخص", "CONTACT", contact.id, contact.fullName);
      state.pendingAvatarDataUrl = "";
      Object.assign(state, await loadAllLocalState());
      state.modal = null;
      showToast(`تم حفظ ${contact.fullName}`);
    } else if (kind === "add-debt") {
      const contactId = String(fd.get("contactId") || "");
      const contact = state.contacts.find((c) => c.id === contactId);
      const minor = parseMajorToMinor(fd.get("amount"));
      const curCode = String(fd.get("currencyCode") || state.preferences.defaultCurrency || "YER");
      const instCount = Math.max(0, Math.min(36, Math.trunc(Number(fd.get("installmentCount")) || 0)));
      if (!contact || !minor || minor <= 0) {
        showToast("يرجى إدخال مبلغ صحيح");
        return;
      }
      const now = Date.now();
      const recordedAtMillis = new Date(String(fd.get("recordedDate"))).getTime() || now;
      const dueAtMillis = new Date(String(fd.get("dueDate"))).getTime() || now + 30 * 86_400_000;
      const debtId = `dbt_${uid()}`;
      const debt = {
        id: debtId,
        userId: activeUid(),
        contactId: contact.id,
        contactName: contact.fullName,
        direction: String(fd.get("direction") || "RECEIVABLE"),
        principalMinor: minor,
        remainingMinor: minor,
        currencyCode: curCode,
        exchangeRateMicros: getCurrencyRateMicros(curCode, currencies),
        recordedAtMillis,
        dueAtMillis,
        reason: String(fd.get("reason") || "").trim() || "معاملة مالية",
        paymentMethod: String(fd.get("paymentMethod") || "CASH"),
        attachmentUri: state.pendingDebtAttachmentUrl || null,
        notes: String(fd.get("notes") || "").trim() || null,
        status: instCount > 1 ? "SCHEDULED" : (dueAtMillis < now ? "OVERDUE" : "OPEN"),
        version: 1
      };
      const installments = buildInstallmentSchedule({
        debtId,
        userId: debt.userId,
        principalMinor: minor,
        installmentCount: instCount,
        recordedAtMillis,
        dueAtMillis,
        nowMillis: now
      });
      await saveDebtLocal(debt);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: debt.id, operation: "UPSERT", payload: debt });
      if (installments.length) {
        await saveInstallmentsLocal(installments);
        for (const ins of installments) {
          await queueOutboxLocal({ id: `ob_${uid()}`, collection: "installments", docId: ins.id, operation: "UPSERT", payload: ins });
        }
      }
      // Auto schedule reminder 24h before due date
      const autoRem = {
        id: `rem_auto_${debt.id}`,
        userId: activeUid(),
        debtId: debt.id,
        contactId: contact.id,
        title: `تذكير استحقاق: ${contact.fullName}`,
        message: `${getDirectionLabelAr(debt.direction)}: ${formatMinor(minor, curCode, currencies)} (${debt.reason})`,
        triggerAtMillis: Math.max(now, dueAtMillis - 86_400_000),
        repeatType: "ONCE",
        priority: state.preferences.reminderPriorityHigh !== false ? "HIGH" : "NORMAL",
        isEnabled: true,
        isSent: false,
        updatedAtMillis: now
      };
      await saveReminderLocal(autoRem);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "reminders", docId: autoRem.id, operation: "UPSERT", payload: autoRem });
      await emitNotification("تم تسجيل قيد مالي جديد", `${contact.fullName} — ${formatMinor(minor, curCode, currencies)} (${debt.reason})`, "DEBT_CREATED", debt.id);
      await recordAudit("إنشاء دين", "DEBT", debt.id, `${contact.fullName} • ${formatMinor(minor, curCode, currencies)}`);

      state.pendingDebtAttachmentUrl = "";
      Object.assign(state, await loadAllLocalState());
      await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });

      const voucherNo = `DBT-${debt.id.slice(-6).toUpperCase()}`;
      const waMsg = buildFormattedWhatsAppMessage(state.preferences, {
        title: "سند إشعار قيد مالي",
        contactName: contact.fullName,
        directionLabel: getDirectionLabelAr(debt.direction),
        principalFormatted: formatMinor(minor, debt.currencyCode, currencies),
        paidFormatted: formatMinor(0, debt.currencyCode, currencies),
        remainingFormatted: formatMinor(minor, debt.currencyCode, currencies),
        recordedDateLabel: formatDate(recordedAtMillis),
        dueDateLabel: formatDate(dueAtMillis),
        reason: debt.reason,
        notes: debt.notes,
        receiptNumber: voucherNo
      });
      state.modal = {
        type: "voucher",
        data: {
          title: "تم تسجيل الدين بنجاح",
          voucherNumber: voucherNo,
          partyName: contact.fullName,
          partyPhone: contact.phone || "",
          amountFormatted: formatMinor(minor, debt.currencyCode, currencies),
          remainingFormatted: formatMinor(minor, debt.currencyCode, currencies),
          whatsappMessage: waMsg
        }
      };
    } else if (kind === "edit-debt") {
      const debt = state.debts.find((d) => d.id === form.dataset.debtId);
      const editReason = String(fd.get("editReason") || "").trim();
      if (!debt || !editReason) return showToast("يرجى كتابة سبب التعديل");
      const newDue = new Date(String(fd.get("dueDate"))).getTime() || debt.dueAtMillis;
      const updated = {
        ...debt,
        dueAtMillis: newDue,
        reason: String(fd.get("reason") || debt.reason).trim(),
        notes: String(fd.get("notes") || "").trim() || null,
        version: (debt.version || 1) + 1
      };
      await saveDebtLocal(updated);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: updated.id, operation: "UPSERT", payload: updated });
      await recordAudit("تعديل قيد دين", "DEBT", updated.id, `تحديث الاستحقاق/البيان لـ ${updated.contactName}`, editReason);
      Object.assign(state, await loadAllLocalState());
      state.modal = { type: "debt-detail", data: { debtId: updated.id } };
      showToast("تم تعديل القيد وتوثيقه في سجل التدقيق ✓");
    } else if (kind === "pay-debt") {
      const debt = state.debts.find((d) => d.id === form.dataset.debtId);
      const contact = state.contacts.find((c) => c.id === debt?.contactId);
      const payMinor = parseMajorToMinor(fd.get("amount")) || 0;
      const discountMinor = parseMajorToMinor(fd.get("discount")) || 0;
      const isFinalSettlement = fd.has("isFinalSettlement");
      const totalReduction = payMinor + discountMinor;
      if (!debt || totalReduction <= 0 || totalReduction > debt.remainingMinor) {
        showToast("المبلغ والخصم يجب أن يكونا أكبر من صفر ولا يتجاوزا المتبقي");
        return;
      }
      const paidAtMillis = new Date(String(fd.get("paidDate"))).getTime() || Date.now();
      const balanceAfter = isFinalSettlement ? 0 : Math.max(0, debt.remainingMinor - totalReduction);
      const effectiveDiscount = isFinalSettlement ? debt.remainingMinor - payMinor : discountMinor;
      const updatedDebt = {
        ...debt,
        remainingMinor: balanceAfter,
        status: balanceAfter === 0 ? (isFinalSettlement || effectiveDiscount > 0 ? "SETTLED" : "PAID") : "PARTIAL",
        version: (debt.version || 1) + 1
      };
      const payment = {
        id: `pay_${uid()}`,
        userId: activeUid(),
        debtId: debt.id,
        contactId: debt.contactId,
        receiptNumber: `HSB-${Date.now().toString().slice(-8)}`,
        amountMinor: payMinor,
        discountMinor: Math.max(0, effectiveDiscount),
        currencyCode: debt.currencyCode,
        method: String(fd.get("method") || "CASH"),
        paidAtMillis,
        balanceBeforeMinor: debt.remainingMinor,
        balanceAfterMinor: balanceAfter,
        notes: String(fd.get("notes") || "").trim() || null,
        idempotencyKey: `idem_${uid()}`
      };
      const debtInsts = state.installments.filter((ins) => ins.debtId === debt.id);
      const modifiedInsts = allocatePaymentToInstallments(debtInsts, debt.remainingMinor - balanceAfter, paidAtMillis);
      await savePaymentAndDebtLocal(payment, updatedDebt, modifiedInsts);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "payments", docId: payment.id, operation: "CREATE", payload: payment });
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: updatedDebt.id, operation: "UPSERT", payload: updatedDebt });
      for (const ins of modifiedInsts) {
        await queueOutboxLocal({ id: `ob_${uid()}`, collection: "installments", docId: ins.id, operation: "UPSERT", payload: ins });
      }
      await emitNotification("تم تسجيل دفعة مالية", `${debt.contactName} — ${formatMinor(payMinor, debt.currencyCode, currencies)} (إيصال ${payment.receiptNumber})`, "PAYMENT_RECORDED", debt.id);
      await recordAudit("تسجيل دفعة", "PAYMENT", payment.id, `${debt.contactName} • مدفوع ${formatMinor(payMinor, debt.currencyCode, currencies)}`);

      Object.assign(state, await loadAllLocalState());
      const waMsg = buildFormattedWhatsAppMessage(state.preferences, {
        title: "سند إشعار باستلام دفعة",
        contactName: debt.contactName,
        directionLabel: getDirectionLabelAr(debt.direction),
        principalFormatted: formatMinor(debt.principalMinor, debt.currencyCode, currencies),
        paidFormatted: formatMinor(payMinor, debt.currencyCode, currencies),
        remainingFormatted: formatMinor(balanceAfter, debt.currencyCode, currencies),
        recordedDateLabel: formatDate(paidAtMillis),
        dueDateLabel: formatDate(debt.dueAtMillis),
        reason: debt.reason,
        notes: payment.notes,
        receiptNumber: payment.receiptNumber
      });
      state.modal = {
        type: "voucher",
        data: {
          title: "تم تسجيل الدفعة بنجاح",
          voucherNumber: payment.receiptNumber,
          partyName: debt.contactName,
          partyPhone: contact?.phone || "",
          amountFormatted: formatMinor(payMinor, debt.currencyCode, currencies),
          remainingFormatted: formatMinor(balanceAfter, debt.currencyCode, currencies),
          whatsappMessage: waMsg
        }
      };
    } else if (kind === "reschedule-installment") {
      const ins = state.installments.find((i) => i.id === form.dataset.installmentId);
      const newAmount = parseMajorToMinor(fd.get("amount"));
      const reason = String(fd.get("reason") || "").trim();
      if (!ins || !newAmount || !reason) return showToast("أدخل المبلغ وسبب الجدولة");
      const updated = {
        ...ins,
        amountMinor: newAmount,
        dueAtMillis: new Date(String(fd.get("dueDate"))).getTime() || ins.dueAtMillis,
        notes: reason,
        updatedAtMillis: Date.now()
      };
      await saveInstallmentLocal(updated);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "installments", docId: updated.id, operation: "UPSERT", payload: updated });
      await recordAudit("إعادة جدولة قسط", "INSTALLMENT", updated.id, `قسط #${updated.installmentNumber} بمبلغ ${newAmount}`, reason);
      Object.assign(state, await loadAllLocalState());
      state.modal = { type: "debt-detail", data: { debtId: updated.debtId } };
      showToast("تمت إعادة جدولة القسط ✓");
    } else if (kind === "add-reminder") {
      const now = Date.now();
      const days = Math.max(0, Math.trunc(Number(fd.get("daysAhead")) || 0));
      const debtIdVal = String(fd.get("debtId") || "general") || "general";
      const linkedDebt = state.debts.find((d) => d.id === debtIdVal);
      const rem = {
        id: `rem_${uid()}`,
        userId: activeUid(),
        debtId: linkedDebt ? linkedDebt.id : "general",
        contactId: linkedDebt ? linkedDebt.contactId : "general",
        title: String(fd.get("title") || "تذكير مالي").trim(),
        message: String(fd.get("message") || "تذكير بسداد المستحقات").trim(),
        triggerAtMillis: now + days * 86_400_000,
        repeatType: String(fd.get("repeatType") || "ONCE"),
        priority: String(fd.get("priority") || "NORMAL"),
        isEnabled: true,
        isSent: false,
        createdAtMillis: now,
        updatedAtMillis: now
      };
      await saveReminderLocal(rem);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "reminders", docId: rem.id, operation: "UPSERT", payload: rem });
      Object.assign(state, await loadAllLocalState());
      state.modal = null;
      showToast("تم حفظ التذكير المخصص ✓");
      if (days === 0) await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
    } else if (kind === "add-currency") {
      const code = String(fd.get("code") || "").trim().toUpperCase();
      const symbol = String(fd.get("symbol") || "").trim();
      const nameAr = String(fd.get("nameAr") || "").trim();
      const rateUsd = Number(fd.get("rateToUsd") || 0);
      if (!code || !symbol || !nameAr || rateUsd <= 0) return showToast("بيانات العملة غير مكتملة");
      const curObj = {
        id: `cur_${code}`,
        userId: activeUid(),
        code,
        nameAr,
        symbol,
        rateToUsdMicros: Math.max(1, Math.round(rateUsd * 1_000_000)),
        isCustom: true,
        updatedAtMillis: Date.now()
      };
      await saveCurrencyLocal(curObj);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "currencies", docId: curObj.id, operation: "UPSERT", payload: curObj });
      Object.assign(state, await loadAllLocalState());
      form.reset();
      showToast(`تمت إضافة العملة ${nameAr} (${code}) ✓`);
    } else if (kind === "save-template") {
      const tplId = String(fd.get("id") || "");
      const existing = state.templates.find((t) => t.id === tplId);
      if (existing) {
        const updated = {
          ...existing,
          userId: activeUid(),
          titleAr: String(fd.get("titleAr") || existing.titleAr).trim(),
          channel: String(fd.get("channel") || existing.channel),
          bodyTemplate: String(fd.get("bodyTemplate") || existing.bodyTemplate),
          updatedAtMillis: Date.now()
        };
        await saveTemplateLocal(updated);
        await queueOutboxLocal({ id: `ob_${uid()}`, collection: "message_templates", docId: updated.id, operation: "UPSERT", payload: updated });
        Object.assign(state, await loadAllLocalState());
        showToast("تم حفظ تعديل القالب ✓");
      }
    } else if (kind === "add-member") {
      const now = Date.now();
      const mem = {
        id: `mem_${uid()}`,
        userId: activeUid(),
        displayName: String(fd.get("displayName") || "").trim(),
        email: String(fd.get("email") || "").trim(),
        role: String(fd.get("role") || "STAFF"),
        canAddDebt: fd.has("canAddDebt"),
        canEditDebt: fd.has("canEditDebt"),
        canRecordPayment: fd.has("canRecordPayment"),
        canViewReports: fd.has("canViewReports"),
        invitedAtMillis: now
      };
      await saveMemberLocal(mem);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "workspace_members", docId: mem.id, operation: "UPSERT", payload: mem });
      await recordAudit("إضافة عضو فريق", "WORKSPACE_MEMBER", mem.id, `${mem.displayName} (${mem.role})`);
      Object.assign(state, await loadAllLocalState());
      form.reset();
      showToast(`تمت إضافة ${mem.displayName} للفريق ✓`);
    }
  } catch (err) {
    showToast(err?.message || "حدث خطأ أثناء تنفيذ العملية");
  } finally {
    state.isSaving = false;
    render();
  }
});

window.addEventListener("online", () => {
  render();
  syncAllWithCloud({ silent: true });
});
window.addEventListener("offline", render);
window.matchMedia?.("(prefers-color-scheme: dark)")?.addEventListener?.("change", render);

(async function boot() {
  const local = await loadAllLocalState();
  Object.assign(state, local);
  if (state.preferences.pinEnabled && state.preferences.pinHash) {
    state.isPinLocked = true;
  }
  render();

  await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
  setInterval(() => {
    runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
  }, 15 * 60_000);

  // Restore redirect sign-in result (e.g. iOS Safari) and listen continuously to Firebase Auth session
  const redirectUser = await handleAuthRedirectResult();
  if (redirectUser) {
    await handleCloudUserAuthenticated(redirectUser);
  }
  await subscribeAuthState(async (user) => {
    if (user && state.cloudUser?.uid !== user.uid) {
      await handleCloudUserAuthenticated(user);
    } else if (!user && state.cloudUser) {
      if (realtimeUnsub) {
        try { realtimeUnsub(); } catch {}
        realtimeUnsub = null;
      }
      state.cloudUser = null;
      render();
    }
  });
})();
