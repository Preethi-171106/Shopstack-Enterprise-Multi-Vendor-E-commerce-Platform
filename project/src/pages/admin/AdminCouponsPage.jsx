import { useEffect, useState } from 'react';
import { Plus, Pencil, Trash2, Ticket, X } from 'lucide-react';
import { useToast } from '../../context/ToastContext';
import { listCoupons, createCoupon, updateCoupon, deleteCoupon } from '../../lib/api';
import { PageLoader, ButtonLoader } from '../../components/Loader';
import { ErrorState, EmptyState } from '../../components/States';
import { DataTable } from '../../components/DataTable';

export function AdminCouponsPage() {
  const toast = useToast();
  const [coupons, setCoupons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ code: '', discount_percentage: '', max_discount: '', is_active: true });

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listCoupons();
      setCoupons(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openNew = () => {
    setEditing(null);
    setForm({ code: '', discount_percentage: '', max_discount: '', is_active: true });
    setShowForm(true);
  };
  const openEdit = (c) => {
    setEditing(c);
    setForm({ code: c.code, discount_percentage: String(c.discount_percentage), max_discount: String(c.max_discount), is_active: c.is_active });
    setShowForm(true);
  };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    const payload = {
      code: form.code.toUpperCase(),
      discount_percentage: Number(form.discount_percentage),
      max_discount: Number(form.max_discount),
      is_active: form.is_active,
    };
    try {
      if (editing) {
        await updateCoupon(editing.id, payload);
        toast.success('Coupon updated');
      } else {
        await createCoupon(payload);
        toast.success('Coupon created');
      }
      setShowForm(false);
      await load();
    } catch (err) {
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (c) => {
    if (!confirm(`Delete coupon "${c.code}"?`)) return;
    try {
      await deleteCoupon(c.id);
      toast.success('Coupon deleted');
      await load();
    } catch (err) {
      toast.error(err.message);
    }
  };

  if (loading) return <PageLoader label="Loading coupons…" />;
  if (error) return <ErrorState message={error} />;

  const columns = [
    { key: 'code', label: 'Code', render: (c) => <span className="font-mono font-semibold text-slate-800">{c.code}</span> },
    { key: 'discount_percentage', label: 'Discount', render: (c) => `${c.discount_percentage}%` },
    { key: 'max_discount', label: 'Max discount', render: (c) => `$${Number(c.max_discount).toFixed(2)}` },
    { key: 'usage_count', label: 'Used', render: (c) => c.usage_count },
    { key: 'is_active', label: 'Status', render: (c) => <span className={c.is_active ? 'badge-success' : 'badge-danger'}>{c.is_active ? 'Active' : 'Inactive'}</span> },
    { key: 'actions', label: '', render: (c) => (
      <div className="flex gap-2">
        <button onClick={() => openEdit(c)} className="text-slate-400 hover:text-brand-600"><Pencil size={16} /></button>
        <button onClick={() => remove(c)} className="text-slate-400 hover:text-rose-600"><Trash2 size={16} /></button>
      </div>
    ) },
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-slate-500">{coupons.length} coupon(s)</p>
        <button onClick={openNew} className="btn-primary"><Plus size={16} /> Add coupon</button>
      </div>

      {coupons.length === 0 ? (
        <EmptyState title="No coupons yet" description="Create discount codes for your customers." icon={Ticket}
          action={<button onClick={openNew} className="btn-primary"><Plus size={16} /> Add coupon</button>} />
      ) : (
        <DataTable columns={columns} rows={coupons} />
      )}

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-cardlg">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">{editing ? 'Edit coupon' : 'Add coupon'}</h2>
              <button onClick={() => setShowForm(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
            </div>
            <form onSubmit={save} className="space-y-3">
              <div>
                <label className="label">Code</label>
                <input className="input uppercase" value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value }))} required />
              </div>
              <div>
                <label className="label">Discount percentage</label>
                <input type="number" step="0.01" className="input" value={form.discount_percentage} onChange={(e) => setForm((f) => ({ ...f, discount_percentage: e.target.value }))} required />
              </div>
              <div>
                <label className="label">Max discount ($)</label>
                <input type="number" step="0.01" className="input" value={form.max_discount} onChange={(e) => setForm((f) => ({ ...f, max_discount: e.target.value }))} required />
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" checked={form.is_active} onChange={(e) => setForm((f) => ({ ...f, is_active: e.target.checked }))} className="h-4 w-4 rounded text-brand-600" />
                Active
              </label>
              <div className="flex gap-2 pt-2">
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
