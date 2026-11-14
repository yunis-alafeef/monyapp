import { sanitizePhone } from "./money.js";

export function buildFormattedWhatsAppMessage(prefs, params) {
  const style = prefs?.whatsappStyle || "FORMAL";
  const workspace = prefs?.printCompanyName || prefs?.workspaceName || "حساباتي";
  const signature = prefs?.whatsappCustomSignature || "شكراً لتعاملكم الكريم • حساباتي";

  const lines = [];
  if (prefs?.whatsappIncludeWorkspaceHeader !== false) {
    lines.push(`*${workspace}*`);
    lines.push("━━━━━━━━━━━━━━");
  }

  const badge = style === "URGENT" ? "⚠️" : style === "FRIENDLY" ? "🌿" : "🧾";
  lines.push(`${badge} *${params.title}*`);
  if (params.receiptNumber) lines.push(`• رقم السند: *${params.receiptNumber}*`);
  lines.push(`• الطرف: *${params.contactName}*`);
  if (params.directionLabel) lines.push(`• النوع: ${params.directionLabel}`);

  if (prefs?.whatsappIncludeFinancialBreakdown !== false) {
    lines.push("");
    lines.push("💰 *الملخص المالي:*");
    if (params.principalFormatted) lines.push(`- أصل المبلغ: ${params.principalFormatted}`);
    if (params.paidFormatted) lines.push(`- المسدد: ${params.paidFormatted}`);
    if (params.remainingFormatted) lines.push(`- المتبقي: *${params.remainingFormatted}*`);
  }

  if (prefs?.whatsappIncludeDates !== false && (params.recordedDateLabel || params.dueDateLabel)) {
    lines.push("");
    if (params.recordedDateLabel) lines.push(`📅 التاريخ: ${params.recordedDateLabel}`);
    if (params.dueDateLabel) lines.push(`⏳ الاستحقاق: ${params.dueDateLabel}`);
  }

  if (prefs?.whatsappIncludeReasonAndNotes !== false) {
    if (params.reason) lines.push(`📝 البيان: ${params.reason}`);
    if (params.notes) lines.push(`📌 ملاحظات: ${params.notes}`);
  }

  if (prefs?.whatsappIncludeFooterSignature !== false) {
    lines.push("");
    lines.push("━━━━━━━━━━━━━━");
    lines.push(signature);
  }

  return lines.join("\n");
}

export function openWhatsAppShare(phone, message) {
  const cleanPhone = sanitizePhone(phone);
  const encoded = encodeURIComponent(message);
  const url = cleanPhone
    ? `https://wa.me/${cleanPhone}?text=${encoded}`
    : `https://wa.me/?text=${encoded}`;
  window.open(url, "_blank", "noopener,noreferrer");
}
