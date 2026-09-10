import { adminListVendors } from '../../lib/adminApi';
import { useFetch } from '../../hooks/useFetch';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { DataTable } from '../../components/DataTable';

export function AdminVendorsPage() {
  const { data: vendors, loading, error, refetch } = useFetch(adminListVendors, []);

  if (loading) return <PageLoader label="Loading vendors…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  const columns = [
    { key: 'name', label: 'Store', render: (v) => <span className="font-medium text-slate-800">{v.name}</span> },
    { key: 'email', label: 'Owner email', render: (v) => v.profile?.email || '—' },
    { key: 'description', label: 'Description', render: (v) => v.description || '—' },
    { key: 'is_active', label: 'Status', render: (v) => (
      <span className={v.is_active ? 'badge-success' : 'badge-danger'}>{v.is_active ? 'Active' : 'Inactive'}</span>
    ) },
    { key: 'created_at', label: 'Joined', render: (v) => new Date(v.created_at).toLocaleDateString() },
  ];

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-500">{(vendors || []).length} vendor(s)</p>
      <DataTable columns={columns} rows={vendors || []} emptyMessage="No vendors found." />
    </div>
  );
}
