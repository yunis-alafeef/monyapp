# توثيق واجهة برمجة التطبيقات — حساباتي | HISABATI API Documentation

## المصادقة (Authentication)
تتطلب جميع نقاط النهاية (عدا `/health`) تمرير رمز Firebase ID Token في الترويسة:
```http
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

---

## 1. فحص الجاهزية
- **GET** `/health`
- **الاستجابة (`200 OK`)**:
```json
{ "ok": true, "service": "hisabati-backend", "database": "connected" }
```

## 2. المزامنة السحابية مع منع التكرار (Idempotent Sync)
- **POST** `/api/v1/sync`
- **الجسم (JSON)**:
```json
{
  "idempotencyKey": "debt_dbt_123456_1",
  "entityType": "DEBT",
  "entityId": "dbt_123456",
  "operation": "UPSERT",
  "payload": {}
}
```
- **الاستجابة (`201 Created` / `200 OK`)**:
```json
{ "accepted": true, "deduplicated": false, "idempotencyKey": "debt_dbt_123456_1" }
```

## 3. بوابة WhatsApp Business Cloud API
- **POST** `/api/v1/messaging/whatsapp/send`
- **المتطلبات على الخادم**: `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`
- **الجسم (JSON)**:
```json
{
  "toPhone": "966501234567",
  "messageBody": "مرحباً، تذكير باستحقاق مبلغ 500 ر.س بتاريخ 2026/05/01",
  "templateType": "DUE_SOON"
}
```

## 4. بوابة الرسائل النصية (SMS Gateway)
- **POST** `/api/v1/messaging/sms/send`
- **المتطلبات على الخادم**: `SMS_PROVIDER_ENDPOINT`, `SMS_API_KEY`, `SMS_SENDER_ID`
- **الجسم (JSON)**:
```json
{
  "toPhone": "+966501234567",
  "messageBody": "تذكير من تطبيق حساباتي بسداد الدفعة المستحقة.",
  "senderId": "HISABATI"
}
```
