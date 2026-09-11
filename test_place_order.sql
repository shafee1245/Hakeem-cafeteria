BEGIN;
SET LOCAL role authenticated;
SELECT set_config('request.jwt.claims', '{"sub": "a1b0b9e2-29af-4336-bcca-4224e5ce8275", "role": "authenticated"}', true);
SELECT public.place_order('[{"product_id": "8a2606ef-4e39-47cc-a8d9-2382508ea2c8", "quantity": 1}]'::jsonb, 'test_idempotency_1', 'CASH');
COMMIT;
