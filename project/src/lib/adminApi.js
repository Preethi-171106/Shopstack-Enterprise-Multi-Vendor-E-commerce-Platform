// Admin data access via the admin-data edge function (service role, admin-verified).
import { supabase } from './supabase';

const baseUrl = `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/admin-data`;

async function adminGet(path) {
  const { data: session } = await supabase.auth.getSession();
  const token = session.session?.access_token;
  if (!token) throw new Error('Not authenticated');
  const res = await fetch(`${baseUrl}${path}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      apikey: import.meta.env.VITE_SUPABASE_ANON_KEY,
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.error || `Request failed (${res.status})`);
  }
  const json = await res.json();
  return json.data;
}

export async function adminListUsers() {
  return adminGet('/users');
}

export async function adminListVendors() {
  return adminGet('/vendors');
}

export async function adminListAllProducts() {
  return adminGet('/products');
}

export async function adminAnalytics() {
  return adminGet('/analytics');
}
