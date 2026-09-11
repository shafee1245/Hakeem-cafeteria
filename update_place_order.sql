CREATE OR REPLACE FUNCTION public.place_order(p_cart_items jsonb, p_idempotency_key text, p_payment_method text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_user_id UUID;
    v_user_name TEXT;
    v_user_register_number TEXT;
    v_order_id UUID;
    v_subtotal DOUBLE PRECISION := 0;
    v_total DOUBLE PRECISION := 0;
    v_item RECORD;
    v_product RECORD;
    v_existing_order UUID;
    
    v_today DATE;
    v_next_token INTEGER;
    v_token_string VARCHAR(50);
    v_settings RECORD;
BEGIN
    -- 1. Identify the authenticated user
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized: User must be logged in to place an order.';
    END IF;

    -- Fetch user profile data
    SELECT full_name, register_number INTO v_user_name, v_user_register_number
    FROM public.users WHERE id = v_user_id;

    IF v_user_name IS NULL THEN
        RAISE EXCEPTION 'User profile not found or incomplete.';
    END IF;

    -- 1.5. Check Emergency Shutdown and Booking Status
    SELECT * INTO v_settings FROM public.settings LIMIT 1;
    IF FOUND THEN
        IF v_settings.is_emergency_closed = true THEN
            RAISE EXCEPTION 'Cafeteria ordering is temporarily closed.';
        END IF;
        IF v_settings.is_booking_enabled = false THEN
            RAISE EXCEPTION 'Ordering is currently closed.';
        END IF;
    END IF;

    -- Validate payment method
    IF p_payment_method NOT IN ('CASH', 'ONLINE') THEN
        RAISE EXCEPTION 'Invalid payment method: %', p_payment_method;
    END IF;

    -- 2. Fast-path Idempotency Check
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
            user_full_name,
            user_register_number,
            subtotal,
            total,
            order_status,
            payment_status,
            payment_method,
            idempotency_key,
            token_number
        ) VALUES (
            v_user_id,
            v_user_name,
            v_user_register_number,
            v_subtotal,
            v_total,
            'PLACED',
            'PENDING',
            p_payment_method,
            p_idempotency_key,
            v_token_string
        ) RETURNING id INTO v_order_id;
    EXCEPTION WHEN unique_violation THEN
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
