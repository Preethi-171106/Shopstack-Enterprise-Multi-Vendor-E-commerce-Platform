import api from './api';

export const returnService = {
  /**
   * Submits a return request for an item in an order.
   * Endpoint: POST /api/returns/orders/{orderId}
   */
  createReturnRequest: async (orderId, returnData) => {
    const response = await api.post(`/returns/orders/${orderId}`, returnData);
    return response.data;
  },

  /**
   * Retrieves customer return requests.
   * Endpoint: GET /api/returns/my
   */
  getCustomerReturns: async () => {
    const response = await api.get('/returns/my');
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Retrieves return request by ID.
   * Endpoint: GET /api/returns/{id}
   */
  getReturnById: async (id) => {
    const response = await api.get(`/returns/${id}`);
    return response.data;
  },

  /**
   * Retrieves return request by return number.
   * Endpoint: GET /api/returns/number/{returnNumber}
   */
  getReturnByNumber: async (returnNumber) => {
    const response = await api.get(`/returns/number/${returnNumber}`);
    return response.data;
  },

  /**
   * Lists all returns in the platform (Admin / Staff).
   * Endpoint: GET /api/returns
   */
  getAllReturns: async () => {
    const response = await api.get('/returns');
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Approves a return request (Admin).
   * Endpoint: PUT /api/returns/{id}/approve
   */
  approveReturn: async (id, decisionData = {}) => {
    const response = await api.put(`/returns/${id}/approve`, decisionData);
    return response.data;
  },

  /**
   * Rejects a return request (Admin).
   * Endpoint: PUT /api/returns/{id}/reject
   */
  rejectReturn: async (id, decisionData = {}) => {
    const response = await api.put(`/returns/${id}/reject`, decisionData);
    return response.data;
  },
};

export default returnService;
