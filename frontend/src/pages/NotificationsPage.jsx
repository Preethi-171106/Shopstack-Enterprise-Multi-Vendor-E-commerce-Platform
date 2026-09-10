import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Bell,
  CheckCheck,
  Package,
  AlertCircle,
  Sparkles,
  Store,
  RotateCcw,
  Trash2,
  CheckCircle2,
  Clock,
  Filter,
  RefreshCw,
  ExternalLink,
  ShieldAlert,
} from 'lucide-react';
import { notificationService } from '../services/notificationService';
import { useApp } from '../context/AppContext';
import { useAuth } from '../context/AuthContext';
import Button from '../components/common/Button';
import EmptyState from '../components/common/EmptyState';
import Loading from '../components/common/Loading';

const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState('ALL'); // ALL, UNREAD
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const { showNotification } = useApp();
  const { user } = useAuth();
  const navigate = useNavigate();

  const fetchNotifications = useCallback(async (filterType = activeFilter, pageNum = page) => {
    try {
      setLoading(true);
      const data = await notificationService.getMyNotifications(filterType, pageNum, 15);
      setNotifications(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);

      const unreadData = await notificationService.getUnreadCount();
      setUnreadCount(unreadData.unreadCount || 0);
    } catch (err) {
      console.error('Failed to load notifications:', err);
      showNotification('Failed to load notifications', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeFilter, page, showNotification]);

  useEffect(() => {
    fetchNotifications(activeFilter, page);
  }, [fetchNotifications, activeFilter, page]);

  const handleMarkAsRead = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      await notificationService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, readStatus: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
      showNotification('Notification marked as read', 'info');
    } catch (err) {
      console.error('Failed to mark read:', err);
      showNotification('Failed to mark notification as read', 'error');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, readStatus: true })));
      setUnreadCount(0);
      showNotification('All notifications marked as read', 'success');
    } catch (err) {
      console.error('Failed to mark all read:', err);
      showNotification('Failed to mark notifications as read', 'error');
    }
  };

  const handleDelete = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      await notificationService.deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      setTotalElements((prev) => Math.max(0, prev - 1));
      showNotification('Notification removed', 'info');
    } catch (err) {
      console.error('Failed to delete notification:', err);
      showNotification('Failed to delete notification', 'error');
    }
  };

  const handleDeleteAll = async () => {
    if (!window.confirm('Are you sure you want to delete all notifications?')) return;
    try {
      await notificationService.deleteAllNotifications();
      setNotifications([]);
      setUnreadCount(0);
      setTotalElements(0);
      showNotification('All notifications deleted', 'success');
    } catch (err) {
      console.error('Failed to clear notifications:', err);
      showNotification('Failed to clear notifications', 'error');
    }
  };

  const handleNotificationClick = (notification) => {
    if (!notification.readStatus) {
      handleMarkAsRead(notification.id);
    }

    // Role-based contextual navigation
    if (notification.referenceId) {
      if (notification.notificationType?.startsWith('ORDER_')) {
        if (user?.role === 'CUSTOMER') {
          navigate(`/orders/${notification.referenceId}`);
        } else if (user?.role === 'WAREHOUSE_STAFF') {
          navigate('/dashboard/warehouse');
        } else if (user?.role === 'VENDOR') {
          navigate('/dashboard/vendor');
        } else {
          navigate('/dashboard/admin');
        }
      } else if (notification.notificationType?.startsWith('RETURN_') || notification.notificationType?.startsWith('REFUND_')) {
        navigate('/dashboard/customer');
      } else if (notification.notificationType?.startsWith('VENDOR_')) {
        navigate('/dashboard/vendor');
      }
    }
  };

  const getNotificationIcon = (type) => {
    if (!type) return <Bell className="w-5 h-5 text-indigo-400" />;
    if (type.startsWith('ORDER_')) {
      return <Package className="w-5 h-5 text-indigo-400" />;
    }
    if (type.startsWith('RETURN_') || type.startsWith('REFUND_')) {
      return <RotateCcw className="w-5 h-5 text-amber-400" />;
    }
    if (type.startsWith('VENDOR_')) {
      return <Store className="w-5 h-5 text-emerald-400" />;
    }
    if (type === 'LOW_STOCK') {
      return <AlertCircle className="w-5 h-5 text-rose-400" />;
    }
    if (type === 'COUPON_AVAILABLE' || type === 'PROMOTION') {
      return <Sparkles className="w-5 h-5 text-purple-400" />;
    }
    if (type === 'SYSTEM' || type === 'WAREHOUSE_ALERT') {
      return <ShieldAlert className="w-5 h-5 text-cyan-400" />;
    }
    return <Bell className="w-5 h-5 text-indigo-400" />;
  };

  const formatTimestamp = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-5xl mx-auto space-y-6">
        
        {/* Header Bar */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-xl backdrop-blur-md">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <Bell className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-black text-white">Notifications</h1>
                {unreadCount > 0 && (
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 animate-pulse">
                    {unreadCount} Unread
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                Stay updated with orders, account alerts, and marketplace updates
              </p>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => fetchNotifications(activeFilter, page)}
              className="p-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-700/60 transition-colors"
              title="Refresh"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
            {unreadCount > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleMarkAllRead}
                icon={CheckCheck}
                className="text-xs"
              >
                Mark All Read
              </Button>
            )}
            {notifications.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleDeleteAll}
                icon={Trash2}
                className="text-xs text-rose-400 hover:text-rose-300 hover:bg-rose-500/10"
              >
                Clear All
              </Button>
            )}
          </div>
        </div>

        {/* Filter Tabs */}
        <div className="flex items-center gap-2 border-b border-slate-800 pb-3">
          <button
            onClick={() => {
              setActiveFilter('ALL');
              setPage(0);
            }}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeFilter === 'ALL'
                ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-900'
            }`}
          >
            <span>All Notifications</span>
            <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-slate-950/40">
              {totalElements}
            </span>
          </button>

          <button
            onClick={() => {
              setActiveFilter('UNREAD');
              setPage(0);
            }}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeFilter === 'UNREAD'
                ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-900'
            }`}
          >
            <span>Unread Only</span>
            {unreadCount > 0 && (
              <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-rose-500 text-white font-bold">
                {unreadCount}
              </span>
            )}
          </button>
        </div>

        {/* Notification List */}
        {loading ? (
          <div className="py-16">
            <Loading message="Loading notifications..." />
          </div>
        ) : notifications.length === 0 ? (
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-12 text-center">
            <EmptyState
              icon={Bell}
              title={activeFilter === 'UNREAD' ? 'No Unread Notifications' : 'No Notifications Yet'}
              description={
                activeFilter === 'UNREAD'
                  ? "You're all caught up! There are no unread notifications right now."
                  : 'Important order, shipment, and account updates will appear here.'
              }
            />
          </div>
        ) : (
          <div className="space-y-3">
            {notifications.map((n) => (
              <div
                key={n.id}
                onClick={() => handleNotificationClick(n)}
                className={`group relative flex items-start gap-4 p-4 sm:p-5 rounded-2xl border transition-all cursor-pointer ${
                  !n.readStatus
                    ? 'bg-slate-900 border-indigo-500/40 shadow-lg shadow-indigo-500/5 hover:border-indigo-500/70'
                    : 'bg-slate-900/50 border-slate-800/80 hover:bg-slate-900/80 hover:border-slate-700'
                }`}
              >
                {/* Unread Glow Indicator */}
                {!n.readStatus && (
                  <div className="absolute left-2 top-1/2 -translate-y-1/2 w-1.5 h-8 bg-indigo-500 rounded-full" />
                )}

                {/* Icon */}
                <div className="shrink-0 mt-0.5 p-3 rounded-xl bg-slate-800 border border-slate-700/60">
                  {getNotificationIcon(n.notificationType)}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0 pr-2">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className={`text-sm font-bold truncate ${!n.readStatus ? 'text-white' : 'text-slate-300'}`}>
                      {n.title}
                    </h3>
                    <span className="shrink-0 text-[11px] text-slate-500 flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {formatTimestamp(n.createdAt)}
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 mt-1 leading-relaxed line-clamp-2">
                    {n.message}
                  </p>

                  <div className="flex items-center gap-3 mt-3">
                    <span className="text-[10px] font-semibold uppercase px-2 py-0.5 rounded-md bg-slate-800 text-slate-400 border border-slate-700">
                      {n.notificationType?.replace(/_/g, ' ') || 'GENERAL'}
                    </span>

                    {n.referenceId && (
                      <span className="text-[11px] text-indigo-400 hover:text-indigo-300 font-medium flex items-center gap-1">
                        View Details <ExternalLink className="w-3 h-3" />
                      </span>
                    )}
                  </div>
                </div>

                {/* Action Buttons on Hover */}
                <div className="shrink-0 flex items-center gap-1 opacity-80 group-hover:opacity-100 transition-opacity">
                  {!n.readStatus && (
                    <button
                      onClick={(e) => handleMarkAsRead(n.id, e)}
                      title="Mark as Read"
                      className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition-colors"
                    >
                      <CheckCircle2 className="w-4 h-4 text-indigo-400" />
                    </button>
                  )}
                  <button
                    onClick={(e) => handleDelete(n.id, e)}
                    title="Delete Notification"
                    className="p-2 rounded-lg bg-slate-800 hover:bg-rose-500/20 text-slate-400 hover:text-rose-400 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between pt-4 border-t border-slate-800">
                <span className="text-xs text-slate-400">
                  Page {page + 1} of {totalPages}
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    className="text-xs"
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                    className="text-xs"
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
