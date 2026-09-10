import { Link } from 'react-router-dom';
import { Heart } from 'lucide-react';
import { useWishlist } from '../context/WishlistContext';
import { useAuth } from '../context/AuthContext';
import { PageLoader } from '../components/Loader';
import { EmptyState } from '../components/States';
import { ProductCard } from '../components/ProductCard';

export function WishlistPage() {
  const { items, loading } = useWishlist();
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return (
      <EmptyState
        title="Sign in to view your wishlist"
        description="Save products to revisit them later."
        action={<Link to="/login" className="btn-primary">Sign in</Link>}
      />
    );
  }

  if (loading) return <PageLoader label="Loading wishlist…" />;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="rounded-lg bg-rose-50 p-2.5 text-rose-600">
          <Heart size={22} />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Wishlist</h1>
          <p className="text-sm text-slate-500">{items.length} saved item(s)</p>
        </div>
      </div>

      {items.length === 0 ? (
        <EmptyState
          title="Your wishlist is empty"
          description="Tap the heart on any product to save it here."
          icon={Heart}
          action={<Link to="/products" className="btn-primary">Browse products</Link>}
        />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {items.map((i) => (
            <ProductCard key={i.id} product={i.product} />
          ))}
        </div>
      )}
    </div>
  );
}
