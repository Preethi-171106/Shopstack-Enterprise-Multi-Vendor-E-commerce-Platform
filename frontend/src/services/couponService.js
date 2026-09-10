import api from './api';

export const normalizeCoupon = (c) => {
  if (!c) return null;
  const isPercentage = c.discountType === 'PERCENTAGE';
  return {
    id: c.id,
    code: c.code,
    name: c.name || c.code,
    description: c.description || (isPercentage ? `${c.discountValue}% off` : `₹${c.discountValue} off`),
    type: isPercentage ? 'percentage' : 'fixed',
    value: c.discountValue ? parseFloat(c.discountValue) : 0,
    discountType: c.discountType,
    discountValue: c.discountValue ? parseFloat(c.discountValue) : 0,
    minimumOrderAmount: c.minimumOrderAmount ? parseFloat(c.minimumOrderAmount) : 0,
    maximumDiscount: c.maximumDiscount ? parseFloat(c.maximumDiscount) : null,
    usageLimit: c.usageLimit ?? null,
    usedCount: c.usedCount ?? 0,
    perUserLimit: c.perUserLimit ?? null,
    applicabilityScope: c.applicabilityScope || 'ENTIRE_PLATFORM',
    eligibleCategoryIds: c.eligibleCategoryIds || [],
    eligibleProductIds: c.eligibleProductIds || [],
    eligibleVendorIds: c.eligibleVendorIds || [],
    eligibleCategoryNames: c.eligibleCategoryNames || [],
    eligibleProductNames: c.eligibleProductNames || [],
    eligibleVendorNames: c.eligibleVendorNames || [],
    eligibleSubtotal: c.eligibleSubtotal !== undefined ? parseFloat(c.eligibleSubtotal) : null,
    estimatedDiscount: c.estimatedDiscount !== undefined ? parseFloat(c.estimatedDiscount) : null,
    message: c.message || '',
    startDate: c.startDate || null,
    expiryDate: c.expiryDate || null,
    active: c.active ?? true,
    status: c.status || (c.active ? 'ACTIVE' : 'DISABLED'),
    createdAt: c.createdAt || null,
    updatedAt: c.updatedAt || null,
  };
};

export const couponService = {
  // ── Customer Endpoints ───────────────────────────────────────────────────
  /**
   * Validates a coupon code against an order subtotal and returns calculated discount amount.
   * Endpoint: POST /api/coupons/validate
   */
  validateCoupon: async (code, orderTotal = 0, userEmail = null) => {
    const payload = {
      code: code ? code.trim().toUpperCase() : '',
      orderTotal: Number(orderTotal) > 0 ? Number(orderTotal) : 1.0,
    };
    if (userEmail) {
      payload.userEmail = userEmail;
    }
    const response = await api.post('/coupons/validate', payload);
    return response.data;
  },

  /**
   * Validates and applies a coupon code against an order subtotal (legacy endpoint).
   * Endpoint: POST /api/coupons/apply
   */
  applyCoupon: async (code, orderTotal = 0) => {
    const response = await api.post('/coupons/apply', {
      code: code ? code.trim().toUpperCase() : '',
      orderTotal: Number(orderTotal) > 0 ? Number(orderTotal) : 1.0,
    });
    return normalizeCoupon(response.data);
  },

  /**
   * Fetches coupon by code.
   * Endpoint: GET /api/coupons/{code}
   */
  getCouponByCode: async (code) => {
    const response = await api.get(`/coupons/${encodeURIComponent(code)}`);
    return normalizeCoupon(response.data);
  },

  /**
   * Lists all currently active and available public coupons.
   * Endpoint: GET /api/coupons/available
   */
  getAvailableCoupons: async () => {
    const response = await api.get('/coupons/available');
    const coupons = Array.isArray(response.data) ? response.data : [];
    return coupons.map(normalizeCoupon);
  },

  /**
   * Lists coupons applicable to current authenticated customer's cart items.
   * Endpoint: GET /api/coupons/applicable
   */
  getApplicableCoupons: async () => {
    const response = await api.get('/coupons/applicable');
    const coupons = Array.isArray(response.data) ? response.data : [];
    return coupons.map(normalizeCoupon);
  },

  // ── Admin Endpoints ──────────────────────────────────────────────────────
  /**
   * Lists all coupons with optional search query and status filter (Admin only).
   * Endpoint: GET /api/admin/coupons
   */
  getAllAdminCoupons: async (search = '', status = 'ALL') => {
    const params = {};
    if (search && search.trim()) params.search = search.trim();
    if (status && status !== 'ALL') params.status = status;

    const response = await api.get('/admin/coupons', { params });
    const coupons = Array.isArray(response.data) ? response.data : [];
    return coupons.map(normalizeCoupon);
  },

  /**
   * Retrieves single coupon details by ID (Admin only).
   * Endpoint: GET /api/admin/coupons/{id}
   */
  getAdminCouponById: async (id) => {
    const response = await api.get(`/admin/coupons/${id}`);
    return normalizeCoupon(response.data);
  },

  /**
   * Creates a new coupon (Admin only).
   * Endpoint: POST /api/admin/coupons
   */
  createCoupon: async (couponData) => {
    const response = await api.post('/admin/coupons', couponData);
    return normalizeCoupon(response.data);
  },

  /**
   * Updates an existing coupon (Admin only).
   * Endpoint: PUT /api/admin/coupons/{id}
   */
  updateCoupon: async (id, couponData) => {
    const response = await api.put(`/admin/coupons/${id}`, couponData);
    return normalizeCoupon(response.data);
  },

  /**
   * Deletes a coupon (Admin only).
   * Endpoint: DELETE /api/admin/coupons/{id}
   */
  deleteCoupon: async (id) => {
    const response = await api.delete(`/admin/coupons/${id}`);
    return response.data;
  },

  /**
   * Activates a coupon (Admin only).
   * Endpoint: PATCH /api/admin/coupons/{id}/activate
   */
  activateCoupon: async (id) => {
    const response = await api.patch(`/admin/coupons/${id}/activate`);
    return normalizeCoupon(response.data);
  },

  /**
   * Deactivates a coupon (Admin only).
   * Endpoint: PATCH /api/admin/coupons/{id}/deactivate
   */
  deactivateCoupon: async (id) => {
    const response = await api.patch(`/admin/coupons/${id}/deactivate`);
    return normalizeCoupon(response.data);
  },

  /**
   * Enables a coupon (Admin only - alias).
   * Endpoint: PATCH /api/admin/coupons/{id}/enable
   */
  enableCoupon: async (id) => {
    const response = await api.patch(`/admin/coupons/${id}/enable`);
    return normalizeCoupon(response.data);
  },

  /**
   * Disables a coupon (Admin only - alias).
   * Endpoint: PATCH /api/admin/coupons/{id}/disable
   */
  disableCoupon: async (id) => {
    const response = await api.patch(`/admin/coupons/${id}/disable`);
    return normalizeCoupon(response.data);
  },

  /**
   * Retrieves usage history for a coupon (Admin only).
   * Endpoint: GET /api/admin/coupons/{id}/usage
   */
  getCouponUsages: async (id) => {
    const response = await api.get(`/admin/coupons/${id}/usage`);
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Retrieves high-level coupon analytics metrics (Admin only).
   * Endpoint: GET /api/admin/coupons/analytics
   */
  getCouponAnalytics: async () => {
    const response = await api.get('/admin/coupons/analytics');
    return response.data;
  },
};

export default couponService;

