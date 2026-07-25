-- V1__init_core.sql
-- Core tables: businesses, users, roles, subscriptions, outlets

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================
-- SUBSCRIPTION PLANS
-- ============================
CREATE TABLE subscription_plans (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    price        NUMERIC(10, 2) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY', -- MONTHLY, ANNUAL
    max_outlets  INT NOT NULL DEFAULT 1,
    max_staff    INT NOT NULL DEFAULT 5,
    modules      TEXT[] DEFAULT '{}',
    features     TEXT[] DEFAULT '{}',
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO subscription_plans (name, price, billing_cycle, max_outlets, max_staff, modules, features) VALUES
    ('Trial',      0,    'MONTHLY',  1,  5,  ARRAY['auth','menu','order','kds'], ARRAY['14-day-trial']),
    ('Starter',    999,  'MONTHLY',  1,  10, ARRAY['auth','menu','order','kds','finance','inventory'], ARRAY['pos','qr-menu','invoice-pdf']),
    ('Growth',     2499, 'MONTHLY',  3,  25, ARRAY['auth','menu','order','kds','finance','inventory','hr','crm'], ARRAY['pos','qr-menu','invoice-pdf','gst-reports','payroll','loyalty']),
    ('Enterprise', 4999, 'MONTHLY',  -1, -1, ARRAY['auth','menu','order','kds','finance','inventory','hr','crm','analytics','whatsapp'], ARRAY['pos','qr-menu','invoice-pdf','gst-reports','payroll','loyalty','analytics','whatsapp','einvoice']);

-- ============================
-- BUSINESSES
-- ============================
CREATE TABLE businesses (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(200) NOT NULL,
    business_type        VARCHAR(50),  -- RESTAURANT, CAFE, QSR, RETAIL, BAKERY, FRANCHISE, OTHER
    gstin                VARCHAR(15),
    pan                  VARCHAR(10),
    address_line1        VARCHAR(255),
    address_line2        VARCHAR(255),
    city                 VARCHAR(100),
    state                VARCHAR(100),
    pincode              VARCHAR(10),
    logo_url             TEXT,
    currency_code        VARCHAR(5) NOT NULL DEFAULT 'INR',
    timezone             VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    status               VARCHAR(30) NOT NULL DEFAULT 'ONBOARDING', -- ONBOARDING, ACTIVE, SUSPENDED, CANCELLED
    gst_inclusive        BOOLEAN NOT NULL DEFAULT false,
    onboarding_step      INT NOT NULL DEFAULT 0,
    subscription_plan_id UUID REFERENCES subscription_plans(id),
    trial_ends_at        TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================
-- SUBSCRIPTIONS
-- ============================
CREATE TABLE subscriptions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id             UUID NOT NULL REFERENCES businesses(id),
    plan_id                 UUID NOT NULL REFERENCES subscription_plans(id),
    status                  VARCHAR(20) NOT NULL DEFAULT 'TRIAL', -- TRIAL, ACTIVE, SUSPENDED, CANCELLED
    trial_ends_at           TIMESTAMPTZ,
    current_period_start    TIMESTAMPTZ,
    current_period_end      TIMESTAMPTZ,
    razorpay_subscription_id VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id)
);

-- ============================
-- OUTLETS
-- ============================
CREATE TABLE outlets (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id    UUID NOT NULL REFERENCES businesses(id),
    name           VARCHAR(200) NOT NULL,
    outlet_type    VARCHAR(50) NOT NULL DEFAULT 'DINE_IN', -- DINE_IN, TAKEAWAY, DELIVERY_ONLY, BOTH
    phone          VARCHAR(20),
    address_line1  VARCHAR(255),
    address_line2  VARCHAR(255),
    city           VARCHAR(100),
    state          VARCHAR(100),
    pincode        VARCHAR(10),
    gst_number     VARCHAR(15),
    latitude       NUMERIC(10, 8),
    longitude      NUMERIC(11, 8),
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================
-- OUTLET OPERATING HOURS
-- ============================
CREATE TABLE outlet_operating_hours (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outlet_id   UUID NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL, -- MONDAY...SUNDAY
    open_time   TIME,
    close_time  TIME,
    is_closed   BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(outlet_id, day_of_week)
);

-- ============================
-- ROLES
-- ============================
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID REFERENCES businesses(id), -- NULL = system role
    name        VARCHAR(50) NOT NULL,
    permissions TEXT[] DEFAULT '{}',
    is_system   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Insert system-level roles (not tied to a business)
INSERT INTO roles (name, permissions, is_system) VALUES
    ('SUPER_ADMIN',     ARRAY['*'],                                                   true),
    ('BUSINESS_OWNER',  ARRAY['manage:business','manage:staff','manage:all'],          true),
    ('OUTLET_MANAGER',  ARRAY['manage:orders','manage:inventory','manage:attendance'], true),
    ('CASHIER',         ARRAY['create:orders','process:payments','view:menu'],         true),
    ('WAITER',          ARRAY['create:orders','view:tables','view:menu'],              true),
    ('KITCHEN_STAFF',   ARRAY['view:kds','update:kds'],                                true),
    ('ACCOUNTANT',      ARRAY['view:finance','export:reports','view:orders'],          true),
    ('HR_MANAGER',      ARRAY['manage:hr','manage:payroll','view:staff'],              true);

-- ============================
-- USERS
-- ============================
CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID REFERENCES businesses(id),
    outlet_id        UUID REFERENCES outlets(id),
    role_id          UUID NOT NULL REFERENCES roles(id),
    name             VARCHAR(200) NOT NULL,
    email            VARCHAR(255),
    phone            VARCHAR(20) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    is_active        BOOLEAN NOT NULL DEFAULT true,
    is_mobile_verified BOOLEAN NOT NULL DEFAULT false,
    is_email_verified  BOOLEAN NOT NULL DEFAULT false,
    whatsapp_opt_out   BOOLEAN NOT NULL DEFAULT false,
    last_login       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(phone),
    UNIQUE(email)
);

-- ============================
-- REFRESH TOKENS
-- ============================
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- ============================
-- AUDIT LOGS (also in Elasticsearch, keep 90 days in PG)
-- ============================
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID,
    user_id     UUID,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    changes     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_business ON audit_logs(business_id);
CREATE INDEX idx_audit_logs_user     ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created  ON audit_logs(created_at);

-- ============================
-- INDEXES
-- ============================
CREATE INDEX idx_businesses_status  ON businesses(status);
CREATE INDEX idx_outlets_business   ON outlets(business_id);
CREATE INDEX idx_users_business     ON users(business_id);
CREATE INDEX idx_users_phone        ON users(phone);
CREATE INDEX idx_subscriptions_bus  ON subscriptions(business_id);
