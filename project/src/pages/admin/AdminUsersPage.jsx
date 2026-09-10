import { adminListUsers } from '../../lib/adminApi';
import { useFetch } from '../../hooks/useFetch';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { DataTable } from '../../components/DataTable';

const roleBadge = {
  ADMIN: 'badge-info', VENDOR: 'badge-success', CUSTOMER: 'badge-neutral', WAREHOUSE: 'badge-warning',
};

export function AdminUsersPage() {
  const { data: users, loading, error, refetch } = useFetch(adminListUsers, []);

  if (loading) return <PageLoader label="Loading users…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  const columns = [
    { key: 'full_name', label: 'Name', render: (u) => <span className="font-medium text-slate-800">{u.full_name || '—'}</span> },
    { key: 'email', label: 'Email' },
    { key: 'role', label: 'Role', render: (u) => <span className={roleBadge[u.role] || 'badge-neutral'}>{u.role}</span> },
    { key: 'phone', label: 'Phone', render: (u) => u.phone || '—' },
    { key: 'created_at', label: 'Joined', render: (u) => new Date(u.created_at).toLocaleDateString() },
  ];

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-500">{(users || []).length} user(s)</p>
      <DataTable columns={columns} rows={users || []} emptyMessage="No users found." />
    </div>
  );
}
