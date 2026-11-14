// Optional Firebase Auth + Firestore Cloud Sync for PWA (gracefully offline-capable)
// Matches the exact Firebase project and Firestore schema used by the Android app.
export const FIREBASE_CONFIG = {
  apiKey: "AIzaSyCi7giVSyL1CJmu780_Qnc7fQHrpg9TuxA",
  authDomain: "gen-lang-client-0821343243.firebaseapp.com",
  projectId: "gen-lang-client-0821343243",
  storageBucket: "gen-lang-client-0821343243.firebasestorage.app",
  messagingSenderId: "485118474969",
  appId: "1:485118474969:web:hisabati_pwa_client"
};

export const FIRESTORE_DATABASE_ID = "ai-studio-android-cc8646e8-3c99-467c-8184-88976fe831ca";

let firebaseContext = null;

export async function initFirebaseSafely() {
  if (!navigator.onLine) return null;
  if (firebaseContext) return firebaseContext;
  try {
    const [appMod, authMod, fsMod] = await Promise.all([
      import("https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js"),
      import("https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js"),
      import("https://www.gstatic.com/firebasejs/11.0.2/firebase-firestore.js")
    ]);
    const app = appMod.getApps().length ? appMod.getApp() : appMod.initializeApp(FIREBASE_CONFIG);
    const auth = authMod.getAuth(app);
    const db = fsMod.initializeFirestore(
      app,
      { localCache: fsMod.persistentLocalCache() },
      FIRESTORE_DATABASE_ID
    );
    firebaseContext = { app, auth, db, authMod, fsMod };
    return firebaseContext;
  } catch {
    return null;
  }
}

export async function signInWithGoogleCloud() {
  const ctx = await initFirebaseSafely();
  if (!ctx) throw new Error("تتطلب المزامنة السحابية اتصالاً بالإنترنت.");
  const provider = new ctx.authMod.GoogleAuthProvider();
  const result = await ctx.authMod.signInWithPopup(ctx.auth, provider);
  return result.user;
}

export async function signOutCloud() {
  if (!firebaseContext) return;
  await firebaseContext.authMod.signOut(firebaseContext.auth);
}

export async function pullCloudSnapshot(uid) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !uid) return null;
  const { db, fsMod } = ctx;
  const [cSnap, dSnap, pSnap] = await Promise.all([
    fsMod.getDocs(fsMod.collection(db, "users", uid, "contacts")),
    fsMod.getDocs(fsMod.collection(db, "users", uid, "debts")),
    fsMod.getDocs(fsMod.collection(db, "users", uid, "payments"))
  ]);
  return {
    contacts: cSnap.docs.map((d) => ({ id: d.id, ...d.data() })),
    debts: dSnap.docs.map((d) => ({ id: d.id, ...d.data() })),
    payments: pSnap.docs.map((d) => ({ id: d.id, ...d.data() }))
  };
}

export async function pushOutboxToCloud(uid, outboxItems) {
  const ctx = await initFirebaseSafely();
  if (!ctx || !uid || !outboxItems.length) return [];
  const { db, fsMod } = ctx;
  const syncedIds = [];
  for (const item of outboxItems) {
    try {
      const ref = fsMod.doc(db, "users", uid, item.collection, item.docId);
      const raw = { ...item.payload };
      delete raw.id;
      raw.userId = uid;
      if (!raw.createdAt) raw.createdAt = fsMod.serverTimestamp();
      if (item.collection !== "payments") {
        raw.updatedAt = fsMod.serverTimestamp();
      } else {
        delete raw.updatedAt;
      }
      await fsMod.setDoc(ref, raw, { merge: item.collection !== "payments" });
      syncedIds.push(item.id);
    } catch {
      // Keep item in IndexedDB outbox for next sync retry
    }
  }
  return syncedIds;
}
