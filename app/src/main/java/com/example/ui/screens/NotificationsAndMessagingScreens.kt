package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddAlert
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.notifications.NotificationHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PayableCoral
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.HisabatiUiState
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenDebt: (String) -> Unit,
    onOpenReminderSettings: () -> Unit
) {
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (!ntf.isRead) EmeraldPrimary.copy(alpha = 0.16f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (!ntf.isRead) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        ntf.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (!ntf.isRead) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        ArabicReportExporter.formatDate(ntf.createdAtMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    ntf.body,
                                    style = MaterialTheme.typography.bodyMedium,
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
    onDeleteReminder: (String) -> Unit
) {
    val context = LocalContext.current
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
            NotificationHelper.schedulePeriodicWorkers(context)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                item {
                    FintechSurfaceCard {
                        Text(
                            "إذن الإشعارات مطلوب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "اسمح للتطبيق بإرسال التنبيهات المحلية في مواعيد استحقاق الديون والأقساط.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
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
                            Text("تفعيل التذكيرات التلقائية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("فحص الديون القريبة والمتأخرة في الخلفية عبر WorkManager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("أولوية إشعار عالية", style = MaterialTheme.typography.titleMedium)
                            Text("إظهار تنبيهات الاستحقاق العاجلة في مقدمة الإشعارات", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = highPriority, onCheckedChange = { highPriority = it })
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onSaveNotificationSettings(enabled, hour, minute, highPriority)
                            if (enabled) NotificationHelper.schedulePeriodicWorkers(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_reminder_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("حفظ إعدادات التذكير", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التذكيرات المجدولة (${state.reminders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        Text("لا توجد تذكيرات مخصصة حالياً. يمكنك إضافة تذكير خاص لأي دين.", style = MaterialTheme.typography.bodyMedium)
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
                                Text(rem.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(rem.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${ArabicReportExporter.formatDate(rem.triggerAtMillis)} • $repeatLabel • أولوية $prioLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                            }
                            IconButton(onClick = { onDeleteReminder(rem.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف التذكير", tint = PayableCoral)
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
    onSaveTemplate: (MessageTemplateEntity) -> Unit,
    onSendCloudTest: (MessageChannel, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var waEnabled by remember(state.preferences) { mutableStateOf(state.preferences.whatsappCloudEnabled) }
    var backendUrl by remember(state.preferences) { mutableStateOf(state.preferences.backendBaseUrl) }
    var phoneNumberId by remember(state.preferences) { mutableStateOf(state.preferences.whatsappPhoneNumberId) }
    var testPhone by remember { mutableStateOf(state.contacts.firstOrNull()?.phone ?: "") }

    var editingTemplate by remember { mutableStateOf<MessageTemplateEntity?>(null) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FintechSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = GoldAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("خيارات الإرسال المتاحة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "1) الإرسال المباشر عبر تطبيق واتساب على جهازك يعمل فوراً دون خادم.\n2) الإرسال الآلي عبر WhatsApp Business Cloud API يتطلب نشر خادم Node.js المرفق وضبط رمز الوصول في الخادم (لا يُخزن أي رمز سري داخل تطبيق الهاتف).",
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
                    Text("تفعيل بوابة WhatsApp Cloud API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = waEnabled, onCheckedChange = { waEnabled = it })
                }
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(12.dp))
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
                        .height(48.dp)
                        .testTag("save_whatsapp_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ إعدادات واتساب", fontWeight = FontWeight.Bold)
                }
            }

            FintechSurfaceCard {
                Text("تجربة إرسال رسالة واتساب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = testPhone,
                    onValueChange = { testPhone = it },
                    label = { Text("رقم الهاتف مع رمز الدولة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val sample = state.templates.firstOrNull()?.bodyTemplate ?: "تذكير من تطبيق حساباتي"
                            ExternalMessagingHelper.openWhatsAppChat(context, testPhone, sample)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Chat, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("فتح واتساب")
                    }
                    Button(
                        onClick = {
                            val sample = state.templates.firstOrNull()?.bodyTemplate ?: "تذكير من تطبيق حساباتي"
                            onSendCloudTest(MessageChannel.WHATSAPP, testPhone, sample, "TEST")
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

            Text("قوالب الرسائل العربية (${state.templates.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            state.templates.forEach { tpl ->
                FintechSurfaceCard(onClick = { editingTemplate = tpl }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tpl.titleAr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("تعديل", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSaveRelayConfig: (Boolean, Boolean, String, String, String) -> Unit,
    onSendCloudTest: (MessageChannel, String, String, String) -> Unit
) {
    val context = LocalContext.current
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FintechSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = GoldAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("أمان مزود الرسائل النصية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text("تفعيل بوابة SMS السحابية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = smsEnabled, onCheckedChange = { smsEnabled = it })
                }
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(12.dp))
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
                        .height(48.dp)
                        .testTag("save_sms_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("حفظ إعدادات SMS", fontWeight = FontWeight.Bold)
                }
            }

            FintechSurfaceCard {
                Text("اختبار إرسال رسالة SMS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
