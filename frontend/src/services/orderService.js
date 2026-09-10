import api from './api';

export const orderService = {
  /**
   * Creates a new order from current cart items.
   * Endpoint: POST /api/orders
   * @param {string} shippingAddress - Full shipping address string
   * @param {string|null} couponCode - Optional coupon code for server-side discount calculation
   */
  createOrder: async (shippingAddress, couponCode = null) => {
    const payload = { shippingAddress };
    if (couponCode) payload.couponCode = couponCode;
    const response = await api.post('/orders', payload);
    return response.data;
  },

  /**
   * Retrieves authenticated user's order history.
   * Endpoint: GET /api/orders
   */
  getMyOrders: async () => {
    const response = await api.get('/orders');
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Retrieves order details by ID.
   * Endpoint: GET /api/orders/{id}
   */
  getOrderById: async (id) => {
    const response = await api.get(`/orders/${id}`);
    return response.data;
  },

  /**
   * Retrieves all orders across platform (Admin only).
   * Endpoint: GET /api/orders/admin
   */
  getAllOrders: async () => {
    const response = await api.get('/orders/admin');
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Updates order status.
   * Endpoint: PUT /api/orders/{id}/status
   */
  updateOrderStatus: async (id, status) => {
    const response = await api.put(`/orders/${id}/status`, { status });
    return response.data;
  },
};

export default orderService;
