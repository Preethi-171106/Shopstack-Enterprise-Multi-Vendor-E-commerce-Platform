import { Link } from 'react-router-dom';
import { Package } from 'lucide-react';
import { useFetch } from '../hooks/useFetch';
import { listOrders } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

const statusBadge = {
  PENDING: 'badge-warning', PAID: 'badge-info', SHIPPED: 'badge-info',
  DELIVERED: 'badge-success', CANCELLED: 'badge-danger', REFUNDED: 'badge-danger',
};

export function OrdersPage() {
  const { user } = useAuth();
  const { data: orders, loading, error, refetch } = useFetch(
    () => listOrders(user.id),
    [user?.id]
  );

  if (loading) return <PageLoader label="Loading orders…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;
  if (!orders || orders.length === 0) {
    return (
      <EmptyState
        title="No orders yet"
        description="When you place an order it will appear here."
        icon={Package}
        action={<Link to="/products" className="btn-primary">Start shopping</Link>}
      />
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">My orders</h1>
      <div className="space-y-3">
        {orders.map((o) => (
          <Link key={o.id} to={`/orders/${o.id}`} className="card flex flex-wrap items-center gap-4 p-5 transition hover:shadow-cardlg">
            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-brand-50 text-brand-600">
              <Package size={22} />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-slate-800">Order #{o.id.slice(0, 8)}</p>
              <p className="text-xs text-slate-500">
                {new Date(o.created_at).toLocaleDateString()} · {(o.items || []).length} item(s)
              </p>
            </div>
            <span className={statusBadge[o.status] || 'badge-neutral'}>{o.status}</span>
            <p className="text-lg font-bold text-slate-900">${Number(o.total_amount).toFixed(2)}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
