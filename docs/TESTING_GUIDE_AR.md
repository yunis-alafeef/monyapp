# دليل الاختبار والتحقق — حساباتي

## 1. اختبارات قواعد الأمان (Firestore Security Rules)
- **المستوى الأول (Node.js Emulator Rules Test)**:
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
- **المستوى الثاني (Kotlin Robolectric + Firebase Emulators)**:
  ```bash
  gradle :app:testDebugUnitTest --tests '*Rule*'
  ```

## 2. اختبارات المنطق المالي والتطبيق (Unit & Robolectric Tests)
تشمل اختبارات `HisabatiLedgerTest.kt`:
- دقة الحسابات المالية عبر `MoneyMath` (`BigDecimal` وتحويل العملات المتعددة دون كسور عائمة).
- إنشاء الديون والأقساط والدفعات الجزئية والكاملة ومنع تجاوز الرصيد المتبقي.
- مفتاح منع التكرار `idempotencyKey` عند إعادة المزامنة.
- التحقق من صلاحيات فريق العمل RBAC (منع دور `VIEWER` من التعديل أو السداد).
- التصدير والاستيراد عبر JSON وتوليد ملفات PDF العربية.

لتشغيل جميع الاختبارات:
```bash
gradle :app:testDebugUnitTest
```
