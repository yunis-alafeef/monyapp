import {
  buildCsvContent,
  convertMinor,
  escapeHtml,
  formatDate,
  formatMinor,
  formatSignedMinor,
  getDirectionLabelAr,
  getPaymentMethodLabelAr,
  getStatusLabelAr,
  mergeAllCurrencies
} from "./money.js";

export const REPORT_KINDS = [
  { id: "POSITION", titleAr: "ملخص المركز المالي" },
  { id: "RECEIVABLES", titleAr: "ديون مستحقة لي" },
  { id: "PAYABLES", titleAr: "ديون مستحقة عليّ" },
  { id: "OVERDUE", titleAr: "الديون المتأخرة" },
  { id: "PAYMENTS", titleAr: "سجل المدفوعات" },
  { id: "MONTHLY", titleAr: "تقرير الشهر الحالي" }
];

export function getActiveMemberPermissions(state) {
  const members = state?.members || [];
  const activeId = state?.preferences?.activeMemberId || "owner";
  const active = members.find((m) => m.id === activeId) || members[0];
  if (!active || active.role === "OWNER") {
    return {
      member: active || { id: "owner", displayName: "المالك", role: "OWNER" },
      canAddDebt: true,
      canEditDebt: true,
      canRecordPayment: true,
      canViewReports: true
    };
  }
  return {
    member: active,
    canAddDebt: Boolean(active.canAddDebt),
    canEditDebt: Boolean(active.canEditDebt),
    canRecordPayment: Boolean(active.canRecordPayment),
    canViewReports: Boolean(active.canViewReports)
  };
}

export function filterDebtsForReport(debts = [], kind = "POSITION", nowMillis = Date.now()) {
  const monthStart = new Date(new Date(nowMillis).getFullYear(), new Date(nowMillis).getMonth(), 1).getTime();
  switch (kind) {
    case "RECEIVABLES":
      return debts.filter((d) => d.direction === "RECEIVABLE");
    case "PAYABLES":
      return debts.filter((d) => d.direction === "PAYABLE");
    case "OVERDUE":
      return debts.filter(
        (d) => (d.remainingMinor || 0) > 0 && d.dueAtMillis < nowMillis && d.status !== "PAID" && d.status !== "SETTLED"
      );
    case "MONTHLY":
      return debts.filter((d) => (d.recordedAtMillis || 0) >= monthStart);
    default:
      return debts;
  }
}

export function buildReportDocument(state, kind = "POSITION", nowMillis = Date.now()) {
  const cur = state?.preferences?.defaultCurrency || "YER";
  const currencies = state?.currencies || [];
  const debts = state?.debts || [];
  const payments = state?.payments || [];
  const kindMeta = REPORT_KINDS.find((k) => k.id === kind) || REPORT_KINDS[0];

  if (kind === "PAYMENTS") {
    const debtMap = new Map(debts.map((d) => [d.id, d]));
    const totalPay = payments.reduce(
      (s, p) => s + convertMinor(p.amountMinor || 0, p.currencyCode || cur, cur, currencies),
      0
    );
    const totalDisc = payments.reduce(
      (s, p) => s + convertMinor(p.discountMinor || 0, p.currencyCode || cur, cur, currencies),
      0
    );
    return {
      docKind: "GENERAL_REPORT",
      title: kindMeta.titleAr,
      subtitle: `عدد الدفعات: ${payments.length} • عملة التقييم: ${cur}`,
      summary: [
        ["إجمالي المدفوعات", formatMinor(totalPay, cur, currencies)],
        ["إجمالي الخصومات", formatMinor(totalDisc, cur, currencies)],
        ["عدد السندات", String(payments.length)]
      ],
      rows: payments.map((p) => {
        const d = debtMap.get(p.debtId);
        return {
          date: formatDate(p.paidAtMillis),
          title: `${d?.contactName || "عميل"} — إيصال ${p.receiptNumber} (${getPaymentMethodLabelAr(p.method)})`,
          amount: formatMinor(p.amountMinor, p.currencyCode, currencies),
          status: p.discountMinor > 0 ? `خصم ${formatMinor(p.discountMinor, p.currencyCode, currencies)}` : "مؤكد"
        };
      }),
      exportDebts: debts
    };
  }

  const filtered = filterDebtsForReport(debts, kind, nowMillis);
  const recSum = filtered
    .filter((d) => d.direction === "RECEIVABLE")
    .reduce((s, d) => s + convertMinor(d.remainingMinor || 0, d.currencyCode || cur, cur, currencies), 0);
  const paySum = filtered
    .filter((d) => d.direction === "PAYABLE")
    .reduce((s, d) => s + convertMinor(d.remainingMinor || 0, d.currencyCode || cur, cur, currencies), 0);

  return {
    docKind: "GENERAL_REPORT",
    title: kindMeta.titleAr,
    subtitle: `عدد القيود: ${filtered.length} • العملة الافتراضية: ${cur}`,
    summary: [
      ["مستحق لي (متبقي)", formatMinor(recSum, cur, currencies)],
      ["مستحق عليّ (متبقي)", formatMinor(paySum, cur, currencies)],
      ["صافي المركز", formatSignedMinor(recSum - paySum, cur, currencies)],
      ["عدد القيود", String(filtered.length)]
    ],
    rows: filtered.map((d) => ({
      date: formatDate(d.dueAtMillis),
      title: `${d.contactName} (${getDirectionLabelAr(d.direction)}) — ${d.reason}`,
      amount: formatMinor(d.remainingMinor, d.currencyCode, currencies),
      status: getStatusLabelAr(d.status)
    })),
    exportDebts: filtered
  };
}

