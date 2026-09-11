-- ==========================================
-- HAKEEM CAFETERIA SUPABASE SCHEMA SETUP
-- ==========================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    register_number VARCHAR(100) UNIQUE,
    department VARCHAR(255),
    year VARCHAR(50),
    phone VARCHAR(50),
    role VARCHAR(50) DEFAULT 'STUDENT',
    password_hash VARCHAR(255),
    created_at BIGINT
);

-- Note: In previous steps we dropped the NOT NULL constraints. 
-- If creating fresh, ensure they are nullable as above.

-- 2. PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE PRECISION NOT NULL,
    category VARCHAR(100) NOT NULL,
    image_url TEXT,
    is_available BOOLEAN DEFAULT TRUE,
    created_at BIGINT
);
-- Note: 'initial_stock' column can be removed or ignored.

-- 3. ORDERS TABLE
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    total_amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    payment_method VARCHAR(50) DEFAULT 'CASH',
    created_at BIGINT,
    token_number VARCHAR(50)
);
-- Note: wallet payment and refund/cancel status logic will be removed from app.

-- 4. ORDER ITEMS TABLE
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    quantity INTEGER NOT NULL,
    price_at_time DOUBLE PRECISION NOT NULL
);

-- 5. ADVERTISEMENTS (CAMPAIGNS) TABLE
CREATE TABLE IF NOT EXISTS advertisements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    image_url TEXT NOT NULL,
    target_url TEXT, -- NEW FIELD FOR URL REDIRECTION
    is_active BOOLEAN DEFAULT TRUE,
    created_at BIGINT
);

-- 6. SETTINGS (PRE-BOOKING RULES) TABLE
CREATE TABLE IF NOT EXISTS settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    is_booking_enabled BOOLEAN DEFAULT TRUE,
    open_time VARCHAR(50),
    close_time VARCHAR(50),
    is_emergency_closed BOOLEAN DEFAULT FALSE,
    emergency_closure_reason TEXT
);

-- 7. SUPABASE STORAGE SETUP
-- Run this to create the bucket for Product and Advertisement Images
INSERT INTO storage.buckets (id, name, public) 
VALUES ('images', 'images', true)
ON CONFLICT (id) DO NOTHING;

-- Allow public access to read files in the 'images' bucket
CREATE POLICY "Public Access" 
ON storage.objects FOR SELECT 
USING (bucket_id = 'images');

-- Allow authenticated users to upload files to 'images' bucket
CREATE POLICY "Auth Upload" 
ON storage.objects FOR INSERT 
WITH CHECK (bucket_id = 'images' AND auth.role() = 'authenticated');

-- Allow authenticated users to delete files in 'images' bucket
CREATE POLICY "Auth Delete" 
ON storage.objects FOR DELETE 
USING (bucket_id = 'images' AND auth.role() = 'authenticated');
