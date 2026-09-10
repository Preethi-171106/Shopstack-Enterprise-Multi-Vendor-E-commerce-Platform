import { useEffect, useState } from 'react';
import { Plus, Pencil, Trash2, Package, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { getVendorByUserId, listVendorProducts, listCategories, createProduct, updateProduct, deleteProduct } from '../../lib/api';
import { PageLoader, ButtonLoader } from '../../components/Loader';
import { ErrorState, EmptyState } from '../../components/States';
import { DataTable } from '../../components/DataTable';

export function VendorProductsPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [vendor, setVendor] = useState(null);
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: '', description: '', price: '', stock_quantity: '', low_stock_threshold: '5', image_url: '', category_id: '',
  });

  const load = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const v = await getVendorByUserId(user.id);
      setVendor(v);
      const [prods, cats] = await Promise.all([
        v ? listVendorProducts(v.id) : Promise.resolve([]),
        listCategories(),
      ]);
      setProducts(prods);
      setCategories(cats);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [user]);

  const openNew = () => {
    setEditing(null);
    setForm({ name: '', description: '', price: '', stock_quantity: '', low_stock_threshold: '5', image_url: '', category_id: '' });
    setShowForm(true);
  };

  const openEdit = (p) => {
    setEditing(p);
    setForm({
      name: p.name, description: p.description || '', price: String(p.price),
      stock_quantity: String(p.stock_quantity), low_stock_threshold: String(p.low_stock_threshold || 5),
      image_url: p.image_url || '', category_id: p.category_id || '',
    });
    setShowForm(true);
  };

  const save = async (e) => {
    e.preventDefault();
    if (!vendor) return;
    setSaving(true);
    const payload = {
      name: form.name,
      description: form.description,
      price: Number(form.price),
      stock_quantity: Number(form.stock_quantity),
      low_stock_threshold: Number(form.low_stock_threshold),
      image_url: form.image_url,
      category_id: form.category_id || null,
      vendor_id: vendor.id,
    };
    try {
      if (editing) {
        await updateProduct(editing.id, payload);
        toast.success('Product updated');
      } else {
        await createProduct(payload);
        toast.success('Product added');
      }
      setShowForm(false);
      await load();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (p) => {
    if (!confirm(`Delete "${p.name}"?`)) return;
    try {
      await deleteProduct(p.id);
      toast.success('Product deleted');
      await load();
    } catch (err) {
      toast.error(err.message);
    }
  };

  if (loading) return <PageLoader label="Loading products…" />;
  if (error) return <ErrorState message={error} />;

  const columns = [
    { key: 'name', label: 'Product', render: (p) => (
      <div className="flex items-center gap-2">
        <div className="h-9 w-9 rounded bg-slate-100" />
        <span className="font-medium text-slate-800">{p.name}</span>
      </div>
    ) },
    { key: 'category', label: 'Category', render: (p) => p.category?.name || '—' },
    { key: 'price', label: 'Price', render: (p) => `$${Number(p.price).toFixed(2)}` },
    { key: 'stock', label: 'Stock', render: (p) => p.stock_quantity },
    { key: 'actions', label: '', render: (p) => (
      <div className="flex gap-2">
        <button onClick={() => openEdit(p)} className="text-slate-400 hover:text-brand-600"><Pencil size={16} /></button>
        <button onClick={() => remove(p)} className="text-slate-400 hover:text-rose-600"><Trash2 size={16} /></button>
      </div>
    ) },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">{products.length} product(s)</p>
        <button onClick={openNew} className="btn-primary"><Plus size={16} /> Add product</button>
      </div>

      {products.length === 0 ? (
        <EmptyState title="No products yet" description="Add your first product to start selling." icon={Package}
          action={<button onClick={openNew} className="btn-primary"><Plus size={16} /> Add product</button>} />
      ) : (
        <DataTable columns={columns} rows={products} />
      )}

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-cardlg">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">{editing ? 'Edit product' : 'Add product'}</h2>
              <button onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            <form onSubmit={save} className="grid gap-3 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <label className="label">Name</label>
                <input className="input" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} required />
              </div>
              <div className="sm:col-span-2">
                <label className="label">Description</label>
                <textarea className="input" rows={3} value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} />
              </div>
              <div>
                <label className="label">Price ($)</label>
                <input type="number" step="0.01" className="input" value={form.price} onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))} required />
              </div>
              <div>
                <label className="label">Stock quantity</label>
                <input type="number" className="input" value={form.stock_quantity} onChange={(e) => setForm((f) => ({ ...f, stock_quantity: e.target.value }))} required />
              </div>
              <div>
                <label className="label">Low stock threshold</label>
                <input type="number" className="input" value={form.low_stock_threshold} onChange={(e) => setForm((f) => ({ ...f, low_stock_threshold: e.target.value }))} />
              </div>
              <div>
                <label className="label">Category</label>
                <select className="input" value={form.category_id} onChange={(e) => setForm((f) => ({ ...f, category_id: e.target.value }))}>
                  <option value="">None</option>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
              <div className="sm:col-span-2">
                <label className="label">Image URL</label>
                <input className="input" value={form.image_url} onChange={(e) => setForm((f) => ({ ...f, image_url: e.target.value }))} placeholder="https://…" />
              </div>
              <div className="sm:col-span-2 flex gap-2 pt-2">
                <button type="submit" disabled={saving} className="btn-primary flex-1">{saving ? <ButtonLoader /> : 'Save'}</button>
                <button type="button" onClick={() => setShowForm(false)} className="btn-secondary">Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
