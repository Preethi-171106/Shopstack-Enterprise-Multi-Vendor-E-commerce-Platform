import api from './api';

export const warehouseStaffService = {
  /**
   * Registers a new warehouse staff applicant (status = PENDING).
   * Endpoint: POST /api/auth/register/warehouse-staff
   */
  register: async (staffData) => {
    const response = await api.post('/auth/register/warehouse-staff', {
      firstName: staffData.firstName,
      lastName: staffData.lastName,
      email: staffData.email,
      password: staffData.password,
      phoneNumber: staffData.phoneNumber || '',
      warehouseId: staffData.warehouseId ? Number(staffData.warehouseId) : null,
    });
    return response.data;
  },

  /**
   * Admin lists all warehouse staff profiles.
   * Endpoint: GET /api/admin/users/warehouse-staff
   */
  getAllStaff: async () => {
    const response = await api.get('/admin/users/warehouse-staff');
    return response.data;
  },

  /**
   * Admin retrieves single warehouse staff profile.
   * Endpoint: GET /api/admin/users/warehouse-staff/{id}
   */
  getStaffById: async (id) => {
    const response = await api.get(`/admin/users/warehouse-staff/${id}`);
    return response.data;
  },

  /**
   * Admin approves a pending warehouse staff profile (ACTIVE).
   * Endpoint: PATCH /api/admin/users/{id}/warehouse-staff/approve
   */
  approveStaff: async (id) => {
    const response = await api.patch(`/admin/users/${id}/warehouse-staff/approve`);
    return response.data;
  },

  /**
   * Admin rejects a warehouse staff application (REJECTED).
   * Endpoint: PATCH /api/admin/users/{id}/warehouse-staff/reject
   */
  rejectStaff: async (id, reason) => {
    const response = await api.patch(`/admin/users/${id}/warehouse-staff/reject`, {
      reason: reason || 'Application declined by administrator.',
    });
    return response.data;
  },

  /**
   * Admin suspends an active warehouse staff account (SUSPENDED).
   * Endpoint: PATCH /api/admin/users/{id}/warehouse-staff/suspend
   */
  suspendStaff: async (id) => {
    const response = await api.patch(`/admin/users/${id}/warehouse-staff/suspend`);
    return response.data;
  },

  /**
   * Admin reactivates a suspended warehouse staff account (ACTIVE).
   * Endpoint: PATCH /api/admin/users/{id}/warehouse-staff/reactivate
   */
  reactivateStaff: async (id) => {
    const response = await api.patch(`/admin/users/${id}/warehouse-staff/reactivate`);
    return response.data;
  },
};

export default warehouseStaffService;
