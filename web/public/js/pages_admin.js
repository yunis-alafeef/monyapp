import {
  escapeHtml,
  formatDate,
  formatDateTime,
  mergeAllCurrencies
} from "./money.js";

const PALETTES = [
  { id: "royal", nameAr: "الملكي الزمردي" },
  { id: "azure", nameAr: "الأزرق الساطع" },
  { id: "teal", nameAr: "الفيروزي العصري" },
  { id: "amethyst", nameAr: "الأرجواني الفاخر" },
  { id: "ruby", nameAr: "الياقوتي الدافئ" },
  { id: "gold", nameAr: "الليلي الذهبي" }
];

export function renderOnboardingOverlay(state) {
  if (state.preferences?.onboardingCompleted) return "";
  const slide = Number(state.onboardingSlide || 0);
  const slides = [
    {
      badge: "١ / ٣ • دفتر الديون الذكي",
      title: "إدارة الديون والأقساط بعملات متعددة",
      body: "سجّل ما لك وما عليك بالريال اليمني والسعودي والدولار وأي عملة مخصصة مع جدولة الأقساط وتنبيهات التأخر تلقائياً."
    },
    {
      badge: "٢ / ٣ • طباعة ورسائل احترافية",
      title: "سندات PDF عربية ومشاركة واتساب و SMS",
      body: "أصدر سندات قيد وإيصالات وكشوفات حساب بـ 4 قوالب طباعة عربية وشاركها فوراً مع عملائك عبر واتساب أو SMS."
    },
    {
      badge: "٣ / ٣ • أوفلاين + مزامنة سحابية آمنة",
      title: "بياناتك محفوظة محلياً ومحمية سحابياً",
      body: "يعمل التطبيق بدون إنترنت عبر قاعدة IndexedDB في جهازك، وعند تسجيل الدخول تُزامن كل بياناتك تلقائياً مع Firebase Firestore عبر جميع أجهزتك."
    }
  ];
  const cur = slides[slide] || slides[0];
  return `
    <div class="modal-backdrop">
      <div class="modal-card" style="text-align:center;padding:22px 18px;">
        <img src="./icons/icon-192.png" alt="شعار حساباتي" style="width:68px;height:68px;border-radius:18px;border:2px solid var(--accent);" />
        <div class="chip active" style="display:inline-block;margin:10px auto 6px;">${escapeHtml(cur.badge)}</div>
        <h2 style="margin:6px 0;font-size:18px;">${escapeHtml(cur.title)}</h2>
        <p class="text-muted" style="font-size:13px;line-height:1.65;">${escapeHtml(cur.body)}</p>
        <div style="display:flex;gap:8px;justify-content:center;margin-top:16px;">
          ${slide < 2
            ? `<button class="btn btn-outline" data-action="finish-onboarding">تخطي</button>
               <button class="btn btn-primary" data-action="next-onboarding">التالي ←</button>`
            : `<button class="btn btn-positive" style="width:100%;" data-action="finish-onboarding">ابدأ استخدام حساباتي الآن ✓</button>`}
        </div>
      </div>
    </div>
  `;
}

export function renderPinLockOverlay(state) {
  if (!state.isPinLocked || !state.preferences?.pinEnabled) return "";
  return `
    <div class="pin-lock-overlay">
      <form class="modal-card" data-form="unlock-pin" style="max-width:360px;text-align:center;">
        <img src="./icons/icon-192.png" alt="قفل حساباتي" style="width:54px;height:54px;border-radius:14px;" />
        <h3 style="margin:8px 0 4px;">التطبيق مقفل برمز الحماية (PIN)</h3>
        <p class="text-muted">أدخل رمز الحماية المكون من 4 إلى 6 أرقام للمتابعة</p>
        <input class="input" name="pin" type="password" inputmode="numeric" pattern="[0-9]{4,6}" minlength="4" maxlength="6" placeholder="••••" autofocus required style="text-align:center;font-size:22px;letter-spacing:8px;margin-top:8px;" />
        ${state.pinError ? `<div class="text-neg" style="font-size:12px;margin-top:6px;">${escapeHtml(state.pinError)}</div>` : ""}
        <button type="submit" class="btn btn-primary" style="width:100%;margin-top:12px;">فتح القفل</button>
      </form>
    </div>
  `;
}

