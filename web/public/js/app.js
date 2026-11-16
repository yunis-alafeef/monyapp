import {
  convertMinor,
  DEFAULT_CURRENCIES,
  escapeHtml,
  formatDate,
  formatMinor,
  formatSignedMinor,
  minorToPlain,
  parseMajorToMinor
} from "./money.js";
import {
  clearOutboxItemLocal,
  DEFAULT_PREFS,
  deleteReminderLocal,
  exportFullBackupJson,
  importFullBackupJson,
  loadAllLocalState,
  markAllNotificationsReadLocal,
  queueOutboxLocal,
  saveContactLocal,
  saveDebtLocal,
  saveInstallmentsLocal,
  saveNotificationsBatchLocal,
  savePaymentAndDebtLocal,
  savePreferencesLocal,
  saveReminderLocal
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
import { buildFormattedWhatsAppMessage, openWhatsAppShare } from "./whatsapp.js";
import {
  PDF_COLOR_SWATCHES,
  PDF_DOC_KINDS,
  PDF_TEMPLATES,
  printInvoiceSheet
} from "./print.js";
import {
  initFirebaseSafely,
  pullCloudSnapshot,
  pushOutboxToCloud,
  signInWithGoogleCloud,
  signOutCloud
} from "./cloud.js";

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

// Clean Material-style Vector Icons (1:1 visual language with Android Icons.Rounded)
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
  back: `<svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M10 6 8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"/></svg>`,
  verified: `<svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1 3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></svg>`
};

const PALETTES = [
  { id: "royal", nameAr: "الملكي الزمردي" },
  { id: "azure", nameAr: "الأزرق الساطع" },
  { id: "teal", nameAr: "الفيروزي العصري" },
  { id: "amethyst", nameAr: "الأرجواني الفاخر" },
  { id: "ruby", nameAr: "الياقوتي الدافئ" },
  { id: "gold", nameAr: "الليلي الذهبي" }
];

const state = {
  tab: "dashboard", // dashboard | debts | contacts | more | notifications | reminders | whatsapp | settings | search
  debtFilter: "ALL",
  searchQuery: "",
  contacts: [],
  debts: [],
  payments: [],
  installments: [],
  reminders: [],
  notifications: [],
  outbox: [],
  preferences: { ...DEFAULT_PREFS },
  cloudUser: null,
  modal: null,
  isSaving: false,
  toast: null
};

let toastTimer = null;
function showToast(msg) {
  state.toast = msg;
  render();
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    state.toast = null;
    render();
  }, 3000);
}

function uid() {
  return (crypto?.randomUUID?.() || `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`).replace(/-/g, "");
}

function applyThemeAttributes() {
  const root = document.documentElement;
  root.setAttribute("data-theme", state.preferences.themeMode === "DARK" ? "dark" : "light");
  root.setAttribute("data-palette", state.preferences.themePalette || "royal");
}

function computeMetrics() {
  const cur = state.preferences.defaultCurrency || "YER";
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
    const conv = convertMinor(d.remainingMinor || 0, d.currencyCode || cur, cur);
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
    const conv = convertMinor(p.amountMinor || 0, p.currencyCode || cur, cur);
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
    monthlySeries: buildMonthlySeries(state.debts, state.payments, cur, now)
  };
}

function statusLabelAr(status) {
  switch (status) {
    case "PARTIAL": return "سداد جزئي";
    case "SCHEDULED": return "مجدول بالأقساط";
    case "OVERDUE": return "متأخر";
    case "PAID": return "مدفوع بالكامل";
    case "SETTLED": return "تمت التسوية";
    case "PENDING": return "قيد الانتظار";
    default: return "مفتوح";
  }
}

