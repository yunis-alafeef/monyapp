import {
  convertMinor,
  formatDate,
  formatMinor,
  formatSignedMinor
} from "./money.js";

export const AI_QUICK_PROMPTS = [
  "ما إجمالي المستحق لي؟",
  "ما إجمالي المستحق عليّ؟",
  "من أكثر شخص عليه ديون؟",
  "ما الديون المتأخرة؟",
  "كم تم تحصيله هذا الشهر؟",
  "ما الأقساط المستحقة هذا الأسبوع؟",
  "اعطني ملخصاً عن هذا الشهر"
];

function isOpenDebt(d) {
  return (d.remainingMinor || 0) > 0 && d.status !== "PAID" && d.status !== "SETTLED";
}

function normalizeArabic(text) {
  return String(text || "")
    .trim()
    .toLowerCase()
    .replace(/[أإآ]/g, "ا")
    .replace(/ى/g, "ي")
    .replace(/ة/g, "ه");
}

export function computeAiSnapshot(state, nowMillis = Date.now()) {
  const cur = state?.preferences?.defaultCurrency || "YER";
  const debts = Array.isArray(state?.debts) ? state.debts : [];
  const payments = Array.isArray(state?.payments) ? state.payments : [];
  const installments = Array.isArray(state?.installments) ? state.installments : [];
  const contacts = Array.isArray(state?.contacts) ? state.contacts : [];

  const monthDate = new Date(nowMillis);
  const monthStart = new Date(monthDate.getFullYear(), monthDate.getMonth(), 1).getTime();
  const weekAhead = nowMillis + 7 * 86_400_000;

  let totalReceivable = 0;
  let totalPayable = 0;
  let totalCollected = 0;
  let totalPaidOut = 0;
  let monthCollected = 0;
  let monthPaidOut = 0;

  const debtorTotals = new Map();
  const creditorTotals = new Map();
  const overdueDebts = [];
  const dueSoonDebts = [];
  const debtMap = new Map(debts.map((d) => [d.id, d]));

  for (const d of debts) {
    const rem = convertMinor(d.remainingMinor || 0, d.currencyCode || cur, cur);
    if (d.direction === "RECEIVABLE") {
      totalReceivable += rem;
      if (isOpenDebt(d)) {
        debtorTotals.set(d.contactName || "عميل", (debtorTotals.get(d.contactName || "عميل") || 0) + rem);
      }
    } else {
      totalPayable += rem;
      if (isOpenDebt(d)) {
        creditorTotals.set(d.contactName || "جهة", (creditorTotals.get(d.contactName || "جهة") || 0) + rem);
      }
    }

    if (isOpenDebt(d)) {
      if ((d.dueAtMillis || 0) < nowMillis) {
        overdueDebts.push({ ...d, convertedRemaining: rem });
      } else if ((d.dueAtMillis || 0) <= weekAhead) {
        dueSoonDebts.push({ ...d, convertedRemaining: rem });
      }
    }
  }

  for (const p of payments) {
    const d = debtMap.get(p.debtId);
    const amt = convertMinor(p.amountMinor || 0, p.currencyCode || cur, cur);
    const isRec = d ? d.direction === "RECEIVABLE" : true;
    if (isRec) {
      totalCollected += amt;
      if ((p.paidAtMillis || 0) >= monthStart) monthCollected += amt;
    } else {
      totalPaidOut += amt;
      if ((p.paidAtMillis || 0) >= monthStart) monthPaidOut += amt;
    }
  }

  const dueInstallments = installments
    .filter((ins) => (ins.amountMinor || 0) - (ins.paidMinor || 0) > 0 && ins.status !== "PAID" && (ins.dueAtMillis || 0) <= weekAhead)
    .sort((a, b) => (a.dueAtMillis || 0) - (b.dueAtMillis || 0))
    .map((ins) => {
      const parent = debtMap.get(ins.debtId);
      const remMinor = Math.max(0, (ins.amountMinor || 0) - (ins.paidMinor || 0));
      return {
        ...ins,
        contactName: parent?.contactName || "عميل",
        currencyCode: parent?.currencyCode || cur,
        remainingMinor: remMinor,
        isOverdue: (ins.dueAtMillis || 0) < nowMillis
      };
    });

  const topDebtors = Array.from(debtorTotals.entries())
    .sort((a, b) => b[1] - a[1]);
  const topCreditors = Array.from(creditorTotals.entries())
    .sort((a, b) => b[1] - a[1]);

  overdueDebts.sort((a, b) => (a.dueAtMillis || 0) - (b.dueAtMillis || 0));

  return {
    cur,
    contactsCount: contacts.length,
    debtsCount: debts.length,
    paymentsCount: payments.length,
    totalReceivable,
    totalPayable,
    netPosition: totalReceivable - totalPayable,
    totalCollected,
    totalPaidOut,
    monthCollected,
    monthPaidOut,
    overdueDebts,
    dueSoonDebts,
    dueInstallments,
    topDebtors,
    topCreditors
  };
}

