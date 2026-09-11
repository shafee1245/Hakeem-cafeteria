-- ==========================================
-- PHASE 13: RPC LEAST PRIVILEGE FIX
-- ==========================================

-- 1. Explicitly assign ownership to the postgres superuser (best practice)
ALTER FUNCTION public.place_order(JSONB, VARCHAR) OWNER TO postgres;

-- 2. Revoke default execution privileges from PUBLIC
-- This prevents the function from being broadly accessible to any role
REVOKE EXECUTE ON FUNCTION public.place_order(JSONB, VARCHAR) FROM PUBLIC;

-- 3. Explicitly revoke execution from anon
-- Unauthenticated users hitting the REST API will be blocked by PostgreSQL entirely,
-- rather than by our internal auth.uid() check
REVOKE EXECUTE ON FUNCTION public.place_order(JSONB, VARCHAR) FROM anon;

-- 4. Grant execution exclusively to authenticated users
GRANT EXECUTE ON FUNCTION public.place_order(JSONB, VARCHAR) TO authenticated;
