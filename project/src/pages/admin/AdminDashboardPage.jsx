import { useEffect, useState } from 'react';
import { Users, Store, Package, ShoppingBag, DollarSign, Ticket, TrendingUp } from 'lucide-react';
import { adminAnalytics } from '../../lib/adminApi';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { StatCard } from '../../components/DataTable';
import {
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid,
  PieChart, Pie, Cell, Legend,
} from 'recharts';

const PIE_COLORS = ['#1c66f5', '#14b8a6', '#f59e0b', '#ef4444', '#64748b', '#a855f7'];

export function AdminDashboardPage() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const a = await adminAnalytics();
        setData(a);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <PageLoader label="Loading admin dashboard…" />;
  if (error) return <ErrorState message={error} />;

  const { counts, revenue, revenueSeries = [], statusCounts = {} } = data;
  const pieData = Object.entries(statusCounts).map(([name, value]) => ({ name, value }));

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard label="Total revenue" value={`$${Number(revenue || 0).toFixed(2)}`} icon={DollarSign} accent="emerald" />
        <StatCard label="Users" value={counts?.users || 0} icon={Users} accent="brand" />
        <StatCard label="Vendors" value={counts?.vendors || 0} icon={Store} accent="slate" />
        <StatCard label="Products" value={counts?.products || 0} icon={Package} accent="amber" />
        <StatCard label="Orders" value={counts?.orders || 0} icon={ShoppingBag} accent="brand" />
        <StatCard label="Coupons" value={counts?.coupons || 0} icon={Ticket} accent="rose" />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <section className="card p-5">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-slate-800">
            <TrendingUp size={18} className="text-brand-600" /> Revenue (last 14 days)
          </h3>
          {revenueSeries.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">No revenue data yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={revenueSeries}>
                <defs>
                  <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#1c66f5" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#1c66f5" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Area type="monotone" dataKey="value" stroke="#1c66f5" fill="url(#rev)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </section>

        <section className="card p-5">
          <h3 className="mb-4 font-semibold text-slate-800">Order status</h3>
          {pieData.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">No orders yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                  {pieData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Legend />
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </section>
      </div>
    </div>
  );
}