export function answerFinancialQuestionLocally({ question, state, nowMillis = Date.now() }) {
  const qRaw = String(question || "").trim();
  const q = normalizeArabic(qRaw);
  const snap = computeAiSnapshot(state, nowMillis);
  const cur = snap.cur;
  const contacts = Array.isArray(state?.contacts) ? state.contacts : [];
  const debts = Array.isArray(state?.debts) ? state.debts : [];

  // 1. Check if question mentions a specific contact name
  const matchedContact = contacts.find((c) => {
    const n = normalizeArabic(c.fullName);
    return n.length >= 2 && q.includes(n);
  });
  if (matchedContact) {
    const cDebts = debts.filter((d) => d.contactId === matchedContact.id || d.contactName === matchedContact.fullName);
    const openRec = cDebts.filter((d) => d.direction === "RECEIVABLE" && isOpenDebt(d))
      .reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode || cur, cur), 0);
    const openPay = cDebts.filter((d) => d.direction === "PAYABLE" && isOpenDebt(d))
      .reduce((s, d) => s + convertMinor(d.remainingMinor, d.currencyCode || cur, cur), 0);
    const net = openRec - openPay;
    const lines = [
      `تحليل حساب العميل: ${matchedContact.fullName}`,
      `• مستحق لك عليه: ${formatMinor(openRec, cur)}`,
      `• مستحق له عليك: ${formatMinor(openPay, cur)}`,
      `• صافي الرصيد: ${formatSignedMinor(net, cur)}`,
      `• إجمالي السجلات المرتبطة: ${cDebts.length} معاملة`
    ];
    const nextOverdue = cDebts.find((d) => isOpenDebt(d) && d.dueAtMillis < nowMillis);
    if (nextOverdue) {
      lines.push(`• تنبيه: يوجد دين متأخر منذ ${formatDate(nextOverdue.dueAtMillis)} بقيمة ${formatMinor(nextOverdue.remainingMinor, nextOverdue.currencyCode || cur)}.`);
    }
    return lines.join("\n");
  }

  // 2. Total Receivables
  if (q.includes("مستحق لي") || (q.includes("الديون التي لي") && !q.includes("علي"))) {
    const topLines = snap.topDebtors.slice(0, 3)
      .map(([name, val], i) => `${i + 1}. ${name}: ${formatMinor(val, cur)}`);
    return [
      `إجمالي المستحق لك حالياً هو ${formatMinor(snap.totalReceivable, cur)} موزعة على ${snap.topDebtors.length} عميل.`,
      topLines.length ? `أبرز المستحقات المفتوحة:\n${topLines.join("\n")}` : "لا توجد مستحقات مفتوحة حالياً.",
      snap.overdueDebts.length > 0
        ? `توصية: ركّز أولاً على متابعة ${snap.overdueDebts.length} سجل متأخر عبر سندات واتساب من تبويب الديون.`
        : "وضع التحصيل منتظم ولا توجد متأخرات حرجة."
    ].join("\n\n");
  }

  // 3. Total Payables
  if (q.includes("مستحق علي") || q.includes("التزامات") || q.includes("مطالب")) {
    const topLines = snap.topCreditors.slice(0, 3)
      .map(([name, val], i) => `${i + 1}. ${name}: ${formatMinor(val, cur)}`);
    return [
      `إجمالي المستحق عليك حالياً هو ${formatMinor(snap.totalPayable, cur)} لصالح ${snap.topCreditors.length} جهة.`,
      topLines.length ? `أبرز الالتزامات القائمة:\n${topLines.join("\n")}` : "سجلك خالٍ من أي التزامات أو ديون عليك حالياً.",
      `صافي مركزك المالي بعد خصم الالتزامات: ${formatSignedMinor(snap.netPosition, cur)}.`
    ].join("\n\n");
  }

  // 4. Top debtor
  if (q.includes("اكثر شخص") || q.includes("اكبر") || q.includes("مديون")) {
    if (!snap.topDebtors.length) {
      return "لا توجد حالياً أي ديون مفتوحة مستحقة لك على الأشخاص.";
    }
    const [topName, topAmount] = snap.topDebtors[0];
    const ranking = snap.topDebtors.slice(0, 5)
      .map(([name, val], idx) => `${idx + 1}. ${name}: ${formatMinor(val, cur)}`)
      .join("\n");
    return [
      `أكثر شخص عليه ديون مستحقة لك هو «${topName}» بإجمالي ${formatMinor(topAmount, cur)}.`,
      `قائمة أعلى الأرصدة المستحقة لك:\n${ranking}`,
      "توصية: يمكنك إرسال تذكير مطالبة متوازن عبر واتساب أو طباعة كشف حسابه مباشرة."
    ].join("\n\n");
  }

  // 5. Overdue debts
  if (q.includes("متاخر") || q.includes("متاخرات")) {
    if (!snap.overdueDebts.length) {
      return "ممتاز! لا توجد أي ديون متأخرة عن موعد استحقاقها حتى الآن.";
    }
    const overdueSum = snap.overdueDebts.reduce((s, d) => s + d.convertedRemaining, 0);
    const items = snap.overdueDebts.slice(0, 6)
      .map((d) => `• ${d.contactName}: ${formatMinor(d.remainingMinor, d.currencyCode || cur)} (استحق ${formatDate(d.dueAtMillis)})`)
      .join("\n");
    return [
      `يوجد ${snap.overdueDebts.length} دين متأخر بإجمالي يعادل ${formatMinor(overdueSum, cur)}:`,
      items,
      "توصية: ابدأ بالمطالبة بالأقدم استحقاقاً أو أعد جدولتها على أقساط منتظمة."
    ].join("\n\n");
  }

  // 6. Installments this week
  if (q.includes("اقساط") || q.includes("قسط") || q.includes("اسبوع")) {
    if (!snap.dueInstallments.length) {
      return "لا توجد أقساط متأخرة أو مستحقة خلال الأيام السبعة القادمة.";
    }
    const lines = snap.dueInstallments.slice(0, 8).map((ins) =>
      `• ${ins.contactName} — القسط #${ins.installmentNumber} (${ins.isOverdue ? "متأخر" : "قادم"}): ${formatMinor(ins.remainingMinor, ins.currencyCode)} بتاريخ ${formatDate(ins.dueAtMillis)}`
    ).join("\n");
    return [
      `الأقساط المستحقة أو المتأخرة خلال 7 أيام (${snap.dueInstallments.length} قسط):`,
      lines,
      "توصية: تم ربط هذه الأقساط بنظام التنبيهات المحلية لتذكيرك عند الاستحقاق."
    ].join("\n\n");
  }

  // 7. Monthly collections
  if (q.includes("تحصيل") || q.includes("حصل") || q.includes("محصل")) {
    return [
      `ملخص التحصيل والسداد (${cur}):`,
      `• المحصل خلال هذا الشهر: ${formatMinor(snap.monthCollected, cur)}`,
      `• المسدد للغير خلال هذا الشهر: ${formatMinor(snap.monthPaidOut, cur)}`,
      `• إجمالي التحصيل التراكمي: ${formatMinor(snap.totalCollected, cur)}`,
      `• إجمالي المسدد التراكمي: ${formatMinor(snap.totalPaidOut, cur)}`
    ].join("\n");
  }

  // 8. Comprehensive summary / General query fallback (100% grounded in real data)
  const tips = [];
  if (snap.overdueDebts.length > 0) {
    tips.push(`متابعة ${snap.overdueDebts.length} دين متأخر وتفعيل سندات التذكير عبر واتساب.`);
  }
  if (snap.dueInstallments.length > 0) {
    tips.push(`مراجعة ${snap.dueInstallments.length} قسط مستحق هذا الأسبوع لتفادي التأخير.`);
  }
  if (!tips.length) {
    tips.push("موقفك المالي منتظم — استمر بتسجيل الدفعات أولاً بأول وأخذ نسخة احتياطية دورياً.");
  }

  return [
    `الملخص المالي الذكي (${cur}):`,
    `• إجمالي المستحق لي: ${formatMinor(snap.totalReceivable, cur)}`,
    `• إجمالي المستحق عليّ: ${formatMinor(snap.totalPayable, cur)}`,
    `• صافي المركز المالي: ${formatSignedMinor(snap.netPosition, cur)}`,
    `• المحصل هذا الشهر: ${formatMinor(snap.monthCollected, cur)} | الإجمالي: ${formatMinor(snap.totalCollected, cur)}`,
    `• الديون المتأخرة: ${snap.overdueDebts.length} | أقساط هذا الأسبوع: ${snap.dueInstallments.length}`,
    `• عدد الأشخاص: ${snap.contactsCount} | إجمالي المعاملات: ${snap.debtsCount + snap.paymentsCount}`,
    `\nالتوصية العملية:\n- ${tips.join("\n- ")}`
  ].join("\n");
}
