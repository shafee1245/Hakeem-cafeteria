-- ==========================================
-- PHASE 10: IDEMPOTENCY & CONCURRENCY FIX
-- ==========================================

-- 1. Backfill any existing rows missing an idempotency key (just in case)
UPDATE orders 
SET idempotency_key = uuid_generate_v4()::text 
WHERE idempotency_key IS NULL;

-- 2. Enforce NOT NULL on the column
ALTER TABLE orders ALTER COLUMN idempotency_key SET NOT NULL;

-- 3. Create explicit UNIQUE constraint which generates a dedicated index
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_idempotency_key_key;
ALTER TABLE orders ADD CONSTRAINT orders_idempotency_key_key UNIQUE (idempotency_key);


-- 4. Replace the place_order RPC to use database-level concurrency protection
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
    -- We parse the JSONB array of cart items
    FOR v_item IN SELECT * FROM jsonb_to_recordset(p_cart_items) AS x(product_id UUID, quantity INT)
    LOOP
        -- Look up the authoritative product price
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

        -- Accumulate subtotal
        v_subtotal := v_subtotal + (v_product.price * v_item.quantity);
    END LOOP;

    -- 4. Calculate final total (for now, same as subtotal, but leaves room for taxes/discounts)
    v_total := v_subtotal;

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
            'PAID',
            'CASH',
            p_idempotency_key,
            UPPER(SUBSTRING(uuid_generate_v4()::text FROM 1 FOR 6))
        ) RETURNING id INTO v_order_id;
    EXCEPTION WHEN unique_violation THEN
        -- Race condition: another concurrent transaction just inserted this key
        SELECT id INTO v_existing_order FROM orders WHERE idempotency_key = p_idempotency_key;
        RETURN jsonb_build_object(
            'status', 'success',
            'message', 'Order already processed (concurrent)',
            'order_id', v_existing_order
        );
    END;

    -- 6. Insert all Order Items
    FOR v_item IN SELECT * FROM jsonb_to_recordset(p_cart_items) AS x(product_id UUID, quantity INT)
    LOOP
        -- We look up the product again (cached in memory mostly)
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
