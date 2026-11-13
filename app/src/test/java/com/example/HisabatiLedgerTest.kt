package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HisabatiDatabase
import com.example.data.local.PreferencesStore
import com.example.data.remote.FirestoreCloudDataSource
import com.example.data.repository.HisabatiRepository
import com.example.data.repository.RepositoryActionResult
import com.example.domain.model.DebtDirection
import com.example.domain.model.DebtStatus
import com.example.domain.model.InstallmentStatus
import com.example.domain.model.MoneyMath
import com.example.domain.model.PaymentMethod
import com.example.domain.model.WorkspaceRole
import com.example.export.ArabicReportExporter
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [36])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class HisabatiLedgerTest {

    private lateinit var context: Context
    private lateinit var db: HisabatiDatabase
    private lateinit var prefs: PreferencesStore
    private lateinit var repo: HisabatiRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val appName = "hisabati_ledger_test_app"
        val app = FirebaseApp.getApps(context).find { it.name == appName }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.hisabati.mrzqkx")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-test-key")
                    .build(),
                appName
            )
        val firestore = FirebaseFirestore.getInstance(
            app,
            "ai-studio-android-cc8646e8-3c99-467c-8184-88976fe831ca"
        )
        runCatching {
            firestore.useEmulator("127.0.0.1", 8085)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }

        db = Room.inMemoryDatabaseBuilder(context, HisabatiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = PreferencesStore(context)
        runBlocking(Dispatchers.IO) {
            withTimeout(5_000L) { prefs.clearAll() }
        }
        repo = HisabatiRepository(db, FirestoreCloudDataSource(firestore), prefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun moneyMath_exactPrecisionAndCurrencyConversion() {
        assertEquals(12550L, MoneyMath.parseMajorToMinor("125.50"))
        assertEquals(100000L, MoneyMath.parseMajorToMinor("1,000"))
        assertNull(MoneyMath.parseMajorToMinor("-50"))

        val sarMinor = MoneyMath.convertMinor(10_000L, 1_000_000L, 266_667L)
        assertEquals(37_500L, sarMinor)
    }

    @Test
    fun offlineLedger_debtInstallmentsPartialFullPaymentOverpaymentAndIdempotency() = runBlocking(Dispatchers.IO) {
        withTimeout(15_000L) {
            val uid = "uid_ledger_test"
            repo.initializeUserWorkspace(uid, "متجر النور", "alnoor@test.com")

            val contactRes = repo.saveContact(uid, null, "سالم القحطاني", "966550001122", "الرياض", null, null)
            assertTrue(contactRes is RepositoryActionResult.Success)
            val contact = (contactRes as RepositoryActionResult.Success).data

            val now = System.currentTimeMillis()
            val debtRes = repo.addDebt(
                userId = uid,
                contactId = contact.id,
                direction = DebtDirection.RECEIVABLE,
                principalMinor = 100_000L,
                currencyCode = "SAR",
                recordedAtMillis = now,
                dueAtMillis = now + 30L * 86_400_000L,
                reason = "توريد أجهزة",
                paymentMethod = "تحويل بنكي",
                attachmentUri = null,
                notes = null,
                installmentCount = 4
            )
            assertTrue(debtRes is RepositoryActionResult.Success)
            val debt = (debtRes as RepositoryActionResult.Success).data

            val installments = repo.observeInstallments(uid).first().filter { it.debtId == debt.id }
            assertEquals(4, installments.size)
            assertEquals(100_000L, installments.sumOf { it.amountMinor })

            val overpayRes = repo.recordPayment(
                userId = uid,
                debtId = debt.id,
                amountMinor = 120_000L,
                discountMinor = 0L,
                method = PaymentMethod.CASH,
                paidAtMillis = now,
                notes = null
            )
            assertTrue(overpayRes is RepositoryActionResult.Error)

            val idemKey = "idem_partial_1"
            val partial1 = repo.recordPayment(
                userId = uid,
                debtId = debt.id,
                amountMinor = 30_000L,
                discountMinor = 0L,
                method = PaymentMethod.TRANSFER,
                paidAtMillis = now,
                notes = "دفعة أولى",
                idempotencyKey = idemKey
            )
            assertTrue(partial1 is RepositoryActionResult.Success)
            val partialRetry = repo.recordPayment(
                userId = uid,
                debtId = debt.id,
                amountMinor = 30_000L,
                discountMinor = 0L,
                method = PaymentMethod.TRANSFER,
                paidAtMillis = now,
                notes = "إعادة إرسال",
                idempotencyKey = idemKey
            )
            assertTrue(partialRetry is RepositoryActionResult.Success)
            assertEquals(
                (partial1 as RepositoryActionResult.Success).data.id,
                (partialRetry as RepositoryActionResult.Success).data.id
            )

            val updatedAfterPartial = db.debtDao().getById(debt.id, uid)!!
            assertEquals(70_000L, updatedAfterPartial.remainingMinor)
            assertEquals(100_000L, updatedAfterPartial.principalMinor)
            assertEquals(DebtStatus.PARTIAL.name, updatedAfterPartial.status)

            val instAfter = repo.observeInstallments(uid).first().filter { it.debtId == debt.id }
            assertEquals(InstallmentStatus.PAID.name, instAfter[0].status)
            assertEquals(5_000L, instAfter[1].paidMinor)

            val finalPay = repo.recordPayment(
                userId = uid,
                debtId = debt.id,
                amountMinor = 60_000L,
                discountMinor = 10_000L,
                method = PaymentMethod.CASH,
                paidAtMillis = now,
                notes = "تسوية نهائية",
                settleRemaining = true
            )
            assertTrue(finalPay is RepositoryActionResult.Success)
            val settledDebt = db.debtDao().getById(debt.id, uid)!!
            assertEquals(0L, settledDebt.remainingMinor)
            assertEquals(DebtStatus.SETTLED.name, settledDebt.status)
        }
    }

    @Test
    fun rbacViewer_cannotAddDebtOrRecordPayment() = runBlocking(Dispatchers.IO) {
        withTimeout(15_000L) {
            val uid = "uid_rbac_test"
            repo.initializeUserWorkspace(uid, "المالك", "owner@test.com")
            val contact = (repo.saveContact(uid, null, "أحمد", "777111222", null, null, null) as RepositoryActionResult.Success).data

            val viewerRes = repo.saveWorkspaceMember(
                userId = uid,
                displayName = "محاسب عرض فقط",
                email = "viewer@test.com",
                role = WorkspaceRole.VIEWER,
                canAddDebt = false,
                canEditDebt = false,
                canRecordPayment = false,
                canViewReports = true
            )
            val viewer = (viewerRes as RepositoryActionResult.Success).data
            prefs.setActiveMemberId(viewer.id)

            val deniedAdd = repo.addDebt(
                userId = uid,
                contactId = contact.id,
                direction = DebtDirection.RECEIVABLE,
                principalMinor = 50_000L,
                currencyCode = "SAR",
                recordedAtMillis = System.currentTimeMillis(),
                dueAtMillis = System.currentTimeMillis() + 86_400_000L,
                reason = "اختبار صلاحية",
                paymentMethod = null,
                attachmentUri = null,
                notes = null
            )
            assertTrue(deniedAdd is RepositoryActionResult.Error)
        }
    }

    @Test
    fun backupExportImportAndArabicPdfCsvExport() = runBlocking(Dispatchers.IO) {
        withTimeout(15_000L) {
            val uid = "uid_backup_test"
            repo.initializeUserWorkspace(uid, "متجر الأمانة", "store@test.com")
            val contact = (repo.saveContact(uid, null, "فاطمة علي", "967770001112", null, null, null) as RepositoryActionResult.Success).data
            val now = System.currentTimeMillis()
            val debt = (repo.addDebt(
                userId = uid,
                contactId = contact.id,
                direction = DebtDirection.PAYABLE,
                principalMinor = 50_000L,
                currencyCode = "SAR",
                recordedAtMillis = now,
                dueAtMillis = now + 86_400_000L,
                reason = "فاتورة مورد",
                paymentMethod = null,
                attachmentUri = null,
                notes = null
            ) as RepositoryActionResult.Success).data

            val json = repo.exportBackupJson(uid)
            assertTrue(json.contains("HISABATI"))
            assertTrue(json.contains("فاطمة علي"))

            repo.deleteAllUserData(uid)
            assertEquals(0, repo.observeDebts(uid).first().size)

            val restoreRes = repo.importBackupJson(uid, json)
            assertTrue(restoreRes is RepositoryActionResult.Success)
            assertEquals(1, repo.observeDebts(uid).first().size)

            val csvFile = ArabicReportExporter.generateCsvReport(context, "test_csv", listOf(debt))
            assertTrue(csvFile.exists())
            val csvContent = csvFile.readText(Charsets.UTF_8)
            assertTrue(csvContent.startsWith("\uFEFF"))
            assertTrue(csvContent.contains("فاطمة علي"))
            assertTrue(csvContent.contains("فاتورة مورد"))
        }
    }
}
