import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Tag } from 'lucide-react';
import { listCategories } from '../lib/api';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

export function CategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      try {
        const data = await listCategories();
        setCategories(data);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <PageLoader label="Loading categories…" />;
  if (error) return <ErrorState message={error} />;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="rounded-lg bg-brand-50 p-2.5 text-brand-600">
          <Tag size={22} />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Categories</h1>
          <p className="text-sm text-slate-500">Browse products by category</p>
        </div>
      </div>

      {categories.length === 0 ? (
        <EmptyState title="No categories yet" description="Check back soon as vendors list products." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {categories.map((c) => (
            <Link
              key={c.id}
              to={`/products?category=${c.id}`}
              className="card flex flex-col gap-3 p-5 transition hover:shadow-cardlg"
            >
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-slate-800">{c.name}</h3>
                <span className="badge-info">{c.slug}</span>
              </div>
              {c.description && <p className="text-sm text-slate-500">{c.description}</p>}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
