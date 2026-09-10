import { supabase } from './supabase';

// ---------- Auth ----------
export async function signUp({ email, password, fullName, role = 'CUSTOMER' }) {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: { data: { full_name: fullName, role } },
  });
  if (error) throw new Error(error.message);
  return data;
}

export async function signIn({ email, password }) {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password });
  if (error) throw new Error(error.message);
  return data;
}

export async function signOut() {
  const { error } = await supabase.auth.signOut();
  if (error) throw new Error(error.message);
}

export async function getSession() {
  const { data, error } = await supabase.auth.getSession();
  if (error) throw new Error(error.message);
  return data.session;
}

export async function getProfile(userId) {
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .eq('id', userId)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function upsertProfile(profile) {
  const { data, error } = await supabase
    .from('profiles')
    .upsert(profile, { onConflict: 'id' })
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Categories ----------
export async function listCategories() {
  const { data, error } = await supabase
    .from('categories')
    .select('*')
    .order('name', { ascending: true });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function createCategory(payload) {
  const { data, error } = await supabase.from('categories').insert(payload).select().maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function getCategoryBySlug(slug) {
  const { data, error } = await supabase
    .from('categories')
    .select('*')
    .eq('slug', slug)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Products ----------
export async function listProducts({ categoryId, vendorId, search, sort = 'newest' } = {}) {
  let query = supabase
    .from('products')
    .select('*, vendor:vendors(name), category:categories(name)', { count: 'exact' });
  if (categoryId) query = query.eq('category_id', categoryId);
  if (vendorId) query = query.eq('vendor_id', vendorId);
  if (search) query = query.ilike('name', `%${search}%`);
  switch (sort) {
    case 'price_asc':
      query = query.order('price', { ascending: true });
      break;
    case 'price_desc':
      query = query.order('price', { ascending: false });
      break;
    case 'rating':
      query = query.order('rating', { ascending: false });
      break;
    default:
      query = query.order('created_at', { ascending: false });
  }
  const { data, error, count } = await query;
  if (error) throw new Error(error.message);
  return { products: data || [], total: count || 0 };
}

export async function getProduct(id) {
  const { data, error } = await supabase
    .from('products')
    .select('*, vendor:vendors(*), category:categories(*)')
    .eq('id', id)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function listVendorProducts(vendorId) {
  const { data, error } = await supabase
    .from('products')
    .select('*, category:categories(name)')
    .eq('vendor_id', vendorId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function createProduct(payload) {
  const { data, error } = await supabase.from('products').insert(payload).select().maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function updateProduct(id, payload) {
  const { data, error } = await supabase
    .from('products')
    .update(payload)
    .eq('id', id)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function deleteProduct(id) {
  const { error } = await supabase.from('products').delete().eq('id', id);
  if (error) throw new Error(error.message);
}

// ---------- Vendors ----------
export async function listVendors() {
  const { data, error } = await supabase
    .from('vendors')
    .select('*')
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function getVendorByUserId(userId) {
  const { data, error } = await supabase
    .from('vendors')
    .select('*')
    .eq('user_id', userId)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function createVendor(payload) {
  const { data, error } = await supabase.from('vendors').insert(payload).select().maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function updateVendor(id, payload) {
  const { data, error } = await supabase
    .from('vendors')
    .update(payload)
    .eq('id', id)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Cart ----------
export async function getCart(userId) {
  const { data, error } = await supabase
    .from('carts')
    .select('*, items:cart_items(*, product:products(*))')
    .eq('user_id', userId)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function ensureCart(userId) {
  const existing = await getCart(userId);
  if (existing) return existing;
  const { data, error } = await supabase
    .from('carts')
    .insert({ user_id: userId })
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return { ...data, items: [] };
}

export async function addCartItem(cartId, productId, quantity = 1) {
  // upsert by (cart_id, product_id) is not directly supported; check existing
  const { data: existing } = await supabase
    .from('cart_items')
    .select('*')
    .eq('cart_id', cartId)
    .eq('product_id', productId)
    .maybeSingle();
  if (existing) {
    const { data, error } = await supabase
      .from('cart_items')
      .update({ quantity: existing.quantity + quantity })
      .eq('id', existing.id)
      .select()
      .maybeSingle();
    if (error) throw new Error(error.message);
    return data;
  }
  const { data, error } = await supabase
    .from('cart_items')
    .insert({ cart_id: cartId, product_id: productId, quantity })
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function updateCartItem(id, quantity) {
  const { data, error } = await supabase
    .from('cart_items')
    .update({ quantity })
    .eq('id', id)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function deleteCartItem(id) {
  const { error } = await supabase.from('cart_items').delete().eq('id', id);
  if (error) throw new Error(error.message);
}

export async function clearCart(cartId) {
  const { error } = await supabase.from('cart_items').delete().eq('cart_id', cartId);
  if (error) throw new Error(error.message);
}

// ---------- Wishlist ----------
export async function getWishlist(userId) {
  const { data, error } = await supabase
    .from('wishlist_items')
    .select('*, product:products(*, vendor:vendors(name))')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function addWishlistItem(userId, productId) {
  const { error } = await supabase
    .from('wishlist_items')
    .insert({ user_id: userId, product_id: productId });
  if (error && !error.message.includes('duplicate')) throw new Error(error.message);
}

export async function removeWishlistItem(id) {
  const { error } = await supabase.from('wishlist_items').delete().eq('id', id);
  if (error) throw new Error(error.message);
}

// ---------- Orders ----------
export async function listOrders(userId) {
  const { data, error } = await supabase
    .from('orders')
    .select('*, items:order_items(*), vendor:vendors(name)')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function getOrder(orderId) {
  const { data, error } = await supabase
    .from('orders')
    .select('*, items:order_items(*), vendor:vendors(name), address:addresses(*)')
    .eq('id', orderId)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function createOrder(payload, items) {
  const { data: order, error } = await supabase
    .from('orders')
    .insert(payload)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  const rows = items.map((it) => ({
    order_id: order.id,
    product_id: it.product_id,
    product_name: it.product_name,
    quantity: it.quantity,
    unit_price: it.unit_price,
    subtotal: it.unit_price * it.quantity,
  }));
  const { error: itemError } = await supabase.from('order_items').insert(rows);
  if (itemError) throw new Error(itemError.message);
  return order;
}

export async function listVendorOrders(vendorId) {
  const { data, error } = await supabase
    .from('orders')
    .select('*, items:order_items(*), user:profiles(full_name,email)')
    .eq('vendor_id', vendorId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

// ---------- Payments ----------
export async function listPayments(userId) {
  const { data, error } = await supabase
    .from('payments')
    .select('*, order:orders(id,status)')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function createPayment(payload) {
  const { data, error } = await supabase
    .from('payments')
    .insert(payload)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Shipments ----------
export async function getShipmentByOrder(orderId) {
  const { data, error } = await supabase
    .from('shipments')
    .select('*')
    .eq('order_id', orderId)
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Addresses ----------
export async function listAddresses(userId) {
  const { data, error } = await supabase
    .from('addresses')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function createAddress(payload) {
  const { data, error } = await supabase
    .from('addresses')
    .insert(payload)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Reviews ----------
export async function listReviews(productId) {
  const { data, error } = await supabase
    .from('reviews')
    .select('*, user:profiles(full_name)')
    .eq('product_id', productId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function createReview(payload) {
  const { data, error } = await supabase
    .from('reviews')
    .insert(payload)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

// ---------- Coupons ----------
export async function listCoupons() {
  const { data, error } = await supabase
    .from('coupons')
    .select('*')
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function createCoupon(payload) {
  const { data, error } = await supabase.from('coupons').insert(payload).select().maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function updateCoupon(id, payload) {
  const { data, error } = await supabase
    .from('coupons')
    .update(payload)
    .eq('id', id)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function deleteCoupon(id) {
  const { error } = await supabase.from('coupons').delete().eq('id', id);
  if (error) throw new Error(error.message);
}

// ---------- Notifications ----------
export async function listNotifications(userId) {
  const { data, error } = await supabase
    .from('notifications')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });
  if (error) throw new Error(error.message);
  return data || [];
}

export async function markNotificationRead(id) {
  const { data, error } = await supabase
    .from('notifications')
    .update({ is_read: true })
    .eq('id', id)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}

export async function createNotification(payload) {
  const { data, error } = await supabase
    .from('notifications')
    .insert(payload)
    .select()
    .maybeSingle();
  if (error) throw new Error(error.message);
  return data;
}
