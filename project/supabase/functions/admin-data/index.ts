import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization, X-Client-Info, Apikey",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 200, headers: corsHeaders });
  }

  try {
    const url = new URL(req.url);
    const path = url.pathname.replace("/admin-data", "") || "/";
    const authHeader = req.headers.get("Authorization") || "";

    // Use service role to bypass RLS for admin catalog operations.
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } }
    );

    // Verify caller is an admin
    const jwt = authHeader.replace("Bearer ", "");
    const { data: userData, error: authError } = await supabase.auth.getUser(jwt);
    if (authError || !userData?.user) {
      return json({ error: "Unauthorized" }, 401);
    }
    const { data: profile } = await supabase
      .from("profiles")
      .select("role")
      .eq("id", userData.user.id)
      .maybeSingle();
    if (profile?.role !== "ADMIN") {
      return json({ error: "Forbidden — admin only" }, 403);
    }

    // GET /users — list all profiles
    if (req.method === "GET" && path === "/users") {
      const { data, error } = await supabase
        .from("profiles")
        .select("*")
        .order("created_at", { ascending: false });
      if (error) return json({ error: error.message }, 500);
      return json({ data });
    }

    // GET /vendors
    if (req.method === "GET" && path === "/vendors") {
      const { data, error } = await supabase
        .from("vendors")
        .select("*, profile:profiles(email, full_name)")
        .order("created_at", { ascending: false });
      if (error) return json({ error: error.message }, 500);
      return json({ data });
    }

    // GET /products
    if (req.method === "GET" && path === "/products") {
      const { data, error } = await supabase
        .from("products")
        .select("*, vendor:vendors(name), category:categories(name)")
        .order("created_at", { ascending: false });
      if (error) return json({ error: error.message }, 500);
      return json({ data });
    }

    // GET /analytics — aggregate counts for the admin dashboard
    if (req.method === "GET" && path === "/analytics") {
      const [users, vendors, products, orders, coupons, payments] = await Promise.all([
        supabase.from("profiles").select("*", { count: "exact", head: true }),
        supabase.from("vendors").select("*", { count: "exact", head: true }),
        supabase.from("products").select("*", { count: "exact", head: true }),
        supabase.from("orders").select("*", { count: "exact", head: true }),
        supabase.from("coupons").select("*", { count: "exact", head: true }),
        supabase.from("payments").select("amount, status"),
      ]);

      const revenue = (payments.data || [])
        .filter((p) => p.status === "SUCCESSFUL")
        .reduce((s, p) => s + Number(p.amount), 0);

      // revenue by day (last 14 days) from orders
      const { data: recentOrders } = await supabase
        .from("orders")
        .select("total_amount, status, created_at")
        .order("created_at", { ascending: false })
        .limit(500);

      const byDay = {};
      (recentOrders || []).forEach((o) => {
        const d = new Date(o.created_at).toISOString().slice(0, 10);
        byDay[d] = (byDay[d] || 0) + Number(o.total_amount);
      });
      const revenueSeries = Object.entries(byDay)
        .sort((a, b) => a[0].localeCompare(b[0]))
        .slice(-14)
        .map(([date, value]) => ({ date, value }));

      const statusCounts = (recentOrders || []).reduce((acc, o) => {
        acc[o.status] = (acc[o.status] || 0) + 1;
        return acc;
      }, {});

      return json({
        counts: {
          users: users.count || 0,
          vendors: vendors.count || 0,
          products: products.count || 0,
          orders: orders.count || 0,
          coupons: coupons.count || 0,
        },
        revenue,
        revenueSeries,
        statusCounts,
      });
    }

    return json({ error: "Not found" }, 404);
  } catch (err) {
    return json({ error: err.message || "Server error" }, 500);
  }
});
