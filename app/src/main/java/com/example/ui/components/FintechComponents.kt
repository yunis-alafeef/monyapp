package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.DebtEntity
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.export.ArabicReportExporter
import com.example.messaging.ExternalMessagingHelper
import com.example.ui.theme.HisabatiTheme
import com.example.ui.viewmodel.MonthlyChartPoint

@Composable
fun FintechHeroBannerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val hc = HisabatiTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_fintech),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.32f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                hc.heroStart.copy(alpha = 0.96f),
                                hc.heroMid.copy(alpha = 0.90f),
                                hc.heroEnd.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                content = content
            )
        }
    }
}

@Composable
fun FintechSurfaceCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun MetricSummaryTile(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    FintechSurfaceCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 10.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DebtStatusBadge(statusName: String) {
    val hc = HisabatiTheme.colors
    val status = runCatching { DebtStatus.valueOf(statusName) }.getOrDefault(DebtStatus.OPEN)
    val (bg, fg) = when (status) {
        DebtStatus.PAID, DebtStatus.SETTLED -> hc.positive.copy(alpha = 0.14f) to hc.positive
        DebtStatus.OVERDUE -> hc.negative.copy(alpha = 0.14f) to hc.negative
        DebtStatus.PARTIAL, DebtStatus.SCHEDULED -> hc.warning.copy(alpha = 0.16f) to hc.warning
        DebtStatus.OPEN -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = status.labelAr,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DebtListCard(
    debt: DebtEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hc = HisabatiTheme.colors
    val direction = runCatching { DebtDirection.valueOf(debt.direction) }.getOrDefault(DebtDirection.RECEIVABLE)
    val dirColor = if (direction == DebtDirection.RECEIVABLE) hc.positive else hc.negative
    val progress = if (debt.principalMinor > 0L) {
        ((debt.principalMinor - debt.remainingMinor).toFloat() / debt.principalMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    FintechSurfaceCard(
        modifier = modifier.testTag("debt_card_${debt.id}"),
        contentPadding = PaddingValues(11.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.contactName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = debt.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            DebtStatusBadge(debt.status)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "المتبقي",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = MoneyMath.formatMinor(debt.remainingMinor, currencyCode = debt.currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = dirColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = direction.labelAr,
                    style = MaterialTheme.typography.labelSmall,
                    color = dirColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "الاستحقاق: ${ArabicReportExporter.formatDate(debt.dueAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(dirColor)
            )
        }
    }
}

@Composable
fun MonthlyCashflowChart(
    series: List<MonthlyChartPoint>,
    modifier: Modifier = Modifier
) {
    if (series.isEmpty()) return
    val hc = HisabatiTheme.colors
    val maxVal = series.maxOf { maxOf(it.receivablesMinor, it.payablesMinor, it.collectedMinor) }.coerceAtLeast(1L)

    FintechSurfaceCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(
            text = "حركة الديون والتحصيل (آخر 6 أشهر)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot("مستحق لي", hc.positive)
            LegendDot("مستحق عليّ", hc.negative)
            LegendDot("المحصل", hc.accent)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
        ) {
            val groupCount = series.size
            val groupWidth = size.width / groupCount
            val barWidth = (groupWidth * 0.20f).coerceAtLeast(7f)
            val chartBottom = size.height - 6f

            series.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val recH = (pt.receivablesMinor.toFloat() / maxVal.toFloat()) * (chartBottom - 6f)
                val payH = (pt.payablesMinor.toFloat() / maxVal.toFloat()) * (chartBottom - 6f)
                val colH = (pt.collectedMinor.toFloat() / maxVal.toFloat()) * (chartBottom - 6f)

                drawRoundRect(
                    color = hc.positive,
                    topLeft = Offset(centerX - barWidth * 1.55f, chartBottom - recH),
                    size = Size(barWidth, recH.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                drawRoundRect(
                    color = hc.negative,
                    topLeft = Offset(centerX - barWidth * 0.45f, chartBottom - payH),
                    size = Size(barWidth, payH.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                drawRoundRect(
                    color = hc.accent,
                    topLeft = Offset(centerX + barWidth * 0.65f, chartBottom - colH),
                    size = Size(barWidth, colH.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            series.forEach { pt ->
                Text(
                    text = pt.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReceivablePayableComparisonCard(
    receivableMinor: Long,
    payableMinor: Long,
    symbol: String,
    modifier: Modifier = Modifier
) {
    val hc = HisabatiTheme.colors
    val total = (receivableMinor + payableMinor).coerceAtLeast(1L)
    val recSweep = (receivableMinor.toFloat() / total.toFloat()) * 360f
    val animSweep by animateFloatAsState(
        targetValue = recSweep,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "donut_sweep"
    )

    FintechSurfaceCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(
            text = "مقارنة المستحقات والالتزامات",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(82.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 13.dp.toPx()
                    drawArc(
                        color = hc.negative.copy(alpha = 0.22f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    if (receivableMinor + payableMinor > 0L) {
                        drawArc(
                            color = hc.negative,
                            startAngle = -90f + animSweep,
                            sweepAngle = 360f - animSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = hc.positive,
                            startAngle = -90f,
                            sweepAngle = animSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = if (receivableMinor + payableMinor == 0L) "0%" else "${((receivableMinor * 100) / total)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = hc.positive
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    LegendDot("مستحقة لي", hc.positive)
                    Text(
                        text = MoneyMath.formatMinor(receivableMinor, symbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    LegendDot("مستحقة عليّ", hc.negative)
                    Text(
                        text = MoneyMath.formatMinor(payableMinor, symbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    FintechSurfaceCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun OperationNotificationVoucherDialog(
    title: String,
    voucherNumber: String,
    partyName: String,
    partyPhone: String,
    directionLabel: String,
    amountFormatted: String,
    remainingFormatted: String? = null,
    dateLabel: String,
    reason: String? = null,
    whatsappMessage: String,
    onSharePdf: (() -> Unit)? = null,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val hc = HisabatiTheme.colors

    Dialog(onDismissRequest = onDone) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, hc.positive.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(hc.positive.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "نجاح الحفظ",
                            tint = hc.positive,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "رقم السند: $voucherNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            VoucherInfoRow("الطرف", partyName)
                            VoucherInfoRow("نوع العملية", directionLabel)
                            VoucherInfoRow("المبلغ", amountFormatted, valueColor = hc.positive)
                            if (!remainingFormatted.isNullOrBlank()) {
                                VoucherInfoRow("الرصيد المتبقي", remainingFormatted)
                            }
                            VoucherInfoRow("التاريخ", dateLabel)
                            if (!reason.isNullOrBlank()) {
                                VoucherInfoRow("البيان", reason)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (partyPhone.isNotBlank()) {
                                ExternalMessagingHelper.openWhatsAppChat(context, partyPhone, whatsappMessage)
                            } else {
                                ExternalMessagingHelper.openWhatsAppShareText(context, whatsappMessage)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("voucher_send_whatsapp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = hc.positive)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إرسال سند الإشعار عبر واتساب", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (partyPhone.isNotBlank()) {
                                    ExternalMessagingHelper.openSmsComposer(context, partyPhone, whatsappMessage)
                                } else {
                                    ExternalMessagingHelper.openWhatsAppShareText(context, whatsappMessage)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SMS / مشاركة", maxLines = 1)
                        }
                        if (onSharePdf != null) {
                            OutlinedButton(
                                onClick = onSharePdf,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("سند PDF", maxLines = 1)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voucher_done_button")
                    ) {
                        Text("تم والعودة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoucherInfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PaymentSuccessCelebrationDialog(
    receiptNumber: String,
    amountFormatted: String,
    remainingFormatted: String,
    onSendWhatsApp: (() -> Unit)? = null,
    onViewReceipt: () -> Unit,
    onDismiss: () -> Unit
) {
    val hc = HisabatiTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, hc.positive.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "نجاح العملية",
                        tint = hc.positive,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "تم تسجيل الدفعة بنجاح",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "إيصال رقم: $receiptNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = amountFormatted,
                        style = MaterialTheme.typography.headlineLarge,
                        color = hc.positive,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الرصيد المتبقي: $remainingFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (onSendWhatsApp != null) {
                        Button(
                            onClick = onSendWhatsApp,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = hc.positive)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إرسال سند الإشعار عبر واتساب")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    OutlinedButton(
                        onClick = onViewReceipt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("view_receipt_button")
                    ) {
                        Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("عرض وطباعة الإيصال")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إغلاق")
                    }
                }
            }
        }
    }
}

@Composable
fun PinUnlockScreen(
    biometricAvailable: Boolean,
    onVerifyPin: (String) -> Boolean,
    onBiometricClick: () -> Unit,
    onUnlocked: () -> Unit
) {
    val hc = HisabatiTheme.colors
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(hc.heroStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "قفل التطبيق",
                    tint = hc.positive,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "تطبيق حساباتي محمي برمز PIN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "أدخل رمز الحماية لفتح بياناتك المالية",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                label = { Text("رمز PIN (4-6 أرقام)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_unlock_input")
            )
            if (errorMsg != null) {
                Spacer(Modifier.height(6.dp))
                Text(errorMsg!!, color = hc.negative, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    if (onVerifyPin(pinInput)) {
                        onUnlocked()
                    } else {
                        errorMsg = "رمز PIN غير صحيح"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pin_unlock_submit")
            ) {
                Text("فتح التطبيق")
            }
            if (biometricAvailable) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onBiometricClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("فتح بالبصمة الحيوية")
                }
            }
        }
    }
}

@Composable
fun AnmaSoftFooterBar(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hc = HisabatiTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 0.5.dp, color = hc.dividerSubtle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 26.dp)
                    .padding(horizontal = 14.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = hc.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "عمل انما سوفت",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    onClick = {
                        ExternalMessagingHelper.dialPhone(context, "772317950")
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.testTag("anma_soft_footer_phone")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "الاتصال بالرقم 772317950",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "772317950",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                textDirection = TextDirection.Ltr
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
