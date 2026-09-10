import { Link, useNavigate } from 'react-router-dom';
import { Trash2, Minus, Plus, ShoppingBag, ArrowRight } from 'lucide-react';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { PageLoader, InlineLoader } from '../components/Loader';
import { EmptyState } from '../components/States';

export function CartPage() {
  const { cart, loading, updateItem, removeItem, total, itemCount } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  if (!isAuthenticated) {
    return (
      <EmptyState
        title="Sign in to view your cart"
        description="Your shopping cart is tied to your account."
        action={
          <Link to="/login" className="btn-primary">Sign in</Link>
        }
      />
    );
  }

  if (loading && !cart) return <PageLoader label="Loading cart…" />;

  if (!cart || !cart.items || cart.items.length === 0) {
    return (
      <EmptyState
        title="Your cart is empty"
        description="Browse the marketplace and add products to your cart."
        icon={ShoppingBag}
        action={<Link to="/products" className="btn-primary">Start shopping</Link>}
      />
    );
  }

  const shipping = total > 75 ? 0 : 6.99;
  const tax = total * 0.08;
  const grand = total + shipping + tax;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-900">Shopping cart</h1>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-3">
          {cart.items.map((it) => {
            const p = it.product;
            if (!p) return null;
            return (
              <div key={it.id} className="card flex items-center gap-4 p-4">
                <Link to={`/products/${p.id}`} className="h-20 w-20 shrink-0 overflow-hidden rounded-lg bg-slate-100">
                  <img src={p.image_url || ''} alt={p.name} className="h-full w-full object-cover" />
                </Link>
                <div className="min-w-0 flex-1">
                  <Link to={`/products/${p.id}`} className="line-clamp-1 text-sm font-semibold text-slate-800 hover:text-brand-700">
                    {p.name}
                  </Link>
                  <p className="text-xs text-slate-500">{p.vendor?.name}</p>
                  <p className="mt-1 text-lg font-bold text-slate-900">${Number(p.price).toFixed(2)}</p>
                </div>
                <div className="flex items-center rounded-lg ring-1 ring-slate-200">
                  <button
                    onClick={() => updateItem(it.id, Math.max(1, it.quantity - 1))}
                    className="px-2.5 py-2 text-slate-600 hover:bg-slate-100"
                    aria-label="Decrease"
                  >
                    <Minus size={14} />
                  </button>
                  <span className="w-8 text-center text-sm font-semibold">{it.quantity}</span>
                  <button
                    onClick={() => updateItem(it.id, Math.min(p.stock_quantity, it.quantity + 1))}
                    className="px-2.5 py-2 text-slate-600 hover:bg-slate-100"
                    aria-label="Increase"
                  >
                    <Plus size={14} />
                  </button>
                </div>
                <div className="w-24 text-right">
                  <p className="text-sm font-bold text-slate-900">
                    ${(Number(p.price) * it.quantity).toFixed(2)}
                  </p>
                </div>
                <button
                  onClick={() => removeItem(it.id)}
                  className="text-slate-400 hover:text-rose-600"
                  aria-label="Remove"
                >
                  <Trash2 size={18} />
                </button>
              </div>
            );
          })}
        </div>

        {/* Summary */}
        <aside className="card h-fit space-y-4 p-5 lg:sticky lg:top-22">
          <h2 className="text-lg font-semibold text-slate-800">Order summary</h2>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-slate-500">Subtotal ({itemCount} items)</span>
              <span className="font-medium">${total.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Shipping</span>
              <span className="font-medium">{shipping === 0 ? 'Free' : `$${shipping.toFixed(2)}`}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Estimated tax</span>
              <span className="font-medium">${tax.toFixed(2)}</span>
            </div>
            <div className="flex justify-between border-t border-slate-100 pt-2 text-base font-bold">
              <span>Total</span>
              <span>${grand.toFixed(2)}</span>
            </div>
          </div>
          {shipping > 0 && (
            <p className="text-xs text-slate-400">
              Add ${(75 - total).toFixed(2)} more for free shipping.
            </p>
          )}
          <button onClick={() => navigate('/checkout')} className="btn-primary w-full">
            Proceed to checkout <ArrowRight size={16} />
          </button>
          <Link to="/products" className="btn-secondary w-full">Continue shopping</Link>
        </aside>
      </div>
    </div>
  );
}