export function buildContactStatementData(state, contactId, periodDays = 0, nowMillis = Date.now()) {
  const contact = (state?.contacts || []).find((c) => c.id === contactId) || (state?.contacts || [])[0];
  if (!contact) return null;
  const currencies = state?.currencies || [];
  const cutoff = periodDays > 0 ? nowMillis - periodDays * 86_400_000 : 0;

  const cDebts = (state?.debts || []).filter(
    (d) => d.contactId === contact.id && (cutoff === 0 || (d.recordedAtMillis || 0) >= cutoff)
  );
  const cPayments = (state?.payments || []).filter(
    (p) => p.contactId === contact.id && (cutoff === 0 || (p.paidAtMillis || 0) >= cutoff)
  );

  // Group balances by original currency code
  const byCurrency = new Map();
  for (const d of cDebts) {
    const code = d.currencyCode || "YER";
    const curObj = byCurrency.get(code) || { code, recRemaining: 0, payRemaining: 0, recPrincipal: 0, payPrincipal: 0 };
    if (d.direction === "RECEIVABLE") {
      curObj.recRemaining += d.remainingMinor || 0;
      curObj.recPrincipal += d.principalMinor || 0;
    } else {
      curObj.payRemaining += d.remainingMinor || 0;
      curObj.payPrincipal += d.principalMinor || 0;
    }
    byCurrency.set(code, curObj);
  }

  const timeline = [
    ...cDebts.map((d) => ({
      millis: d.recordedAtMillis || 0,
      date: formatDate(d.recordedAtMillis),
      title: `${getDirectionLabelAr(d.direction)}: ${d.reason || "قيد"}`,
      amount: formatMinor(d.principalMinor, d.currencyCode, currencies),
      status: `متبقي ${formatMinor(d.remainingMinor, d.currencyCode, currencies)}`
    })),
    ...cPayments.map((p) => ({
      millis: p.paidAtMillis || 0,
      date: formatDate(p.paidAtMillis),
      title: `دفعة (${p.receiptNumber}) — ${getPaymentMethodLabelAr(p.method)}`,
      amount: formatMinor(p.amountMinor, p.currencyCode, currencies),
      status: p.discountMinor > 0 ? `خصم ${formatMinor(p.discountMinor, p.currencyCode, currencies)}` : "سداد"
    }))
  ].sort((a, b) => b.millis - a.millis);

  return {
    contact,
    debts: cDebts,
    payments: cPayments,
    currencySummaries: Array.from(byCurrency.values()),
    timeline
  };
}

