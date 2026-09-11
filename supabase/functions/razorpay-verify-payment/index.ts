import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "@supabase/supabase-js";
import crypto from "node:crypto";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Razorpay Test Keys
const RAZORPAY_KEY_SECRET = Deno.env.get("RAZORPAY_KEY_SECRET");

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    if (!RAZORPAY_KEY_SECRET) {
      throw new Error("Razorpay credentials are not configured on the server");
    }

    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      throw new Error("Missing Authorization header");
    }

    const { data: { user }, error: authError } = await supabaseClient.auth.getUser(
      authHeader.replace("Bearer ", "")
    );

    if (authError || !user) {
      throw new Error("Unauthorized");
    }

    const { order_id, razorpay_payment_id, razorpay_order_id, razorpay_signature } = await req.json();
    
    if (!order_id || !razorpay_payment_id || !razorpay_order_id || !razorpay_signature) {
      throw new Error("Missing required payment fields");
    }

    // 1. Verify the signature
    const text = razorpay_order_id + "|" + razorpay_payment_id;
    const expectedSignature = crypto
      .createHmac("sha256", RAZORPAY_KEY_SECRET)
      .update(text)
      .digest("hex");

    if (expectedSignature !== razorpay_signature) {
      throw new Error("Invalid payment signature");
    }

    // 2. Signature is valid, update the order in the database
    const { data: orderData, error: updateError } = await supabaseClient
      .from("orders")
      .update({
        payment_status: "PAID",
        razorpay_payment_id: razorpay_payment_id,
        razorpay_signature: razorpay_signature
      })
      .eq("id", order_id)
      .eq("user_id", user.id) // Ensure security
      .select("order_status, token_number")
      .single();

    if (updateError) {
      throw new Error("Failed to update order status");
    }

    // Since the database trigger for FCM doesn't fire natively for Edge Function updates 
    // unless configured for UPDATE, and we want Admin to know, we could call send-fcm here
    // but assuming there is a webhook for INSERT/UPDATE, it should trigger automatically.
    
    return new Response(
      JSON.stringify({ success: true, order: orderData }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error: any) {
    console.error("Razorpay Verify Error:", error.message);
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 400,
    });
  }
});
