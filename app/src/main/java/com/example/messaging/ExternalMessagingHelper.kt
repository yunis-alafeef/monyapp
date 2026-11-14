package com.example.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.UserAppPreferences
import java.net.URLEncoder

enum class WhatsAppMessageStyle(val code: String, val labelAr: String, val descriptionAr: String) {
    FORMAL("FORMAL", "رسمي وتجاري", "صيغة منظمة ومناسبة للمحلات والشركات"),
    FRIENDLY("FRIENDLY", "ودي ومحترم", "أسلوب لطيف ومناسب للأصدقاء والعملاء الدائمين"),
    URGENT("URGENT", "تنبيه عاجل وحازم", "تذكير واضح ومباشر للمبالغ المتأخرة")
}

object ExternalMessagingHelper {

    fun interpolateTemplate(
        template: String,
        contactName: String,
        amountFormatted: String,
        remainingFormatted: String,
        totalFormatted: String = amountFormatted,
        paidFormatted: String = "0",
        reason: String = "",
        dateLabel: String = "",
        receiptNumber: String = ""
    ): String {
        return template
            .replace("{الاسم}", contactName)
            .replace("{المبلغ}", amountFormatted)
            .replace("{المتبقي}", remainingFormatted)
            .replace("{الإجمالي}", totalFormatted)
            .replace("{المسدد}", paidFormatted)
            .replace("{السبب}", reason)
            .replace("{التاريخ}", dateLabel)
            .replace("{رقم_الإيصال}", receiptNumber)
    }

    /**
     * Generates a balanced, professional Arabic WhatsApp message tailored to user preferences.
     */
    fun buildFormattedWhatsAppMessage(
        prefs: UserAppPreferences,
        title: String,
        contactName: String,
        directionLabel: String,
        principalFormatted: String,
        paidFormatted: String,
        remainingFormatted: String,
        recordedDateLabel: String = "",
        dueDateLabel: String = "",
        reason: String = "",
        notes: String = "",
        receiptNumber: String = "",
        paymentMethodLabel: String = ""
    ): String {
        val style = WhatsAppMessageStyle.entries.find { it.code == prefs.whatsappStyle } ?: WhatsAppMessageStyle.FORMAL
        val sb = StringBuilder()

        if (prefs.whatsappIncludeWorkspaceHeader) {
            sb.appendLine("*${prefs.workspaceName}*")
            sb.appendLine("📋 *$title*")
        } else {
            sb.appendLine("📋 *$title*")
        }
        sb.appendLine("──────────────")

        val greeting = when (style) {
            WhatsAppMessageStyle.FORMAL -> "الأخ الكريم / *$contactName* المحترم،"
            WhatsAppMessageStyle.FRIENDLY -> "مرحباً أخي العزيز *$contactName*، طاب يومكم بكل خير،"
            WhatsAppMessageStyle.URGENT -> "تنبيه هام للأخ / *$contactName* بخصوص استحقاق مالي:"
        }
        sb.appendLine(greeting)
        sb.appendLine()

        val intro = when (style) {
            WhatsAppMessageStyle.FORMAL -> "نرفق لكم تفاصيل المعاملة المالية المسجلة لدينا:"
            WhatsAppMessageStyle.FRIENDLY -> "نود مشاركتكم ملخص الحساب التالي بكل ود وتقدير:"
            WhatsAppMessageStyle.URGENT -> "نرجو التكرم بمراجعة المبلغ المستحق أدناه والمبادرة بالسداد في أقرب وقت:"
        }
        sb.appendLine(intro)
        sb.appendLine()

        if (receiptNumber.isNotBlank()) {
            sb.appendLine("▪️ *رقم السند / الإيصال:* `$receiptNumber`")
        }
        sb.appendLine("▪️ *نوع القيد:* $directionLabel")
        if (paymentMethodLabel.isNotBlank()) {
            sb.appendLine("▪️ *طريقة الدفع:* $paymentMethodLabel")
        }

        if (prefs.whatsappIncludeFinancialBreakdown) {
            sb.appendLine("▪️ *أصل المبلغ:* $principalFormatted")
            sb.appendLine("▪️ *المسدد حتى الآن:* $paidFormatted")
            sb.appendLine("▪️ *الرصيد المتبقي:* *$remainingFormatted*")
        } else {
            sb.appendLine("▪️ *المبلغ المستحق:* *$remainingFormatted*")
        }

        if (prefs.whatsappIncludeDates && (recordedDateLabel.isNotBlank() || dueDateLabel.isNotBlank())) {
            if (recordedDateLabel.isNotBlank()) {
                sb.appendLine("📅 *تاريخ القيد:* $recordedDateLabel")
            }
            if (dueDateLabel.isNotBlank()) {
                sb.appendLine("⏰ *تاريخ الاستحقاق:* $dueDateLabel")
            }
        }

        if (prefs.whatsappIncludeReasonAndNotes) {
            if (reason.isNotBlank()) {
                sb.appendLine("📝 *البيان / السبب:* $reason")
            }
            if (notes.isNotBlank()) {
                sb.appendLine("💬 *ملاحظات:* $notes")
            }
        }

        if (prefs.whatsappIncludeFooterSignature) {
            sb.appendLine("──────────────")
            sb.appendLine(prefs.whatsappCustomSignature)
        }
        return sb.toString().trim()
    }

    fun openWhatsAppChat(context: Context, phone: String, message: String): Boolean {
        val cleanDigits = phone.filter { it.isDigit() }
        if (cleanDigits.isBlank()) return false
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$cleanDigits?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openWhatsAppShareText(context: Context, message: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(Intent.createChooser(intent, "إرسال السند عبر التطبيقات").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        }.getOrDefault(false)
    }

    fun openSmsComposer(context: Context, phone: String, message: String): Boolean {
        val uri = Uri.parse("smsto:${phone.trim()}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun dialPhone(context: Context, phone: String): Boolean {
        val uri = Uri.parse("tel:${phone.trim()}")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
