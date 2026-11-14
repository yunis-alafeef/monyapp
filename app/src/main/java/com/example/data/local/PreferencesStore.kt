package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccountType
import com.example.export.PdfDocumentKind
import com.example.export.PdfInvoiceTemplate
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.CustomThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "hisabati_prefs")

data class UserAppPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val themePalette: AppThemePalette = AppThemePalette.ROYAL_EMERALD,
    val customPrimaryHex: Long = 0xFF059669L,
    val customAccentHex: Long = 0xFFD97706L,
    val customThemes: List<CustomThemePreset> = emptyList(),
    val defaultCurrency: String = "YER",
    val accountType: AccountType = AccountType.PERSONAL,
    val workspaceName: String = "محفظتي الشخصية",
    val activeMemberId: String = "owner",
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val biometricEnabled: Boolean = false,
    val aiConsentGranted: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val reminderPriorityHigh: Boolean = true,
    val whatsappCloudEnabled: Boolean = false,
    val smsGatewayEnabled: Boolean = false,
    val backendBaseUrl: String = "",
    val whatsappPhoneNumberId: String = "",
    val smsSenderId: String = "HISABATI",
    val whatsappStyle: String = "FORMAL",
    val whatsappIncludeWorkspaceHeader: Boolean = true,
    val whatsappIncludeFinancialBreakdown: Boolean = true,
    val whatsappIncludeDates: Boolean = true,
    val whatsappIncludeReasonAndNotes: Boolean = true,
    val whatsappIncludeFooterSignature: Boolean = true,
    val whatsappCustomSignature: String = "شكراً لتعاملكم الكريم • حساباتي",
    val printTemplate: String = PdfInvoiceTemplate.CLASSIC_FORMAL.name,
    val printCompanyName: String = "",
    val printCompanyPhone: String = "",
    val printCompanyAddress: String = "",
    val printTaxOrCrNumber: String = "",
    val printLogoPath: String = "",
    val printFooterNote: String = "شكراً لتعاملكم معنا • هذا المستند صادر آلياً من تطبيق حساباتي",
    val printShowLogoInHeader: Boolean = true,
    val printShowInmaSoftInHeader: Boolean = true,
    val printShowInmaSoftInFooter: Boolean = true,
    val printShowSignatureBox: Boolean = true,
    val printShowWatermarkBadge: Boolean = true,
    val printDebtColorHex: Long = 0xFF0B1B32L,
    val printReceiptColorHex: Long = 0xFF065F46L,
    val printStatementColorHex: Long = 0xFF1E3A8AL,
    val printReportColorHex: Long = 0xFF115E59L,
    val lastSyncAtMillis: Long = 0L,
    val lastBackupAtMillis: Long = 0L
)

