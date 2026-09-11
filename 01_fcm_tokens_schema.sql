-- 01_fcm_tokens_schema.sql
-- Run this in the Supabase SQL Editor

-- 1. Create the FCM Tokens Table
CREATE TABLE IF NOT EXISTS public.fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Note: The `users` table should have its primary key as `id` of type `TEXT` based on your Kotlin models 
-- (`id: String` which maps to Firebase UID). If `users.id` is UUID, change `user_id` above to UUID.

-- 2. Create Webhooks for Push Notifications (Calls the Edge Function)
-- You must deploy the 'send-fcm' edge function first, then you can create these triggers via the Supabase Dashboard
-- Dashboard -> Database -> Webhooks -> Create Webhook
-- Or using SQL (requires HTTP extension and pg_net):

-- Example Webhook Setup for INSERT (New Orders):
-- Name: fcm_new_order
-- Table: orders
-- Events: INSERT
-- Webhook URL: https://[YOUR_PROJECT_REF].supabase.co/functions/v1/send-fcm
-- Method: POST
-- Headers: Authorization: Bearer [ANON_KEY]

-- Example Webhook Setup for UPDATE (Status Changes):
-- Name: fcm_order_status
-- Table: orders
-- Events: UPDATE
-- Webhook URL: https://[YOUR_PROJECT_REF].supabase.co/functions/v1/send-fcm
-- Method: POST
-- Headers: Authorization: Bearer [ANON_KEY]
