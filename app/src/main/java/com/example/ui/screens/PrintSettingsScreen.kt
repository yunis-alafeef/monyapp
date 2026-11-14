package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.UserAppPreferences
import com.example.export.ArabicReportExporter
import com.example.export.PdfDocumentKind
import com.example.export.PdfInvoiceColorSwatches
import com.example.export.PdfInvoiceTemplate
import com.example.export.StatementPdfLine
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.HisabatiUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintSettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSavePrintSettings: (
        template: PdfInvoiceTemplate,
        companyName: String,
        companyPhone: String,
        companyAddress: String,
        taxOrCrNumber: String,
        footerNote: String,
        showLogoInHeader: Boolean,
        showInmaSoftInHeader: Boolean,
        showInmaSoftInFooter: Boolean,
        showSignatureBox: Boolean,
        showWatermarkBadge: Boolean,
        debtColorHex: Long,
        receiptColorHex: Long,
        statementColorHex: Long,
        reportColorHex: Long
    ) -> Unit,
    onLogoUpdated: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val hc = HisabatiTheme.colors
    val p = state.preferences

    var selectedTemplate by remember(p.printTemplate) {
        mutableStateOf(runCatching { PdfInvoiceTemplate.valueOf(p.printTemplate) }.getOrDefault(PdfInvoiceTemplate.CLASSIC_FORMAL))
    }
    var companyName by remember(p.printCompanyName, p.workspaceName) {
        mutableStateOf(p.printCompanyName.ifBlank { p.workspaceName })
    }
    var companyPhone by remember(p.printCompanyPhone) { mutableStateOf(p.printCompanyPhone) }
    var companyAddress by remember(p.printCompanyAddress) { mutableStateOf(p.printCompanyAddress) }
    var taxOrCrNumber by remember(p.printTaxOrCrNumber) { mutableStateOf(p.printTaxOrCrNumber) }
    var footerNote by remember(p.printFooterNote) { mutableStateOf(p.printFooterNote) }

    var showLogo by remember(p.printShowLogoInHeader) { mutableStateOf(p.printShowLogoInHeader) }
    var showInmaHeader by remember(p.printShowInmaSoftInHeader) { mutableStateOf(p.printShowInmaSoftInHeader) }
    var showInmaFooter by remember(p.printShowInmaSoftInFooter) { mutableStateOf(p.printShowInmaSoftInFooter) }
    var showSignature by remember(p.printShowSignatureBox) { mutableStateOf(p.printShowSignatureBox) }
    var showWatermark by remember(p.printShowWatermarkBadge) { mutableStateOf(p.printShowWatermarkBadge) }

    var debtColorHex by remember(p.printDebtColorHex) { mutableLongStateOf(p.printDebtColorHex) }
    var receiptColorHex by remember(p.printReceiptColorHex) { mutableLongStateOf(p.printReceiptColorHex) }
    var statementColorHex by remember(p.printStatementColorHex) { mutableLongStateOf(p.printStatementColorHex) }
    var reportColorHex by remember(p.printReportColorHex) { mutableLongStateOf(p.printReportColorHex) }

    var activeDocKind by remember { mutableStateOf(PdfDocumentKind.DEBT_VOUCHER) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ArabicReportExporter.saveLogoFromUri(context, uri)
            if (savedPath != null) {
                onLogoUpdated(savedPath)
            }
        }
    }

    val activeDocColorHex = when (activeDocKind) {
        PdfDocumentKind.DEBT_VOUCHER -> debtColorHex
        PdfDocumentKind.PAYMENT_RECEIPT -> receiptColorHex
        PdfDocumentKind.STATEMENT -> statementColorHex
        PdfDocumentKind.GENERAL_REPORT -> reportColorHex
    }

    fun updateActiveDocColor(newHex: Long) {
        when (activeDocKind) {
            PdfDocumentKind.DEBT_VOUCHER -> debtColorHex = newHex
            PdfDocumentKind.PAYMENT_RECEIPT -> receiptColorHex = newHex
            PdfDocumentKind.STATEMENT -> statementColorHex = newHex
            PdfDocumentKind.GENERAL_REPORT -> reportColorHex = newHex
        }
    }

    fun buildLivePreviewPrefs(): UserAppPreferences = p.copy(
        printTemplate = selectedTemplate.name,
        printCompanyName = companyName,
        printCompanyPhone = companyPhone,
        printCompanyAddress = companyAddress,
        printTaxOrCrNumber = taxOrCrNumber,
        printFooterNote = footerNote,
        printShowLogoInHeader = showLogo,
        printShowInmaSoftInHeader = showInmaHeader,
        printShowInmaSoftInFooter = showInmaFooter,
        printShowSignatureBox = showSignature,
        printShowWatermarkBadge = showWatermark,
        printDebtColorHex = debtColorHex,
        printReceiptColorHex = receiptColorHex,
        printStatementColorHex = statementColorHex,
        printReportColorHex = reportColorHex
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الطباعة والفواتير PDF") },
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
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1) Live Invoice Header & Footer Visual Preview
            item {
                LiveInvoicePreviewCard(
                    prefs = buildLivePreviewPrefs(),
                    docKind = activeDocKind
                )
            }

            // 2) Template Selector
            item {
                FintechSurfaceCard {
                    Text("قوالب الفواتير والسندات الاحترافية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "اختر التصميم المعتمد لجميع فواتيرك وسنداتك وكشوف الحساب",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PdfInvoiceTemplate.entries.forEach { tpl ->
                            val isSelected = selectedTemplate == tpl
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(activeDocColorHex) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTemplate = tpl }
                                    .testTag("print_template_${tpl.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tpl.titleAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            tpl.subtitleAr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(activeDocColorHex)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3) Per-Document Color Picker ("تغيير لون كل فاتورة")
            item {
                FintechSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = Color(activeDocColorHex), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ألوان الفواتير حسب نوع المستند", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "خصص لوناً مختلفاً لكل فاتورة أو سند، أو وحّد اللون لجميع المستندات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfDocumentKind.entries.forEach { kind ->
                            val kindColor = when (kind) {
                                PdfDocumentKind.DEBT_VOUCHER -> debtColorHex
                                PdfDocumentKind.PAYMENT_RECEIPT -> receiptColorHex
                                PdfDocumentKind.STATEMENT -> statementColorHex
                                PdfDocumentKind.GENERAL_REPORT -> reportColorHex
                            }
                            FilterChip(
                                selected = activeDocKind == kind,
                                onClick = { activeDocKind = kind },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(kindColor))
                                    )
                                },
                                label = { Text(kind.titleAr, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfInvoiceColorSwatches.forEach { swatch ->
                            val selected = activeDocColorHex == swatch.primaryHex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { updateActiveDocColor(swatch.primaryHex) }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(swatch.primaryHex), Color(swatch.accentHex))
                                            )
                                        )
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(Icons.Rounded.Check, contentDescription = swatch.nameAr, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(swatch.nameAr, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            debtColorHex = activeDocColorHex
                            receiptColorHex = activeDocColorHex
                            statementColorHex = activeDocColorHex
                            reportColorHex = activeDocColorHex
                        }
                    ) {
                        Text("تطبيق لون «${activeDocKind.titleAr}» على جميع الفواتير والتقارير")
                    }
                }
            }

            // 4) Logo Uploader (Zero-permission Photo Picker)
            item {
                FintechSurfaceCard {
                    Text("شعار المؤسسة (في منتصف الهيدر)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(2.dp, Color(activeDocColorHex), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val logoFile = p.printLogoPath.takeIf { it.isNotBlank() }?.let { File(it) }
                            if (logoFile != null && logoFile.exists()) {
                                AsyncImage(
                                    model = logoFile,
                                    contentDescription = "شعار المؤسسة",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    companyName.take(1).ifBlank { "ح" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(activeDocColorHex)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upload_print_logo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (p.printLogoPath.isBlank()) "رفع شعار المؤسسة" else "تغيير الشعار")
                            }
                            if (p.printLogoPath.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        ArabicReportExporter.clearSavedLogo(p.printLogoPath)
                                        onLogoUpdated("")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = hc.negative, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("إزالة الشعار", color = hc.negative)
                                }
                            }
                        }
                    }
                }
            }

            // 5) Organization Info & Footer Settings
            item {
                FintechSurfaceCard {
                    Text("معلومات المؤسسة وتخصيص الهيدر والفوتر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("اسم المؤسسة / النشاط التجاري (يظهر يمين الهيدر)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("print_company_name_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companyPhone,
                        onValueChange = { companyPhone = it },
                        label = { Text("رقم هاتف المؤسسة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companyAddress,
                        onValueChange = { companyAddress = it },
                        label = { Text("العنوان / المدينة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = taxOrCrNumber,
                        onValueChange = { taxOrCrNumber = it },
                        label = { Text("الرقم الضريبي أو السجل التجاري (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = footerNote,
                        onValueChange = { footerNote = it },
                        label = { Text("عبارة التذييل (الفوتر) أسفل الفواتير") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))
                    PrintSwitchRow("إظهار الشعار في منتصف الهيدر", showLogo) { showLogo = it }
                    PrintSwitchRow("إظهار بيانات انما سوفت (772317950) في الهيدر", showInmaHeader) { showInmaHeader = it }
                    PrintSwitchRow("إظهار توقيع انما سوفت في الفوتر", showInmaFooter) { showInmaFooter = it }
                    PrintSwitchRow("إظهار مربعات التوقيع وختم المؤسسة", showSignature) { showSignature = it }
                    PrintSwitchRow("إظهار العلامة المائية التوثيقية الخفيفة", showWatermark) { showWatermark = it }
                }
            }

            // 6) Save & Live Sample PDF Test Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSavePrintSettings(
                                selectedTemplate,
                                companyName,
                                companyPhone,
                                companyAddress,
                                taxOrCrNumber,
                                footerNote,
                                showLogo,
                                showInmaHeader,
                                showInmaFooter,
                                showSignature,
                                showWatermark,
                                debtColorHex,
                                receiptColorHex,
                                statementColorHex,
                                reportColorHex
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_print_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("حفظ إعدادات الطباعة والفواتير", fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val samplePdf = buildSamplePreviewPdf(context, buildLivePreviewPrefs(), activeDocKind)
                                ArabicReportExporter.shareFile(context, samplePdf, "application/pdf", "معاينة نموذج الفاتورة PDF")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("معاينة PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val samplePdf = buildSamplePreviewPdf(context, buildLivePreviewPrefs(), activeDocKind)
                                ArabicReportExporter.printPdfFile(context, samplePdf, "معاينة ${activeDocKind.titleAr}")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("طباعة تجريبية")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrintSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LiveInvoicePreviewCard(
    prefs: UserAppPreferences,
    docKind: PdfDocumentKind
) {
    val template = runCatching { PdfInvoiceTemplate.valueOf(prefs.printTemplate) }.getOrDefault(PdfInvoiceTemplate.CLASSIC_FORMAL)
    val swatch = PdfInvoiceColorSwatches.find {
        val target = when (docKind) {
            PdfDocumentKind.DEBT_VOUCHER -> prefs.printDebtColorHex
            PdfDocumentKind.PAYMENT_RECEIPT -> prefs.printReceiptColorHex
            PdfDocumentKind.STATEMENT -> prefs.printStatementColorHex
            PdfDocumentKind.GENERAL_REPORT -> prefs.printReportColorHex
        }
        it.primaryHex == target
    }
    val primaryColor = Color(swatch?.primaryHex ?: prefs.printDebtColorHex)
    val accentColor = Color(swatch?.accentHex ?: 0xFF10B981L)
    val companyName = prefs.printCompanyName.ifBlank { prefs.workspaceName }.ifBlank { "مؤسسة حساباتي التجارية" }

    val headerDark = template == PdfInvoiceTemplate.CLASSIC_FORMAL || template == PdfInvoiceTemplate.MODERN_LUXURY
    val headerBg = when (template) {
        PdfInvoiceTemplate.CLASSIC_FORMAL -> Brush.horizontalGradient(listOf(primaryColor, primaryColor))
        PdfInvoiceTemplate.MODERN_LUXURY -> Brush.horizontalGradient(listOf(primaryColor, accentColor.copy(alpha = 0.85f)))
        PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER -> Brush.horizontalGradient(listOf(primaryColor.copy(alpha = 0.08f), primaryColor.copy(alpha = 0.08f)))
        PdfInvoiceTemplate.MINIMAL_EXECUTIVE -> Brush.horizontalGradient(listOf(Color.White, Color.White))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (template == PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER) 2.dp else 1.dp,
            color = if (template == PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER) primaryColor else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Preview: Right = Organization, Center = Logo, Left = Inma Soft
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right (RTL start): Organization info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            companyName,
                            color = if (headerDark) Color.White else primaryColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (prefs.printCompanyPhone.isNotBlank()) {
                            Text(
                                prefs.printCompanyPhone,
                                color = if (headerDark) Color.White.copy(alpha = 0.85f) else Color(0xFF475569),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                        if (prefs.printCompanyAddress.isNotBlank()) {
                            Text(
                                prefs.printCompanyAddress,
                                color = if (headerDark) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Center: Logo
                    if (prefs.printShowLogoInHeader) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.5.dp, accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val logoFile = prefs.printLogoPath.takeIf { it.isNotBlank() }?.let { File(it) }
                            if (logoFile != null && logoFile.exists()) {
                                AsyncImage(
                                    model = logoFile,
                                    contentDescription = "شعار",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    companyName.take(1).ifBlank { "ح" },
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Left (RTL end): Inma Soft info & Doc kind
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        if (prefs.printShowInmaSoftInHeader) {
                            Text(
                                "انما سوفت | Inma Soft",
                                color = if (headerDark) accentColor else primaryColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                textAlign = TextAlign.End
                            )
                            Text(
                                "772317950",
                                color = if (headerDark) Color.White.copy(alpha = 0.85f) else Color(0xFF475569),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            docKind.titleAr,
                            color = if (headerDark) Color.White else primaryColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(accentColor)
            )

            // Mini body preview
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Surface(
                    color = primaryColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "معاينة حية: ${template.titleAr} • ${docKind.titleAr}",
                        color = primaryColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("التاريخ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("البيان", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("المبلغ", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("2026/11/14", color = Color(0xFF334155), fontSize = 9.sp)
                    Text("معاملة نموذجية", color = Color(0xFF0F172A), fontSize = 9.sp)
                    Text("150,000 ر.ي", color = primaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = accentColor, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        prefs.printFooterNote,
                        color = Color(0xFF64748B),
                        fontSize = 8.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (prefs.printShowInmaSoftInFooter) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "عمل انما سوفت • 772317950",
                            color = primaryColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun buildSamplePreviewPdf(
    context: android.content.Context,
    prefs: UserAppPreferences,
    docKind: PdfDocumentKind
): File {
    return ArabicReportExporter.generateArabicPdfReport(
        context = context,
        fileNamePrefix = "sample_${docKind.name.lowercase()}",
        reportTitle = "نموذج معاينة: ${docKind.titleAr}",
        subtitle = "الطرف: شركة الأفق للتجارة • رقم السند التجريبي #HSB-2026",
        summaryLines = listOf(
            "العميل / الطرف" to "شركة الأفق للتجارة",
            "رقم الهاتف" to "772317950",
            "المبلغ الإجمالي" to "250,000 ر.ي",
            "الرصيد المتبقي" to "100,000 ر.ي"
        ),
        rows = listOf(
            StatementPdfLine("2026/11/01", "توريد بضائع ومستلزمات تجارية", "250,000 ر.ي", "قيد معتمد"),
            StatementPdfLine("2026/11/10", "دفعة أولى نقداً — إيصال #HSB-102", "150,000 ر.ي", "نقدي")
        ),
        prefs = prefs,
        docKind = docKind
    )
}
