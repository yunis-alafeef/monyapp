import { escapeHtml, formatDate } from "./money.js";

export const PDF_TEMPLATES = [
  { id: "CLASSIC_FORMAL", titleAr: "الكلاسيكي الرسمي", subtitleAr: "ترويسة داكنة متكاملة وجدول مؤطر" },
  { id: "MODERN_LUXURY", titleAr: "العصري الفاخر", subtitleAr: "تدرج لوني حديث مع بطاقات ناعمة" },
  { id: "DOUBLE_FRAME_VOUCHER", titleAr: "السند المحاسبي المؤطر", subtitleAr: "إطار مزدوج وشعار بارز للسندات" },
  { id: "MINIMAL_EXECUTIVE", titleAr: "التنفيذي الاقتصادي", subtitleAr: "أبيض نقي مع شريط جانبي للطباعة" }
];

export const PDF_DOC_KINDS = [
  { id: "DEBT_VOUCHER", titleAr: "سند قيد دين", key: "printDebtColorHex" },
  { id: "PAYMENT_RECEIPT", titleAr: "إيصال استلام دفعة", key: "printReceiptColorHex" },
  { id: "STATEMENT", titleAr: "كشف حساب تفصيلي", key: "printStatementColorHex" },
  { id: "GENERAL_REPORT", titleAr: "التقارير المالية", key: "printReportColorHex" }
];

export const PDF_COLOR_SWATCHES = [
  { nameAr: "كحلي ملكي", hex: "#0B1B32", accent: "#10B981" },
  { nameAr: "زمردي مالي", hex: "#065F46", accent: "#10B981" },
  { nameAr: "أزرق مؤسسي", hex: "#1E3A8A", accent: "#06B6D4" },
  { nameAr: "ذهبي كهرماني", hex: "#78350F", accent: "#D97706" },
  { nameAr: "عنابي فاخر", hex: "#881337", accent: "#F59E0B" },
  { nameAr: "فيروزي حديث", hex: "#115E59", accent: "#14B8A6" },
  { nameAr: "بنفسجي تنفيذي", hex: "#4C1D95", accent: "#8B5CF6" },
  { nameAr: "فحمي اقتصادي", hex: "#1E293B", accent: "#64748B" }
];

export function resolveInvoiceColors(prefs, docKindId) {
  const kind = PDF_DOC_KINDS.find((k) => k.id === docKindId) || PDF_DOC_KINDS[3];
  const primary = prefs?.[kind.key] || "#0B1B32";
  const swatch = PDF_COLOR_SWATCHES.find((s) => s.hex === primary);
  return {
    primary,
    accent: swatch?.accent || "#10B981",
    kindTitle: kind.titleAr
  };
}

