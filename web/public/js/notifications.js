import { formatMinor } from "./money.js";

const DAY_MS = 86_400_000;
const SOON_WINDOW_MS = 48 * 3_600_000;

export function buildInstallmentSchedule({
  debtId,
  userId,
  principalMinor,
  installmentCount,
  recordedAtMillis,
  dueAtMillis,
  nowMillis = Date.now()
}) {
  const count = Math.max(0, Math.min(36, Math.trunc(Number(installmentCount) || 0)));
  if (count <= 1 || principalMinor <= 0) return [];
  const basePart = Math.floor(principalMinor / count);
  const remainder = principalMinor % count;
  const interval = Math.max(DAY_MS, Math.floor((dueAtMillis - recordedAtMillis) / count));

  const list = [];
  for (let i = 1; i <= count; i++) {
    const amount = i === count ? basePart + remainder : basePart;
    const instDue = recordedAtMillis + interval * i;
    list.push({
      id: `ins_${debtId}_${i}`,
      userId: userId || "local_device",
      debtId,
      installmentNumber: i,
      amountMinor: amount,
      paidMinor: 0,
      dueAtMillis: instDue,
      status: instDue < nowMillis ? "OVERDUE" : "PENDING",
      paidAtMillis: null,
      updatedAtMillis: nowMillis
    });
  }
  return list;
}

export function allocatePaymentToInstallments(installments, creditMinor, paidAtMillis = Date.now()) {
  let remainingCredit = Math.max(0, Math.trunc(Number(creditMinor) || 0));
  const sorted = [...(installments || [])].sort((a, b) => a.installmentNumber - b.installmentNumber);
  const modified = [];

  for (const inst of sorted) {
    if (remainingCredit <= 0) break;
    const unpaid = Math.max(0, (inst.amountMinor || 0) - (inst.paidMinor || 0));
    if (unpaid <= 0) continue;
    const apply = Math.min(unpaid, remainingCredit);
    const newPaid = (inst.paidMinor || 0) + apply;
    const fullyPaid = newPaid >= inst.amountMinor;
    modified.push({
      ...inst,
      paidMinor: newPaid,
      status: fullyPaid ? "PAID" : inst.status,
      paidAtMillis: fullyPaid ? paidAtMillis : inst.paidAtMillis,
      updatedAtMillis: paidAtMillis
    });
    remainingCredit -= apply;
  }
  return modified;
}

/**
 * Evaluates due/overdue debts, unpaid installments, and custom reminders locally.
 * Pure & deterministic for unit testing and offline PWA runtime.
 */
