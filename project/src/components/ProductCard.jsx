import { Link } from 'react-router-dom';
import { ShoppingCart, Heart, Store } from 'lucide-react';
import { Rating } from './Rating';
import { useCart } from '../context/CartContext';
import { useWishlist } from '../context/WishlistContext';
import { useToast } from '../context/ToastContext';
import { useState } from 'react';
import { ButtonLoader } from './Loader';

const fallbackImg =
  'data:image/svg+xml,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="400" height="300" fill="#e2e8f0"/><text x="50%" y="50%" font-family="sans-serif" font-size="18" fill="#64748b" text-anchor="middle" dy=".3em">No image</text></svg>'
  );

export function ProductCard({ product }) {
  const { addItem } = useCart();
  const { has, toggle } = useWishlist();
  const toast = useToast();
  const [adding, setAdding] = useState(false);

  const onAdd = async (e) => {
    e.preventDefault();
    setAdding(true);
    try {
      await addItem(product.id, 1);
      toast.success('Added to cart');
    } catch (err) {
      toast.error(err.message || 'Could not add to cart');
    } finally {
      setAdding(false);
    }
  };

  const onWish = async (e) => {
    e.preventDefault();
    try {
      await toggle(product.id);
    } catch (err) {
      toast.error(err.message || 'Wishlist update failed');
    }
  };

  const outOfStock = product.stock_quantity <= 0;
  const low = !outOfStock && product.stock_quantity <= (product.low_stock_threshold || 5);

  return (
    <Link
      to={`/products/${product.id}`}
      className="group card flex flex-col overflow-hidden transition hover:shadow-cardlg"
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-slate-100">
        <img
          src={product.image_url || fallbackImg}
          alt={product.name}
          loading="lazy"
          className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
        />
        <button
          onClick={onWish}
          className="absolute right-2 top-2 rounded-full bg-white/90 p-2 text-slate-600 shadow-sm transition hover:text-rose-600"
          aria-label="Toggle wishlist"
        >
          <Heart size={16} className={has(product.id) ? 'fill-rose-500 text-rose-500' : ''} />
        </button>
        {outOfStock && <span className="badge-danger absolute left-2 top-2">Out of stock</span>}
        {!outOfStock && low && <span className="badge-warning absolute left-2 top-2">Low stock</span>}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-center gap-1 text-xs text-slate-400">
          <Store size={12} />
          <span className="truncate">{product.vendor?.name || 'ShopStack'}</span>
        </div>
        <h3 className="line-clamp-2 text-sm font-semibold text-slate-800 group-hover:text-brand-700">
          {product.name}
        </h3>
        <Rating value={product.rating || 0} size={14} />
        <div className="mt-auto flex items-center justify-between pt-2">
          <span className="text-lg font-bold text-slate-900">
            ${Number(product.price).toFixed(2)}
          </span>
          <button
            onClick={onAdd}
            disabled={outOfStock || adding}
            className="btn-primary !px-3 !py-2"
            aria-label="Add to cart"
          >
            {adding ? <ButtonLoader /> : <ShoppingCart size={16} />}
          </button>
        </div>
      </div>
    </Link>
  );
}
