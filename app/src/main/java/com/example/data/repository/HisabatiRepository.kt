package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AuditLogEntity
import com.example.data.local.ContactEntity
import com.example.data.local.CurrencyEntity
import com.example.data.local.DebtEntity
import com.example.data.local.HisabatiDatabase
import com.example.data.local.InstallmentEntity
import com.example.data.local.MessageTemplateEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.ReminderEntity
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.WorkspaceMemberEntity
import com.example.data.remote.FirestoreCloudDataSource
import com.example.data.remote.MessagingRelayClient
import com.example.data.remote.RelayMessageRequest
import com.example.domain.model.AccountType
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.InstallmentStatus
import com.example.domain.model.MessageChannel
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReminderPriority
import com.example.domain.model.RepeatType
import com.example.domain.model.SyncStatus
import com.example.domain.model.WorkspaceRole
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface RepositoryActionResult<out T> {
    data class Success<T>(val data: T) : RepositoryActionResult<T>
    data class Error(val messageAr: String) : RepositoryActionResult<Nothing>
}

class HisabatiRepository(
    private val db: HisabatiDatabase,
    private val cloud: FirestoreCloudDataSource,
    private val preferencesStore: PreferencesStore
) {
    constructor(context: Context) : this(
        db = HisabatiDatabase.getInstance(context),
        cloud = FirestoreCloudDataSource(context),
        preferencesStore = PreferencesStore(context)
    )

    fun observeContacts(userId: String): Flow<List<ContactEntity>> = db.contactDao().observeContacts(userId)
    fun observeDebts(userId: String): Flow<List<DebtEntity>> = db.debtDao().observeDebts(userId)
    fun observePayments(userId: String): Flow<List<PaymentEntity>> = db.paymentDao().observePayments(userId)
    fun observeInstallments(userId: String): Flow<List<InstallmentEntity>> = db.installmentDao().observeInstallments(userId)
    fun observeCurrencies(userId: String): Flow<List<CurrencyEntity>> = db.currencyDao().observeCurrencies(userId)
    fun observeReminders(userId: String): Flow<List<ReminderEntity>> = db.reminderDao().observeReminders(userId)
    fun observeNotifications(userId: String): Flow<List<NotificationLogEntity>> = db.notificationLogDao().observeLogs(userId)
    fun observeTemplates(userId: String): Flow<List<MessageTemplateEntity>> = db.messageTemplateDao().observeTemplates(userId)
    fun observeAuditLogs(userId: String): Flow<List<AuditLogEntity>> = db.auditLogDao().observeAuditLogs(userId)
    fun observeWorkspaceMembers(userId: String): Flow<List<WorkspaceMemberEntity>> = db.workspaceMemberDao().observeMembers(userId)
    fun observeOutbox(userId: String): Flow<List<SyncOutboxEntity>> = db.syncOutboxDao().observeOutbox(userId)

    suspend fun initializeUserWorkspace(
        userId: String,
        displayName: String,
        email: String?
    ) {
        val now = System.currentTimeMillis()
        // Seed default currencies locally if empty
        if (db.currencyDao().getCurrenciesOnce(userId).isEmpty()) {
            val defaults = MoneyMath.defaultCurrenciesSeed().map { spec ->
                CurrencyEntity(
                    id = "cur_${spec.code}",
                    userId = userId,
                    code = spec.code,
                    nameAr = spec.nameAr,
                    symbol = spec.symbol,
                    rateToUsdMicros = spec.rateToUsdMicros,
                    isCustom = spec.isCustom,
                    updatedAtMillis = now,
                    syncStatus = SyncStatus.PENDING.name
                )
            }
            db.currencyDao().upsertAll(defaults)
            defaults.forEach { enqueueOutbox(userId, "currency", it.id, "UPSERT") }
        }

        // Seed Arabic WhatsApp & SMS templates if empty
        if (db.messageTemplateDao().getTemplatesOnce(userId).isEmpty()) {
            val templates = listOf(
                MessageTemplateEntity(
                    id = "tpl_due_soon",
                    userId = userId,
                    type = "DUE_SOON",
                    titleAr = "تذكير بقرب موعد الاستحقاق",
                    bodyTemplate = "مرحباً {الاسم}، نود تذكيركم بأن موعد استحقاق المبلغ المتبقي ({المبلغ}) الخاص بـ ({السبب}) يحين بتاريخ {التاريخ}. شكراً لتعاونكم.",
                    channel = MessageChannel.BOTH.name,
                    updatedAtMillis = now
                ),
                MessageTemplateEntity(
                    id = "tpl_overdue",
                    userId = userId,
                    type = "OVERDUE",
                    titleAr = "إشعار بتأخر السداد",
                    bodyTemplate = "الأخ الكريم {الاسم}، نرجو التكرم بمراجعة المبلغ المستحق ({المبلغ}) والمتأخر عن موعد استحقاقه بتاريخ {التاريخ}. نأمل التواصل لتنسيق السداد.",
                    channel = MessageChannel.BOTH.name,
                    updatedAtMillis = now
                ),
                MessageTemplateEntity(
                    id = "tpl_payment_receipt",
                    userId = userId,
                    type = "PAYMENT_RECEIPT",
                    titleAr = "تأكيد استلام دفعة",
                    bodyTemplate = "تم بحمد الله استلام دفعة بمبلغ {المبلغ} بتاريخ {التاريخ} (إيصال رقم {رقم_الإيصال}). الرصيد المتبقي الحالي: {المتبقي}.",
                    channel = MessageChannel.WHATSAPP.name,
                    updatedAtMillis = now
                ),
                MessageTemplateEntity(
                    id = "tpl_statement",
                    userId = userId,
                    type = "STATEMENT",
                    titleAr = "كشف حساب مختصر",
                    bodyTemplate = "كشف حساب {الاسم} من تطبيق حساباتي:\nإجمالي المستحقات: {الإجمالي}\nالمبلغ المسدد: {المسدد}\nالرصيد المتبقي: {المتبقي}.",
                    channel = MessageChannel.WHATSAPP.name,
                    updatedAtMillis = now
                ),
                MessageTemplateEntity(
                    id = "tpl_thank_you",
                    userId = userId,
                    type = "THANK_YOU",
                    titleAr = "رسالة شكر بعد التسديد الكامل",
                    bodyTemplate = "شكراً جزيلاً {الاسم} على التزامكم وسداد كامل المبلغ ({المبلغ}). نسعد دائماً بالتعامل معكم.",
                    channel = MessageChannel.WHATSAPP.name,
                    updatedAtMillis = now
                )
            )
            db.messageTemplateDao().upsertAll(templates)
            templates.forEach { enqueueOutbox(userId, "template", it.id, "UPSERT") }
        }

        // Seed owner workspace member if empty
        if (db.workspaceMemberDao().getMembersOnce(userId).isEmpty()) {
            val ownerMember = WorkspaceMemberEntity(
                id = "owner",
                userId = userId,
                displayName = displayName.ifBlank { "المالك" },
                email = email ?: "owner@hisabati.local",
                role = WorkspaceRole.OWNER.name,
                accountType = AccountType.PERSONAL.name,
                canAddDebt = true,
                canEditDebt = true,
                canRecordPayment = true,
                canViewReports = true,
                invitedAtMillis = now
            )
            db.workspaceMemberDao().upsert(ownerMember)
            enqueueOutbox(userId, "member", ownerMember.id, "UPSERT")
        }

        // Best-effort cloud profile initialization (never blocks offline startup)
        val prefs = preferencesStore.preferencesFlow.first()
        runCatching {
            cloud.ensureUserProfile(
                userId = userId,
                displayName = displayName,
                email = email,
                defaultCurrency = prefs.defaultCurrency,
                accountType = prefs.accountType,
                aiConsentGranted = prefs.aiConsentGranted
            )
        }
    }

    private suspend fun requirePermission(
        userId: String,
        check: (WorkspaceMemberEntity) -> Boolean
    ): WorkspaceMemberEntity? {
        val prefs = preferencesStore.preferencesFlow.first()
        val members = db.workspaceMemberDao().getMembersOnce(userId)
        val active = members.find { it.id == prefs.activeMemberId } ?: return null
        return if (check(active)) active else null
    }

    suspend fun saveContact(
        userId: String,
        existingId: String?,
        fullName: String,
        phone: String,
        address: String?,
        notes: String?,
        avatarUri: String?
    ): RepositoryActionResult<ContactEntity> {
        if (fullName.isBlank()) return RepositoryActionResult.Error("يرجى إدخال الاسم الكامل للشخص")
        val now = System.currentTimeMillis()
        val id = existingId ?: "cnt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val previous = existingId?.let { db.contactDao().getById(it, userId) }
        val contact = ContactEntity(
            id = id,
            userId = userId,
            fullName = fullName.trim(),
            phone = phone.trim(),
            address = address?.trim()?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            avatarUri = avatarUri?.takeIf { it.isNotEmpty() },
            createdAtMillis = previous?.createdAtMillis ?: now,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            db.contactDao().upsert(contact)
            enqueueOutbox(userId, "contact", contact.id, "UPSERT")
            recordAudit(
                userId = userId,
                action = if (previous == null) "إضافة شخص" else "تعديل بيانات شخص",
                entityType = "CONTACT",
                entityId = contact.id,
                details = "${contact.fullName} - ${contact.phone}",
                reason = null
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(contact)
    }

    suspend fun addDebt(
        userId: String,
        contactId: String,
        direction: DebtDirection,
        principalMinor: Long,
        currencyCode: String,
        recordedAtMillis: Long,
        dueAtMillis: Long,
        reason: String,
        paymentMethod: String?,
        attachmentUri: String?,
        notes: String?,
        installmentCount: Int = 0
    ): RepositoryActionResult<DebtEntity> {
        val actor = requirePermission(userId) { it.canAddDebt }
            ?: return RepositoryActionResult.Error("لا تملك صلاحية إضافة ديون في مساحة العمل الحالية")
        if (principalMinor <= 0L) return RepositoryActionResult.Error("يجب أن يكون المبلغ أكبر من صفر")
        val contact = db.contactDao().getById(contactId, userId)
            ?: return RepositoryActionResult.Error("يرجى اختيار شخص صالح")

        val currency = db.currencyDao().getCurrenciesOnce(userId).find { it.code == currencyCode }
        val rateMicros = currency?.rateToUsdMicros ?: 1_000_000L
        val now = System.currentTimeMillis()
        val debtId = "dbt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val initialStatus = when {
            installmentCount > 1 -> DebtStatus.SCHEDULED
            dueAtMillis < now -> DebtStatus.OVERDUE
            else -> DebtStatus.OPEN
        }
        val debt = DebtEntity(
            id = debtId,
            userId = userId,
            contactId = contact.id,
            contactName = contact.fullName,
            direction = direction.name,
            principalMinor = principalMinor,
            remainingMinor = principalMinor,
            currencyCode = currencyCode,
            exchangeRateMicros = rateMicros,
            recordedAtMillis = recordedAtMillis,
            dueAtMillis = dueAtMillis,
            reason = reason.trim().ifEmpty { "معاملة مالية" },
            paymentMethod = paymentMethod?.trim()?.takeIf { it.isNotEmpty() },
            attachmentUri = attachmentUri?.takeIf { it.isNotEmpty() },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            status = initialStatus.name,
            version = 1L,
            createdAtMillis = now,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING.name
        )

        db.withTransaction {
            db.debtDao().upsert(debt)
            enqueueOutbox(userId, "debt", debt.id, "UPSERT")

            if (installmentCount > 1) {
                val basePart = principalMinor / installmentCount
                val remainder = principalMinor % installmentCount
                val intervalMillis = ((dueAtMillis - recordedAtMillis).coerceAtLeast(86_400_000L)) / installmentCount
                for (i in 1..installmentCount) {
                    val amount = if (i == installmentCount) basePart + remainder else basePart
                    val instDue = recordedAtMillis + (intervalMillis * i)
                    val inst = InstallmentEntity(
                        id = "ins_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                        userId = userId,
                        debtId = debt.id,
                        installmentNumber = i,
                        amountMinor = amount,
                        paidMinor = 0L,
                        dueAtMillis = instDue,
                        status = if (instDue < now) InstallmentStatus.OVERDUE.name else InstallmentStatus.PENDING.name,
                        updatedAtMillis = now
                    )
                    db.installmentDao().upsert(inst)
                    enqueueOutbox(userId, "installment", inst.id, "UPSERT")
                }
            }

            // Auto-create default reminder 24h before due date
            val reminderTime = (dueAtMillis - 86_400_000L).coerceAtLeast(now + 3_600_000L)
            val reminder = ReminderEntity(
                id = "rem_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                userId = userId,
                debtId = debt.id,
                contactId = contact.id,
                title = "تذكير استحقاق: ${contact.fullName}",
                message = "موعد استحقاق مبلغ ${MoneyMath.formatMinor(principalMinor, currencyCode = currencyCode)} (${debt.reason})",
                triggerAtMillis = reminderTime,
                repeatType = RepeatType.ONCE.name,
                priority = ReminderPriority.HIGH.name,
                isEnabled = true,
                isSent = false,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            db.reminderDao().upsert(reminder)
            enqueueOutbox(userId, "reminder", reminder.id, "UPSERT")

            val notif = NotificationLogEntity(
                id = "ntf_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                userId = userId,
                title = "تم تسجيل ${direction.labelAr}",
                body = "${contact.fullName} - ${MoneyMath.formatMinor(principalMinor, currencyCode = currencyCode)}",
                category = "DEBT_CREATED",
                relatedDebtId = debt.id,
                isRead = false,
                createdAtMillis = now
            )
            db.notificationLogDao().upsert(notif)
            enqueueOutbox(userId, "notification", notif.id, "UPSERT")

            recordAudit(
                userId = userId,
                actorName = actor.displayName,
                action = "إضافة دين جديد",
                entityType = "DEBT",
                entityId = debt.id,
                details = "${direction.labelAr} بقيمة ${MoneyMath.formatMinor(principalMinor, currencyCode = currencyCode)} مع ${contact.fullName}",
                reason = debt.reason
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(debt)
    }

    suspend fun updateDebtDetails(
        userId: String,
        debtId: String,
        dueAtMillis: Long,
        reason: String,
        notes: String?,
        editReason: String
    ): RepositoryActionResult<DebtEntity> {
        val actor = requirePermission(userId) { it.canEditDebt }
            ?: return RepositoryActionResult.Error("لا تملك صلاحية تعديل الديون")
        if (editReason.isBlank()) return RepositoryActionResult.Error("يرجى توضيح سبب التعديل لحفظه في سجل التدقيق")
        val existing = db.debtDao().getById(debtId, userId)
            ?: return RepositoryActionResult.Error("الدين غير موجود")
        val now = System.currentTimeMillis()
        val recomputedStatus = when {
            existing.remainingMinor == 0L -> existing.status
            dueAtMillis < now && existing.status != DebtStatus.SETTLED.name -> DebtStatus.OVERDUE.name
            existing.remainingMinor < existing.principalMinor -> DebtStatus.PARTIAL.name
            else -> DebtStatus.OPEN.name
        }
        val updated = existing.copy(
            dueAtMillis = dueAtMillis,
            reason = reason.trim().ifEmpty { existing.reason },
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            status = recomputedStatus,
            version = existing.version + 1L,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            db.debtDao().upsert(updated)
            enqueueOutbox(userId, "debt", updated.id, "UPSERT")
            recordAudit(
                userId = userId,
                actorName = actor.displayName,
                action = "تعديل بيانات دين",
                entityType = "DEBT",
                entityId = updated.id,
                details = "تحديث تاريخ الاستحقاق أو الوصف مع الحفاظ على أصل الدين (${MoneyMath.formatMinor(updated.principalMinor, currencyCode = updated.currencyCode)})",
                reason = editReason.trim()
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(updated)
    }

    /**
     * Records an immutable payment (and optional agreed discount / settlement).
     * Never alters original principalMinor. Verifies balance integrity from payment history.
     * Protected by idempotencyKey to prevent duplicate submissions offline/online.
     */
    suspend fun recordPayment(
        userId: String,
        debtId: String,
        amountMinor: Long,
        discountMinor: Long = 0L,
        method: PaymentMethod,
        paidAtMillis: Long,
        notes: String?,
        settleRemaining: Boolean = false,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): RepositoryActionResult<PaymentEntity> {
        val actor = requirePermission(userId) { it.canRecordPayment }
            ?: return RepositoryActionResult.Error("لا تملك صلاحية تسجيل الدفعات")

        // Idempotency guard against double taps or outbox retries
        db.paymentDao().getByIdempotencyKey(idempotencyKey)?.let { existingPayment ->
            return RepositoryActionResult.Success(existingPayment)
        }

        if (amountMinor < 0L || discountMinor < 0L || (amountMinor + discountMinor) <= 0L) {
            return RepositoryActionResult.Error("يجب أن يكون مبلغ الدفعة أو الخصم أكبر من صفر")
        }

        val debt = db.debtDao().getById(debtId, userId)
            ?: return RepositoryActionResult.Error("الدين المطلوب غير موجود")

        // Recompute authoritative remaining balance from immutable payments ledger
        val existingPayments = db.paymentDao().getByDebtOnce(debtId, userId)
        val totalAppliedSoFar = existingPayments.sumOf { it.amountMinor + it.discountMinor }
        val verifiedBalanceBefore = (debt.principalMinor - totalAppliedSoFar).coerceAtLeast(0L)

        if (verifiedBalanceBefore == 0L) {
            return RepositoryActionResult.Error("هذا الدين مسدد بالكامل بالفعل")
        }

        val appliedTotal = amountMinor + discountMinor
        if (appliedTotal > verifiedBalanceBefore) {
            return RepositoryActionResult.Error(
                "المبلغ المدخل (${MoneyMath.formatMinor(appliedTotal, currencyCode = debt.currencyCode)}) يتجاوز الرصيد المتبقي (${MoneyMath.formatMinor(verifiedBalanceBefore, currencyCode = debt.currencyCode)})"
            )
        }

        val balanceAfter = verifiedBalanceBefore - appliedTotal
        val now = System.currentTimeMillis()
        val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val receiptNo = "HSB-${(now % 100_000_000L).toString().padStart(8, '0')}"

        val payment = PaymentEntity(
            id = paymentId,
            userId = userId,
            debtId = debt.id,
            contactId = debt.contactId,
            receiptNumber = receiptNo,
            amountMinor = amountMinor,
            discountMinor = discountMinor,
            currencyCode = debt.currencyCode,
            method = method.name,
            paidAtMillis = paidAtMillis,
            balanceBeforeMinor = verifiedBalanceBefore,
            balanceAfterMinor = balanceAfter,
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            idempotencyKey = idempotencyKey,
            createdAtMillis = now,
            syncStatus = SyncStatus.PENDING.name
        )

        val newStatus = when {
            balanceAfter == 0L && (settleRemaining || discountMinor > 0L) -> DebtStatus.SETTLED.name
            balanceAfter == 0L -> DebtStatus.PAID.name
            else -> DebtStatus.PARTIAL.name
        }

        val updatedDebt = debt.copy(
            remainingMinor = balanceAfter,
            status = newStatus,
            version = debt.version + 1L,
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING.name
        )

        db.withTransaction {
            db.paymentDao().insertImmutable(payment)
            db.debtDao().upsert(updatedDebt)
            enqueueOutbox(userId, "payment", payment.id, "UPSERT")
            enqueueOutbox(userId, "debt", updatedDebt.id, "UPSERT")

            // Apply payment to open installments sequentially
            var creditToAllocate = appliedTotal
            val installments = db.installmentDao().observeByDebt(debt.id, userId).first()
            for (inst in installments) {
                if (creditToAllocate <= 0L) break
                val unpaid = (inst.amountMinor - inst.paidMinor).coerceAtLeast(0L)
                if (unpaid > 0L) {
                    val payHere = minOf(unpaid, creditToAllocate)
                    val newPaid = inst.paidMinor + payHere
                    val instStatus = if (newPaid >= inst.amountMinor) InstallmentStatus.PAID.name else inst.status
                    val updatedInst = inst.copy(
                        paidMinor = newPaid,
                        status = instStatus,
                        paidAtMillis = if (newPaid >= inst.amountMinor) paidAtMillis else inst.paidAtMillis,
                        updatedAtMillis = now,
                        syncStatus = SyncStatus.PENDING.name
                    )
                    db.installmentDao().upsert(updatedInst)
                    enqueueOutbox(userId, "installment", updatedInst.id, "UPSERT")
                    creditToAllocate -= payHere
                }
            }

            val notif = NotificationLogEntity(
                id = "ntf_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                userId = userId,
                title = "تم تسجيل دفعة (${receiptNo})",
                body = "${debt.contactName}: ${MoneyMath.formatMinor(amountMinor, currencyCode = debt.currencyCode)} — المتبقي ${MoneyMath.formatMinor(balanceAfter, currencyCode = debt.currencyCode)}",
                category = "PAYMENT_RECORDED",
                relatedDebtId = debt.id,
                isRead = false,
                createdAtMillis = now
            )
            db.notificationLogDao().upsert(notif)
            enqueueOutbox(userId, "notification", notif.id, "UPSERT")

            recordAudit(
                userId = userId,
                actorName = actor.displayName,
                action = if (settleRemaining || discountMinor > 0L) "تسوية / دفعة بخصم" else "تسجيل دفعة",
                entityType = "PAYMENT",
                entityId = payment.id,
                details = "إيصال $receiptNo: دفع ${MoneyMath.formatMinor(amountMinor, currencyCode = debt.currencyCode)} + خصم ${MoneyMath.formatMinor(discountMinor, currencyCode = debt.currencyCode)}، المتبقي ${MoneyMath.formatMinor(balanceAfter, currencyCode = debt.currencyCode)}",
                reason = notes
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(payment)
    }

    suspend fun updateInstallmentSchedule(
        userId: String,
        installmentId: String,
        newDueAtMillis: Long,
        newAmountMinor: Long,
        reason: String
    ): RepositoryActionResult<InstallmentEntity> {
        val actor = requirePermission(userId) { it.canEditDebt }
            ?: return RepositoryActionResult.Error("لا تملك صلاحية تعديل جدول الأقساط")
        val existing = db.installmentDao().getById(installmentId, userId)
            ?: return RepositoryActionResult.Error("القسط غير موجود")
        if (newAmountMinor < existing.paidMinor || newAmountMinor <= 0L) {
            return RepositoryActionResult.Error("قيمة القسط غير صالحة مقارنة بالمبلغ المدفوع")
        }
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            dueAtMillis = newDueAtMillis,
            amountMinor = newAmountMinor,
            status = when {
                existing.paidMinor >= newAmountMinor -> InstallmentStatus.PAID.name
                newDueAtMillis < now -> InstallmentStatus.OVERDUE.name
                else -> InstallmentStatus.PENDING.name
            },
            updatedAtMillis = now,
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            db.installmentDao().upsert(updated)
            enqueueOutbox(userId, "installment", updated.id, "UPSERT")
            recordAudit(
                userId = userId,
                actorName = actor.displayName,
                action = "تعديل قسط",
                entityType = "INSTALLMENT",
                entityId = updated.id,
                details = "القسط #${updated.installmentNumber} بمبلغ ${MoneyMath.formatMinor(newAmountMinor)}",
                reason = reason.ifBlank { "تعديل مجدول" }
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(updated)
    }

    suspend fun saveCustomCurrency(
        userId: String,
        code: String,
        nameAr: String,
        symbol: String,
        rateToUsdMicros: Long
    ): RepositoryActionResult<CurrencyEntity> {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length !in 2..10 || nameAr.isBlank() || symbol.isBlank() || rateToUsdMicros <= 0L) {
            return RepositoryActionResult.Error("بيانات العملة غير مكتملة أو سعر الصرف غير صالح")
        }
        val currency = CurrencyEntity(
            id = "cur_$cleanCode",
            userId = userId,
            code = cleanCode,
            nameAr = nameAr.trim(),
            symbol = symbol.trim(),
            rateToUsdMicros = rateToUsdMicros,
            isCustom = true,
            updatedAtMillis = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING.name
        )
        db.withTransaction {
            db.currencyDao().upsert(currency)
            enqueueOutbox(userId, "currency", currency.id, "UPSERT")
            recordAudit(
                userId = userId,
                action = "إضافة/تحديث عملة",
                entityType = "CURRENCY",
                entityId = currency.id,
                details = "$nameAr ($cleanCode) بسعر صرف ميكرو $rateToUsdMicros",
                reason = null
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(currency)
    }

    suspend fun saveReminder(reminder: ReminderEntity) {
        db.withTransaction {
            db.reminderDao().upsert(reminder.copy(syncStatus = SyncStatus.PENDING.name))
            enqueueOutbox(reminder.userId, "reminder", reminder.id, "UPSERT")
        }
        syncPendingBestEffort(reminder.userId)
    }

    suspend fun deleteReminder(userId: String, reminderId: String) {
        db.reminderDao().deleteById(reminderId, userId)
    }

    suspend fun markAllNotificationsRead(userId: String) {
        db.notificationLogDao().markAllRead(userId)
    }

    suspend fun saveTemplate(template: MessageTemplateEntity) {
        db.withTransaction {
            db.messageTemplateDao().upsert(template.copy(syncStatus = SyncStatus.PENDING.name))
            enqueueOutbox(template.userId, "template", template.id, "UPSERT")
        }
        syncPendingBestEffort(template.userId)
    }

    suspend fun saveWorkspaceMember(
        userId: String,
        displayName: String,
        email: String,
        role: WorkspaceRole,
        canAddDebt: Boolean,
        canEditDebt: Boolean,
        canRecordPayment: Boolean,
        canViewReports: Boolean
    ): RepositoryActionResult<WorkspaceMemberEntity> {
        if (displayName.isBlank() || !email.contains("@")) {
            return RepositoryActionResult.Error("يرجى إدخال اسم وبريد إلكتروني صحيح")
        }
        val member = WorkspaceMemberEntity(
            id = "mbr_${UUID.randomUUID().toString().replace("-", "").take(14)}",
            userId = userId,
            displayName = displayName.trim(),
            email = email.trim(),
            role = role.name,
            accountType = AccountType.BUSINESS.name,
            canAddDebt = canAddDebt,
            canEditDebt = canEditDebt,
            canRecordPayment = canRecordPayment,
            canViewReports = canViewReports,
            invitedAtMillis = System.currentTimeMillis()
        )
        db.withTransaction {
            db.workspaceMemberDao().upsert(member)
            enqueueOutbox(userId, "member", member.id, "UPSERT")
            recordAudit(
                userId = userId,
                action = "إضافة عضو فريق",
                entityType = "WORKSPACE_MEMBER",
                entityId = member.id,
                details = "${member.displayName} (${role.labelAr})",
                reason = null
            )
        }
        syncPendingBestEffort(userId)
        return RepositoryActionResult.Success(member)
    }

    suspend fun removeWorkspaceMember(userId: String, memberId: String) {
        if (memberId == "owner") return
        db.workspaceMemberDao().deleteById(memberId, userId)
        runCatching { cloud.deleteWorkspaceMember(userId, memberId) }
    }

    suspend fun sendViaBackendRelay(
        channel: MessageChannel,
        phone: String,
        messageBody: String,
        templateType: String
    ): RepositoryActionResult<String> {
        val prefs = preferencesStore.preferencesFlow.first()
        if (channel == MessageChannel.WHATSAPP && !prefs.whatsappCloudEnabled) {
            return RepositoryActionResult.Error("الإرسال الآلي عبر WhatsApp Cloud API معطّل من الإعدادات")
        }
        if (channel == MessageChannel.SMS && !prefs.smsGatewayEnabled) {
            return RepositoryActionResult.Error("بوابة رسائل SMS معطّلة من الإعدادات")
        }
        if (prefs.backendBaseUrl.isBlank()) {
            return RepositoryActionResult.Error(
                "لم يتم ربط خادم Backend بعد. أدخل رابط الخادم الآمن (HTTPS) في إعدادات WhatsApp / SMS أولاً."
            )
        }
        val service = MessagingRelayClient.create(prefs.backendBaseUrl)
            ?: return RepositoryActionResult.Error("رابط الخادم يجب أن يستخدم بروتوكول HTTPS آمن")

        val token = runCatching {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull() ?: return RepositoryActionResult.Error("يرجى تسجيل الدخول لإرسال الطلب عبر الخادم الآمن")

        return try {
            val req = RelayMessageRequest(
                phoneNumber = phone,
                messageBody = messageBody,
                templateType = templateType,
                phoneNumberId = prefs.whatsappPhoneNumberId.takeIf { it.isNotBlank() },
                senderId = prefs.smsSenderId,
                idempotencyKey = UUID.randomUUID().toString()
            )
            val response = if (channel == MessageChannel.SMS) {
                service.sendSmsGatewayMessage("Bearer $token", req)
            } else {
                service.sendWhatsAppCloudMessage("Bearer $token", req)
            }
            if (response.isSuccessful && response.body()?.success == true) {
                RepositoryActionResult.Success("تم الإرسال بنجاح عبر الخادم (${response.body()?.providerMessageId ?: "مؤكد"})")
            } else {
                RepositoryActionResult.Error("رفض الخادم الطلب (HTTP ${response.code()}). تأكد من إعداد مفاتيح المزود على الخادم.")
            }
        } catch (e: Exception) {
            RepositoryActionResult.Error("تعذر الاتصال بخادم الإرسال: ${e.localizedMessage ?: "تحقق من الشبكة والرابط"}")
        }
    }

    /**
     * Flushes all pending outbox items idempotently to Firestore.
     * Safe to invoke from WorkManager or manual sync button.
     */
    suspend fun syncPendingOutbox(userId: String): RepositoryActionResult<Int> {
        val pending = db.syncOutboxDao().getPendingOnce(userId)
        var syncedCount = 0
        val now = System.currentTimeMillis()
        for (item in pending) {
            try {
                when (item.entityType) {
                    "contact" -> db.contactDao().getById(item.entityId, userId)?.let {
                        cloud.upsertContact(it)
                        db.contactDao().upsert(it.copy(syncStatus = SyncStatus.SYNCED.name))
                    }
                    "debt" -> db.debtDao().getById(item.entityId, userId)?.let {
                        cloud.upsertDebt(it)
                        db.debtDao().upsert(it.copy(syncStatus = SyncStatus.SYNCED.name))
                    }
                    "payment" -> db.paymentDao().getById(item.entityId, userId)?.let {
                        cloud.createPayment(it)
                    }
                    "installment" -> db.installmentDao().getById(item.entityId, userId)?.let {
                        cloud.upsertInstallment(it)
                        db.installmentDao().upsert(it.copy(syncStatus = SyncStatus.SYNCED.name))
                    }
                    "currency" -> db.currencyDao().getCurrenciesOnce(userId).find { it.id == item.entityId }?.let {
                        cloud.upsertCurrency(it)
                    }
                    "reminder" -> db.reminderDao().getEnabledOnce(userId).find { it.id == item.entityId }?.let {
                        cloud.upsertReminder(it)
                    }
                    "notification" -> db.notificationLogDao().observeLogs(userId).first().find { it.id == item.entityId }?.let {
                        cloud.upsertNotificationLog(it)
                    }
                    "template" -> db.messageTemplateDao().getTemplatesOnce(userId).find { it.id == item.entityId }?.let {
                        cloud.upsertMessageTemplate(it)
                    }
                    "audit" -> db.auditLogDao().getAuditLogsOnce(userId).find { it.id == item.entityId }?.let {
                        cloud.createAuditLog(it)
                    }
                    "member" -> db.workspaceMemberDao().getMembersOnce(userId).find { it.id == item.entityId }?.let {
                        cloud.upsertWorkspaceMember(it)
                    }
                }
                db.syncOutboxDao().updateStatus(item.id, SyncStatus.SYNCED.name, item.retryCount, null, now)
                syncedCount++
            } catch (e: Exception) {
                db.syncOutboxDao().updateStatus(
                    item.id,
                    SyncStatus.FAILED.name,
                    item.retryCount + 1,
                    e.localizedMessage,
                    now
                )
            }
        }
        db.syncOutboxDao().pruneSynced(userId)
        preferencesStore.setLastSyncAt(now)
        return RepositoryActionResult.Success(syncedCount)
    }

    private suspend fun syncPendingBestEffort(userId: String) {
        runCatching { syncPendingOutbox(userId) }
    }

    suspend fun exportBackupJson(userId: String): String {
        val contacts = db.contactDao().getContactsOnce(userId)
        val debts = db.debtDao().getDebtsOnce(userId)
        val payments = db.paymentDao().getPaymentsOnce(userId)
        val installments = db.installmentDao().getInstallmentsOnce(userId)
        val currencies = db.currencyDao().getCurrenciesOnce(userId)

        val root = JSONObject().apply {
            put("app", "HISABATI")
            put("version", 1)
            put("exportedAtMillis", System.currentTimeMillis())
            put("contacts", JSONArray().apply {
                contacts.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("fullName", c.fullName)
                        put("phone", c.phone)
                        put("address", c.address ?: "")
                        put("notes", c.notes ?: "")
                        put("createdAtMillis", c.createdAtMillis)
                        put("updatedAtMillis", c.updatedAtMillis)
                    })
                }
            })
            put("debts", JSONArray().apply {
                debts.forEach { d ->
                    put(JSONObject().apply {
                        put("id", d.id)
                        put("contactId", d.contactId)
                        put("contactName", d.contactName)
                        put("direction", d.direction)
                        put("principalMinor", d.principalMinor)
                        put("remainingMinor", d.remainingMinor)
                        put("currencyCode", d.currencyCode)
                        put("exchangeRateMicros", d.exchangeRateMicros)
                        put("recordedAtMillis", d.recordedAtMillis)
                        put("dueAtMillis", d.dueAtMillis)
                        put("reason", d.reason)
                        put("paymentMethod", d.paymentMethod ?: "")
                        put("notes", d.notes ?: "")
                        put("status", d.status)
                        put("version", d.version)
                    })
                }
            })
            put("payments", JSONArray().apply {
                payments.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("debtId", p.debtId)
                        put("contactId", p.contactId)
                        put("receiptNumber", p.receiptNumber)
                        put("amountMinor", p.amountMinor)
                        put("discountMinor", p.discountMinor)
                        put("currencyCode", p.currencyCode)
                        put("method", p.method)
                        put("paidAtMillis", p.paidAtMillis)
                        put("balanceBeforeMinor", p.balanceBeforeMinor)
                        put("balanceAfterMinor", p.balanceAfterMinor)
                        put("notes", p.notes ?: "")
                        put("idempotencyKey", p.idempotencyKey)
                    })
                }
            })
            put("installments", JSONArray().apply {
                installments.forEach { i ->
                    put(JSONObject().apply {
                        put("id", i.id)
                        put("debtId", i.debtId)
                        put("installmentNumber", i.installmentNumber)
                        put("amountMinor", i.amountMinor)
                        put("paidMinor", i.paidMinor)
                        put("dueAtMillis", i.dueAtMillis)
                        put("status", i.status)
                    })
                }
            })
            put("currencies", JSONArray().apply {
                currencies.forEach { cur ->
                    put(JSONObject().apply {
                        put("id", cur.id)
                        put("code", cur.code)
                        put("nameAr", cur.nameAr)
                        put("symbol", cur.symbol)
                        put("rateToUsdMicros", cur.rateToUsdMicros)
                        put("isCustom", cur.isCustom)
                    })
                }
            })
        }
        preferencesStore.setLastBackupAt(System.currentTimeMillis())
        return root.toString(2)
    }

    suspend fun importBackupJson(userId: String, jsonText: String): RepositoryActionResult<Int> {
        return try {
            val root = JSONObject(jsonText)
            if (root.optString("app") != "HISABATI") {
                return RepositoryActionResult.Error("ملف النسخة الاحتياطية غير صالح لتطبيق حساباتي")
            }
            val now = System.currentTimeMillis()
            var count = 0
            db.withTransaction {
                val contactsArr = root.optJSONArray("contacts") ?: JSONArray()
                for (idx in 0 until contactsArr.length()) {
                    val o = contactsArr.getJSONObject(idx)
                    val c = ContactEntity(
                        id = o.getString("id"),
                        userId = userId,
                        fullName = o.getString("fullName"),
                        phone = o.optString("phone"),
                        address = o.optString("address").takeIf { it.isNotBlank() },
                        notes = o.optString("notes").takeIf { it.isNotBlank() },
                        createdAtMillis = o.optLong("createdAtMillis", now),
                        updatedAtMillis = o.optLong("updatedAtMillis", now)
                    )
                    db.contactDao().upsert(c)
                    enqueueOutbox(userId, "contact", c.id, "UPSERT")
                    count++
                }
                val debtsArr = root.optJSONArray("debts") ?: JSONArray()
                for (idx in 0 until debtsArr.length()) {
                    val o = debtsArr.getJSONObject(idx)
                    val d = DebtEntity(
                        id = o.getString("id"),
                        userId = userId,
                        contactId = o.getString("contactId"),
                        contactName = o.getString("contactName"),
                        direction = o.getString("direction"),
                        principalMinor = o.getLong("principalMinor"),
                        remainingMinor = o.getLong("remainingMinor"),
                        currencyCode = o.getString("currencyCode"),
                        exchangeRateMicros = o.optLong("exchangeRateMicros", 1_000_000L),
                        recordedAtMillis = o.optLong("recordedAtMillis", now),
                        dueAtMillis = o.optLong("dueAtMillis", now),
                        reason = o.optString("reason"),
                        paymentMethod = o.optString("paymentMethod").takeIf { it.isNotBlank() },
                        notes = o.optString("notes").takeIf { it.isNotBlank() },
                        status = o.getString("status"),
                        version = o.optLong("version", 1L),
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                    db.debtDao().upsert(d)
                    enqueueOutbox(userId, "debt", d.id, "UPSERT")
                    count++
                }
                val payArr = root.optJSONArray("payments") ?: JSONArray()
                for (idx in 0 until payArr.length()) {
                    val o = payArr.getJSONObject(idx)
                    val p = PaymentEntity(
                        id = o.getString("id"),
                        userId = userId,
                        debtId = o.getString("debtId"),
                        contactId = o.getString("contactId"),
                        receiptNumber = o.getString("receiptNumber"),
                        amountMinor = o.getLong("amountMinor"),
                        discountMinor = o.optLong("discountMinor", 0L),
                        currencyCode = o.getString("currencyCode"),
                        method = o.getString("method"),
                        paidAtMillis = o.optLong("paidAtMillis", now),
                        balanceBeforeMinor = o.getLong("balanceBeforeMinor"),
                        balanceAfterMinor = o.getLong("balanceAfterMinor"),
                        notes = o.optString("notes").takeIf { it.isNotBlank() },
                        idempotencyKey = o.getString("idempotencyKey"),
                        createdAtMillis = now
                    )
                    db.paymentDao().insertImmutable(p)
                    enqueueOutbox(userId, "payment", p.id, "UPSERT")
                    count++
                }
            }
            syncPendingBestEffort(userId)
            RepositoryActionResult.Success(count)
        } catch (e: Exception) {
            RepositoryActionResult.Error("تعذر استيراد النسخة الاحتياطية: ${e.localizedMessage ?: "صيغة غير صالحة"}")
        }
    }

    suspend fun deleteAllUserData(userId: String) {
        cloud.deleteAllUserData(userId)
        db.withTransaction {
            db.syncOutboxDao().deleteAllForUser(userId)
            db.auditLogDao().deleteAllForUser(userId)
            db.workspaceMemberDao().deleteAllForUser(userId)
            db.messageTemplateDao().deleteAllForUser(userId)
            db.notificationLogDao().deleteAllForUser(userId)
            db.reminderDao().deleteAllForUser(userId)
            db.installmentDao().deleteAllForUser(userId)
            db.paymentDao().deleteAllForUser(userId)
            db.debtDao().deleteAllForUser(userId)
            db.contactDao().deleteAllForUser(userId)
            db.currencyDao().deleteAllForUser(userId)
        }
        preferencesStore.clearAll()
    }

    private suspend fun enqueueOutbox(
        userId: String,
        entityType: String,
        entityId: String,
        operation: String
    ) {
        val now = System.currentTimeMillis()
        val key = "${entityType}_${entityId}_${now}_${UUID.randomUUID().toString().take(6)}"
        db.syncOutboxDao().enqueue(
            SyncOutboxEntity(
                id = "out_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                userId = userId,
                idempotencyKey = key,
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                status = SyncStatus.PENDING.name,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    private suspend fun recordAudit(
        userId: String,
        actorName: String = "صاحب الحساب",
        action: String,
        entityType: String,
        entityId: String,
        details: String,
        reason: String?
    ) {
        val now = System.currentTimeMillis()
        val log = AuditLogEntity(
            id = "aud_${UUID.randomUUID().toString().replace("-", "").take(14)}",
            userId = userId,
            actorName = actorName,
            action = action,
            entityType = entityType,
            entityId = entityId,
            details = details,
            reason = reason?.takeIf { it.isNotBlank() },
            createdAtMillis = now
        )
        db.auditLogDao().insertImmutable(log)
        enqueueOutbox(userId, "audit", log.id, "UPSERT")
    }
}
