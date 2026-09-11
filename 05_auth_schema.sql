-- ==========================================
-- PHASE 3: DATA CLEANUP, FCM TOKENS & IDEMPOTENCY
-- ==========================================

-- 1. Create FCM Tokens table
CREATE TABLE IF NOT EXISTS fcm_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL,
    created_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    updated_at BIGINT DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
    UNIQUE(user_id, token)
);

-- 2. Add idempotency_key to orders table
ALTER TABLE orders 
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255) UNIQUE;

-- 3. Create necessary performance indexes
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(order_status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user_id ON fcm_tokens(user_id);

-- 4. Create an RPC to safely look up email by register number
-- This allows students to login using their register number while RLS restricts public.users
CREATE OR REPLACE FUNCTION get_email_by_reg(p_reg VARCHAR)
RETURNS TEXT 
LANGUAGE plpgsql 
SECURITY DEFINER 
SET search_path = public
AS $$
DECLARE
    v_email TEXT;
BEGIN
    SELECT email INTO v_email FROM users WHERE registerNumber = p_reg LIMIT 1;
    RETURN v_email;
END;
$$;
