import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { formatMinor, parseMajorToMinor } from "../public/js/money.js";
import { buildFormattedWhatsAppMessage } from "../public/js/whatsapp.js";
import { buildInvoiceHtml, PDF_TEMPLATES } from "../public/js/print.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.resolve(__dirname, "../public");
const rootDir = path.resolve(__dirname, "../..");

test("PWA manifest, icons, Apple meta tags, and silent SW registration exist", () => {
  const manifestRaw = fs.readFileSync(path.join(publicDir, "manifest.json"), "utf8");
  const manifest = JSON.parse(manifestRaw);
  assert.equal(manifest.short_name, "حساباتي");
  assert.equal(manifest.dir, "rtl");
  assert.equal(manifest.display, "standalone");
  assert.ok(manifest.icons.some((i) => i.sizes === "192x192"));
  assert.ok(manifest.icons.some((i) => i.sizes === "512x512"));

  assert.ok(fs.existsSync(path.join(publicDir, "icons/icon-192.png")));
  assert.ok(fs.existsSync(path.join(publicDir, "icons/icon-512.png")));
  assert.ok(fs.existsSync(path.join(publicDir, "icons/apple-touch-icon.png")));

  const indexHtml = fs.readFileSync(path.join(publicDir, "index.html"), "utf8");
  assert.ok(indexHtml.includes('rel="manifest"'));
  assert.ok(indexHtml.includes('name="apple-mobile-web-app-capable"'));
  assert.ok(indexHtml.includes('rel="apple-touch-icon"'));

  const swJs = fs.readFileSync(path.join(publicDir, "sw.js"), "utf8");
  assert.ok(swJs.includes("networkFirst"));
  assert.ok(swJs.includes("staleWhileRevalidate"));

  const appJs = fs.readFileSync(path.join(publicDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes('navigator.serviceWorker.register("./sw.js"'));

  const readme = fs.readFileSync(path.join(rootDir, "README.md"), "utf8");
  assert.ok(readme.includes("تحميل واستخدام التطبيق على الآيفون (بدون إنترنت)"));
  assert.ok(readme.includes("[ضع رابط الاستضافة هنا]"));
});

test("Yemeni Rial minor-unit math, WhatsApp formatter, and 4 invoice templates work", () => {
  assert.equal(parseMajorToMinor("1,250.50"), 125050);
  assert.equal(parseMajorToMinor("١٠٠"), 10000);
  assert.equal(formatMinor(500000, "YER"), "5,000 ر.ي");

  const msg = buildFormattedWhatsAppMessage(
    { workspaceName: "متجر اليمن", whatsappStyle: "FORMAL" },
    {
      title: "سند مطالبة",
      contactName: "خالد علي",
      remainingFormatted: "5,000 ر.ي",
      receiptNumber: "DBT-101"
    }
  );
  assert.ok(msg.includes("متجر اليمن"));
  assert.ok(msg.includes("خالد علي"));
  assert.ok(msg.includes("5,000 ر.ي"));

  for (const tpl of PDF_TEMPLATES) {
    const html = buildInvoiceHtml(
      { printTemplate: tpl.id, printCompanyName: "مؤسسة الأمل", printDebtColorHex: "#0B1B32" },
      {
        docKind: "DEBT_VOUCHER",
        title: "سند قيد مالي",
        subtitle: "العميل: خالد علي",
        summary: [["المبلغ", "5,000 ر.ي"]],
        rows: [{ date: "2026/11/14", title: "فاتورة", amount: "5,000 ر.ي", status: "مفتوح" }]
      }
    );
    assert.ok(html.includes("مؤسسة الأمل"));
    assert.ok(html.includes("772317950"));
  }
});
