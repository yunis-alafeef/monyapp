import {
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
  exportFullBackupJson,
  importFullBackupJson,
  loadAllLocalState,
  queueOutboxLocal,
  saveContactLocal,
  saveDebtLocal,
  savePaymentAndDebtLocal,
  savePreferencesLocal
} from "./storage.js";
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
    navigator.serviceWorker.register("./sw.js", { scope: "./" }).catch(() => {
      // Silently ignore when running on unsupported contexts (e.g., file://)
    });
  });
}

const state = {
  tab: "dashboard",
  debtFilter: "ALL",
  searchQuery: "",
  contacts: [],
  debts: [],
  payments: [],
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
  }, 2800);
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
  let receivable = 0;
  let payable = 0;
  let overdueCount = 0;
  for (const d of state.debts) {
    if (d.currencyCode !== cur) continue;
    if (d.direction === "RECEIVABLE") receivable += d.remainingMinor || 0;
    else payable += d.remainingMinor || 0;
    if (d.remainingMinor > 0 && d.dueAtMillis < now && d.status !== "PAID" && d.status !== "SETTLED") {
      overdueCount++;
    }
  }
  const collected = state.payments
    .filter((p) => p.currencyCode === cur)
    .reduce((sum, p) => sum + (p.amountMinor || 0), 0);

  return {
    currency: cur,
    receivable,
    payable,
    net: receivable - payable,
    overdueCount,
    collected,
    contactsCount: state.contacts.length
  };
}

function statusLabelAr(status) {
  switch (status) {
    case "PARTIAL": return "سداد جزئي";
    case "OVERDUE": return "متأخر";
    case "PAID": return "مدفوع بالكامل";
    case "SETTLED": return "تمت التسوية";
    default: return "مفتوح";
  }
}

function renderTopBar() {
  const online = navigator.onLine;
  const cur = state.preferences.defaultCurrency || "YER";
  const orgName = state.preferences.printCompanyName || state.preferences.workspaceName || "حساباتي";
  return `
    <header class="top-bar">
      <div class="brand-group">
        <img src="./icons/icon-192.png" alt="شعار حساباتي" class="brand-logo" />
        <div>
          <h1 class="brand-title">${escapeHtml(orgName)}</h1>
          <p class="brand-sub">كل ديونك ومستحقاتك في مكان واحد</p>
        </div>
      </div>
      <div class="top-actions">
        <button class="badge-pill" data-action="cycle-currency" title="تبديل العملة">(C) ${escapeHtml(cur)}</button>
        <span class="net-pill ${online ? "online" : "offline"}">${online ? "متصل" : "أوفلاين"}</span>
      </div>
    </header>
  `;
}

function renderDashboard() {
  const m = computeMetrics();
  const recentDebts = state.debts.slice(0, 6);
  return `
    <section class="hero-card">
      <div class="hero-label">صافي المركز المالي (${escapeHtml(m.currency)})</div>
      <div class="hero-value">${formatSignedMinor(m.net, m.currency)}</div>
      <div class="hero-stats">
        <div class="hero-stat">
          <div class="hero-label">مستحق لي</div>
          <div style="font-weight:800;font-size:14px;">${formatMinor(m.receivable, m.currency)}</div>
        </div>
        <div class="hero-stat">
          <div class="hero-label">مستحق عليّ</div>
          <div style="font-weight:800;font-size:14px;">${formatMinor(m.payable, m.currency)}</div>
        </div>
      </div>
    </section>

    <div class="kpi-grid">
      <div class="kpi-tile"><div class="kpi-title">إجمالي التحصيل</div><div class="kpi-val text-pos">${formatMinor(m.collected, m.currency)}</div></div>
      <div class="kpi-tile"><div class="kpi-title">ديون متأخرة</div><div class="kpi-val text-neg">${m.overdueCount}</div></div>
      <div class="kpi-tile"><div class="kpi-title">العملاء والأشخاص</div><div class="kpi-val">${m.contactsCount}</div></div>
      <div class="kpi-tile"><div class="kpi-title">بانتظار المزامنة</div><div class="kpi-val">${state.outbox.length}</div></div>
    </div>

    <div class="quick-actions">
      <button class="btn btn-primary" data-action="open-add-debt">+ تسجيل دين جديد</button>
      <button class="btn btn-accent" data-action="open-add-contact">+ إضافة شخص</button>
    </div>

    <div class="row-between" style="margin-top:4px;">
      <strong>أحدث القيود المالية</strong>
      <button class="btn btn-outline btn-sm" data-action="print-summary-report">تقرير PDF</button>
    </div>

    <div class="list-stack">
      ${recentDebts.length ? recentDebts.map(renderDebtCard).join("") : `
        <div class="card text-muted" style="text-align:center;padding:20px;">
          لا توجد ديون مسجلة بعد. ابدأ بإضافة شخص ثم تسجيل دين جديد — يعمل بالكامل بدون إنترنت.
        </div>
      `}
    </div>
  `;
}