export function renderAuthPage(state) {
  const user = state.cloudUser;
  const authTab = state.authMode || "signin"; // signin | signup | recovery
  const pendingCount = state.outbox?.length || 0;

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>الحساب السحابي والمزامنة</strong>
      </div>
      <span class="chip ${user ? "active" : ""}">${user ? "متصل بالسحابة ✓" : "محلي أوفلاين"}</span>
    </div>

    ${user ? `
      <div class="card" style="border-color:var(--positive);">
        <div class="row-between">
          <div>
            <strong>${escapeHtml(user.displayName || "مستخدم حساباتي")}</strong>
            <div class="text-muted">${escapeHtml(user.email || user.uid)}</div>
            <div style="font-size:11px;color:var(--positive);margin-top:3px;">
              معرّف المزامنة: ${escapeHtml(user.uid.slice(0, 10))}... • بانتظار الرفع: ${pendingCount}
            </div>
          </div>
          <span class="status-badge st-PAID">نشط</span>
        </div>
        <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;">
          <button class="btn btn-primary btn-sm" data-action="sync-cloud">مزامنة شاملة الآن</button>
          <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="account">الأمان وسجل التدقيق</button>
          <button class="btn btn-danger btn-sm" data-action="sign-out-cloud">تسجيل خروج</button>
        </div>
      </div>
    ` : `
      <div class="card">
        <strong>ربط الموقع بقاعدة البيانات السحابية (Firebase)</strong>
        <p class="text-muted" style="margin-top:4px;">
          سجّل دخولك لمزامنة ديونك وعملائك وسجلاتك فوراً بين متصفح الويب والآيفون والأندرويد وحمايتها من الضياع.
        </p>
        <button class="btn btn-primary" style="width:100%;margin-top:8px;" data-action="sign-in-cloud">
          تسجيل الدخول بحساب Google
        </button>

        <div class="tabs-row" style="margin-top:14px;">
          <button class="chip ${authTab === "signin" ? "active" : ""}" data-action="set-auth-mode" data-val="signin">دخول بالبريد</button>
          <button class="chip ${authTab === "signup" ? "active" : ""}" data-action="set-auth-mode" data-val="signup">إنشاء حساب جديد</button>
          <button class="chip ${authTab === "recovery" ? "active" : ""}" data-action="set-auth-mode" data-val="recovery">استعادة كلمة المرور</button>
        </div>

        ${authTab === "signin" ? `
          <form data-form="email-signin">
            <label class="field-label">البريد الإلكتروني</label>
            <input class="input" name="email" type="email" required placeholder="name@example.com" />
            <label class="field-label">كلمة المرور</label>
            <input class="input" name="password" type="password" required minlength="6" placeholder="••••••" />
            <button type="submit" class="btn btn-positive" style="width:100%;margin-top:10px;" ${state.isSaving ? "disabled" : ""}>تسجيل الدخول بالبريد</button>
          </form>
        ` : authTab === "signup" ? `
          <form data-form="email-signup">
            <label class="field-label">الاسم الكامل / اسم النشاط</label>
            <input class="input" name="displayName" required placeholder="مثال: متجر البركة" />
            <label class="field-label">البريد الإلكتروني</label>
            <input class="input" name="email" type="email" required placeholder="name@example.com" />
            <label class="field-label">كلمة المرور (6 أحرف فأكثر)</label>
            <input class="input" name="password" type="password" required minlength="6" placeholder="••••••" />
            <button type="submit" class="btn btn-positive" style="width:100%;margin-top:10px;" ${state.isSaving ? "disabled" : ""}>إنشاء حساب وبدء المزامنة</button>
          </form>
        ` : `
          <form data-form="email-reset">
            <label class="field-label">البريد الإلكتروني لاستعادة الحساب</label>
            <input class="input" name="email" type="email" required placeholder="name@example.com" />
            <button type="submit" class="btn btn-primary" style="width:100%;margin-top:10px;">إرسال رابط إعادة تعيين كلمة المرور</button>
          </form>
        `}
      </div>
    `}

    <div class="card">
      <strong>أين تُخزن بياناتك في تطبيق الويب (PWA)؟</strong>
      <ul style="padding-right:18px;margin:8px 0 0;font-size:12.5px;color:var(--muted);line-height:1.7;">
        <li><strong>محلياً داخل المتصفح (IndexedDB + Cache Storage):</strong> كل الديون والعملاء والمدفوعات تُحفظ فورياً في قاعدة بيانات المتصفح الداخلية <code>hisabati_pwa_db</code> فتعمل 100% أوفلاين.</li>
        <li><strong>لماذا التخزين السحابي ضروري؟</strong> مسح بيانات المتصفح أو فقدان الهاتف أو حذف أيقونة الآيفون يحذف التخزين المحلي، بينما <strong>Firebase Firestore</strong> يحفظ نسخة فورية مشفرة ومزامنة على كل أجهزتك.</li>
      </ul>
    </div>
  `;
}

export function renderAccountAndSecurityPage(state) {
  const user = state.cloudUser;
  const p = state.preferences;
  const quota = state.storageQuota || { usage: 0, quota: 0, persisted: false };
  const usageKb = Math.round((quota.usage || 0) / 1024);

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>الحساب والأمان وسجل التدقيق</strong>
      </div>
    </div>

    <div class="card">
      <div class="row-between">
        <div>
          <strong>حالة الحساب السحابي</strong>
          <div class="text-muted">${user ? `${escapeHtml(user.displayName || "")} (${escapeHtml(user.email || "")})` : "غير مرتبط — وضع محلي على الجهاز"}</div>
        </div>
        <button class="btn btn-primary btn-sm" data-action="go-tab" data-val="auth">${user ? "إدارة الحساب" : "تسجيل الدخول"}</button>
      </div>
    </div>

    <div class="card">
      <div class="row-between">
        <strong>قفل الحماية السريع (PIN)</strong>
        <span class="chip ${p.pinEnabled ? "active" : ""}">${p.pinEnabled ? "مفعّل 🔒" : "معطّل"}</span>
      </div>
      <form data-form="set-pin" style="margin-top:8px;">
        <label class="field-label">${p.pinEnabled ? "تغيير رمز PIN (4-6 أرقام)" : "تعيين رمز PIN جديد (4-6 أرقام)"}</label>
        <div style="display:flex;gap:8px;">
          <input class="input" name="pin" type="password" inputmode="numeric" pattern="[0-9]{4,6}" minlength="4" maxlength="6" placeholder="1234" required />
          <button type="submit" class="btn btn-primary btn-sm">حفظ PIN</button>
        </div>
      </form>
      ${p.pinEnabled ? `
        <div style="display:flex;gap:8px;margin-top:8px;">
          <button class="btn btn-outline btn-sm" data-action="lock-app-now">قفل التطبيق الآن</button>
          <button class="btn btn-danger btn-sm" data-action="disable-pin">تعطيل PIN</button>
        </div>
      ` : ""}
    </div>

    <div class="card">
      <strong>التخزين المحلي المستديم (IndexedDB)</strong>
      <div class="text-muted" style="margin-top:4px;">
        المساحة المستخدمة حالياً: ${usageKb} كيلوبايت • الحماية من المسح التلقائي: ${quota.persisted ? "مفعّلة ✓" : "عادية"}
      </div>
      <button class="btn btn-outline btn-sm" style="margin-top:8px;" data-action="request-persistent-storage">تفعيل حماية التخزين الدائم للمتصفح</button>
    </div>

    <div class="card" style="border-color:rgba(225,29,72,0.4);">
      <strong class="text-neg">منطقة الخطر — مسح البيانات</strong>
      <p class="text-muted">حذف جميع الديون والأشخاص والمدفوعات من هذا الجهاز${user ? " ومن السحابة Firebase" : ""} نهائياً.</p>
      <button class="btn btn-danger btn-sm" data-action="wipe-all-data">حذف كافة البيانات المحلية والسحابية</button>
    </div>

    <div class="card">
      <strong>سجل التدقيق والعمليات المحاسبية (${state.auditLogs?.length || 0})</strong>
      <div class="list-stack" style="margin-top:8px;">
        ${state.auditLogs?.length ? state.auditLogs.slice(0, 40).map((log) => `
          <div style="border-bottom:1px solid var(--border);padding-bottom:6px;">
            <div class="row-between">
              <strong style="font-size:12.5px;">${escapeHtml(log.action)}</strong>
              <span class="text-muted">${formatDateTime(log.createdAtMillis)}</span>
            </div>
            <div class="text-muted">${escapeHtml(log.details)}</div>
            <div style="font-size:11px;color:var(--primary);">المنفذ: ${escapeHtml(log.actorName || "صاحب الحساب")} ${log.reason ? `• السبب: ${escapeHtml(log.reason)}` : ""}</div>
          </div>
        `).join("") : `<div class="text-muted">لا توجد عمليات مسجلة في سجل التدقيق بعد.</div>`}
      </div>
    </div>
  `;
}

