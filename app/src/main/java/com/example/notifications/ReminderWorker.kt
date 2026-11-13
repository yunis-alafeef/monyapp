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
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.HisabatiDatabase
import com.example.data.local.NotificationLogEntity
import com.example.data.repository.HisabatiRepository
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val CHANNEL_ID = "hisabati_reminders_channel"

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
        body: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun schedulePeriodicWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val reminderWork = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            "hisabati_due_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
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
}

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = HisabatiDatabase.getInstance(applicationContext)
        val now = System.currentTimeMillis()
        val soonWindow = now + 48 * 3600 * 1000L

        val debts = db.debtDao().getDebtsOnce(uid)
        var notifyIndex = 1000
        for (debt in debts) {
            if (debt.remainingMinor > 0L && debt.status != DebtStatus.PAID.name && debt.status != DebtStatus.SETTLED.name) {
                if (debt.dueAtMillis < now) {
                    val msg = "تأخر سداد مبلغ ${MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode)} من ${debt.contactName}"
                    NotificationHelper.showReminderNotification(
                        applicationContext,
                        notifyIndex++,
                        "تنبيه تأخر سداد — حساباتي",
                        msg
                    )
                    db.notificationLogDao().upsert(
                        NotificationLogEntity(
                            id = "ntf_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                            userId = uid,
                            title = "تنبيه تأخر سداد",
                            body = msg,
                            category = "OVERDUE",
                            relatedDebtId = debt.id,
                            isRead = false,
                            createdAtMillis = now
                        )
                    )
                } else if (debt.dueAtMillis <= soonWindow) {
                    val msg = "اقترب موعد استحقاق ${MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode)} لـ ${debt.contactName}"
                    NotificationHelper.showReminderNotification(
                        applicationContext,
                        notifyIndex++,
                        "موعد استحقاق قريب — حساباتي",
                        msg
                    )
                }
            }
        }
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
            NotificationHelper.schedulePeriodicWorkers(context.applicationContext)
        }
    }
}
