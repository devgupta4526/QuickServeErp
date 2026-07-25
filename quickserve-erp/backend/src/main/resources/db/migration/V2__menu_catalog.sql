-- V2__menu_catalog.sql
-- Menu categories, items, variants, addons, tax slabs

-- ============================
-- TAX SLABS
-- ============================
CREATE TABLE tax_slabs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    name        VARCHAR(100) NOT NULL,
    percentage  NUMERIC(5, 2) NOT NULL,
    hsn_code    VARCHAR(10),
    sac_code    VARCHAR(10),
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(business_id, percentage)
);

CREATE INDEX idx_tax_slabs_business ON tax_slabs(business_id);

-- ============================
-- MENU CATEGORIES
-- ============================
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id),
    outlet_id   UUID REFERENCES outlets(id),
    name        VARCHAR(100) NOT NULL,
    image_url   TEXT,
    sort_order  INT NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_business ON categories(business_id);
CREATE INDEX idx_categories_outlet   ON categories(outlet_id);

-- ============================
-- MENU ITEMS
-- ============================
CREATE TABLE menu_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id      UUID NOT NULL REFERENCES businesses(id),
    outlet_id        UUID REFERENCES outlets(id),
    category_id      UUID NOT NULL REFERENCES categories(id),
    tax_slab_id      UUID REFERENCES tax_slabs(id),
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    base_price       NUMERIC(10, 2) NOT NULL,
    image_url        TEXT,
    is_veg           BOOLEAN NOT NULL DEFAULT true,
    is_available     BOOLEAN NOT NULL DEFAULT true,
    is_archived      BOOLEAN NOT NULL DEFAULT false,
    preparation_time INT,   -- minutes
    calories         INT,
    sort_order       INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_menu_items_business  ON menu_items(business_id);
CREATE INDEX idx_menu_items_category  ON menu_items(category_id);
CREATE INDEX idx_menu_items_available ON menu_items(is_available, is_archived);

-- ============================
-- MENU ITEM VARIANTS
-- ============================
CREATE TABLE menu_item_variants (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id   UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    name           VARCHAR(100) NOT NULL,  -- Small, Medium, Large
    price_modifier NUMERIC(10, 2) NOT NULL DEFAULT 0,
    is_active      BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_variants_item ON menu_item_variants(menu_item_id);

-- ============================
-- MENU ITEM ADDONS
-- ============================
CREATE TABLE menu_item_addons (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,  -- Extra cheese, Extra sauce
    price        NUMERIC(10, 2) NOT NULL DEFAULT 0,
    is_required  BOOLEAN NOT NULL DEFAULT false,
    is_active    BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_addons_item ON menu_item_addons(menu_item_id);

-- ============================
-- TABLE SECTIONS & TABLES
-- ============================
CREATE TABLE table_sections (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outlet_id UUID NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    name      VARCHAR(100) NOT NULL,  -- Indoor, Outdoor, Rooftop
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE tables (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outlet_id   UUID NOT NULL REFERENCES outlets(id) ON DELETE CASCADE,
    section_id  UUID REFERENCES table_sections(id),
    name        VARCHAR(50) NOT NULL,  -- T1, T2, A1, etc.
    capacity    INT NOT NULL DEFAULT 4,
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, OCCUPIED, RESERVED, CLEANING
    qr_code_url TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tables_outlet  ON tables(outlet_id);
CREATE INDEX idx_tables_status  ON tables(status);

-- ============================
-- TABLE RESERVATIONS
-- ============================
CREATE TABLE table_reservations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outlet_id    UUID NOT NULL REFERENCES outlets(id),
    table_id     UUID REFERENCES tables(id),
    customer_id  UUID,
    guest_name   VARCHAR(200) NOT NULL,
    guest_phone  VARCHAR(20) NOT NULL,
    party_size   INT NOT NULL DEFAULT 2,
    reserved_for TIMESTAMPTZ NOT NULL,
    duration     INT NOT NULL DEFAULT 60, -- minutes
    status       VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, SEATED, COMPLETED, CANCELLED, NO_SHOW
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reservations_outlet  ON table_reservations(outlet_id);
CREATE INDEX idx_reservations_table   ON table_reservations(table_id);
CREATE INDEX idx_reservations_time    ON table_reservations(reserved_for);
