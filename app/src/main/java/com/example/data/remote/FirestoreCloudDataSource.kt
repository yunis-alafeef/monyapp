package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.AuditLogEntity
import com.example.data.local.ContactEntity
import com.example.data.local.CurrencyEntity
import com.example.data.local.DebtEntity
import com.example.data.local.InstallmentEntity
import com.example.data.local.MessageTemplateEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.WorkspaceMemberEntity
import com.example.domain.model.AccountType
import com.example.domain.model.CloudAuditLog
import com.example.domain.model.CloudContact
import com.example.domain.model.CloudDebt
import com.example.domain.model.CloudInstallment
import com.example.domain.model.CloudPayment
import com.example.domain.model.CloudUserProfile
import com.example.domain.model.SyncStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreCloudDataSource(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        defaultCurrency: String,
        accountType: AccountType,
        aiConsentGranted: Boolean
    ) {
        val docRef = userDoc(userId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val profile = CloudUserProfile(
                    userId = userId,
                    displayName = displayName.ifBlank { "مستخدم حساباتي" },
                    email = email,
                    accountType = accountType.name,
                    defaultCurrency = defaultCurrency,
                    aiConsentGranted = aiConsentGranted
                )
                docRef.set(profile.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserProfilePreferences(
        userId: String,
        accountType: AccountType,
        workspaceName: String,
        defaultCurrency: String,
        aiConsentGranted: Boolean
    ) {
        val docRef = userDoc(userId)
        val updates = buildMap<String, Any> {
            put("accountType", accountType.name)
            put("workspaceName", workspaceName.take(120))
            put("defaultCurrency", defaultCurrency)
            put("aiConsentGranted", aiConsentGranted)
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertContact(contact: ContactEntity) {
        val docRef = userDoc(contact.userId).collection("contacts").document(contact.id)
        try {
            val existing = docRef.get().await()
            val model = CloudContact(
                userId = contact.userId,
                fullName = contact.fullName,
                phone = contact.phone,
                address = contact.address,
                notes = contact.notes,
                avatarUri = contact.avatarUri,
                createdAtMillis = contact.createdAtMillis,
                updatedAtMillis = contact.updatedAtMillis
            )
            val payload = model.toWriteMap(isCreate = !existing.exists())
            if (existing.exists()) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteContact(userId: String, contactId: String) {
        val docRef = userDoc(userId).collection("contacts").document(contactId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun upsertDebt(debt: DebtEntity) {
        val docRef = userDoc(debt.userId).collection("debts").document(debt.id)
        try {
            val existing = docRef.get().await()
            val model = CloudDebt(
                userId = debt.userId,
                contactId = debt.contactId,
                contactName = debt.contactName,
                direction = debt.direction,
                principalMinor = debt.principalMinor,
                remainingMinor = debt.remainingMinor,
                currencyCode = debt.currencyCode,
                exchangeRateMicros = debt.exchangeRateMicros,
                recordedAtMillis = debt.recordedAtMillis,
                dueAtMillis = debt.dueAtMillis,
                reason = debt.reason,
                paymentMethod = debt.paymentMethod,
                attachmentUri = debt.attachmentUri,
                notes = debt.notes,
                status = debt.status,
                version = debt.version
            )
            if (existing.exists()) {
                docRef.update(model.toUpdateMap()).await()
            } else {
                docRef.set(model.toCreateMap()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun createPayment(payment: PaymentEntity) {
        val docRef = userDoc(payment.userId).collection("payments").document(payment.id)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) return // Idempotent: payment already synced
            val model = CloudPayment(
                userId = payment.userId,
                debtId = payment.debtId,
                contactId = payment.contactId,
                receiptNumber = payment.receiptNumber,
                amountMinor = payment.amountMinor,
                discountMinor = payment.discountMinor,
                currencyCode = payment.currencyCode,
                method = payment.method,
                paidAtMillis = payment.paidAtMillis,
                balanceBeforeMinor = payment.balanceBeforeMinor,
                balanceAfterMinor = payment.balanceAfterMinor,
                notes = payment.notes,
                idempotencyKey = payment.idempotencyKey
            )
            docRef.set(model.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertInstallment(installment: InstallmentEntity) {
        val docRef = userDoc(installment.userId).collection("installments").document(installment.id)
        try {
            val existing = docRef.get().await()
            val model = CloudInstallment(
                userId = installment.userId,
                debtId = installment.debtId,
                installmentNumber = installment.installmentNumber.toLong(),
                amountMinor = installment.amountMinor,
                paidMinor = installment.paidMinor,
                dueAtMillis = installment.dueAtMillis,
                status = installment.status,
                paidAtMillis = installment.paidAtMillis,
                notes = installment.notes
            )
            val payload = model.toWriteMap(isCreate = !existing.exists())
            if (existing.exists()) {
                docRef.update(payload).await()
            } else {
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun upsertCurrency(currency: CurrencyEntity) {
        val docRef = userDoc(currency.userId).collection("currencies").document(currency.id)
        try {
            val existing = docRef.get().await()
            val payload = buildMap<String, Any> {
                if (!existing.exists()) {
                    put("userId", currency.userId)
                    put("code", currency.code)
                    put("isCustom", currency.isCustom)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                put("nameAr", currency.nameAr)
                put("symbol", currency.symbol)
                put("rateToUsdMicros", currency.rateToUsdMicros)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (existing.exists()) docRef.update(payload).await() else docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun upsertReminder(reminder: ReminderEntity) {
        val docRef = userDoc(reminder.userId).collection("reminders").document(reminder.id)
        try {
            val existing = docRef.get().await()
            val payload = buildMap<String, Any> {
                if (!existing.exists()) {
                    put("userId", reminder.userId)
                    put("debtId", reminder.debtId)
                    put("contactId", reminder.contactId)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                put("title", reminder.title)
                put("message", reminder.message)
                put("triggerAtMillis", reminder.triggerAtMillis)
                put("repeatType", reminder.repeatType)
                put("priority", reminder.priority)
                put("isEnabled", reminder.isEnabled)
                put("isSent", reminder.isSent)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (existing.exists()) docRef.update(payload).await() else docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun upsertNotificationLog(log: NotificationLogEntity) {
        val docRef = userDoc(log.userId).collection("notification_logs").document(log.id)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "isRead" to log.isRead,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", log.userId)
                    put("title", log.title)
                    put("body", log.body)
                    put("category", log.category)
                    log.relatedDebtId?.takeIf { it.isNotBlank() }?.let { put("relatedDebtId", it) }
                    put("isRead", log.isRead)
                    put("createdAtMillis", log.createdAtMillis)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun upsertMessageTemplate(template: MessageTemplateEntity) {
        val docRef = userDoc(template.userId).collection("message_templates").document(template.id)
        try {
            val existing = docRef.get().await()
            val payload = buildMap<String, Any> {
                if (!existing.exists()) {
                    put("userId", template.userId)
                    put("type", template.type)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                put("titleAr", template.titleAr)
                put("bodyTemplate", template.bodyTemplate)
                put("channel", template.channel)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (existing.exists()) docRef.update(payload).await() else docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun createAuditLog(log: AuditLogEntity) {
        val docRef = userDoc(log.userId).collection("audit_logs").document(log.id)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) return
            val model = CloudAuditLog(
                userId = log.userId,
                actorName = log.actorName,
                action = log.action,
                entityType = log.entityType,
                entityId = log.entityId,
                details = log.details,
                reason = log.reason,
                createdAtMillis = log.createdAtMillis
            )
            docRef.set(model.toCreateMap()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertWorkspaceMember(member: WorkspaceMemberEntity) {
        val docRef = userDoc(member.userId).collection("workspace_members").document(member.id)
        try {
            val existing = docRef.get().await()
            val payload = buildMap<String, Any> {
                if (!existing.exists()) {
                    put("userId", member.userId)
                    put("email", member.email)
                    put("createdAt", FieldValue.serverTimestamp())
                }
                put("displayName", member.displayName)
                put("role", member.role)
                put("canAddDebt", member.canAddDebt)
                put("canEditDebt", member.canEditDebt)
                put("canRecordPayment", member.canRecordPayment)
                put("canViewReports", member.canViewReports)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (existing.exists()) docRef.update(payload).await() else docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorkspaceMember(userId: String, memberId: String) {
        val docRef = userDoc(userId).collection("workspace_members").document(memberId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeContacts(userId: String): Flow<List<ContactEntity>> {
        val col = userDoc(userId).collection("contacts")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(CloudContact::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map {
                        ContactEntity(
                            id = it.id,
                            userId = it.userId,
                            fullName = it.fullName,
                            phone = it.phone,
                            address = it.address,
                            notes = it.notes,
                            avatarUri = it.avatarUri,
                            createdAtMillis = it.createdAtMillis,
                            updatedAtMillis = it.updatedAtMillis,
                            syncStatus = SyncStatus.SYNCED.name
                        )
                    }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, col.path)
                throw error
            }
    }

    fun observeDebts(userId: String): Flow<List<DebtEntity>> {
        val col = userDoc(userId).collection("debts")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(CloudDebt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map {
                        DebtEntity(
                            id = it.id,
                            userId = it.userId,
                            contactId = it.contactId,
                            contactName = it.contactName,
                            direction = it.direction,
                            principalMinor = it.principalMinor,
                            remainingMinor = it.remainingMinor,
                            currencyCode = it.currencyCode,
                            exchangeRateMicros = it.exchangeRateMicros,
                            recordedAtMillis = it.recordedAtMillis,
                            dueAtMillis = it.dueAtMillis,
                            reason = it.reason,
                            paymentMethod = it.paymentMethod,
                            attachmentUri = it.attachmentUri,
                            notes = it.notes,
                            status = it.status,
                            version = it.version,
                            createdAtMillis = it.createdAt?.toDate()?.time ?: it.recordedAtMillis,
                            updatedAtMillis = it.updatedAt?.toDate()?.time ?: it.recordedAtMillis,
                            syncStatus = SyncStatus.SYNCED.name
                        )
                    }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, col.path)
                throw error
            }
    }

    fun observePayments(userId: String): Flow<List<PaymentEntity>> {
        val col = userDoc(userId).collection("payments")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(CloudPayment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map {
                        PaymentEntity(
                            id = it.id,
                            userId = it.userId,
                            debtId = it.debtId,
                            contactId = it.contactId,
                            receiptNumber = it.receiptNumber,
                            amountMinor = it.amountMinor,
                            discountMinor = it.discountMinor,
                            currencyCode = it.currencyCode,
                            method = it.method,
                            paidAtMillis = it.paidAtMillis,
                            balanceBeforeMinor = it.balanceBeforeMinor,
                            balanceAfterMinor = it.balanceAfterMinor,
                            notes = it.notes,
                            idempotencyKey = it.idempotencyKey,
                            createdAtMillis = it.createdAt?.toDate()?.time ?: it.paidAtMillis,
                            syncStatus = SyncStatus.SYNCED.name
                        )
                    }
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.LIST, col.path)
                throw error
            }
    }

    suspend fun deleteAllUserData(userId: String) {
        val collections = listOf(
            "contacts", "debts", "payments", "installments",
            "currencies", "reminders", "notification_logs",
            "message_templates", "audit_logs", "workspace_members"
        )
        for (col in collections) {
            val ref = userDoc(userId).collection(col)
            try {
                val snap = ref.get().await()
                for (doc in snap.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, ref.path)
            }
        }
        try {
            userDoc(userId).delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userDoc(userId).path)
        }
    }
}
