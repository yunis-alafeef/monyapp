package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.AccountType
import com.example.domain.model.WorkspaceRole
import com.example.export.ArabicReportExporter
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PayableCoral
import com.example.ui.viewmodel.HisabatiUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetDefaultCurrency: (String) -> Unit,
    onAddCustomCurrency: (String, String, String, Long) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onOpenSms: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHelp: () -> Unit
) {
    var showAddCurrencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والتخصيص") },
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
                Text("المظهر (الوضع الفاتح / الداكن)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.preferences.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.titleAr) }
                        )
                    }
                }
            }

            FintechSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("العملة الافتراضية وأسعار الصرف", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddCurrencyDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة عملة")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.currencies.forEach { cur ->
                        FilterChip(
                            selected = state.preferences.defaultCurrency == cur.code,
                            onClick = { onSetDefaultCurrency(cur.code) },
                            label = { Text("${cur.nameAr} (${cur.code})") }
                        )
                    }
                }
            }

            SettingsNavRow("إعدادات التذكيرات والإشعارات", Icons.Rounded.Notifications, onOpenNotifications)
            SettingsNavRow("إعدادات واتساب وقوالب الرسائل", Icons.Rounded.Chat, onOpenWhatsApp)
            SettingsNavRow("إعدادات الرسائل النصية SMS", Icons.Rounded.Sms, onOpenSms)
            SettingsNavRow("مساحة العمل وفريق الموظفين (RBAC)", Icons.Rounded.Groups, onOpenWorkspace)
            SettingsNavRow("النسخ الاحتياطي والمزامنة السحابية", Icons.Rounded.Backup, onOpenBackup)
            SettingsNavRow("الملف الشخصي، القفل، وسجل التدقيق", Icons.Rounded.Security, onOpenProfile)
            SettingsNavRow("المساعدة والدعم والأسئلة الشائعة", Icons.Rounded.HelpOutline, onOpenHelp)
        }
    }

    if (showAddCurrencyDialog) {
        AddCurrencyDialog(
            onDismiss = { showAddCurrencyDialog = false },
            onSave = { code, nameAr, symbol, rateMicros ->
                onAddCustomCurrency(code, nameAr, symbol, rateMicros)
                showAddCurrencyDialog = false
            }
        )
    }
}

@Composable
private fun SettingsNavRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    FintechSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = EmeraldPrimary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            }
            Text("فتح", style = MaterialTheme.typography.labelLarge, color = EmeraldPrimary)
        }
    }
}

