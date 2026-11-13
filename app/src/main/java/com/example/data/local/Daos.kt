package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeContacts(userId: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    suspend fun getContactsOnce(userId: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeDebts(userId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    suspend fun getDebtsOnce(userId: String): List<DebtEntity>

    @Query("SELECT * FROM debts WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE contactId = :contactId AND userId = :userId ORDER BY recordedAtMillis DESC")
    fun observeByContact(contactId: String, userId: String): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(debts: List<DebtEntity>)

    @Query("DELETE FROM debts WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM debts WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY paidAtMillis DESC")
    fun observePayments(userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY paidAtMillis DESC")
    suspend fun getPaymentsOnce(userId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE debtId = :debtId AND userId = :userId ORDER BY paidAtMillis DESC")
    fun observeByDebt(debtId: String, userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId AND userId = :userId")
    suspend fun getByDebtOnce(debtId: String, userId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): PaymentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImmutable(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments WHERE userId = :userId ORDER BY dueAtMillis ASC")
    fun observeInstallments(userId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE userId = :userId ORDER BY dueAtMillis ASC")
    suspend fun getInstallmentsOnce(userId: String): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE debtId = :debtId AND userId = :userId ORDER BY installmentNumber ASC")
    fun observeByDebt(debtId: String, userId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getById(id: String, userId: String): InstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(installment: InstallmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(installments: List<InstallmentEntity>)

    @Query("DELETE FROM installments WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currencies WHERE userId = :userId ORDER BY isCustom ASC, code ASC")
    fun observeCurrencies(userId: String): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE userId = :userId")
    suspend fun getCurrenciesOnce(userId: String): List<CurrencyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(currency: CurrencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(currencies: List<CurrencyEntity>)

    @Query("DELETE FROM currencies WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY triggerAtMillis ASC")
    fun observeReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND isEnabled = 1 ORDER BY triggerAtMillis ASC")
    suspend fun getEnabledOnce(userId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeLogs(userId: String): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: NotificationLogEntity)

    @Query("UPDATE notification_logs SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllRead(userId: String)

    @Query("DELETE FROM notification_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates WHERE userId = :userId")
    fun observeTemplates(userId: String): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE userId = :userId")
    suspend fun getTemplatesOnce(userId: String): List<MessageTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: MessageTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(templates: List<MessageTemplateEntity>)

    @Query("DELETE FROM message_templates WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeAuditLogs(userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY createdAtMillis DESC")
    suspend fun getAuditLogsOnce(userId: String): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImmutable(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface WorkspaceMemberDao {
    @Query("SELECT * FROM workspace_members WHERE userId = :userId ORDER BY invitedAtMillis ASC")
    fun observeMembers(userId: String): Flow<List<WorkspaceMemberEntity>>

    @Query("SELECT * FROM workspace_members WHERE userId = :userId")
    suspend fun getMembersOnce(userId: String): List<WorkspaceMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(member: WorkspaceMemberEntity)

    @Query("DELETE FROM workspace_members WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: String, userId: String)

    @Query("DELETE FROM workspace_members WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE userId = :userId ORDER BY createdAtMillis ASC")
    fun observeOutbox(userId: String): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE userId = :userId AND status != 'SYNCED' ORDER BY createdAtMillis ASC")
    suspend fun getPendingOnce(userId: String): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(item: SyncOutboxEntity): Long

    @Query("UPDATE sync_outbox SET status = :status, retryCount = :retryCount, lastError = :lastError, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, retryCount: Int, lastError: String?, updatedAtMillis: Long)

    @Query("DELETE FROM sync_outbox WHERE userId = :userId AND status = 'SYNCED'")
    suspend fun pruneSynced(userId: String)

    @Query("DELETE FROM sync_outbox WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