export function renderWorkspacePage(state) {
  const members = state.members || [];
  const activeId = state.preferences?.activeMemberId || "owner";
  const roleLabelAr = { OWNER: "مالك", MANAGER: "مدير", STAFF: "موظف", VIEWER: "مشاهد فقط" };

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>فريق العمل والصلاحيات (RBAC)</strong>
      </div>
    </div>

    <div class="card">
      <strong>المشغّل النشط حالياً على هذا الجهاز</strong>
      <select class="select" style="margin-top:6px;" data-change="switch-active-member">
        ${members.map((m) => `<option value="${m.id}" ${m.id === activeId ? "selected" : ""}>${escapeHtml(m.displayName)} (${roleLabelAr[m.role] || m.role})</option>`).join("")}
      </select>
    </div>

    <form class="card" data-form="add-member">
      <strong>إضافة عضو فريق جديد</strong>
      <label class="field-label">الاسم الكامل *</label>
      <input class="input" name="displayName" required placeholder="مثال: المحاسب سالم" />
      <label class="field-label">البريد الإلكتروني *</label>
      <input class="input" name="email" type="email" required placeholder="accountant@company.com" />
      <label class="field-label">الدور الوظيفي</label>
      <select class="select" name="role">
        <option value="MANAGER">مدير (صلاحيات واسعة)</option>
        <option value="STAFF" selected>موظف تحصيل/إدخال</option>
        <option value="VIEWER">مشاهد فقط</option>
      </select>
      <label class="switch-row"><span>إضافة ديون جديدة</span><input type="checkbox" name="canAddDebt" checked /></label>
      <label class="switch-row"><span>تعديل القيود والأقساط</span><input type="checkbox" name="canEditDebt" /></label>
      <label class="switch-row"><span>تسجيل المدفوعات</span><input type="checkbox" name="canRecordPayment" checked /></label>
      <label class="switch-row"><span>عرض التقارير وتصديرها</span><input type="checkbox" name="canViewReports" checked /></label>
      <button type="submit" class="btn btn-primary" style="width:100%;margin-top:8px;">إضافة عضو الفريق</button>
    </form>

    <div class="list-stack">
      ${members.map((m) => `
        <div class="card">
          <div class="row-between">
            <div>
              <strong>${escapeHtml(m.displayName)} ${m.id === activeId ? "(النشط)" : ""}</strong>
              <div class="text-muted">${escapeHtml(m.email)} • ${roleLabelAr[m.role] || m.role}</div>
              <div style="font-size:11px;color:var(--muted);margin-top:3px;">
                ${m.canAddDebt ? "✓ إضافة " : ""}${m.canEditDebt ? "✓ تعديل " : ""}${m.canRecordPayment ? "✓ سداد " : ""}${m.canViewReports ? "✓ تقارير" : ""}
              </div>
            </div>
            ${m.id !== "owner" ? `<button class="btn btn-danger btn-sm" data-action="delete-member" data-id="${m.id}">حذف</button>` : `<span class="chip active">المالك</span>`}
          </div>
        </div>
      `).join("")}
    </div>
  `;
}

export function renderSettingsPageFull(state) {
  const p = state.preferences;
  const allCurrencies = mergeAllCurrencies(state.currencies);
  const customThemes = p.customThemes || [];

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>الثيمات والعملات والإعدادات العامة</strong>
      </div>
    </div>

    <div class="card">
      <strong>المظهر والألوان</strong>
      <label class="field-label">الوضع</label>
      <div class="tabs-row">
        <button class="chip ${p.themeMode === "SYSTEM" ? "active" : ""}" data-action="set-theme-mode" data-val="SYSTEM">تلقائي حسب الجهاز</button>
        <button class="chip ${p.themeMode === "LIGHT" ? "active" : ""}" data-action="set-theme-mode" data-val="LIGHT">فاتح ساطع</button>
        <button class="chip ${p.themeMode === "DARK" ? "active" : ""}" data-action="set-theme-mode" data-val="DARK">داكن فاخر</button>
      </div>

      <label class="field-label">لوحة الألوان (6 ثيمات احترافية + المخصص)</label>
      <div class="tabs-row">
        ${PALETTES.map((pal) => `<button class="chip ${p.themePalette === pal.id ? "active" : ""}" data-action="set-palette" data-val="${pal.id}">${pal.nameAr}</button>`).join("")}
        <button class="chip ${p.themePalette === "custom" ? "active" : ""}" data-action="set-palette" data-val="custom">ثيم مخصص 🎨</button>
      </div>

      <form data-form="custom-theme" style="margin-top:8px;padding-top:8px;border-top:1px solid var(--border);">
        <label class="field-label">تخصيص وحفظ ثيم جديد</label>
        <div style="display:grid;grid-template-columns:1fr auto auto;gap:8px;align-items:center;">
          <input class="input" name="themeName" placeholder="اسم الثيم (مثال: أخضر تجاري)" required />
          <input type="color" name="primaryHex" value="${escapeHtml(p.customPrimaryHex || "#0B1B32")}" title="اللون الأساسي" style="width:42px;height:42px;border:none;background:transparent;" />
          <input type="color" name="accentHex" value="${escapeHtml(p.customAccentHex || "#10B981")}" title="لون التمييز" style="width:42px;height:42px;border:none;background:transparent;" />
        </div>
        <button type="submit" class="btn btn-outline btn-sm" style="margin-top:6px;">حفظ وتطبيق الثيم المخصص</button>
      </form>

      ${customThemes.length ? `
        <div class="tabs-row" style="margin-top:8px;">
          ${customThemes.map((t) => `
            <span class="chip ${p.themePalette === "custom" && p.customPrimaryHex === t.primaryHex ? "active" : ""}" style="display:inline-flex;align-items:center;gap:6px;">
              <span data-action="apply-custom-theme" data-id="${t.id}" style="cursor:pointer;">● ${escapeHtml(t.name)}</span>
              <span data-action="delete-custom-theme" data-id="${t.id}" style="cursor:pointer;color:var(--negative);">✕</span>
            </span>
          `).join("")}
        </div>
      ` : ""}
    </div>

    <div class="card">
      <strong>العملة الافتراضية وإضافة عملات مخصصة</strong>
      <div class="tabs-row" style="margin-top:6px;">
        ${allCurrencies.map((c) => `<button class="chip ${p.defaultCurrency === c.code ? "active" : ""}" data-action="set-currency" data-val="${c.code}">${escapeHtml(c.nameAr)} (${escapeHtml(c.symbol)})</button>`).join("")}
      </div>
      <form data-form="add-currency" style="margin-top:10px;padding-top:8px;border-top:1px solid var(--border);">
        <label class="field-label">إضافة عملة مخصصة جديدة</label>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
          <input class="input" name="code" placeholder="الرمز (مثال: KWD)" maxlength="6" required />
          <input class="input" name="symbol" placeholder="العلامة (مثال: د.ك)" maxlength="6" required />
          <input class="input" name="nameAr" placeholder="الاسم بالعربي" required />
          <input class="input" name="rateToUsd" type="number" step="0.0001" min="0.0001" placeholder="قيمتها بالدولار (مثال: 3.25)" required />
        </div>
        <button type="submit" class="btn btn-outline btn-sm" style="margin-top:8px;">+ حفظ العملة</button>
      </form>
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
      <strong>المزامنة السحابية والنسخ الاحتياطي JSON</strong>
      <p class="text-muted">يدعم التصدير والاستيراد المتوافق 100% بين نسخة الويب ونسخة الأندرويد.</p>
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:8px;">
        <button class="btn btn-primary btn-sm" data-action="go-tab" data-val="auth">
          ${state.cloudUser ? `الحساب المتصل: ${escapeHtml(state.cloudUser.email || "")}` : "فتح بوابة تسجيل الدخول والمزامنة"}
        </button>
        <button class="btn btn-outline btn-sm" data-action="export-backup">تصدير نسخة JSON</button>
        <label class="btn btn-outline btn-sm" style="cursor:pointer;">
          استيراد JSON
          <input type="file" accept="application/json" style="display:none;" data-input="import-backup" />
        </label>
      </div>
    </div>
  `;
}

