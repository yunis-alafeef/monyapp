package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.export.ArabicReportExporter
import com.example.ui.components.DebtListCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FintechHeroBannerCard
import com.example.ui.components.FintechSurfaceCard
import com.example.ui.components.MetricSummaryTile
import com.example.ui.components.MonthlyCashflowChart
import com.example.ui.components.ReceivablePayableComparisonCard
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.HisabatiUiState

@Composable
fun DashboardScreen(
    state: HisabatiUiState,
    userDisplayName: String,
    onAddDebtClick: () -> Unit,
    onQuickPaymentClick: (String) -> Unit,
    onDebtClick: (String) -> Unit,
    onPaymentClick: (String) -> Unit,
    onOpenOverdue: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onSelectCurrency: (String) -> Unit = {},
    onSyncNow: () -> Unit
) {
    val hc = HisabatiTheme.colors
    val m = state.metrics
    val sym = m.defaultCurrencySymbol
    val upcomingDebts = state.debts
        .filter { it.remainingMinor > 0L && it.status != DebtStatus.PAID.name && it.status != DebtStatus.SETTLED.name }
        .sortedBy { it.dueAtMillis }
        .take(5)
    val recentPayments = state.payments.take(4)
    val unreadNotifications = state.notifications.count { !it.isRead }
    var showCurrencyPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Top Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مرحباً، ${userDisplayName.ifBlank { "بك" }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.preferences.workspaceName} • ${state.preferences.accountType.labelAr}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(38.dp).testTag("dashboard_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "الثيمات والإعدادات",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onSyncNow,
                        modifier = Modifier.size(38.dp).testTag("dashboard_sync_button")
                    ) {
                        Icon(
                            imageVector = if (state.pendingOutboxCount > 0) Icons.Rounded.CloudUpload else Icons.Rounded.CloudDone,
                            contentDescription = "حالة المزامنة",
                            tint = if (state.pendingOutboxCount > 0) hc.warning else hc.positive,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.size(38.dp).testTag("dashboard_search_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "بحث ذكي", modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.size(38.dp).testTag("dashboard_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge { Text(unreadNotifications.toString(), fontSize = 10.sp) }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "الإشعارات", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Compact & Balanced Hero Card
        item {
            FintechHeroBannerCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صافي المركز المالي",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        onClick = { showCurrencyPicker = true },
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("dashboard_currency_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CurrencyExchange,
                                contentDescription = "تغيير العملة",
                                tint = hc.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "C • ${m.defaultCurrencyCode}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = MoneyMath.formatSignedMinor(m.netPositionMinor, sym),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (m.netPositionMinor >= 0) Color(0xFF6EE7B7) else Color(0xFFFFA5B4),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.11f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مستحق لي", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = MoneyMath.formatMinor(m.totalReceivableMinor, sym),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.11f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = Color(0xFFFFA5B4), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مستحق عليّ", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = MoneyMath.formatMinor(m.totalPayableMinor, sym),
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAddDebtClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("quick_add_debt_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF041D15)
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("إضافة دين", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = {
                            val firstOpen = upcomingDebts.firstOrNull()
                            if (firstOpen != null) {
                                onQuickPaymentClick(firstOpen.id)
                            } else {
                                onAddDebtClick()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("quick_record_payment_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("تسجيل دفعة", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Compact AI Assistant Banner
        item {
            FintechSurfaceCard(
                onClick = onOpenAiAssistant,
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier.testTag("dashboard_ai_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(hc.accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = hc.accent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المساعد المالي الذكي", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "تحليل المتأخرات، كبار العملاء، وملخص السيولة الفوري",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Balanced 2x3 Compact KPI Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryTile(
                    title = "إجمالي المحصّل",
                    value = MoneyMath.formatMinor(m.totalCollectedMinor, sym),
                    icon = Icons.Rounded.AccountBalanceWallet,
                    accentColor = hc.positive,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryTile(
                    title = "إجمالي المسدّد",
                    value = MoneyMath.formatMinor(m.totalPaidOutMinor, sym),
                    icon = Icons.Rounded.Payments,
                    accentColor = hc.info,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryTile(
                    title = "الديون المتأخرة",
                    value = MoneyMath.formatMinor(m.overdueAmountMinor, sym),
                    subtitle = "${m.overdueCount} دين",
                    icon = Icons.Rounded.WarningAmber,
                    accentColor = hc.negative,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenOverdue
                )
                MetricSummaryTile(
                    title = "اقترب استحقاقها",
                    value = MoneyMath.formatMinor(m.dueSoonAmountMinor, sym),
                    subtitle = "${m.dueSoonCount} خلال 7 أيام",
                    icon = Icons.Rounded.Schedule,
                    accentColor = hc.warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryTile(
                    title = "عدد الأشخاص",
                    value = "${m.contactsCount}",
                    icon = Icons.Rounded.Groups,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenContacts
                )
                MetricSummaryTile(
                    title = "عدد المعاملات",
                    value = "${m.transactionsCount}",
                    icon = Icons.Rounded.ReceiptLong,
                    accentColor = hc.accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Compact Interactive Charts
        item {
            MonthlyCashflowChart(series = m.monthlySeries)
        }
        item {
            ReceivablePayableComparisonCard(
                receivableMinor = m.totalReceivableMinor,
                payableMinor = m.totalPayableMinor,
                symbol = sym
            )
        }

        // Upcoming Dues Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المستحقات القادمة والمتأخرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenOverdue) { Text("عرض الكل") }
            }
        }
        if (upcomingDebts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "لا توجد مستحقات مفتوحة",
                    subtitle = "ابدأ بإضافة أول دين أو مستحق لمتابعته بدقة.",
                    actionLabel = "إضافة دين جديد",
                    onAction = onAddDebtClick
                )
            }
        } else {
            items(upcomingDebts, key = { it.id }) { debt ->
                DebtListCard(debt = debt, onClick = { onDebtClick(debt.id) })
            }
        }

        // Recent Payments Section
        item {
            Text("العمليات والدفعات الأخيرة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (recentPayments.isEmpty()) {
            item {
                FintechSurfaceCard {
                    Text(
                        text = "لم تُسجل دفعات بعد. عند تسجيل أي دفعة سيظهر سجل الإيصال هنا فوراً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentPayments, key = { it.id }) { p ->
                val debt = state.debts.find { it.id == p.debtId }
                val methodLabel = runCatching { PaymentMethod.valueOf(p.method).labelAr }.getOrDefault(p.method)
                FintechSurfaceCard(onClick = { onPaymentClick(p.id) }, contentPadding = PaddingValues(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(hc.positive.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = hc.positive, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(debt?.contactName ?: "دفعة مالية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "إيصال ${p.receiptNumber} • $methodLabel • ${ArabicReportExporter.formatDate(p.paidAtMillis)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            color = hc.positive,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showCurrencyPicker) {
        AlertDialog(
            onDismissRequest = { showCurrencyPicker = false },
            title = { Text("تغيير العملة الافتراضية (C)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.currencies.forEach { cur ->
                        val selected = cur.code == state.preferences.defaultCurrency
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCurrency(cur.code)
                                    showCurrencyPicker = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${cur.nameAr} (${cur.code} - ${cur.symbol})", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCurrencyPicker = false
                    onOpenSettings()
                }) { Text("إدارة العملات في الإعدادات") }
            },
            dismissButton = {
                TextButton(onClick = { showCurrencyPicker = false }) { Text("إغلاق") }
            }
        )
    }
}
