import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Truck, MapPin, CreditCard } from 'lucide-react';
import { useFetch } from '../hooks/useFetch';
import { getOrder, getShipmentByOrder } from '../lib/api';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

const statusBadge = {
  PENDING: 'badge-warning', PAID: 'badge-info', SHIPPED: 'badge-info',
  DELIVERED: 'badge-success', CANCELLED: 'badge-danger', REFUNDED: 'badge-danger',
};

export function OrderDetailsPage() {
  const { id } = useParams();
  const { data: order, loading, error, refetch } = useFetch(() => getOrder(id), [id]);
  const { data: shipment } = useFetch(() => getShipmentByOrder(id), [id]);

  if (loading) return <PageLoader label="Loading order…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;
  if (!order) return <EmptyState title="Order not found" />;

  const items = order.items || [];

  return (
    <div className="space-y-6">
      <Link to="/orders" className="inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-brand-600">
        <ArrowLeft size={16} /> Back to orders
      </Link>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Order #{order.id.slice(0, 8)}</h1>
          <p className="text-sm text-slate-500">{new Date(order.created_at).toLocaleString()}</p>
        </div>
        <span className={statusBadge[order.status] || 'badge-neutral'}>{order.status}</span>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-4">
          <section className="card divide-y divide-slate-100">
            {items.map((it) => (
              <div key={it.id} className="flex items-center gap-4 p-4">
                <div className="h-14 w-14 rounded-lg bg-slate-100" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-slate-800">{it.product_name}</p>
                  <p className="text-xs text-slate-500">Qty {it.quantity} · ${Number(it.unit_price).toFixed(2)} each</p>
                </div>
                <p className="font-bold text-slate-900">${Number(it.subtotal).toFixed(2)}</p>
              </div>
            ))}
          </section>

          {/* Shipment */}
          <section className="card p-5">
            <div className="mb-3 flex items-center gap-2">
              <Truck size={18} className="text-brand-600" />
              <h2 className="text-lg font-semibold text-slate-800">Shipment</h2>
            </div>
            {shipment ? (
              <div className="space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-slate-500">Carrier</span><span>{shipment.carrier}</span></div>
                <div className="flex justify-between"><span className="text-slate-500">Tracking #</span><span className="font-mono">{shipment.tracking_number || '—'}</span></div>
                <div className="flex justify-between"><span className="text-slate-500">Status</span><span className={statusBadge[shipment.status] || 'badge-neutral'}>{shipment.status}</span></div>
                {shipment.estimated_delivery && (
                  <div className="flex justify-between"><span className="text-slate-500">Est. delivery</span><span>{new Date(shipment.estimated_delivery).toLocaleDateString()}</span></div>
                )}
                <Link to={`/shipments/${order.id}`} className="btn-secondary mt-2 w-full">Track shipment</Link>
              </div>
            ) : (
              <p className="text-sm text-slate-500">Shipment info will appear once your order ships.</p>
            )}
          </section>
        </div>

        <aside className="space-y-4">
          <section className="card space-y-3 p-5">
            <h2 className="text-lg font-semibold text-slate-800">Summary</h2>
            <div className="flex justify-between text-sm"><span className="text-slate-500">Subtotal</span><span>${Number(order.total_amount).toFixed(2)}</span></div>
            <div className="flex justify-between text-sm"><span className="text-slate-500">Discount</span><span>-${Number(order.discount_amount).toFixed(2)}</span></div>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-base font-bold"><span>Total</span><span>${Number(order.total_amount).toFixed(2)}</span></div>
          </section>

          {order.address && (
            <section className="card p-5">
              <div className="mb-2 flex items-center gap-2">
                <MapPin size={18} className="text-brand-600" />
                <h2 className="text-lg font-semibold text-slate-800">Shipping to</h2>
              </div>
              <p className="text-sm text-slate-600">{order.address.full_name}</p>
              <p className="text-sm text-slate-600">{order.address.line1}</p>
              <p className="text-sm text-slate-600">{order.address.city}, {order.address.state} {order.address.postal_code}</p>
            </section>
          )}

          <section className="card p-5">
            <div className="mb-2 flex items-center gap-2">
              <CreditCard size={18} className="text-brand-600" />
              <h2 className="text-lg font-semibold text-slate-800">Payment</h2>
            </div>
            <p className="text-sm text-slate-600">Status: <span className={statusBadge[order.status] || 'badge-neutral'}>{order.status}</span></p>
            <Link to="/payments" className="btn-secondary mt-2 w-full">View payments</Link>
          </section>
        </aside>
      </div>
    </div>
  );
}