export function renderWhatsAppAndSmsPage(state, previewMsg) {
  const p = state.preferences;
  const templates = state.templates || [];
  const editingTpl = templates.find((t) => t.id === state.editingTemplateId) || templates[0];

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>إعدادات واتساب والقوالب و SMS</strong>
      </div>
    </div>

    <form class="card" data-form="wa-settings">
      <strong>تنسيق رسائل واتساب السريعة</strong>
      <label class="field-label">أسلوب الرسالة</label>
      <div class="tabs-row">
        ${[["FORMAL", "رسمي متوازن"], ["FRIENDLY", "ودي لطيف"], ["URGENT", "تنبيه عاجل"]]
          .map(([k, l]) => `<button type="button" class="chip ${p.whatsappStyle === k ? "active" : ""}" data-action="set-wa-style" data-val="${k}">${l}</button>`)
          .join("")}
      </div>
      <label class="switch-row"><span>تضمين اسم المتجر والترويسة</span><input type="checkbox" name="whatsappIncludeWorkspaceHeader" ${p.whatsappIncludeWorkspaceHeader !== false ? "checked" : ""} /></label>
      <label class="switch-row"><span>تضمين الملخص المالي</span><input type="checkbox" name="whatsappIncludeFinancialBreakdown" ${p.whatsappIncludeFinancialBreakdown !== false ? "checked" : ""} /></label>
      <label class="switch-row"><span>تضمين تواريخ التسجيل والاستحقاق</span><input type="checkbox" name="whatsappIncludeDates" ${p.whatsappIncludeDates !== false ? "checked" : ""} /></label>
      <label class="switch-row"><span>تضمين البيان والسبب</span><input type="checkbox" name="whatsappIncludeReasonAndNotes" ${p.whatsappIncludeReasonAndNotes !== false ? "checked" : ""} /></label>
      <label class="switch-row"><span>تضمين التوقيع الختامي أسفل الرسالة</span><input type="checkbox" name="whatsappIncludeFooterSignature" ${p.whatsappIncludeFooterSignature !== false ? "checked" : ""} /></label>
      <label class="field-label">التوقيع الختامي</label>
      <input class="input" name="whatsappCustomSignature" value="${escapeHtml(p.whatsappCustomSignature)}" />
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;">
        <button type="submit" class="btn btn-primary btn-sm" style="flex:1;">حفظ تنسيق واتساب</button>
        <button type="button" class="btn btn-outline btn-sm" data-action="export-wa-config">تصدير القوالب</button>
        <label class="btn btn-outline btn-sm" style="cursor:pointer;">
          استيراد
          <input type="file" accept="application/json" style="display:none;" data-input="import-wa-config" />
        </label>
      </div>
    </form>

    <div class="card">
      <strong>معاينة حية لرسالة السند</strong>
      <pre style="white-space:pre-wrap;font-family:inherit;background:var(--surface-alt);padding:10px;border-radius:10px;font-size:12px;">${escapeHtml(previewMsg)}</pre>
      <button class="btn btn-positive btn-sm" data-action="test-wa-preview">مشاركة المعاينة عبر واتساب</button>
    </div>

    ${editingTpl ? `
      <form class="card" data-form="save-template">
        <strong>محرر القوالب العربية (واتساب / SMS)</strong>
        <label class="field-label">اختر القالب</label>
        <select class="select" data-change="select-template">
          ${templates.map((t) => `<option value="${t.id}" ${t.id === editingTpl.id ? "selected" : ""}>${escapeHtml(t.titleAr)}</option>`).join("")}
        </select>
        <input type="hidden" name="id" value="${escapeHtml(editingTpl.id)}" />
        <label class="field-label">عنوان القالب</label>
        <input class="input" name="titleAr" value="${escapeHtml(editingTpl.titleAr)}" required />
        <label class="field-label">القناة</label>
        <select class="select" name="channel">
          <option value="WHATSAPP" ${editingTpl.channel === "WHATSAPP" ? "selected" : ""}>واتساب فقط</option>
          <option value="SMS" ${editingTpl.channel === "SMS" ? "selected" : ""}>SMS فقط</option>
          <option value="BOTH" ${editingTpl.channel === "BOTH" ? "selected" : ""}>واتساب + SMS</option>
        </select>
        <label class="field-label">نص القالب (المتغيرات المتاحة: {الاسم} {المبلغ} {المتبقي} {السبب} {التاريخ} {رقم_الإيصال})</label>
        <textarea class="textarea" name="bodyTemplate" rows="5" required>${escapeHtml(editingTpl.bodyTemplate)}</textarea>
        <button type="submit" class="btn btn-primary btn-sm" style="width:100%;margin-top:8px;">حفظ تعديل القالب</button>
      </form>
    ` : ""}

    <form class="card" data-form="messaging-relay">
      <strong>ربط الإرسال الآلي عبر الخادم (WhatsApp Cloud API & SMS)</strong>
      <p class="text-muted">يمكنك استخدام الإرسال المباشر مجاناً من المتصفح، أو ربط خادم Backend الآمن للإرسال الآلي بالخلفية.</p>
      <label class="switch-row"><span>تفعيل WhatsApp Cloud API الآلي</span><input type="checkbox" name="whatsappCloudEnabled" ${p.whatsappCloudEnabled ? "checked" : ""} /></label>
      <label class="switch-row"><span>تفعيل بوابة رسائل SMS الآلية</span><input type="checkbox" name="smsGatewayEnabled" ${p.smsGatewayEnabled ? "checked" : ""} /></label>
      <label class="field-label">رابط الخادم الآمن (HTTPS Backend URL)</label>
      <input class="input" name="backendBaseUrl" value="${escapeHtml(p.backendBaseUrl || "")}" placeholder="https://your-backend.run.app" dir="ltr" />
      <label class="field-label">معرّف رقم واتساب للأعمال (Phone Number ID)</label>
      <input class="input" name="whatsappPhoneNumberId" value="${escapeHtml(p.whatsappPhoneNumberId || "")}" dir="ltr" />
      <label class="field-label">اسم المرسل لرسائل SMS (Sender ID)</label>
      <input class="input" name="smsSenderId" value="${escapeHtml(p.smsSenderId || "HISABATI")}" dir="ltr" />
      <button type="submit" class="btn btn-primary btn-sm" style="width:100%;margin-top:10px;">حفظ إعدادات بوابة الإرسال</button>
    </form>
  `;
}

export function renderHelpPage() {
  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>المساعدة والدعم الفني</strong>
      </div>
    </div>

    <div class="card">
      <strong>أين تُخزن البيانات في تطبيق الويب PWA؟ وهل السحابة ضرورية؟</strong>
      <p class="text-muted" style="line-height:1.7;margin-top:6px;">
        <strong>1) التخزين المحلي (يعمل بدون إنترنت):</strong> تُحفظ كل الديون والعملاء والأقساط والإيصالات داخل قاعدة بيانات المتصفح الداخلية (<strong>IndexedDB — hisabati_pwa_db</strong>)، كما تُخزن واجهة التطبيق في <strong>Cache Storage</strong> عبر الـ Service Worker لتعمل كـ تطبيق مستقل تماماً.<br/>
        <strong>2) متى تكون السحابة ضرورية؟</strong> إذا مسح المستخدم بيانات Safari/Chrome أو غيّر جهازه أو فتح التطبيق على أكثر من جهاز (كمبيوتر + آيفون + أندرويد)، فقد يضيع التخزين المحلي؛ لذلك يضمن <strong>ربط الحساب بـ Firebase Cloud Firestore</strong> بقاء بياناتك محفوظة ومزامنة لحظياً في كل الأوقات.
      </p>
    </div>

    <div class="card">
      <strong>أسئلة شائعة</strong>
      <div class="list-stack" style="margin-top:8px;">
        <div>
          <strong>كيف أثبت التطبيق على شاشة الآيفون؟</strong>
          <div class="text-muted">افتح الرابط في Safari ← اضغط مشاركة ⬆ ← «إضافة إلى الشاشة الرئيسية».</div>
        </div>
        <div>
          <strong>كيف أصدّر تقارير Excel عربية بدون تقطيع حروف؟</strong>
          <div class="text-muted">من المزيد ← التقارير ← اضغط «تصدير Excel CSV»، يُصدّر بترميز UTF-8 BOM المتوافق مع Excel.</div>
        </div>
        <div>
          <strong>الدعم الفني — انما سوفت</strong>
          <div class="text-muted">للتواصل والاستفسار: <a href="tel:772317950" style="color:var(--primary);font-weight:800;">772317950</a></div>
        </div>
      </div>
    </div>
  `;
}
