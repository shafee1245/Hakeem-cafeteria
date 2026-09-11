-- ==========================================
-- PHASE 2: ROW LEVEL SECURITY & AUTHORIZATION
-- ==========================================

-- 1. Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE settings ENABLE ROW LEVEL SECURITY;

-- 2. Clean up any existing unsafe policies
DROP POLICY IF EXISTS "Public Access" ON users;
DROP POLICY IF EXISTS "Public Access" ON products;
DROP POLICY IF EXISTS "Public Access" ON orders;
DROP POLICY IF EXISTS "Public Access" ON order_items;
DROP POLICY IF EXISTS "Public Access" ON campaigns;
DROP POLICY IF EXISTS "Public Access" ON settings;

-- 3. USERS Table Policies
-- Students can read and update their own profile
CREATE POLICY "Users can view own profile" 
ON users FOR SELECT 
USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" 
ON users FOR UPDATE 
USING (auth.uid() = id);

-- Admins can view and update all profiles
CREATE POLICY "Admins can view all profiles" 
ON users FOR SELECT 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

CREATE POLICY "Admins can update all profiles" 
ON users FOR UPDATE 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- Note: We allow INSERT into users for newly registered accounts
-- We must restrict this to only inserting their own UID
CREATE POLICY "Users can insert own profile" 
ON users FOR INSERT 
WITH CHECK (auth.uid() = id);

-- 4. PRODUCTS Table Policies
-- Anyone can view products
CREATE POLICY "Public can view products" 
ON products FOR SELECT 
USING (true);

-- Only admins can modify products
CREATE POLICY "Admins can modify products" 
ON products FOR ALL 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- 5. CAMPAIGNS Table Policies
-- Anyone can view campaigns
CREATE POLICY "Public can view campaigns" 
ON campaigns FOR SELECT 
USING (true);

-- Only admins can modify campaigns
CREATE POLICY "Admins can modify campaigns" 
ON campaigns FOR ALL 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- 6. SETTINGS Table Policies
-- Anyone can view settings
CREATE POLICY "Public can view settings" 
ON settings FOR SELECT 
USING (true);

-- Only admins can modify settings
CREATE POLICY "Admins can modify settings" 
ON settings FOR ALL 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- 7. ORDERS Table Policies
-- Users can view their own orders
CREATE POLICY "Users can view own orders" 
ON orders FOR SELECT 
USING (auth.uid() = user_id);

-- Admins can view all orders
CREATE POLICY "Admins can view all orders" 
ON orders FOR SELECT 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- Note: We DO NOT create an INSERT policy for orders!
-- Orders will be inserted via the place_order RPC with SECURITY DEFINER,
-- which bypasses RLS safely and strictly.
-- Admins can UPDATE order status
CREATE POLICY "Admins can update orders" 
ON orders FOR UPDATE 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- 8. ORDER ITEMS Table Policies
-- Users can view their own order items
CREATE POLICY "Users can view own order items" 
ON order_items FOR SELECT 
USING (
    EXISTS (
        SELECT 1 FROM orders 
        WHERE orders.id = order_items.order_id AND orders.user_id = auth.uid()
    )
);

-- Admins can view all order items
CREATE POLICY "Admins can view all order items" 
ON order_items FOR SELECT 
USING (
    EXISTS (SELECT 1 FROM users WHERE id = auth.uid() AND role = 'ADMIN')
);

-- Note: Like orders, order_items are INSERTED by the RPC, so no INSERT policy needed.