export function buildInvoiceHtml(prefs, doc) {
  const template = prefs?.printTemplate || "CLASSIC_FORMAL";
  const { primary, accent, kindTitle } = resolveInvoiceColors(prefs, doc.docKind || "GENERAL_REPORT");
  const company = prefs?.printCompanyName || prefs?.workspaceName || "حساباتي لإدارة الديون";
  const darkHeader = template === "CLASSIC_FORMAL" || template === "MODERN_LUXURY";
  const headerStyle =
    template === "CLASSIC_FORMAL"
      ? `background:${primary};color:#fff;border-bottom:4px solid ${accent};`
      : template === "MODERN_LUXURY"
      ? `background:linear-gradient(120deg, ${primary}, ${accent});color:#fff;`
      : template === "DOUBLE_FRAME_VOUCHER"
      ? `background:${primary}12;color:${primary};border:2px double ${primary};`
      : `background:#fff;color:${primary};border-right:5px solid ${primary};border-bottom:2px solid ${primary};`;

  const outerFrameStyle =
    template === "DOUBLE_FRAME_VOUCHER"
      ? `border:3px double ${primary};padding:14px;border-radius:10px;`
      : "";

  const logoBlock = prefs?.printShowLogoInHeader !== false
    ? prefs?.printLogoDataUrl
      ? `<img src="${prefs.printLogoDataUrl}" class="inv-logo" alt="شعار المؤسسة" />`
      : `<div class="inv-logo" style="color:${primary};">${escapeHtml(company.slice(0, 1) || "ح")}</div>`
    : `<div></div>`;

  const orgMeta = [
    prefs?.printCompanyPhone ? `هاتف: ${escapeHtml(prefs.printCompanyPhone)}` : "",
    prefs?.printCompanyAddress ? `العنوان: ${escapeHtml(prefs.printCompanyAddress)}` : "",
    prefs?.printTaxOrCrNumber ? `السجل/الضريبي: ${escapeHtml(prefs.printTaxOrCrNumber)}` : ""
  ].filter(Boolean).join("<br/>") || "نظام حساباتي للإدارة المالية";

  const inmaHeader = prefs?.printShowInmaSoftInHeader !== false
    ? `<div style="font-weight:800;color:${darkHeader ? "#fff" : accent};font-size:11.5px;">انما سوفت | Inma Soft</div>
       <div style="font-size:10.5px;opacity:.9;">للتطوير والحلول الذكية • 772317950</div>`
    : "";

  const summaryBoxes = (doc.summary || [])
    .map(([k, v]) => `<div class="inv-box"><span style="color:#64748B;">${escapeHtml(k)}:</span> <strong>${escapeHtml(v)}</strong></div>`)
    .join("");

  const tableRows = (doc.rows || [])
    .map(
      (r) => `<tr>
        <td>${escapeHtml(r.date)}</td>
        <td>${escapeHtml(r.title)}</td>
        <td style="font-weight:700;color:${primary};">${escapeHtml(r.amount)}</td>
        <td>${escapeHtml(r.status)}</td>
      </tr>`
    )
    .join("");

  const signatures = prefs?.printShowSignatureBox !== false
    ? `<div class="inv-sigs">
         <div class="inv-sig-box">توقيع المحاسب / ختم ${escapeHtml(company)}</div>
         <div class="inv-sig-box">توقيع العميل / المستلم</div>
       </div>`
    : "";

  return `
    <div style="${outerFrameStyle}">
      <div class="inv-header" style="${headerStyle}">
        <div style="text-align:right;">
          <div style="font-size:15px;font-weight:800;">${escapeHtml(company)}</div>
          <div style="font-size:10.5px;opacity:.9;margin-top:3px;">${orgMeta}</div>
        </div>
        <div style="display:flex;justify-content:center;">${logoBlock}</div>
        <div style="text-align:left;">
          ${inmaHeader}
          <div style="font-size:10.5px;margin-top:4px;opacity:.9;">${escapeHtml(kindTitle)} • ${formatDate(Date.now())}</div>
        </div>
      </div>

      <div style="background:${primary}10;border:1px solid ${primary}33;border-radius:8px;padding:8px 12px;margin-bottom:10px;">
        <div style="font-size:15px;font-weight:800;color:${primary};">${escapeHtml(doc.title)}</div>
        <div style="font-size:11.5px;color:#475569;">${escapeHtml(doc.subtitle || "")}</div>
      </div>

      <div class="inv-grid">${summaryBoxes}</div>

      <table class="inv-table">
        <thead>
          <tr style="background:${primary};color:#fff;">
            <th>التاريخ</th>
            <th>البيان / التفاصيل</th>
            <th>المبلغ</th>
            <th>الحالة</th>
          </tr>
        </thead>
        <tbody>${tableRows}</tbody>
      </table>

      ${signatures}

      <div class="inv-footer">
        <div>${escapeHtml(prefs?.printFooterNote || "شكراً لتعاملكم معنا • حساباتي")}</div>
        ${prefs?.printShowInmaSoftInFooter !== false
          ? `<div style="font-weight:800;color:${primary};">عمل انما سوفت (Inma Soft) • هاتف: 772317950</div>`
          : ""}
      </div>
    </div>
  `;
}

export function printInvoiceSheet(prefs, doc) {
  const sheet = document.getElementById("print-sheet");
  if (!sheet) return;
  sheet.innerHTML = buildInvoiceHtml(prefs, doc);
  window.print();
}
