-- V3__orders.sql
-- Orders, order items, payments

-- ============================
-- CUSTOMERS (lightweight — full CRM in V7)
-- ============================
CREATE TABLE customers (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    name             VARCHAR(200) NOT NULL,
    phone            VARCHAR(20),
    email            VARCHAR(255),
    address          TEXT,
    date_of_birth    DATE,
    anniversary      DATE,
    loyalty_points   INT NOT NULL DEFAULT 0,
    tier             VARCHAR(20) NOT NULL DEFAULT 'BRONZE', -- BRONZE, SILVER, GOLD, PLATINUM
    total_spend      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    visit_count      INT NOT NULL DEFAULT 0,
    last_visit_at    TIMESTAMPTZ,
    tags             TEXT[] DEFAULT '{}',
    notes            TEXT,
    whatsapp_opt_out BOOLEAN NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, phone)
);

CREATE INDEX idx_customers_business ON customers(business_id);
CREATE INDEX idx_customers_phone    ON customers(business_id, phone);

-- ============================
-- ORDERS
-- ============================
CREATE TABLE orders (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    outlet_id        UUID NOT NULL REFERENCES outlets(id),
    order_number     VARCHAR(50) NOT NULL,
    order_type       VARCHAR(20) NOT NULL DEFAULT 'DINE_IN', -- DINE_IN, TAKEAWAY, DELIVERY, QR_SELF
    table_id         UUID REFERENCES tables(id),
    customer_id      UUID REFERENCES customers(id),
    staff_id         UUID REFERENCES users(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PLACED, PREPARING, READY, DELIVERED, CANCELLED
    subtotal         NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tax_amount       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    service_charge   NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total            NUMERIC(10, 2) NOT NULL DEFAULT 0,
    payment_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PARTIAL, PAID
    notes            TEXT,
    cancellation_reason TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(outlet_id, order_number)
);

CREATE INDEX idx_orders_business    ON orders(business_id);
CREATE INDEX idx_orders_outlet      ON orders(outlet_id);
CREATE INDEX idx_orders_status      ON orders(status);
CREATE INDEX idx_orders_customer    ON orders(customer_id);
CREATE INDEX idx_orders_table       ON orders(table_id);
CREATE INDEX idx_orders_created_at  ON orders(created_at);

-- ============================
-- ORDER ITEMS
-- ============================
CREATE TABLE order_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id   UUID REFERENCES menu_items(id),
    menu_item_name VARCHAR(200) NOT NULL, -- snapshot at order time
    quantity       INT NOT NULL DEFAULT 1,
    unit_price     NUMERIC(10, 2) NOT NULL,
    variant_id     UUID REFERENCES menu_item_variants(id),
    variant_name   VARCHAR(100),
    addons         JSONB DEFAULT '[]', -- [{id, name, price}]
    tax_amount     NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_price    NUMERIC(10, 2) NOT NULL,
    kds_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PREPARING, DONE
    kds_notes      TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order  ON order_items(order_id);
CREATE INDEX idx_order_items_kds    ON order_items(kds_status);

-- ============================
-- PAYMENTS
-- ============================
CREATE TABLE payments (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id              UUID NOT NULL REFERENCES orders(id),
    amount                NUMERIC(10, 2) NOT NULL,
    method                VARCHAR(20) NOT NULL, -- CASH, CARD, UPI, RAZORPAY, CREDIT
    transaction_id        VARCHAR(200),
    razorpay_order_id     VARCHAR(200),
    razorpay_payment_id   VARCHAR(200),
    razorpay_signature    VARCHAR(500),
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED
    notes                 TEXT,
    paid_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order    ON payments(order_id);
CREATE INDEX idx_payments_status   ON payments(status);
CREATE INDEX idx_payments_paid_at  ON payments(paid_at);

-- ============================
-- ORDER NUMBER SEQUENCE (per outlet per financial year)
-- ============================
CREATE TABLE order_number_sequences (
    outlet_id       UUID NOT NULL REFERENCES outlets(id),
    financial_year  VARCHAR(10) NOT NULL,  -- e.g., 2024-25
    last_number     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (outlet_id, financial_year)
);

-- ============================
-- INVOICE NUMBER SEQUENCE (per business per financial year)
-- ============================
CREATE TABLE invoice_number_sequences (
    business_id     UUID NOT NULL REFERENCES businesses(id),
    financial_year  VARCHAR(10) NOT NULL,
    last_number     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (business_id, financial_year)
);
