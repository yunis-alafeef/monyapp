package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AccountType
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.InstallmentStatus
import com.example.domain.model.MessageChannel
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderPriority
import com.example.domain.model.RepeatType
import com.example.domain.model.SyncStatus
import com.example.domain.model.WorkspaceRole

@Entity(
    tableName = "contacts",
    indices = [Index("userId"), Index("fullName"), Index("phone")]
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val fullName: String,
    val phone: String,
    val address: String? = null,
    val notes: String? = null,
    val avatarUri: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "debts",
    indices = [Index("userId"), Index("contactId"), Index("status"), Index("dueAtMillis")]
)
data class DebtEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val contactId: String,
    val contactName: String,
    val direction: String = DebtDirection.RECEIVABLE.name,
    val principalMinor: Long,
    val remainingMinor: Long,
    val currencyCode: String,
    val exchangeRateMicros: Long,
    val recordedAtMillis: Long,
    val dueAtMillis: Long,
    val reason: String,
    val paymentMethod: String? = null,
    val attachmentUri: String? = null,
    val notes: String? = null,
    val status: String = DebtStatus.OPEN.name,
    val version: Long = 1L,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "payments",
    indices = [Index("userId"), Index("debtId"), Index("contactId"), Index(value = ["idempotencyKey"], unique = true)]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val debtId: String,
    val contactId: String,
    val receiptNumber: String,
    val amountMinor: Long,
    val discountMinor: Long = 0L,
    val currencyCode: String,
    val method: String = PaymentMethod.CASH.name,
    val paidAtMillis: Long,
    val balanceBeforeMinor: Long,
    val balanceAfterMinor: Long,
    val notes: String? = null,
    val idempotencyKey: String,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "installments",
    indices = [Index("userId"), Index("debtId"), Index("dueAtMillis")]
)
data class InstallmentEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val debtId: String,
    val installmentNumber: Int,
    val amountMinor: Long,
    val paidMinor: Long = 0L,
    val dueAtMillis: Long,
    val status: String = InstallmentStatus.PENDING.name,
    val paidAtMillis: Long? = null,
    val notes: String? = null,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "currencies",
    indices = [Index("userId")]
)
data class CurrencyEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val code: String,
    val nameAr: String,
    val symbol: String,
    val rateToUsdMicros: Long,
    val isCustom: Boolean = false,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "reminders",
    indices = [Index("userId"), Index("debtId"), Index("triggerAtMillis")]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val debtId: String,
    val contactId: String,
    val title: String,
    val message: String,
    val triggerAtMillis: Long,
    val repeatType: String = RepeatType.ONCE.name,
    val priority: String = ReminderPriority.NORMAL.name,
    val isEnabled: Boolean = true,
    val isSent: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "notification_logs",
    indices = [Index("userId"), Index("createdAtMillis")]
)
data class NotificationLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val category: String,
    val relatedDebtId: String? = null,
    val isRead: Boolean = false,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "message_templates",
    indices = [Index("userId")]
)
data class MessageTemplateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val titleAr: String,
    val bodyTemplate: String,
    val channel: String = MessageChannel.WHATSAPP.name,
    val updatedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "audit_logs",
    indices = [Index("userId"), Index("entityId"), Index("createdAtMillis")]
)
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val actorName: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val reason: String? = null,
    val createdAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "workspace_members",
    indices = [Index("userId")]
)
data class WorkspaceMemberEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val displayName: String,
    val email: String,
    val role: String = WorkspaceRole.STAFF.name,
    val accountType: String = AccountType.BUSINESS.name,
    val canAddDebt: Boolean = true,
    val canEditDebt: Boolean = false,
    val canRecordPayment: Boolean = true,
    val canViewReports: Boolean = false,
    val invitedAtMillis: Long,
    val syncStatus: String = SyncStatus.PENDING.name
)

@Entity(
    tableName = "sync_outbox",
    indices = [Index("userId"), Index("status"), Index(value = ["idempotencyKey"], unique = true)]
)
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val idempotencyKey: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val status: String = SyncStatus.PENDING.name,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)
