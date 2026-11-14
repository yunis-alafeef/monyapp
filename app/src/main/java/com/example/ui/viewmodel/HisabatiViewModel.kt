package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuditLogEntity
import com.example.data.local.ContactEntity
import com.example.data.local.CurrencyEntity
import com.example.data.local.DebtEntity
import com.example.data.local.InstallmentEntity
import com.example.data.local.MessageTemplateEntity
import com.example.data.local.NotificationLogEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.ReminderEntity
import com.example.data.local.SyncOutboxEntity
import com.example.data.local.UserAppPreferences
import com.example.data.local.WorkspaceMemberEntity
import com.example.data.repository.AiFinancialRepository
import com.example.data.repository.AiStreamEvent
import com.example.data.repository.HisabatiRepository
import com.example.data.repository.RepositoryActionResult
import com.example.domain.model.AccountType
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MessageChannel
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.domain.model.SyncStatus
import com.example.domain.model.WorkspaceRole
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MonthlyChartPoint(
    val monthLabel: String,
    val receivablesMinor: Long,
    val payablesMinor: Long,
    val collectedMinor: Long
)

data class DashboardMetrics(
    val defaultCurrencyCode: String = "YER",
    val defaultCurrencySymbol: String = "ر.ي",
    val totalReceivableMinor: Long = 0L,
    val totalPayableMinor: Long = 0L,
    val netPositionMinor: Long = 0L,
    val totalCollectedMinor: Long = 0L,
    val totalPaidOutMinor: Long = 0L,
    val overdueCount: Int = 0,
    val overdueAmountMinor: Long = 0L,
    val dueSoonCount: Int = 0,
    val dueSoonAmountMinor: Long = 0L,
    val contactsCount: Int = 0,
    val transactionsCount: Int = 0,
    val monthlySeries: List<MonthlyChartPoint> = emptyList()
)

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isError: Boolean = false
)

data class HisabatiUiState(
    val isLoading: Boolean = true,
    val preferences: UserAppPreferences = UserAppPreferences(),
    val contacts: List<ContactEntity> = emptyList(),
    val debts: List<DebtEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val installments: List<InstallmentEntity> = emptyList(),
    val currencies: List<CurrencyEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val notifications: List<NotificationLogEntity> = emptyList(),
    val templates: List<MessageTemplateEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val members: List<WorkspaceMemberEntity> = emptyList(),
    val pendingOutboxCount: Int = 0,
    val activeMember: WorkspaceMemberEntity? = null,
    val metrics: DashboardMetrics = DashboardMetrics(),
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false
)

