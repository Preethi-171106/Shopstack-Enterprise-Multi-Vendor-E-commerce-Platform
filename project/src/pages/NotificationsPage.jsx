import { Bell, Check } from 'lucide-react';
import { useFetch } from '../hooks/useFetch';
import { listNotifications, markNotificationRead } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

const typeAccent = {
  SUCCESS: 'bg-emerald-50 text-emerald-600',
  ERROR: 'bg-rose-50 text-rose-600',
  INFO: 'bg-brand-50 text-brand-600',
  WARNING: 'bg-amber-50 text-amber-600',
};

export function NotificationsPage() {
  const { user } = useAuth();
  const { data: notifications, loading, error, refetch, setData } = useFetch(
    () => listNotifications(user.id),
    [user?.id]
  );

  const markRead = async (id) => {
    try {
      await markNotificationRead(id);
      setData((prev) => (prev || []).map((n) => (n.id === id ? { ...n, is_read: true } : n)));
    } catch {
      refetch();
    }
  };

  if (loading) return <PageLoader label="Loading notifications…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="rounded-lg bg-brand-50 p-2.5 text-brand-600">
          <Bell size={22} />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Notifications</h1>
          <p className="text-sm text-slate-500">{(notifications || []).filter((n) => !n.is_read).length} unread</p>
        </div>
      </div>

      {(!notifications || notifications.length === 0) ? (
        <EmptyState title="No notifications" description="You are all caught up." icon={Bell} />
      ) : (
        <div className="space-y-2">
          {notifications.map((n) => (
            <div key={n.id} className={`card flex items-start gap-3 p-4 ${n.is_read ? 'opacity-70' : ''}`}>
              <div className={`rounded-lg p-2 ${typeAccent[n.type] || typeAccent.INFO}`}>
                <Bell size={16} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-800">{n.title}</p>
                <p className="text-sm text-slate-600">{n.message}</p>
                <p className="mt-1 text-xs text-slate-400">{new Date(n.created_at).toLocaleString()}</p>
              </div>
              {!n.is_read && (
                <button onClick={() => markRead(n.id)} className="text-slate-400 hover:text-brand-600" aria-label="Mark read">
                  <Check size={18} />
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
