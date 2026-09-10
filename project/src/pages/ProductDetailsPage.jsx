import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, ShoppingCart, Heart, Store, Minus, Plus, Truck, ShieldCheck, RotateCcw } from 'lucide-react';
import { getProduct, listReviews, createReview } from '../lib/api';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { PageLoader } from '../components/Loader';
import { ErrorState, EmptyState } from '../components/States';
import { Rating } from '../components/Rating';
import { ButtonLoader } from '../components/Loader';

export function ProductDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addItem } = useCart();
  const { has, toggle } = useWishlist();
  const { isAuthenticated } = useAuth();
  const toast = useToast();

  const [product, setProduct] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [qty, setQty] = useState(1);
  const [adding, setAdding] = useState(false);

  const [reviewForm, setReviewForm] = useState({ rating: 5, comment: '' });
  const [submittingReview, setSubmittingReview] = useState(false);

  useEffect(() => {
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const [p, r] = await Promise.all([getProduct(id), listReviews(id)]);
        setProduct(p);
        setReviews(r);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const onAdd = async () => {
    if (!isAuthenticated) {
      toast.info('Please sign in to add to cart');
      navigate('/login');
      return;
    }
    setAdding(true);
    try {
      await addItem(product.id, qty);
      toast.success('Added to cart');
    } catch (e) {
      toast.error(e.message);
    } finally {
      setAdding(false);
    }
  };

  const onBuyNow = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    await addItem(product.id, qty);
    navigate('/cart');
  };

  const onWish = async () => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    try {
      await toggle(product.id);
    } catch (e) {
      toast.error(e.message);
    }
  };

  const submitReview = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      toast.info('Please sign in to review');
      navigate('/login');
      return;
    }
    setSubmittingReview(true);
    try {
      await createReview({
        product_id: product.id,
        rating: reviewForm.rating,
        comment: reviewForm.comment,
      });
      setReviewForm({ rating: 5, comment: '' });
      const r = await listReviews(id);
      setReviews(r);
      toast.success('Review submitted');
    } catch (e) {
      toast.error(e.message);
    } finally {
      setSubmittingReview(false);
    }
  };

  if (loading) return <PageLoader label="Loading product…" />;
  if (error) return <ErrorState message={error} />;
  if (!product) return <EmptyState title="Product not found" />;

  const outOfStock = product.stock_quantity <= 0;
  const avg = reviews.length
    ? reviews.reduce((s, r) => s + r.rating, 0) / reviews.length
    : product.rating || 0;

  return (
    <div className="space-y-8">
      <Link to="/products" className="inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-brand-600">
        <ArrowLeft size={16} /> Back to products
      </Link>

      <div className="grid gap-8 lg:grid-cols-2">
        <div className="card overflow-hidden">
          <img
            src={product.image_url || 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="600" height="450"%3E%3Crect width="600" height="450" fill="%23e2e8f0"/%3E%3Ctext x="50%25" y="50%25" font-family="sans-serif" font-size="20" fill="%2364748b" text-anchor="middle" dy=".3em"%3ENo image%3C/text%3E%3C/svg%3E'}
            alt={product.name}
            className="aspect-square w-full object-cover"
          />
        </div>

        <div className="space-y-5">
          <div>
            <div className="mb-2 flex items-center gap-2 text-sm text-slate-500">
              <Store size={14} /> {product.vendor?.name || 'ShopStack'}
              {product.category?.name && (
                <>
                  <span>·</span>
                  <Link to={`/products?category=${product.category_id}`} className="hover:text-brand-600">
                    {product.category.name}
                  </Link>
                </>
              )}
            </div>
            <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">{product.name}</h1>
            <div className="mt-2 flex items-center gap-3">
              <Rating value={avg} count={reviews.length} size={18} />
            </div>
          </div>

          <p className="text-3xl font-bold text-brand-700">${Number(product.price).toFixed(2)}</p>

          <p className="text-slate-600">{product.description || 'No description available.'}</p>

          <div className="flex flex-wrap items-center gap-3">
            {outOfStock ? (
              <span className="badge-danger">Out of stock</span>
            ) : product.stock_quantity <= (product.low_stock_threshold || 5) ? (
              <span className="badge-warning">Only {product.stock_quantity} left</span>
            ) : (
              <span className="badge-success">In stock</span>
            )}
          </div>

          {!outOfStock && (
            <div className="flex items-center gap-4">
              <div className="flex items-center rounded-lg ring-1 ring-slate-200">
                <button
                  onClick={() => setQty((q) => Math.max(1, q - 1))}
                  className="px-3 py-2 text-slate-600 hover:bg-slate-100"
                  aria-label="Decrease"
                >
                  <Minus size={16} />
                </button>
                <span className="w-12 text-center font-semibold">{qty}</span>
                <button
                  onClick={() => setQty((q) => Math.min(product.stock_quantity, q + 1))}
                  className="px-3 py-2 text-slate-600 hover:bg-slate-100"
                  aria-label="Increase"
                >
                  <Plus size={16} />
                </button>
              </div>
            </div>
          )}

          <div className="flex flex-wrap gap-3">
            <button onClick={onAdd} disabled={outOfStock || adding} className="btn-primary">
              {adding ? <ButtonLoader /> : <ShoppingCart size={18} />} Add to cart
            </button>
            <button onClick={onBuyNow} disabled={outOfStock} className="btn-secondary">
              Buy now
            </button>
            <button onClick={onWish} className="btn-secondary">
              <Heart size={18} className={has(product.id) ? 'fill-rose-500 text-rose-500' : ''} />
              {has(product.id) ? 'Saved' : 'Save'}
            </button>
          </div>

          <div className="grid grid-cols-3 gap-3 border-t border-slate-100 pt-5 text-center">
            {[
              { icon: Truck, label: 'Free shipping' },
              { icon: ShieldCheck, label: 'Secure checkout' },
              { icon: RotateCcw, label: 'Easy returns' },
            ].map((b) => (
              <div key={b.label} className="flex flex-col items-center gap-1 text-xs text-slate-500">
                <b.icon size={18} className="text-brand-600" />
                {b.label}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Reviews */}
      <section className="space-y-4">
        <h2 className="text-xl font-bold text-slate-900">Reviews ({reviews.length})</h2>

        {isAuthenticated && (
          <form onSubmit={submitReview} className="card space-y-3 p-5">
            <h3 className="text-sm font-semibold text-slate-700">Write a review</h3>
            <div>
              <label className="label">Your rating</label>
              <Rating
                value={reviewForm.rating}
                readOnly={false}
                size={24}
                onChange={(v) => setReviewForm((f) => ({ ...f, rating: v }))}
              />
            </div>
            <div>
              <label className="label">Comment</label>
              <textarea
                value={reviewForm.comment}
                onChange={(e) => setReviewForm((f) => ({ ...f, comment: e.target.value }))}
                rows={3}
                className="input"
                placeholder="Share your thoughts…"
              />
            </div>
            <button type="submit" disabled={submittingReview} className="btn-primary">
              {submittingReview ? <ButtonLoader /> : 'Submit review'}
            </button>
          </form>
        )}

        {reviews.length === 0 ? (
          <EmptyState title="No reviews yet" description="Be the first to review this product." />
        ) : (
          <div className="space-y-3">
            {reviews.map((r) => (
              <div key={r.id} className="card p-4">
                <div className="mb-1 flex items-center justify-between">
                  <span className="text-sm font-semibold text-slate-800">
                    {r.user?.full_name || 'Anonymous'}
                  </span>
                  <Rating value={r.rating} size={14} />
                </div>
                {r.comment && <p className="text-sm text-slate-600">{r.comment}</p>}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
