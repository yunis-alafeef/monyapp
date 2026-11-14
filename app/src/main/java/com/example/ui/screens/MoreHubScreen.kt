package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.rounded.Palette
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.theme.HisabatiTheme

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
    val hc = HisabatiTheme.colors
    val items = listOf(
        HubMenuItem("الثيمات والمظهر", "ألوان ساطعة وداكنة ومخصصة", Icons.Rounded.Palette, MaterialTheme.colorScheme.primary, "hub_themes", onOpenSettings),
        HubMenuItem("التقارير والتصدير", "PDF عربي و Excel", Icons.Rounded.Assessment, hc.positive, "hub_reports", onOpenReports),
        HubMenuItem("كشوف الحساب", "كشف تفصيلي لكل شخص", Icons.Rounded.Description, hc.info, "hub_statement", onOpenStatement),
        HubMenuItem("المساعد الذكي", "تحليلات وإجابات فورية", Icons.Rounded.AutoAwesome, hc.accent, "hub_ai", onOpenAi),
        HubMenuItem("البحث الشامل", "تصفية بالعملة والحالة", Icons.Rounded.Search, hc.positive, "hub_search", onOpenSearch),
        HubMenuItem("الإشعارات", "سجل التنبيهات", Icons.Rounded.NotificationsActive, hc.warning, "hub_notifications", onOpenNotifications),
        HubMenuItem("التذكيرات", "جدولة WorkManager", Icons.Rounded.NotificationsActive, hc.negative, "hub_reminders", onOpenReminders),
        HubMenuItem("إعدادات واتساب", "التنسيق، القوالب والتصدير", Icons.Rounded.Chat, hc.positive, "hub_whatsapp", onOpenWhatsApp),
        HubMenuItem("رسائل SMS", "بوابة الرسائل النصية", Icons.Rounded.Sms, hc.info, "hub_sms", onOpenSms),
        HubMenuItem("فريق العمل RBAC", "إدارة الأدوار والصلاحيات", Icons.Rounded.Groups, hc.accent, "hub_workspace", onOpenWorkspace),
        HubMenuItem("النسخ والمزامنة", "تصدير واستيراد JSON", Icons.Rounded.Backup, hc.positive, "hub_backup", onOpenBackup),
        HubMenuItem("الأمان والملف", "PIN والبصمة وسجل التدقيق", Icons.Rounded.Security, hc.negative, "hub_profile", onOpenProfile),
        HubMenuItem("الإعدادات العامة", "العملة الافتراضية (C) والتخصيص", Icons.Rounded.Settings, hc.info, "hub_settings", onOpenSettings),
        HubMenuItem("المساعدة والدعم", "الأسئلة الشائعة", Icons.Rounded.HelpOutline, hc.warning, "hub_help", onOpenHelp)
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("الأدوات والإعدادات المتقدمة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.testTag }) { item ->
                FintechSurfaceCard(
                    onClick = item.onClick,
                    contentPadding = PaddingValues(11.dp),
                    modifier = Modifier.testTag(item.testTag)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(item.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, contentDescription = item.title, tint = item.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
