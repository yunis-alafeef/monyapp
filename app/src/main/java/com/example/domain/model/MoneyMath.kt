package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Exact monetary arithmetic using minor units (Long, 100 minor = 1.00 major) and BigDecimal.
 * Never stores or sums financial balances with Float or Double.
 */
object MoneyMath {
    private val HUNDRED = BigDecimal(100)
    private val MILLION = BigDecimal(1_000_000L)

    fun parseMajorToMinor(input: String): Long? {
        val normalized = input.trim()
            .replace(",", "")
            .replace("٬", "")
            .replace("٫", ".")
        if (normalized.isEmpty()) return null
        return try {
            val bd = BigDecimal(normalized)
            if (bd < BigDecimal.ZERO) return null
            bd.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun parseRateToMicros(input: String): Long? {
        val normalized = input.trim()
            .replace(",", "")
            .replace("٫", ".")
        if (normalized.isEmpty()) return null
        return try {
            val bd = BigDecimal(normalized)
            if (bd <= BigDecimal.ZERO) return null
            bd.multiply(MILLION).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun minorToBigDecimal(minor: Long): BigDecimal =
        BigDecimal.valueOf(minor).divide(HUNDRED, 2, RoundingMode.HALF_UP)

    fun formatMinor(minor: Long, currencySymbol: String = "", currencyCode: String = ""): String {
        val bd = minorToBigDecimal(minor)
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0.##", symbols)
        val numStr = formatter.format(bd)
        val suffix = when {
            currencySymbol.isNotBlank() -> " $currencySymbol"
            currencyCode.isNotBlank() -> " $currencyCode"
            else -> ""
        }
        return "$numStr$suffix"
    }

    fun formatSignedMinor(minor: Long, currencySymbol: String = ""): String {
        val prefix = if (minor < 0) "-" else ""
        return "$prefix${formatMinor(kotlin.math.abs(minor), currencySymbol)}"
    }

    /**
     * Converts an amount in source currency minor units to target currency minor units
     * via USD micro-rates (1 USD = 1_000_000 micros; rateToUsdMicros = how many USD micros 1 unit of currency equals).
     * Example: 1 USD = 1_000_000; 1 SAR (1/3.75 USD) = 266_667; 1 YER (1/530 USD) = 1_887.
     */
    fun convertMinor(
        amountMinor: Long,
        sourceRateToUsdMicros: Long,
        targetRateToUsdMicros: Long
    ): Long {
        if (amountMinor == 0L || sourceRateToUsdMicros == targetRateToUsdMicros) return amountMinor
        if (sourceRateToUsdMicros <= 0L || targetRateToUsdMicros <= 0L) return amountMinor
        val amount = BigDecimal.valueOf(amountMinor)
        val src = BigDecimal.valueOf(sourceRateToUsdMicros)
        val tgt = BigDecimal.valueOf(targetRateToUsdMicros)
        return amount.multiply(src).divide(tgt, 0, RoundingMode.HALF_UP).longValueExact()
    }

    fun defaultCurrenciesSeed(): List<DefaultCurrencySpec> = listOf(
        DefaultCurrencySpec("YER", "الريال اليمني", "ر.ي", 1_887L, false),
        DefaultCurrencySpec("SAR", "الريال السعودي", "ر.س", 266_667L, false),
        DefaultCurrencySpec("USD", "الدولار الأمريكي", "$", 1_000_000L, false),
        DefaultCurrencySpec("AED", "الدرهم الإماراتي", "د.إ", 272_294L, false),
        DefaultCurrencySpec("EGP", "الجنيه المصري", "ج.م", 20_408L, false)
    )
}

data class DefaultCurrencySpec(
    val code: String,
    val nameAr: String,
    val symbol: String,
    val rateToUsdMicros: Long,
    val isCustom: Boolean
)
