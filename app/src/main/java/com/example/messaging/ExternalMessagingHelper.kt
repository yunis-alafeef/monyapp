package com.example.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

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
