-- ==========================================
-- SECURE STORAGE POLICIES
-- RUN THIS SCRIPT IN SUPABASE SQL EDITOR
-- ==========================================

-- Ensure the bucket exists (in case it doesn't)
INSERT INTO storage.buckets (id, name, public) 
VALUES ('images', 'images', true)
ON CONFLICT (id) DO NOTHING;

-- 1. Drop existing permissive policies
DROP POLICY IF EXISTS "Public Access" ON storage.objects;
DROP POLICY IF EXISTS "Auth Upload" ON storage.objects;
DROP POLICY IF EXISTS "Auth Update" ON storage.objects;
DROP POLICY IF EXISTS "Auth Delete" ON storage.objects;

-- 2. Allow PUBLIC to SELECT (read) images
CREATE POLICY "Public Read Access" 
ON storage.objects FOR SELECT 
USING (bucket_id = 'images');

-- 3. Allow only ADMIN to INSERT (upload)
CREATE POLICY "Admin Upload Access" 
ON storage.objects FOR INSERT 
WITH CHECK (
    bucket_id = 'images' AND 
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE users.id = auth.uid() 
        AND users.role = 'ADMIN'
    )
);

-- 4. Allow only ADMIN to UPDATE
CREATE POLICY "Admin Update Access" 
ON storage.objects FOR UPDATE 
USING (
    bucket_id = 'images' AND 
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE users.id = auth.uid() 
        AND users.role = 'ADMIN'
    )
);

-- 5. Allow only ADMIN to DELETE
CREATE POLICY "Admin Delete Access" 
ON storage.objects FOR DELETE 
USING (
    bucket_id = 'images' AND 
    EXISTS (
        SELECT 1 FROM public.users 
        WHERE users.id = auth.uid() 
        AND users.role = 'ADMIN'
    )
);