function renderDebtCard(d) {
  const dirAr = d.direction === "RECEIVABLE" ? "مستحق لي" : "مستحق عليّ";
  const now = Date.now();
  const effectiveStatus = d.remainingMinor > 0 && d.dueAtMillis < now && d.status !== "PAID" && d.status !== "SETTLED"
    ? "OVERDUE" : d.status;
  return `
    <div class="card">
      <div class="row-between">
        <div>
          <strong>${escapeHtml(d.contactName)}</strong>
          <div class="text-muted">${escapeHtml(dirAr)} • ${escapeHtml(d.reason)}</div>
        </div>
        <span class="status-badge st-${effectiveStatus}">${statusLabelAr(effectiveStatus)}</span>
      </div>
      <div class="row-between" style="margin-top:8px;">
        <div>
          <span class="text-muted">المتبقي:</span>
          <strong class="${d.direction === "RECEIVABLE" ? "text-pos" : "text-neg"}">${formatMinor(d.remainingMinor, d.currencyCode)}</strong>
          <span class="text-muted"> / أصل ${formatMinor(d.principalMinor, d.currencyCode)}</span>
        </div>
        <div class="text-muted">استحقاق: ${formatDate(d.dueAtMillis)}</div>
      </div>
      <div style="display:flex;gap:6px;margin-top:8px;flex-wrap:wrap;">
        ${d.remainingMinor > 0 ? `<button class="btn btn-primary btn-sm" data-action="open-pay-debt" data-id="${d.id}">تسجيل دفعة</button>` : ""}
        <button class="btn btn-outline btn-sm" data-action="send-debt-wa" data-id="${d.id}">واتساب</button>
        <button class="btn btn-outline btn-sm" data-action="print-debt-voucher" data-id="${d.id}">سند PDF</button>
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
      (state.debtFilter === "OVERDUE" && d.remainingMinor > 0 && d.dueAtMillis < now);
    const matchSearch = !q || d.contactName.toLowerCase().includes(q) || (d.reason || "").toLowerCase().includes(q);
    return matchTab && matchSearch;
  });

  return `
    <div class="row-between">
      <strong>سجل الديون (${filtered.length})</strong>
      <button class="btn btn-primary btn-sm" data-action="open-add-debt">+ دين جديد</button>
    </div>
    <input class="input" type="search" placeholder="ابحث باسم الشخص أو البيان..." value="${escapeHtml(state.searchQuery)}" data-input="search-query" />
    <div class="tabs-row">
      ${[["ALL", "الكل"], ["RECEIVABLE", "مستحق لي"], ["PAYABLE", "مستحق عليّ"], ["OVERDUE", "متأخر"]]
        .map(([k, lbl]) => `<button class="chip ${state.debtFilter === k ? "active" : ""}" data-action="set-debt-filter" data-val="${k}">${lbl}</button>`)
        .join("")}
    </div>
    <div class="list-stack">
      ${filtered.length ? filtered.map(renderDebtCard).join("") : `<div class="card text-muted" style="text-align:center;">لا توجد قيود مطابقة</div>`}
    </div>
  `;
}

function renderContactsTab() {
  return `
    <div class="row-between">
      <strong>الأشخاص والعملاء (${state.contacts.length})</strong>
      <button class="btn btn-primary btn-sm" data-action="open-add-contact">+ إضافة شخص</button>
    </div>
    <div class="list-stack">
      ${state.contacts.length ? state.contacts.map((c) => {
        const contactDebts = state.debts.filter((d) => d.contactId === c.id);
        const cur = state.preferences.defaultCurrency || "YER";
        const rec = contactDebts.filter((d) => d.direction === "RECEIVABLE" && d.currencyCode === cur).reduce((s, d) => s + d.remainingMinor, 0);
        const pay = contactDebts.filter((d) => d.direction === "PAYABLE" && d.currencyCode === cur).reduce((s, d) => s + d.remainingMinor, 0);
        return `
          <div class="card">
            <div class="row-between">
              <div>
                <strong>${escapeHtml(c.fullName)}</strong>
                <div class="text-muted">${escapeHtml(c.phone || "بدون هاتف")} ${c.address ? `• ${escapeHtml(c.address)}` : ""}</div>
              </div>
              <button class="btn btn-outline btn-sm" data-action="print-contact-statement" data-id="${c.id}">كشف PDF</button>
            </div>
            <div class="row-between" style="margin-top:8px;">
              <span class="text-pos">له: ${formatMinor(rec, cur)}</span>
              <span class="text-neg">عليه: ${formatMinor(pay, cur)}</span>
              ${c.phone ? `<a class="btn btn-outline btn-sm" href="tel:${escapeHtml(c.phone)}">اتصال</a>` : ""}
            </div>
          </div>
        `;
      }).join("") : `<div class="card text-muted" style="text-align:center;padding:20px;">لم تقم بإضافة أشخاص بعد.</div>`}
    </div>
  `;
}

function renderSettingsTab() {
  const p = state.preferences;
  return `
    <div class="card">
      <strong>المظهر والثيمات والعملة الافتراضية</strong>
      <label class="field-label">الوضع</label>
      <div class="tabs-row">
        <button class="chip ${p.themeMode === "LIGHT" ? "active" : ""}" data-action="set-theme-mode" data-val="LIGHT">فاتح ساطع</button>
        <button class="chip ${p.themeMode === "DARK" ? "active" : ""}" data-action="set-theme-mode" data-val="DARK">داكن فاخر</button>
      </div>
      <label class="field-label">لوحة الألوان</label>
      <div class="tabs-row">
        ${[["royal", "كحلي"], ["emerald", "زمردي"], ["indigo", "نيلي"], ["gold", "ذهبي"]]
          .map(([k, l]) => `<button class="chip ${p.themePalette === k ? "active" : ""}" data-action="set-palette" data-val="${k}">${l}</button>`)
          .join("")}
      </div>
      <label class="field-label">العملة الافتراضية</label>
      <div class="tabs-row">
        ${DEFAULT_CURRENCIES.map((c) => `<button class="chip ${p.defaultCurrency === c.code ? "active" : ""}" data-action="set-currency" data-val="${c.code}">${c.nameAr} (${c.symbol})</button>`).join("")}
      </div>
    </div>

    <div class="card">
      <div class="row-between">
        <strong>إعدادات الطباعة والفواتير PDF</strong>
        <button class="btn btn-primary btn-sm" data-action="open-print-settings">تخصيص القالب والشعار والألوان</button>
      </div>
      <div class="text-muted" style="margin-top:6px;">
        القالب النشط: ${escapeHtml(PDF_TEMPLATES.find((t) => t.id === p.printTemplate)?.titleAr || "الكلاسيكي الرسمي")}
      </div>
    </div>

    <div class="card">
      <strong>المزامنة السحابية والنسخ الاحتياطي</strong>
      <p class="text-muted">يعمل التطبيق بدون إنترنت عبر الذاكرة المحلية للآيفون (IndexedDB). يمكنك المزامنة مع حساب Google أو تصدير نسخة JSON.</p>
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

function renderModal() {
  if (!state.modal) return "";
  const { type, data } = state.modal;

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
    const contactOptions = state.contacts.map((c) => `<option value="${c.id}">${escapeHtml(c.fullName)}</option>`).join("");
    const today = new Date().toISOString().slice(0, 10);
    const nextMonth = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
    return `
      <div class="modal-backdrop">
        <form class="modal-card" data-form="add-debt">
          <div class="row-between"><strong>تسجيل دين جديد</strong><button type="button" class="btn btn-outline btn-sm" data-action="close-modal">إغلاق</button></div>
          ${!state.contacts.length ? `<p class="text-neg">يرجى إضافة شخص أولاً قبل تسجيل الدين.</p>` : `
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
            <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;" ${state.isSaving ? "disabled" : ""}>
              ${state.isSaving ? "جاري الحفظ..." : "حفظ الدين وإصدار السند"}
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
    renderSettingsTab();

  root.innerHTML = `
    <div class="app-shell">
      ${renderTopBar()}
      <main class="main-content">${pageHtml}</main>
      <div class="bottom-chrome">
        <nav class="bottom-nav">
          ${[
            ["dashboard", "🏠", "الرئيسية"],
            ["debts", "💼", "الديون"],
            ["contacts", "👥", "الأشخاص"],
            ["settings", "⚙️", "المزيد"]
          ].map(([id, icon, lbl]) => `
            <button class="nav-item ${state.tab === id ? "active" : ""}" data-action="switch-tab" data-val="${id}">
              <span class="nav-pill">${icon}</span>
              <span>${lbl}</span>
            </button>
          `).join("")}
        </nav>
        <footer class="anmasoft-footer">
          <span>عمل انما سوفت (Inma Soft)</span>
          <a href="tel:772317950">📞 772317950</a>
        </footer>
      </div>
      ${renderModal()}
      ${state.toast ? `<div class="toast">${escapeHtml(state.toast)}</div>` : ""}
    </div>
  `;
}

async function syncAllWithCloud() {
  if (!state.cloudUser || !navigator.onLine) return;
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
  const action = target.dataset.action;
  const val = target.dataset.val;
  const id = target.dataset.id;

  if (action === "switch-tab") { state.tab = val; render(); }
  else if (action === "set-debt-filter") { state.debtFilter = val; render(); }
  else if (action === "close-modal") { state.modal = null; render(); }
  else if (action === "open-add-contact") { state.modal = { type: "add-contact", data: {} }; render(); }
  else if (action === "open-add-debt") { state.modal = { type: "add-debt", data: {} }; render(); }
  else if (action === "open-pay-debt") { state.modal = { type: "pay-debt", data: { debtId: id } }; render(); }
  else if (action === "open-print-settings") { state.modal = { type: "print-settings", data: {} }; render(); }
  else if (action === "cycle-currency") {
    const codes = DEFAULT_CURRENCIES.map((c) => c.code);
    const next = codes[(codes.indexOf(state.preferences.defaultCurrency) + 1) % codes.length];
    state.preferences = { ...state.preferences, defaultCurrency: next };
    await savePreferencesLocal(state.preferences);
    render();
    showToast(`العملة الافتراضية: ${next}`);
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
        ["مستحق لي", formatMinor(m.receivable, m.currency)],
        ["مستحق عليّ", formatMinor(m.payable, m.currency)],
        ["الصافي", formatSignedMinor(m.net, m.currency)],
        ["المحصل", formatMinor(m.collected, m.currency)]
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
      if (!contact || !minor || minor <= 0) {
        showToast("يرجى إدخال مبلغ صحيح");
        return;
      }
      const debt = {
        id: `dbt_${uid()}`,
        userId: state.cloudUser?.uid || "local_device",
        contactId: contact.id,
        contactName: contact.fullName,
        direction: String(fd.get("direction") || "RECEIVABLE"),
        principalMinor: minor,
        remainingMinor: minor,
        currencyCode: state.preferences.defaultCurrency || "YER",
        exchangeRateMicros: 1_000_000,
        recordedAtMillis: new Date(String(fd.get("recordedDate"))).getTime() || Date.now(),
        dueAtMillis: new Date(String(fd.get("dueDate"))).getTime() || Date.now() + 30 * 86400000,
        reason: String(fd.get("reason") || "").trim(),
        status: "OPEN",
        version: 1
      };
      await saveDebtLocal(debt);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: debt.id, payload: debt });
      Object.assign(state, await loadAllLocalState());
      state.modal = null;
      showToast("تم حفظ الدين وإصدار السند");
    } else if (kind === "pay-debt") {
      const debt = state.debts.find((d) => d.id === form.dataset.debtId);
      const payMinor = parseMajorToMinor(fd.get("amount"));
      if (!debt || !payMinor || payMinor <= 0 || payMinor > debt.remainingMinor) {
        showToast("المبلغ المدخل غير صالح أو يتجاوز المتبقي");
        return;
      }
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
        receiptNumber: `RCP-${Date.now().toString().slice(-6)}`,
        amountMinor: payMinor,
        discountMinor: 0,
        currencyCode: debt.currencyCode,
        method: String(fd.get("method") || "CASH"),
        paidAtMillis: Date.now(),
        balanceBeforeMinor: debt.remainingMinor,
        balanceAfterMinor: balanceAfter,
        idempotencyKey: `idem_${uid()}`
      };
      await savePaymentAndDebtLocal(payment, updatedDebt);
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "payments", docId: payment.id, payload: payment });
      await queueOutboxLocal({ id: `ob_${uid()}`, collection: "debts", docId: updatedDebt.id, payload: updatedDebt });
      Object.assign(state, await loadAllLocalState());
      state.modal = null;
      showToast(`تم إصدار إيصال #${payment.receiptNumber}`);
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

  const ctx = await initFirebaseSafely();
  if (ctx?.auth?.currentUser) {
    state.cloudUser = ctx.auth.currentUser;
    syncAllWithCloud();
  }
})();
