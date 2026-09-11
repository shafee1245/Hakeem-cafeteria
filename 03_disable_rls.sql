-- ==========================================
-- HAKEEM CAFETERIA SECURITY SYNC
-- DISABLE RLS TO ALLOW APP INSERTS
-- RUN THIS ENTIRE SCRIPT IN SUPABASE SQL EDITOR
-- ==========================================

-- Disable Row Level Security (RLS) on all core tables to allow unrestricted access 
-- from the application for inserting products, campaigns, orders, etc.
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE products DISABLE ROW LEVEL SECURITY;
ALTER TABLE categories DISABLE ROW LEVEL SECURITY;
ALTER TABLE orders DISABLE ROW LEVEL SECURITY;
ALTER TABLE order_items DISABLE ROW LEVEL SECURITY;
ALTER TABLE campaigns DISABLE ROW LEVEL SECURITY;
ALTER TABLE settings DISABLE ROW LEVEL SECURITY;
