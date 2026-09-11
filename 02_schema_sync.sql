-- ==========================================
-- HAKEEM CAFETERIA SCHEMA SYNC SCRIPT
-- RUN THIS ENTIRE SCRIPT IN SUPABASE SQL EDITOR
-- ==========================================

-- 1. FIX PRODUCTS TABLE
ALTER TABLE products 
    ADD COLUMN IF NOT EXISTS is_veg BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS availability VARCHAR(50) DEFAULT 'AVAILABLE',
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN DEFAULT false;

-- Safely convert created_at to BIGINT if it's currently a TIMESTAMP
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'products' AND column_name = 'created_at' AND data_type LIKE 'timestamp%'
    ) THEN
        ALTER TABLE products ALTER COLUMN created_at DROP DEFAULT;
        ALTER TABLE products ALTER COLUMN created_at TYPE BIGINT USING (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT;
    END IF;
END $$;

-- Drop deprecated columns
ALTER TABLE products DROP COLUMN IF EXISTS is_available;

-- 2. FIX ORDERS TABLE
ALTER TABLE orders 
    ADD COLUMN IF NOT EXISTS user_full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS user_register_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS subtotal DOUBLE PRECISION DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS total DOUBLE PRECISION DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) DEFAULT 'PAID',
    ADD COLUMN IF NOT EXISTS order_status VARCHAR(50) DEFAULT 'PLACED';

-- Safely convert created_at to BIGINT if it's currently a TIMESTAMP
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'orders' AND column_name = 'created_at' AND data_type LIKE 'timestamp%'
    ) THEN
        ALTER TABLE orders ALTER COLUMN created_at DROP DEFAULT;
        ALTER TABLE orders ALTER COLUMN created_at TYPE BIGINT USING (EXTRACT(EPOCH FROM created_at) * 1000)::BIGINT;
    END IF;
END $$;

-- Drop deprecated columns
ALTER TABLE orders 
    DROP COLUMN IF EXISTS total_amount,
    DROP COLUMN IF EXISTS status;

-- 3. FIX ORDER ITEMS TABLE
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS product_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS product_price DOUBLE PRECISION;

-- Drop deprecated columns
ALTER TABLE order_items DROP COLUMN IF EXISTS price_at_time;

-- 4. FIX CAMPAIGNS (ADVERTISEMENTS) TABLE
-- First ensure the table is called campaigns
CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4()
);

ALTER TABLE campaigns
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS title VARCHAR(255) NOT NULL DEFAULT 'Untitled',
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS banner_url TEXT,
    ADD COLUMN IF NOT EXISTS cta_text VARCHAR(100),
    ADD COLUMN IF NOT EXISTS redirect_url TEXT,
    ADD COLUMN IF NOT EXISTS start_date BIGINT,
    ADD COLUMN IF NOT EXISTS end_date BIGINT,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS impressions INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS clicks INTEGER DEFAULT 0;

-- 5. FIX SETTINGS TABLE
ALTER TABLE settings
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    ADD COLUMN IF NOT EXISTS active_days VARCHAR(255) DEFAULT 'Mon,Tue,Wed,Thu,Fri,Sat';

-- Drop any legacy advertisements table if it exists as we now strictly use 'campaigns'
DROP TABLE IF EXISTS advertisements;
