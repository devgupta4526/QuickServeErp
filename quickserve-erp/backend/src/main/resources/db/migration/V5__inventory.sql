-- V5__inventory.sql
-- Inventory items, stock transactions, suppliers, purchase orders, recipes

-- ============================
-- SUPPLIERS
-- ============================
CREATE TABLE suppliers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID NOT NULL REFERENCES businesses(id),
    name            VARCHAR(200) NOT NULL,
    contact_person  VARCHAR(200),
    phone           VARCHAR(20),
    email           VARCHAR(255),
    gstin           VARCHAR(15),
    address         TEXT,
    payment_terms   VARCHAR(100), -- Net30, Immediate, etc.
    rating          INT DEFAULT 5 CHECK (rating BETWEEN 1 AND 5),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_suppliers_business ON suppliers(business_id);

-- ============================
-- INVENTORY ITEMS
-- ============================
CREATE TABLE inventory_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id         UUID NOT NULL REFERENCES businesses(id),
    outlet_id           UUID REFERENCES outlets(id),
    supplier_id         UUID REFERENCES suppliers(id),
    name                VARCHAR(200) NOT NULL,
    sku                 VARCHAR(100),
    barcode             VARCHAR(100),
    unit                VARCHAR(20) NOT NULL DEFAULT 'kg', -- kg, g, L, ml, pcs, dozen
    category            VARCHAR(100),
    current_stock       NUMERIC(12, 3) NOT NULL DEFAULT 0,
    min_stock_level     NUMERIC(12, 3) NOT NULL DEFAULT 0,
    max_stock_level     NUMERIC(12, 3),
    reorder_point       NUMERIC(12, 3) NOT NULL DEFAULT 0,
    cost_price          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    last_purchase_price NUMERIC(10, 2),
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_business   ON inventory_items(business_id);
CREATE INDEX idx_inventory_outlet     ON inventory_items(outlet_id);
CREATE INDEX idx_inventory_low_stock  ON inventory_items(current_stock, reorder_point);

-- ============================
-- STOCK TRANSACTIONS
-- ============================
CREATE TABLE stock_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outlet_id       UUID NOT NULL REFERENCES outlets(id),
    item_id         UUID NOT NULL REFERENCES inventory_items(id),
    type            VARCHAR(20) NOT NULL, -- IN, OUT, ADJUSTMENT, TRANSFER, WASTE
    quantity        NUMERIC(12, 3) NOT NULL,
    balance_before  NUMERIC(12, 3) NOT NULL,
    balance_after   NUMERIC(12, 3) NOT NULL,
    reference       VARCHAR(200),
    reason          VARCHAR(200),
    notes           TEXT,
    performed_by    UUID REFERENCES users(id),
    transaction_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_txn_item    ON stock_transactions(item_id);
CREATE INDEX idx_stock_txn_outlet  ON stock_transactions(outlet_id);
CREATE INDEX idx_stock_txn_date    ON stock_transactions(transaction_at);

-- ============================
-- PURCHASE ORDERS
-- ============================
CREATE TABLE purchase_orders (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id       UUID NOT NULL REFERENCES businesses(id),
    outlet_id         UUID NOT NULL REFERENCES outlets(id),
    po_number         VARCHAR(50) NOT NULL,
    supplier_id       UUID NOT NULL REFERENCES suppliers(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, SENT, PARTIAL, RECEIVED, CANCELLED
    expected_delivery DATE,
    subtotal          NUMERIC(10, 2) NOT NULL DEFAULT 0,
    tax               NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total             NUMERIC(10, 2) NOT NULL DEFAULT 0,
    notes             TEXT,
    created_by        UUID REFERENCES users(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, po_number)
);

CREATE INDEX idx_po_business  ON purchase_orders(business_id);
CREATE INDEX idx_po_supplier  ON purchase_orders(supplier_id);
CREATE INDEX idx_po_status    ON purchase_orders(status);

-- ============================
-- PURCHASE ORDER ITEMS
-- ============================
CREATE TABLE po_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id         UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item_id       UUID NOT NULL REFERENCES inventory_items(id),
    ordered_qty   NUMERIC(12, 3) NOT NULL,
    received_qty  NUMERIC(12, 3) NOT NULL DEFAULT 0,
    unit_price    NUMERIC(10, 2) NOT NULL,
    tax_amount    NUMERIC(10, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_po_items_po ON po_items(po_id);

-- ============================
-- GOODS RECEIVED NOTES
-- ============================
CREATE TABLE goods_received_notes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id    UUID NOT NULL REFERENCES businesses(id),
    po_id          UUID REFERENCES purchase_orders(id),
    supplier_id    UUID NOT NULL REFERENCES suppliers(id),
    grn_number     VARCHAR(50) NOT NULL,
    received_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    invoice_number VARCHAR(100),
    notes          TEXT,
    received_by    UUID REFERENCES users(id),
    UNIQUE(business_id, grn_number)
);

CREATE TABLE grn_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grn_id          UUID NOT NULL REFERENCES goods_received_notes(id) ON DELETE CASCADE,
    po_item_id      UUID REFERENCES po_items(id),
    received_qty    NUMERIC(12, 3) NOT NULL,
    quality_status  VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED' -- ACCEPTED, REJECTED, PARTIAL
);

-- ============================
-- RECIPES (Bill of Materials)
-- ============================
CREATE TABLE recipes (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id  UUID NOT NULL REFERENCES businesses(id),
    menu_item_id UUID NOT NULL REFERENCES menu_items(id),
    name         VARCHAR(200),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(menu_item_id)
);

CREATE TABLE recipe_ingredients (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id        UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    quantity         NUMERIC(12, 3) NOT NULL,
    unit             VARCHAR(20)
);

CREATE INDEX idx_recipes_business  ON recipes(business_id);
CREATE INDEX idx_recipes_item      ON recipes(menu_item_id);
