-- ==========================================
-- PHASE 15: HARDENED RLS RECURSION FIX
-- ==========================================

-- 1. Create a strict SECURITY DEFINER helper to break the RLS recursion loop
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = pg_catalog, public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.users 
        WHERE id = auth.uid() AND role = 'ADMIN'
    );
$$;

-- 2. Lock down the execution context to the Postgres superuser
ALTER FUNCTION public.is_admin() OWNER TO postgres;

-- 3. Enforce Least Privilege for Execution
REVOKE EXECUTE ON FUNCTION public.is_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_admin() TO authenticated;


-- ==========================================
-- REFACTOR RECURSIVE POLICIES
-- ==========================================

-- public.users
DROP POLICY IF EXISTS "Admins can view all profiles" ON public.users;
CREATE POLICY "Admins can view all profiles" 
ON public.users FOR SELECT 
USING (public.is_admin());

DROP POLICY IF EXISTS "Admins can update all profiles" ON public.users;
CREATE POLICY "Admins can update all profiles" 
ON public.users FOR UPDATE 
USING (public.is_admin());


-- public.products
DROP POLICY IF EXISTS "Admins can modify products" ON public.products;
CREATE POLICY "Admins can modify products" 
ON public.products FOR ALL 
USING (public.is_admin())
WITH CHECK (public.is_admin());


-- public.campaigns
DROP POLICY IF EXISTS "Admins can modify campaigns" ON public.campaigns;
CREATE POLICY "Admins can modify campaigns" 
ON public.campaigns FOR ALL 
USING (public.is_admin())
WITH CHECK (public.is_admin());


-- public.settings
DROP POLICY IF EXISTS "Admins can modify settings" ON public.settings;
CREATE POLICY "Admins can modify settings" 
ON public.settings FOR ALL 
USING (public.is_admin())
WITH CHECK (public.is_admin());


-- public.orders
DROP POLICY IF EXISTS "Admins can view all orders" ON public.orders;
CREATE POLICY "Admins can view all orders" 
ON public.orders FOR SELECT 
USING (public.is_admin());

DROP POLICY IF EXISTS "Admins can update orders" ON public.orders;
CREATE POLICY "Admins can update orders" 
ON public.orders FOR UPDATE 
USING (public.is_admin());


-- public.order_items
DROP POLICY IF EXISTS "Admins can view all order items" ON public.order_items;
CREATE POLICY "Admins can view all order items" 
ON public.order_items FOR SELECT 
USING (public.is_admin());


-- storage.objects (Supabase Storage)
DROP POLICY IF EXISTS "Admin Upload Access" ON storage.objects;
CREATE POLICY "Admin Upload Access" 
ON storage.objects FOR INSERT 
WITH CHECK (bucket_id = 'images' AND public.is_admin());

DROP POLICY IF EXISTS "Admin Update Access" ON storage.objects;
CREATE POLICY "Admin Update Access" 
ON storage.objects FOR UPDATE 
USING (bucket_id = 'images' AND public.is_admin());

DROP POLICY IF EXISTS "Admin Delete Access" ON storage.objects;
CREATE POLICY "Admin Delete Access" 
ON storage.objects FOR DELETE 
USING (bucket_id = 'images' AND public.is_admin());
