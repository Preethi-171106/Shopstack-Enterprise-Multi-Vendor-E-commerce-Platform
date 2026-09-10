import { useEffect, useState } from 'react';
import { TrendingUp, Users, Store, Package, ShoppingBag, DollarSign } from 'lucide-react';
import { adminAnalytics } from '../../lib/adminApi';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { StatCard } from '../../components/DataTable';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
  AreaChart, Area, PieChart, Pie, Cell, Legend,
} from 'recharts';

const PIE_COLORS = ['#1c66f5', '#14b8a6', '#f59e0b', '#ef4444', '#64748b', '#a855f7'];

export function AdminAnalyticsPage() {
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

  if (loading) return <PageLoader label="Loading analytics…" />;
  if (error) return <ErrorState message={error} />;

  const { counts, revenue, revenueSeries = [], statusCounts = {} } = data;
  const barData = [
    { name: 'Users', value: counts?.users || 0 },
    { name: 'Vendors', value: counts?.vendors || 0 },
    { name: 'Products', value: counts?.products || 0 },
    { name: 'Orders', value: counts?.orders || 0 },
    { name: 'Coupons', value: counts?.coupons || 0 },
  ];
  const pieData = Object.entries(statusCounts).map(([name, value]) => ({ name, value }));

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Revenue" value={`$${Number(revenue || 0).toFixed(2)}`} icon={DollarSign} accent="emerald" />
        <StatCard label="Orders" value={counts?.orders || 0} icon={ShoppingBag} accent="brand" />
        <StatCard label="Avg order value" value={counts?.orders ? `$${(Number(revenue || 0) / counts.orders).toFixed(2)}` : '$0.00'} icon={TrendingUp} accent="amber" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card p-5">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-slate-800">
            <TrendingUp size={18} className="text-brand-600" /> Revenue trend
          </h3>
          {revenueSeries.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">No revenue data yet.</p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <AreaChart data={revenueSeries}>
                <defs>
                  <linearGradient id="rev2" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#14b8a6" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#14b8a6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Area type="monotone" dataKey="value" stroke="#14b8a6" fill="url(#rev2)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </section>

        <section className="card p-5">
          <h3 className="mb-4 flex items-center gap-2 font-semibold text-slate-800">
            <Users size={18} className="text-brand-600" /> Platform counts
          </h3>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="name" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Bar dataKey="value" fill="#1c66f5" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </section>
      </div>

      <section className="card p-5">
        <h3 className="mb-4 font-semibold text-slate-800">Order status distribution</h3>
        {pieData.length === 0 ? (
          <p className="py-8 text-center text-sm text-slate-400">No orders yet.</p>
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <PieChart>
              <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label>
                {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
              </Pie>
              <Legend />
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        )}
      </section>
    </div>
  );
}
