# دليل قاعدة البيانات والخادم الخلفي — حساباتي

## 1. قاعدة البيانات السحابية (Cloud Firestore)
- معرف قاعدة البيانات: `ai-studio-android-cc8646e8-3c99-467c-8184-88976fe831ca`.
- الهيكل المعرف في `/firebase-blueprint.json` يعتمد مسار `/users/{userId}/...` لعزل بيانات كل مستخدم تماماً.
- الجداول المحمية بعدم قابلية التعديل (`allow update: if false`): `payments` و `audit_logs`.
- حقل `principalMinor` في `debts` محمي ضد التغيير بعد الإنشاء (`request.resource.data.principalMinor == resource.data.principalMinor`).

## 2. قاعدة البيانات العلائقية (PostgreSQL)
- ملف الترحيل `/backend/migrations/001_initial_schema.sql` ينشئ **18 جدولاً** مترابطاً:
  `users`, `profiles`, `workspaces`, `workspace_members`, `currencies`, `exchange_rates`, `contacts`, `debts`, `payments`, `installments`, `transactions`, `reminders`, `notification_logs`, `message_templates`, `attachments`, `audit_logs`, `sync_records`, `user_settings`.
- جميع المبالغ مخزنة كـ `BIGINT` مع قيود `CHECK (remaining_minor >= 0 AND remaining_minor <= principal_minor)` لمنع أي سداد زائد على مستوى قاعدة البيانات أيضاً.

## 3. تشغيل الخادم الخلفي محلياً
```bash
cd backend
cp .env.example .env
npm install
npm run migrate
npm run build && npm start
```