class HisabatiViewModel(
    private val repository: HisabatiRepository,
    private val preferencesStore: PreferencesStore,
    private val aiRepository: AiFinancialRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String?
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<Pair<String, Boolean>?>(null)
    private val inFlightKeys = mutableSetOf<String>()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _aiStreamingText = MutableStateFlow<String?>(null)
    val aiStreamingText: StateFlow<String?> = _aiStreamingText.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeUserWorkspace(currentUserId, userDisplayName, userEmail)
        }
    }

    private val primaryDataFlow = combine(
        preferencesStore.preferencesFlow,
        repository.observeContacts(currentUserId),
        repository.observeDebts(currentUserId),
        repository.observePayments(currentUserId),
        repository.observeInstallments(currentUserId)
    ) { prefs, contacts, debts, payments, installments ->
        PrimaryDataBundle(prefs, contacts, debts, payments, installments)
    }

    private val secondaryDataFlow = combine(
        repository.observeCurrencies(currentUserId),
        repository.observeReminders(currentUserId),
        repository.observeNotifications(currentUserId),
        repository.observeTemplates(currentUserId),
        repository.observeAuditLogs(currentUserId)
    ) { currencies, reminders, notifications, templates, audits ->
        SecondaryDataBundle(currencies, reminders, notifications, templates, audits)
    }

    private val metaDataFlow = combine(
        repository.observeWorkspaceMembers(currentUserId),
        repository.observeOutbox(currentUserId),
        _isSyncing,
        _feedback
    ) { members, outbox, syncing, feedback ->
        MetaDataBundle(members, outbox, syncing, feedback)
    }

    val uiState: StateFlow<HisabatiUiState> = combine(
        primaryDataFlow,
        secondaryDataFlow,
        metaDataFlow
    ) { primary, secondary, meta ->
        val defaultCode = primary.prefs.defaultCurrency
        val currencyMap = secondary.currencies.associateBy { it.code }
        val targetRate = currencyMap[defaultCode]?.rateToUsdMicros ?: 1_887L
        val targetSymbol = currencyMap[defaultCode]?.symbol ?: defaultCode

        val now = System.currentTimeMillis()
        val soonBoundary = now + 7L * 86_400_000L

        var totalRec = 0L
        var totalPay = 0L
        var overdueCount = 0
        var overdueAmt = 0L
        var dueSoonCount = 0
        var dueSoonAmt = 0L

        val debtById = primary.debts.associateBy { it.id }

        for (debt in primary.debts) {
            val rate = currencyMap[debt.currencyCode]?.rateToUsdMicros ?: debt.exchangeRateMicros
            val convertedRem = MoneyMath.convertMinor(debt.remainingMinor, rate, targetRate)
            if (debt.direction == DebtDirection.RECEIVABLE.name) {
                totalRec += convertedRem
            } else {
                totalPay += convertedRem
            }
            val isOpen = debt.remainingMinor > 0L &&
                debt.status != DebtStatus.PAID.name &&
                debt.status != DebtStatus.SETTLED.name
            if (isOpen) {
                if (debt.dueAtMillis < now) {
                    overdueCount++
                    overdueAmt += convertedRem
                } else if (debt.dueAtMillis <= soonBoundary) {
                    dueSoonCount++
                    dueSoonAmt += convertedRem
                }
            }
        }

        var totalCollected = 0L
        var totalPaidOut = 0L
        for (p in primary.payments) {
            val rate = currencyMap[p.currencyCode]?.rateToUsdMicros ?: 266_667L
            val converted = MoneyMath.convertMinor(p.amountMinor, rate, targetRate)
            val parentDir = debtById[p.debtId]?.direction
            if (parentDir == DebtDirection.PAYABLE.name) {
                totalPaidOut += converted
            } else {
                totalCollected += converted
            }
        }

        val monthlySeries = buildMonthlySeries(
            debts = primary.debts,
            payments = primary.payments,
            currencyMap = currencyMap,
            targetRate = targetRate
        )

        val activeMember = meta.members.find { it.id == primary.prefs.activeMemberId }
            ?: meta.members.firstOrNull()

        val metrics = DashboardMetrics(
            defaultCurrencyCode = defaultCode,
            defaultCurrencySymbol = targetSymbol,
            totalReceivableMinor = totalRec,
            totalPayableMinor = totalPay,
            netPositionMinor = totalRec - totalPay,
            totalCollectedMinor = totalCollected,
            totalPaidOutMinor = totalPaidOut,
            overdueCount = overdueCount,
            overdueAmountMinor = overdueAmt,
            dueSoonCount = dueSoonCount,
            dueSoonAmountMinor = dueSoonAmt,
            contactsCount = primary.contacts.size,
            transactionsCount = primary.debts.size + primary.payments.size,
            monthlySeries = monthlySeries
        )

        HisabatiUiState(
            isLoading = false,
            preferences = primary.prefs,
            contacts = primary.contacts,
            debts = primary.debts,
            payments = primary.payments,
            installments = primary.installments,
            currencies = secondary.currencies,
            reminders = secondary.reminders,
            notifications = secondary.notifications,
            templates = secondary.templates,
            auditLogs = secondary.audits,
            members = meta.members,
            pendingOutboxCount = meta.outbox.count { it.status != SyncStatus.SYNCED.name },
            activeMember = activeMember,
            metrics = metrics,
            isSyncing = meta.syncing,
            feedbackMessage = meta.feedback?.first,
            isFeedbackError = meta.feedback?.second ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HisabatiUiState()
    )

    private fun buildMonthlySeries(
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>,
        currencyMap: Map<String, CurrencyEntity>,
        targetRate: Long
    ): List<MonthlyChartPoint> {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MM/yy", Locale.US)
        val result = mutableListOf<MonthlyChartPoint>()
        for (offset in 5 downTo 0) {
            val c = (cal.clone() as Calendar).apply {
                add(Calendar.MONTH, -offset)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val start = c.timeInMillis
            val end = (c.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis

            var rec = 0L
            var pay = 0L
            for (d in debts) {
                if (d.recordedAtMillis in start until end) {
                    val rate = currencyMap[d.currencyCode]?.rateToUsdMicros ?: d.exchangeRateMicros
                    val amt = MoneyMath.convertMinor(d.principalMinor, rate, targetRate)
                    if (d.direction == DebtDirection.RECEIVABLE.name) rec += amt else pay += amt
                }
            }
            var col = 0L
            for (p in payments) {
                if (p.paidAtMillis in start until end) {
                    val rate = currencyMap[p.currencyCode]?.rateToUsdMicros ?: 266_667L
                    col += MoneyMath.convertMinor(p.amountMinor, rate, targetRate)
                }
            }
            result.add(MonthlyChartPoint(monthFormat.format(Date(start)), rec, pay, col))
        }
        return result
    }

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun emitFeedback(message: String, isError: Boolean = false) {
        _feedback.value = message to isError
    }

    fun saveContact(
        existingId: String?,
        fullName: String,
        phone: String,
        address: String?,
        notes: String?,
        avatarUri: String?,
        idempotencyKey: String = UUID.randomUUID().toString(),
        onError: (String) -> Unit = {},
        onSaved: (ContactEntity) -> Unit = {}
    ) {
        if (!inFlightKeys.add("contact_$idempotencyKey")) return
        viewModelScope.launch {
            try {
                when (val res = repository.saveContact(currentUserId, existingId, fullName, phone, address, notes, avatarUri, idempotencyKey)) {
                    is RepositoryActionResult.Success -> {
                        emitFeedback("تم حفظ بيانات الشخص بنجاح")
                        onSaved(res.data)
                    }
                    is RepositoryActionResult.Error -> {
                        emitFeedback(res.messageAr, isError = true)
                        onError(res.messageAr)
                    }
                }
            } finally {
                inFlightKeys.remove("contact_$idempotencyKey")
            }
        }
    }

    fun addDebt(
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
        installmentCount: Int,
        idempotencyKey: String = UUID.randomUUID().toString(),
        onError: (String) -> Unit = {},
        onSaved: (DebtEntity) -> Unit = {}
    ) {
        if (!inFlightKeys.add("debt_$idempotencyKey")) return
        viewModelScope.launch {
            try {
                when (
                    val res = repository.addDebt(
                        userId = currentUserId,
                        contactId = contactId,
                        direction = direction,
                        principalMinor = principalMinor,
                        currencyCode = currencyCode,
                        recordedAtMillis = recordedAtMillis,
                        dueAtMillis = dueAtMillis,
                        reason = reason,
                        paymentMethod = paymentMethod,
                        attachmentUri = attachmentUri,
                        notes = notes,
                        installmentCount = installmentCount,
                        idempotencyKey = idempotencyKey
                    )
                ) {
                    is RepositoryActionResult.Success -> {
                        emitFeedback("تم تسجيل الدين وإصدار سند الإشعار بنجاح")
                        onSaved(res.data)
                    }
                    is RepositoryActionResult.Error -> {
                        emitFeedback(res.messageAr, isError = true)
                        onError(res.messageAr)
                    }
                }
            } finally {
                inFlightKeys.remove("debt_$idempotencyKey")
            }
        }
    }

    fun updateDebtDetails(
        debtId: String,
        dueAtMillis: Long,
        reason: String,
        notes: String?,
        editReason: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val res = repository.updateDebtDetails(currentUserId, debtId, dueAtMillis, reason, notes, editReason)) {
                is RepositoryActionResult.Success -> {
                    emitFeedback("تم تحديث البيانات وحفظ التعديل في سجل التدقيق")
                    onDone()
                }
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun recordPayment(
        debtId: String,
        amountMinor: Long,
        discountMinor: Long,
        method: PaymentMethod,
        paidAtMillis: Long,
        notes: String?,
        settleRemaining: Boolean,
        idempotencyKey: String = UUID.randomUUID().toString(),
        onError: (String) -> Unit = {},
        onSuccess: (PaymentEntity) -> Unit
    ) {
        if (!inFlightKeys.add("pay_$idempotencyKey")) return
        viewModelScope.launch {
            try {
                when (
                    val res = repository.recordPayment(
                        userId = currentUserId,
                        debtId = debtId,
                        amountMinor = amountMinor,
                        discountMinor = discountMinor,
                        method = method,
                        paidAtMillis = paidAtMillis,
                        notes = notes,
                        settleRemaining = settleRemaining,
                        idempotencyKey = idempotencyKey
                    )
                ) {
                    is RepositoryActionResult.Success -> onSuccess(res.data)
                    is RepositoryActionResult.Error -> {
                        emitFeedback(res.messageAr, isError = true)
                        onError(res.messageAr)
                    }
                }
            } finally {
                inFlightKeys.remove("pay_$idempotencyKey")
            }
        }
    }

    fun rescheduleInstallment(
        installmentId: String,
        newDueAtMillis: Long,
        newAmountMinor: Long,
        reason: String
    ) {
        viewModelScope.launch {
            when (val res = repository.updateInstallmentSchedule(currentUserId, installmentId, newDueAtMillis, newAmountMinor, reason)) {
                is RepositoryActionResult.Success -> emitFeedback("تم تحديث جدول القسط بنجاح")
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun addCustomCurrency(code: String, nameAr: String, symbol: String, rateToUsdMicros: Long) {
        viewModelScope.launch {
            when (val res = repository.saveCustomCurrency(currentUserId, code, nameAr, symbol, rateToUsdMicros)) {
                is RepositoryActionResult.Success -> emitFeedback("تم حفظ العملة ${res.data.nameAr} بنجاح")
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
            emitFeedback("تم حفظ التذكير")
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            repository.deleteReminder(currentUserId, id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentUserId)
        }
    }

    fun saveTemplate(template: MessageTemplateEntity) {
        viewModelScope.launch {
            repository.saveTemplate(template)
            emitFeedback("تم حفظ قالب الرسالة")
        }
    }

    fun sendBackendRelayMessage(channel: MessageChannel, phone: String, body: String, type: String) {
        viewModelScope.launch {
            when (val res = repository.sendViaBackendRelay(channel, phone, body, type)) {
                is RepositoryActionResult.Success -> emitFeedback(res.data)
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun addWorkspaceMember(
        displayName: String,
        email: String,
        role: WorkspaceRole,
        canAddDebt: Boolean,
        canEditDebt: Boolean,
        canRecordPayment: Boolean,
        canViewReports: Boolean
    ) {
        viewModelScope.launch {
            when (
                val res = repository.saveWorkspaceMember(
                    currentUserId, displayName, email, role,
                    canAddDebt, canEditDebt, canRecordPayment, canViewReports
                )
            ) {
                is RepositoryActionResult.Success -> emitFeedback("تمت إضافة ${res.data.displayName} بنجاح")
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun removeWorkspaceMember(memberId: String) {
        viewModelScope.launch {
            repository.removeWorkspaceMember(currentUserId, memberId)
            emitFeedback("تمت إزالة العضو")
        }
    }

    fun setActiveMember(memberId: String) {
        viewModelScope.launch {
            preferencesStore.setActiveMemberId(memberId)
            emitFeedback("تم تبديل المشغل النشط في مساحة العمل")
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesStore.setThemeMode(mode) }
    }

    fun setThemePalette(palette: AppThemePalette) {
        viewModelScope.launch {
            preferencesStore.setThemePalette(palette)
            emitFeedback("تم تطبيق ثيم: ${palette.titleAr}")
        }
    }

    fun setCustomThemeColors(primaryHex: Long, accentHex: Long, mode: AppThemeMode? = null) {
        viewModelScope.launch {
            preferencesStore.setCustomThemeColors(primaryHex, accentHex, mode)
            emitFeedback("تم تطبيق ألوان الثيم المخصص")
        }
    }

    fun saveCustomThemePreset(nameAr: String, primaryHex: Long, accentHex: Long, darkModePreferred: Boolean) {
        viewModelScope.launch {
            preferencesStore.saveCustomThemePreset(nameAr, primaryHex, accentHex, darkModePreferred)
            emitFeedback("تم حفظ الثيم المخصص وتفعيله")
        }
    }

    fun deleteCustomThemePreset(id: String) {
        viewModelScope.launch {
            preferencesStore.deleteCustomThemePreset(id)
            emitFeedback("تم حذف الثيم المخصص")
        }
    }

    fun setDefaultCurrency(code: String) {
        viewModelScope.launch {
            preferencesStore.setDefaultCurrency(code)
            emitFeedback("تم تغيير العملة الافتراضية إلى $code")
        }
    }

    fun setWorkspaceConfig(type: AccountType, name: String) {
        viewModelScope.launch {
            preferencesStore.setWorkspaceConfig(type, name)
            emitFeedback("تم تحديث إعدادات مساحة العمل")
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            preferencesStore.setPin(pin)
            emitFeedback("تم تفعيل رمز PIN لقفل التطبيق")
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            preferencesStore.disablePin()
            emitFeedback("تم إيقاف قفل رمز PIN")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setBiometricEnabled(enabled) }
    }

    fun setAiConsent(granted: Boolean) {
        viewModelScope.launch { preferencesStore.setAiConsentGranted(granted) }
    }

    fun setNotificationSettings(enabled: Boolean, hour: Int, minute: Int, highPriority: Boolean) {
        viewModelScope.launch {
            preferencesStore.setNotificationSettings(enabled, hour, minute, highPriority)
            emitFeedback("تم حفظ إعدادات التذكيرات")
        }
    }

    fun setMessagingRelaySettings(
        waEnabled: Boolean,
        smsEnabled: Boolean,
        backendUrl: String,
        waPhoneNumberId: String,
        smsSenderId: String
    ) {
        viewModelScope.launch {
            preferencesStore.setMessagingRelaySettings(waEnabled, smsEnabled, backendUrl, waPhoneNumberId, smsSenderId)
            emitFeedback("تم حفظ إعدادات بوابة الإرسال السحابية")
        }
    }

    fun setWhatsAppFormatSettings(
        style: String,
        includeHeader: Boolean,
        includeBreakdown: Boolean,
        includeDates: Boolean,
        includeReason: Boolean,
        includeFooter: Boolean,
        customSignature: String
    ) {
        viewModelScope.launch {
            preferencesStore.setWhatsAppFormatSettings(
                style = style,
                includeHeader = includeHeader,
                includeBreakdown = includeBreakdown,
                includeDates = includeDates,
                includeReason = includeReason,
                includeFooter = includeFooter,
                customSignature = customSignature
            )
            emitFeedback("تم حفظ تنسيق رسائل واتساب بنجاح")
        }
    }

    suspend fun exportWhatsAppConfigJson(): String = repository.exportWhatsAppConfigJson(currentUserId)

    fun importWhatsAppConfigJson(jsonText: String) {
        viewModelScope.launch {
            when (val res = repository.importWhatsAppConfigJson(currentUserId, jsonText)) {
                is RepositoryActionResult.Success -> emitFeedback("تم استيراد ${res.data} قالب وتنسيق واتساب بنجاح")
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val res = repository.syncPendingOutbox(currentUserId)) {
                is RepositoryActionResult.Success -> emitFeedback("اكتملت المزامنة السحابية (${res.data} عملية)")
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
            _isSyncing.value = false
        }
    }

    suspend fun exportBackupJson(): String = repository.exportBackupJson(currentUserId)

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            when (val res = repository.importBackupJson(currentUserId, json)) {
                is RepositoryActionResult.Success -> emitFeedback("تمت استعادة ${res.data} سجل بنجاح")
                is RepositoryActionResult.Error -> emitFeedback(res.messageAr, isError = true)
            }
        }
    }

    fun deleteAccountAndAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData(currentUserId)
            onComplete()
        }
    }

    fun askAiAssistant(question: String) {
        val state = uiState.value
        if (!state.preferences.aiConsentGranted) {
            emitFeedback("يرجى الموافقة على إرسال ملخص البيانات للمساعد الذكي أولاً", isError = true)
            return
        }
        val cleanQ = question.trim()
        if (cleanQ.isEmpty() || _aiBusy.value) return

        val userMsg = AiChatMessage("u_${System.currentTimeMillis()}", true, cleanQ)
        _aiMessages.value = _aiMessages.value + userMsg

        val contextSummary = buildAiAccountContext(state)
        viewModelScope.launch {
            _aiBusy.value = true
            aiRepository.streamFinancialAnswer(cleanQ, contextSummary).collect { event ->
                when (event) {
                    is AiStreamEvent.Loading -> _aiStreamingText.value = ""
                    is AiStreamEvent.Streaming -> _aiStreamingText.value = event.partialText
                    is AiStreamEvent.Success -> {
                        _aiStreamingText.value = null
                        _aiMessages.value = _aiMessages.value + AiChatMessage(
                            "a_${System.currentTimeMillis()}", false, event.fullText
                        )
                        _aiBusy.value = false
                    }
                    is AiStreamEvent.Error -> {
                        _aiStreamingText.value = null
                        _aiMessages.value = _aiMessages.value + AiChatMessage(
                            "e_${System.currentTimeMillis()}", false, event.messageAr, isError = true
                        )
                        _aiBusy.value = false
                    }
                }
            }
        }
    }

    private fun buildAiAccountContext(state: HisabatiUiState): String {
        val m = state.metrics
        val sym = m.defaultCurrencySymbol
        val now = System.currentTimeMillis()
        val weekAhead = now + 7L * 86_400_000L
        val df = SimpleDateFormat("yyyy/MM/dd", Locale.US)

        val overdueDebts = state.debts.filter {
            it.remainingMinor > 0L && it.dueAtMillis < now &&
                it.status != DebtStatus.PAID.name && it.status != DebtStatus.SETTLED.name
        }.take(15).joinToString("\n") { d ->
            "- ${d.contactName}: متبقي ${MoneyMath.formatMinor(d.remainingMinor, currencyCode = d.currencyCode)} (${DebtDirection.valueOf(d.direction).labelAr}) استحق بتاريخ ${df.format(Date(d.dueAtMillis))}"
        }

        val weekInstallments = state.installments.filter {
            it.dueAtMillis in now..weekAhead && it.paidMinor < it.amountMinor
        }.take(10).joinToString("\n") { ins ->
            val debt = state.debts.find { it.id == ins.debtId }
            "- ${debt?.contactName ?: "عميل"}: قسط #${ins.installmentNumber} بمبلغ ${MoneyMath.formatMinor(ins.amountMinor - ins.paidMinor, currencyCode = debt?.currencyCode ?: "")} بتاريخ ${df.format(Date(ins.dueAtMillis))}"
        }

        val topDebtors = state.debts
            .filter { it.direction == DebtDirection.RECEIVABLE.name && it.remainingMinor > 0L }
            .groupBy { it.contactName }
            .mapValues { (_, list) -> list.sumOf { it.remainingMinor } }
            .entries.sortedByDescending { it.value }.take(5)
            .joinToString("\n") { "- ${it.key}: ${MoneyMath.formatMinor(it.value, sym)}" }

        return """
            العملة الافتراضية للعرض: ${m.defaultCurrencyCode} ($sym)
            إجمالي المستحق لي: ${MoneyMath.formatMinor(m.totalReceivableMinor, sym)}
            إجمالي المستحق عليّ: ${MoneyMath.formatMinor(m.totalPayableMinor, sym)}
            صافي المركز المالي: ${MoneyMath.formatSignedMinor(m.netPositionMinor, sym)}
            إجمالي المبالغ المحصلة: ${MoneyMath.formatMinor(m.totalCollectedMinor, sym)}
            إجمالي المبالغ المسددة للغير: ${MoneyMath.formatMinor(m.totalPaidOutMinor, sym)}
            عدد الأشخاص: ${m.contactsCount} | عدد المعاملات: ${m.transactionsCount}
            الديون المتأخرة (${m.overdueCount}):
            ${overdueDebts.ifBlank { "لا توجد ديون متأخرة حالياً" }}
            أكثر الأشخاص مديونية لك:
            ${topDebtors.ifBlank { "لا توجد مستحقات مفتوحة" }}
            أقساط الأسبوع القادم:
            ${weekInstallments.ifBlank { "لا توجد أقساط مستحقة هذا الأسبوع" }}
        """.trimIndent()
    }
}

private data class PrimaryDataBundle(
    val prefs: UserAppPreferences,
    val contacts: List<ContactEntity>,
    val debts: List<DebtEntity>,
    val payments: List<PaymentEntity>,
    val installments: List<InstallmentEntity>
)

private data class SecondaryDataBundle(
    val currencies: List<CurrencyEntity>,
    val reminders: List<ReminderEntity>,
    val notifications: List<NotificationLogEntity>,
    val templates: List<MessageTemplateEntity>,
    val audits: List<AuditLogEntity>
)

private data class MetaDataBundle(
    val members: List<WorkspaceMemberEntity>,
    val outbox: List<SyncOutboxEntity>,
    val syncing: Boolean,
    val feedback: Pair<String, Boolean>?
)
