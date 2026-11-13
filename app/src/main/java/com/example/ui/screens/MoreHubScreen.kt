package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InfoSlate
import com.example.ui.theme.PayableCoral
import com.example.ui.theme.WarningAmber

private data class HubMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreHubScreen(
    onOpenReports: () -> Unit,
    onOpenStatement: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onOpenSms: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHelp: () -> Unit
) {
    val items = listOf(
        HubMenuItem("التقارير والتصدير", "PDF عربي و Excel", Icons.Rounded.Assessment, EmeraldPrimary, "hub_reports", onOpenReports),
        HubMenuItem("كشوف الحساب", "كشف تفصيلي لكل شخص", Icons.Rounded.Description, InfoSlate, "hub_statement", onOpenStatement),
        HubMenuItem("المساعد الذكي", "تحليلات وإجابات فورية", Icons.Rounded.AutoAwesome, GoldAccent, "hub_ai", onOpenAi),
        HubMenuItem("البحث الشامل", "تصفية بالعملة والحالة", Icons.Rounded.Search, EmeraldPrimary, "hub_search", onOpenSearch),
        HubMenuItem("الإشعارات", "سجل التنبيهات", Icons.Rounded.NotificationsActive, WarningAmber, "hub_notifications", onOpenNotifications),
        HubMenuItem("التذكيرات", "جدولة WorkManager", Icons.Rounded.NotificationsActive, PayableCoral, "hub_reminders", onOpenReminders),
        HubMenuItem("إعدادات واتساب", "القوالب والإرسال السحابي", Icons.Rounded.Chat, EmeraldPrimary, "hub_whatsapp", onOpenWhatsApp),
        HubMenuItem("رسائل SMS", "بوابة الرسائل النصية", Icons.Rounded.Sms, InfoSlate, "hub_sms", onOpenSms),
        HubMenuItem("فريق العمل RBAC", "إدارة الأدوار والصلاحيات", Icons.Rounded.Groups, GoldAccent, "hub_workspace", onOpenWorkspace),
        HubMenuItem("النسخ والمزامنة", "تصدير واستيراد JSON", Icons.Rounded.Backup, EmeraldPrimary, "hub_backup", onOpenBackup),
        HubMenuItem("الأمان والملف", "PIN والبصمة وسجل التدقيق", Icons.Rounded.Security, PayableCoral, "hub_profile", onOpenProfile),
        HubMenuItem("الإعدادات العامة", "المظهر والعملات", Icons.Rounded.Settings, InfoSlate, "hub_settings", onOpenSettings),
        HubMenuItem("المساعدة والدعم", "الأسئلة الشائعة", Icons.Rounded.HelpOutline, WarningAmber, "hub_help", onOpenHelp)
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("الأدوات والإعدادات المتقدمة") })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.testTag }) { item ->
                FintechSurfaceCard(
                    onClick = item.onClick,
                    modifier = Modifier.testTag(item.testTag)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(item.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.title, tint = item.accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
