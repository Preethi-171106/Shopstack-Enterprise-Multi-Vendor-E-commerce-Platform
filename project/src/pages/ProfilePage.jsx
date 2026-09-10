import { useState } from 'react';
import { User, Mail, Phone, Save, MapPin, Package } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { upsertProfile, listAddresses } from '../lib/api';
import { useFetch } from '../hooks/useFetch';
import { ButtonLoader } from '../components/Loader';
import { ErrorState } from '../components/States';

export function ProfilePage() {
  const { user, profile, refreshProfile } = useAuth();
  const toast = useToast();
  const [form, setForm] = useState({
    full_name: profile?.full_name || '',
    phone: profile?.phone || '',
    avatar_url: profile?.avatar_url || '',
  });
  const [saving, setSaving] = useState(false);

  const { data: addresses, loading, error } = useFetch(
    () => listAddresses(user.id),
    [user?.id]
  );

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await upsertProfile({ id: user.id, email: profile.email, role: profile.role, ...form });
      await refreshProfile();
      toast.success('Profile updated');
    } catch (e) {
      toast.error(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">My profile</h1>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <form onSubmit={save} className="card space-y-4 p-6">
          <div className="flex items-center gap-4">
            <div className="grid h-16 w-16 place-items-center rounded-full bg-brand-100 text-2xl font-bold text-brand-700">
              {(form.full_name || profile?.email || 'U').charAt(0).toUpperCase()}
            </div>
            <div>
              <p className="text-lg font-semibold text-slate-800">{form.full_name || 'Your name'}</p>
              <p className="text-sm text-slate-500">{profile?.email}</p>
              <span className="badge-info mt-1">{profile?.role}</span>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="label">Full name</label>
              <div className="relative">
                <User className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <input className="input pl-9" value={form.full_name} onChange={(e) => setForm((f) => ({ ...f, full_name: e.target.value }))} />
              </div>
            </div>
            <div>
              <label className="label">Phone</label>
              <div className="relative">
                <Phone className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <input className="input pl-9" value={form.phone} onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))} />
              </div>
            </div>
            <div className="sm:col-span-2">
              <label className="label">Avatar URL</label>
              <input className="input" value={form.avatar_url} onChange={(e) => setForm((f) => ({ ...f, avatar_url: e.target.value }))} placeholder="https://…" />
            </div>
            <div className="sm:col-span-2">
              <label className="label">Email (read-only)</label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
                <input className="input bg-slate-50 pl-9" value={profile?.email || ''} disabled />
              </div>
            </div>
          </div>

          <button type="submit" disabled={saving} className="btn-primary">
            {saving ? <ButtonLoader /> : <><Save size={16} /> Save changes</>}
          </button>
        </form>

        <aside className="space-y-4">
          <section className="card p-5">
            <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold text-slate-800">
              <MapPin size={18} className="text-brand-600" /> Saved addresses
            </h2>
            {loading ? (
              <p className="text-sm text-slate-400">Loading…</p>
            ) : error ? (
              <ErrorState message={error} />
            ) : !addresses?.length ? (
              <p className="text-sm text-slate-500">No saved addresses.</p>
            ) : (
              <div className="space-y-2 text-sm">
                {addresses.map((a) => (
                  <div key={a.id} className="rounded-lg bg-slate-50 p-3">
                    <p className="font-medium text-slate-800">{a.full_name}</p>
                    <p className="text-slate-600">{a.line1}, {a.city} {a.postal_code}</p>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="card p-5">
            <h2 className="mb-2 flex items-center gap-2 text-lg font-semibold text-slate-800">
              <Package size={18} className="text-brand-600" /> Quick links
            </h2>
            <div className="flex flex-col gap-1 text-sm">
              <a href="/orders" className="nav-link">My orders</a>
              <a href="/payments" className="nav-link">My payments</a>
              <a href="/notifications" className="nav-link">Notifications</a>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
