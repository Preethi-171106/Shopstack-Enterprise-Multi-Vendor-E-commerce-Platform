import api from './api';

export const notificationService = {
  /**
   * Get paginated notifications for current user.
   * @param {string} filter 'ALL' or 'UNREAD'
   * @param {number} page
   * @param {number} size
   */
  getMyNotifications: async (filter = 'ALL', page = 0, size = 20) => {
    const params = { page, size };
    if (filter && filter !== 'ALL') {
      params.filter = filter;
    }
    const response = await api.get('/notifications', { params });
    return response.data;
  },

  /**
   * Get unread notification count.
   */
  getUnreadCount: async () => {
    const response = await api.get('/notifications/unread-count');
    return response.data;
  },

  /**
   * Mark a single notification as read.
   * @param {number} id
   */
  markAsRead: async (id) => {
    const response = await api.patch(`/notifications/${id}/read`);
    return response.data;
  },

  /**
   * Mark all notifications as read.
   */
  markAllAsRead: async () => {
    const response = await api.patch('/notifications/read-all');
    return response.data;
  },

  /**
   * Delete a single notification.
   * @param {number} id
   */
  deleteNotification: async (id) => {
    const response = await api.delete(`/notifications/${id}`);
    return response.data;
  },

  /**
   * Delete all notifications for the current user.
   */
  deleteAllNotifications: async () => {
    const response = await api.delete('/notifications');
    return response.data;
  },
};

export default notificationService;
