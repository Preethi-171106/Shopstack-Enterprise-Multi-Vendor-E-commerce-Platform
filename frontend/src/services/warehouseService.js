import api from './api';

export const warehouseService = {
  // ── Dashboard & Metrics ───────────────────────────────────────────────────
  getDashboard: async () => {
    try {
      const response = await api.get('/warehouse/dashboard');
      return response.data || null;
    } catch (error) {
      console.error('Failed to load warehouse dashboard metrics:', error);
      return null;
    }
  },

  // ── Multi-Warehouse Physical Facilities (Admin/Staff) ─────────────────────
  getWarehouses: async (search = '') => {
    const params = search && search.trim() ? { search: search.trim() } : {};
    try {
      const response = await api.get('/admin/warehouses', { params });
      if (Array.isArray(response.data)) return response.data;
      if (Array.isArray(response.data?.content)) return response.data.content;
      if (Array.isArray(response.data?.data)) return response.data.data;
      return [];
    } catch (err1) {
      try {
        const response = await api.get('/warehouse/facilities', { params });
        if (Array.isArray(response.data)) return response.data;
        if (Array.isArray(response.data?.content)) return response.data.content;
        return [];
      } catch (err2) {
        console.error('Failed to load warehouses:', err1, err2);
        return [];
      }
    }
  },

  getActiveWarehouses: async () => {
    try {
      const response = await api.get('/admin/warehouses/active');
      if (Array.isArray(response.data)) return response.data;
      if (Array.isArray(response.data?.content)) return response.data.content;
      return [];
    } catch (err1) {
      try {
        const response = await api.get('/warehouse/facilities/active');
        if (Array.isArray(response.data)) return response.data;
        if (Array.isArray(response.data?.content)) return response.data.content;
        return [];
      } catch (err2) {
        console.error('Failed to load active warehouses:', err1, err2);
        return [];
      }
    }
  },

  getWarehouseById: async (warehouseId) => {
    try {
      const response = await api.get(`/admin/warehouses/${warehouseId}`);
      return response.data;
    } catch (err1) {
      try {
        const response = await api.get(`/warehouse/facilities/${warehouseId}`);
        return response.data;
      } catch (err2) {
        console.error(`Failed to load warehouse #${warehouseId}:`, err1, err2);
        return null;
      }
    }
  },

  createWarehouse: async (warehouseData) => {
    const response = await api.post('/admin/warehouses', warehouseData);
    return response.data;
  },

  updateWarehouse: async (warehouseId, warehouseData) => {
    const response = await api.put(`/admin/warehouses/${warehouseId}`, warehouseData);
    return response.data;
  },

  activateWarehouse: async (warehouseId) => {
    const response = await api.patch(`/admin/warehouses/${warehouseId}/activate`);
    return response.data;
  },

  deactivateWarehouse: async (warehouseId) => {
    const response = await api.patch(`/admin/warehouses/${warehouseId}/deactivate`);
    return response.data;
  },

  getAllWarehouseInventories: async () => {
    try {
      const response = await api.get('/admin/warehouses/inventory');
      if (Array.isArray(response.data)) return response.data;
      if (Array.isArray(response.data?.content)) return response.data.content;
      return [];
    } catch (err1) {
      try {
        const response = await api.get('/warehouse/facilities/inventory');
        if (Array.isArray(response.data)) return response.data;
        if (Array.isArray(response.data?.content)) return response.data.content;
        return [];
      } catch (err2) {
        console.error('Failed to load all warehouse inventories:', err1, err2);
        return [];
      }
    }
  },

  getWarehouseInventoryByWarehouse: async (warehouseId) => {
    try {
      const response = await api.get(`/admin/warehouses/${warehouseId}/inventory`);
      if (Array.isArray(response.data)) return response.data;
      if (Array.isArray(response.data?.content)) return response.data.content;
      return [];
    } catch (err1) {
      try {
        const response = await api.get(`/warehouse/facilities/${warehouseId}/inventory`);
        if (Array.isArray(response.data)) return response.data;
        if (Array.isArray(response.data?.content)) return response.data.content;
        return [];
      } catch (err2) {
        console.error(`Failed to load inventory for warehouse #${warehouseId}:`, err1, err2);
        return [];
      }
    }
  },

  addWarehouseProductStock: async (warehouseId, payload) => {
    const response = await api.post(`/admin/warehouses/${warehouseId}/inventory`, payload);
    return response.data;
  },

  adjustWarehouseProductStock: async (warehouseId, productId, payload) => {
    const response = await api.post(`/admin/warehouses/${warehouseId}/inventory/${productId}/adjust`, payload);
    return response.data;
  },

  // ── Multi-Warehouse Allocations Workflow ──────────────────────────────────
  getAllocations: async (warehouseId = null, status = 'ALL') => {
    try {
      const params = {};
      if (warehouseId) params.warehouseId = warehouseId;
      if (status && status !== 'ALL') params.status = status;
      const response = await api.get('/warehouse/allocations', { params });
      if (Array.isArray(response.data)) return response.data;
      if (Array.isArray(response.data?.content)) return response.data.content;
      return [];
    } catch (error) {
      console.error('Failed to load warehouse allocations:', error);
      return [];
    }
  },

  getPickingAllocations: async (warehouseId = null) => {
    try {
      const params = warehouseId ? { warehouseId } : {};
      const response = await api.get('/warehouse/allocations/picking', { params });
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load picking allocations:', error);
      return [];
    }
  },

  getPackingAllocations: async (warehouseId = null) => {
    try {
      const params = warehouseId ? { warehouseId } : {};
      const response = await api.get('/warehouse/allocations/packing', { params });
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load packing allocations:', error);
      return [];
    }
  },

  getReadyToShipAllocations: async (warehouseId = null) => {
    try {
      const params = warehouseId ? { warehouseId } : {};
      const response = await api.get('/warehouse/allocations/ready-to-ship', { params });
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load ready-to-ship allocations:', error);
      return [];
    }
  },

  getAllocationById: async (allocationId) => {
    const response = await api.get(`/warehouse/allocations/${allocationId}`);
    return response.data;
  },

  pickAllocation: async (allocationId) => {
    const response = await api.post(`/warehouse/allocations/${allocationId}/pick`);
    return response.data;
  },

  packAllocation: async (allocationId, packingData = {}) => {
    const response = await api.post(`/warehouse/allocations/${allocationId}/pack`, packingData);
    return response.data;
  },

  readyAllocationForShipment: async (allocationId, shipmentData = {}) => {
    const response = await api.post(`/warehouse/allocations/${allocationId}/ready-for-shipment`, shipmentData);
    return response.data;
  },

  // ── Warehouse Order Queue & Legacy Operations ─────────────────────────────
  getOrders: async (status = null) => {
    try {
      const params = status && status !== 'ALL' ? { status } : {};
      const response = await api.get('/warehouse/orders', { params });
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load warehouse orders:', error);
      return [];
    }
  },

  getPickingOrders: async () => {
    try {
      const response = await api.get('/warehouse/picking/orders');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load picking orders:', error);
      return [];
    }
  },

  getPackingOrders: async () => {
    try {
      const response = await api.get('/warehouse/packing/orders');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load packing orders:', error);
      return [];
    }
  },

  getReadyToShipOrders: async () => {
    try {
      const response = await api.get('/warehouse/ready-to-ship/orders');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load ready-to-ship orders:', error);
      return [];
    }
  },

  getOrderById: async (orderId) => {
    const response = await api.get(`/warehouse/orders/${orderId}`);
    return response.data;
  },

  startPicking: async (orderId) => {
    const response = await api.post(`/warehouse/orders/${orderId}/start-picking`);
    return response.data;
  },

  markPicked: async (orderId) => {
    const response = await api.post(`/warehouse/orders/${orderId}/mark-picked`);
    return response.data;
  },

  completePicking: async (orderId) => {
    const response = await api.post(`/warehouse/orders/${orderId}/complete-picking`);
    return response.data;
  },

  startPacking: async (orderId) => {
    const response = await api.post(`/warehouse/orders/${orderId}/start-packing`);
    return response.data;
  },

  markPacked: async (orderId, packageData = {}) => {
    const response = await api.post(`/warehouse/orders/${orderId}/mark-packed`, packageData);
    return response.data;
  },

  completePacking: async (orderId, packageData = {}) => {
    const response = await api.post(`/warehouse/orders/${orderId}/complete-packing`, packageData);
    return response.data;
  },

  readyToShip: async (orderId, shippingData) => {
    const response = await api.post(`/warehouse/orders/${orderId}/ready-to-ship`, shippingData);
    return response.data;
  },

  prepareShipment: async (orderId, shippingData) => {
    const response = await api.post(`/warehouse/orders/${orderId}/prepare-shipment`, shippingData);
    return response.data;
  },

  // ── Inventory & Stock Management ─────────────────────────────────────────
  getInventory: async (search = '', status = 'ALL') => {
    try {
      const params = {};
      if (search && search.trim()) params.search = search.trim();
      if (status && status !== 'ALL') params.status = status;
      const response = await api.get('/warehouse/inventory', { params });
      const data = response.data;
      return Array.isArray(data) ? data : (data?.content || []);
    } catch (error) {
      console.error('Failed to load warehouse inventory:', error);
      return [];
    }
  },

  getInventories: async (search = '', status = 'ALL') => {
    return warehouseService.getInventory(search, status);
  },

  getLowStock: async () => {
    try {
      const response = await api.get('/warehouse/inventory/low-stock');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load low stock inventories:', error);
      return [];
    }
  },

  getOutOfStock: async () => {
    try {
      const response = await api.get('/warehouse/inventory/out-of-stock');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load out of stock inventories:', error);
      return [];
    }
  },

  addStock: async (productId, quantity, reason = 'Stock replenishment') => {
    const response = await api.post(`/warehouse/inventory/${productId}/add`, {
      quantity: Number(quantity),
      reason,
    });
    return response.data;
  },

  adjustStock: async (productId, payloadOrTotal, reason = 'Physical inventory count') => {
    let payload;
    if (typeof payloadOrTotal === 'object' && payloadOrTotal !== null) {
      payload = payloadOrTotal;
    } else {
      payload = {
        newTotalStock: Number(payloadOrTotal),
        reason,
      };
    }
    const response = await api.post(`/warehouse/inventory/${productId}/adjust`, payload);
    return response.data;
  },

  updateThreshold: async (productId, lowStockThreshold) => {
    const response = await api.put(`/warehouse/inventory/${productId}/threshold`, {
      lowStockThreshold: Number(lowStockThreshold),
    });
    return response.data;
  },

  getStockMovements: async (page = 0, size = 20) => {
    try {
      const response = await api.get('/warehouse/stock-movements', {
        params: { page, size },
      });
      return response.data;
    } catch (error) {
      console.error('Failed to load stock movements:', error);
      return { content: [] };
    }
  },

  // ── Shipments ────────────────────────────────────────────────────────────
  getShipments: async () => {
    try {
      const response = await api.get('/warehouse/shipments');
      return Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      console.error('Failed to load shipments:', error);
      return [];
    }
  },

  updateShipmentStatus: async (shipmentId, statusData) => {
    const response = await api.patch(`/warehouse/shipments/${shipmentId}/status`, statusData);
    return response.data;
  },

  // ── Stock Distribution (Admin) ──────────────────────────────────────────
  distributeStock: async (payload) => {
    const response = await api.post('/admin/warehouses/distribute-stock', payload);
    return response.data;
  },

  getDistributionOverview: async (productId) => {
    const response = await api.get(`/admin/warehouses/distribution-overview/${productId}`);
    return response.data;
  },

  getDamagedInventories: async (warehouseId = null) => {
    const params = warehouseId ? { warehouseId } : {};
    try {
      const response = await api.get('/admin/warehouses/inventory/damaged', { params });
      return Array.isArray(response.data) ? response.data : (response.data?.content || []);
    } catch (err1) {
      try {
        const response = await api.get('/warehouse/facilities/damaged', { params });
        return Array.isArray(response.data) ? response.data : (response.data?.content || []);
      } catch (err2) {
        console.error('Failed to load damaged inventories:', err1, err2);
        return [];
      }
    }
  },

  // ── Warehouse Returns & Quality Check (Staff) ───────────────────────────
  getWarehouseReturns: async (warehouseId = null) => {
    try {
      const url = warehouseId ? `/returns/warehouse/${warehouseId}` : '/returns';
      const response = await api.get(url);
      return Array.isArray(response.data) ? response.data : (response.data?.content || []);
    } catch (error) {
      console.error('Failed to load warehouse returns:', error);
      return [];
    }
  },

  getPendingReceivingReturns: async (warehouseId = null) => {
    try {
      const url = warehouseId ? `/returns/warehouse/${warehouseId}/receiving` : '/returns/receiving';
      const response = await api.get(url);
      return Array.isArray(response.data) ? response.data : (response.data?.content || []);
    } catch (error) {
      console.error('Failed to load pending receiving returns:', error);
      return [];
    }
  },

  getPendingQcReturns: async (warehouseId = null) => {
    try {
      const url = warehouseId ? `/returns/warehouse/${warehouseId}/qc` : '/returns/qc';
      const response = await api.get(url);
      return Array.isArray(response.data) ? response.data : (response.data?.content || []);
    } catch (error) {
      console.error('Failed to load pending QC returns:', error);
      return [];
    }
  },

  receiveReturn: async (returnId, receivingData = {}) => {
    const response = await api.put(`/returns/${returnId}/receive`, receivingData);
    return response.data;
  },

  performQualityCheck: async (returnId, qcData) => {
    const response = await api.put(`/returns/${returnId}/quality-check`, qcData);
    return response.data;
  },
};

export default warehouseService;
