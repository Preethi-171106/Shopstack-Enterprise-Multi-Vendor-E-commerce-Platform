import { CreditCard } from 'lucide-react';
import { useFetch } from '../hooks/useFetch';
import { listPayments } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

const statusBadge = {
  SUCCESSFUL: 'badge-success', FAILED: 'badge-danger', REFUNDED: 'badge-warning', PENDING: 'badge-warning',
};

export function PaymentsPage() {
  const { user } = useAuth();
  const { data: payments, loading, error, refetch } = useFetch(
    () => listPayments(user.id),
    [user?.id]
  );

  if (loading) return <PageLoader label="Loading payments…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  const totalPaid = (payments || []).filter((p) => p.status === 'SUCCESSFUL').reduce((s, p) => s + Number(p.amount), 0);

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="rounded-lg bg-brand-50 p-2.5 text-brand-600">
          <CreditCard size={22} />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Payments</h1>
          <p className="text-sm text-slate-500">Total paid: ${totalPaid.toFixed(2)}</p>
        </div>
      </div>

      {(!payments || payments.length === 0) ? (
        <EmptyState title="No payments yet" description="Your payment history will appear here." icon={CreditCard} />
      ) : (
        <div className="space-y-3">
          {payments.map((p) => (
            <div key={p.id} className="card flex flex-wrap items-center gap-4 p-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
                <CreditCard size={18} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-800">Order #{(p.order_id || '').slice(0, 8)}</p>
                <p className="text-xs text-slate-500">{p.payment_method} · {new Date(p.created_at).toLocaleDateString()}</p>
              </div>
              <span className={statusBadge[p.status] || 'badge-neutral'}>{p.status}</span>
              <p className="text-lg font-bold text-slate-900">${Number(p.amount).toFixed(2)}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
