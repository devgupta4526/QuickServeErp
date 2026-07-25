-- V7__crm_loyalty.sql
-- CRM enhancements, loyalty program, campaigns

-- ============================
-- LOYALTY PROGRAM
-- ============================
CREATE TABLE loyalty_programs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    name             VARCHAR(200) NOT NULL DEFAULT 'Loyalty Program',
    points_per_rupee NUMERIC(5, 3) NOT NULL DEFAULT 0.1, -- 1 point per ₹10
    redemption_rate  NUMERIC(5, 3) NOT NULL DEFAULT 1.0,  -- 1 point = ₹1
    min_order_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,    -- min order to earn
    is_active        BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id)
);

-- ============================
-- LOYALTY TIERS
-- ============================
CREATE TABLE loyalty_tiers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id      UUID NOT NULL REFERENCES loyalty_programs(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL, -- Bronze, Silver, Gold, Platinum
    min_points      INT NOT NULL DEFAULT 0,
    discount_pct    NUMERIC(5, 2) NOT NULL DEFAULT 0,
    sort_order      INT NOT NULL DEFAULT 0
);

-- ============================
-- LOYALTY TRANSACTIONS
-- ============================
CREATE TABLE loyalty_transactions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id    UUID NOT NULL REFERENCES customers(id),
    type           VARCHAR(20) NOT NULL, -- EARNED, REDEEMED, EXPIRED, ADJUSTED
    points         INT NOT NULL,
    order_id       UUID REFERENCES orders(id),
    description    TEXT,
    transaction_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_txn_customer ON loyalty_transactions(customer_id);
CREATE INDEX idx_loyalty_txn_order    ON loyalty_transactions(order_id);

-- ============================
-- CAMPAIGNS
-- ============================
CREATE TABLE campaigns (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    name             VARCHAR(200) NOT NULL,
    type             VARCHAR(30) NOT NULL, -- BIRTHDAY, ANNIVERSARY, WINBACK, PROMOTIONAL, CUSTOM
    template_name    VARCHAR(100),
    target_segment   VARCHAR(50),  -- ALL, GOLD_TIER, SILVER_TIER, INACTIVE_30D, INACTIVE_60D, BIRTHDAY_THIS_WEEK
    scheduled_at     TIMESTAMPTZ,
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SCHEDULED, RUNNING, COMPLETED, CANCELLED
    total_targets    INT NOT NULL DEFAULT 0,
    sent_count       INT NOT NULL DEFAULT 0,
    delivered_count  INT NOT NULL DEFAULT 0,
    read_count       INT NOT NULL DEFAULT 0,
    failed_count     INT NOT NULL DEFAULT 0,
    created_by       UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_campaigns_business  ON campaigns(business_id);
CREATE INDEX idx_campaigns_status    ON campaigns(status);
CREATE INDEX idx_campaigns_scheduled ON campaigns(scheduled_at) WHERE status = 'SCHEDULED';
