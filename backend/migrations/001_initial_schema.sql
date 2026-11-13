-- Hisabati | حساباتي — PostgreSQL Initial Schema Migration (18 Core Tables)
-- All financial amounts are stored strictly as BIGINT minor units (e.g. halalas / cents)
-- to guarantee zero floating-point rounding drift.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid VARCHAR(128) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Profiles
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40),
    business_name VARCHAR(160),
    avatar_url TEXT,
    locale VARCHAR(16) NOT NULL DEFAULT 'ar',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Workspaces (Personal / Business)
CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    account_type VARCHAR(24) NOT NULL CHECK (account_type IN ('PERSONAL', 'BUSINESS')),
    default_currency_code VARCHAR(10) NOT NULL DEFAULT 'SAR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Workspace Members (RBAC)
CREATE TABLE IF NOT EXISTS workspace_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    display_name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(24) NOT NULL CHECK (role IN ('OWNER', 'MANAGER', 'STAFF', 'VIEWER')),
    can_add_debt BOOLEAN NOT NULL DEFAULT TRUE,
    can_edit_debt BOOLEAN NOT NULL DEFAULT FALSE,
    can_record_payment BOOLEAN NOT NULL DEFAULT TRUE,
    can_view_reports BOOLEAN NOT NULL DEFAULT FALSE,
    invited_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, email)
);

-- 5. Currencies
CREATE TABLE IF NOT EXISTS currencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    name_ar VARCHAR(80) NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    is_custom BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, code)
);

-- 6. Exchange Rates (micro-scaled rate relative to USD = 1_000_000)
CREATE TABLE IF NOT EXISTS exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    currency_code VARCHAR(10) NOT NULL,
    rate_to_usd_micros BIGINT NOT NULL CHECK (rate_to_usd_micros > 0),
    effective_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. Contacts
CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    client_ref_id VARCHAR(64) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40) NOT NULL DEFAULT '',
    address TEXT,
    notes TEXT,
    avatar_uri TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, client_ref_id)
);

-- 8. Debts
CREATE TABLE IF NOT EXISTS debts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    client_ref_id VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL CHECK (direction IN ('RECEIVABLE', 'PAYABLE')),
    principal_minor BIGINT NOT NULL CHECK (principal_minor > 0),
    remaining_minor BIGINT NOT NULL CHECK (remaining_minor >= 0 AND remaining_minor <= principal_minor),
    currency_code VARCHAR(10) NOT NULL,
    exchange_rate_micros BIGINT NOT NULL CHECK (exchange_rate_micros > 0),
    recorded_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL,
    payment_method VARCHAR(64),
    notes TEXT,
    status VARCHAR(24) NOT NULL CHECK (status IN ('OPEN', 'PARTIAL', 'PAID', 'OVERDUE', 'SCHEDULED', 'SETTLED')),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, client_ref_id)
);

-- 9. Payments (Immutable ledger entries with idempotency_key)
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    debt_id UUID NOT NULL REFERENCES debts(id) ON DELETE CASCADE,
    contact_id UUID NOT NULL REFERENCES contacts(id) ON DELETE CASCADE,
    receipt_number VARCHAR(32) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    discount_minor BIGINT NOT NULL DEFAULT 0 CHECK (discount_minor >= 0),
    currency_code VARCHAR(10) NOT NULL,
    method VARCHAR(24) NOT NULL CHECK (method IN ('CASH', 'TRANSFER', 'CARD', 'OTHER')),
    paid_at TIMESTAMPTZ NOT NULL,
    balance_before_minor BIGINT NOT NULL CHECK (balance_before_minor >= 0),
    balance_after_minor BIGINT NOT NULL CHECK (balance_after_minor >= 0),
    notes TEXT,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, idempotency_key),
    CHECK ((amount_minor + discount_minor) > 0)
);

-- 10. Installments
CREATE TABLE IF NOT EXISTS installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    debt_id UUID NOT NULL REFERENCES debts(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL CHECK (installment_number >= 1),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    paid_minor BIGINT NOT NULL DEFAULT 0 CHECK (paid_minor >= 0),
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'OVERDUE')),
    paid_at TIMESTAMPTZ,
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. Transactions (Unified ledger summary projection)
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    debt_id UUID REFERENCES debts(id) ON DELETE SET NULL,
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    tx_type VARCHAR(24) NOT NULL CHECK (tx_type IN ('DEBT_CREATED', 'PAYMENT_RECORDED', 'SETTLEMENT')),
    amount_minor BIGINT NOT NULL,
    currency_code VARCHAR(10) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. Reminders
CREATE TABLE IF NOT EXISTS reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    debt_id UUID REFERENCES debts(id) ON DELETE CASCADE,
    contact_id UUID REFERENCES contacts(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    trigger_at TIMESTAMPTZ NOT NULL,
    repeat_type VARCHAR(16) NOT NULL CHECK (repeat_type IN ('ONCE', 'DAILY', 'WEEKLY', 'MONTHLY')),
    priority VARCHAR(16) NOT NULL CHECK (priority IN ('HIGH', 'NORMAL', 'LOW')),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. Notification Logs
CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    related_debt_id UUID REFERENCES debts(id) ON DELETE SET NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 14. Message Templates
CREATE TABLE IF NOT EXISTS message_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    template_type VARCHAR(32) NOT NULL,
    title_ar VARCHAR(160) NOT NULL,
    body_template TEXT NOT NULL,
    channel VARCHAR(16) NOT NULL CHECK (channel IN ('WHATSAPP', 'SMS', 'BOTH')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, template_type)
);

-- 15. Attachments
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    debt_id UUID REFERENCES debts(id) ON DELETE CASCADE,
    payment_id UUID REFERENCES payments(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    storage_uri TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 16. Audit Logs (Append-only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    actor_name VARCHAR(160) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    details TEXT NOT NULL,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 17. Sync Records (Idempotency & conflict resolution tracking)
CREATE TABLE IF NOT EXISTS sync_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'APPLIED',
    synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, idempotency_key)
);

-- 18. User Settings
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme_mode VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    default_currency VARCHAR(10) NOT NULL DEFAULT 'SAR',
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_hour INTEGER NOT NULL DEFAULT 9,
    reminder_minute INTEGER NOT NULL DEFAULT 0,
    ai_consent_granted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_contacts_workspace_name ON contacts(workspace_id, full_name);
CREATE INDEX IF NOT EXISTS idx_debts_workspace_status_due ON debts(workspace_id, status, due_at);
CREATE INDEX IF NOT EXISTS idx_debts_contact ON debts(contact_id);
CREATE INDEX IF NOT EXISTS idx_payments_debt ON payments(debt_id);
CREATE INDEX IF NOT EXISTS idx_installments_debt_due ON installments(debt_id, due_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_created ON audit_logs(workspace_id, created_at DESC);
