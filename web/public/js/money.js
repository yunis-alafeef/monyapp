export const DEFAULT_CURRENCIES = [
  { code: "YER", nameAr: "ريال يمني", symbol: "ر.ي", rateToUsdMicros: 1887 },
  { code: "SAR", nameAr: "ريال سعودي", symbol: "ر.س", rateToUsdMicros: 266667 },
  { code: "USD", nameAr: "دولار أمريكي", symbol: "$", rateToUsdMicros: 1000000 },
  { code: "AED", nameAr: "درهم إماراتي", symbol: "د.إ", rateToUsdMicros: 272294 },
  { code: "EGP", nameAr: "جنيه مصري", symbol: "ج.م", rateToUsdMicros: 20500 }
];

export function getCurrencySymbol(code) {
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

export function formatMinor(minor, currencyCode = "YER") {
  const safe = Math.trunc(Number(minor) || 0);
  const abs = Math.abs(safe);
  const whole = Math.floor(abs / 100).toLocaleString("en-US");
  const cents = abs % 100;
  const sign = safe < 0 ? "-" : "";
  const sym = getCurrencySymbol(currencyCode);
  return cents === 0
    ? `${sign}${whole} ${sym}`
    : `${sign}${whole}.${String(cents).padStart(2, "0")} ${sym}`;
}

export function formatSignedMinor(minor, currencyCode = "YER") {
  const safe = Math.trunc(Number(minor) || 0);
  const prefix = safe > 0 ? "+" : "";
  return `${prefix}${formatMinor(safe, currencyCode)}`;
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
