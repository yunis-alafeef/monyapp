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
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.InfoSlate
import com.example.ui.theme.PayableCoral
import com.example.ui.theme.WarningAmber
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
    onSyncNow: () -> Unit
) {
    val m = state.metrics
    val sym = m.defaultCurrencySymbol
    val now = System.currentTimeMillis()
    val upcomingDebts = state.debts
        .filter { it.remainingMinor > 0L && it.status != DebtStatus.PAID.name && it.status != DebtStatus.SETTLED.name }
        .sortedBy { it.dueAtMillis }
        .take(5)
    val recentPayments = state.payments.take(4)
    val unreadNotifications = state.notifications.count { !it.isRead }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مرحباً، ${userDisplayName.ifBlank { "بك" }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.preferences.workspaceName} (${state.preferences.accountType.labelAr})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSyncNow,
                        modifier = Modifier.testTag("dashboard_sync_button")
                    ) {
                        Icon(
                            imageVector = if (state.pendingOutboxCount > 0) Icons.Rounded.CloudUpload else Icons.Rounded.CloudDone,
                            contentDescription = "حالة المزامنة",
                            tint = if (state.pendingOutboxCount > 0) WarningAmber else EmeraldPrimary
                        )
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("dashboard_search_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = "بحث ذكي")
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("dashboard_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge { Text(unreadNotifications.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "الإشعارات")
                        }
                    }
                }
            }
        }

        // Luxury Hero Card: Net Financial Position + Receivable / Payable + Quick Actions
        item {
            FintechHeroBannerCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صافي المركز المالي",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = m.defaultCurrencyCode,
                            color = GoldAccent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = MoneyMath.formatSignedMinor(m.netPositionMinor, sym),
                    style = MaterialTheme.typography.displayLarge,
                    color = if (m.netPositionMinor >= 0) EmeraldBright else Color(0xFFFF8A9B),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مستحق لي", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = MoneyMath.formatMinor(m.totalReceivableMinor, sym),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = Color(0xFFFF8A9B), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("مستحق عليّ", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = MoneyMath.formatMinor(m.totalPayableMinor, sym),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddDebtClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_add_debt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldBright, contentColor = Color(0xFF062019))
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إضافة دين", fontWeight = FontWeight.Bold)
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
                            .height(48.dp)
                            .testTag("quick_record_payment_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تسجيل دفعة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // AI Smart Financial Assistant Banner
        item {
            FintechSurfaceCard(
                onClick = onOpenAiAssistant,
                modifier = Modifier.testTag("dashboard_ai_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldAccent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldAccent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("المساعد المالي الذكي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "اسأل عن المتأخرات، أكثر العملاء مديونية، أو ملخص هذا الشهر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // KPI Grid (Collected, Paid Out, Overdue, Due Soon, Contacts, Transactions)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryTile(
                    title = "إجمالي المحصّل",
                    value = MoneyMath.formatMinor(m.totalCollectedMinor, sym),
                    icon = Icons.Rounded.AccountBalanceWallet,
                    accentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryTile(
                    title = "إجمالي المسدّد",
                    value = MoneyMath.formatMinor(m.totalPaidOutMinor, sym),
                    icon = Icons.Rounded.Payments,
                    accentColor = InfoSlate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryTile(
                    title = "الديون المتأخرة",
                    value = MoneyMath.formatMinor(m.overdueAmountMinor, sym),
                    subtitle = "${m.overdueCount} دين",
                    icon = Icons.Rounded.WarningAmber,
                    accentColor = PayableCoral,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenOverdue
                )
                MetricSummaryTile(
                    title = "اقترب استحقاقها",
                    value = MoneyMath.formatMinor(m.dueSoonAmountMinor, sym),
                    subtitle = "${m.dueSoonCount} خلال 7 أيام",
                    icon = Icons.Rounded.Schedule,
                    accentColor = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    accentColor = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Charts
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
                Text("المستحقات القادمة والمتأخرة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenOverdue) { Text("عرض المتأخرة") }
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

        // Recent Payments / Operations Section
        item {
            Text("العمليات والدفعات الأخيرة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (recentPayments.isEmpty()) {
            item {
                FintechSurfaceCard {
                    Text(
                        text = "لم تُسجل دفعات بعد. عند تسجيل أي دفعة سيظهر سجل الإيصال هنا فوراً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentPayments, key = { it.id }) { p ->
                val debt = state.debts.find { it.id == p.debtId }
                val methodLabel = runCatching { PaymentMethod.valueOf(p.method).labelAr }.getOrDefault(p.method)
                FintechSurfaceCard(onClick = { onPaymentClick(p.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = EmeraldPrimary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(debt?.contactName ?: "دفعة مالية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "إيصال ${p.receiptNumber} • $methodLabel • ${ArabicReportExporter.formatDate(p.paidAtMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
