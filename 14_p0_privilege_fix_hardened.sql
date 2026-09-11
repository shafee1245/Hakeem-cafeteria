-- ==========================================
-- PHASE 14: P0 PRIVILEGE FIX - HARDENED
-- ==========================================

-- 1. Replace the trigger function with hardened security and search_path
CREATE OR REPLACE FUNCTION public.prevent_student_privilege_escalation()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
DECLARE
    v_calling_role TEXT;
    v_is_admin BOOLEAN := false;
BEGIN
    -- 1. Identify the current execution context role
    -- PostgREST explicitly sets 'role' to 'authenticated', 'anon', or 'service_role'.
    -- Direct Postgres DB connections will return their login role (e.g., 'postgres').
    v_calling_role := current_setting('role', true);
    
    -- 2. Trusted internal superusers and service backends strictly bypass this protection
    IF v_calling_role IN ('service_role', 'postgres', 'supabase_admin') THEN
        RETURN NEW;
    END IF;

    -- 3. If an authenticated REST user is making the request, verify if they are genuinely an ADMIN
    IF v_calling_role = 'authenticated' AND auth.uid() IS NOT NULL THEN
        SELECT EXISTS (
            SELECT 1 FROM public.users 
            WHERE id = auth.uid() AND role = 'ADMIN'
        ) INTO v_is_admin;
    END IF;
    
    -- 4. If NOT an Admin, explicitly reject attempts to escalate or tamper with protected fields
    IF NOT v_is_admin THEN
        IF NEW.role IS DISTINCT FROM OLD.role THEN
            RAISE EXCEPTION 'Unauthorized modification of protected user field: role';
        END IF;
        
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'Unauthorized modification of protected user field: id';
        END IF;

        IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION 'Unauthorized modification of protected user field: created_at';
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$;

-- 2. Explicitly set the function owner to postgres (Locks down the SECURITY DEFINER context)
ALTER FUNCTION public.prevent_student_privilege_escalation() OWNER TO postgres;

-- 3. (Optional but safe) Reattach the trigger to ensure it's active
DROP TRIGGER IF EXISTS enforce_role_protection ON public.users;
CREATE TRIGGER enforce_role_protection
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.prevent_student_privilege_escalation();
