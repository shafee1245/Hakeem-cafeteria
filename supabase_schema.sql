-- Supabase Database Schema for Hakeem Cafeteria
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    register_number VARCHAR(100) UNIQUE NOT NULL,
    department VARCHAR(255),
    year VARCHAR(50),
    phone VARCHAR(50),
    role VARCHAR(50) DEFAULT 'STUDENT',
    password_hash VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    price DECIMAL(10, 2) NOT NULL,
    image_url TEXT,
    is_veg BOOLEAN DEFAULT true,
    availability VARCHAR(50) DEFAULT 'AVAILABLE',
    is_featured BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL,
    icon_name VARCHAR(100),
    sort_order INTEGER DEFAULT 0
);

-- ORDERS TABLE
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    user_full_name VARCHAR(255),
    user_register_number VARCHAR(100),
    token_number VARCHAR(50) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'ONLINE',
    payment_status VARCHAR(50) DEFAULT 'PAID',
    order_status VARCHAR(50) DEFAULT 'PLACED', -- PLACED, ACCEPTED, DELIVERED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ORDER ITEMS TABLE
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    product_name VARCHAR(255),
    product_price DECIMAL(10, 2),
    quantity INTEGER NOT NULL
);

-- CAMPAIGNS (ADVERTISEMENTS) TABLE
CREATE TABLE IF NOT EXISTS campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_name VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    banner_url TEXT,
    cta_text VARCHAR(100),
    redirect_url TEXT,
    start_date BIGINT,
    end_date BIGINT,
    is_active BOOLEAN DEFAULT true,
    impressions INTEGER DEFAULT 0,
    clicks INTEGER DEFAULT 0
);

-- SETTINGS TABLE
CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY DEFAULT 1,
    open_time VARCHAR(10) DEFAULT '09:00 AM',
    close_time VARCHAR(10) DEFAULT '11:40 AM',
    timezone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    active_days VARCHAR(255) DEFAULT 'Mon,Tue,Wed,Thu,Fri,Sat',
    is_booking_enabled BOOLEAN DEFAULT true,
    is_emergency_closed BOOLEAN DEFAULT false,
    emergency_closure_reason TEXT
);

-- INITIAL SETTINGS INSERT
INSERT INTO settings (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

-- RLS POLICIES (Row Level Security)
-- You can configure these in the Supabase Dashboard. 
-- For now, disable RLS to allow the app to function quickly while migrating.
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE products DISABLE ROW LEVEL SECURITY;
ALTER TABLE categories DISABLE ROW LEVEL SECURITY;
ALTER TABLE orders DISABLE ROW LEVEL SECURITY;
ALTER TABLE order_items DISABLE ROW LEVEL SECURITY;
ALTER TABLE campaigns DISABLE ROW LEVEL SECURITY;
ALTER TABLE settings DISABLE ROW LEVEL SECURITY;
