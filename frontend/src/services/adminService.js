import api from './api';

/**
 * adminService — REST API Client for Admin Dashboard, Vendors, Analytics,
 * Orders, Commissions, System Health, and Business Reports.
 */
export const adminService = {
  // ── Dashboard & Analytics ────────────────────────────────────────────────
  getDashboardStats: async () => {
    const response = await api.get('/admin/dashboard');
    return response.data;
  },

  getAnalyticsTrends: async () => {
    const response = await api.get('/admin/analytics/trends');
    return response.data;
  },

  // ── Vendor Management ───────────────────────────────────────────────────
  getVendors: async () => {
    const response = await api.get('/admin/vendors');
    return Array.isArray(response.data) ? response.data : [];
  },

  getVendorDetails: async (vendorProfileId) => {
    const response = await api.get(`/admin/vendors/${vendorProfileId}`);
    return response.data;
  },

  approveVendor: async (vendorProfileId) => {
    const response = await api.patch(`/admin/vendors/${vendorProfileId}/approve`);
    return response.data;
  },

  rejectVendor: async (vendorProfileId) => {
    const response = await api.patch(`/admin/vendors/${vendorProfileId}/reject`);
    return response.data;
  },

  suspendVendor: async (vendorProfileId) => {
    const response = await api.patch(`/admin/vendors/${vendorProfileId}/suspend`);
    return response.data;
  },

  // ── Order Monitoring ─────────────────────────────────────────────────────
  getAdminOrders: async (status = null) => {
    const params = status && status !== 'ALL' ? { status } : {};
    const response = await api.get('/admin/orders', { params });
    return Array.isArray(response.data) ? response.data : [];
  },

  updateOrderStatus: async (orderId, newStatus) => {
    const response = await api.put(`/admin/orders/${orderId}/status`, { status: newStatus });
    return response.data;
  },

  // ── Commission Management ────────────────────────────────────────────────
  getCommissions: async () => {
    const response = await api.get('/admin/commissions');
    return Array.isArray(response.data) ? response.data : [];
  },

  getCommissionSummary: async () => {
    const response = await api.get('/admin/commissions/summary');
    return response.data;
  },

  // ── System Health & Monitoring ───────────────────────────────────────────
  getSystemHealth: async () => {
    const response = await api.get('/admin/system/health');
    return response.data;
  },

  // ── Business Reports ─────────────────────────────────────────────────────
  getSalesReport: async () => {
    const response = await api.get('/admin/reports/sales');
    return response.data;
  },

  getOrderReport: async () => {
    const response = await api.get('/admin/reports/orders');
    return response.data;
  },

  getVendorReport: async () => {
    const response = await api.get('/admin/reports/vendors');
    return response.data;
  },

  getCommissionReport: async () => {
    const response = await api.get('/admin/reports/commissions');
    return response.data;
  },

  getProductReport: async () => {
    const response = await api.get('/admin/reports/products');
    return response.data;
  },

  // ── Staff & User Management ──────────────────────────────────────────────
  getStaffUsers: async () => {
    const response = await api.get('/admin/users/warehouse-staff');
    return Array.isArray(response.data) ? response.data : [];
  },

  createStaffUser: async (staffData) => {
    const response = await api.post('/admin/users/staff', staffData);
    return response.data;
  },

  approveWarehouseStaff: async (id) => {
    const response = await api.patch(`/admin/users/warehouse-staff/${id}/approve`);
    return response.data;
  },

  rejectWarehouseStaff: async (id, reason) => {
    const response = await api.patch(`/admin/users/warehouse-staff/${id}/reject`, { reason });
    return response.data;
  },

  suspendWarehouseStaff: async (id) => {
    const response = await api.patch(`/admin/users/warehouse-staff/${id}/suspend`);
    return response.data;
  },

  reactivateWarehouseStaff: async (id) => {
    const response = await api.patch(`/admin/users/warehouse-staff/${id}/reactivate`);
    return response.data;
  },

  toggleUserStatus: async (userId, enabled) => {
    const action = enabled ? 'enable' : 'disable';
    const response = await api.patch(`/admin/users/${userId}/${action}`);
    return response.data;
  },

  // ── CSV Export ───────────────────────────────────────────────────────────
  downloadReportCsv: async (type) => {
    const response = await api.get(`/admin/reports/${type}/export`, {
      responseType: 'blob',
    });

    // Create a client-side download link
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `shopstack_${type}_report_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    link.parentNode.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};

export default adminService;
