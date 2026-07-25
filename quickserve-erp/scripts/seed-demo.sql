-- seed-demo.sql — Demo data for QuickServe ERP onboarding
-- Run: psql -U quickserve -d quickserve -f scripts/seed-demo.sql

BEGIN;

-- Insert demo restaurant business
INSERT INTO businesses (id, name, business_type, status, currency_code, timezone, gst_inclusive)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Demo Restaurant',
    'RESTAURANT',
    'ACTIVE',
    'INR',
    'Asia/Kolkata',
    false
) ON CONFLICT DO NOTHING;

-- Insert demo outlet
INSERT INTO outlets (id, business_id, name, outlet_type, phone, city, state, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'Demo Restaurant - Main Branch',
    'DINE_IN',
    '9876543210',
    'Bengaluru',
    'Karnataka',
    true
) ON CONFLICT DO NOTHING;

-- Insert demo tables T1-T10
DO $$
BEGIN
    FOR i IN 1..10 LOOP
        INSERT INTO tables (outlet_id, name, capacity, status)
        VALUES ('00000000-0000-0000-0000-000000000002', 'T' || i, 4, 'AVAILABLE')
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

-- Tax slabs for demo business
INSERT INTO tax_slabs (business_id, name, percentage) VALUES
    ('00000000-0000-0000-0000-000000000001', 'GST 0%',  0),
    ('00000000-0000-0000-0000-000000000001', 'GST 5%',  5),
    ('00000000-0000-0000-0000-000000000001', 'GST 12%', 12),
    ('00000000-0000-0000-0000-000000000001', 'GST 18%', 18),
    ('00000000-0000-0000-0000-000000000001', 'GST 28%', 28)
ON CONFLICT DO NOTHING;

-- Demo menu categories
INSERT INTO categories (business_id, outlet_id, name, sort_order) VALUES
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Starters',     1),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Main Course',  2),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Beverages',    3),
    ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Desserts',     4)
ON CONFLICT DO NOTHING;

-- Demo menu items
DO $$
DECLARE
    cat_starter   UUID;
    cat_main      UUID;
    cat_bev       UUID;
    tax_5pct      UUID;
    tax_18pct     UUID;
BEGIN
    SELECT id INTO cat_starter FROM categories WHERE business_id='00000000-0000-0000-0000-000000000001' AND name='Starters';
    SELECT id INTO cat_main    FROM categories WHERE business_id='00000000-0000-0000-0000-000000000001' AND name='Main Course';
    SELECT id INTO cat_bev     FROM categories WHERE business_id='00000000-0000-0000-0000-000000000001' AND name='Beverages';
    SELECT id INTO tax_5pct    FROM tax_slabs  WHERE business_id='00000000-0000-0000-0000-000000000001' AND percentage=5;
    SELECT id INTO tax_18pct   FROM tax_slabs  WHERE business_id='00000000-0000-0000-0000-000000000001' AND percentage=18;

    INSERT INTO menu_items (business_id, outlet_id, category_id, tax_slab_id, name, description, base_price, is_veg) VALUES
        ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', cat_starter, tax_5pct,  'Paneer Tikka',          'Grilled cottage cheese with spices',       280, true),
        ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', cat_main,    tax_5pct,  'Paneer Butter Masala',  'Rich creamy tomato gravy with paneer',     320, true),
        ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', cat_main,    tax_5pct,  'Dal Tadka',             'Yellow lentils tempered with ghee',        180, true),
        ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', cat_bev,     tax_18pct, 'Fresh Lime Soda',       'Refreshing lime with soda',                 80, true),
        ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', cat_bev,     tax_18pct, 'Mango Lassi',           'Sweet yogurt drink with mango',            120, true)
    ON CONFLICT DO NOTHING;
END $$;

COMMIT;

\echo '✅ Demo data seeded successfully!'
