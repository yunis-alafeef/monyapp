const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTs() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write contacts or debts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("contacts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("debts").get());
});

test("Authenticated user: cannot read or write another user's profile, contacts, or debts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).collection("contacts").doc("c1").set({
      userId: BOB_UID,
      fullName: "خالد التاجر",
      phone: "777123456",
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("contacts").doc("c1").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("contacts").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("contacts").doc("c2").set({
      userId: BOB_UID,
      fullName: "اختراق",
      phone: "000",
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );
});

test("Authenticated owner: can create and read own contact and debt with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc("contact_1").set({
      userId: ALICE_UID,
      fullName: "شركة النور التجارية",
      phone: "966501234567",
      createdAtMillis: 1700000000000,
      updatedAtMillis: 1700000000000,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("debts").doc("debt_1").set({
      userId: ALICE_UID,
      contactId: "contact_1",
      contactName: "شركة النور التجارية",
      direction: "RECEIVABLE",
      principalMinor: 50000,
      remainingMinor: 50000,
      currencyCode: "SAR",
      exchangeRateMicros: 266667,
      recordedAtMillis: 1700000000000,
      dueAtMillis: 1702000000000,
      reason: "توريد بضاعة",
      status: "OPEN",
      version: 1,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("debts").get());
});

test("Shadow update & principal mutation rejected on debts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const debtRef = aliceDb.collection("users").doc(ALICE_UID).collection("debts").doc("debt_1");
  await assertSucceeds(
    debtRef.set({
      userId: ALICE_UID,
      contactId: "contact_1",
      contactName: "أحمد",
      direction: "RECEIVABLE",
      principalMinor: 10000,
      remainingMinor: 10000,
      currencyCode: "USD",
      exchangeRateMicros: 1000000,
      recordedAtMillis: 1700000000000,
      dueAtMillis: 1702000000000,
      reason: "قرض حسن",
      status: "OPEN",
      version: 1,
      createdAt: serverTs(),
      updatedAt: serverTs(),
    })
  );

  // Attempting to mutate immutable principalMinor must fail
  await assertFails(
    debtRef.update({
      principalMinor: 1000,
      updatedAt: serverTs(),
    })
  );

  // Ghost field injection must fail
  await assertFails(
    debtRef.update({
      ghostAdminOverride: true,
      updatedAt: serverTs(),
    })
  );

  // Valid remainingMinor update must succeed
  await assertSucceeds(
    debtRef.update({
      remainingMinor: 5000,
      status: "PARTIAL",
      version: 2,
      updatedAt: serverTs(),
    })
  );
});

test("Payments and audit logs are immutable on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payRef = aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1");
  await assertSucceeds(
    payRef.set({
      userId: ALICE_UID,
      debtId: "debt_1",
      contactId: "contact_1",
      receiptNumber: "RCP-1001",
      amountMinor: 5000,
      discountMinor: 0,
      currencyCode: "SAR",
      method: "CASH",
      paidAtMillis: 1700000000000,
      balanceBeforeMinor: 10000,
      balanceAfterMinor: 5000,
      idempotencyKey: "idem_key_123456",
      createdAt: serverTs(),
    })
  );

  await assertFails(payRef.update({ amountMinor: 9000 }));
});
