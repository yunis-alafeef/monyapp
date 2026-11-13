package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.ContactEntity
import com.example.data.local.DebtEntity
import com.example.data.local.PaymentEntity
import com.example.data.remote.FirestoreCloudDataSource
import com.example.domain.model.AccountType
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class HisabatiRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedOwner_canCreateContactDebtAndImmutablePayment() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val cloud = FirestoreCloudDataSource(firestore)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            cloud.ensureUserProfile(
                userId = uid,
                displayName = "تاجر الاختبار",
                email = ALICE_EMAIL,
                defaultCurrency = "SAR",
                accountType = AccountType.BUSINESS,
                aiConsentGranted = false
            )
        }

        val now = System.currentTimeMillis()
        val contactId = "cnt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val contact = ContactEntity(
            id = contactId,
            userId = uid,
            fullName = "عبدالله الحربي",
            phone = "966501112233",
            createdAtMillis = now,
            updatedAtMillis = now
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { cloud.upsertContact(contact) }

        val debtId = "dbt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val debt = DebtEntity(
            id = debtId,
            userId = uid,
            contactId = contactId,
            contactName = contact.fullName,
            direction = DebtDirection.RECEIVABLE.name,
            principalMinor = 100_000L,
            remainingMinor = 100_000L,
            currencyCode = "SAR",
            exchangeRateMicros = 266_667L,
            recordedAtMillis = now,
            dueAtMillis = now + 86_400_000L,
            reason = "بضاعة آجل",
            status = DebtStatus.OPEN.name,
            version = 1L,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { cloud.upsertDebt(debt) }

        val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val payment = PaymentEntity(
            id = paymentId,
            userId = uid,
            debtId = debtId,
            contactId = contactId,
            receiptNumber = "HSB-000001",
            amountMinor = 40_000L,
            discountMinor = 0L,
            currencyCode = "SAR",
            method = PaymentMethod.CASH.name,
            paidAtMillis = now,
            balanceBeforeMinor = 100_000L,
            balanceAfterMinor = 60_000L,
            idempotencyKey = "idem_${UUID.randomUUID()}",
            createdAtMillis = now
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { cloud.createPayment(payment) }

        val observedDebts = withTimeout(FLOW_TIMEOUT_MS) {
            cloud.observeDebts(uid).first { list -> list.any { it.id == debtId } }
        }
        assertTrue(observedDebts.any { it.id == debtId })
    }

    @Test
    fun crossUserAccess_deniedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val cloud = FirestoreCloudDataSource(firestore)
        val now = System.currentTimeMillis()
        val contactId = "cnt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            cloud.upsertContact(
                ContactEntity(
                    id = contactId,
                    userId = aliceUid,
                    fullName = "عميل سري",
                    phone = "777000111",
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                cloud.observeContacts(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user access")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun unauthenticatedObserve_deniedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val cloud = FirestoreCloudDataSource(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                cloud.observeDebts("some_uid").first()
            }
            fail("Expected PERMISSION_DENIED when signed out")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_hisabati@test.com"
        const val BOB_EMAIL = "bob_hisabati@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
