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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.DebtEntity
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.export.ArabicReportExporter
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyDeep
import com.example.ui.theme.NavyRich
import com.example.ui.theme.PayableCoral
import com.example.ui.theme.PayableSoft
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSoft
import com.example.ui.viewmodel.MonthlyChartPoint

@Composable
fun FintechHeroBannerCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_fintech),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.45f)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NavyDeep.copy(alpha = 0.94f),
                                NavyRich.copy(alpha = 0.88f),
                                EmeraldPrimary.copy(alpha = 0.50f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                content = content
            )
        }
    }
}

@Composable
fun FintechSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
    FintechSurfaceCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DebtStatusBadge(statusName: String) {
    val status = runCatching { DebtStatus.valueOf(statusName) }.getOrDefault(DebtStatus.OPEN)
    val (bg, fg) = when (status) {
        DebtStatus.PAID, DebtStatus.SETTLED -> EmeraldSoft to EmeraldPrimary
        DebtStatus.OVERDUE -> PayableSoft to PayableCoral
        DebtStatus.PARTIAL, DebtStatus.SCHEDULED -> WarningSoft to WarningAmber
        DebtStatus.OPEN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.primary
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DebtListCard(
    debt: DebtEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val direction = runCatching { DebtDirection.valueOf(debt.direction) }.getOrDefault(DebtDirection.RECEIVABLE)
    val dirColor = if (direction == DebtDirection.RECEIVABLE) EmeraldPrimary else PayableCoral
    val progress = if (debt.principalMinor > 0L) {
        ((debt.principalMinor - debt.remainingMinor).toFloat() / debt.principalMinor.toFloat()).coerceIn(0f, 1f)
    } else 0f

    FintechSurfaceCard(
        modifier = modifier.testTag("debt_card_${debt.id}"),
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
                    style = MaterialTheme.typography.titleMedium,
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
        Spacer(modifier = Modifier.height(12.dp))
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
                    style = MaterialTheme.typography.titleLarge,
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
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
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
    val maxVal = series.maxOf { maxOf(it.receivablesMinor, it.payablesMinor, it.collectedMinor) }.coerceAtLeast(1L)

    FintechSurfaceCard(modifier = modifier) {
        Text(
            text = "حركة الديون والتحصيل (آخر 6 أشهر)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot("مستحق لي", EmeraldBright)
            LegendDot("مستحق عليّ", PayableCoral)
            LegendDot("المحصل", GoldAccent)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val groupCount = series.size
            val groupWidth = size.width / groupCount
            val barWidth = (groupWidth * 0.22f).coerceAtLeast(8f)
            val chartBottom = size.height - 8f

            series.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f
                val recH = (pt.receivablesMinor.toFloat() / maxVal.toFloat()) * (chartBottom - 8f)
                val payH = (pt.payablesMinor.toFloat() / maxVal.toFloat()) * (chartBottom - 8f)
                val colH = (pt.collectedMinor.toFloat() / maxVal.toFloat()) * (chartBottom - 8f)

                drawRoundRect(
                    color = EmeraldBright,
                    topLeft = Offset(centerX - barWidth * 1.6f, chartBottom - recH),
                    size = Size(barWidth, recH.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = PayableCoral,
                    topLeft = Offset(centerX - barWidth * 0.5f, chartBottom - payH),
                    size = Size(barWidth, payH.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = GoldAccent,
                    topLeft = Offset(centerX + barWidth * 0.6f, chartBottom - colH),
                    size = Size(barWidth, colH.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(6f, 6f)
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
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
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
    val total = (receivableMinor + payableMinor).coerceAtLeast(1L)
    val recSweep = (receivableMinor.toFloat() / total.toFloat()) * 360f
    val animSweep by animateFloatAsState(
        targetValue = recSweep,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "donut_sweep"
    )

    FintechSurfaceCard(modifier = modifier) {
        Text(
            text = "مقارنة المستحقات والالتزامات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 18.dp.toPx()
                    drawArc(
                        color = PayableCoral.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    if (receivableMinor + payableMinor > 0L) {
                        drawArc(
                            color = PayableCoral,
                            startAngle = -90f + animSweep,
                            sweepAngle = 360f - animSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = EmeraldBright,
                            startAngle = -90f,
                            sweepAngle = animSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = if (receivableMinor + payableMinor == 0L) "0%" else "${((receivableMinor * 100) / total)}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    LegendDot("مستحقة لي", EmeraldBright)
                    Text(
                        text = MoneyMath.formatMinor(receivableMinor, symbol),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    LegendDot("مستحقة عليّ", PayableCoral)
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
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun PaymentSuccessCelebrationDialog(
    receiptNumber: String,
    amountFormatted: String,
    remainingFormatted: String,
    onViewReceipt: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "نجاح العملية",
                        tint = EmeraldBright,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "تم تسجيل الدفعة بنجاح",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "إيصال رقم: $receiptNumber",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = amountFormatted,
                        style = MaterialTheme.typography.displayMedium,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الرصيد المتبقي: $remainingFormatted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onViewReceipt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("view_receipt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Rounded.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("عرض وطباعة الإيصال")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
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
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(NavyDeep),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "قفل التطبيق",
                    tint = EmeraldBright,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "تطبيق حساباتي محمي برمز PIN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "أدخل رمز الحماية لفتح بياناتك المالية",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
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
                Spacer(Modifier.height(8.dp))
                Text(errorMsg!!, color = PayableCoral, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
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
                    .height(50.dp)
                    .testTag("pin_unlock_submit")
            ) {
                Text("فتح التطبيق")
            }
            if (biometricAvailable) {
                Spacer(Modifier.height(10.dp))
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
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "عمل انما سوفت",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = {
                        com.example.messaging.ExternalMessagingHelper.dialPhone(context, "772317950")
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = EmeraldPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .testTag("anma_soft_footer_phone")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "الاتصال بالرقم 772317950",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "772317950",
                            style = MaterialTheme.typography.labelLarge.copy(
                                textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                            ),
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}

