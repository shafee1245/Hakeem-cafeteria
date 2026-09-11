import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "@supabase/supabase-js";
import Razorpay from "razorpay";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Razorpay Test Keys (now injected via environment variables)
const RAZORPAY_KEY_ID = Deno.env.get("RAZORPAY_KEY_ID");
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

    if (!RAZORPAY_KEY_ID || !RAZORPAY_KEY_SECRET) {
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

    const { order_id } = await req.json();
    if (!order_id) {
      throw new Error("Missing order_id");
    }

    // 1. Fetch the order details securely from DB
    const { data: orderData, error: orderError } = await supabaseClient
      .from("orders")
      .select("total, user_id")
      .eq("id", order_id)
      .single();

    if (orderError || !orderData) {
      throw new Error("Order not found");
    }

    // Security check: ensure the caller is the owner of the order
    if (orderData.user_id !== user.id) {
      throw new Error("Unauthorized access to this order");
    }

    // 2. Initialize Razorpay
    const razorpay = new Razorpay({
      key_id: RAZORPAY_KEY_ID,
      key_secret: RAZORPAY_KEY_SECRET,
    });

    // 3. Create Razorpay Order
    // Amount must be in paise (smallest currency unit), e.g., ₹100 = 10000 paise
    const amountInPaise = Math.round(orderData.total * 100);

    const rzpOrder = await razorpay.orders.create({
      amount: amountInPaise,
      currency: "INR",
      receipt: `receipt_${order_id.replace(/-/g, '').substring(0, 30)}`,
    });

    if (!rzpOrder || !rzpOrder.id) {
      throw new Error("Failed to generate Razorpay order");
    }

    // 4. Save the razorpay_order_id to our database
    const { error: updateError } = await supabaseClient
      .from("orders")
      .update({ razorpay_order_id: rzpOrder.id })
      .eq("id", order_id);

    if (updateError) {
      throw new Error("Failed to update order with Razorpay ID");
    }

    // 5. Return the ID back to the Android client
    return new Response(
      JSON.stringify({ 
        success: true, 
        razorpay_order_id: rzpOrder.id,
        amount: amountInPaise,
        currency: "INR",
        key: RAZORPAY_KEY_ID
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
}catch (error: any) {
    console.error(
      "Razorpay Create Order Error:",
      JSON.stringify(error, Object.getOwnPropertyNames(error))
    );

    const errorPayload = {
      message: error?.message ?? null,
      code: error?.code ?? error?.error?.code ?? null,
      description: error?.description ?? error?.error?.description ?? null,
      reason: error?.reason ?? error?.error?.reason ?? null,
      statusCode: error?.statusCode ?? null,
      raw: error
    };

    console.error(
      "Razorpay Create Order Error Details:",
      JSON.stringify(errorPayload)
    );

    return new Response(
      JSON.stringify({
        error:
          errorPayload.description ||
          errorPayload.message ||
          "Razorpay order creation failed",
        code: errorPayload.code,
        reason: errorPayload.reason
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: errorPayload.statusCode && errorPayload.statusCode >= 400
          ? errorPayload.statusCode
          : 400
      }
    );
  }
});
