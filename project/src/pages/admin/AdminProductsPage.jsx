import { adminListAllProducts } from '../../lib/adminApi';
import { useFetch } from '../../hooks/useFetch';
import { PageLoader } from '../../components/Loader';
import { ErrorState } from '../../components/States';
import { DataTable } from '../../components/DataTable';

export function AdminProductsPage() {
  const { data: products, loading, error, refetch } = useFetch(adminListAllProducts, []);

  if (loading) return <PageLoader label="Loading products…" />;
  if (error) return <ErrorState message={error} onRetry={refetch} />;

  const columns = [
    { key: 'name', label: 'Product', render: (p) => <span className="font-medium text-slate-800">{p.name}</span> },
    { key: 'vendor', label: 'Vendor', render: (p) => p.vendor?.name || '—' },
    { key: 'category', label: 'Category', render: (p) => p.category?.name || '—' },
    { key: 'price', label: 'Price', render: (p) => `$${Number(p.price).toFixed(2)}` },
    { key: 'stock', label: 'Stock', render: (p) => p.stock_quantity },
    { key: 'status', label: 'Status', render: (p) => {
      const out = p.stock_quantity <= 0;
      const low = !out && p.stock_quantity <= (p.low_stock_threshold || 5);
      return <span className={out ? 'badge-danger' : low ? 'badge-warning' : 'badge-success'}>
        {out ? 'Out of stock' : low ? 'Low stock' : 'In stock'}
      </span>;
    } },
  ];

  return (
    <div className="space-y-4">
      <p className="text-sm text-slate-500">{(products || []).length} product(s)</p>
      <DataTable columns={columns} rows={products || []} emptyMessage="No products found." />
    </div>
  );
}
