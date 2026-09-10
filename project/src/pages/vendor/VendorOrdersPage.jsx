import { useEffect, useState } from 'react';
import { ShoppingBag } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { getVendorByUserId, listVendorOrders } from '../../lib/api';
import { PageLoader } from '../../components/Loader';
import { ErrorState, EmptyState } from '../../components/States';
import { DataTable } from '../../components/DataTable';

const statusBadge = {
  PENDING: 'badge-warning', PAID: 'badge-info', SHIPPED: 'badge-info',
  DELIVERED: 'badge-success', CANCELLED: 'badge-danger', REFUNDED: 'badge-danger',
};

export function VendorOrdersPage() {
  const { user } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      if (!user) return;
      setLoading(true);
      try {
        const v = await getVendorByUserId(user.id);
        const data = v ? await listVendorOrders(v.id) : [];
        setOrders(data);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [user]);

  if (loading) return <PageLoader label="Loading orders…" />;
  if (error) return <ErrorState message={error} />;
  if (orders.length === 0) {
    return <EmptyState title="No orders yet" description="Orders for your products will appear here." icon={ShoppingBag} />;
  }

  const columns = [
    { key: 'id', label: 'Order', render: (o) => <span className="font-mono text-xs">#{o.id.slice(0, 8)}</span> },
    { key: 'customer', label: 'Customer', render: (o) => o.user?.full_name || 'Customer' },
    { key: 'items', label: 'Items', render: (o) => (o.items || []).length },
    { key: 'total', label: 'Total', render: (o) => `$${Number(o.total_amount).toFixed(2)}` },
    { key: 'status', label: 'Status', render: (o) => <span className={statusBadge[o.status] || 'badge-neutral'}>{o.status}</span> },
    { key: 'date', label: 'Date', render: (o) => new Date(o.created_at).toLocaleDateString() },
  ];

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-500">{orders.length} order(s)</p>
      <DataTable columns={columns} rows={orders} />
    </div>
  );
}
