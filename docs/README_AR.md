# حساباتي | HISABATI — الدليل الشامل للتشغيل والنشر

**الشعار:** كل ديونك ومستحقاتك في مكان واحد.

## 1. نظرة عامة على البنية البرمجية
- **تطبيق Android**: Kotlin + Jetpack Compose + Material 3 + اتجاه RTL عربي 100% + خطوط محلية (`IBM Plex Sans Arabic` و `Tajawal`).
- **الحسابات المالية الدقيقة**: استخدام `Long` بالوحدات الصغرى (هللة/سنت) و `BigDecimal` فقط في `MoneyMath.kt` دون أي `Float` أو `Double`.
- **العمل دون إنترنت (Offline-First)**: قاعدة بيانات محلية `Room` + جدول `sync_outbox` بمفتاح منع التكرار `idempotencyKey` + مزامنة خلفية عبر `WorkManager` مع `Cloud Firestore`.
- **الأمان والعزل**: مصادقة Google (`CredentialManager`) + قواعد أمان Zero-Trust في `firestore.rules` + قفل `PIN` (SHA-256 salted) والبصمة الحيوية `BiometricPrompt` + `Firebase App Check`.
- **المساعد المالي الذكي**: مبني عبر `Firebase AI Logic` (`gemini-3.8-flash`) مع موافقة صريحة للخصوصية من المستخدم.
- **التقارير والفواتير**: توليد ملفات PDF عربية RTL (`PdfDocument` + `StaticLayout`) وملفات CSV UTF-8 BOM متوافقة مع Excel، بالإضافة إلى الطباعة والمشاركة عبر `FileProvider`.

## 2. خطوات التشغيل والإعداد
1. لتشغيل الميزات السحابية (Firestore و AI Logic)، تم ربط المشروع بـ Firebase (`app/google-services.json` و `firebase_applet_config.xml`).
2. لتشغيل الإرسال الآلي لرسائل WhatsApp Cloud API و SMS في الخلفية:
   - انشر المجلد `backend/` على أي خادم Node.js + PostgreSQL.
   - نفّذ الترحيل `backend/migrations/001_initial_schema.sql`.
   - ضع رابط الخادم HTTPS داخل التطبيق في شاشة **إعدادات واتساب** أو **إعدادات SMS**.
3. لا يحتاج التطبيق إلى مفاتيح API داخلية أو `GEMINI_API_KEY` في `.env` بفضل استخدام Firebase AI Logic و App Check.
