import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { convertMinor, formatMinor, parseMajorToMinor } from "../public/js/money.js";
import { buildFormattedWhatsAppMessage } from "../public/js/whatsapp.js";
import { buildInvoiceHtml, PDF_TEMPLATES } from "../public/js/print.js";
import {
  allocatePaymentToInstallments,
  buildInstallmentSchedule,
  evaluateDueNotifications
} from "../public/js/notifications.js";
import {
  buildMonthlySeries,
  renderMonthlyCashflowChartHtml,
  renderReceivablePayableDonutHtml
} from "../public/js/charts.js";
import { AI_QUICK_PROMPTS, answerFinancialQuestionLocally } from "../public/js/ai.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.resolve(__dirname, "../public");
const rootDir = path.resolve(__dirname, "../..");

test("PWA manifest, icons, Apple meta tags, notificationclick SW, and silent registration exist", () => {
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
  assert.ok(swJs.includes("notificationclick"));
  assert.ok(swJs.includes("./js/notifications.js"));
  assert.ok(swJs.includes("./js/charts.js"));
  assert.ok(swJs.includes("./js/ai.js"));

  const appJs = fs.readFileSync(path.join(publicDir, "js/app.js"), "utf8");
  assert.ok(appJs.includes('navigator.serviceWorker.register("./sw.js"'));
  assert.ok(appJs.includes("renderAiAssistantPage"));

  const readme = fs.readFileSync(path.join(rootDir, "README.md"), "utf8");
  assert.ok(readme.includes("تحميل واستخدام التطبيق على الآيفون (بدون إنترنت)"));
  assert.ok(readme.includes("[ضع رابط الاستضافة هنا]"));
});