export function renderReportsPage(state) {
  const perms = getActiveMemberPermissions(state);
  if (!perms.canViewReports) {
    return `
      <div class="row-between">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>التقارير المالية</strong>
      </div>
      <div class="card" style="text-align:center;padding:24px;">
        <strong>لا تملك صلاحية عرض التقارير</strong>
        <p class="text-muted">عضو الفريق النشط (${escapeHtml(perms.member.displayName)}) مقيد من الاطلاع على التقارير.</p>
      </div>
    `;
  }

  const kind = state.reportKind || "POSITION";
  const doc = buildReportDocument(state, kind);
  const currencies = mergeAllCurrencies(state.currencies);

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>التقارير والتصدير (PDF / CSV)</strong>
      </div>
      <div style="display:flex;gap:6px;">
        <button class="btn btn-primary btn-sm" data-action="print-active-report">طباعة PDF</button>
        <button class="btn btn-positive btn-sm" data-action="export-report-csv">تصدير Excel CSV</button>
      </div>
    </div>

    <div class="tabs-row">
      ${REPORT_KINDS.map((k) => `
        <button class="chip ${kind === k.id ? "active" : ""}" data-action="set-report-kind" data-val="${k.id}">
          ${escapeHtml(k.titleAr)}
        </button>
      `).join("")}
    </div>

    <div class="kpi-grid">
      ${doc.summary.map(([label, val]) => `
        <div class="kpi-tile">
          <div class="kpi-title">${escapeHtml(label)}</div>
          <div class="kpi-val">${escapeHtml(val)}</div>
        </div>
      `).join("")}
    </div>

    <div class="card">
      <div class="row-between" style="margin-bottom:8px;">
        <strong>${escapeHtml(doc.title)}</strong>
        <span class="text-muted">${escapeHtml(doc.subtitle)} • ${currencies.length} عملات</span>
      </div>
      ${doc.rows.length ? `
        <div style="overflow-x:auto;">
          <table class="data-table">
            <thead>
              <tr>
                <th>التاريخ</th>
                <th>البيان</th>
                <th>المبلغ</th>
                <th>الحالة</th>
              </tr>
            </thead>
            <tbody>
              ${doc.rows.slice(0, 80).map((r) => `
                <tr>
                  <td>${escapeHtml(r.date)}</td>
                  <td>${escapeHtml(r.title)}</td>
                  <td><strong>${escapeHtml(r.amount)}</strong></td>
                  <td>${escapeHtml(r.status)}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        </div>
      ` : `<div class="text-muted" style="text-align:center;padding:16px;">لا توجد بيانات في هذا التصنيف</div>`}
    </div>
  `;
}

export function renderStatementPage(state) {
  const contacts = state.contacts || [];
  if (!contacts.length) {
    return `
      <div class="row-between">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>كشف حساب شخص تفصيلي</strong>
      </div>
      <div class="card text-muted" style="text-align:center;padding:24px;">
        أضف شخصاً أو عميلاً أولاً لعرض كشف حسابه ومشاركته.
      </div>
    `;
  }

  const selectedId = state.statementContactId || contacts[0].id;
  const periodDays = Number(state.statementPeriodDays || 0);
  const data = buildContactStatementData(state, selectedId, periodDays);
  const currencies = state.currencies || [];

  return `
    <div class="row-between">
      <div style="display:flex;align-items:center;gap:6px;">
        <button class="btn btn-outline btn-sm" data-action="switch-tab" data-val="more">رجوع</button>
        <strong>كشف حساب تفصيلي</strong>
      </div>
    </div>

    <div class="card">
      <label class="field-label">اختر الشخص / العميل</label>
      <select class="select" data-change="statement-contact">
        ${contacts.map((c) => `<option value="${c.id}" ${c.id === data.contact.id ? "selected" : ""}>${escapeHtml(c.fullName)} (${escapeHtml(c.phone || "بدون هاتف")})</option>`).join("")}
      </select>
      <label class="field-label">الفترة الزمنية</label>
      <div class="tabs-row">
        ${[[0, "كل الفترة"], [30, "آخر 30 يوماً"], [90, "آخر 90 يوماً"]].map(([days, lbl]) => `
          <button class="chip ${periodDays === days ? "active" : ""}" data-action="set-statement-period" data-val="${days}">${lbl}</button>
        `).join("")}
      </div>
      <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:10px;">
        <button class="btn btn-primary btn-sm" data-action="print-contact-statement" data-id="${data.contact.id}">طباعة PDF</button>
        <button class="btn btn-positive btn-sm" data-action="share-statement-wa" data-id="${data.contact.id}">ملخص واتساب</button>
        <button class="btn btn-outline btn-sm" data-action="share-statement-sms" data-id="${data.contact.id}">إرسال SMS</button>
        <button class="btn btn-outline btn-sm" data-action="export-statement-csv" data-id="${data.contact.id}">تصدير CSV</button>
      </div>
    </div>

    <div class="list-stack">
      ${data.currencySummaries.length ? data.currencySummaries.map((cs) => {
        const net = cs.recRemaining - cs.payRemaining;
        return `
          <div class="card">
            <div class="row-between">
              <strong>الرصيد بعملة (${escapeHtml(cs.code)})</strong>
              <strong class="${net >= 0 ? "text-pos" : "text-neg"}">${formatSignedMinor(net, cs.code, currencies)}</strong>
            </div>
            <div class="row-between text-muted" style="margin-top:6px;">
              <span>مستحق لك: <strong class="text-pos">${formatMinor(cs.recRemaining, cs.code, currencies)}</strong></span>
              <span>مستحق عليك: <strong class="text-neg">${formatMinor(cs.payRemaining, cs.code, currencies)}</strong></span>
            </div>
          </div>
        `;
      }).join("") : `<div class="card text-muted">لا توجد قيود مالية في الفترة المحددة.</div>`}
    </div>

    <div class="card">
      <strong>حركة الحساب (${data.timeline.length})</strong>
      ${data.timeline.length ? `
        <div class="list-stack" style="margin-top:8px;">
          ${data.timeline.map((row) => `
            <div class="row-between" style="border-bottom:1px solid var(--border);padding-bottom:6px;">
              <div>
                <strong style="font-size:12.5px;">${escapeHtml(row.title)}</strong>
                <div class="text-muted">${escapeHtml(row.date)} • ${escapeHtml(row.status)}</div>
              </div>
              <strong>${escapeHtml(row.amount)}</strong>
            </div>
          `).join("")}
        </div>
      ` : `<div class="text-muted" style="margin-top:6px;">لا توجد حركات مسجلة.</div>`}
    </div>
  `;
}

export function downloadCsvFile(filename, debts) {
  const csv = buildCsvContent(debts);
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
}
