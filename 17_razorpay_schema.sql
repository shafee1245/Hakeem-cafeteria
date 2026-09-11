-- ==========================================
-- PHASE 17: RAZORPAY PAYMENT SCHEMA
-- ==========================================

-- 1. Add Razorpay tracking columns to orders table
ALTER TABLE orders 
    ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS razorpay_signature VARCHAR(255);

-- 2. Create or Replace place_order to set payment_status to PENDING
CREATE OR REPLACE FUNCTION place_order(
    p_cart_items JSONB,
    p_idempotency_key VARCHAR
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
    v_user_id UUID;
    v_order_id UUID;
    v_subtotal DOUBLE PRECISION := 0;
    v_total DOUBLE PRECISION := 0;
    v_item RECORD;
    v_product RECORD;
    v_existing_order UUID;
    
    v_today DATE;
    v_next_token INTEGER;
    v_token_string VARCHAR(50);
BEGIN
    -- 1. Identify the authenticated user
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized: User must be logged in to place an order.';
    END IF;

    -- 2. Fast-path Idempotency Check (saves DB cycles if request was fully processed earlier)
    SELECT id INTO v_existing_order FROM orders WHERE idempotency_key = p_idempotency_key;
    IF v_existing_order IS NOT NULL THEN
        RETURN jsonb_build_object(
            'status', 'success',
            'message', 'Order already processed',
            'order_id', v_existing_order
        );
    END IF;

    -- 3. Calculate Subtotal Server-Side by looping through cart items
    FOR v_item IN SELECT * FROM jsonb_to_recordset(p_cart_items) AS x(product_id UUID, quantity INT)
    LOOP
        SELECT * INTO v_product FROM products WHERE id = v_item.product_id;
        
        IF NOT FOUND THEN
            RAISE EXCEPTION 'Product with ID % does not exist.', v_item.product_id;
        END IF;

        IF v_product.availability != 'AVAILABLE' THEN
            RAISE EXCEPTION 'Product % is currently unavailable.', v_product.name;
        END IF;

        IF v_item.quantity <= 0 THEN
            RAISE EXCEPTION 'Invalid quantity for product %.', v_product.name;
        END IF;

        v_subtotal := v_subtotal + (v_product.price * v_item.quantity);
    END LOOP;

    v_total := v_subtotal;

    -- 4. Generate Daily Token (Concurrency Safe & Asia/Kolkata Timezone)
    -- We extract the date based on India standard time to handle rollover properly
    v_today := (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Kolkata')::DATE;
    
    INSERT INTO public.daily_token_counter (date_key, last_token)
    VALUES (v_today, 1)
    ON CONFLICT (date_key) 
    DO UPDATE SET last_token = daily_token_counter.last_token + 1
    RETURNING last_token INTO v_next_token;

    -- Format token, e.g., 'A041'
    v_token_string := 'A' || LPAD(v_next_token::TEXT, 3, '0');

    -- 5. Insert the Order inside an Exception Block for Race Conditions
    BEGIN
        INSERT INTO orders (
            user_id,
            subtotal,
            total,
            order_status,
            payment_status,
            payment_method,
            idempotency_key,
            token_number
        ) VALUES (
            v_user_id,
            v_subtotal,
            v_total,
            'PLACED',
            'PENDING',    -- Changed from PAID to PENDING for Razorpay integration
            'ONLINE',
            p_idempotency_key,
            v_token_string
        ) RETURNING id INTO v_order_id;
    EXCEPTION WHEN unique_violation THEN
        -- Race condition: another concurrent transaction just inserted this key
        SELECT id INTO v_existing_order FROM orders WHERE idempotency_key = p_idempotency_key;
        
        -- Safe fallback: we return the existing order
        RETURN jsonb_build_object(
            'status', 'success',
            'message', 'Order already processed (concurrent)',
            'order_id', v_existing_order
        );
    END;

    -- 6. Insert all Order Items
    FOR v_item IN SELECT * FROM jsonb_to_recordset(p_cart_items) AS x(product_id UUID, quantity INT)
    LOOP
        SELECT * INTO v_product FROM products WHERE id = v_item.product_id;

        INSERT INTO order_items (
            order_id,
            product_id,
            quantity,
            product_name,
            product_price
        ) VALUES (
            v_order_id,
            v_item.product_id,
            v_item.quantity,
            v_product.name,
            v_product.price
        );
    END LOOP;

    -- 7. Return success
    RETURN jsonb_build_object(
        'status', 'success',
        'order_id', v_order_id,
        'total', v_total
    );

EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'Checkout failed: %', SQLERRM;
END;
$$;

-- Preserve Phase 13 privilege hardening
ALTER FUNCTION public.place_order(JSONB, VARCHAR) OWNER TO postgres;
REVOKE EXECUTE ON FUNCTION public.place_order(JSONB, VARCHAR) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.place_order(JSONB, VARCHAR) FROM anon;
GRANT EXECUTE ON FUNCTION public.place_order(JSONB, VARCHAR) TO authenticated;