export function evaluateDueNotifications({
  debts = [],
  installments = [],
  reminders = [],
  existingNotifications = [],
  prefs = {},
  nowMillis = Date.now()
}) {
  if (prefs.notificationsEnabled === false) {
    return { newNotifications: [], updatedInstallments: [], updatedReminders: [] };
  }

  const dayBucket = Math.floor(nowMillis / DAY_MS);
  const soonWindow = nowMillis + SOON_WINDOW_MS;
  const existingIds = new Set((existingNotifications || []).map((n) => n.id));
  const debtMap = new Map((debts || []).map((d) => [d.id, d]));

  const newNotifications = [];
  const updatedInstallments = [];
  const updatedReminders = [];

  // 1) Open Debts
  for (const debt of debts || []) {
    const isOpen = (debt.remainingMinor || 0) > 0 && debt.status !== "PAID" && debt.status !== "SETTLED";
    if (!isOpen) continue;

    const dirAction = debt.direction === "RECEIVABLE" ? "تحصيل من" : "سداد إلى";
    const amtText = formatMinor(debt.remainingMinor, debt.currencyCode);

    if (debt.dueAtMillis < nowMillis) {
      const id = `due_debt_over_${debt.id}_${dayBucket}`;
      if (!existingIds.has(id)) {
        existingIds.add(id);
        newNotifications.push({
          id,
          title: `تنبيه تأخر استحقاق — ${debt.contactName}`,
          body: `تأخر موعد ${dirAction} ${debt.contactName} بمبلغ ${amtText} (${debt.reason || "قيد مالي"})`,
          category: "OVERDUE",
          relatedDebtId: debt.id,
          isRead: false,
          createdAtMillis: nowMillis
        });
      }
    } else if (debt.dueAtMillis <= soonWindow) {
      const id = `due_debt_soon_${debt.id}_${dayBucket}`;
      if (!existingIds.has(id)) {
        existingIds.add(id);
        newNotifications.push({
          id,
          title: `اقترب موعد استحقاق — ${debt.contactName}`,
          body: `موعد ${dirAction} ${debt.contactName} قريب بمبلغ ${amtText} (${debt.reason || "قيد مالي"})`,
          category: "DUE_SOON",
          relatedDebtId: debt.id,
          isRead: false,
          createdAtMillis: nowMillis
        });
      }
    }
  }

  // 2) Installments
  for (const inst of installments || []) {
    const remainingInst = Math.max(0, (inst.amountMinor || 0) - (inst.paidMinor || 0));
    if (remainingInst <= 0 || inst.status === "PAID") continue;

    const parentDebt = debtMap.get(inst.debtId);
    const currency = parentDebt?.currencyCode || prefs.defaultCurrency || "YER";
    const contactName = parentDebt?.contactName || "عميل";
    const dirAction = parentDebt?.direction === "PAYABLE" ? "سداد قسط إلى" : "تحصيل قسط من";
    const instAmt = formatMinor(remainingInst, currency);

    if (inst.dueAtMillis < nowMillis) {
      if (inst.status !== "OVERDUE") {
        updatedInstallments.push({ ...inst, status: "OVERDUE", updatedAtMillis: nowMillis });
      }
      const id = `due_inst_over_${inst.id}_${dayBucket}`;
      if (!existingIds.has(id)) {
        existingIds.add(id);
        newNotifications.push({
          id,
          title: `قسط متأخر (#${inst.installmentNumber}) — ${contactName}`,
          body: `تأخر ${dirAction} ${contactName} (قسط رقم ${inst.installmentNumber}) بمبلغ ${instAmt}`,
          category: "INSTALLMENT_OVERDUE",
          relatedDebtId: inst.debtId,
          isRead: false,
          createdAtMillis: nowMillis
        });
      }
    } else if (inst.dueAtMillis <= soonWindow) {
      const id = `due_inst_soon_${inst.id}_${dayBucket}`;
      if (!existingIds.has(id)) {
        existingIds.add(id);
        newNotifications.push({
          id,
          title: `تذكير قسط مستحق (#${inst.installmentNumber}) — ${contactName}`,
          body: `اقترب موعد ${dirAction} ${contactName} (قسط رقم ${inst.installmentNumber}) بمبلغ ${instAmt}`,
          category: "INSTALLMENT_DUE_SOON",
          relatedDebtId: inst.debtId,
          isRead: false,
          createdAtMillis: nowMillis
        });
      }
    }
  }

  // 3) Custom Scheduled Reminders
  for (const rem of reminders || []) {
    if (rem.isEnabled === false || rem.isSent || rem.triggerAtMillis > nowMillis) continue;
    const id = `rem_trig_${rem.id}_${dayBucket}`;
    if (!existingIds.has(id)) {
      existingIds.add(id);
      newNotifications.push({
        id,
        title: rem.title,
        body: rem.message,
        category: "CUSTOM_REMINDER",
        relatedDebtId: rem.debtId || null,
        isRead: false,
        createdAtMillis: nowMillis
      });
    }
    const repeatInterval =
      rem.repeatType === "DAILY" ? DAY_MS :
      rem.repeatType === "WEEKLY" ? 7 * DAY_MS :
      rem.repeatType === "MONTHLY" ? 30 * DAY_MS : 0;

    if (repeatInterval > 0) {
      let nextTrigger = rem.triggerAtMillis + repeatInterval;
      while (nextTrigger <= nowMillis) nextTrigger += repeatInterval;
      updatedReminders.push({ ...rem, triggerAtMillis: nextTrigger, isSent: false, updatedAtMillis: nowMillis });
    } else {
      updatedReminders.push({ ...rem, isSent: true, updatedAtMillis: nowMillis });
    }
  }

  return { newNotifications, updatedInstallments, updatedReminders };
}

export function isIosDevice() {
  if (typeof navigator === "undefined") return false;
  return /iphone|ipad|ipod/i.test(navigator.userAgent || "") ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

export function isStandalonePwa() {
  if (typeof window === "undefined") return false;
  return Boolean(window.matchMedia?.("(display-mode: standalone)")?.matches || window.navigator?.standalone === true);
}

export function getNotificationPermissionStatus() {
  if (typeof window === "undefined" || !("Notification" in window)) return "unsupported";
  return Notification.permission;
}

export async function requestNotificationPermission() {
  if (typeof window === "undefined" || !("Notification" in window)) return "unsupported";
  try {
    return await Notification.requestPermission();
  } catch {
    return Notification.permission || "denied";
  }
}

export async function dispatchSystemNotification({ title, body, tag, data = {} }) {
  if (typeof window === "undefined" || !("Notification" in window)) return false;
  if (Notification.permission !== "granted") return false;
  const options = {
    body,
    icon: "./icons/icon-192.png",
    badge: "./icons/icon-192.png",
    dir: "rtl",
    lang: "ar",
    tag: tag || `hisabati_${Date.now()}`,
    renotify: false,
    data
  };
  try {
    if ("serviceWorker" in navigator) {
      const reg = await Promise.race([
        navigator.serviceWorker.ready,
        new Promise((r) => setTimeout(() => r(null), 1000))
      ]);
      if (reg && typeof reg.showNotification === "function") {
        await reg.showNotification(title, options);
        return true;
      }
    }
    new Notification(title, options);
    return true;
  } catch {
    try {
      new Notification(title, options);
      return true;
    } catch {
      return false;
    }
  }
}
