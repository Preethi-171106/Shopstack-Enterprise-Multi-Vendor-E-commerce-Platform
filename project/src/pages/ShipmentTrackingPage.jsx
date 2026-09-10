import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Truck, Package, Warehouse, CheckCircle2, Home } from 'lucide-react';
import { useFetch } from '../hooks/useFetch';
import { getShipmentByOrder, getOrder } from '../lib/api';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

const steps = [
  { key: 'PENDING', label: 'Order placed', icon: Package },
  { key: 'PROCESSING', label: 'Processing', icon: Warehouse },
  { key: 'SHIPPED', label: 'Shipped', icon: Truck },
  { key: 'DELIVERED', label: 'Delivered', icon: Home },
];

const orderIndex = (s) => steps.findIndex((x) => x.key === s);

export function ShipmentTrackingPage() {
  const { orderId } = useParams();
  const { data: shipment, loading: sLoading, error: sError } = useFetch(
    () => getShipmentByOrder(orderId),
    [orderId]
  );
  const { data: order } = useFetch(() => getOrder(orderId), [orderId]);

  if (sLoading) return <PageLoader label="Loading tracking info…" />;
  if (sError) return <ErrorState message={sError} />;
  if (!shipment) {
    return (
      <EmptyState
        title="No shipment yet"
        description="Tracking information will appear once your order has been shipped."
        action={<Link to={`/orders/${orderId}`} className="btn-primary">Back to order</Link>}
      />
    );
  }

  const currentIdx = orderIndex(shipment.status);
  const activeIdx = currentIdx < 0 ? 0 : currentIdx;

  return (
    <div className="space-y-6">
      <Link to={`/orders/${orderId}`} className="inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-brand-600">
        <ArrowLeft size={16} /> Back to order
      </Link>

      <div>
        <h1 className="text-2xl font-bold text-slate-900">Track shipment</h1>
        <p className="text-sm text-slate-500">Order #{(orderId || '').slice(0, 8)}</p>
      </div>

      <div className="card p-6">
        <div className="mb-6 grid gap-4 sm:grid-cols-2">
          <div>
            <p className="text-xs font-medium uppercase text-slate-400">Carrier</p>
            <p className="text-sm font-semibold text-slate-800">{shipment.carrier}</p>
          </div>
          <div>
            <p className="text-xs font-medium uppercase text-slate-400">Tracking number</p>
            <p className="font-mono text-sm font-semibold text-slate-800">{shipment.tracking_number || '—'}</p>
          </div>
          {shipment.estimated_delivery && (
            <div>
              <p className="text-xs font-medium uppercase text-slate-400">Estimated delivery</p>
              <p className="text-sm font-semibold text-slate-800">{new Date(shipment.estimated_delivery).toLocaleDateString()}</p>
            </div>
          )}
          {shipment.shipped_at && (
            <div>
              <p className="text-xs font-medium uppercase text-slate-400">Shipped on</p>
              <p className="text-sm font-semibold text-slate-800">{new Date(shipment.shipped_at).toLocaleDateString()}</p>
            </div>
          )}
        </div>

        {/* Progress timeline */}
        <div className="relative flex justify-between">
          <div className="absolute left-0 right-0 top-5 h-1 bg-slate-200" />
          <div
            className="absolute left-0 top-5 h-1 bg-brand-600 transition-all duration-500"
            style={{ width: `${(activeIdx / (steps.length - 1)) * 100}%` }}
          />
          {steps.map((s, i) => {
            const done = i <= activeIdx;
            const Icon = s.icon;
            return (
              <div key={s.key} className="relative z-10 flex flex-col items-center gap-2">
                <div
                  className={`grid h-10 w-10 place-items-center rounded-full ring-4 ring-white ${
                    done ? 'bg-brand-600 text-white' : 'bg-slate-200 text-slate-400'
                  }`}
                >
                  {done && i < activeIdx ? <CheckCircle2 size={18} /> : <Icon size={18} />}
                </div>
                <span className={`text-xs font-medium ${done ? 'text-brand-700' : 'text-slate-400'}`}>{s.label}</span>
              </div>
            );
          })}
        </div>

        <div className="mt-6 rounded-lg bg-brand-50 p-4 text-sm text-brand-800">
          Current status: <span className="font-semibold">{shipment.status}</span>
        </div>
      </div>
    </div>
  );
}
