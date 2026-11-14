package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Print
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AccountType
import com.example.domain.model.WorkspaceRole
import com.example.export.ArabicReportExporter
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.CustomThemeAccentSwatches
import com.example.ui.theme.CustomThemePrimarySwatches
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.HisabatiUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: HisabatiUiState,
    onBack: () -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetThemePalette: (AppThemePalette) -> Unit,
    onApplyCustomColors: (primaryHex: Long, accentHex: Long, mode: AppThemeMode?) -> Unit,
    onSaveCustomTheme: (nameAr: String, primaryHex: Long, accentHex: Long, darkModePreferred: Boolean) -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onSetDefaultCurrency: (String) -> Unit,
    onAddCustomCurrency: (String, String, String, Long) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrintSettings: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onOpenSms: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val hc = HisabatiTheme.colors
    var showAddCurrencyDialog by remember { mutableStateOf(false) }
    var showCustomThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات والثيمات والعملة") },
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
            // 1) Theme Mode + Professional Curated Palettes + Custom Themes
            FintechSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("المظهر والثيمات الاحترافية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showCustomThemeDialog = true },
                        modifier = Modifier.testTag("create_custom_theme_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("ثيم مخصص")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("وضع الإضاءة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.preferences.themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.titleAr) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("لوحات الألوان الجاهزة (ساطعة وداكنة)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemePalette.entries.filter { it != AppThemePalette.CUSTOM }.forEach { palette ->
                        val selected = state.preferences.themePalette == palette
                        Surface(
                            onClick = { onSetThemePalette(palette) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 0.8.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.width(136.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(palette.previewPrimaryHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(Color(palette.previewAccentHex))
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "مفعل",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = palette.titleAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = palette.subtitleAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (state.preferences.customThemes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("ثيماتك المخصصة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.preferences.customThemes.forEach { preset ->
                            val isActive = state.preferences.themePalette == AppThemePalette.CUSTOM &&
                                state.preferences.customPrimaryHex == preset.primaryHex &&
                                state.preferences.customAccentHex == preset.accentHex
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onApplyCustomColors(
                                            preset.primaryHex,
                                            preset.accentHex,
                                            if (preset.darkModePreferred) AppThemeMode.DARK else AppThemeMode.LIGHT
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(preset.primaryHex), Color(preset.accentHex))
                                                    )
                                                )
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(preset.nameAr, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                if (preset.darkModePreferred) "نمط داكن فاخر" else "نمط فاتح ساطع",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteCustomTheme(preset.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف الثيم", tint = hc.negative, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2) Default Currency ("C") & Exchange Rates
            FintechSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("العملة الافتراضية (C)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "المفعلة حالياً: ${state.metrics.defaultCurrencyCode} (${state.metrics.defaultCurrencySymbol})",
                            style = MaterialTheme.typography.bodySmall,
                            color = hc.positive
                        )
                    }
                    TextButton(onClick = { showAddCurrencyDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("إضافة عملة")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .testTag("settings_currency_row"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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

            SettingsNavRow("إعدادات الطباعة والفواتير PDF (القوالب والشعار والألوان)", Icons.Rounded.Print, onOpenPrintSettings)
            SettingsNavRow("إعدادات التذكيرات والإشعارات", Icons.Rounded.Notifications, onOpenNotifications)
            SettingsNavRow("إعدادات واتساب وقوالب الرسائل والتصدير", Icons.Rounded.Chat, onOpenWhatsApp)
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

    if (showCustomThemeDialog) {
        CreateCustomThemeDialog(
            onDismiss = { showCustomThemeDialog = false },
            onCreate = { name, primaryHex, accentHex, darkMode ->
                onSaveCustomTheme(name, primaryHex, accentHex, darkMode)
                showCustomThemeDialog = false
            }
        )
    }
}

@Composable
private fun CreateCustomThemeDialog(
    onDismiss: () -> Unit,
    onCreate: (nameAr: String, primaryHex: Long, accentHex: Long, darkModePreferred: Boolean) -> Unit
) {
    var themeName by remember { mutableStateOf("") }
    var selectedPrimary by remember { mutableLongStateOf(CustomThemePrimarySwatches.first().second) }
    var selectedAccent by remember { mutableLongStateOf(CustomThemeAccentSwatches.first().second) }
    var preferDark by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء ثيم ألوان مخصص") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(selectedPrimary), Color(selectedAccent))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = themeName.ifBlank { "معاينة الثيم الجديد" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("اسم الثيم (مثال: السماوي الذهبي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اللون الرئيسي", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomThemePrimarySwatches.forEach { (label, hex) ->
                        val isSel = selectedPrimary == hex
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .then(
                                        if (isSel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedPrimary = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Rounded.Check, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text("لون التمييز الثانوي", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomThemeAccentSwatches.forEach { (label, hex) ->
                        val isSel = selectedAccent == hex
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .then(
                                        if (isSel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedAccent = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(Icons.Rounded.Check, contentDescription = label, tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (preferDark) "الوضع المفضل: داكن فاخر" else "الوضع المفضل: فاتح ساطع", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = preferDark, onCheckedChange = { preferDark = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onCreate(themeName.ifBlank { "ثيم مخصص" }, selectedPrimary, selectedAccent, preferDark)
            }) { Text("حفظ وتطبيق الثيم") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun SettingsNavRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    FintechSurfaceCard(onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            }
            Text("فتح", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
    val hc = HisabatiTheme.colors
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
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FintechSurfaceCard {
                    Text("نوع الحساب التجاري / الشخصي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                    Text("الأعضاء والصلاحيات (${state.members.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${m.email} • $roleLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "إضافة دين: ${if (m.canAddDebt) "نعم" else "لا"} | تعديل: ${if (m.canEditDebt) "نعم" else "لا"} | دفعات: ${if (m.canRecordPayment) "نعم" else "لا"} | تقارير: ${if (m.canViewReports) "نعم" else "لا"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = hc.positive
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (!isActive) {
                                TextButton(onClick = { onSetActiveMember(m.id) }) { Text("تفعيل") }
                            }
                            if (m.id != "owner") {
                                IconButton(onClick = { onRemoveMember(m.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "إزالة العضو", tint = hc.negative)
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
    val hc = HisabatiTheme.colors
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FintechSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = hc.positive, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("حالة المزامنة السحابية (Firestore)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("العمليات المعلقة: ${state.pendingOutboxCount}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "آخر مزامنة: ${if (state.preferences.lastSyncAtMillis > 0) ArabicReportExporter.formatDate(state.preferences.lastSyncAtMillis) else "لم تتم بعد"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSyncNow,
                    enabled = !state.isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("sync_now_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (state.isSyncing) "جاري المزامنة..." else "مزامنة الآن", fontWeight = FontWeight.Bold)
                }
            }

            FintechSurfaceCard {
                Text("النسخ الاحتياطي المحلي والسحابي (JSON)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "صدّر نسخة احتياطية كاملة لجميع الأشخاص والديون والمدفوعات إلى ملف JSON آمن أو استعدها في أي وقت.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { exportLauncher.launch("hisabati_backup_${System.currentTimeMillis()}.json") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
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
                        .height(46.dp)
                        .testTag("import_backup_button")
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("استعادة من نسخة احتياطية")
                }
                if (statusText != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(statusText!!, style = MaterialTheme.typography.bodySmall, color = hc.positive)
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
    val hc = HisabatiTheme.colors
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
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FintechSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = hc.positive, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(userDisplayName.ifBlank { "مستخدم حساباتي" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = hc.accent)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("قفل التطبيق برمز PIN", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(if (state.preferences.pinEnabled) "مفعّل حالياً" else "غير مفعّل", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (state.preferences.pinEnabled) {
                            TextButton(onClick = onDisablePin) { Text("إيقاف", color = hc.negative) }
                        } else {
                            Button(onClick = { showPinDialog = true }, modifier = Modifier.testTag("enable_pin_button")) {
                                Text("تفعيل PIN")
                            }
                        }
                    }
                    if (state.preferences.pinEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = hc.positive)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (biometricSupported) "الفتح بالبصمة الحيوية" else "البصمة غير مهيأة على هذا الجهاز",
                                    style = MaterialTheme.typography.bodySmall
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
                            .height(46.dp)
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
                            .height(46.dp)
                            .testTag("delete_account_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = hc.negative)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حذف الحساب والبيانات")
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = hc.positive)
                    Spacer(Modifier.width(6.dp))
                    Text("سجل التدقيق والعمليات (${state.auditLogs.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        Text("السبب: ${log.reason}", style = MaterialTheme.typography.labelSmall, color = hc.positive)
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
                    colors = ButtonDefaults.buttonColors(containerColor = hc.negative)
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
    val hc = HisabatiTheme.colors
    val faqs = listOf(
        "كيف يعمل التطبيق دون إنترنت؟" to "تُحفظ جميع عملياتك فوراً في قاعدة البيانات المحلية (Room) مع جدول انتظار للمزامنة (Outbox)، وبمجرد عودة الشبكة تُرفع التغييرات إلى Cloud Firestore دون أي تكرار.",
        "كيف أصدر كشف حساب PDF بالعربية؟" to "من شاشة تفاصيل الشخص أو قسم التقارير، اضغط على مشاركة PDF أو طباعة ليتم توليد ملف A4 عربي منسق جاهز للإرسال عبر واتساب أو البريد.",
        "كيف أغيّر العملة أو الثيم والألوان؟" to "من لوحة التحكم يمكنك الضغط على شارة العملة (C) لتغييرها فوراً، ومن الإعدادات يمكنك التبديل بين الثيمات الساطعة والداكنة أو إنشاء ثيمك المخصص.",
        "كيف أفعّل الإرسال التلقائي عبر واتساب أو SMS؟" to "الإرسال المباشر عبر تطبيق الهاتف يعمل مجاناً دون خادم، كما يمكنك تخصيص شكل رسائل واتساب وتصدير القوالب من شاشة إعدادات واتساب.",
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
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                FintechSurfaceCard {
                    Text("حساباتي | HISABATI — الإصدار 1.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("نظام عربي متكامل لإدارة الديون والمستحقات والمدفوعات الشخصية والتجارية.", style = MaterialTheme.typography.bodySmall)
                }
            }
            items(faqs) { (q, a) ->
                FintechSurfaceCard {
                    Text(q, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = hc.positive)
                    Spacer(Modifier.height(4.dp))
                    Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
