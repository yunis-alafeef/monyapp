package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.example.R
import com.example.data.local.ContactEntity
import com.example.data.local.DebtEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.UserAppPreferences
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StatementPdfLine(
    val dateLabel: String,
    val title: String,
    val amountLabel: String,
    val statusOrMethod: String
)

object ArabicReportExporter {
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun resolveColorPair(prefs: UserAppPreferences, docKind: PdfDocumentKind): Pair<Int, Int> {
        val primaryHex = when (docKind) {
            PdfDocumentKind.DEBT_VOUCHER -> prefs.printDebtColorHex
            PdfDocumentKind.PAYMENT_RECEIPT -> prefs.printReceiptColorHex
            PdfDocumentKind.STATEMENT -> prefs.printStatementColorHex
            PdfDocumentKind.GENERAL_REPORT -> prefs.printReportColorHex
        }
        val swatch = PdfInvoiceColorSwatches.find { it.primaryHex == primaryHex }
        val primary = primaryHex.toInt()
        val accent = swatch?.accentHex?.toInt() ?: ColorUtils.blendARGB(primary, Color.parseColor("#D4AF37"), 0.55f)
        return primary to accent
    }

    fun saveLogoFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            val brandingDir = File(context.filesDir, "branding").apply { mkdirs() }
            val destFile = File(brandingDir, "org_logo.png")
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return null

