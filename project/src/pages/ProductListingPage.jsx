import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { SlidersHorizontal, Search } from 'lucide-react';
import { listCategories, listProducts } from '../lib/api';
import { ProductCard } from '../components/ProductCard';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';

const SORTS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: Low to High' },
  { value: 'price_desc', label: 'Price: High to Low' },
  { value: 'rating', label: 'Top rated' },
];

export function ProductListingPage() {
  const [params, setParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const search = params.get('search') || '';
  const categoryId = params.get('category') || '';
  const sort = params.get('sort') || 'newest';
  const minPrice = params.get('minPrice') || '';
  const maxPrice = params.get('maxPrice') || '';
  const inStock = params.get('inStock') === '1';

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const [cats, { products: prods, total: count }] = await Promise.all([
          listCategories(),
          listProducts({ categoryId: categoryId || undefined, search: search || undefined, sort }),
        ]);
        setCategories(cats);
        let filtered = prods;
        if (minPrice) filtered = filtered.filter((p) => Number(p.price) >= Number(minPrice));
        if (maxPrice) filtered = filtered.filter((p) => Number(p.price) <= Number(maxPrice));
        if (inStock) filtered = filtered.filter((p) => p.stock_quantity > 0);
        setProducts(filtered);
        setTotal(count);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [search, categoryId, sort, minPrice, maxPrice, inStock]);

  const update = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">
            {search ? `Results for “${search}”` : 'All products'}
          </h1>
          <p className="text-sm text-slate-500">{total} product{total !== 1 ? 's' : ''}</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        {/* Filters */}
        <aside className="card h-fit space-y-5 p-5 lg:sticky lg:top-22">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-700">
            <SlidersHorizontal size={16} /> Filters
          </div>

          <div>
            <label className="label">Category</label>
            <select className="input" value={categoryId} onChange={(e) => update('category', e.target.value)}>
              <option value="">All categories</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="label">Sort by</label>
            <select className="input" value={sort} onChange={(e) => update('sort', e.target.value)}>
              {SORTS.map((s) => (
                <option key={s.value} value={s.value}>{s.label}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="label">Price range</label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                placeholder="Min"
                className="input !py-2"
                value={minPrice}
                onChange={(e) => update('minPrice', e.target.value)}
              />
              <span className="text-slate-400">–</span>
              <input
                type="number"
                placeholder="Max"
                className="input !py-2"
                value={maxPrice}
                onChange={(e) => update('maxPrice', e.target.value)}
              />
            </div>
          </div>

          <label className="flex items-center gap-2 text-sm text-slate-700">
            <input
              type="checkbox"
              checked={inStock}
              onChange={(e) => update('inStock', e.target.checked ? '1' : '')}
              className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
            />
            In stock only
          </label>
        </aside>

        {/* Results */}
        <div>
          {loading ? (
            <PageLoader label="Finding products…" />
          ) : error ? (
            <ErrorState message={error} />
          ) : products.length === 0 ? (
            <EmptyState
              title="No products found"
              description="Try adjusting your filters or search terms."
              icon={Search}
            />
          ) : (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
              {products.map((p) => (
                <ProductCard key={p.id} product={p} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
