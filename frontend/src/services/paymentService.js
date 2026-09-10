import api from './api';

export const paymentService = {
  /**
   * Creates a payment order for an application order.
   * Endpoint: POST /api/payments/create/{orderId}
   */
  createPayment: async (orderId, paymentData = {}) => {
    const response = await api.post(`/payments/create/${orderId}`, paymentData);
    return response.data;
  },

  /**
   * Verifies Razorpay payment signature.
   * Endpoint: POST /api/payments/verify
   */
  verifyPayment: async (verifyData) => {
    const response = await api.post('/payments/verify', verifyData);
    return response.data;
  },

  /**
   * Retrieves payment details for an order.
   * Endpoint: GET /api/payments/order/{orderId}
   */
  getPaymentByOrder: async (orderId) => {
    const response = await api.get(`/payments/order/${orderId}`);
    return response.data;
  },

  /**
   * Requests a refund for a successfully paid order.
   * Endpoint: POST /api/payments/refund/{orderId}
   * NOTE: Requires real Razorpay credentials for live gateway refund processing.
   *       With mock credentials the DB status is updated but no real gateway call is made.
   */
  refundPayment: async (orderId, reason = '') => {
    const response = await api.post(`/payments/refund/${orderId}`, { reason });
    return response.data;
  },
};

export default paymentService;
