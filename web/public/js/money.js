export const DEFAULT_CURRENCIES = [
  { code: "YER", nameAr: "ريال يمني", symbol: "ر.ي", rateToUsdMicros: 1887, isCustom: false },
  { code: "SAR", nameAr: "ريال سعودي", symbol: "ر.س", rateToUsdMicros: 266667, isCustom: false },
  { code: "USD", nameAr: "دولار أمريكي", symbol: "$", rateToUsdMicros: 1000000, isCustom: false },
  { code: "AED", nameAr: "درهم إماراتي", symbol: "د.إ", rateToUsdMicros: 272294, isCustom: false },
  { code: "EGP", nameAr: "جنيه مصري", symbol: "ج.م", rateToUsdMicros: 20500, isCustom: false }
];

export function mergeAllCurrencies(customCurrencies = []) {
  const map = new Map();
  for (const c of DEFAULT_CURRENCIES) map.set(c.code, c);
  for (const c of customCurrencies || []) {
    if (c?.code) map.set(c.code, c);
  }
  return Array.from(map.values());
}

export function getCurrencySymbol(code, customCurrencies = []) {
  const foundCustom = (customCurrencies || []).find((c) => c.code === code);
  if (foundCustom?.symbol) return foundCustom.symbol;
  return DEFAULT_CURRENCIES.find((c) => c.code === code)?.symbol || code || "ر.ي";
}

export function getCurrencyRateMicros(code, customCurrencies = []) {
  const foundCustom = (customCurrencies || []).find((c) => c.code === code);
  if (foundCustom?.rateToUsdMicros) return foundCustom.rateToUsdMicros;
  return DEFAULT_CURRENCIES.find((c) => c.code === code)?.rateToUsdMicros || 1887;
}

export function convertMinor(minor, fromCode, toCode, customCurrencies = []) {
  const safeMinor = Math.trunc(Number(minor) || 0);
  if (!safeMinor || fromCode === toCode) return safeMinor;
  const fromRate = getCurrencyRateMicros(fromCode, customCurrencies);
  const toRate = getCurrencyRateMicros(toCode, customCurrencies);
  if (!fromRate || !toRate) return safeMinor;
  return Math.round((safeMinor * fromRate) / toRate);
}

export function parseMajorToMinor(rawInput) {
  if (rawInput == null) return null;
  const normalized = normalizeDigits(String(rawInput)).replace(/[,،\s]/g, "").trim();
  if (!normalized || normalized.startsWith("-")) return null;
  if (!/^\d+(\.\d{1,4})?$/.test(normalized)) return null;
  const [wholePart, fracPart = ""] = normalized.split(".");
  const paddedFrac = (fracPart + "00").slice(0, 2);
  const minor = Number(wholePart) * 100 + Number(paddedFrac);
  return Number.isSafeInteger(minor) ? minor : null;
}

export function minorToPlain(minor) {
  const safe = Math.trunc(Number(minor) || 0);
  const abs = Math.abs(safe);
  const whole = Math.floor(abs / 100);
  const cents = abs % 100;
  const sign = safe < 0 ? "-" : "";
  return cents === 0 ? `${sign}${whole}` : `${sign}${whole}.${String(cents).padStart(2, "0")}`;
}

export function formatMinor(minor, currencyCode = "YER", customCurrencies = []) {
  const safe = Math.trunc(Number(minor) || 0);
  const abs = Math.abs(safe);
  const whole = Math.floor(abs / 100).toLocaleString("en-US");
  const cents = abs % 100;
  const sign = safe < 0 ? "-" : "";
  const sym = getCurrencySymbol(currencyCode, customCurrencies);
  return cents === 0
    ? `${sign}${whole} ${sym}`
    : `${sign}${whole}.${String(cents).padStart(2, "0")} ${sym}`;
}

export function formatSignedMinor(minor, currencyCode = "YER", customCurrencies = []) {
  const safe = Math.trunc(Number(minor) || 0);
  const prefix = safe > 0 ? "+" : "";
  return `${prefix}${formatMinor(safe, currencyCode, customCurrencies)}`;
}

export function normalizeDigits(str) {
  const arabicIndic = "٠١٢٣٤٥٦٧٨٩";
  const easternIndic = "۰۱۲۳۴۵۶۷۸۹";
  return str
    .replace(/[٠-٩]/g, (d) => String(arabicIndic.indexOf(d)))
    .replace(/[۰-۹]/g, (d) => String(easternIndic.indexOf(d)));
}

export function formatDate(millis) {
  const d = new Date(Number(millis) || Date.now());
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}/${m}/${day}`;
}

export function formatDateTime(millis) {
  const d = new Date(Number(millis) || Date.now());
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${formatDate(millis)} ${hh}:${mm}`;
}

export function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

export function sanitizePhone(phone) {
  const digits = normalizeDigits(String(phone || "")).replace(/\D/g, "");
  if (!digits) return "";
  if (digits.length === 9 && digits.startsWith("7")) return `967${digits}`;
  if (digits.startsWith("00")) return digits.slice(2);
  return digits;
}

export function getDirectionLabelAr(direction) {
  return direction === "PAYABLE" ? "مستحق عليّ" : "مستحق لي";
}

export function getStatusLabelAr(status) {
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

export function getPaymentMethodLabelAr(method) {
  switch (method) {
    case "TRANSFER": return "تحويل بنكي / صرافة";
    case "CARD": return "بطاقة / محفظة إلكترونية";
    case "OTHER": return "طريقة أخرى";
    default: return "نقدي";
  }
}

export function buildCsvContent(debts = []) {
  const lines = ["\uFEFFالمعرف,الشخص,الاتجاه,أصل الدين,المتبقي,العملة,تاريخ التسجيل,تاريخ الاستحقاق,الحالة,السبب"];
  for (const d of debts || []) {
    const dir = getDirectionLabelAr(d.direction);
    const st = getStatusLabelAr(d.status);
    const safeName = String(d.contactName || "").replace(/"/g, "\"\"");
    const safeReason = String(d.reason || "").replace(/"/g, "\"\"");
    lines.push(
      `${d.id},"${safeName}","${dir}",${minorToPlain(d.principalMinor)},${minorToPlain(d.remainingMinor)},${d.currencyCode || "YER"},${formatDate(d.recordedAtMillis)},${formatDate(d.dueAtMillis)},"${st}","${safeReason}"`
    );
  }
  return `${lines.join("\n")}\n`;
}
