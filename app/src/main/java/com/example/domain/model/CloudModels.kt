package com.example.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val accountType: String = AccountType.PERSONAL.name,
    val workspaceName: String? = null,
    val defaultCurrency: String = "SAR",
    val aiConsentGranted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("displayName", displayName)
        email?.takeIf { it.isNotBlank() }?.let { put("email", it) }
        put("accountType", accountType)
        workspaceName?.takeIf { it.isNotBlank() }?.let { put("workspaceName", it) }
        put("defaultCurrency", defaultCurrency)
        put("aiConsentGranted", aiConsentGranted)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class CloudContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val address: String? = null,
    val notes: String? = null,
    val avatarUri: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        if (isCreate) {
            put("userId", userId)
            put("createdAtMillis", createdAtMillis)
            put("createdAt", FieldValue.serverTimestamp())
        }
        put("fullName", fullName)
        put("phone", phone)
        address?.takeIf { it.isNotBlank() }?.let { put("address", it) }
        notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
        avatarUri?.takeIf { it.isNotBlank() }?.let { put("avatarUri", it) }
        put("updatedAtMillis", updatedAtMillis)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class CloudDebt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val direction: String = DebtDirection.RECEIVABLE.name,
    val principalMinor: Long = 0L,
    val remainingMinor: Long = 0L,
    val currencyCode: String = "SAR",
    val exchangeRateMicros: Long = 266_667L,
    val recordedAtMillis: Long = 0L,
    val dueAtMillis: Long = 0L,
    val reason: String = "",
    val paymentMethod: String? = null,
    val attachmentUri: String? = null,
    val notes: String? = null,
    val status: String = DebtStatus.OPEN.name,
    val version: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("contactId", contactId)
        put("contactName", contactName)
        put("direction", direction)
        put("principalMinor", principalMinor)
        put("remainingMinor", remainingMinor)
        put("currencyCode", currencyCode)
        put("exchangeRateMicros", exchangeRateMicros)
        put("recordedAtMillis", recordedAtMillis)
        put("dueAtMillis", dueAtMillis)
        put("reason", reason)
        paymentMethod?.takeIf { it.isNotBlank() }?.let { put("paymentMethod", it) }
        attachmentUri?.takeIf { it.isNotBlank() }?.let { put("attachmentUri", it) }
        notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
        put("status", status)
        put("version", version)
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }

    fun toUpdateMap(): Map<String, Any> = buildMap {
        put("contactName", contactName)
        put("remainingMinor", remainingMinor)
        put("dueAtMillis", dueAtMillis)
        put("reason", reason)
        paymentMethod?.takeIf { it.isNotBlank() }?.let { put("paymentMethod", it) }
        attachmentUri?.takeIf { it.isNotBlank() }?.let { put("attachmentUri", it) }
        notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
        put("status", status)
        put("version", version)
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class CloudPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val debtId: String = "",
    val contactId: String = "",
    val receiptNumber: String = "",
    val amountMinor: Long = 0L,
    val discountMinor: Long = 0L,
    val currencyCode: String = "SAR",
    val method: String = PaymentMethod.CASH.name,
    val paidAtMillis: Long = 0L,
    val balanceBeforeMinor: Long = 0L,
    val balanceAfterMinor: Long = 0L,
    val notes: String? = null,
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("debtId", debtId)
        put("contactId", contactId)
        put("receiptNumber", receiptNumber)
        put("amountMinor", amountMinor)
        put("discountMinor", discountMinor)
        put("currencyCode", currencyCode)
        put("method", method)
        put("paidAtMillis", paidAtMillis)
        put("balanceBeforeMinor", balanceBeforeMinor)
        put("balanceAfterMinor", balanceAfterMinor)
        notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
        put("idempotencyKey", idempotencyKey)
        put("createdAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class CloudInstallment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val debtId: String = "",
    val installmentNumber: Long = 1L,
    val amountMinor: Long = 0L,
    val paidMinor: Long = 0L,
    val dueAtMillis: Long = 0L,
    val status: String = InstallmentStatus.PENDING.name,
    val paidAtMillis: Long? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        if (isCreate) {
            put("userId", userId)
            put("debtId", debtId)
            put("installmentNumber", installmentNumber)
            put("createdAt", FieldValue.serverTimestamp())
        }
        put("amountMinor", amountMinor)
        put("paidMinor", paidMinor)
        put("dueAtMillis", dueAtMillis)
        put("status", status)
        paidAtMillis?.let { put("paidAtMillis", it) }
        notes?.takeIf { it.isNotBlank() }?.let { put("notes", it) }
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class CloudAuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actorName: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val details: String = "",
    val reason: String? = null,
    val createdAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("userId", userId)
        put("actorName", actorName)
        put("action", action)
        put("entityType", entityType)
        put("entityId", entityId)
        put("details", details)
        reason?.takeIf { it.isNotBlank() }?.let { put("reason", it) }
        put("createdAtMillis", createdAtMillis)
        put("createdAt", FieldValue.serverTimestamp())
    }
}
