-- ==========================================
-- PHASE 11: P0 PRIVILEGE ESCALATION FIX
-- ==========================================

-- 1. Safely remove the deprecated password_hash column from public schema.
-- Since Supabase Auth manages passwords in the auth.users schema, 
-- this column in public.users is completely unused and poses a theoretical confusion risk.
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;

-- 2. Create a robust BEFORE UPDATE trigger function to protect security columns.
-- This guarantees that even if an RLS policy permits an UPDATE to a row,
-- unauthorized roles absolutely cannot manipulate critical fields.
CREATE OR REPLACE FUNCTION prevent_student_privilege_escalation()
RETURNS trigger AS $$
BEGIN
    -- Evaluate if the authenticated user performing the action is an ADMIN.
    -- We use a direct SELECT here because auth.uid() is the session token.
    IF NOT EXISTS (SELECT 1 FROM public.users WHERE id = auth.uid() AND role = 'ADMIN') THEN
        
        -- If NOT an admin (e.g., a STUDENT), forcefully revert critical fields to their existing OLD values.
        -- This silently ignores malicious JSON payloads like {"role": "ADMIN"}.
        NEW.id := OLD.id;
        NEW.role := OLD.role;
        NEW.created_at := OLD.created_at;
        
        -- Students are only allowed to modify non-critical fields (full_name, phone, department, etc.)
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Attach the trigger to the users table.
-- It fires before any update, acting as a final column-level security gate.
DROP TRIGGER IF EXISTS enforce_role_protection ON users;
CREATE TRIGGER enforce_role_protection
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION prevent_student_privilege_escalation();