class PreferencesStore(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val CUSTOM_PRIMARY_HEX = longPreferencesKey("custom_primary_hex")
        val CUSTOM_ACCENT_HEX = longPreferencesKey("custom_accent_hex")
        val CUSTOM_THEMES_JSON = stringPreferencesKey("custom_themes_json")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val ACCOUNT_TYPE = stringPreferencesKey("account_type")
        val WORKSPACE_NAME = stringPreferencesKey("workspace_name")
        val ACTIVE_MEMBER_ID = stringPreferencesKey("active_member_id")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AI_CONSENT_GRANTED = booleanPreferencesKey("ai_consent_granted")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_PRIORITY_HIGH = booleanPreferencesKey("reminder_priority_high")
        val WHATSAPP_CLOUD_ENABLED = booleanPreferencesKey("whatsapp_cloud_enabled")
        val SMS_GATEWAY_ENABLED = booleanPreferencesKey("sms_gateway_enabled")
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url")
        val WHATSAPP_PHONE_NUMBER_ID = stringPreferencesKey("whatsapp_phone_number_id")
        val SMS_SENDER_ID = stringPreferencesKey("sms_sender_id")
        val WA_STYLE = stringPreferencesKey("wa_style")
        val WA_INCLUDE_HEADER = booleanPreferencesKey("wa_include_header")
        val WA_INCLUDE_BREAKDOWN = booleanPreferencesKey("wa_include_breakdown")
        val WA_INCLUDE_DATES = booleanPreferencesKey("wa_include_dates")
        val WA_INCLUDE_REASON = booleanPreferencesKey("wa_include_reason")
        val WA_INCLUDE_FOOTER = booleanPreferencesKey("wa_include_footer")
        val WA_CUSTOM_SIGNATURE = stringPreferencesKey("wa_custom_signature")
        val PRINT_TEMPLATE = stringPreferencesKey("print_template")
        val PRINT_COMPANY_NAME = stringPreferencesKey("print_company_name")
        val PRINT_COMPANY_PHONE = stringPreferencesKey("print_company_phone")
        val PRINT_COMPANY_ADDRESS = stringPreferencesKey("print_company_address")
        val PRINT_TAX_OR_CR = stringPreferencesKey("print_tax_or_cr")
        val PRINT_LOGO_PATH = stringPreferencesKey("print_logo_path")
        val PRINT_FOOTER_NOTE = stringPreferencesKey("print_footer_note")
        val PRINT_SHOW_LOGO = booleanPreferencesKey("print_show_logo")
        val PRINT_SHOW_INMA_HEADER = booleanPreferencesKey("print_show_inma_header")
        val PRINT_SHOW_INMA_FOOTER = booleanPreferencesKey("print_show_inma_footer")
        val PRINT_SHOW_SIGNATURE = booleanPreferencesKey("print_show_signature")
        val PRINT_SHOW_WATERMARK = booleanPreferencesKey("print_show_watermark")
        val PRINT_DEBT_COLOR_HEX = longPreferencesKey("print_debt_color_hex")
        val PRINT_RECEIPT_COLOR_HEX = longPreferencesKey("print_receipt_color_hex")
        val PRINT_STATEMENT_COLOR_HEX = longPreferencesKey("print_statement_color_hex")
        val PRINT_REPORT_COLOR_HEX = longPreferencesKey("print_report_color_hex")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }

    val preferencesFlow: Flow<UserAppPreferences> = context.dataStore.data.map { prefs ->
        UserAppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM,
            themePalette = prefs[Keys.THEME_PALETTE]?.let { runCatching { AppThemePalette.valueOf(it) }.getOrNull() } ?: AppThemePalette.ROYAL_EMERALD,
            customPrimaryHex = prefs[Keys.CUSTOM_PRIMARY_HEX] ?: 0xFF059669L,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: 0xFFD97706L,
            customThemes = parseCustomThemes(prefs[Keys.CUSTOM_THEMES_JSON] ?: "[]"),
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "YER",
            accountType = prefs[Keys.ACCOUNT_TYPE]?.let { runCatching { AccountType.valueOf(it) }.getOrNull() } ?: AccountType.PERSONAL,
            workspaceName = prefs[Keys.WORKSPACE_NAME] ?: "محفظتي الشخصية",
            activeMemberId = prefs[Keys.ACTIVE_MEMBER_ID] ?: "owner",
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            aiConsentGranted = prefs[Keys.AI_CONSENT_GRANTED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            defaultReminderHour = prefs[Keys.REMINDER_HOUR] ?: 9,
            defaultReminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            reminderPriorityHigh = prefs[Keys.REMINDER_PRIORITY_HIGH] ?: true,
            whatsappCloudEnabled = prefs[Keys.WHATSAPP_CLOUD_ENABLED] ?: false,
            smsGatewayEnabled = prefs[Keys.SMS_GATEWAY_ENABLED] ?: false,
            backendBaseUrl = prefs[Keys.BACKEND_BASE_URL] ?: "",
            whatsappPhoneNumberId = prefs[Keys.WHATSAPP_PHONE_NUMBER_ID] ?: "",
            smsSenderId = prefs[Keys.SMS_SENDER_ID] ?: "HISABATI",
            whatsappStyle = prefs[Keys.WA_STYLE] ?: "FORMAL",
            whatsappIncludeWorkspaceHeader = prefs[Keys.WA_INCLUDE_HEADER] ?: true,
            whatsappIncludeFinancialBreakdown = prefs[Keys.WA_INCLUDE_BREAKDOWN] ?: true,
            whatsappIncludeDates = prefs[Keys.WA_INCLUDE_DATES] ?: true,
            whatsappIncludeReasonAndNotes = prefs[Keys.WA_INCLUDE_REASON] ?: true,
            whatsappIncludeFooterSignature = prefs[Keys.WA_INCLUDE_FOOTER] ?: true,
            whatsappCustomSignature = prefs[Keys.WA_CUSTOM_SIGNATURE] ?: "شكراً لتعاملكم الكريم • حساباتي",
            printTemplate = prefs[Keys.PRINT_TEMPLATE] ?: PdfInvoiceTemplate.CLASSIC_FORMAL.name,
            printCompanyName = prefs[Keys.PRINT_COMPANY_NAME] ?: "",
            printCompanyPhone = prefs[Keys.PRINT_COMPANY_PHONE] ?: "",
            printCompanyAddress = prefs[Keys.PRINT_COMPANY_ADDRESS] ?: "",
            printTaxOrCrNumber = prefs[Keys.PRINT_TAX_OR_CR] ?: "",
            printLogoPath = prefs[Keys.PRINT_LOGO_PATH] ?: "",
            printFooterNote = prefs[Keys.PRINT_FOOTER_NOTE] ?: "شكراً لتعاملكم معنا • هذا المستند صادر آلياً من تطبيق حساباتي",
            printShowLogoInHeader = prefs[Keys.PRINT_SHOW_LOGO] ?: true,
            printShowInmaSoftInHeader = prefs[Keys.PRINT_SHOW_INMA_HEADER] ?: true,
            printShowInmaSoftInFooter = prefs[Keys.PRINT_SHOW_INMA_FOOTER] ?: true,
            printShowSignatureBox = prefs[Keys.PRINT_SHOW_SIGNATURE] ?: true,
            printShowWatermarkBadge = prefs[Keys.PRINT_SHOW_WATERMARK] ?: true,
            printDebtColorHex = prefs[Keys.PRINT_DEBT_COLOR_HEX] ?: 0xFF0B1B32L,
            printReceiptColorHex = prefs[Keys.PRINT_RECEIPT_COLOR_HEX] ?: 0xFF065F46L,
            printStatementColorHex = prefs[Keys.PRINT_STATEMENT_COLOR_HEX] ?: 0xFF1E3A8AL,
            printReportColorHex = prefs[Keys.PRINT_REPORT_COLOR_HEX] ?: 0xFF115E59L,
            lastSyncAtMillis = prefs[Keys.LAST_SYNC_AT] ?: 0L,
            lastBackupAtMillis = prefs[Keys.LAST_BACKUP_AT] ?: 0L
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemePalette(palette: AppThemePalette) {
        context.dataStore.edit { it[Keys.THEME_PALETTE] = palette.name }
    }

    suspend fun setCustomThemeColors(primaryHex: Long, accentHex: Long, mode: AppThemeMode? = null) {
        context.dataStore.edit {
            it[Keys.THEME_PALETTE] = AppThemePalette.CUSTOM.name
            it[Keys.CUSTOM_PRIMARY_HEX] = primaryHex
            it[Keys.CUSTOM_ACCENT_HEX] = accentHex
            if (mode != null) it[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveCustomThemePreset(
        nameAr: String,
        primaryHex: Long,
        accentHex: Long,
        darkModePreferred: Boolean
    ) {
        context.dataStore.edit { prefs ->
            val current = parseCustomThemes(prefs[Keys.CUSTOM_THEMES_JSON] ?: "[]").toMutableList()
            val preset = CustomThemePreset(
                id = "thm_${UUID.randomUUID().toString().replace("-", "").take(10)}",
                nameAr = nameAr.trim().ifBlank { "ثيم مخصص ${current.size + 1}" },
                primaryHex = primaryHex,
                accentHex = accentHex,
                darkModePreferred = darkModePreferred
            )
            current.add(0, preset)
            prefs[Keys.CUSTOM_THEMES_JSON] = encodeCustomThemes(current)
            prefs[Keys.THEME_PALETTE] = AppThemePalette.CUSTOM.name
            prefs[Keys.CUSTOM_PRIMARY_HEX] = primaryHex
            prefs[Keys.CUSTOM_ACCENT_HEX] = accentHex
            prefs[Keys.THEME_MODE] = if (darkModePreferred) AppThemeMode.DARK.name else AppThemeMode.LIGHT.name
        }
    }

    suspend fun deleteCustomThemePreset(id: String) {
        context.dataStore.edit { prefs ->
            val updated = parseCustomThemes(prefs[Keys.CUSTOM_THEMES_JSON] ?: "[]").filterNot { it.id == id }
            prefs[Keys.CUSTOM_THEMES_JSON] = encodeCustomThemes(updated)
        }
    }

    suspend fun setDefaultCurrency(code: String) {
        context.dataStore.edit { it[Keys.DEFAULT_CURRENCY] = code }
    }

    suspend fun setWorkspaceConfig(accountType: AccountType, workspaceName: String) {
        context.dataStore.edit {
            it[Keys.ACCOUNT_TYPE] = accountType.name
            it[Keys.WORKSPACE_NAME] = workspaceName
        }
    }

    suspend fun setActiveMemberId(memberId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_MEMBER_ID] = memberId }
    }

    suspend fun setPin(pin: String) {
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hashHex = hashPin(pin, saltHex)
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = true
            it[Keys.PIN_SALT] = saltHex
            it[Keys.PIN_HASH] = hashHex
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.PIN_SALT] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    fun verifyPin(pin: String, storedSalt: String, storedHash: String): Boolean {
        if (storedHash.isBlank() || storedSalt.isBlank()) return false
        return hashPin(pin, storedSalt) == storedHash
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAiConsentGranted(granted: Boolean) {
        context.dataStore.edit { it[Keys.AI_CONSENT_GRANTED] = granted }
    }

    suspend fun setNotificationSettings(enabled: Boolean, hour: Int, minute: Int, highPriority: Boolean) {
        context.dataStore.edit {
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
            it[Keys.REMINDER_PRIORITY_HIGH] = highPriority
        }
    }

    suspend fun setMessagingRelaySettings(
        whatsappCloudEnabled: Boolean,
        smsGatewayEnabled: Boolean,
        backendBaseUrl: String,
        whatsappPhoneNumberId: String,
        smsSenderId: String
    ) {
        context.dataStore.edit {
            it[Keys.WHATSAPP_CLOUD_ENABLED] = whatsappCloudEnabled
            it[Keys.SMS_GATEWAY_ENABLED] = smsGatewayEnabled
            it[Keys.BACKEND_BASE_URL] = backendBaseUrl.trim()
            it[Keys.WHATSAPP_PHONE_NUMBER_ID] = whatsappPhoneNumberId.trim()
            it[Keys.SMS_SENDER_ID] = smsSenderId.trim().ifEmpty { "HISABATI" }
        }
    }

    suspend fun setWhatsAppFormatSettings(
        style: String,
        includeHeader: Boolean,
        includeBreakdown: Boolean,
        includeDates: Boolean,
        includeReason: Boolean,
        includeFooter: Boolean,
        customSignature: String
    ) {
        context.dataStore.edit {
            it[Keys.WA_STYLE] = style
            it[Keys.WA_INCLUDE_HEADER] = includeHeader
            it[Keys.WA_INCLUDE_BREAKDOWN] = includeBreakdown
            it[Keys.WA_INCLUDE_DATES] = includeDates
            it[Keys.WA_INCLUDE_REASON] = includeReason
            it[Keys.WA_INCLUDE_FOOTER] = includeFooter
            it[Keys.WA_CUSTOM_SIGNATURE] = customSignature.trim().ifEmpty { "شكراً لتعاملكم الكريم • حساباتي" }
        }
    }

    suspend fun setPrintSettings(
        template: PdfInvoiceTemplate,
        companyName: String,
        companyPhone: String,
        companyAddress: String,
        taxOrCrNumber: String,
        footerNote: String,
        showLogoInHeader: Boolean,
        showInmaSoftInHeader: Boolean,
        showInmaSoftInFooter: Boolean,
        showSignatureBox: Boolean,
        showWatermarkBadge: Boolean,
        debtColorHex: Long,
        receiptColorHex: Long,
        statementColorHex: Long,
        reportColorHex: Long
    ) {
        context.dataStore.edit {
            it[Keys.PRINT_TEMPLATE] = template.name
            it[Keys.PRINT_COMPANY_NAME] = companyName.trim()
            it[Keys.PRINT_COMPANY_PHONE] = companyPhone.trim()
            it[Keys.PRINT_COMPANY_ADDRESS] = companyAddress.trim()
            it[Keys.PRINT_TAX_OR_CR] = taxOrCrNumber.trim()
            it[Keys.PRINT_FOOTER_NOTE] = footerNote.trim().ifEmpty {
                "شكراً لتعاملكم معنا • هذا المستند صادر آلياً من تطبيق حساباتي"
            }
            it[Keys.PRINT_SHOW_LOGO] = showLogoInHeader
            it[Keys.PRINT_SHOW_INMA_HEADER] = showInmaSoftInHeader
            it[Keys.PRINT_SHOW_INMA_FOOTER] = showInmaSoftInFooter
            it[Keys.PRINT_SHOW_SIGNATURE] = showSignatureBox
            it[Keys.PRINT_SHOW_WATERMARK] = showWatermarkBadge
            it[Keys.PRINT_DEBT_COLOR_HEX] = debtColorHex
            it[Keys.PRINT_RECEIPT_COLOR_HEX] = receiptColorHex
            it[Keys.PRINT_STATEMENT_COLOR_HEX] = statementColorHex
            it[Keys.PRINT_REPORT_COLOR_HEX] = reportColorHex
        }
    }

    suspend fun setPrintLogoPath(path: String) {
        context.dataStore.edit { it[Keys.PRINT_LOGO_PATH] = path }
    }

    suspend fun setPrintDocumentColor(kind: PdfDocumentKind, colorHex: Long) {
        context.dataStore.edit {
            when (kind) {
                PdfDocumentKind.DEBT_VOUCHER -> it[Keys.PRINT_DEBT_COLOR_HEX] = colorHex
                PdfDocumentKind.PAYMENT_RECEIPT -> it[Keys.PRINT_RECEIPT_COLOR_HEX] = colorHex
                PdfDocumentKind.STATEMENT -> it[Keys.PRINT_STATEMENT_COLOR_HEX] = colorHex
                PdfDocumentKind.GENERAL_REPORT -> it[Keys.PRINT_REPORT_COLOR_HEX] = colorHex
            }
        }
    }

    suspend fun applyColorToAllPrintDocuments(colorHex: Long) {
        context.dataStore.edit {
            it[Keys.PRINT_DEBT_COLOR_HEX] = colorHex
            it[Keys.PRINT_RECEIPT_COLOR_HEX] = colorHex
            it[Keys.PRINT_STATEMENT_COLOR_HEX] = colorHex
            it[Keys.PRINT_REPORT_COLOR_HEX] = colorHex
        }
    }

    suspend fun setLastSyncAt(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_AT] = millis }
    }

    suspend fun setLastBackupAt(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = millis }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun parseCustomThemes(json: String): List<CustomThemePreset> {
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        CustomThemePreset(
                            id = obj.getString("id"),
                            nameAr = obj.getString("nameAr"),
                            primaryHex = obj.getLong("primaryHex"),
                            accentHex = obj.getLong("accentHex"),
                            darkModePreferred = obj.optBoolean("darkModePreferred", false)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeCustomThemes(list: List<CustomThemePreset>): String {
        val arr = JSONArray()
        list.forEach { preset ->
            arr.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("nameAr", preset.nameAr)
                    put("primaryHex", preset.primaryHex)
                    put("accentHex", preset.accentHex)
                    put("darkModePreferred", preset.darkModePreferred)
                }
            )
        }
        return arr.toString()
    }
}
