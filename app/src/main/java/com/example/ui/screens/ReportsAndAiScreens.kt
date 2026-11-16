package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.export.ArabicReportExporter
import com.example.export.PdfDocumentKind
import com.example.export.StatementPdfLine
import com.example.ui.components.DebtListCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.AiChatMessage
import com.example.ui.viewmodel.HisabatiUiState

private enum class ReportKind(val labelAr: String) {
    RECEIVABLES("تقرير المستحق لي"),
    PAYABLES("تقرير المستحق عليّ"),
    OVERDUE("الديون المتأخرة"),
    PAYMENTS("تقرير المدفوعات"),
    MONTHLY("الحركة الشهرية"),
    POSITION("المركز المالي الشامل")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onOpenStatement: (String) -> Unit,
    onOpenPrintSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val hc = HisabatiTheme.colors
    var selectedKind by remember { mutableStateOf(ReportKind.POSITION) }
    var lastExportMessage by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val filteredDebts = remember(state.debts, selectedKind) {
        when (selectedKind) {
            ReportKind.RECEIVABLES -> state.debts.filter { it.direction == DebtDirection.RECEIVABLE.name }
            ReportKind.PAYABLES -> state.debts.filter { it.direction == DebtDirection.PAYABLE.name }
            ReportKind.OVERDUE -> state.debts.filter {
                it.remainingMinor > 0L && it.dueAtMillis < now &&
                    it.status != DebtStatus.PAID.name && it.status != DebtStatus.SETTLED.name
            }
            else -> state.debts
        }
    }

