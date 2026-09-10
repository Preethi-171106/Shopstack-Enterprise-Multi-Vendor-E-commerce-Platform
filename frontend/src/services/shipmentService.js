import api from './api';

export const shipmentService = {
  /**
   * Retrieves customer shipments.
   * Endpoint: GET /api/customer/shipments
   */
  getCustomerShipments: async () => {
    const response = await api.get('/customer/shipments');
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Tracks a shipment by tracking number.
   * Endpoint: GET /api/shipments/tracking/{trackingNumber}
   */
  trackShipment: async (trackingNumber) => {
    const response = await api.get(`/shipments/tracking/${trackingNumber}`);
    return response.data;
  },

  /**
   * Retrieves vendor shipments (Vendor role).
   * Endpoint: GET /api/vendor/shipments
   */
  getVendorShipments: async () => {
    const response = await api.get('/vendor/shipments');
    return Array.isArray(response.data) ? response.data : [];
  },

  /**
   * Retrieves warehouse shipments (Warehouse role).
   * Endpoint: GET /api/warehouse/shipments
   */
  getWarehouseShipments: async () => {
    const response = await api.get('/warehouse/shipments');
    return Array.isArray(response.data) ? response.data : [];
  },
};

export default shipmentService;
