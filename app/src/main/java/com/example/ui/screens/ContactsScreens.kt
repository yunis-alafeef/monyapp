package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ContactEntity
import com.example.data.local.UserAppPreferences
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.export.ArabicReportExporter
import com.example.messaging.ExternalMessagingHelper
import com.example.notifications.NotificationHelper
import com.example.ui.components.DebtListCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.components.OperationNotificationVoucherDialog
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.HisabatiUiState
import java.util.UUID

@Composable
fun ContactsScreen(
    state: HisabatiUiState,
    onAddContact: () -> Unit,
    onContactClick: (String) -> Unit
) {
    val hc = HisabatiTheme.colors
    var search by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) } // 0 All, 1 Receivable, 2 Payable, 3 Overdue
    var sortByBalance by remember { mutableStateOf(true) }

    val now = System.currentTimeMillis()
    val contactsWithNet = remember(state.contacts, state.debts, search, filterMode, sortByBalance) {
        state.contacts.map { c ->
            val cDebts = state.debts.filter { it.contactId == c.id }
            val rec = cDebts.filter { it.direction == DebtDirection.RECEIVABLE.name }.sumOf { it.remainingMinor }
            val pay = cDebts.filter { it.direction == DebtDirection.PAYABLE.name }.sumOf { it.remainingMinor }
            val hasOverdue = cDebts.any {
                it.remainingMinor > 0L && it.dueAtMillis < now &&
                    it.status != DebtStatus.PAID.name && it.status != DebtStatus.SETTLED.name
            }
            Triple(c, rec - pay, Triple(rec, pay, hasOverdue))
        }.filter { (c, _, meta) ->
            val matchesQuery = search.isBlank() ||
                c.fullName.contains(search, ignoreCase = true) ||
                c.phone.contains(search)
            val matchesFilter = when (filterMode) {
                1 -> meta.first > 0L
                2 -> meta.second > 0L
                3 -> meta.third
                else -> true
            }
            matchesQuery && matchesFilter
        }.let { list ->
            if (sortByBalance) list.sortedByDescending { kotlin.math.abs(it.second) }
            else list.sortedBy { it.first.fullName }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContact,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Rounded.PersonAdd, contentDescription = "إضافة شخص")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("الأشخاص والعملاء", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("ابحث بالاسم أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_search_input")
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FilterChip(selected = filterMode == 0, onClick = { filterMode = 0 }, label = { Text("الكل") }) }
                item { FilterChip(selected = filterMode == 1, onClick = { filterMode = 1 }, label = { Text("مستحق لي") }) }
                item { FilterChip(selected = filterMode == 2, onClick = { filterMode = 2 }, label = { Text("مستحق عليّ") }) }
                item { FilterChip(selected = filterMode == 3, onClick = { filterMode = 3 }, label = { Text("متأخرون") }) }
                item {
                    FilterChip(
                        selected = sortByBalance,
                        onClick = { sortByBalance = !sortByBalance },
                        label = { Text(if (sortByBalance) "ترتيب: الأعلى رصيداً" else "ترتيب: أبجدي") }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (contactsWithNet.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Rounded.PersonAdd,
                    title = "لا يوجد أشخاص مطابقون",
                    subtitle = "أضف عميلاً أو مورداً أو صديقاً لتسجيل معاملاتك معه.",
                    actionLabel = "إضافة شخص جديد",
                    onAction = onAddContact
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(contactsWithNet, key = { it.first.id }) { (contact, net, meta) ->
                        FintechSurfaceCard(
                            onClick = { onContactClick(contact.id) },
                            contentPadding = PaddingValues(11.dp),
                            modifier = Modifier.testTag("contact_card_${contact.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (!contact.avatarUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = contact.avatarUri,
                                            contentDescription = contact.fullName,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.fullName.take(1),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(contact.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            contact.phone.ifBlank { "بدون رقم" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MoneyMath.formatSignedMinor(net, state.metrics.defaultCurrencySymbol),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net >= 0) hc.positive else hc.negative
                                    )
                                    if (meta.third) {
                                        Text("متأخر", style = MaterialTheme.typography.labelSmall, color = hc.negative)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    existingContact: ContactEntity?,
    preferences: UserAppPreferences,
    onBack: () -> Unit,
    onSave: (
        fullName: String,
        phone: String,
        address: String?,
        notes: String?,
        avatarUri: String?,
        idempotencyKey: String,
        onError: (String) -> Unit,
        onSuccess: (ContactEntity) -> Unit
    ) -> Unit
) {
    BackHandler(onBack = { onBack() })
    val context = LocalContext.current
    val hc = HisabatiTheme.colors

    var fullName by remember(existingContact) { mutableStateOf(existingContact?.fullName ?: "") }
    var phone by remember(existingContact) { mutableStateOf(existingContact?.phone ?: "") }
    var address by remember(existingContact) { mutableStateOf(existingContact?.address ?: "") }
    var notes by remember(existingContact) { mutableStateOf(existingContact?.notes ?: "") }
    var avatarUri by remember(existingContact) { mutableStateOf(existingContact?.avatarUri) }

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val formSubmissionKey = remember(existingContact?.id) { UUID.randomUUID().toString() }
    var savedContactVoucher by remember { mutableStateOf<ContactEntity?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) avatarUri = uri.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingContact == null) "إضافة شخص جديد" else "تعديل بيانات الشخص") },
                navigationIcon = {
                    IconButton(onClick = { if (!isSaving) onBack() }) {
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "صورة الشخص",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isSaving
                ) {
                    Text("اختيار صورة شخصية (اختياري)")
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { if (!isSaving) fullName = it },
                label = { Text("الاسم الكامل *") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_name_input")
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { if (!isSaving) phone = it },
                label = { Text("رقم الهاتف (مثال: 967772317950)") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_phone_input")
            )
            OutlinedTextField(
                value = address,
                onValueChange = { if (!isSaving) address = it },
                label = { Text("العنوان") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { if (!isSaving) notes = it },
                label = { Text("وصف أو ملاحظات") },
                minLines = 3,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            if (errorMsg != null) {
                Text(errorMsg!!, color = hc.negative, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    if (isSaving) return@Button
                    errorMsg = null
                    isSaving = true
                    onSave(
                        fullName,
                        phone,
                        address,
                        notes,
                        avatarUri,
                        formSubmissionKey,
                        { msg ->
                            isSaving = false
                            errorMsg = msg
                        },
                        { saved ->
                            isSaving = false
                            NotificationHelper.showReminderNotification(
                                context = context,
                                notificationId = saved.id.hashCode(),
                                title = "تم حفظ حساب العميل — حساباتي",
                                body = "تم اعتماد حساب ${saved.fullName} بنجاح"
                            )
                            savedContactVoucher = saved
                        }
                    )
                },
                enabled = fullName.isNotBlank() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_contact_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("جاري الحفظ...", fontWeight = FontWeight.Bold)
                } else {
                    Text("حفظ الشخص وإصدار إشعار", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    savedContactVoucher?.let { contact ->
        val voucherNo = "CNT-${contact.id.takeLast(6).uppercase()}"
        val zeroFmt = MoneyMath.formatMinor(0L, currencyCode = preferences.defaultCurrency)
        val whatsappMsg = ExternalMessagingHelper.buildFormattedWhatsAppMessage(
            prefs = preferences,
            title = if (existingContact == null) "إشعار فتح حساب جديد" else "إشعار تحديث بيانات الحساب",
            contactName = contact.fullName,
            directionLabel = "إدارة حساب العميل / المورد",
            principalFormatted = zeroFmt,
            paidFormatted = zeroFmt,
            remainingFormatted = zeroFmt,
            recordedDateLabel = ArabicReportExporter.formatDate(contact.updatedAtMillis),
            reason = if (existingContact == null) "فتح سجل مالي جديد لدى ${preferences.workspaceName}" else "تحديث بيانات التواصل",
            notes = contact.notes.orEmpty(),
            receiptNumber = voucherNo
        )

        OperationNotificationVoucherDialog(
            title = if (existingContact == null) "تم حفظ الشخص بنجاح" else "تم تحديث البيانات بنجاح",
            voucherNumber = voucherNo,
            partyName = contact.fullName,
            partyPhone = contact.phone,
            directionLabel = if (existingContact == null) "تسجيل شخص جديد" else "تحديث سجل",
            amountFormatted = contact.phone.ifBlank { "مسجل" },
            dateLabel = ArabicReportExporter.formatDate(contact.updatedAtMillis),
            reason = contact.address ?: contact.notes,
            whatsappMessage = whatsappMsg,
            onDone = {
                savedContactVoucher = null
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: ContactEntity,
    state: HisabatiUiState,
    onBack: () -> Unit,
    onEditContact: () -> Unit,
    onAddDebtForContact: () -> Unit,
    onDebtClick: (String) -> Unit,
    onOpenStatement: () -> Unit,
    onPaymentClick: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val hc = HisabatiTheme.colors
    val cDebts = state.debts.filter { it.contactId == contact.id }
    val cPayments = state.payments.filter { it.contactId == contact.id }
    val totalRec = cDebts.filter { it.direction == DebtDirection.RECEIVABLE.name }.sumOf { it.remainingMinor }
    val totalPay = cDebts.filter { it.direction == DebtDirection.PAYABLE.name }.sumOf { it.remainingMinor }
    val net = totalRec - totalPay

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.fullName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onEditContact) {
                        Icon(Icons.Rounded.Edit, contentDescription = "تعديل الشخص")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FintechSurfaceCard {
                    Text(contact.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "الهاتف: ${contact.phone.ifBlank { "غير مسجل" }} • أضيف: ${ArabicReportExporter.formatDate(contact.createdAtMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!contact.address.isNullOrBlank()) {
                        Text("العنوان: ${contact.address}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!contact.notes.isNullOrBlank()) {
                        Text("ملاحظات: ${contact.notes}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("مستحق لي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyMath.formatMinor(totalRec, state.metrics.defaultCurrencySymbol), color = hc.positive, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("مستحق عليّ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyMath.formatMinor(totalPay, state.metrics.defaultCurrencySymbol), color = hc.negative, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("صافي الرصيد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                MoneyMath.formatSignedMinor(net, state.metrics.defaultCurrencySymbol),
                                color = if (net >= 0) hc.positive else hc.negative,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { ExternalMessagingHelper.dialPhone(context, contact.phone) },
                            enabled = contact.phone.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("اتصال")
                        }
                        OutlinedButton(
                            onClick = {
                                val sym = state.metrics.defaultCurrencySymbol
                                val msg = ExternalMessagingHelper.buildFormattedWhatsAppMessage(
                                    prefs = state.preferences,
                                    title = "كشف حساب مختصر",
                                    contactName = contact.fullName,
                                    directionLabel = if (net >= 0) "رصيد مستحق لكم/علينا لصالحنا" else "رصيد مستحق لصالحكم",
                                    principalFormatted = MoneyMath.formatMinor(totalRec + totalPay, sym),
                                    paidFormatted = MoneyMath.formatMinor(cPayments.sumOf { it.amountMinor }, sym),
                                    remainingFormatted = MoneyMath.formatSignedMinor(net, sym),
                                    recordedDateLabel = ArabicReportExporter.formatDate(System.currentTimeMillis()),
                                    reason = "إجمالي المعاملات المسجلة: ${cDebts.size} قيد"
                                )
                                ExternalMessagingHelper.openWhatsAppChat(context, contact.phone, msg)
                            },
                            enabled = contact.phone.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("واتساب")
                        }
                        OutlinedButton(
                            onClick = onOpenStatement,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("كشف PDF")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddDebtForContact,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("إضافة دين جديد لهذا الشخص")
                    }
                }
            }

            item {
                Text("سجل الديون (${cDebts.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(cDebts, key = { it.id }) { debt ->
                DebtListCard(debt = debt, onClick = { onDebtClick(debt.id) })
            }

            item {
                Text("سجل المدفوعات (${cPayments.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(cPayments, key = { it.id }) { p ->
                FintechSurfaceCard(onClick = { onPaymentClick(p.id) }) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("إيصال #${p.receiptNumber}", fontWeight = FontWeight.Bold)
                            Text(ArabicReportExporter.formatDate(p.paidAtMillis), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode),
                            color = hc.positive,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
