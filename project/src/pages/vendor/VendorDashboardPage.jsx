import { useFetch } from '../../hooks/useFetch';
import { getVendorByUserId } from '../../lib/api';
import { useAuth } from '../../context/AuthContext';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { Package, Boxes, ShoppingBag, DollarSign, AlertTriangle, TrendingUp } from 'lucide-react';
import { StatCard } from '../../components/DataTable';
import { Link } from 'react-router-dom';
import { listVendorProducts, listVendorOrders } from '../../lib/api';
import { useEffect, useState } from 'react';

export function VendorDashboardPage() {
  const { user } = useAuth();
  const { data: vendor, loading: vLoading, error: vError } = useFetch(
    () => getVendorByUserId(user.id),
    [user?.id]
  );
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loadingData, setLoadingData] = useState(true);
  const [dataError, setDataError] = useState(null);

  useEffect(() => {
    if (!vendor) return;
    (async () => {
      setLoadingData(true);
      try {
        const [p, o] = await Promise.all([
          listVendorProducts(vendor.id),
          listVendorOrders(vendor.id),
        ]);
        setProducts(p);
        setOrders(o);
      } catch (e) {
        setDataError(e.message);
      } finally {
        setLoadingData(false);
      }
    })();
  }, [vendor]);

  if (vLoading) return <PageLoader label="Loading your store…" />;
  if (vError) return <ErrorState message={vError} />;
  if (!vendor) {
    return (
      <div className="card p-8 text-center">
        <h2 className="text-lg font-semibold text-slate-800">No vendor store yet</h2>
        <p className="mt-1 text-sm text-slate-500">
          Your vendor profile has not been set up. Contact an admin to create your store.
        </p>
      </div>
    );
  }

  if (loadingData) return <PageLoader label="Loading dashboard…" />;
  if (dataError) return <ErrorState message={dataError} />;

  const revenue = orders
    .filter((o) => ['PAID', 'SHIPPED', 'DELIVERED'].includes(o.status))
    .reduce((s, o) => s + Number(o.total_amount), 0);
  const lowStock = products.filter((p) => p.stock_quantity > 0 && p.stock_quantity <= (p.low_stock_threshold || 5));
  const outOfStock = products.filter((p) => p.stock_quantity <= 0);

  return (
    <div className="space-y-6">
      <div className="card flex items-center gap-4 p-5">
        <div className="grid h-12 w-12 place-items-center rounded-xl bg-brand-100 text-brand-700 font-bold">
          {vendor.name.charAt(0)}
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-800">{vendor.name}</h2>
          <p className="text-sm text-slate-500">{vendor.description || 'Your store'}</p>
        </div>
        <span className={`badge ml-auto ${vendor.is_active ? 'badge-success' : 'badge-danger'}`}>
          {vendor.is_active ? 'Active' : 'Inactive'}
        </span>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Revenue" value={`$${revenue.toFixed(2)}`} icon={DollarSign} accent="emerald" />
        <StatCard label="Orders" value={orders.length} icon={ShoppingBag} accent="brand" />
        <StatCard label="Products" value={products.length} icon={Package} accent="slate" />
        <StatCard label="Low stock" value={lowStock.length + outOfStock.length} icon={AlertTriangle} accent="amber" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card p-5">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="flex items-center gap-2 font-semibold text-slate-800">
              <ShoppingBag size={18} className="text-brand-600" /> Recent orders
            </h3>
            <Link to="/vendor/orders" className="text-sm font-medium text-brand-600">View all →</Link>
          </div>
          {orders.length === 0 ? (
            <p className="text-sm text-slate-500">No orders yet.</p>
          ) : (
            <div className="space-y-2">
              {orders.slice(0, 5).map((o) => (
                <div key={o.id} className="flex items-center justify-between rounded-lg bg-slate-50 p-3 text-sm">
                  <span className="font-medium text-slate-700">#{o.id.slice(0, 8)}</span>
                  <span className="text-slate-500">{o.user?.full_name || 'Customer'}</span>
                  <span className="badge-info">{o.status}</span>
                  <span className="font-bold text-slate-900">${Number(o.total_amount).toFixed(2)}</span>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="card p-5">
          <div className="mb-3 flex items-center justify-between">
            <h3 className="flex items-center gap-2 font-semibold text-slate-800">
              <Boxes size={18} className="text-brand-600" /> Stock alerts
            </h3>
            <Link to="/vendor/inventory" className="text-sm font-medium text-brand-600">Manage →</Link>
          </div>
          {lowStock.length === 0 && outOfStock.length === 0 ? (
            <p className="text-sm text-slate-500">All products are well stocked.</p>
          ) : (
            <div className="space-y-2">
              {[...outOfStock, ...lowStock].slice(0, 5).map((p) => (
                <div key={p.id} className="flex items-center justify-between rounded-lg bg-slate-50 p-3 text-sm">
                  <span className="font-medium text-slate-700">{p.name}</span>
                  <span className={p.stock_quantity <= 0 ? 'badge-danger' : 'badge-warning'}>
                    {p.stock_quantity <= 0 ? 'Out of stock' : `${p.stock_quantity} left`}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
