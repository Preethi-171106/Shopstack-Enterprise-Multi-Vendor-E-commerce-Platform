import api from './api';

export const authService = {
  /**
   * Registers a new user as CUSTOMER or VENDOR.
   * Endpoint: POST /api/auth/register
   *
   * @param {Object} userData - Registration form data
   * @param {string} userData.firstName
   * @param {string} userData.lastName
   * @param {string} userData.email
   * @param {string} userData.password
   * @param {string} [userData.phoneNumber]
   * @param {string} [userData.registrationRole] - 'CUSTOMER' (default) or 'VENDOR'
   */
  register: async (userData) => {
    const response = await api.post('/auth/register', {
      firstName: userData.firstName,
      lastName: userData.lastName,
      email: userData.email,
      password: userData.password,
      phoneNumber: userData.phoneNumber || '',
      // Pass the selected role — backend validates it.
      // CUSTOMER, VENDOR, and ADMIN are accepted.
      // WAREHOUSE_STAFF is rejected server-side (admin-created only).
      registrationRole: userData.registrationRole || 'CUSTOMER',
    });
    return response.data;
  },

  /**
   * Authenticates user and returns JWT token + user details.
   * Endpoint: POST /api/auth/login
   */
  login: async (credentials) => {
    const response = await api.post('/auth/login', {
      email: credentials.email,
      password: credentials.password,
    });
    return response.data; // { accessToken, tokenType, expiresIn, user }
  },

  /**
   * Retrieves currently authenticated user details.
   * Endpoint: GET /api/auth/me
   */
  getCurrentUser: async () => {
    const response = await api.get('/auth/me');
    return response.data; // UserResponse
  },

  forgotPassword: async (email) => {
    const response = await api.post('/auth/forgot-password', { email });
    return response.data;
  },

  resetPassword: async (token, newPassword) => {
    const response = await api.post(`/auth/reset-password${token ? `?token=${encodeURIComponent(token)}` : ''}`, {
      token,
      newPassword,
    });
    return response.data;
  },
};

export default authService;