test("Yemeni Rial minor-unit math, currency conversion, WhatsApp formatter, and 4 invoice templates work", () => {
  assert.equal(parseMajorToMinor("1,250.50"), 125050);
  assert.equal(parseMajorToMinor("١٠٠"), 10000);
  assert.equal(formatMinor(500000, "YER"), "5,000 ر.ي");
  assert.ok(convertMinor(10000, "USD", "YER") > 10000);

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

test("Installment scheduling, payment allocation, local notification evaluation, and SVG charts work offline", () => {
  const now = 1_760_000_000_000;
  const DAY = 86_400_000;

  const schedule = buildInstallmentSchedule({
    debtId: "dbt_1",
    userId: "u1",
    principalMinor: 100_000,
    installmentCount: 3,
    recordedAtMillis: now - 10 * DAY,
    dueAtMillis: now + 20 * DAY,
    nowMillis: now
  });
  assert.equal(schedule.length, 3);
  assert.equal(schedule.reduce((s, i) => s + i.amountMinor, 0), 100_000);

  const modified = allocatePaymentToInstallments(schedule, 40_000, now);
  assert.equal(modified[0].status, "PAID");
  assert.equal(modified[1].paidMinor, 40_000 - schedule[0].amountMinor);

  const debt = {
    id: "dbt_over",
    contactName: "طارق سعيد",
    direction: "RECEIVABLE",
    principalMinor: 100_000,
    remainingMinor: 60_000,
    currencyCode: "YER",
    recordedAtMillis: now - 20 * DAY,
    dueAtMillis: now - 2 * DAY,
    reason: "فاتورة",
    status: "PARTIAL"
  };

  const disabledEval = evaluateDueNotifications({
    debts: [debt],
    installments: schedule,
    reminders: [],
    existingNotifications: [],
    prefs: { notificationsEnabled: false },
    nowMillis: now
  });
  assert.equal(disabledEval.newNotifications.length, 0);

  const eval1 = evaluateDueNotifications({
    debts: [debt],
    installments: [
      { id: "ins_due_soon", debtId: "dbt_over", installmentNumber: 2, amountMinor: 30_000, paidMinor: 0, dueAtMillis: now + 12 * 3_600_000, status: "PENDING" }
    ],
    reminders: [
      { id: "rem_1", debtId: "dbt_over", title: "متابعة قسط", message: "رسالة", triggerAtMillis: now - 1000, repeatType: "DAILY", isEnabled: true, isSent: false }
    ],
    existingNotifications: [],
    prefs: { notificationsEnabled: true, defaultCurrency: "YER" },
    nowMillis: now
  });
  assert.equal(eval1.newNotifications.length, 3);
  assert.ok(eval1.newNotifications.some((n) => n.category === "OVERDUE"));
  assert.ok(eval1.newNotifications.some((n) => n.category === "INSTALLMENT_DUE_SOON"));
  assert.ok(eval1.newNotifications.some((n) => n.category === "CUSTOM_REMINDER"));
  assert.ok(eval1.updatedReminders[0].triggerAtMillis > now);

  // Deduplicates same day
  const eval2 = evaluateDueNotifications({
    debts: [debt],
    installments: [
      { id: "ins_due_soon", debtId: "dbt_over", installmentNumber: 2, amountMinor: 30_000, paidMinor: 0, dueAtMillis: now + 12 * 3_600_000, status: "PENDING" }
    ],
    reminders: eval1.updatedReminders,
    existingNotifications: eval1.newNotifications,
    prefs: { notificationsEnabled: true, defaultCurrency: "YER" },
    nowMillis: now + 60_000
  });
  assert.equal(eval2.newNotifications.length, 0);

  const series = buildMonthlySeries([debt], [], "YER", now);
  assert.equal(series.length, 6);
  const cashflowHtml = renderMonthlyCashflowChartHtml(series);
  assert.ok(cashflowHtml.includes("<svg"));
  const donutHtml = renderReceivablePayableDonutHtml(60_000, 20_000, "YER");
  assert.ok(donutHtml.includes("75%"));
});

test("Web/iPhone PWA Smart Financial AI answers receivables, overdue debts, installments, and specific contact lookups offline", () => {
  const now = 1_760_000_000_000;
  const DAY = 86_400_000;
  const state = {
    preferences: { defaultCurrency: "YER", aiConsentGranted: true },
    contacts: [{ id: "c1", fullName: "أحمد السقاف", phone: "771122334" }],
    debts: [
      {
        id: "d1",
        contactId: "c1",
        contactName: "أحمد السقاف",
        direction: "RECEIVABLE",
        principalMinor: 150_000,
        remainingMinor: 100_000,
        currencyCode: "YER",
        recordedAtMillis: now - 15 * DAY,
        dueAtMillis: now - 3 * DAY,
        status: "PARTIAL"
      },
      {
        id: "d2",
        contactId: "c2",
        contactName: "مؤسسة البناء",
        direction: "PAYABLE",
        principalMinor: 40_000,
        remainingMinor: 40_000,
        currencyCode: "YER",
        recordedAtMillis: now - 5 * DAY,
        dueAtMillis: now + 10 * DAY,
        status: "OPEN"
      }
    ],
    payments: [{ id: "p1", debtId: "d1", amountMinor: 50_000, currencyCode: "YER", paidAtMillis: now - DAY }],
    installments: [
      { id: "i1", debtId: "d1", installmentNumber: 2, amountMinor: 50_000, paidMinor: 0, dueAtMillis: now + 2 * DAY, status: "PENDING" }
    ]
  };

  assert.equal(AI_QUICK_PROMPTS.length, 7);
  const recAnswer = answerFinancialQuestionLocally({ question: "ما إجمالي المستحق لي؟", state, nowMillis: now });
  assert.ok(recAnswer.includes("1,000 ر.ي"));
  assert.ok(recAnswer.includes("أحمد السقاف"));

  const overdueAnswer = answerFinancialQuestionLocally({ question: "ما الديون المتأخرة؟", state, nowMillis: now });
  assert.ok(overdueAnswer.includes("أحمد السقاف"));

  const instAnswer = answerFinancialQuestionLocally({ question: "ما الأقساط المستحقة هذا الأسبوع؟", state, nowMillis: now });
  assert.ok(instAnswer.includes("#2"));

  const contactAnswer = answerFinancialQuestionLocally({ question: "كم حساب أحمد السقاف؟", state, nowMillis: now });
  assert.ok(contactAnswer.includes("أحمد السقاف"));
  assert.ok(contactAnswer.includes("1,000 ر.ي"));

  const summaryAnswer = answerFinancialQuestionLocally({ question: "اعطني ملخصاً عن هذا الشهر", state, nowMillis: now });
  assert.ok(summaryAnswer.includes("صافي المركز المالي"));
});
