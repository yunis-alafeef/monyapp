package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.MessageTemplateEntity
import com.example.data.local.ReminderEntity
import com.example.domain.model.MessageChannel
import com.example.domain.model.ReminderPriority
import com.example.domain.model.RepeatType
import com.example.export.ArabicReportExporter
import com.example.messaging.ExternalMessagingHelper
import com.example.messaging.WhatsAppMessageStyle
import com.example.notifications.NotificationHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.HisabatiUiState
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenDebt: (String) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onCheckDueNow: () -> Unit = {}
) {
    val hc = HisabatiTheme.colors
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإشعارات والتنبيهات") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onCheckDueNow,
                        modifier = Modifier.testTag("check_due_notifications_button")
                    ) {
                        Text("فحص الآن")
                    }
                    IconButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.testTag("mark_all_notifications_read")
                    ) {
                        Icon(Icons.Rounded.DoneAll, contentDescription = "تعليم الكل كمقروء")
                    }
                    TextButton(onClick = onOpenReminderSettings) {
                        Text("الإعدادات")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.notifications.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.NotificationsOff,
                        title = "لا توجد إشعارات بعد",
                        subtitle = "ستظهر هنا تنبيهات مواعيد الاستحقاق والديون المتأخرة والمدفوعات.",
                        actionLabel = "إعداد التذكيرات",
                        onAction = onOpenReminderSettings
                    )
                }
            } else {
                items(state.notifications, key = { it.id }) { ntf ->
                    FintechSurfaceCard(
                        onClick = { ntf.relatedDebtId?.let(onOpenDebt) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!ntf.isRead) hc.positive.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (!ntf.isRead) hc.positive else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        ntf.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (!ntf.isRead) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        ArabicReportExporter.formatDate(ntf.createdAtMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    ntf.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
fun ReminderSettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSaveNotificationSettings: (Boolean, Int, Int, Boolean) -> Unit,
    onCreateReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onCheckDueNow: () -> Unit = {},
    onSendTestNotification: () -> Unit = {}
) {
    val context = LocalContext.current
    val hc = HisabatiTheme.colors
    var enabled by remember(state.preferences) { mutableStateOf(state.preferences.notificationsEnabled) }
    var hour by remember(state.preferences) { mutableIntStateOf(state.preferences.defaultReminderHour) }
    var minute by remember(state.preferences) { mutableIntStateOf(state.preferences.defaultReminderMinute) }
    var highPriority by remember(state.preferences) { mutableStateOf(state.preferences.reminderPriorityHigh) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            NotificationHelper.schedulePeriodicWorkers(context, hour, minute)
            onCheckDueNow()
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات التذكيرات والإشعارات") },
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
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                item {
                    FintechSurfaceCard {
                        Text(
                            "إذن الإشعارات مطلوب",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = hc.warning
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "اسمح للتطبيق بإرسال التنبيهات المحلية في مواعيد استحقاق الديون والأقساط.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.testTag("grant_notification_permission_button")
                        ) {
                            Text("منح إذن الإشعارات")
                        }
                    }
                }
            }

            item {
                FintechSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل التذكيرات التلقائية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("فحص مواعيد سداد الأقساط وتحصيل الديون المستحقة والمتأخرة في الخلفية", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("أولوية إشعار عالية", style = MaterialTheme.typography.titleSmall)
                            Text("إظهار تنبيهات الاستحقاق العاجلة في مقدمة الإشعارات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = highPriority, onCheckedChange = { highPriority = it })
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hour.toString(),
                            onValueChange = { hour = it.toIntOrNull()?.coerceIn(0, 23) ?: hour },
                            label = { Text("الساعة (0-23)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minute.toString(),
                            onValueChange = { minute = it.toIntOrNull()?.coerceIn(0, 59) ?: minute },
                            label = { Text("الدقيقة (0-59)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onSaveNotificationSettings(enabled, hour, minute, highPriority)
                            if (enabled) {
                                NotificationHelper.schedulePeriodicWorkers(context, hour, minute)
                                onCheckDueNow()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_reminder_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("حفظ إعدادات التذكير", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCheckDueNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_local_notifications_now_button")
                        ) {
                            Text("فحص المستحقات الآن", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("send_test_local_notification_button")
                        ) {
                            Text("إشعار تجريبي", maxLines = 1)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التذكيرات المجدولة (${state.reminders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.AddAlert, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة تذكير")
                    }
                }
            }

            if (state.reminders.isEmpty()) {
                item {
                    FintechSurfaceCard {
                        Text("لا توجد تذكيرات مخصصة حالياً. يمكنك إضافة تذكير خاص لأي دين.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(state.reminders, key = { it.id }) { rem ->
                    val repeatLabel = runCatching { RepeatType.valueOf(rem.repeatType).labelAr }.getOrDefault(rem.repeatType)
                    val prioLabel = runCatching { ReminderPriority.valueOf(rem.priority).labelAr }.getOrDefault(rem.priority)
                    FintechSurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rem.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(rem.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${ArabicReportExporter.formatDate(rem.triggerAtMillis)} • $repeatLabel • أولوية $prioLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = hc.positive
                                )
                            }
                            IconButton(onClick = { onDeleteReminder(rem.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف التذكير", tint = hc.negative)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            state = state,
            onDismiss = { showAddDialog = false },
            onSave = { rem ->
                onCreateReminder(rem)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddReminderDialog(
    state: HisabatiUiState,
    onDismiss: () -> Unit,
    onSave: (ReminderEntity) -> Unit
) {
    val firstDebt = state.debts.firstOrNull()
    var title by remember { mutableStateOf("تذكير بموعد سداد") }
    var message by remember { mutableStateOf(firstDebt?.let { "تذكير بمتابعة دين ${it.contactName}" } ?: "تذكير بسداد المستحقات") }
    var daysAhead by remember { mutableStateOf("1") }
    var repeatType by remember { mutableStateOf(RepeatType.ONCE) }
    var priority by remember { mutableStateOf(ReminderPriority.HIGH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء تذكير مخصص") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان التذكير") }, singleLine = true)
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("نص التذكير") })
                OutlinedTextField(value = daysAhead, onValueChange = { if (it.all(Char::isDigit)) daysAhead = it }, label = { Text("بعد كم يوم؟") }, singleLine = true)
                Text("التكرار", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RepeatType.entries.take(3).forEach { r ->
                        FilterChip(selected = repeatType == r, onClick = { repeatType = r }, label = { Text(r.labelAr) })
                    }
                }
                Text("الأولوية", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderPriority.entries.forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.labelAr) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val days = (daysAhead.toLongOrNull() ?: 1L).coerceAtLeast(0L)
                    onSave(
                        ReminderEntity(
                            id = "rem_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                            userId = state.contacts.firstOrNull()?.userId ?: "",
                            debtId = firstDebt?.id ?: "",
                            contactId = firstDebt?.contactId ?: "",
                            title = title.ifBlank { "تذكير مالي" },
                            message = message.ifBlank { "تذكير بمتابعة الديون" },
                            triggerAtMillis = now + days * 86_400_000L,
                            repeatType = repeatType.name,
                            priority = priority.name,
                            isEnabled = true,
                            isSent = false,
                            createdAtMillis = now,
                            updatedAtMillis = now
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSaveRelayConfig: (Boolean, Boolean, String, String, String) -> Unit,
    onSaveWhatsAppFormat: (
        style: String,
        includeHeader: Boolean,
        includeBreakdown: Boolean,
        includeDates: Boolean,
        includeReason: Boolean,
        includeFooter: Boolean,
        customSignature: String
    ) -> Unit,
    onSaveTemplate: (MessageTemplateEntity) -> Unit,
    onExportWhatsAppJson: suspend () -> String,
    onImportWhatsAppJson: (String) -> Unit,
    onSendCloudTest: (MessageChannel, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hc = HisabatiTheme.colors

    var waEnabled by remember(state.preferences) { mutableStateOf(state.preferences.whatsappCloudEnabled) }
    var backendUrl by remember(state.preferences) { mutableStateOf(state.preferences.backendBaseUrl) }
    var phoneNumberId by remember(state.preferences) { mutableStateOf(state.preferences.whatsappPhoneNumberId) }
    var testPhone by remember { mutableStateOf(state.contacts.firstOrNull()?.phone ?: "") }

    // WhatsApp Professional Message Formatting State
    var msgStyle by remember(state.preferences.whatsappStyle) { mutableStateOf(state.preferences.whatsappStyle) }
    var includeHeader by remember(state.preferences.whatsappIncludeWorkspaceHeader) { mutableStateOf(state.preferences.whatsappIncludeWorkspaceHeader) }
    var includeBreakdown by remember(state.preferences.whatsappIncludeFinancialBreakdown) { mutableStateOf(state.preferences.whatsappIncludeFinancialBreakdown) }
    var includeDates by remember(state.preferences.whatsappIncludeDates) { mutableStateOf(state.preferences.whatsappIncludeDates) }
    var includeReason by remember(state.preferences.whatsappIncludeReasonAndNotes) { mutableStateOf(state.preferences.whatsappIncludeReasonAndNotes) }
    var includeFooter by remember(state.preferences.whatsappIncludeFooterSignature) { mutableStateOf(state.preferences.whatsappIncludeFooterSignature) }
    var customSignature by remember(state.preferences.whatsappCustomSignature) { mutableStateOf(state.preferences.whatsappCustomSignature) }

    var editingTemplate by remember { mutableStateOf<MessageTemplateEntity?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    val previewPrefs = remember(
        state.preferences,
        msgStyle,
        includeHeader,
        includeBreakdown,
        includeDates,
        includeReason,
        includeFooter,
        customSignature
    ) {
        state.preferences.copy(
            whatsappStyle = msgStyle,
            whatsappIncludeWorkspaceHeader = includeHeader,
            whatsappIncludeFinancialBreakdown = includeBreakdown,
            whatsappIncludeDates = includeDates,
            whatsappIncludeReasonAndNotes = includeReason,
            whatsappIncludeFooterSignature = includeFooter,
            whatsappCustomSignature = customSignature
        )
    }

    val livePreviewMessage = remember(previewPrefs) {
        ExternalMessagingHelper.buildFormattedWhatsAppMessage(
            prefs = previewPrefs,
            title = "سند إشعار مالي",
            contactName = state.contacts.firstOrNull()?.fullName ?: "أحمد محمد",
            directionLabel = "مستحق لي",
            principalFormatted = "150,000 ${state.metrics.defaultCurrencySymbol}",
            paidFormatted = "50,000 ${state.metrics.defaultCurrencySymbol}",
            remainingFormatted = "100,000 ${state.metrics.defaultCurrencySymbol}",
            recordedDateLabel = ArabicReportExporter.formatDate(System.currentTimeMillis()),
            dueDateLabel = ArabicReportExporter.formatDate(System.currentTimeMillis() + 15L * 86_400_000L),
            reason = "فاتورة مبيعات رقم 104",
            notes = "الدفعة القادمة حسب الاتفاق",
            receiptNumber = "DBT-202601"
        )
    }

    val exportTemplatesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = onExportWhatsAppJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                exportStatus = "تم تصدير قوالب وتنسيقات واتساب بنجاح ✓"
            }
        }
    }

    val importTemplatesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                onImportWhatsAppJson(content)
                exportStatus = "تم استيراد قوالب وتنسيقات واتساب بنجاح ✓"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات واتساب والقوالب") },
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1) Message Style & Content Control
            FintechSurfaceCard {
                Text("تصميم وشكل رسائل واتساب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "تحكم بأسلوب الرسالة وما يتم تضمينه من تفاصيل عند إرسال السندات والتذكيرات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WhatsAppMessageStyle.entries.forEach { st ->
                        FilterChip(
                            selected = msgStyle == st.code,
                            onClick = { msgStyle = st.code },
                            label = { Text(st.labelAr) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                WhatsAppToggleRow("تضمين اسم المتجر / النشاط وعنوان السند", includeHeader) { includeHeader = it }
                WhatsAppToggleRow("تضمين التفصيل المالي (الأصل / المسدد / المتبقي)", includeBreakdown) { includeBreakdown = it }
                WhatsAppToggleRow("تضمين تواريخ القيد والاستحقاق", includeDates) { includeDates = it }
                WhatsAppToggleRow("تضمين البيان والسبب والملاحظات", includeReason) { includeReason = it }
                WhatsAppToggleRow("تضمين التوقيع الختامي أسفل الرسالة", includeFooter) { includeFooter = it }
                if (includeFooter) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customSignature,
                        onValueChange = { customSignature = it },
                        label = { Text("نص التوقيع الختامي") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveWhatsAppFormat(
                            msgStyle,
                            includeHeader,
                            includeBreakdown,
                            includeDates,
                            includeReason,
                            includeFooter,
                            customSignature
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_whatsapp_format_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ تصميم وتنسيق الرسائل", fontWeight = FontWeight.Bold)
                }
            }

            // 2) Live Preview & Export/Import
            FintechSurfaceCard {
                Text("معاينة حية لشكل الرسالة وتصديرها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = livePreviewMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            exportTemplatesLauncher.launch("hisabati_whatsapp_templates_${System.currentTimeMillis()}.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_whatsapp_templates_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = hc.positive)
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تصدير JSON", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            importTemplatesLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_whatsapp_templates_button")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("استيراد JSON", maxLines = 1)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { ExternalMessagingHelper.openWhatsAppShareText(context, livePreviewMessage) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تصدير ومشاركة نموذج الرسالة كنص")
                }
                if (exportStatus != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(exportStatus!!, style = MaterialTheme.typography.bodySmall, color = hc.positive)
                }
            }

            // 3) Cloud Gateway Settings
            FintechSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = hc.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("خيارات الإرسال المتاحة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "1) الإرسال المباشر عبر واتساب على جهازك يعمل فوراً.\n2) الإرسال الآلي عبر WhatsApp Business Cloud API يعمل عبر رابط الخادم الخلفي الآمن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل بوابة WhatsApp Cloud API", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Switch(checked = waEnabled, onCheckedChange = { waEnabled = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("رابط الخادم الخلفي HTTPS (Backend URL)") },
                    placeholder = { Text("https://api.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumberId,
                    onValueChange = { phoneNumberId = it },
                    label = { Text("معرف رقم الواتساب (Phone Number ID)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveRelayConfig(
                            waEnabled,
                            state.preferences.smsGatewayEnabled,
                            backendUrl,
                            phoneNumberId,
                            state.preferences.smsSenderId
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_whatsapp_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ إعدادات البوابة", fontWeight = FontWeight.Bold)
                }
            }

            // 4) Test Message
            FintechSurfaceCard {
                Text("تجربة إرسال رسالة واتساب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = testPhone,
                    onValueChange = { testPhone = it },
                    label = { Text("رقم الهاتف مع رمز الدولة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (testPhone.isNotBlank()) {
                                ExternalMessagingHelper.openWhatsAppChat(context, testPhone, livePreviewMessage)
                            } else {
                                ExternalMessagingHelper.openWhatsAppShareText(context, livePreviewMessage)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Chat, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("تجربة في واتساب")
                    }
                    Button(
                        onClick = {
                            onSendCloudTest(MessageChannel.WHATSAPP, testPhone, livePreviewMessage, "TEST")
                        },
                        enabled = waEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إرسال سحابي")
                    }
                }
            }

            Text("قوالب الرسائل العربية (${state.templates.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.templates.forEach { tpl ->
                FintechSurfaceCard(onClick = { editingTemplate = tpl }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tpl.titleAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("تعديل", style = MaterialTheme.typography.labelLarge, color = hc.positive)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(tpl.bodyTemplate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    editingTemplate?.let { tpl ->
        var body by remember(tpl.id) { mutableStateOf(tpl.bodyTemplate) }
        AlertDialog(
            onDismissRequest = { editingTemplate = null },
            title = { Text("تعديل قالب: ${tpl.titleAr}") },
            text = {
                Column {
                    Text(
                        "المتغيرات المتاحة: {الاسم} {المبلغ} {المتبقي} {التاريخ} {السبب} {رقم_الإيصال}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveTemplate(tpl.copy(bodyTemplate = body, updatedAtMillis = System.currentTimeMillis()))
                    editingTemplate = null
                }) { Text("حفظ القالب") }
            },
            dismissButton = { TextButton(onClick = { editingTemplate = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun WhatsAppToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSaveRelayConfig: (Boolean, Boolean, String, String, String) -> Unit,
    onSendCloudTest: (MessageChannel, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val hc = HisabatiTheme.colors
    var smsEnabled by remember(state.preferences) { mutableStateOf(state.preferences.smsGatewayEnabled) }
    var backendUrl by remember(state.preferences) { mutableStateOf(state.preferences.backendBaseUrl) }
    var senderId by remember(state.preferences) { mutableStateOf(state.preferences.smsSenderId) }
    var testPhone by remember { mutableStateOf(state.contacts.firstOrNull()?.phone ?: "") }
    var testBody by remember { mutableStateOf("تذكير من تطبيق حساباتي بمتابعة المستحقات المالية.") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات الرسائل النصية SMS") },
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FintechSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = hc.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("أمان مزود الرسائل النصية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "تُحفظ مفاتيح مزود الرسائل النصية (Twilio / Unifonic / Taqnyat) في الخادم الخلفي الآمن فقط. كما يمكنك دوماً إرسال SMS مجاناً عبر تطبيق الرسائل في هاتفك.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FintechSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل بوابة SMS السحابية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Switch(checked = smsEnabled, onCheckedChange = { smsEnabled = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = backendUrl,
                    onValueChange = { backendUrl = it },
                    label = { Text("رابط الخادم الخلفي HTTPS") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = senderId,
                    onValueChange = { senderId = it },
                    label = { Text("اسم المرسل (Sender ID)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveRelayConfig(
                            state.preferences.whatsappCloudEnabled,
                            smsEnabled,
                            backendUrl,
                            state.preferences.whatsappPhoneNumberId,
                            senderId
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_sms_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("حفظ إعدادات SMS", fontWeight = FontWeight.Bold)
                }
            }

            FintechSurfaceCard {
                Text("اختبار إرسال رسالة SMS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = testPhone, onValueChange = { testPhone = it }, label = { Text("رقم الهاتف") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = testBody, onValueChange = { testBody = it }, label = { Text("نص الرسالة") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { ExternalMessagingHelper.openSmsComposer(context, testPhone, testBody) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("عبر الهاتف")
                    }
                    Button(
                        onClick = { onSendCloudTest(MessageChannel.SMS, testPhone, testBody, "TEST") },
                        enabled = smsEnabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إرسال سحابي")
                    }
                }
            }
        }
    }
}