    val sym = state.metrics.defaultCurrencySymbol
    val canViewReports = state.activeMember?.canViewReports ?: true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("التقارير المالية والتصدير") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onOpenPrintSettings,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تخصيص القالب", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        if (!canViewReports) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Rounded.Description,
                    title = "صلاحية غير متاحة",
                    subtitle = "دورك الحالي لا يملك صلاحية الاطلاع على التقارير المالية أو تصديرها."
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportKind.entries.forEach { kind ->
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = { selectedKind = kind },
                            label = { Text(kind.labelAr) }
                        )
                    }
                }
            }

            item {
                FintechSurfaceCard {
                    Text(selectedKind.labelAr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("إجمالي المستحق لي: ${MoneyMath.formatMinor(state.metrics.totalReceivableMinor, sym)}", style = MaterialTheme.typography.bodyMedium, color = hc.positive)
                    Text("إجمالي المستحق عليّ: ${MoneyMath.formatMinor(state.metrics.totalPayableMinor, sym)}", style = MaterialTheme.typography.bodyMedium, color = hc.negative)
                    Text("صافي المركز المالي: ${MoneyMath.formatSignedMinor(state.metrics.netPositionMinor, sym)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("عدد البنود المطابقة: ${if (selectedKind == ReportKind.PAYMENTS) state.payments.size else filteredDebts.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val rows = buildReportPdfRows(selectedKind, state, filteredDebts)
                                val file = ArabicReportExporter.generateArabicPdfReport(
                                    context = context,
                                    fileNamePrefix = "report_${selectedKind.name.lowercase()}",
                                    reportTitle = selectedKind.labelAr,
                                    subtitle = state.preferences.workspaceName,
                                    summaryLines = listOf(
                                        "إجمالي المستحق لي" to MoneyMath.formatMinor(state.metrics.totalReceivableMinor, sym),
                                        "إجمالي المستحق عليّ" to MoneyMath.formatMinor(state.metrics.totalPayableMinor, sym),
                                        "صافي المركز المالي" to MoneyMath.formatSignedMinor(state.metrics.netPositionMinor, sym)
                                    ),
                                    rows = rows,
                                    prefs = state.preferences,
                                    docKind = PdfDocumentKind.GENERAL_REPORT
                                )
                                ArabicReportExporter.shareFile(context, file, "application/pdf", "مشاركة التقرير المالي PDF")
                                lastExportMessage = "تم إنشاء ومشاركة ملف PDF بنجاح"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_report_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تصدير PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val rows = buildReportPdfRows(selectedKind, state, filteredDebts)
                                val file = ArabicReportExporter.generateArabicPdfReport(
                                    context = context,
                                    fileNamePrefix = "print_${selectedKind.name.lowercase()}",
                                    reportTitle = selectedKind.labelAr,
                                    subtitle = state.preferences.workspaceName,
                                    summaryLines = listOf(
                                        "صافي المركز المالي" to MoneyMath.formatSignedMinor(state.metrics.netPositionMinor, sym)
                                    ),
                                    rows = rows,
                                    prefs = state.preferences,
                                    docKind = PdfDocumentKind.GENERAL_REPORT
                                )
                                ArabicReportExporter.printPdfFile(context, file, selectedKind.labelAr)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("طباعة")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val csvFile = ArabicReportExporter.generateCsvReport(
                                context = context,
                                fileNamePrefix = "hisabati_excel",
                                debts = filteredDebts
                            )
                            ArabicReportExporter.shareFile(context, csvFile, "text/csv", "مشاركة ملف Excel / CSV")
                            lastExportMessage = "تم تصدير ملف CSV المتوافق مع Excel بالعربية"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_report_button")
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تصدير Excel (CSV UTF-8)")
                    }
                    if (lastExportMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(lastExportMessage!!, style = MaterialTheme.typography.bodySmall, color = hc.positive)
                    }
                }
            }

            if (state.contacts.isNotEmpty()) {
                item {
                    FintechSurfaceCard(
                        onClick = { onOpenStatement(state.contacts.first().id) }
                    ) {
                        Text("استخراج كشف حساب تفصيلي لشخص محدد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("اضغط هنا لاختيار شخص وإصدار كشف حساب PDF رسمي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (selectedKind == ReportKind.PAYMENTS) {
                items(state.payments, key = { it.id }) { p ->
                    val debt = state.debts.find { it.id == p.debtId }
                    FintechSurfaceCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(debt?.contactName ?: "دفعة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("إيصال ${p.receiptNumber} • ${ArabicReportExporter.formatDate(p.paidAtMillis)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode), color = hc.positive, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filteredDebts, key = { it.id }) { debt ->
                    DebtListCard(debt = debt, onClick = { onOpenStatement(debt.contactId) })
                }
            }
        }
    }
}

private fun buildReportPdfRows(
    kind: ReportKind,
    state: HisabatiUiState,
    filteredDebts: List<com.example.data.local.DebtEntity>
): List<StatementPdfLine> {
    return when (kind) {
        ReportKind.PAYMENTS -> state.payments.map { p ->
            val name = state.debts.find { it.id == p.debtId }?.contactName ?: "عميل"
            val method = runCatching { PaymentMethod.valueOf(p.method).labelAr }.getOrDefault(p.method)
            StatementPdfLine(
                dateLabel = ArabicReportExporter.formatDate(p.paidAtMillis),
                title = "$name — إيصال #${p.receiptNumber}",
                amountLabel = MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode),
                statusOrMethod = method
            )
        }
        ReportKind.MONTHLY -> state.metrics.monthlySeries.map { pt ->
            StatementPdfLine(
                dateLabel = pt.monthLabel,
                title = "ملخص الشهر",
                amountLabel = "له: ${MoneyMath.formatMinor(pt.receivablesMinor, state.metrics.defaultCurrencySymbol)} | عليه: ${MoneyMath.formatMinor(pt.payablesMinor, state.metrics.defaultCurrencySymbol)}",
                statusOrMethod = "تحصيل ${MoneyMath.formatMinor(pt.collectedMinor, state.metrics.defaultCurrencySymbol)}"
            )
        }
        else -> filteredDebts.map { d ->
            val st = runCatching { DebtStatus.valueOf(d.status).labelAr }.getOrDefault(d.status)
            StatementPdfLine(
                dateLabel = ArabicReportExporter.formatDate(d.dueAtMillis),
                title = "${d.contactName}: ${d.reason}",
                amountLabel = MoneyMath.formatMinor(d.remainingMinor, currencyCode = d.currencyCode),
                statusOrMethod = st
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementScreen(
    initialContactId: String,
    state: HisabatiUiState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedContactId by remember(initialContactId, state.contacts) {
        mutableStateOf(
            if (state.contacts.any { it.id == initialContactId }) initialContactId
            else state.contacts.firstOrNull()?.id ?: ""
        )
    }
    var rangeDays by remember { mutableIntStateOf(0) } // 0=الكل, 30, 90

    val contact = state.contacts.find { it.id == selectedContactId }
    val cutoff = if (rangeDays > 0) System.currentTimeMillis() - rangeDays * 86_400_000L else 0L
    val contactDebts = state.debts.filter { it.contactId == selectedContactId && it.recordedAtMillis >= cutoff }
    val contactPayments = state.payments.filter { it.contactId == selectedContactId && it.paidAtMillis >= cutoff }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("كشف الحساب التفصيلي") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
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
                Text("اختر الشخص", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.contacts.forEach { c ->
                        FilterChip(
                            selected = c.id == selectedContactId,
                            onClick = { selectedContactId = c.id },
                            label = { Text(c.fullName) }
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = rangeDays == 0, onClick = { rangeDays = 0 }, label = { Text("كل الفترة") })
                    FilterChip(selected = rangeDays == 30, onClick = { rangeDays = 30 }, label = { Text("آخر 30 يوماً") })
                    FilterChip(selected = rangeDays == 90, onClick = { rangeDays = 90 }, label = { Text("آخر 90 يوماً") })
                }
            }

            if (contact == null) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Description,
                        title = "لا يوجد أشخاص",
                        subtitle = "أضف شخصاً أولاً لإصدار كشف حساب تفصيلي."
                    )
                }
            } else {
                item {
                    FintechSurfaceCard {
                        Text(contact.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        val grouped = contactDebts.groupBy { it.currencyCode }
                        grouped.forEach { (code, list) ->
                            val rec = list.filter { it.direction == DebtDirection.RECEIVABLE.name }.sumOf { it.remainingMinor }
                            val pay = list.filter { it.direction == DebtDirection.PAYABLE.name }.sumOf { it.remainingMinor }
                            Text(
                                "[$code] مستحق لي: ${MoneyMath.formatMinor(rec, currencyCode = code)} | مستحق عليّ: ${MoneyMath.formatMinor(pay, currencyCode = code)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val pdf = ArabicReportExporter.generateContactStatementPdf(context, contact, contactDebts, contactPayments, state.preferences)
                                    ArabicReportExporter.shareFile(context, pdf, "application/pdf", "مشاركة كشف حساب ${contact.fullName}")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("statement_share_pdf_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("مشاركة PDF")
                            }
                            OutlinedButton(
                                onClick = {
                                    val pdf = ArabicReportExporter.generateContactStatementPdf(context, contact, contactDebts, contactPayments, state.preferences)
                                    ArabicReportExporter.printPdfFile(context, pdf, "كشف حساب ${contact.fullName}")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("طباعة")
                            }
                        }
                    }
                }

                items(contactDebts, key = { it.id }) { debt ->
                    DebtListCard(debt = debt, onClick = {})
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    state: HisabatiUiState,
    messages: List<AiChatMessage>,
    streamingText: String?,
    isBusy: Boolean,
    onBack: () -> Unit,
    onToggleConsent: (Boolean) -> Unit,
    onAskQuestion: (question: String, grantConsentAndSend: Boolean) -> Unit,
    onClearChat: () -> Unit = {}
) {
    val hc = HisabatiTheme.colors
    var input by remember { mutableStateOf("") }
    var pendingQuestion by remember { mutableStateOf<String?>(null) }
    val quickPrompts = listOf(
        "ما إجمالي المستحق لي؟",
        "ما إجمالي المستحق عليّ؟",
        "من أكثر شخص عليه ديون؟",
        "ما الديون المتأخرة؟",
        "كم تم تحصيله هذا الشهر؟",
        "ما الأقساط المستحقة هذا الأسبوع؟",
        "اعطني ملخصاً عن هذا الشهر"
    )

    fun submitQuestion(q: String) {
        val clean = q.trim()
        if (clean.isEmpty() || isBusy) return
        if (!state.preferences.aiConsentGranted) {
            pendingQuestion = clean
        } else {
            pendingQuestion = null
            onAskQuestion(clean, false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("المساعد المالي الذكي", fontWeight = FontWeight.Bold)
                        Text(
                            if (state.preferences.aiConsentGranted) "Gemini AI • مفعّل وجاهز" else "بانتظار التفعيل",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.preferences.aiConsentGranted) hc.positive else hc.warning
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (messages.isNotEmpty() && !isBusy) {
                        IconButton(
                            onClick = {
                                pendingQuestion = null
                                onClearChat()
                            },
                            modifier = Modifier.testTag("ai_clear_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "محادثة جديدة")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            FintechSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("تفعيل المساعد المالي الذكي (Gemini AI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "تحليل فوري لأرصدتك وأقساطك ومتأخراتك بالاعتماد على بيانات حسابك الفعلية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.preferences.aiConsentGranted,
                        onCheckedChange = { enabled ->
                            onToggleConsent(enabled)
                            if (enabled && !pendingQuestion.isNullOrBlank()) {
                                val queued = pendingQuestion!!
                                pendingQuestion = null
                                onAskQuestion(queued, true)
                            }
                        },
                        modifier = Modifier.testTag("ai_consent_switch")
                    )
                }
                if (!state.preferences.aiConsentGranted) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val queued = pendingQuestion
                            pendingQuestion = null
                            if (!queued.isNullOrBlank()) {
                                onAskQuestion(queued, true)
                            } else {
                                onToggleConsent(true)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_activate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = hc.positive)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (pendingQuestion != null) "موافق — تفعيل وإرسال سؤالك الآن" else "تفعيل المساعد الذكي الآن",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (pendingQuestion != null && !state.preferences.aiConsentGranted) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = hc.warning.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "السؤال الجاهز للإرسال: «$pendingQuestion» — اضغط الزر الأخضر أعلاه للتفعيل والإجابة فوراً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { prompt ->
                    FilterChip(
                        selected = false,
                        onClick = { submitQuestion(prompt) },
                        label = { Text(prompt) },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = hc.accent, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty() && streamingText == null) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "اسأل عن وضعك المالي",
                            subtitle = "اختر سؤالاً سريعاً أعلاه أو اكتب سؤالك عن الديون والتحصيل والأقساط."
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = when {
                                msg.isError -> hc.negative.copy(alpha = 0.14f)
                                msg.isUser -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surface
                            },
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Text(
                                text = msg.text,
                                color = when {
                                    msg.isError -> hc.negative
                                    msg.isUser -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                if (streamingText != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (streamingText.isBlank()) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("جاري تحليل سجلاتك المالية...")
                                } else {
                                    Text(streamingText, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("اكتب سؤالك المالي هنا...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_question_input"),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        val q = input
                        input = ""
                        submitQuestion(q)
                    },
                    enabled = !isBusy && input.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(hc.positive)
                        .testTag("ai_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "إرسال", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onOpenDebt: (String) -> Unit,
    onOpenContact: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("ALL") }
    var selectedDirection by remember { mutableStateOf<DebtDirection?>(null) }
    var selectedStatus by remember { mutableStateOf<DebtStatus?>(null) }

    val matchingContacts = remember(state.contacts, query) {
        if (query.isBlank()) emptyList()
        else state.contacts.filter {
            it.fullName.contains(query, ignoreCase = true) || it.phone.contains(query, ignoreCase = true)
        }
    }

    val matchingDebts = remember(state.debts, state.payments, query, selectedCurrency, selectedDirection, selectedStatus) {
        val matchingReceiptDebtIds = if (query.isBlank()) emptySet() else {
            state.payments.filter { it.receiptNumber.contains(query, ignoreCase = true) }.map { it.debtId }.toSet()
        }
        state.debts.filter { d ->
            val curOk = selectedCurrency == "ALL" || d.currencyCode == selectedCurrency
            val dirOk = selectedDirection == null || d.direction == selectedDirection?.name
            val stOk = selectedStatus == null || d.status == selectedStatus?.name
            val qOk = query.isBlank() ||
                d.contactName.contains(query, ignoreCase = true) ||
                d.reason.contains(query, ignoreCase = true) ||
                (d.notes?.contains(query, ignoreCase = true) == true) ||
                MoneyMath.formatMinor(d.remainingMinor, currencyCode = d.currencyCode).contains(query) ||
                ArabicReportExporter.formatDate(d.dueAtMillis).contains(query) ||
                d.id in matchingReceiptDebtIds
            curOk && dirOk && stOk && qOk
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("البحث الشامل والتصفية") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("ابحث بالاسم، الهاتف، المبلغ، التاريخ، رقم الإيصال، أو السبب") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input"),
                    singleLine = true
                )
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = selectedDirection == null, onClick = { selectedDirection = null }, label = { Text("كل الاتجاهات") })
                    DebtDirection.entries.forEach { dir ->
                        FilterChip(
                            selected = selectedDirection == dir,
                            onClick = { selectedDirection = if (selectedDirection == dir) null else dir },
                            label = { Text(dir.labelAr) }
                        )
                    }
                    FilterChip(selected = selectedCurrency == "ALL", onClick = { selectedCurrency = "ALL" }, label = { Text("كل العملات") })
                    state.currencies.forEach { cur ->
                        FilterChip(
                            selected = selectedCurrency == cur.code,
                            onClick = { selectedCurrency = cur.code },
                            label = { Text(cur.code) }
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = selectedStatus == null, onClick = { selectedStatus = null }, label = { Text("كل الحالات") })
                    DebtStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = if (selectedStatus == st) null else st },
                            label = { Text(st.labelAr) }
                        )
                    }
                }
            }

            if (matchingContacts.isNotEmpty()) {
                item { Text("الأشخاص (${matchingContacts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(matchingContacts, key = { it.id }) { c ->
                    FintechSurfaceCard(onClick = { onOpenContact(c.id) }) {
                        Text(c.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(c.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Text("الديون والسجلات (${matchingDebts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (matchingDebts.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Search,
                        title = "لا توجد نتائج مطابقة",
                        subtitle = "جرّب تعديل كلمات البحث أو مسح الفلاتر."
                    )
                }
            } else {
                items(matchingDebts, key = { it.id }) { debt ->
                    DebtListCard(debt = debt, onClick = { onOpenDebt(debt.id) })
                }
            }
        }
    }
}
