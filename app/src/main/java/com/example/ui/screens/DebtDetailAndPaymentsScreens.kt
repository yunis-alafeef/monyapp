package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.DebtEntity
import com.example.data.local.InstallmentEntity
import com.example.data.local.PaymentEntity
import com.example.domain.model.DebtDirection
import com.example.domain.model.InstallmentStatus
import com.example.domain.model.MessageChannel
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.export.ArabicReportExporter
import com.example.export.StatementPdfLine
import com.example.messaging.ExternalMessagingHelper
import com.example.ui.components.DebtStatusBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.components.PaymentSuccessCelebrationDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PayableCoral
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HisabatiUiState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    debt: DebtEntity,
    state: HisabatiUiState,
    onBack: () -> Unit,
    onRecordPayment: () -> Unit,
    onOpenInstallments: () -> Unit,
    onPaymentClick: (String) -> Unit,
    onUpdateDebtDetails: (dueAtMillis: Long, reason: String, notes: String?, editReason: String) -> Unit,
    onSendCloudMessage: (MessageChannel, String, String, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val contact = state.contacts.find { it.id == debt.contactId }
    val debtPayments = state.payments.filter { it.debtId == debt.id }
    val debtInstallments = state.installments.filter { it.debtId == debt.id }
    val debtAudits = state.auditLogs.filter { it.entityId == debt.id || debtPayments.any { p -> p.id == it.entityId } }
    val paidTotalMinor = debt.principalMinor - debt.remainingMinor

    var showEditDialog by remember { mutableStateOf(false) }

    val reminderTemplate = state.templates.find {
        it.type == if (debt.dueAtMillis < System.currentTimeMillis()) "OVERDUE" else "DUE_SOON"
    }?.bodyTemplate ?: "مرحباً {الاسم}، تذكير بالمبلغ المتبقي {المبلغ} بتاريخ {التاريخ}."

    val preparedReminder = ExternalMessagingHelper.interpolateTemplate(
        template = reminderTemplate,
        contactName = debt.contactName,
        amountFormatted = MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode),
        remainingFormatted = MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode),
        reason = debt.reason,
        dateLabel = ArabicReportExporter.formatDate(debt.dueAtMillis)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الدين") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "تعديل")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                FintechSurfaceCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(debt.contactName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(debt.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DebtStatusBadge(debt.status)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("أصل الدين (ثابت)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyMath.formatMinor(debt.principalMinor, currencyCode = debt.currencyCode), fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("المسدد / المخصوم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyMath.formatMinor(paidTotalMinor, currencyCode = debt.currencyCode), color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("الرصيد المتبقي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode), color = PayableCoral, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "التسجيل: ${ArabicReportExporter.formatDate(debt.recordedAtMillis)}  •  الاستحقاق: ${ArabicReportExporter.formatDate(debt.dueAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!debt.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("ملاحظات: ${debt.notes}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!debt.attachmentUri.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        AsyncImage(
                            model = debt.attachmentUri,
                            contentDescription = "مرفق الدين",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (debt.remainingMinor > 0L) {
                        Button(
                            onClick = onRecordPayment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("debt_detail_record_payment"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Rounded.Payments, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تسجيل دفعة أو تسوية", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onOpenInstallments, modifier = Modifier.weight(1f)) {
                            Text("الأقساط (${debtInstallments.size})")
                        }
                        OutlinedButton(
                            onClick = {
                                val phone = contact?.phone.orEmpty()
                                ExternalMessagingHelper.openWhatsAppChat(context, phone, preparedReminder)
                            },
                            enabled = !contact?.phone.isNullOrBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("تذكير واتساب")
                        }
                        OutlinedButton(
                            onClick = {
                                val phone = contact?.phone.orEmpty()
                                ExternalMessagingHelper.openSmsComposer(context, phone, preparedReminder)
                            },
                            enabled = !contact?.phone.isNullOrBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SMS")
                        }
                    }
                    if (state.preferences.whatsappCloudEnabled && !contact?.phone.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onSendCloudMessage(MessageChannel.WHATSAPP, contact!!.phone, preparedReminder, "DUE_REMINDER")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إرسال آلي عبر WhatsApp Cloud API")
                        }
                    }
                }
            }

            item {
                Text("سجل الدفعات والإيصالات (${debtPayments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (debtPayments.isEmpty()) {
                item {
                    FintechSurfaceCard {
                        Text("لا توجد دفعات مسجلة لهذا الدين بعد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(debtPayments, key = { it.id }) { p ->
                    FintechSurfaceCard(onClick = { onPaymentClick(p.id) }) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("إيصال #${p.receiptNumber}", fontWeight = FontWeight.Bold)
                                Text(
                                    "${PaymentMethod.valueOf(p.method).labelAr} • ${ArabicReportExporter.formatDate(p.paidAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode), color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                if (p.discountMinor > 0L) {
                                    Text("خصم: ${MoneyMath.formatMinor(p.discountMinor)}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("سجل التدقيق غير القابل للتلاعب (${debtAudits.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(debtAudits, key = { it.id }) { log ->
                FintechSurfaceCard {
                    Text("${log.action} — ${log.actorName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                    if (!log.reason.isNullOrBlank()) {
                        Text("السبب: ${log.reason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(ArabicReportExporter.formatDate(log.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showEditDialog) {
        EditDebtDetailsDialog(
            debt = debt,
            onDismiss = { showEditDialog = false },
            onConfirm = { newDue, newReason, newNotes, auditReason ->
                onUpdateDebtDetails(newDue, newReason, newNotes, auditReason)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditDebtDetailsDialog(
    debt: DebtEntity,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String?, String) -> Unit
) {
    val context = LocalContext.current
    var dueMillis by remember { mutableLongStateOf(debt.dueAtMillis) }
    var reason by remember { mutableStateOf(debt.reason) }
    var notes by remember { mutableStateOf(debt.notes ?: "") }
    var editReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل تفاصيل الدين (مع حفظ سجل التدقيق)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "ملاحظة: أصل الدين (${MoneyMath.formatMinor(debt.principalMinor, currencyCode = debt.currencyCode)}) لا يتغير حمايةً لدقة السجل.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dueMillis }
                        DatePickerDialog(context, { _, y, m, d ->
                            dueMillis = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تاريخ الاستحقاق: ${ArabicReportExporter.formatDate(dueMillis)}")
                }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("الوصف") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") })
                OutlinedTextField(value = editReason, onValueChange = { editReason = it }, label = { Text("سبب التعديل (إلزامي) *") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dueMillis, reason, notes, editReason) },
                enabled = editReason.isNotBlank()
            ) { Text("حفظ التعديل") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    debt: DebtEntity,
    onBack: () -> Unit,
    onSubmitPayment: (
        amountMinor: Long,
        discountMinor: Long,
        method: PaymentMethod,
        paidAtMillis: Long,
        notes: String?,
        settleRemaining: Boolean,
        onSuccess: (PaymentEntity) -> Unit
    ) -> Unit,
    onViewReceipt: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var amountText by remember { mutableStateOf(MoneyMath.minorToBigDecimal(debt.remainingMinor).toPlainString()) }
    var discountText by remember { mutableStateOf("0") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var paidAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notes by remember { mutableStateOf("") }
    var settleRemaining by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var completedPayment by remember { mutableStateOf<PaymentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تسجيل دفعة مالية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FintechSurfaceCard {
                Text(debt.contactName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "الرصيد المتبقي الحالي: ${MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = PayableCoral,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { amountText = MoneyMath.minorToBigDecimal(debt.remainingMinor).toPlainString() },
                    modifier = Modifier.weight(1f)
                ) { Text("سداد كامل") }
                OutlinedButton(
                    onClick = { amountText = MoneyMath.minorToBigDecimal(debt.remainingMinor / 2).toPlainString() },
                    modifier = Modifier.weight(1f)
                ) { Text("نصف المبلغ") }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("المبلغ المدفوع (${debt.currencyCode}) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_amount_input")
            )

            OutlinedTextField(
                value = discountText,
                onValueChange = { discountText = it },
                label = { Text("خصم متفق عليه / تسوية (اختياري)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = settleRemaining, onCheckedChange = { settleRemaining = it })
                Text("اعتبار العملية تسوية نهائية للدين", style = MaterialTheme.typography.bodyMedium)
            }

            Text("طريقة الدفع", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PaymentMethod.entries.forEach { pm ->
                    FilterChip(
                        selected = method == pm,
                        onClick = { method = pm },
                        label = { Text(pm.labelAr) }
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = paidAtMillis }
                    DatePickerDialog(context, { _, y, m, d ->
                        paidAtMillis = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("تاريخ الدفعة: ${ArabicReportExporter.formatDate(paidAtMillis)}")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات الدفع") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMsg != null) {
                Text(errorMsg!!, color = PayableCoral, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val amt = MoneyMath.parseMajorToMinor(amountText) ?: -1L
                    val disc = MoneyMath.parseMajorToMinor(discountText) ?: 0L
                    if (amt < 0L || disc < 0L || (amt + disc) <= 0L) {
                        errorMsg = "يرجى إدخال مبلغ صحيح"
                        return@Button
                    }
                    if (amt + disc > debt.remainingMinor) {
                        errorMsg = "المبلغ يتجاوز الرصيد المتبقي (${MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode)})"
                        return@Button
                    }
                    errorMsg = null
                    onSubmitPayment(amt, disc, method, paidAtMillis, notes, settleRemaining) { created ->
                        completedPayment = created
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_payment_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تأكيد وإصدار الإيصال", fontWeight = FontWeight.Bold)
            }
        }
    }

    completedPayment?.let { payment ->
        PaymentSuccessCelebrationDialog(
            receiptNumber = payment.receiptNumber,
            amountFormatted = MoneyMath.formatMinor(payment.amountMinor, currencyCode = payment.currencyCode),
            remainingFormatted = MoneyMath.formatMinor(payment.balanceAfterMinor, currencyCode = payment.currencyCode),
            onViewReceipt = {
                val id = payment.id
                completedPayment = null
                onViewReceipt(id)
            },
            onDismiss = {
                completedPayment = null
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(
    debt: DebtEntity,
    installments: List<InstallmentEntity>,
    canEditSchedule: Boolean,
    onBack: () -> Unit,
    onPayInstallment: () -> Unit,
    onReschedule: (installmentId: String, newDueMillis: Long, newAmountMinor: Long, reason: String) -> Unit
) {
    BackHandler(onBack = onBack)
    var editingInst by remember { mutableStateOf<InstallmentEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جدول الأقساط — ${debt.contactName}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        if (installments.isEmpty()) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "لا توجد أقساط مجدولة",
                    subtitle = "يمكنك سداد الدين مباشرة بدفعات مرنة.",
                    actionLabel = "تسجيل دفعة",
                    onAction = onPayInstallment
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(installments, key = { it.id }) { inst ->
                    val status = runCatching { InstallmentStatus.valueOf(inst.status) }.getOrDefault(InstallmentStatus.PENDING)
                    FintechSurfaceCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("القسط #${inst.installmentNumber}", fontWeight = FontWeight.Bold)
                                Text("الاستحقاق: ${ArabicReportExporter.formatDate(inst.dueAtMillis)}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "المبلغ: ${MoneyMath.formatMinor(inst.amountMinor, currencyCode = debt.currencyCode)} | المدفوع: ${MoneyMath.formatMinor(inst.paidMinor, currencyCode = debt.currencyCode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    status.labelAr,
                                    color = when (status) {
                                        InstallmentStatus.PAID -> EmeraldPrimary
                                        InstallmentStatus.OVERDUE -> PayableCoral
                                        InstallmentStatus.PENDING -> WarningAmber
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                if (canEditSchedule && status != InstallmentStatus.PAID) {
                                    TextButton(onClick = { editingInst = inst }) { Text("تعديل الموعد") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingInst?.let { inst ->
        val context = LocalContext.current
        var dueMillis by remember { mutableLongStateOf(inst.dueAtMillis) }
        var amountText by remember { mutableStateOf(MoneyMath.minorToBigDecimal(inst.amountMinor).toPlainString()) }
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editingInst = null },
            title = { Text("تعديل القسط #${inst.installmentNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dueMillis }
                        DatePickerDialog(context, { _, y, m, d ->
                            dueMillis = Calendar.getInstance().apply { set(y, m, d, 12, 0, 0) }.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("التاريخ الجديد: ${ArabicReportExporter.formatDate(dueMillis)}")
                    }
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("قيمة القسط") })
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("سبب التعديل") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = MoneyMath.parseMajorToMinor(amountText) ?: inst.amountMinor
                    onReschedule(inst.id, dueMillis, amt, reason)
                    editingInst = null
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editingInst = null }) { Text("إلغاء") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    payment: PaymentEntity,
    debt: DebtEntity?,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val methodLabel = runCatching { PaymentMethod.valueOf(payment.method).labelAr }.getOrDefault(payment.method)

    fun buildReceiptPdf() = ArabicReportExporter.generateArabicPdfReport(
        context = context,
        fileNamePrefix = "receipt_${payment.receiptNumber}",
        reportTitle = "إيصال سداد رقم ${payment.receiptNumber}",
        subtitle = "الشخص: ${debt?.contactName ?: "عميل"}",
        summaryLines = listOf(
            "رقم العملية" to payment.receiptNumber,
            "المبلغ المدفوع" to MoneyMath.formatMinor(payment.amountMinor, currencyCode = payment.currencyCode),
            "الخصم" to MoneyMath.formatMinor(payment.discountMinor, currencyCode = payment.currencyCode),
            "الرصيد قبل الدفع" to MoneyMath.formatMinor(payment.balanceBeforeMinor, currencyCode = payment.currencyCode),
            "الرصيد بعد الدفع" to MoneyMath.formatMinor(payment.balanceAfterMinor, currencyCode = payment.currencyCode),
            "طريقة الدفع" to methodLabel,
            "التاريخ" to ArabicReportExporter.formatDate(payment.paidAtMillis)
        ),
        rows = listOf(
            StatementPdfLine(
                dateLabel = ArabicReportExporter.formatDate(payment.paidAtMillis),
                title = debt?.reason ?: "سداد دين",
                amountLabel = MoneyMath.formatMinor(payment.amountMinor, currencyCode = payment.currencyCode),
                statusOrMethod = methodLabel
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إيصال رقم ${payment.receiptNumber}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FintechSurfaceCard {
                Text("إيصال دفع معتمد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                Spacer(Modifier.height(10.dp))
                ReceiptRow("رقم العملية", payment.receiptNumber)
                ReceiptRow("الطرف", debt?.contactName ?: "-")
                ReceiptRow("المبلغ المدفوع", MoneyMath.formatMinor(payment.amountMinor, currencyCode = payment.currencyCode))
                ReceiptRow("الخصم", MoneyMath.formatMinor(payment.discountMinor, currencyCode = payment.currencyCode))
                ReceiptRow("الرصيد قبل الدفع", MoneyMath.formatMinor(payment.balanceBeforeMinor, currencyCode = payment.currencyCode))
                ReceiptRow("الرصيد بعد الدفع", MoneyMath.formatMinor(payment.balanceAfterMinor, currencyCode = payment.currencyCode))
                ReceiptRow("طريقة الدفع", methodLabel)
                ReceiptRow("التاريخ", ArabicReportExporter.formatDate(payment.paidAtMillis))
                if (!payment.notes.isNullOrBlank()) {
                    ReceiptRow("ملاحظات", payment.notes)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val pdf = buildReceiptPdf()
                        ArabicReportExporter.shareFile(context, pdf, "application/pdf", "مشاركة الإيصال")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("مشاركة PDF")
                }
                OutlinedButton(
                    onClick = {
                        val pdf = buildReceiptPdf()
                        ArabicReportExporter.printPdfFile(context, pdf, "إيصال ${payment.receiptNumber}")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Print, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("طباعة")
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
