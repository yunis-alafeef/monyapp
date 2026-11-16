package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.HisabatiDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.UserAppPreferences
import com.example.data.repository.HisabatiRepository
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.InstallmentStatus
import com.example.domain.model.MoneyMath
import com.example.domain.model.RepeatType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "hisabati_reminders_channel"
    private const val DAY_MS = 86_400_000L
    private const val SOON_WINDOW_MS = 48L * 3_600_000L

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val desc = context.getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = desc
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        highPriority: Boolean = true
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val priority = if (highPriority) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun schedulePeriodicWorkers(
        context: Context,
        preferredHour: Int = 9,
        preferredMinute: Int = 0
    ) {
        val workManager = WorkManager.getInstance(context)

        // Align next scheduled check close to preferred reminder hour while keeping 6-hour periodic cadence
        val initialDelayMs = computeDelayToNextReminderWindow(preferredHour, preferredMinute)
        val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "hisabati_due_reminders",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderWork
        )

        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncWork = PeriodicWorkRequestBuilder<OfflineSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(syncConstraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "hisabati_offline_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    fun triggerImmediateDueCheck(context: Context) {
        val work = OneTimeWorkRequestBuilder<ReminderWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "hisabati_immediate_due_check",
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun scheduleOneShotReminder(context: Context, triggerAtMillis: Long) {
        val delayMs = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(5_000L)
        val work = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "hisabati_custom_reminder_${triggerAtMillis / 60_000L}",
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    private fun computeDelayToNextReminderWindow(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }

    /**
     * Evaluates overdue/due-soon debts, due/overdue installments, and enabled custom reminders.
     * Creates deduplicated daily logs in NotificationLogDao and dispatches Android local notifications.
     */
    suspend fun evaluateDueNotificationsForUser(
        context: Context,
        db: HisabatiDatabase,
        uid: String,
        prefs: UserAppPreferences,
        nowMillis: Long = System.currentTimeMillis(),
        postSystemNotifications: Boolean = true
    ): Int {
        if (!prefs.notificationsEnabled) return 0

        val dayBucket = nowMillis / DAY_MS
        val soonWindow = nowMillis + SOON_WINDOW_MS
        val existingIds = db.notificationLogDao().getLogsOnce(uid).mapTo(mutableSetOf()) { it.id }

        val debts = db.debtDao().getDebtsOnce(uid)
        val debtMap = debts.associateBy { it.id }
        var createdCount = 0

        // 1) Evaluate Debts (Overdue & Due Soon within 48 hours)
        for (debt in debts) {
            val isOpen = debt.remainingMinor > 0L &&
                debt.status != DebtStatus.PAID.name &&
                debt.status != DebtStatus.SETTLED.name
            if (!isOpen) continue

            val dirAction = if (debt.direction == DebtDirection.RECEIVABLE.name) {
                "تحصيل من"
            } else {
                "سداد إلى"
            }
            val amtText = MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode)

            if (debt.dueAtMillis < nowMillis) {
                val logId = "due_debt_over_${debt.id}_$dayBucket"
                if (existingIds.add(logId)) {
                    val title = "تنبيه تأخر استحقاق — ${debt.contactName}"
                    val msg = "تأخر موعد $dirAction ${debt.contactName} بمبلغ $amtText (${debt.reason})"
                    db.notificationLogDao().upsert(
                        NotificationLogEntity(
                            id = logId,
                            userId = uid,
                            title = title,
                            body = msg,
                            category = "OVERDUE",
                            relatedDebtId = debt.id,
                            isRead = false,
                            createdAtMillis = nowMillis
                        )
                    )
                    if (postSystemNotifications) {
                        showReminderNotification(
                            context = context,
                            notificationId = ("debt_over_${debt.id}").hashCode(),
                            title = title,
                            body = msg,
                            highPriority = prefs.reminderPriorityHigh
                        )
                    }
                    createdCount++
                }
            } else if (debt.dueAtMillis <= soonWindow) {
                val logId = "due_debt_soon_${debt.id}_$dayBucket"
                if (existingIds.add(logId)) {
                    val title = "اقترب موعد استحقاق — ${debt.contactName}"
                    val msg = "موعد $dirAction ${debt.contactName} قريب بمبلغ $amtText (${debt.reason})"
                    db.notificationLogDao().upsert(
                        NotificationLogEntity(
                            id = logId,
                            userId = uid,
                            title = title,
                            body = msg,
                            category = "DUE_SOON",
                            relatedDebtId = debt.id,
                            isRead = false,
                            createdAtMillis = nowMillis
                        )
                    )
                    if (postSystemNotifications) {
                        showReminderNotification(
                            context = context,
                            notificationId = ("debt_soon_${debt.id}").hashCode(),
                            title = title,
                            body = msg,
                            highPriority = prefs.reminderPriorityHigh
                        )
                    }
                    createdCount++
                }
            }
        }

        // 2) Evaluate Installments (Overdue & Due Soon within 48 hours)
        val installments = db.installmentDao().getInstallmentsOnce(uid)
        for (inst in installments) {
            val remainingInst = (inst.amountMinor - inst.paidMinor).coerceAtLeast(0L)
            if (remainingInst <= 0L || inst.status == InstallmentStatus.PAID.name) continue

            val parentDebt = debtMap[inst.debtId]
            val currencyCode = parentDebt?.currencyCode ?: prefs.defaultCurrency
            val contactName = parentDebt?.contactName ?: "عميل"
            val dirAction = if (parentDebt?.direction == DebtDirection.PAYABLE.name) {
                "سداد قسط إلى"
            } else {
                "تحصيل قسط من"
            }
            val instAmtText = MoneyMath.formatMinor(remainingInst, currencyCode = currencyCode)

            if (inst.dueAtMillis < nowMillis) {
                if (inst.status != InstallmentStatus.OVERDUE.name) {
                    db.installmentDao().upsert(
                        inst.copy(status = InstallmentStatus.OVERDUE.name, updatedAtMillis = nowMillis)
                    )
                }
                val logId = "due_inst_over_${inst.id}_$dayBucket"
                if (existingIds.add(logId)) {
                    val title = "قسط متأخر (#${inst.installmentNumber}) — $contactName"
                    val msg = "تأخر $dirAction $contactName (قسط رقم ${inst.installmentNumber}) بمبلغ $instAmtText"
                    db.notificationLogDao().upsert(
                        NotificationLogEntity(
                            id = logId,
                            userId = uid,
                            title = title,
                            body = msg,
                            category = "INSTALLMENT_OVERDUE",
                            relatedDebtId = inst.debtId,
                            isRead = false,
                            createdAtMillis = nowMillis
                        )
                    )
                    if (postSystemNotifications) {
                        showReminderNotification(
                            context = context,
                            notificationId = ("inst_over_${inst.id}").hashCode(),
                            title = title,
                            body = msg,
                            highPriority = prefs.reminderPriorityHigh
                        )
                    }
                    createdCount++
                }
            } else if (inst.dueAtMillis <= soonWindow) {
                val logId = "due_inst_soon_${inst.id}_$dayBucket"
                if (existingIds.add(logId)) {
                    val title = "تذكير قسط مستحق (#${inst.installmentNumber}) — $contactName"
                    val msg = "اقترب موعد $dirAction $contactName (قسط رقم ${inst.installmentNumber}) بمبلغ $instAmtText"
                    db.notificationLogDao().upsert(
                        NotificationLogEntity(
                            id = logId,
                            userId = uid,
                            title = title,
                            body = msg,
                            category = "INSTALLMENT_DUE_SOON",
                            relatedDebtId = inst.debtId,
                            isRead = false,
                            createdAtMillis = nowMillis
                        )
                    )
                    if (postSystemNotifications) {
                        showReminderNotification(
                            context = context,
                            notificationId = ("inst_soon_${inst.id}").hashCode(),
                            title = title,
                            body = msg,
                            highPriority = prefs.reminderPriorityHigh
                        )
                    }
                    createdCount++
                }
            }
        }

        // 3) Evaluate Custom Scheduled Reminders
        val reminders = db.reminderDao().getEnabledOnce(uid)
        for (rem in reminders) {
            if (!rem.isEnabled || rem.isSent || rem.triggerAtMillis > nowMillis) continue

            val logId = "rem_trig_${rem.id}_$dayBucket"
            if (existingIds.add(logId)) {
                db.notificationLogDao().upsert(
                    NotificationLogEntity(
                        id = logId,
                        userId = uid,
                        title = rem.title,
                        body = rem.message,
                        category = "CUSTOM_REMINDER",
                        relatedDebtId = rem.debtId.ifBlank { null },
                        isRead = false,
                        createdAtMillis = nowMillis
                    )
                )
                if (postSystemNotifications) {
                    showReminderNotification(
                        context = context,
                        notificationId = ("rem_${rem.id}").hashCode(),
                        title = rem.title,
                        body = rem.message,
                        highPriority = prefs.reminderPriorityHigh
                    )
                }
                createdCount++
            }

            val repeatInterval = when (runCatching { RepeatType.valueOf(rem.repeatType) }.getOrDefault(RepeatType.ONCE)) {
                RepeatType.DAILY -> DAY_MS
                RepeatType.WEEKLY -> 7L * DAY_MS
                RepeatType.MONTHLY -> 30L * DAY_MS
                RepeatType.ONCE -> 0L
            }
            if (repeatInterval > 0L) {
                var nextTrigger = rem.triggerAtMillis + repeatInterval
                while (nextTrigger <= nowMillis) {
                    nextTrigger += repeatInterval
                }
                db.reminderDao().upsert(
                    rem.copy(triggerAtMillis = nextTrigger, isSent = false, updatedAtMillis = nowMillis)
                )
            } else {
                db.reminderDao().upsert(
                    rem.copy(isSent = true, updatedAtMillis = nowMillis)
                )
            }
        }

        return createdCount
    }
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = HisabatiDatabase.getInstance(applicationContext)
        val prefs = PreferencesStore(applicationContext).preferencesFlow.first()
        NotificationHelper.evaluateDueNotificationsForUser(
            context = applicationContext,
            db = db,
            uid = uid,
            prefs = prefs,
            nowMillis = System.currentTimeMillis(),
            postSystemNotifications = true
        )
        return Result.success()
    }
}

class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val repo = HisabatiRepository(applicationContext)
        repo.syncPendingOutbox(uid)
        return Result.success()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appCtx = context.applicationContext
            NotificationHelper.schedulePeriodicWorkers(appCtx)
            NotificationHelper.triggerImmediateDueCheck(appCtx)
        }
    }
}
