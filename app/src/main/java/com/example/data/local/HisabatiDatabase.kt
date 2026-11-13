package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ContactEntity::class,
        DebtEntity::class,
        PaymentEntity::class,
        InstallmentEntity::class,
        CurrencyEntity::class,
        ReminderEntity::class,
        NotificationLogEntity::class,
        MessageTemplateEntity::class,
        AuditLogEntity::class,
        WorkspaceMemberEntity::class,
        SyncOutboxEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HisabatiDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun debtDao(): DebtDao
    abstract fun paymentDao(): PaymentDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun currencyDao(): CurrencyDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun messageTemplateDao(): MessageTemplateDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun workspaceMemberDao(): WorkspaceMemberDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile
        private var INSTANCE: HisabatiDatabase? = null

        fun getInstance(context: Context): HisabatiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HisabatiDatabase::class.java,
                    "hisabati_ledger.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
