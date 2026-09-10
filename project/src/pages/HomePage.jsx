import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Truck, ShieldCheck, Tag, Headphones } from 'lucide-react';
import { listCategories, listProducts } from '../lib/api';
import { ProductCard } from '../components/ProductCard';
import { PageLoader } from '../components/Loader';
import { ErrorState } from '../components/States';

const heroImg =
  'https://images.pexels.com/photos/35560482/pexels-photo-35560482.jpeg?auto=compress&cs=tinysrgb&h=650&w=940';

const categoryImages = {
  electronics:
    'https://images.pexels.com/photos/7989742/pexels-photo-7989742.jpeg?auto=compress&cs=tinysrgb&h=400&w=400',
  fashion:
    'https://images.pexels.com/photos/36730470/pexels-photo-36730470.jpeg?auto=compress&cs=tinysrgb&h=400&w=400',
  home: 'https://images.pexels.com/photos/4765366/pexels-photo-4765366.jpeg?auto=compress&cs=tinysrgb&h=400&w=400',
  default:
    'https://images.pexels.com/photos/8346914/pexels-photo-8346914.jpeg?auto=compress&cs=tinysrgb&h=400&w=400',
};

function imageForCategory(slug) {
  if (!slug) return categoryImages.default;
  if (slug.includes('electro') || slug.includes('tech')) return categoryImages.electronics;
  if (slug.includes('fashion') || slug.includes('cloth')) return categoryImages.fashion;
  if (slug.includes('home') || slug.includes('living')) return categoryImages.home;
  return categoryImages.default;
}

export function HomePage() {
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const [cats, { products: prods }] = await Promise.all([
          listCategories(),
          listProducts({ sort: 'newest' }),
        ]);
        setCategories(cats.slice(0, 6));
        setProducts(prods.slice(0, 8));
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <PageLoader label="Loading the marketplace…" />;
  if (error) return <ErrorState message={error} />;

  return (
    <div className="space-y-12">
      {/* Hero */}
      <section className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 text-white shadow-cardlg">
        <img
          src={heroImg}
          alt=""
          className="absolute inset-0 h-full w-full object-cover opacity-20"
        />
        <div className="relative grid items-center gap-6 px-6 py-12 sm:px-10 md:grid-cols-2 md:py-16">
          <div className="space-y-5">
            <span className="badge bg-white/15 text-white ring-1 ring-white/30">
              Enterprise Multi-Vendor Marketplace
            </span>
            <h1 className="text-3xl font-bold leading-tight sm:text-4xl md:text-5xl">
              Everything your store needs, in one place.
            </h1>
            <p className="max-w-md text-brand-100">
              Discover thousands of products from trusted vendors, manage your inventory,
              track orders, and grow your business — all on ShopStack.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link to="/products" className="btn bg-white text-brand-700 hover:bg-brand-50">
                Shop now <ArrowRight size={18} />
              </Link>
              <Link to="/categories" className="btn bg-white/10 text-white ring-1 ring-white/40 hover:bg-white/20">
                Browse categories
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Trust badges */}
      <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { icon: Truck, title: 'Fast delivery', text: 'Tracked shipments nationwide' },
          { icon: ShieldCheck, title: 'Secure payments', text: 'Encrypted checkout' },
          { icon: Tag, title: 'Best prices', text: 'From competing vendors' },
          { icon: Headphones, title: '24/7 support', text: 'We are here to help' },
        ].map((f) => (
          <div key={f.title} className="card flex items-center gap-3 p-4">
            <div className="rounded-lg bg-brand-50 p-2.5 text-brand-600">
              <f.icon size={20} />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-800">{f.title}</p>
              <p className="text-xs text-slate-500">{f.text}</p>
            </div>
          </div>
        ))}
      </section>

      {/* Categories */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-bold text-slate-900">Shop by category</h2>
          <Link to="/categories" className="text-sm font-medium text-brand-600 hover:text-brand-700">
            View all →
          </Link>
        </div>
        {categories.length === 0 ? (
          <p className="text-sm text-slate-500">No categories yet.</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
            {categories.map((c) => (
              <Link
                key={c.id}
                to={`/products?category=${c.id}`}
                className="group card flex flex-col items-center gap-2 p-4 transition hover:shadow-cardlg"
              >
                <div className="h-16 w-16 overflow-hidden rounded-full bg-slate-100">
                  <img
                    src={c.icon_url || imageForCategory(c.slug)}
                    alt={c.name}
                    className="h-full w-full object-cover transition group-hover:scale-105"
                  />
                </div>
                <span className="text-center text-sm font-medium text-slate-700 group-hover:text-brand-700">
                  {c.name}
                </span>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Featured products */}
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-bold text-slate-900">Featured products</h2>
          <Link to="/products" className="text-sm font-medium text-brand-600 hover:text-brand-700">
            View all →
          </Link>
        </div>
        {products.length === 0 ? (
          <p className="text-sm text-slate-500">No products available yet.</p>
        ) : (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {products.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