async function runDueNotificationCheck({ dispatchOs = true, manualFeedback = false } = {}) {
  const result = evaluateDueNotifications({
    debts: state.debts,
    installments: state.installments,
    reminders: state.reminders,
    existingNotifications: state.notifications,
    prefs: state.preferences,
    nowMillis: Date.now()
  });

  if (result.updatedInstallments.length) {
    await saveInstallmentsLocal(result.updatedInstallments);
  }
  for (const rem of result.updatedReminders) {
    await saveReminderLocal(rem);
  }
  if (result.newNotifications.length) {
    await saveNotificationsBatchLocal(result.newNotifications);
    if (dispatchOs) {
      for (const n of result.newNotifications.slice(0, 3)) {
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
    const refreshed = await loadAllLocalState();
    Object.assign(state, refreshed);
    render();
  }

  if (manualFeedback) {
    if (result.newNotifications.length) {
      showToast(`تم فحص المستحقات وإصدار ${result.newNotifications.length} تنبيه`);
    } else {
      showToast("تم الفحص: لا توجد أقساط أو ديون جديدة مستحقة");
    }
  }
  return result.newNotifications.length;
}

function renderTopBar() {
  const unread = state.notifications.filter((n) => !n.isRead).length;
  const userName = state.cloudUser?.displayName || "بك";
  const accountLabel = state.preferences.accountType === "BUSINESS" ? "تجاري" : "شخصي";
  const workspace = state.preferences.printCompanyName || state.preferences.workspaceName || "محفظتي الشخصية";
  const pendingCount = state.outbox.length;

  return `
    <header class="top-bar">
      <div class="brand-group">
        <img src="./icons/icon-192.png" alt="شعار حساباتي" class="brand-logo" />
        <div style="min-width:0;">
          <h1 class="brand-title">مرحباً، ${escapeHtml(userName)}</h1>
          <p class="brand-sub">${escapeHtml(workspace)} • ${accountLabel} ${navigator.onLine ? "" : "• أوفلاين"}</p>
        </div>
      </div>
      <div class="top-actions">
        <button class="icon-btn" data-action="go-tab" data-val="settings" title="الثيمات والإعدادات" style="color:var(--primary);">${ICONS.palette}</button>
        <button class="icon-btn" data-action="sync-cloud" title="حالة المزامنة" style="color:${pendingCount > 0 ? "var(--warning)" : "var(--positive)"};">
          ${pendingCount > 0 ? ICONS.cloudWait : ICONS.cloudOk}
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
  const now = Date.now();
  const upcomingDebts = state.debts
    .filter((d) => (d.remainingMinor || 0) > 0 && d.status !== "PAID" && d.status !== "SETTLED")
    .sort((a, b) => (a.dueAtMillis || 0) - (b.dueAtMillis || 0))
    .slice(0, 5);
  const recentPayments = state.payments.slice(0, 4);
  const overdueInstCount = state.installments.filter(
    (ins) => (ins.amountMinor - ins.paidMinor) > 0 && ins.dueAtMillis <= now + 48 * 3600_000
  ).length;
  const iosNotInstalledBanner = isIosDevice() && !isStandalonePwa();

  return `
    <section class="hero-card">
      <div class="row-between">
        <span class="hero-label">صافي المركز المالي</span>
        <button class="currency-pill" data-action="open-currency-picker">C • ${escapeHtml(m.currency)}</button>
      </div>
      <div class="hero-value" style="color:${m.netPosition >= 0 ? "#6EE7B7" : "#FFA5B4"};">
        ${formatSignedMinor(m.netPosition, m.currency)}
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <div class="hero-label">↓ مستحق لي</div>
          <div style="font-weight:800;font-size:14.5px;">${formatMinor(m.totalReceivable, m.currency)}</div>
        </div>
        <div class="hero-stat">
          <div class="hero-label">↑ مستحق عليّ</div>
          <div style="font-weight:800;font-size:14.5px;">${formatMinor(m.totalPayable, m.currency)}</div>
        </div>
      </div>
      <div class="hero-actions">
        <button class="btn btn-hero-emerald" data-action="open-add-debt">${ICONS.add}<span>إضافة دين</span></button>
        <button class="btn btn-hero-glass" data-action="quick-payment">${ICONS.pay}<span>تسجيل دفعة</span></button>
      </div>
    </section>

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

    <!-- Compact 2x3 KPI Grid matching Android DashboardScreen.kt -->
    <div class="kpi-grid">
      <div class="kpi-tile">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(14,159,110,0.14);color:var(--positive);">${ICONS.wallet}</span>
        </div>
        <div class="kpi-title">إجمالي المحصّل</div>
        <div class="kpi-val">${formatMinor(m.totalCollected, m.currency)}</div>
      </div>
      <div class="kpi-tile">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(59,130,246,0.14);color:var(--info);">${ICONS.pay}</span>
        </div>
        <div class="kpi-title">إجمالي المسدّد</div>
        <div class="kpi-val">${formatMinor(m.totalPaidOut, m.currency)}</div>
      </div>
      <div class="kpi-tile card-clickable" data-action="open-overdue-debts">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(225,29,72,0.14);color:var(--negative);">${ICONS.warn}</span>
          <span class="kpi-sub">${m.overdueCount} دين</span>
        </div>
        <div class="kpi-title">الديون المتأخرة</div>
        <div class="kpi-val text-neg">${formatMinor(m.overdueAmount, m.currency)}</div>
      </div>
      <div class="kpi-tile">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(217,119,6,0.14);color:var(--warning);">${ICONS.clock}</span>
          <span class="kpi-sub">${m.dueSoonCount} خلال 7 أيام</span>
        </div>
        <div class="kpi-title">اقترب استحقاقها</div>
        <div class="kpi-val">${formatMinor(m.dueSoonAmount, m.currency)}</div>
      </div>
      <div class="kpi-tile card-clickable" data-action="switch-tab" data-val="contacts">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:var(--primary-soft);color:var(--primary);">${ICONS.groups}</span>
        </div>
        <div class="kpi-title">عدد الأشخاص</div>
        <div class="kpi-val">${m.contactsCount}</div>
      </div>
      <div class="kpi-tile card-clickable" data-action="switch-tab" data-val="debts">
        <div class="kpi-top">
          <span class="tile-icon-box" style="background:rgba(16,185,129,0.14);color:var(--accent);">${ICONS.receipt}</span>
        </div>
        <div class="kpi-title">عدد المعاملات</div>
        <div class="kpi-val">${m.transactionsCount}</div>
      </div>
    </div>

    <!-- Interactive Charts matching Android MonthlyCashflowChart & Donut -->
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
      <button class="btn btn-outline btn-sm" data-action="print-summary-report">${ICONS.print}<span>تقرير PDF</span></button>
    </div>

    <div class="list-stack">
      ${recentPayments.length ? recentPayments.map((p) => {
        const d = state.debts.find((x) => x.id === p.debtId);
        return `
          <div class="card card-clickable" data-action="open-debt-detail" data-id="${p.debtId}">
            <div class="row-between">
              <div style="display:flex;align-items:center;gap:10px;">
                <span class="tile-icon-box" style="border-radius:50%;background:rgba(14,159,110,0.14);color:var(--positive);">${ICONS.receipt}</span>
                <div>
                  <strong>${escapeHtml(d?.contactName || "دفعة مالية")}</strong>
                  <div class="text-muted">إيصال ${escapeHtml(p.receiptNumber)} • ${formatDate(p.paidAtMillis)}</div>
                </div>
              </div>
              <strong class="text-pos">${formatMinor(p.amountMinor, p.currencyCode)}</strong>
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
  const dirAr = d.direction === "RECEIVABLE" ? "مستحق لي" : "مستحق عليّ";
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
          <div class="text-muted">${escapeHtml(d.reason)} ${instList.length > 1 ? `• ${instList.length} أقساط` : ""}</div>
        </div>
        <span class="status-badge st-${effectiveStatus}">${statusLabelAr(effectiveStatus)}</span>
      </div>
      <div class="row-between" style="margin-top:8px;">
        <div>
          <div class="text-muted">المتبقي</div>
          <strong style="font-size:15px;color:${dirColor};">${formatMinor(d.remainingMinor, d.currencyCode)}</strong>
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
        <button class="btn btn-outline btn-sm" data-action="print-debt-voucher" data-id="${d.id}">${ICONS.print}<span>PDF</span></button>
      </div>
    </div>
  `;
}

function renderDebtsTab() {
  const now = Date.now();
  const q = state.searchQuery.trim().toLowerCase();
  const filtered = state.debts.filter((d) => {
    const matchTab =
      state.debtFilter === "ALL" ||
      (state.debtFilter === "RECEIVABLE" && d.direction === "RECEIVABLE") ||
      (state.debtFilter === "PAYABLE" && d.direction === "PAYABLE") ||
      (state.debtFilter === "OVERDUE" && d.remainingMinor > 0 && d.dueAtMillis < now && d.status !== "PAID" && d.status !== "SETTLED") ||
      (state.debtFilter === "INSTALLMENTS" && state.installments.some((ins) => ins.debtId === d.id));
    const matchSearch = !q || d.contactName.toLowerCase().includes(q) || (d.reason || "").toLowerCase().includes(q);
    return matchTab && matchSearch;
  });

  return `
    <div class="row-between">
      <strong>سجل الديون والأقساط (${filtered.length})</strong>
      <button class="btn btn-primary btn-sm" data-action="open-add-debt">${ICONS.add}<span>دين جديد</span></button>
    </div>
    <input class="input" type="search" placeholder="ابحث باسم الشخص أو البيان..." value="${escapeHtml(state.searchQuery)}" data-input="search-query" />
    <div class="tabs-row">
      ${[["ALL", "الكل"], ["RECEIVABLE", "مستحق لي"], ["PAYABLE", "مستحق عليّ"], ["OVERDUE", "متأخر"], ["INSTALLMENTS", "مقسط"]]
        .map(([k, lbl]) => `<button class="chip ${state.debtFilter === k ? "active" : ""}" data-action="set-debt-filter" data-val="${k}">${lbl}</button>`)
        .join("")}
    </div>
    <div class="list-stack">
      ${filtered.length ? filtered.map(renderDebtCard).join("") : `<div class="card text-muted" style="text-align:center;">لا توجد قيود مطابقة</div>`}
    </div>
  `;
}

function renderContactsTab() {
  const cur = state.preferences.defaultCurrency || "YER";
  return `
    <div class="row-between">
      <strong>الأشخاص والعملاء (${state.contacts.length})</strong>
      <button class="btn btn-primary btn-sm" data-action="open-add-contact">${ICONS.add}<span>إضافة شخص</span></button>
    </div>
    <div class="list-stack">
      ${state.contacts.length ? state.contacts.map((c) => {
        const contactDebts = state.debts.filter((d) => d.contactId === c.id);
        const rec = contactDebts.filter((d) => d.direction === "RECEIVABLE")
          .reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode, cur), 0);
        const pay = contactDebts.filter((d) => d.direction === "PAYABLE")
          .reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode, cur), 0);
        return `
          <div class="card">
            <div class="row-between">
              <div>
                <strong>${escapeHtml(c.fullName)}</strong>
                <div class="text-muted">${escapeHtml(c.phone || "بدون هاتف")} ${c.address ? `• ${escapeHtml(c.address)}` : ""}</div>
              </div>
              <div style="display:flex;gap:6px;">
                <button class="btn btn-outline btn-sm" data-action="open-add-debt" data-id="${c.id}">+ دين</button>
                <button class="btn btn-outline btn-sm" data-action="print-contact-statement" data-id="${c.id}">كشف PDF</button>
              </div>
            </div>
            <div class="row-between" style="margin-top:8px;">
              <span class="text-pos" style="font-weight:700;">له: ${formatMinor(rec, cur)}</span>
              <span class="text-neg" style="font-weight:700;">عليه: ${formatMinor(pay, cur)}</span>
              ${c.phone ? `<a class="btn btn-outline btn-sm" href="tel:${escapeHtml(c.phone)}">اتصال</a>` : ""}
            </div>
          </div>
        `;
      }).join("") : `<div class="card text-muted" style="text-align:center;padding:20px;">لم تقم بإضافة أشخاص بعد.</div>`}
    </div>
  `;
}

function renderMoreHub() {
  const unread = state.notifications.filter((n) => !n.isRead).length;
  const items = [
    { tab: "notifications", title: `الإشعارات (${unread})`, sub: "تنبيهات الأقساط والديون", color: "var(--warning)", icon: ICONS.bell },
    { tab: "reminders", title: "التذكيرات المحلية", sub: "جدولة وإعداد الإشعارات", color: "var(--negative)", icon: ICONS.clock },
    { action: "open-print-settings", title: "الطباعة والفواتير PDF", sub: "قوالب والشعار والألوان", color: "var(--accent)", icon: ICONS.print },
    { tab: "settings", title: "الثيمات والعملة", sub: "6 ثيمات وتبديل (C)", color: "var(--primary)", icon: ICONS.palette },
    { tab: "whatsapp", title: "إعدادات واتساب", sub: "تنسيق الرسائل والتصدير", color: "var(--positive)", icon: ICONS.chat },
    { action: "print-summary-report", title: "التقرير المالي PDF", sub: "ملخص شامل للطباعة", color: "var(--info)", icon: ICONS.receipt },
    { tab: "search", title: "البحث الشامل", sub: "البحث في الديون والأشخاص", color: "var(--positive)", icon: ICONS.search },
    { action: "open-ios-guide", title: "تثبيت الآيفون وإشعاراته", sub: "Safari + الشاشة الرئيسية", color: "var(--info)", icon: ICONS.verified }
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
          ? "⚠️ على الآيفون (iOS 16.4+): متصفح Safari العادي لا يدعم الإشعارات إلا بعد تثبيت التطبيق: اضغط زر المشاركة ⬆ ثم «إضافة إلى الشاشة الرئيسية» وافتح التطبيق من أيقونته."
          : ios && standalone
          ? "📱 تنبيه هام لمستخدمي الآيفون: الإشعارات المحلية تعمل فور فتح التطبيق وأثناء استخدامه (إشعار نظام + تنبيهات الأقساط والديون). أما إشعارات الخلفية والتطبيق مغلق تماماً في الآيفون فتتطلب اتصال إنترنت وخادم Web Push لأن نظام iOS يوقف مؤقتات الخلفية عند إغلاق الـ PWA."
          : "✅ يمكنك تفعيل إشعارات النظام لتصلك تنبيهات الأقساط والديون المستحقة فور فحصها."
        }
      </p>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:8px;">
        ${perm !== "granted" && perm !== "unsupported"
          ? `<button class="btn btn-primary btn-sm" data-action="request-notif-perm">تفعيل إشعارات الجهاز</button>`
          : ""}
        <button class="btn btn-outline btn-sm" data-action="check-due-now">فحص الأقساط والديون الآن</button>
        <button class="btn btn-positive btn-sm" data-action="send-test-notif">إرسال إشعار تجريبي</button>
      </div>
    </div>

    <div class="card">
      <div class="switch-row">
        <div>
          <strong>تفعيل الفحص التلقائي للاستحقاقات</strong>
          <div class="text-muted">فحص الأقساط والديون المتأخرة والتي تستحق خلال 48 ساعة عند تشغيل التطبيق</div>
        </div>
        <button class="chip ${state.preferences.notificationsEnabled !== false ? "active" : ""}" data-action="toggle-notif-enabled">
          ${state.preferences.notificationsEnabled !== false ? "مفعّل" : "معطّل"}
        </button>
      </div>
    </div>

    <strong>التذكيرات المخصصة المجدولة (${state.reminders.length})</strong>
    <div class="list-stack">
      ${state.reminders.length ? state.reminders.map((r) => `
        <div class="card">
          <div class="row-between">
            <div>
              <strong>${escapeHtml(r.title)}</strong>
              <div class="text-muted">${escapeHtml(r.message)}</div>
              <div style="font-size:11px;color:var(--positive);margin-top:3px;">
                موعد التنبيه: ${formatDate(r.triggerAtMillis)} • ${r.isSent ? "تم التنبيه" : "نشط"}
              </div>
            </div>
            <button class="btn btn-danger btn-sm" data-action="delete-reminder" data-id="${r.id}">حذف</button>
          </div>
        </div>
      `).join("") : `<div class="card text-muted">لا توجد تذكيرات مخصصة حالياً. يمكنك إضافة تذكير خاص لأي دين أو قسط.</div>`}
    </div>
  `;
}

function renderWhatsAppSettingsPage() {
  const p = state.preferences;
  const preview = buildFormattedWhatsAppMessage(p, {
    title: "سند إشعار مالي",
    contactName: state.contacts[0]?.fullName || "أحمد محمد",
    directionLabel: "مستحق لي",
    principalFormatted: `150,000 ${p.defaultCurrency}`,
    paidFormatted: `50,000 ${p.defaultCurrency}`,
    remainingFormatted: `100,000 ${p.defaultCurrency}`,
    recordedDateLabel: formatDate(Date.now()),
    dueDateLabel: formatDate(Date.now() + 15 * 86_400_000),
    reason: "فاتورة مبيعات رقم 104",
    receiptNumber: "DBT-202601"
  });

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>إعدادات وتنسيق رسائل واتساب</strong>
      </div>
    </div>
    <form class="card" data-form="wa-settings">
      <label class="field-label">أسلوب الرسالة</label>
      <div class="tabs-row">
        ${[["FORMAL", "رسمي متوازن"], ["FRIENDLY", "ودي لطيف"], ["URGENT", "تنبيه عاجل"]]
          .map(([k, l]) => `<button type="button" class="chip ${p.whatsappStyle === k ? "active" : ""}" data-action="set-wa-style" data-val="${k}">${l}</button>`)
          .join("")}
      </div>
      <label class="switch-row">
        <span>تضمين اسم المتجر والترويسة</span>
        <input type="checkbox" name="whatsappIncludeWorkspaceHeader" ${p.whatsappIncludeWorkspaceHeader !== false ? "checked" : ""} />
      </label>
      <label class="switch-row">
        <span>تضمين الملخص المالي (الأصل/المسدد/المتبقي)</span>
        <input type="checkbox" name="whatsappIncludeFinancialBreakdown" ${p.whatsappIncludeFinancialBreakdown !== false ? "checked" : ""} />
      </label>
      <label class="switch-row">
        <span>تضمين تواريخ التسجيل والاستحقاق</span>
        <input type="checkbox" name="whatsappIncludeDates" ${p.whatsappIncludeDates !== false ? "checked" : ""} />
      </label>
      <label class="switch-row">
        <span>تضمين البيان والسبب</span>
        <input type="checkbox" name="whatsappIncludeReasonAndNotes" ${p.whatsappIncludeReasonAndNotes !== false ? "checked" : ""} />
      </label>
      <label class="field-label">التوقيع الختامي</label>
      <input class="input" name="whatsappCustomSignature" value="${escapeHtml(p.whatsappCustomSignature)}" />
      <button type="submit" class="btn btn-primary" style="width:100%;margin-top:10px;">حفظ تنسيق واتساب</button>
    </form>

    <div class="card">
      <strong>معاينة حية للرسالة</strong>
      <pre style="white-space:pre-wrap;font-family:inherit;background:var(--surface-alt);padding:10px;border-radius:10px;font-size:12px;">${escapeHtml(preview)}</pre>
      <button class="btn btn-positive btn-sm" data-action="test-wa-preview">مشاركة المعاينة عبر واتساب</button>
    </div>
  `;
}

function renderSettingsPage() {
  const p = state.preferences;
  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>الثيمات والإعدادات العامة</strong>
      </div>
    </div>

    <div class="card">
      <strong>المظهر والثيمات والعملة الافتراضية</strong>
      <label class="field-label">الوضع</label>
      <div class="tabs-row">
        <button class="chip ${p.themeMode === "LIGHT" ? "active" : ""}" data-action="set-theme-mode" data-val="LIGHT">فاتح ساطع</button>
        <button class="chip ${p.themeMode === "DARK" ? "active" : ""}" data-action="set-theme-mode" data-val="DARK">داكن فاخر</button>
      </div>
      <label class="field-label">لوحة الألوان (6 ثيمات احترافية)</label>
      <div class="tabs-row">
        ${PALETTES.map((pal) => `<button class="chip ${p.themePalette === pal.id ? "active" : ""}" data-action="set-palette" data-val="${pal.id}">${pal.nameAr}</button>`).join("")}
      </div>
      <label class="field-label">العملة الافتراضية (C)</label>
      <div class="tabs-row">
        ${DEFAULT_CURRENCIES.map((c) => `<button class="chip ${p.defaultCurrency === c.code ? "active" : ""}" data-action="set-currency" data-val="${c.code}">${c.nameAr} (${c.symbol})</button>`).join("")}
      </div>
    </div>

    <div class="card">
      <strong>مساحة العمل</strong>
      <form data-form="workspace-form">
        <label class="field-label">اسم المتجر / المحفظة</label>
        <input class="input" name="workspaceName" value="${escapeHtml(p.workspaceName)}" />
        <label class="field-label">نوع الحساب</label>
        <select class="select" name="accountType">
          <option value="PERSONAL" ${p.accountType === "PERSONAL" ? "selected" : ""}>شخصي</option>
          <option value="BUSINESS" ${p.accountType === "BUSINESS" ? "selected" : ""}>تجاري</option>
        </select>
        <button type="submit" class="btn btn-primary btn-sm" style="margin-top:10px;">حفظ مساحة العمل</button>
      </form>
    </div>

    <div class="card">
      <strong>المزامنة السحابية والنسخ الاحتياطي</strong>
      <p class="text-muted">تُحفظ جميع البيانات والأقساط والتنبيهات محلياً (IndexedDB) وتعمل بدون إنترنت.</p>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:8px;">
        ${state.cloudUser
          ? `<button class="btn btn-primary btn-sm" data-action="sync-cloud">مزامنة الآن (${escapeHtml(state.cloudUser.email || "")})</button>
             <button class="btn btn-outline btn-sm" data-action="sign-out-cloud">تسجيل خروج</button>`
          : `<button class="btn btn-primary btn-sm" data-action="sign-in-cloud">ربط ومزامنة بحساب Google</button>`}
        <button class="btn btn-outline btn-sm" data-action="export-backup">تصدير نسخة JSON</button>
        <label class="btn btn-outline btn-sm" style="cursor:pointer;">
          استيراد JSON
          <input type="file" accept="application/json" style="display:none;" data-input="import-backup" />
        </label>
      </div>
    </div>
  `;
}

function renderSearchPage() {
  const q = state.searchQuery.trim().toLowerCase();
  const matchedDebts = !q ? state.debts.slice(0, 8) : state.debts.filter((d) =>
    d.contactName.toLowerCase().includes(q) || (d.reason || "").toLowerCase().includes(q)
  );
  const matchedContacts = !q ? [] : state.contacts.filter((c) =>
    c.fullName.toLowerCase().includes(q) || (c.phone || "").includes(q)
  );

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="dashboard">رجوع</button>
        <strong>البحث الشامل</strong>
      </div>
    </div>
    <input class="input" type="search" placeholder="ابحث عن عميل، رقم هاتف، دين، أو بيان..." value="${escapeHtml(state.searchQuery)}" data-input="search-query" autofocus />
    ${matchedContacts.length ? `
      <strong>الأشخاص (${matchedContacts.length})</strong>
      <div class="list-stack">
        ${matchedContacts.map((c) => `<div class="card"><strong>${escapeHtml(c.fullName)}</strong><div class="text-muted">${escapeHtml(c.phone || "")}</div></div>`).join("")}
      </div>
    ` : ""}
    <strong>القيود المالية (${matchedDebts.length})</strong>
    <div class="list-stack">
      ${matchedDebts.length ? matchedDebts.map(renderDebtCard).join("") : `<div class="card text-muted">لا توجد نتائج مطابقة</div>`}
    </div>
  `;
}

function renderModal() {
  if (!state.modal) return "";
  const { type, data } = state.modal;

  if (type === "currency-picker") {
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between"><strong>تغيير العملة الافتراضية (C)</strong><button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <div class="list-stack" style="margin-top:10px;">
            ${DEFAULT_CURRENCIES.map((c) => {
              const sel = c.code === state.preferences.defaultCurrency;
              return `
                <button class="btn ${sel ? "btn-primary" : "btn-outline"}" style="justify-content:space-between;" data-action="set-currency-and-close" data-val="${c.code}">
                  <span>${c.nameAr} (${c.code} - ${c.symbol})</span>
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
            <li>من <strong>المزيد ← التذكيرات المحلية</strong> اضغط <strong>تفعيل إشعارات الجهاز</strong> للسماح بإشعارات iOS 16.4+.</li>
          </ol>
          <p class="text-muted">ملاحظة تقنية: على الآيفون تصلك التنبيهات المحلية بمواعيد الأقساط والديون عند فتح التطبيق وأثناء استخدامه؛ أما إرسال تنبيه والتطبيق مغلق تماماً على الآيفون فيحتاج اتصال إنترنت وخادم Web Push حسب قيود نظام Apple iOS.</p>
        </div>
      </div>
    `;
  }

  if (type === "add-contact") {
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="add-contact">
          <div class="row-between"><strong>إضافة شخص / عميل جديد</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <label class="field-label">الاسم الكامل *</label>
          <input class="input" name="fullName" required placeholder="مثال: أحمد محمد علي" />
          <label class="field-label">رقم الهاتف (واتساب)</label>
          <input class="input" name="phone" type="tel" placeholder="77xxxxxxx" />
          <label class="field-label">العنوان</label>
          <input class="input" name="address" placeholder="المدينة / الحي" />
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
            ${state.isSaving ? "جاري الحفظ..." : "حفظ بيانات الشخص"}
          </button>
        </form>
      </div>
    `;
  }

  if (type === "add-debt") {
    const preContactId = data?.contactId || "";
    const contactOptions = state.contacts.map((c) => `<option value="${c.id}" ${c.id === preContactId ? "selected" : ""}>${escapeHtml(c.fullName)}</option>`).join("");
    const today = new Date().toISOString().slice(0, 10);
    const nextMonth = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
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
            <label class="field-label">نوع الدين</label>
            <select class="select" name="direction">
              <option value="RECEIVABLE">مستحق لي عند الآخرين</option>
              <option value="PAYABLE">مستحق عليّ للآخرين</option>
            </select>
            <label class="field-label">المبلغ (${escapeHtml(state.preferences.defaultCurrency)}) *</label>
            <input class="input" name="amount" type="number" step="0.01" min="0.01" required placeholder="0" />
            <label class="field-label">البيان / السبب *</label>
            <input class="input" name="reason" required placeholder="مثال: فاتورة بضاعة أو سلفة" />
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
              <div><label class="field-label">تاريخ التسجيل</label><input class="input" name="recordedDate" type="date" value="${today}" /></div>
              <div><label class="field-label">تاريخ الاستحقاق</label><input class="input" name="dueDate" type="date" value="${nextMonth}" /></div>
            </div>
            <label class="field-label">تقسيط المبلغ (عدد الأقساط: 0 بدون تقسيط أو 2 - 36)</label>
            <input class="input" name="installmentCount" type="number" min="0" max="36" value="0" />
            <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
              ${state.isSaving ? "جاري الحفظ..." : "حفظ الدين وإصدار سند الإشعار"}
            </button>
          `}
        </form>
      </div>
    `;
  }

  if (type === "pay-debt") {
    const debt = state.debts.find((d) => d.id === data.debtId);
    if (!debt) return "";
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="pay-debt" data-debt-id="${debt.id}">
          <div class="row-between"><strong>تسجيل دفعة — ${escapeHtml(debt.contactName)}</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <p class="text-muted">المتبقي الحالي: <strong>${formatMinor(debt.remainingMinor, debt.currencyCode)}</strong></p>
          <label class="field-label">المبلغ المدفوع (${escapeHtml(debt.currencyCode)}) *</label>
          <input class="input" name="amount" type="number" step="0.01" min="0.01" value="${minorToPlain(debt.remainingMinor)}" required />
          <label class="field-label">طريقة الدفع</label>
          <select class="select" name="method">
            <option value="CASH">نقدي</option>
            <option value="TRANSFER">تحويل بنكي / صرافة</option>
            <option value="CARD">بطاقة</option>
          </select>
          <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
            ${state.isSaving ? "جاري الحفظ..." : "تأكيد الدفعة وإصدار الإيصال"}
          </button>
        </form>
      </div>
    `;
  }

  if (type === "debt-detail") {
    const debt = state.debts.find((d) => d.id === data.debtId);
    if (!debt) return "";
    const instList = state.installments.filter((ins) => ins.debtId === debt.id);
    const payList = state.payments.filter((p) => p.debtId === debt.id);
    return `
      <div class="modal-backdrop">
        <div class="modal-card">
          <div class="row-between"><strong>تفاصيل القيد — ${escapeHtml(debt.contactName)}</strong><button class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <div class="card" style="background:var(--surface-alt);margin-top:8px;">
            <div class="row-between"><span>أصل الدين</span><strong>${formatMinor(debt.principalMinor, debt.currencyCode)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>المتبقي</span><strong class="text-neg">${formatMinor(debt.remainingMinor, debt.currencyCode)}</strong></div>
            <div class="row-between" style="margin-top:4px;"><span>الاستحقاق</span><strong>${formatDate(debt.dueAtMillis)}</strong></div>
            <div class="text-muted" style="margin-top:4px;">البيان: ${escapeHtml(debt.reason)}</div>
          </div>

          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:10px;">
            ${debt.remainingMinor > 0 ? `<button class="btn btn-primary btn-sm" data-action="open-pay-debt" data-id="${debt.id}">تسجيل دفعة</button>` : ""}
            <button class="btn btn-positive btn-sm" data-action="send-debt-wa" data-id="${debt.id}">إرسال واتساب</button>
            <button class="btn btn-outline btn-sm" data-action="print-debt-voucher" data-id="${debt.id}">طباعة PDF</button>
            <button class="btn btn-outline btn-sm" data-action="open-add-reminder" data-id="${debt.id}">+ تذكير</button>
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
                      <span class="status-badge st-${ins.status}">${statusLabelAr(ins.status)}</span>
                    </div>
                    <div class="row-between text-muted" style="margin-top:4px;">
                      <span>المتبقي: ${formatMinor(rem, debt.currencyCode)} / ${formatMinor(ins.amountMinor, debt.currencyCode)}</span>
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
                <div class="card" style="padding:8px 10px;">
                  <div class="row-between">
                    <strong>إيصال ${escapeHtml(p.receiptNumber)}</strong>
                    <strong class="text-pos">${formatMinor(p.amountMinor, p.currencyCode)}</strong>
                  </div>
                  <div class="text-muted">${formatDate(p.paidAtMillis)} • المتبقي بعدها ${formatMinor(p.balanceAfterMinor, p.currencyCode)}</div>
                </div>
              `).join("")}
            </div>
          ` : ""}
        </div>
      </div>
    `;
  }

  if (type === "add-reminder") {
    const debt = state.debts.find((d) => d.id === data?.debtId) || state.debts[0];
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="add-reminder">
          <div class="row-between"><strong>إنشاء تذكير محلي مخصص</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          <input type="hidden" name="debtId" value="${escapeHtml(debt?.id || "")}" />
          <label class="field-label">عنوان التذكير</label>
          <input class="input" name="title" required value="${escapeHtml(debt ? `تذكير سداد: ${debt.contactName}` : "تذكير بمتابعة الأقساط")}" />
          <label class="field-label">نص التذكير</label>
          <input class="input" name="message" required value="${escapeHtml(debt ? `متابعة تحصيل/سداد ${formatMinor(debt.remainingMinor, debt.currencyCode)} (${debt.reason})` : "يرجى متابعة مواعيد السداد المستحقة")}" />
          <label class="field-label">التذكير بعد (أيام)</label>
          <input class="input" name="daysAhead" type="number" min="0" value="0" />
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
          <button class="btn btn-positive" style="width:100%;" data-action="share-voucher-wa">${ICONS.chat}<span>إرسال سند الإشعار عبر واتساب</span></button>
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
          <label class="field-label">ألوان كل مستند</label>
          ${PDF_DOC_KINDS.map((k) => `
            <div class="row-between" style="margin-bottom:6px;">
              <span style="font-size:12px;">${escapeHtml(k.titleAr)}</span>
              <select class="select" style="width:auto;min-height:34px;" name="${k.key}">
                ${PDF_COLOR_SWATCHES.map((s) => `<option value="${s.hex}" ${p[k.key] === s.hex ? "selected" : ""}>${escapeHtml(s.nameAr)}</option>`).join("")}
              </select>
            </div>
          `).join("")}
          <label class="field-label">عبارة الفوتر أسفل الفاتورة</label>
          <input class="input" name="printFooterNote" value="${escapeHtml(p.printFooterNote)}" />
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

  const pageHtml =
    state.tab === "dashboard" ? renderDashboard() :
    state.tab === "debts" ? renderDebtsTab() :
    state.tab === "contacts" ? renderContactsTab() :
    state.tab === "notifications" ? renderNotificationsPage() :
    state.tab === "reminders" ? renderRemindersPage() :
    state.tab === "whatsapp" ? renderWhatsAppSettingsPage() :
    state.tab === "settings" ? renderSettingsPage() :
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
      ${state.toast ? `<div class="toast">${escapeHtml(state.toast)}</div>` : ""}
    </div>
  `;
}

async function syncAllWithCloud() {
  if (!state.cloudUser || !navigator.onLine) {
    if (!navigator.onLine) showToast("أنت تعمل حالياً بدون إنترنت — البيانات محفوظة محلياً");
    return;
  }
  try {
    const syncedIds = await pushOutboxToCloud(state.cloudUser.uid, state.outbox);
    for (const id of syncedIds) await clearOutboxItemLocal(id);
    const snap = await pullCloudSnapshot(state.cloudUser.uid);
    if (snap) {
      for (const c of snap.contacts) await saveContactLocal(c);
      for (const d of snap.debts) await saveDebtLocal(d);
    }
    const refreshed = await loadAllLocalState();
    Object.assign(state, refreshed);
    render();
    showToast("تمت المزامنة السحابية بنجاح");
  } catch {
    showToast("تعذرت المزامنة — تم حفظ البيانات محلياً");
  }
}

document.addEventListener("click", async (e) => {
  const target = e.target.closest("[data-action]");
  if (!target) return;
  if (target.closest("[data-stop-card]")) e.stopPropagation();

  const action = target.dataset.action;
  const val = target.dataset.val;
  const id = target.dataset.id;

  if (action === "switch-tab" || action === "go-tab") { state.tab = val; render(); }
  else if (action === "open-overdue-debts") { state.tab = "debts"; state.debtFilter = "OVERDUE"; render(); }
  else if (action === "set-debt-filter") { state.debtFilter = val; render(); }
  else if (action === "close-modal") { state.modal = null; render(); }
  else if (action === "open-currency-picker") { state.modal = { type: "currency-picker", data: {} }; render(); }
  else if (action === "open-ios-guide") { state.modal = { type: "ios-guide", data: {} }; render(); }
  else if (action === "open-add-contact") { state.modal = { type: "add-contact", data: {} }; render(); }
  else if (action === "open-add-debt") { state.modal = { type: "add-debt", data: { contactId: id || "" } }; render(); }
  else if (action === "open-pay-debt") { state.modal = { type: "pay-debt", data: { debtId: id } }; render(); }
  else if (action === "open-debt-detail") { state.modal = { type: "debt-detail", data: { debtId: id } }; render(); }
  else if (action === "open-add-reminder") { state.modal = { type: "add-reminder", data: { debtId: id || "" } }; render(); }
  else if (action === "open-print-settings") { state.modal = { type: "print-settings", data: {} }; render(); }
  else if (action === "quick-payment") {
    const firstOpen = state.debts.find((d) => d.remainingMinor > 0 && d.status !== "PAID" && d.status !== "SETTLED");
    if (firstOpen) state.modal = { type: "pay-debt", data: { debtId: firstOpen.id } };
    else state.modal = { type: "add-debt", data: {} };
    render();
  } else if (action === "set-currency-and-close") {
    state.preferences = { ...state.preferences, defaultCurrency: val };
    await savePreferencesLocal(state.preferences);
    state.modal = null;
    render();
    showToast(`العملة الافتراضية: ${val}`);
  } else if (action === "set-theme-mode") {
    state.preferences = { ...state.preferences, themeMode: val };
    await savePreferencesLocal(state.preferences);
    render();
  } else if (action === "set-palette") {
    state.preferences = { ...state.preferences, themePalette: val };
    await savePreferencesLocal(state.preferences);
    render();
  } else if (action === "set-currency") {
    state.preferences = { ...state.preferences, defaultCurrency: val };
    await savePreferencesLocal(state.preferences);
    render();
    showToast(`العملة الافتراضية: ${val}`);
  } else if (action === "set-wa-style") {
    state.preferences = { ...state.preferences, whatsappStyle: val };
    await savePreferencesLocal(state.preferences);
    render();
  } else if (action === "toggle-notif-enabled") {
    const next = state.preferences.notificationsEnabled === false;
    state.preferences = { ...state.preferences, notificationsEnabled: next };
    await savePreferencesLocal(state.preferences);
    render();
    if (next) await runDueNotificationCheck({ dispatchOs: true, manualFeedback: true });
  } else if (action === "request-notif-perm") {
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
  } else if (action === "check-due-now") {
    await runDueNotificationCheck({ dispatchOs: true, manualFeedback: true });
  } else if (action === "send-test-notif") {
    const perm = await requestNotificationPermission();
    const sent = perm === "granted" && await dispatchSystemNotification({
      title: "إشعار تجريبي — حساباتي",
      body: "تذكير محلي بمواعيد الأقساط وتحصيل الديون المستحقة يعمل بنجاح ✓",
      tag: `test_${Date.now()}`
    });
    showToast(sent ? "تم إرسال الإشعار التجريبي للجهاز ✓" : "يرجى السماح بالإشعارات أولاً");
  } else if (action === "mark-all-read") {
    await markAllNotificationsReadLocal(state.notifications);
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم تعليم جميع الإشعارات كمقروءة");
  } else if (action === "delete-reminder") {
    await deleteReminderLocal(id);
    Object.assign(state, await loadAllLocalState());
    render();
    showToast("تم حذف التذكير");
  } else if (action === "share-voucher-wa") {
    const d = state.modal?.data;
    if (d) openWhatsAppShare(d.partyPhone || "", d.whatsappMessage || "");
  } else if (action === "test-wa-preview") {
    const p = state.preferences;
    const msg = buildFormattedWhatsAppMessage(p, {
      title: "سند إشعار مالي",
      contactName: state.contacts[0]?.fullName || "أحمد محمد",
      remainingFormatted: `100,000 ${p.defaultCurrency}`,
      receiptNumber: "DBT-202601"
    });
    openWhatsAppShare("", msg);
  } else if (action === "clear-logo") {
    state.preferences = { ...state.preferences, printLogoDataUrl: "" };
    await savePreferencesLocal(state.preferences);
    render();
  } else if (action === "send-debt-wa") {
    const d = state.debts.find((x) => x.id === id);
    const c = state.contacts.find((x) => x.id === d?.contactId);
    if (!d) return;
    const msg = buildFormattedWhatsAppMessage(state.preferences, {
      title: "سند مطالبة وتذكير مالي",
      contactName: d.contactName,
      directionLabel: d.direction === "RECEIVABLE" ? "مستحق لي" : "مستحق عليّ",
      principalFormatted: formatMinor(d.principalMinor, d.currencyCode),
      paidFormatted: formatMinor(d.principalMinor - d.remainingMinor, d.currencyCode),
      remainingFormatted: formatMinor(d.remainingMinor, d.currencyCode),
      recordedDateLabel: formatDate(d.recordedAtMillis),
      dueDateLabel: formatDate(d.dueAtMillis),
      reason: d.reason,
      receiptNumber: `DBT-${d.id.slice(-6).toUpperCase()}`
    });
    openWhatsAppShare(c?.phone || "", msg);
  } else if (action === "print-debt-voucher") {
    const d = state.debts.find((x) => x.id === id);
    if (!d) return;
    printInvoiceSheet(state.preferences, {
      docKind: "DEBT_VOUCHER",
      title: `سند قيد دين #${d.id.slice(-6).toUpperCase()}`,
      subtitle: `الطرف: ${d.contactName}`,
      summary: [
        ["العميل", d.contactName],
        ["أصل المبلغ", formatMinor(d.principalMinor, d.currencyCode)],
        ["المتبقي", formatMinor(d.remainingMinor, d.currencyCode)],
        ["الاستحقاق", formatDate(d.dueAtMillis)]
      ],
      rows: [{
        date: formatDate(d.recordedAtMillis),
        title: d.reason,
        amount: formatMinor(d.principalMinor, d.currencyCode),
        status: statusLabelAr(d.status)
      }]
    });
  } else if (action === "print-contact-statement") {
    const c = state.contacts.find((x) => x.id === id);
    if (!c) return;
    const cDebts = state.debts.filter((d) => d.contactId === c.id);
    printInvoiceSheet(state.preferences, {
      docKind: "STATEMENT",
      title: `كشف حساب: ${c.fullName}`,
      subtitle: `الهاتف: ${c.phone || "غير مسجل"}`,
      summary: [["إجمالي المعاملات", String(cDebts.length)]],
      rows: cDebts.map((d) => ({
        date: formatDate(d.recordedAtMillis),
        title: d.reason,
        amount: formatMinor(d.remainingMinor, d.currencyCode),
        status: statusLabelAr(d.status)
      }))
    });
  } else if (action === "print-summary-report") {
    const m = computeMetrics();
    printInvoiceSheet(state.preferences, {
      docKind: "GENERAL_REPORT",
      title: "التقرير المالي الشامل",
      subtitle: `العملة: ${m.currency}`,
      summary: [
        ["مستحق لي", formatMinor(m.totalReceivable, m.currency)],
        ["مستحق عليّ", formatMinor(m.totalPayable, m.currency)],
        ["الصافي", formatSignedMinor(m.netPosition, m.currency)],
        ["المحصل", formatMinor(m.totalCollected, m.currency)]
      ],
      rows: state.debts.slice(0, 30).map((d) => ({
        date: formatDate(d.dueAtMillis),
        title: `${d.contactName} — ${d.reason}`,
        amount: formatMinor(d.remainingMinor, d.currencyCode),
        status: statusLabelAr(d.status)
      }))
    });
  } else if (action === "export-backup") {
    const json = await exportFullBackupJson();
    const blob = new Blob([json], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `hisabati-backup-${Date.now()}.json`;
    a.click();
    showToast("تم تصدير النسخة الاحتياطية بنجاح");
  } else if (action === "sign-in-cloud") {
    try {
      state.cloudUser = await signInWithGoogleCloud();
      await syncAllWithCloud();
    } catch (err) {
      showToast(err.message || "تعذر تسجيل الدخول");
    }
  } else if (action === "sync-cloud") {
    await syncAllWithCloud();
  } else if (action === "sign-out-cloud") {
    await signOutCloud();
    state.cloudUser = null;
    render();
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
  if (inputType === "upload-logo" && e.target.files?.[0]) {
    const reader = new FileReader();
    reader.onload = async () => {
      state.preferences = { ...state.preferences, printLogoDataUrl: String(reader.result || "") };
      await savePreferencesLocal(state.preferences);
      render();
      showToast("تم حفظ الشعار");
    };
    reader.readAsDataURL(e.target.files[0]);
  } else if (inputType === "import-backup" && e.target.files?.[0]) {
    try {
      const text = await e.target.files[0].text();
      await importFullBackupJson(text);
      Object.assign(state, await loadAllLocalState());
      render();
      showToast("تم استيراد البيانات بنجاح");
    } catch {
      showToast("ملف النسخة الاحتياطية غير صالح");
    }
  }
});

document.addEventListener("submit", async (e) => {
  e.preventDefault();
  const form = e.target;
  const kind = form?.dataset?.form;
  if (!kind || state.isSaving) return;

  const fd = new FormData(form);
  if (kind === "print-settings") {
    state.preferences = {
      ...state.preferences,
      printTemplate: String(fd.get("printTemplate") || "CLASSIC_FORMAL"),
      printCompanyName: String(fd.get("printCompanyName") || "").trim(),
      printCompanyPhone: String(fd.get("printCompanyPhone") || "").trim(),
      printCompanyAddress: String(fd.get("printCompanyAddress") || "").trim(),
      printTaxOrCrNumber: String(fd.get("printTaxOrCrNumber") || "").trim(),
      printFooterNote: String(fd.get("printFooterNote") || "").trim() || DEFAULT_PREFS.printFooterNote,
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
    render();
    showToast("تم تحديث مساحة العمل");
    return;
  }

  if (kind === "wa-settings") {
    state.preferences = {
      ...state.preferences,
      whatsappIncludeWorkspaceHeader: fd.has("whatsappIncludeWorkspaceHeader"),
      whatsappIncludeFinancialBreakdown: fd.has("whatsappIncludeFinancialBreakdown"),
      whatsappIncludeDates: fd.has("whatsappIncludeDates"),
      whatsappIncludeReasonAndNotes: fd.has("whatsappIncludeReasonAndNotes"),
      whatsappCustomSignature: String(fd.get("whatsappCustomSignature") || "").trim() || DEFAULT_PREFS.whatsappCustomSignature
    };
    await savePreferencesLocal(state.preferences);
    render();
    showToast("تم حفظ تنسيق رسائل واتساب");
    return;
  }

  state.isSaving = true;
  render();
  try {
    if (kind === "add-contact") {
      const contact = {
        id: `cnt_${uid()}`,
        userId: state.cloudUser?.uid || "local_device",
        fullName: String(fd.get("fullName") || "").trim(),
        phone: String(fd.get("phone") || "").trim(),
        address: String(fd.get("address") || "").trim() || null,
        createdAtMillis: Date.now(),
        updatedAtMillis: Date.now()
      };
      await saveContactLocal(contact);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "contacts", docId: contact.id, payload: contact });
      Object.assign(state, await loadAllLocalState());
      state.modal = null;
      showToast(`تم حفظ ${contact.fullName}`);
    } else if (kind === "add-debt") {
      const contactId = String(fd.get("contactId") || "");
      const contact = state.contacts.find((c) => c.id === contactId);
      const minor = parseMajorToMinor(fd.get("amount"));
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
        userId: state.cloudUser?.uid || "local_device",
        contactId: contact.id,
        contactName: contact.fullName,
        direction: String(fd.get("direction") || "RECEIVABLE"),
        principalMinor: minor,
        remainingMinor: minor,
        currencyCode: state.preferences.defaultCurrency || "YER",
        exchangeRateMicros: 1_000_000,
        recordedAtMillis,
        dueAtMillis,
        reason: String(fd.get("reason") || "").trim() || "معاملة مالية",
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
      if (installments.length) await saveInstallmentsLocal(installments);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: debt.id, payload: debt });
      Object.assign(state, await loadAllLocalState());
      await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });

      const voucherNo = `DBT-${debt.id.slice(-6).toUpperCase()}`;
      const waMsg = buildFormattedWhatsAppMessage(state.preferences, {
        title: "سند إشعار قيد مالي",
        contactName: contact.fullName,
        directionLabel: debt.direction === "RECEIVABLE" ? "مستحق لي" : "مستحق عليّ",
        principalFormatted: formatMinor(minor, debt.currencyCode),
        paidFormatted: formatMinor(0, debt.currencyCode),
        remainingFormatted: formatMinor(minor, debt.currencyCode),
        recordedDateLabel: formatDate(recordedAtMillis),
        dueDateLabel: formatDate(dueAtMillis),
        reason: debt.reason,
        receiptNumber: voucherNo
      });
      state.modal = {
        type: "voucher",
        data: {
          title: "تم تسجيل الدين بنجاح",
          voucherNumber: voucherNo,
          partyName: contact.fullName,
          partyPhone: contact.phone || "",
          amountFormatted: formatMinor(minor, debt.currencyCode),
          remainingFormatted: formatMinor(minor, debt.currencyCode),
          whatsappMessage: waMsg
        }
      };
    } else if (kind === "pay-debt") {
      const debt = state.debts.find((d) => d.id === form.dataset.debtId);
      const contact = state.contacts.find((c) => c.id === debt?.contactId);
      const payMinor = parseMajorToMinor(fd.get("amount"));
      if (!debt || !payMinor || payMinor <= 0 || payMinor > debt.remainingMinor) {
        showToast("المبلغ المدخل غير صالح أو يتجاوز المتبقي");
        return;
      }
      const now = Date.now();
      const balanceAfter = debt.remainingMinor - payMinor;
      const updatedDebt = {
        ...debt,
        remainingMinor: balanceAfter,
        status: balanceAfter === 0 ? "PAID" : "PARTIAL",
        version: (debt.version || 1) + 1
      };
      const payment = {
        id: `pay_${uid()}`,
        userId: state.cloudUser?.uid || "local_device",
        debtId: debt.id,
        contactId: debt.contactId,
        receiptNumber: `HSB-${now.toString().slice(-8)}`,
        amountMinor: payMinor,
        discountMinor: 0,
        currencyCode: debt.currencyCode,
        method: String(fd.get("method") || "CASH"),
        paidAtMillis: now,
        balanceBeforeMinor: debt.remainingMinor,
        balanceAfterMinor: balanceAfter,
        idempotencyKey: `idem_${uid()}`
      };
      const debtInsts = state.installments.filter((ins) => ins.debtId === debt.id);
      const modifiedInsts = allocatePaymentToInstallments(debtInsts, payMinor, now);
      await savePaymentAndDebtLocal(payment, updatedDebt, modifiedInsts);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "payments", docId: payment.id, payload: payment });
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: updatedDebt.id, payload: updatedDebt });
      Object.assign(state, await loadAllLocalState());

      const waMsg = buildFormattedWhatsAppMessage(state.preferences, {
        title: "سند إشعار باستلام دفعة",
        contactName: debt.contactName,
        directionLabel: debt.direction === "RECEIVABLE" ? "مستحق لي" : "مستحق عليّ",
        principalFormatted: formatMinor(debt.principalMinor, debt.currencyCode),
        paidFormatted: formatMinor(payMinor, debt.currencyCode),
        remainingFormatted: formatMinor(balanceAfter, debt.currencyCode),
        recordedDateLabel: formatDate(now),
        dueDateLabel: formatDate(debt.dueAtMillis),
        reason: debt.reason,
        receiptNumber: payment.receiptNumber
      });
      state.modal = {
        type: "voucher",
        data: {
          title: "تم تسجيل الدفعة بنجاح",
          voucherNumber: payment.receiptNumber,
          partyName: debt.contactName,
          partyPhone: contact?.phone || "",
          amountFormatted: formatMinor(payMinor, debt.currencyCode),
          remainingFormatted: formatMinor(balanceAfter, debt.currencyCode),
          whatsappMessage: waMsg
        }
      };
    } else if (kind === "add-reminder") {
      const now = Date.now();
      const days = Math.max(0, Math.trunc(Number(fd.get("daysAhead")) || 0));
      const rem = {
        id: `rem_${uid()}`,
        debtId: String(fd.get("debtId") || ""),
        title: String(fd.get("title") || "تذكير مالي").trim(),
        message: String(fd.get("message") || "تذكير بسداد المستحقات").trim(),
        triggerAtMillis: now + days * 86_400_000,
        repeatType: String(fd.get("repeatType") || "ONCE"),
        isEnabled: true,
        isSent: false,
        createdAtMillis: now,
        updatedAtMillis: now
      };
      await saveReminderLocal(rem);
      Object.assign(state, await loadAllLocalState());
      state.modal = null;
      showToast("تم حفظ التذكير المخصص");
      if (days === 0) await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
    }
  } finally {
    state.isSaving = false;
    render();
  }
});

window.addEventListener("online", () => {
  render();
  syncAllWithCloud();
});
window.addEventListener("offline", render);

(async function boot() {
  const local = await loadAllLocalState();
  Object.assign(state, local);
  render();

  await runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
  setInterval(() => {
    runDueNotificationCheck({ dispatchOs: true, manualFeedback: false });
  }, 15 * 60_000);

  const ctx = await initFirebaseSafely();
  if (ctx?.auth?.currentUser) {
    state.cloudUser = ctx.auth.currentUser;
    syncAllWithCloud();
  }
})();