            val maxDim = 512
            val scale = minOf(1f, maxDim.toFloat() / maxOf(rawBitmap.width, rawBitmap.height).coerceAtLeast(1))
            val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
            val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true).also {
                    if (it != rawBitmap) rawBitmap.recycle()
                }
            } else {
                rawBitmap
            }
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            finalBitmap.recycle()
            destFile.absolutePath
        }.getOrNull()
    }

    fun clearSavedLogo(path: String) {
        if (path.isNotBlank()) {
            runCatching { File(path).delete() }
        }
    }

    fun generateArabicPdfReport(
        context: Context,
        fileNamePrefix: String,
        reportTitle: String,
        subtitle: String,
        summaryLines: List<Pair<String, String>>,
        rows: List<StatementPdfLine>,
        prefs: UserAppPreferences = UserAppPreferences(),
        docKind: PdfDocumentKind = PdfDocumentKind.GENERAL_REPORT
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "${fileNamePrefix}_${System.currentTimeMillis()}.pdf")

        val arabicTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic)
        }.getOrNull() ?: Typeface.DEFAULT

        val template = runCatching {
            PdfInvoiceTemplate.valueOf(prefs.printTemplate)
        }.getOrDefault(PdfInvoiceTemplate.CLASSIC_FORMAL)

        val (primaryColor, accentColor) = resolveColorPair(prefs, docKind)
        val tintBgColor = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.93f)
        val borderTintColor = ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.72f)
        val darkTextColor = Color.parseColor("#0F172A")
        val mutedTextColor = Color.parseColor("#475569")

        val logoBitmap = if (prefs.printShowLogoInHeader && prefs.printLogoPath.isNotBlank()) {
            runCatching {
                val logoFile = File(prefs.printLogoPath)
                if (logoFile.exists()) BitmapFactory.decodeFile(logoFile.absolutePath) else null
            }.getOrNull()
        } else null

        val companyName = prefs.printCompanyName.ifBlank { prefs.workspaceName }.ifBlank { "حساباتي لإدارة الديون" }
        val issuedDate = formatDate(System.currentTimeMillis())

        val pdf = PdfDocument()
        try {
            val pageWidth = 595 // A4 width at 72dpi
            val pageHeight = 842 // A4 height at 72dpi
            val margin = 34f
            val contentWidth = (pageWidth - margin * 2)
            val footerTopY = pageHeight - 52f

            var pageNum = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = borderTintColor
                strokeWidth = 0.9f
                style = Paint.Style.STROKE
            }
            val rowAltPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tintBgColor }

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkTextColor
                textSize = 15.5f
                typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = darkTextColor
                textSize = 10.2f
                typeface = arabicTypeface
            }
            val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primaryColor
                textSize = 10.6f
                typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
            }
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedTextColor
                textSize = 9.2f
                typeface = arabicTypeface
            }

            fun drawPageChromeIfNeeded(c: Canvas) {
                if (template == PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER) {
                    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = primaryColor
                        strokeWidth = 2.0f
                        style = Paint.Style.STROKE
                    }
                    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = accentColor
                        strokeWidth = 0.8f
                        style = Paint.Style.STROKE
                    }
                    c.drawRoundRect(16f, 16f, pageWidth - 16f, pageHeight - 16f, 8f, 8f, outer)
                    c.drawRoundRect(20f, 20f, pageWidth - 20f, pageHeight - 20f, 6f, 6f, inner)
                }
                if (prefs.printShowWatermarkBadge) {
                    c.save()
                    c.rotate(-28f, pageWidth / 2f, pageHeight / 2f)
                    val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ColorUtils.setAlphaComponent(primaryColor, 14)
                        textSize = 42f
                        typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    c.drawText("حساباتي • $companyName", pageWidth / 2f, pageHeight / 2f, wmPaint)
                    c.restore()
                }
            }

            fun drawFooter(c: Canvas, currentPage: Int) {
                val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = accentColor
                    strokeWidth = 1.2f
                }
                c.drawLine(margin, footerTopY, pageWidth - margin, footerTopY, divider)

                val footerRightPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedTextColor
                    textSize = 8.6f
                    typeface = arabicTypeface
                }
                val footerLeftPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textSize = 8.8f
                    typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                }
                val footerCenterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedTextColor
                    textSize = 8.6f
                    typeface = arabicTypeface
                    textAlign = Paint.Align.CENTER
                }

                // Right column (RTL): custom org footer note
                val footerSideW = 195
                drawAlignedParagraph(
                    c,
                    prefs.printFooterNote,
                    footerRightPaint,
                    pageWidth - margin - footerSideW,
                    footerTopY + 8f,
                    footerSideW,
                    Layout.Alignment.ALIGN_NORMAL,
                    TextDirectionHeuristics.RTL
                )

                // Center page badge
                c.drawText("صفحة $currentPage", pageWidth / 2f, footerTopY + 22f, footerCenterPaint)

                // Left column: Inma Soft developer info (RTL BiDi anchored left)
                if (prefs.printShowInmaSoftInFooter) {
                    drawAlignedParagraph(
                        c,
                        "عمل انما سوفت (Inma Soft) • هاتف: 772317950",
                        footerLeftPaint,
                        margin,
                        footerTopY + 8f,
                        footerSideW,
                        Layout.Alignment.ALIGN_OPPOSITE,
                        TextDirectionHeuristics.RTL
                    )
                }
            }

            fun drawFullHeader(c: Canvas): Float {
                val headerTop = if (template == PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER) 26f else 0f
                val headerBottom = headerTop + 120f
                val headerLeft = if (template == PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER) 26f else 0f
                val headerRight = if (template == PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER) pageWidth - 26f else pageWidth.toFloat()

                val lightHeaderText = template == PdfInvoiceTemplate.CLASSIC_FORMAL || template == PdfInvoiceTemplate.MODERN_LUXURY
                when (template) {
                    PdfInvoiceTemplate.CLASSIC_FORMAL -> {
                        val bg = Paint().apply { color = primaryColor }
                        val bar = Paint().apply { color = accentColor }
                        c.drawRect(headerLeft, headerTop, headerRight, headerBottom, bg)
                        c.drawRect(headerLeft, headerBottom - 4f, headerRight, headerBottom, bar)
                    }
                    PdfInvoiceTemplate.MODERN_LUXURY -> {
                        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = LinearGradient(
                                headerLeft,
                                headerTop,
                                headerRight,
                                headerBottom,
                                primaryColor,
                                ColorUtils.blendARGB(primaryColor, accentColor, 0.35f),
                                Shader.TileMode.CLAMP
                            )
                        }
                        val rect = RectF(18f, 16f, pageWidth - 18f, headerBottom)
                        c.drawRoundRect(rect, 14f, 14f, gradPaint)
                    }
                    PdfInvoiceTemplate.DOUBLE_FRAME_VOUCHER -> {
                        val rect = RectF(headerLeft, headerTop, headerRight, headerBottom)
                        c.drawRoundRect(rect, 8f, 8f, tintPaint)
                        val rule = Paint().apply { color = accentColor; strokeWidth = 1.8f }
                        c.drawLine(headerLeft, headerBottom, headerRight, headerBottom, rule)
                    }
                    PdfInvoiceTemplate.MINIMAL_EXECUTIVE -> {
                        val sideStripe = Paint().apply { color = primaryColor }
                        c.drawRect(pageWidth - 12f, 20f, pageWidth - 6f, headerBottom - 6f, sideStripe)
                        val rule = Paint().apply { color = primaryColor; strokeWidth = 1.6f }
                        val accentRule = Paint().apply { color = accentColor; strokeWidth = 0.9f }
                        c.drawLine(margin, headerBottom - 4f, pageWidth - margin, headerBottom - 4f, rule)
                        c.drawLine(margin, headerBottom, pageWidth - margin, headerBottom, accentRule)
                    }
                }

                val hPrimaryColor = if (lightHeaderText) Color.WHITE else primaryColor
                val hSecondaryColor = if (lightHeaderText) Color.parseColor("#E2E8F0") else mutedTextColor
                val hAccentColor = if (lightHeaderText) ColorUtils.blendARGB(accentColor, Color.WHITE, 0.35f) else accentColor

                val orgTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = hPrimaryColor
                    textSize = 13.5f
                    typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                }
                val hMetaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = hSecondaryColor
                    textSize = 8.8f
                    typeface = arabicTypeface
                }
                val devTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = hAccentColor
                    textSize = 9.8f
                    typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                }

                // Right Column (RTL): Organization Information
                val rightColW = 190
                val rightColX = pageWidth - margin - rightColW
                var rightY = headerTop + 24f
                rightY += drawAlignedParagraph(
                    c, companyName, orgTitlePaint, rightColX, rightY, rightColW,
                    Layout.Alignment.ALIGN_NORMAL, TextDirectionHeuristics.RTL
                ) + 4f
                val orgDetails = buildList {
                    if (prefs.printCompanyPhone.isNotBlank()) add("هاتف: ${prefs.printCompanyPhone}")
                    if (prefs.printCompanyAddress.isNotBlank()) add("العنوان: ${prefs.printCompanyAddress}")
                    if (prefs.printTaxOrCrNumber.isNotBlank()) add("السجل/الضريبي: ${prefs.printTaxOrCrNumber}")
                    if (isEmpty()) add("تطبيق حساباتي للإدارة المالية")
                }.joinToString("\n")
                drawAlignedParagraph(
                    c, orgDetails, hMetaPaint, rightColX, rightY, rightColW,
                    Layout.Alignment.ALIGN_NORMAL, TextDirectionHeuristics.RTL
                )

                // Center Column: Uploaded Organization Logo (or styled monogram emblem)
                if (prefs.printShowLogoInHeader) {
                    val cx = pageWidth / 2f
                    val cy = headerTop + 58f
                    val radius = 29f
                    val whiteBacking = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = accentColor
                        strokeWidth = 2.2f
                        style = Paint.Style.STROKE
                    }
                    c.drawCircle(cx, cy, radius + 2f, whiteBacking)
                    if (logoBitmap != null) {
                        val destRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
                        c.save()
                        val clipPath = Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) }
                        c.clipPath(clipPath)
                        c.drawBitmap(logoBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        c.restore()
                    } else {
                        val crestFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tintBgColor }
                        c.drawCircle(cx, cy, radius, crestFill)
                        val monogramPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = primaryColor
                            textSize = 22f
                            typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                            textAlign = Paint.Align.CENTER
                        }
                        val fm = monogramPaint.fontMetrics
                        val textY = cy - (fm.ascent + fm.descent) / 2f
                        c.drawText(companyName.take(1).ifBlank { "ح" }, cx, textY, monogramPaint)
                    }
                    c.drawCircle(cx, cy, radius + 2f, ring)
                }

                // Left Column: Inma Soft Info & Document Metadata
                val leftColW = 182
                var leftY = headerTop + 24f
                if (prefs.printShowInmaSoftInHeader) {
                    leftY += drawAlignedParagraph(
                        c,
                        "انما سوفت | Inma Soft",
                        devTitlePaint,
                        margin,
                        leftY,
                        leftColW,
                        Layout.Alignment.ALIGN_OPPOSITE,
                        TextDirectionHeuristics.RTL
                    ) + 2f
                    leftY += drawAlignedParagraph(
                        c,
                        "للتطوير والحلول الذكية • 772317950",
                        hMetaPaint,
                        margin,
                        leftY,
                        leftColW,
                        Layout.Alignment.ALIGN_OPPOSITE,
                        TextDirectionHeuristics.RTL
                    ) + 6f
                }
                drawAlignedParagraph(
                    c,
                    "${docKind.titleAr}\nتاريخ الإصدار: $issuedDate",
                    hMetaPaint,
                    margin,
                    leftY,
                    leftColW,
                    Layout.Alignment.ALIGN_OPPOSITE,
                    TextDirectionHeuristics.RTL
                )

                return headerBottom + 16f
            }

            fun startNewPage(): Float {
                drawFooter(canvas, pageNum)
                pdf.finishPage(page)
                pageNum++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                drawPageChromeIfNeeded(canvas)
                // Compact continuation header
                val contPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = primaryColor
                    textSize = 10f
                    typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                }
                drawAlignedParagraph(
                    canvas,
                    "$companyName — $reportTitle (تابع)",
                    contPaint,
                    margin,
                    28f,
                    contentWidth.toInt(),
                    Layout.Alignment.ALIGN_NORMAL,
                    TextDirectionHeuristics.RTL
                )
                val lineP = Paint().apply { color = accentColor; strokeWidth = 1f }
                canvas.drawLine(margin, 48f, pageWidth - margin, 48f, lineP)
                return 58f
            }

            drawPageChromeIfNeeded(canvas)
            var y = drawFullHeader(canvas)

            // Document Title Banner
            val bannerRect = RectF(margin, y, pageWidth - margin, y + 46f)
            if (template == PdfInvoiceTemplate.MINIMAL_EXECUTIVE) {
                val accentSide = Paint().apply { color = primaryColor }
                canvas.drawRect(pageWidth - margin - 4f, y, pageWidth - margin, y + 44f, accentSide)
            } else {
                canvas.drawRoundRect(bannerRect, 8f, 8f, tintPaint)
                canvas.drawRoundRect(bannerRect, 8f, 8f, borderPaint)
            }
            drawAlignedParagraph(
                canvas,
                reportTitle,
                titlePaint.apply { color = primaryColor },
                margin + 12f,
                y + 7f,
                (contentWidth - 24f).toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                TextDirectionHeuristics.RTL
            )
            drawAlignedParagraph(
                canvas,
                subtitle,
                labelPaint,
                margin + 12f,
                y + 28f,
                (contentWidth - 24f).toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                TextDirectionHeuristics.RTL
            )
            y += 58f

            // Two-column RTL Summary Grid
            if (summaryLines.isNotEmpty()) {
                val gap = 10f
                val cellWidth = (contentWidth - gap) / 2f
                val chunked = summaryLines.chunked(2)
                for (pairRow in chunked) {
                    val rightItem = pairRow[0]
                    val leftItem = pairRow.getOrNull(1)

                    val rLayout = buildRtlStaticLayout("${rightItem.first}: ${rightItem.second}", boldBodyPaint, (cellWidth - 16f).toInt())
                    val lLayout = leftItem?.let {
                        buildRtlStaticLayout("${it.first}: ${it.second}", boldBodyPaint, (cellWidth - 16f).toInt())
                    }
                    val rowH = maxOf(rLayout.height, lLayout?.height ?: 0) + 14f
                    if (y + rowH > footerTopY - 24f) {
                        y = startNewPage()
                    }
                    // Right cell (RTL first item)
                    val rightRect = RectF(margin + cellWidth + gap, y, pageWidth - margin, y + rowH)
                    canvas.drawRoundRect(rightRect, 6f, 6f, if (template == PdfInvoiceTemplate.MINIMAL_EXECUTIVE) rowAltPaint else tintPaint)
                    canvas.drawRoundRect(rightRect, 6f, 6f, borderPaint)
                    canvas.save()
                    canvas.translate(rightRect.left + 8f, y + 7f)
                    rLayout.draw(canvas)
                    canvas.restore()

                    if (lLayout != null) {
                        val leftRect = RectF(margin, y, margin + cellWidth, y + rowH)
                        canvas.drawRoundRect(leftRect, 6f, 6f, if (template == PdfInvoiceTemplate.MINIMAL_EXECUTIVE) rowAltPaint else tintPaint)
                        canvas.drawRoundRect(leftRect, 6f, 6f, borderPaint)
                        canvas.save()
                        canvas.translate(leftRect.left + 8f, y + 7f)
                        lLayout.draw(canvas)
                        canvas.restore()
                    }
                    y += rowH + 7f
                }
                y += 8f
            }

            // Section Title + Table Header
            y += drawAlignedParagraph(
                canvas,
                "تفاصيل البنود والحركات (${rows.size})",
                boldBodyPaint,
                margin,
                y,
                contentWidth.toInt(),
                Layout.Alignment.ALIGN_NORMAL,
                TextDirectionHeuristics.RTL
            ) + 6f

            // RTL Columns from Left to Right: [Status (95)] [Amount (115)] [Title (content - 285)] [Date (75)]
            val colStatusW = 92f
            val colAmountW = 115f
            val colDateW = 74f
            val colTitleW = contentWidth - colStatusW - colAmountW - colDateW

            fun drawTableHeader(targetCanvas: Canvas, topY: Float): Float {
                val thHeight = 26f
                val thRect = RectF(margin, topY, pageWidth - margin, topY + thHeight)
                if (template == PdfInvoiceTemplate.MINIMAL_EXECUTIVE) {
                    val lineP = Paint().apply { color = primaryColor; strokeWidth = 1.4f }
                    targetCanvas.drawRect(thRect, rowAltPaint)
                    targetCanvas.drawLine(margin, topY + thHeight, pageWidth - margin, topY + thHeight, lineP)
                } else {
                    val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryColor }
                    targetCanvas.drawRoundRect(thRect, 5f, 5f, thPaint)
                }
                val thTextColor = if (template == PdfInvoiceTemplate.MINIMAL_EXECUTIVE) primaryColor else Color.WHITE
                val thPaintText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = thTextColor
                    textSize = 9.6f
                    typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
                }
                var cx = margin
                drawAlignedParagraph(targetCanvas, "الحالة", thPaintText, cx + 4f, topY + 6f, (colStatusW - 8f).toInt(), Layout.Alignment.ALIGN_CENTER, TextDirectionHeuristics.RTL)
                cx += colStatusW
                drawAlignedParagraph(targetCanvas, "المبلغ", thPaintText, cx + 4f, topY + 6f, (colAmountW - 8f).toInt(), Layout.Alignment.ALIGN_CENTER, TextDirectionHeuristics.RTL)
                cx += colAmountW
                drawAlignedParagraph(targetCanvas, "البيان / التفاصيل", thPaintText, cx + 6f, topY + 6f, (colTitleW - 12f).toInt(), Layout.Alignment.ALIGN_NORMAL, TextDirectionHeuristics.RTL)
                cx += colTitleW
                drawAlignedParagraph(targetCanvas, "التاريخ", thPaintText, cx + 4f, topY + 6f, (colDateW - 8f).toInt(), Layout.Alignment.ALIGN_CENTER, TextDirectionHeuristics.RTL)
                return topY + thHeight
            }

            if (y + 56f > footerTopY) {
                y = startNewPage()
            }
            y = drawTableHeader(canvas, y)

            rows.forEachIndexed { index, row ->
                val statusLayout = buildRtlStaticLayout(row.statusOrMethod, bodyPaint, (colStatusW - 8f).toInt(), Layout.Alignment.ALIGN_CENTER)
                val amountLayout = buildRtlStaticLayout(row.amountLabel, boldBodyPaint, (colAmountW - 8f).toInt(), Layout.Alignment.ALIGN_CENTER)
                val titleLayout = buildRtlStaticLayout(row.title, bodyPaint, (colTitleW - 12f).toInt(), Layout.Alignment.ALIGN_NORMAL)
                val dateLayout = buildRtlStaticLayout(row.dateLabel, bodyPaint, (colDateW - 8f).toInt(), Layout.Alignment.ALIGN_CENTER)

                val rowH = maxOf(statusLayout.height, amountLayout.height, titleLayout.height, dateLayout.height) + 12f
                if (y + rowH > footerTopY - 12f) {
                    y = startNewPage()
                    y = drawTableHeader(canvas, y)
                }

                if (index % 2 == 0) {
                    canvas.drawRect(margin, y, pageWidth - margin, y + rowH, rowAltPaint)
                }

                var cx = margin
                drawLayoutAt(canvas, statusLayout, cx + 4f, y + 6f)
                cx += colStatusW
                drawLayoutAt(canvas, amountLayout, cx + 4f, y + 6f)
                cx += colAmountW
                drawLayoutAt(canvas, titleLayout, cx + 6f, y + 6f)
                cx += colTitleW
                drawLayoutAt(canvas, dateLayout, cx + 4f, y + 6f)

                canvas.drawLine(margin, y + rowH, pageWidth - margin, y + rowH, borderPaint)
                y += rowH
            }

            // Optional Signature & Official Stamp Boxes
            if (prefs.printShowSignatureBox) {
                if (y + 76f > footerTopY) {
                    y = startNewPage()
                }
                y += 16f
                val boxW = (contentWidth - 20f) / 2f
                val boxH = 54f
                val rightBox = RectF(pageWidth - margin - boxW, y, pageWidth - margin, y + boxH)
                val leftBox = RectF(margin, y, margin + boxW, y + boxH)

                canvas.drawRoundRect(rightBox, 6f, 6f, borderPaint)
                canvas.drawRoundRect(leftBox, 6f, 6f, borderPaint)
                drawAlignedParagraph(
                    canvas,
                    "توقيع المحاسب / ختم $companyName",
                    labelPaint,
                    rightBox.left + 8f,
                    y + 7f,
                    (boxW - 16f).toInt(),
                    Layout.Alignment.ALIGN_CENTER,
                    TextDirectionHeuristics.RTL
                )
                drawAlignedParagraph(
                    canvas,
                    "توقيع العميل / المستلم",
                    labelPaint,
                    leftBox.left + 8f,
                    y + 7f,
                    (boxW - 16f).toInt(),
                    Layout.Alignment.ALIGN_CENTER,
                    TextDirectionHeuristics.RTL
                )
            }

            drawFooter(canvas, pageNum)
            pdf.finishPage(page)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            return file
        } finally {
            runCatching { logoBitmap?.recycle() }
            runCatching { pdf.close() }
        }
    }

    private fun buildRtlStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        val safeText = text.ifEmpty { "-" }
        return StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width.coerceAtLeast(40))
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(1.5f, 1.12f)
            .setIncludePad(false)
            .build()
    }

    private fun drawLayoutAt(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawAlignedParagraph(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
        alignment: Layout.Alignment,
        direction: android.text.TextDirectionHeuristic
    ): Float {
        val safeText = text.ifEmpty { " " }
        val layout = StaticLayout.Builder.obtain(safeText, 0, safeText.length, paint, width.coerceAtLeast(40))
            .setAlignment(alignment)
            .setTextDirection(direction)
            .setLineSpacing(1.5f, 1.12f)
            .setIncludePad(false)
            .build()
        drawLayoutAt(canvas, layout, x, y)
        return layout.height.toFloat()
    }

    fun generateContactStatementPdf(
        context: Context,
        contact: ContactEntity,
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>,
        prefs: UserAppPreferences = UserAppPreferences()
    ): File {
        val groupedTotals = debts.groupBy { it.currencyCode }.map { (cur, list) ->
            val rec = list.filter { it.direction == DebtDirection.RECEIVABLE.name }.sumOf { it.remainingMinor }
            val pay = list.filter { it.direction == DebtDirection.PAYABLE.name }.sumOf { it.remainingMinor }
            "عملة $cur" to "له: ${MoneyMath.formatMinor(rec, currencyCode = cur)} | عليه: ${MoneyMath.formatMinor(pay, currencyCode = cur)}"
        }
        val rows = debts.map { d ->
            val dir = runCatching { DebtDirection.valueOf(d.direction).labelAr }.getOrDefault(d.direction)
            val st = runCatching { DebtStatus.valueOf(d.status).labelAr }.getOrDefault(d.status)
            StatementPdfLine(
                dateLabel = formatDate(d.recordedAtMillis),
                title = "$dir: ${d.reason}",
                amountLabel = "أصل ${MoneyMath.formatMinor(d.principalMinor, currencyCode = d.currencyCode)}\nمتبقي ${MoneyMath.formatMinor(d.remainingMinor, currencyCode = d.currencyCode)}",
                statusOrMethod = st
            )
        } + payments.map { p ->
            val method = runCatching { PaymentMethod.valueOf(p.method).labelAr }.getOrDefault(p.method)
            StatementPdfLine(
                dateLabel = formatDate(p.paidAtMillis),
                title = "دفعة إيصال #${p.receiptNumber}",
                amountLabel = MoneyMath.formatMinor(p.amountMinor, currencyCode = p.currencyCode),
                statusOrMethod = method
            )
        }
        return generateArabicPdfReport(
            context = context,
            fileNamePrefix = "statement_${contact.id}",
            reportTitle = "كشف حساب تفصيلي: ${contact.fullName}",
            subtitle = "الهاتف: ${contact.phone.ifBlank { "غير مسجل" }}",
            summaryLines = listOf(
                "الاسم" to contact.fullName,
                "الهاتف" to contact.phone.ifBlank { "غير مسجل" }
            ) + groupedTotals,
            rows = rows,
            prefs = prefs,
            docKind = PdfDocumentKind.STATEMENT
        )
    }

    fun generateCsvReport(
        context: Context,
        fileNamePrefix: String,
        debts: List<DebtEntity>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "${fileNamePrefix}_${System.currentTimeMillis()}.csv")
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel opens Arabic columns cleanly without mojibake
        sb.append('\uFEFF')
        sb.append("المعرف,الشخص,الاتجاه,أصل الدين,المتبقي,العملة,تاريخ التسجيل,تاريخ الاستحقاق,الحالة,السبب\n")
        for (d in debts) {
            val dir = runCatching { DebtDirection.valueOf(d.direction).labelAr }.getOrDefault(d.direction)
            val st = runCatching { DebtStatus.valueOf(d.status).labelAr }.getOrDefault(d.status)
            val safeReason = d.reason.replace("\"", "\"\"")
            val safeName = d.contactName.replace("\"", "\"\"")
            sb.append("${d.id},\"$safeName\",\"$dir\",${MoneyMath.minorToBigDecimal(d.principalMinor)},${MoneyMath.minorToBigDecimal(d.remainingMinor)},${d.currencyCode},${formatDate(d.recordedAtMillis)},${formatDate(d.dueAtMillis)},\"$st\",\"$safeReason\"\n")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    fun printPdfFile(context: Context, file: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter: PrintDocumentAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = android.print.PrintDocumentInfo.Builder(file.name)
                    .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: android.os.ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    file.inputStream().use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
