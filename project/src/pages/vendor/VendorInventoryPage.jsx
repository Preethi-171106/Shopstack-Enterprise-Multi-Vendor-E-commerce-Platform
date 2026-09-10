import { useEffect, useState } from 'react';
import { Boxes, Save } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { getVendorByUserId, listVendorProducts, updateProduct } from '../../lib/api';
import { PageLoader, ButtonLoader } from '../../components/Loader';
import { ErrorState, EmptyState } from '../../components/States';
import { StatCard } from '../../components/DataTable';

export function VendorInventoryPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editing, setEditing] = useState({}); // {id: qty}
  const [savingId, setSavingId] = useState(null);

  const load = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const v = await getVendorByUserId(user.id);
      const prods = v ? await listVendorProducts(v.id) : [];
      setProducts(prods);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [user]);

  const saveQty = async (p) => {
    const qty = editing[p.id];
    if (qty == null) return;
    setSavingId(p.id);
    try {
      await updateProduct(p.id, { stock_quantity: Number(qty) });
      toast.success('Stock updated');
      setEditing((e) => { const n = { ...e }; delete n[p.id]; return n; });
      await load();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSavingId(null);
    }
  };

  if (loading) return <PageLoader label="Loading inventory…" />;
  if (error) return <ErrorState message={error} />;

  const totalValue = products.reduce((s, p) => s + Number(p.price) * p.stock_quantity, 0);
  const lowStock = products.filter((p) => p.stock_quantity > 0 && p.stock_quantity <= (p.low_stock_threshold || 5));
  const outOfStock = products.filter((p) => p.stock_quantity <= 0);

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label="Inventory value" value={`$${totalValue.toFixed(2)}`} icon={Boxes} accent="emerald" />
        <StatCard label="Low stock" value={lowStock.length} icon={Boxes} accent="amber" />
        <StatCard label="Out of stock" value={outOfStock.length} icon={Boxes} accent="rose" />
      </div>

      {products.length === 0 ? (
        <EmptyState title="No inventory" description="Add products to manage your inventory." icon={Boxes} />
      ) : (
        <div className="overflow-x-auto rounded-xl bg-white ring-1 ring-slate-100">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Product</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Unit price</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Stock</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-slate-500">Update</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {products.map((p) => {
                const status = p.stock_quantity <= 0 ? 'Out of stock' : p.stock_quantity <= (p.low_stock_threshold || 5) ? 'Low stock' : 'In stock';
                const badge = p.stock_quantity <= 0 ? 'badge-danger' : p.stock_quantity <= (p.low_stock_threshold || 5) ? 'badge-warning' : 'badge-success';
                return (
                  <tr key={p.id}>
                    <td className="px-4 py-3 text-sm font-medium text-slate-800">{p.name}</td>
                    <td className="px-4 py-3 text-sm text-slate-700">${Number(p.price).toFixed(2)}</td>
                    <td className="px-4 py-3">
                      <input
                        type="number"
                        className="input !py-1.5 w-24"
                        value={editing[p.id] != null ? editing[p.id] : p.stock_quantity}
                        onChange={(e) => setEditing((ed) => ({ ...ed, [p.id]: e.target.value }))}
                      />
                    </td>
                    <td className="px-4 py-3"><span className={badge}>{status}</span></td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => saveQty(p)}
                        disabled={editing[p.id] == null || savingId === p.id}
                        className="btn-secondary !py-1.5"
                      >
                        {savingId === p.id ? <ButtonLoader /> : <><Save size={14} /> Save</>}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
