package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.ContactEntity
import com.example.data.local.DebtEntity
import com.example.data.local.PaymentEntity
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

    fun generateArabicPdfReport(
        context: Context,
        fileNamePrefix: String,
        reportTitle: String,
        subtitle: String,
        summaryLines: List<Pair<String, String>>,
        rows: List<StatementPdfLine>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "${fileNamePrefix}_${System.currentTimeMillis()}.pdf")

        val arabicTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic)
        }.getOrNull() ?: Typeface.DEFAULT

        val pdf = PdfDocument()
        try {
            val pageWidth = 595 // A4 width at 72dpi
            val pageHeight = 842 // A4 height at 72dpi
            val margin = 36f
            val contentWidth = (pageWidth - margin * 2).toInt()

            var pageNum = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            var canvas = page.canvas

            val headerBgPaint = Paint().apply { color = Color.parseColor("#0B1B32") }
            val accentPaint = Paint().apply { color = Color.parseColor("#10B981") }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            val rowAltPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 20f
                typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
            }
            val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D1FAE5")
                textSize = 12f
                typeface = arabicTypeface
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11.5f
                typeface = arabicTypeface
            }
            val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0B1B32")
                textSize = 12f
                typeface = Typeface.create(arabicTypeface, Typeface.BOLD)
            }

            // Draw luxury Navy header band
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerBgPaint)
            canvas.drawRect(0f, 106f, pageWidth.toFloat(), 110f, accentPaint)

            var y = 24f
            y += drawRtlParagraph(canvas, "حساباتي — $reportTitle", titlePaint, margin, y, contentWidth) + 6f
            drawRtlParagraph(canvas, "$subtitle   |   تاريخ الإصدار: ${formatDate(System.currentTimeMillis())}", subtitlePaint, margin, y, contentWidth)

            y = 132f
            // Summary grid box
            val summaryBoxStart = y
            y += 10f
            for ((label, value) in summaryLines) {
                val h = drawRtlParagraph(canvas, "$label: $value", boldBodyPaint, margin + 10f, y, contentWidth - 20)
                y += h + 6f
            }
            y += 6f
            canvas.drawRoundRect(margin, summaryBoxStart, pageWidth - margin, y, 8f, 8f, borderPaint)

            y += 20f
            y += drawRtlParagraph(canvas, "تفاصيل الحركات المالية (${rows.size})", boldBodyPaint, margin, y, contentWidth) + 8f

            rows.forEachIndexed { index, row ->
                if (y > pageHeight - 90f) {
                    pdf.finishPage(page)
                    pageNum++
                    page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                    canvas = page.canvas
                    y = 40f
                }
                val lineText = "${row.dateLabel}  •  ${row.title}  •  ${row.amountLabel}  (${row.statusOrMethod})"
                val layout = buildRtlStaticLayout(lineText, bodyPaint, contentWidth - 16)
                val rowH = layout.height + 14f
                if (index % 2 == 0) {
                    canvas.drawRect(margin, y, pageWidth - margin, y + rowH, rowAltPaint)
                }
                canvas.save()
                canvas.translate(margin + 8f, y + 7f)
                layout.draw(canvas)
                canvas.restore()
                canvas.drawLine(margin, y + rowH, pageWidth - margin, y + rowH, borderPaint)
                y += rowH
            }

            // Footer
            val footerText = "تم إنشاء هذا التقرير آلياً عبر تطبيق حساباتي — نظام إدارة الديون والمستحقات"
            drawRtlParagraph(canvas, footerText, subtitlePaint.apply { color = Color.parseColor("#64748B") }, margin, pageHeight - 36f, contentWidth)

            pdf.finishPage(page)
            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            return file
        } finally {
            runCatching { pdf.close() }
        }
    }

    private fun buildRtlStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(100))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setLineSpacing(2f, 1.15f)
            .setIncludePad(false)
            .build()
    }

    private fun drawRtlParagraph(
        canvas: android.graphics.Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int
    ): Float {
        val layout = buildRtlStaticLayout(text, paint, width)
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height.toFloat()
    }

    fun generateContactStatementPdf(
        context: Context,
        contact: ContactEntity,
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>
    ): File {
        val groupedTotals = debts.groupBy { it.currencyCode }.map { (cur, list) ->
            val rec = list.filter { it.direction == DebtDirection.RECEIVABLE.name }.sumOf { it.remainingMinor }
            val pay = list.filter { it.direction == DebtDirection.PAYABLE.name }.sumOf { it.remainingMinor }
            "عملة $cur" to "مستحق لي: ${MoneyMath.formatMinor(rec, currencyCode = cur)} | مستحق عليّ: ${MoneyMath.formatMinor(pay, currencyCode = cur)}"
        }
        val rows = debts.map { d ->
            val dir = runCatching { DebtDirection.valueOf(d.direction).labelAr }.getOrDefault(d.direction)
            val st = runCatching { DebtStatus.valueOf(d.status).labelAr }.getOrDefault(d.status)
            StatementPdfLine(
                dateLabel = formatDate(d.recordedAtMillis),
                title = "$dir: ${d.reason}",
                amountLabel = "أصل ${MoneyMath.formatMinor(d.principalMinor, currencyCode = d.currencyCode)} / متبقي ${MoneyMath.formatMinor(d.remainingMinor, currencyCode = d.currencyCode)}",
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
            reportTitle = "كشف حساب: ${contact.fullName}",
            subtitle = "الهاتف: ${contact.phone}",
            summaryLines = listOf("الاسم" to contact.fullName, "الهاتف" to contact.phone) + groupedTotals,
            rows = rows
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
