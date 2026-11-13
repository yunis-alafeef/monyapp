package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccountType
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.dataStore by preferencesDataStore(name = "hisabati_prefs")

data class UserAppPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val defaultCurrency: String = "SAR",
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
    val lastSyncAtMillis: Long = 0L,
    val lastBackupAtMillis: Long = 0L
)

class PreferencesStore(private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
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
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }

    val preferencesFlow: Flow<UserAppPreferences> = context.dataStore.data.map { prefs ->
        UserAppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM,
            defaultCurrency = prefs[Keys.DEFAULT_CURRENCY] ?: "SAR",
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
}
