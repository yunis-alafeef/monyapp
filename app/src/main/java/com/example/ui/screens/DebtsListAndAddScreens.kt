package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.export.ArabicReportExporter
import com.example.ui.components.DebtListCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PayableCoral
import com.example.ui.viewmodel.HisabatiUiState
import java.util.Calendar

@Composable
fun DebtsHubScreen(
    state: HisabatiUiState,
    initialTab: Int = 0, // 0 Receivables (مستحقة لي), 1 Payables (مستحقة عليّ), 2 Overdue (المتأخرة)
    onAddDebt: (DebtDirection) -> Unit,
    onDebtClick: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var statusFilter by remember { mutableStateOf<String?>(null) }
    var currencyFilter by remember { mutableStateOf<String?>(null) }
    val now = System.currentTimeMillis()

    val filteredDebts = remember(state.debts, selectedTab, statusFilter, currencyFilter, now) {
        state.debts.filter { debt ->
            val matchesTab = when (selectedTab) {
                0 -> debt.direction == DebtDirection.RECEIVABLE.name
                1 -> debt.direction == DebtDirection.PAYABLE.name
                else -> debt.remainingMinor > 0L &&
                    debt.dueAtMillis < now &&
                    debt.status != DebtStatus.PAID.name &&
                    debt.status != DebtStatus.SETTLED.name
            }
            val matchesStatus = statusFilter == null || debt.status == statusFilter
            val matchesCurrency = currencyFilter == null || debt.currencyCode == currencyFilter
            matchesTab && matchesStatus && matchesCurrency
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onAddDebt(if (selectedTab == 1) DebtDirection.PAYABLE else DebtDirection.RECEIVABLE)
                },
                containerColor = EmeraldPrimary,
                modifier = Modifier.testTag("debts_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "إضافة دين")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "إدارة الديون والمستحقات",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("مستحقة لي") },
                    modifier = Modifier.testTag("tab_receivables")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("مستحقة عليّ") },
                    modifier = Modifier.testTag("tab_payables")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("المتأخرة") },
                    modifier = Modifier.testTag("tab_overdue")
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("كل الحالات") }
                    )
                }
                items(DebtStatus.entries) { st ->
                    FilterChip(
                        selected = statusFilter == st.name,
                        onClick = { statusFilter = if (statusFilter == st.name) null else st.name },
                        label = { Text(st.labelAr) }
                    )
                }
            }

            if (state.currencies.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = currencyFilter == null,
                            onClick = { currencyFilter = null },
                            label = { Text("كل العملات") }
                        )
                    }
                    items(state.currencies, key = { it.id }) { cur ->
                        FilterChip(
                            selected = currencyFilter == cur.code,
                            onClick = { currencyFilter = if (currencyFilter == cur.code) null else cur.code },
                            label = { Text(cur.code) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (filteredDebts.isEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "لا توجد ديون في هذا القسم",
                        subtitle = "يمكنك تسجيل دين جديد أو تغيير الفلاتر الحالية.",
                        actionLabel = "تسجيل دين جديد",
                        onAction = { onAddDebt(if (selectedTab == 1) DebtDirection.PAYABLE else DebtDirection.RECEIVABLE) }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDebts, key = { it.id }) { debt ->
                        DebtListCard(debt = debt, onClick = { onDebtClick(debt.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtScreen(
    state: HisabatiUiState,
    preselectedContactId: String?,
    initialDirection: DebtDirection,
    onBack: () -> Unit,
    onQuickAddContact: () -> Unit,
    onSaveDebt: (
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
        installmentCount: Int
    ) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var selectedContactId by remember(preselectedContactId, state.contacts) {
        mutableStateOf(preselectedContactId ?: state.contacts.firstOrNull()?.id ?: "")
    }
    var contactMenuExpanded by remember { mutableStateOf(false) }
    var direction by remember(initialDirection) { mutableStateOf(initialDirection) }
    var amountInput by remember { mutableStateOf("") }
    var currencyCode by remember(state.preferences.defaultCurrency) { mutableStateOf(state.preferences.defaultCurrency) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var recordedAtMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var dueAtMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 30L * 86_400_000L) }
    var reason by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH.labelAr) }
    var attachmentUri by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var installmentCountText by remember { mutableStateOf("0") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) attachmentUri = uri.toString()
    }

    fun pickDate(currentMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val chosen = Calendar.getInstance().apply {
                    set(y, m, d, 12, 0, 0)
                }
                onPicked(chosen.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val selectedContact = state.contacts.find { it.id == selectedContactId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إضافة دين أو مستحق جديد") },
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
            // Direction selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = direction == DebtDirection.RECEIVABLE,
                    onClick = { direction = DebtDirection.RECEIVABLE },
                    label = { Text("مستحق لي (أطلبهم)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = direction == DebtDirection.PAYABLE,
                    onClick = { direction = DebtDirection.PAYABLE },
                    label = { Text("مستحق عليّ (أدين لهم)") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Contact selector
            if (state.contacts.isEmpty()) {
                OutlinedButton(
                    onClick = onQuickAddContact,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("أضف شخصاً أولاً لتسجيل الدين")
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = contactMenuExpanded,
                    onExpandedChange = { contactMenuExpanded = !contactMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedContact?.fullName ?: "اختر الشخص",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الشخص / العميل *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contactMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = contactMenuExpanded,
                        onDismissRequest = { contactMenuExpanded = false }
                    ) {
                        state.contacts.forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.fullName} (${c.phone})") },
                                onClick = {
                                    selectedContactId = c.id
                                    contactMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Amount + Currency
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("المبلغ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("debt_amount_input")
                )
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = currencyCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العملة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        state.currencies.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text("${cur.code} - ${cur.nameAr}") },
                                onClick = {
                                    currencyCode = cur.code
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Dates
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { pickDate(recordedAtMillis) { recordedAtMillis = it } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("التسجيل: ${ArabicReportExporter.formatDate(recordedAtMillis)}")
                }
                OutlinedButton(
                    onClick = { pickDate(dueAtMillis) { dueAtMillis = it } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("الاستحقاق: ${ArabicReportExporter.formatDate(dueAtMillis)}")
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("وصف سبب الدين *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_reason_input")
            )

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("طريقة السداد المتفق عليها") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = installmentCountText,
                onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) installmentCountText = it },
                label = { Text("عدد الأقساط (0 بدون تقسيط، 2-36 لتوليد أقساط)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = {
                    attachmentPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (attachmentUri == null) "إرفاق صورة فاتورة أو مستند (اختياري)" else "تم إرفاق مستند ✓")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (validationError != null) {
                Text(validationError!!, color = PayableCoral, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val minor = MoneyMath.parseMajorToMinor(amountInput)
                    if (selectedContactId.isBlank()) {
                        validationError = "يرجى اختيار الشخص"
                        return@Button
                    }
                    if (minor == null || minor <= 0L) {
                        validationError = "يرجى إدخال مبلغ صحيح أكبر من صفر"
                        return@Button
                    }
                    if (reason.isBlank()) {
                        validationError = "يرجى كتابة وصف سبب الدين"
                        return@Button
                    }
                    val instCount = installmentCountText.toIntOrNull()?.coerceIn(0, 36) ?: 0
                    validationError = null
                    onSaveDebt(
                        selectedContactId,
                        direction,
                        minor,
                        currencyCode,
                        recordedAtMillis,
                        dueAtMillis,
                        reason,
                        paymentMethod,
                        attachmentUri,
                        notes,
                        instCount
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_debt_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("حفظ الدين", fontWeight = FontWeight.Bold)
            }
        }
    }
}
