-- ==========================================
-- PHASE 12: FCM TOKENS RLS POLICIES
-- ==========================================

-- 1. Ensure RLS is fully enabled (in case it wasn't)
ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

-- 2. Drop existing policies if any
DROP POLICY IF EXISTS "Users can insert own FCM token" ON public.fcm_tokens;
DROP POLICY IF EXISTS "Users can update own FCM token" ON public.fcm_tokens;
DROP POLICY IF EXISTS "Users can delete own FCM token" ON public.fcm_tokens;

-- 3. Students can INSERT their own token
CREATE POLICY "Users can insert own FCM token" 
ON public.fcm_tokens FOR INSERT 
WITH CHECK (user_id = auth.uid());

-- 4. Students can UPDATE their own token
CREATE POLICY "Users can update own FCM token" 
ON public.fcm_tokens FOR UPDATE 
USING (user_id = auth.uid())
WITH CHECK (user_id = auth.uid());

-- 5. Students can DELETE their own token (if they logout or uninstall, though typically managed via cascade)
CREATE POLICY "Users can delete own FCM token" 
ON public.fcm_tokens FOR DELETE 
USING (user_id = auth.uid());

-- NOTE: We do not add a SELECT policy for students. Students do not need to read other tokens.
-- If the application requires retrieving tokens for sending notifications, this is handled 
-- by the Edge Function running as a Service Role (admin) which bypasses RLS.
