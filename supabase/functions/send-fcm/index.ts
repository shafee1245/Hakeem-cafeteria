import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "@supabase/supabase-js";
import { GoogleAuth } from "google-auth-library";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const payload = await req.json();
    const { type, record, old_record } = payload;
    let tokens: string[] = [];
    let title = "";
    let body = "";

    // 1. New Order -> Notify Admin
    if (type === "INSERT") {
      const { data: adminUsers } = await supabaseClient
        .from("users")
        .select("id")
        .eq("role", "ADMIN");

      if (adminUsers && adminUsers.length > 0) {
        const adminIds = adminUsers.map((u: any) => u.id);
        const { data: fcmTokens } = await supabaseClient
          .from("fcm_tokens")
          .select("token")
          .in("user_id", adminIds);
        
        if (fcmTokens) tokens = fcmTokens.map((t: any) => t.token);
      }
      
      title = "New Order Placed";
      body = `Order #${record.token_number} has been placed.`;
    } 
    // 2. Order Status Update -> Notify Student
    else if (type === "UPDATE") {
      if (old_record.order_status !== record.order_status) {
        if (record.order_status === "ACCEPTED") {
          title = "Order Accepted!";
          body = `Your order #${record.token_number} is being prepared.`;
        } else if (record.order_status === "DELIVERED") {
          title = "Order Ready!";
          body = `Your order #${record.token_number} is ready for pickup.`;
        }

        if (title) {
          // INSERT INTO notifications table
          const { error: insertError } = await supabaseClient
            .from("notifications")
            .insert({
              user_id: record.user_id,
              title: title,
              message: body,
              is_read: false
            });
          if (insertError) {
            console.error("Failed to insert notification:", insertError);
          }

          const { data: fcmTokens } = await supabaseClient
            .from("fcm_tokens")
            .select("token")
            .eq("user_id", record.user_id);
          
          if (fcmTokens) tokens = fcmTokens.map((t: any) => t.token);
        }
      }
    }

    if (tokens.length === 0 || !title) {
      return new Response(JSON.stringify({ message: "No notifications to send" }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200,
      });
    }

    // Authenticate with Firebase via Service Account
    const serviceAccountJson = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT") || "{}");
    if (!serviceAccountJson.project_id) {
        throw new Error("Missing FIREBASE_SERVICE_ACCOUNT secret");
    }

    const auth = new GoogleAuth({
      credentials: {
        client_email: serviceAccountJson.client_email,
        private_key: serviceAccountJson.private_key,
      },
      scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
      projectId: serviceAccountJson.project_id,
    });

    const accessToken = await auth.getAccessToken();

    const fcmEndpoint = `https://fcm.googleapis.com/v1/projects/${serviceAccountJson.project_id}/messages:send`;
    
    const sendPromises = tokens.map(async (token) => {
      const res = await fetch(fcmEndpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({
          message: {
            token: token,
            notification: { title, body },
          },
        }),
      });
      return res.json();
    });

    const results = await Promise.all(sendPromises);

    return new Response(JSON.stringify({ success: true, results }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });
  } catch (error: any) {
    console.error("Error sending FCM:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 400,
    });
  }
});
