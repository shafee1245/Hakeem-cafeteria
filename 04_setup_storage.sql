-- ==========================================
-- HAKEEM CAFETERIA STORAGE SETUP
-- RUN THIS ENTIRE SCRIPT IN SUPABASE SQL EDITOR
-- ==========================================

-- 1. Create the 'images' bucket if it doesn't exist, and make it public
INSERT INTO storage.buckets (id, name, public) 
VALUES ('images', 'images', true)
ON CONFLICT (id) DO NOTHING;

-- 2. Create permissive policies so the app can upload and read images freely
-- Note: 'storage.objects' is the table where all files are stored.

-- Drop old policies if they exist to avoid duplicate errors
DROP POLICY IF EXISTS "Public Access" ON storage.objects;
DROP POLICY IF EXISTS "Auth Upload" ON storage.objects;

-- Allow anyone to view images
CREATE POLICY "Public Access" 
ON storage.objects FOR SELECT 
USING (bucket_id = 'images');

-- Allow anyone to upload images (bypasses auth for now)
CREATE POLICY "Auth Upload" 
ON storage.objects FOR INSERT 
WITH CHECK (bucket_id = 'images');

-- Allow anyone to update images
CREATE POLICY "Auth Update" 
ON storage.objects FOR UPDATE 
USING (bucket_id = 'images');

-- Allow anyone to delete images
CREATE POLICY "Auth Delete" 
ON storage.objects FOR DELETE 
USING (bucket_id = 'images');