@Composable
private fun AddCurrencyDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var usdRateText by remember { mutableStateOf("1.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عملة مخصصة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it.uppercase().take(5) }, label = { Text("رمز العملة (مثل BHD)") }, singleLine = true)
                OutlinedTextField(value = nameAr, onValueChange = { nameAr = it }, label = { Text("الاسم بالعربية") }, singleLine = true)
                OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("الرمز المختصر (مثل د.ب)") }, singleLine = true)
                OutlinedTextField(value = usdRateText, onValueChange = { usdRateText = it }, label = { Text("قيمة الوحدة الواحدة بالدولار") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                val usdVal = usdRateText.toBigDecimalOrNull() ?: java.math.BigDecimal.ONE
                val micros = usdVal.multiply(java.math.BigDecimal(1_000_000L)).toLong().coerceAtLeast(1L)
                onSave(code, nameAr, symbol, micros)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceMembersScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onUpdateWorkspace: (AccountType, String) -> Unit,
    onAddMember: (String, String, WorkspaceRole, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onRemoveMember: (String) -> Unit,
    onSetActiveMember: (String) -> Unit
) {
    var accountType by remember(state.preferences) { mutableStateOf(state.preferences.accountType) }
    var workspaceName by remember(state.preferences) { mutableStateOf(state.preferences.workspaceName) }
    var showInviteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مساحة العمل والصلاحيات") },
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
                FintechSurfaceCard {
                    Text("نوع الحساب التجاري / الشخصي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountType.entries.forEach { t ->
                            FilterChip(
                                selected = accountType == t,
                                onClick = { accountType = t },
                                label = { Text(t.labelAr) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = workspaceName,
                        onValueChange = { workspaceName = it },
                        label = { Text("اسم مساحة العمل أو المتجر") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onUpdateWorkspace(accountType, workspaceName) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("حفظ إعدادات المساحة")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الأعضاء والصلاحيات (${state.members.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة عضو")
                    }
                }
            }

            items(state.members, key = { it.id }) { m ->
                val roleLabel = runCatching { WorkspaceRole.valueOf(m.role).labelAr }.getOrDefault(m.role)
                val isActive = state.activeMember?.id == m.id
                FintechSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${m.displayName} ${if (isActive) "(النشط حالياً)" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${m.email} • $roleLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "إضافة دين: ${if (m.canAddDebt) "نعم" else "لا"} | تعديل: ${if (m.canEditDebt) "نعم" else "لا"} | دفعات: ${if (m.canRecordPayment) "نعم" else "لا"} | تقارير: ${if (m.canViewReports) "نعم" else "لا"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (!isActive) {
                                TextButton(onClick = { onSetActiveMember(m.id) }) { Text("تفعيل") }
                            }
                            if (m.id != "owner") {
                                IconButton(onClick = { onRemoveMember(m.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "إزالة العضو", tint = PayableCoral)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(WorkspaceRole.STAFF) }
        var canAdd by remember { mutableStateOf(true) }
        var canEdit by remember { mutableStateOf(false) }
        var canPay by remember { mutableStateOf(true) }
        var canRep by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("إضافة عضو لفريق العمل") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, singleLine = true)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(WorkspaceRole.MANAGER, WorkspaceRole.STAFF, WorkspaceRole.VIEWER).forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = {
                                    role = r
                                    canAdd = r.defaultAddDebt
                                    canEdit = r.defaultEditDebt
                                    canPay = r.defaultRecordPayment
                                    canRep = r.defaultViewReports
                                },
                                label = { Text(r.labelAr) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = canAdd, onCheckedChange = { canAdd = it }); Text("صلاحية إضافة دين") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = canEdit, onCheckedChange = { canEdit = it }); Text("صلاحية تعديل الديون") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = canPay, onCheckedChange = { canPay = it }); Text("صلاحية تسجيل دفعة") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = canRep, onCheckedChange = { canRep = it }); Text("صلاحية عرض التقارير") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onAddMember(name, email, role, canAdd, canEdit, canPay, canRep)
                    showInviteDialog = false
                }) { Text("إضافة") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("إلغاء") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSyncNow: () -> Unit,
    onExportJson: suspend () -> String,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = onExportJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                statusText = "تم حفظ النسخة الاحتياطية بنجاح"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (!content.isNullOrBlank()) {
                onImportJson(content)
                statusText = "تم استيراد البيانات من النسخة الاحتياطية"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("النسخ الاحتياطي والمزامنة") },
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
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("حالة المزامنة السحابية (Firestore)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("العمليات المعلقة: ${state.pendingOutboxCount}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "آخر مزامنة: ${if (state.preferences.lastSyncAtMillis > 0) ArabicReportExporter.formatDate(state.preferences.lastSyncAtMillis) else "لم تتم بعد"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onSyncNow,
                    enabled = !state.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sync_now_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (state.isSyncing) "جاري المزامنة..." else "مزامنة الآن", fontWeight = FontWeight.Bold)
                }
            }

            FintechSurfaceCard {
                Text("النسخ الاحتياطي المحلي والسحابي (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "صدّر نسخة احتياطية كاملة لجميع الأشخاص والديون والمدفوعات إلى ملف JSON آمن أو استعدها في أي وقت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { exportLauncher.launch("hisabati_backup_${System.currentTimeMillis()}.json") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_backup_button")
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تصدير نسخة احتياطية")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("import_backup_button")
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("استعادة من نسخة احتياطية")
                }
                if (statusText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(statusText!!, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: HisabatiUiState,
    userDisplayName: String,
    userEmail: String?,
    onBack: () -> Unit,
    onSetPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onSetBiometric: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccountData: () -> Unit
) {
    val context = LocalContext.current
    val biometricSupported = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الحساب والأمان وسجل التدقيق") },
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
                FintechSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(userDisplayName.ifBlank { "مستخدم حساباتي" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(userEmail ?: "حساب Google موثق", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = GoldAccent)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("قفل التطبيق برمز PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(if (state.preferences.pinEnabled) "مفعّل حالياً" else "غير مفعّل", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (state.preferences.pinEnabled) {
                            TextButton(onClick = onDisablePin) { Text("إيقاف", color = PayableCoral) }
                        } else {
                            Button(onClick = { showPinDialog = true }, modifier = Modifier.testTag("enable_pin_button")) {
                                Text("تفعيل PIN")
                            }
                        }
                    }
                    if (state.preferences.pinEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = EmeraldPrimary)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (biometricSupported) "الفتح بالبصمة الحيوية" else "البصمة غير مهيأة على هذا الجهاز",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = state.preferences.biometricEnabled && biometricSupported,
                                enabled = biometricSupported,
                                onCheckedChange = onSetBiometric
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("تسجيل الخروج")
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_account_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PayableCoral)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حذف الحساب والبيانات")
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("سجل التدقيق والعمليات (${state.auditLogs.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            items(state.auditLogs.take(30), key = { it.id }) { log ->
                FintechSurfaceCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(ArabicReportExporter.formatDate(log.createdAtMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                    if (!log.reason.isNullOrBlank()) {
                        Text("السبب: ${log.reason}", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("تعيين رمز حماية PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("أدخل 4 إلى 6 أرقام") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length >= 4) {
                            onSetPin(newPin)
                            showPinDialog = false
                        }
                    }
                ) { Text("حفظ الرمز") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد حذف الحساب والبيانات") },
            text = { Text("سيؤدي هذا الإجراء إلى مسح جميع السجلات المحلية والسحابية المرتبطة بحسابك نهائياً. هل أنت متأكد؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PayableCoral)
                ) { Text("نعم، احذف كل شيء") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("تراجع") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
    val faqs = listOf(
        "كيف يعمل التطبيق دون إنترنت؟" to "تُحفظ جميع عملياتك فوراً في قاعدة البيانات المحلية (Room) مع جدول انتظار للمزامنة (Outbox)، وبمجرد عودة الشبكة تُرفع التغييرات إلى Cloud Firestore دون أي تكرار.",
        "كيف أصدر كشف حساب PDF بالعربية؟" to "من شاشة تفاصيل الشخص أو قسم التقارير، اضغط على مشاركة PDF أو طباعة ليتم توليد ملف A4 عربي منسق جاهز للإرسال عبر واتساب أو البريد.",
        "كيف أفعّل الإرسال التلقائي عبر واتساب أو SMS؟" to "الإرسال المباشر عبر تطبيق الهاتف يعمل مجاناً دون خادم. أما الإرسال التلقائي بالخلفية فيتطلب نشر خادم Node.js المرفق وربط رابط HTTPS الخاص به في إعدادات واتساب أو SMS.",
        "هل بياناتي المالية معزولة وآمنة؟" to "نعم، كل مستخدم معزول تماماً في مسار السحابة الخاص به بقواعد أمان صارمة، ويمكنك تفعيل قفل PIN والبصمة الحيوية وسجل التدقيق في أي وقت."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المساعدة والدعم الفني") },
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
                FintechSurfaceCard {
                    Text("حساباتي | HISABATI — الإصدار 1.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("نظام عربي متكامل لإدارة الديون والمستحقات والمدفوعات الشخصية والتجارية.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(faqs) { (q, a) ->
                FintechSurfaceCard {
                    Text(q, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(a, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
