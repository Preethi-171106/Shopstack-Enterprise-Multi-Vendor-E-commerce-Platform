import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  ShoppingCart,
  Trash2,
  Plus,
  Minus,
  ArrowLeft,
  ArrowRight,
  ShoppingBag,
} from 'lucide-react';
import {
  selectCartItems,
  selectCartTotalCount,
  selectCartSubtotal,
  selectCartShippingFee,
  selectCartGrandTotal,
  fetchCartThunk,
  removeFromCartThunk,
  updateQuantityThunk,
  removeFromCart,
  increaseQuantity,
  decreaseQuantity,
} from '../store/slices/cartSlice';
import EmptyState from '../components/common/EmptyState';
import Button from '../components/common/Button';
import { useApp } from '../context/AppContext';
import { useAuth } from '../context/AuthContext';

const Cart = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { showNotification } = useApp();
  const { isAuthenticated } = useAuth();

  const cartItems = useSelector(selectCartItems);
  const totalCount = useSelector(selectCartTotalCount);
  const subtotal = useSelector(selectCartSubtotal);
  const shippingFee = useSelector(selectCartShippingFee);
  const grandTotal = useSelector(selectCartGrandTotal);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchCartThunk());
    }
  }, [dispatch, isAuthenticated]);

  const handleRemove = (product, itemId) => {
    if (isAuthenticated && itemId) {
      dispatch(removeFromCartThunk(itemId));
    } else {
      dispatch(removeFromCart(product.id));
    }
    showNotification(`"${product.name}" removed from cart`, 'info');
  };

  const handleIncrease = (item) => {
    if (isAuthenticated && item.id) {
      dispatch(updateQuantityThunk({ itemId: item.id, quantity: item.quantity + 1 }));
    } else {
      dispatch(increaseQuantity(item.product.id));
    }
  };

  const handleDecrease = (item) => {
    if (item.quantity <= 1) return;
    if (isAuthenticated && item.id) {
      dispatch(updateQuantityThunk({ itemId: item.id, quantity: item.quantity - 1 }));
    } else {
      dispatch(decreaseQuantity(item.product.id));
    }
  };

  if (cartItems.length === 0) {
    return (
      <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 w-full">
        <div className="mb-8">
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Shopping Cart</h1>
        </div>
        <EmptyState
          icon={ShoppingCart}
          title="Your cart is empty"
          description="Looks like you haven't added any products yet. Start browsing our marketplace to discover amazing products from verified vendors."
          actionLabel="Browse Products"
          onAction={() => navigate('/products')}
        />
      </div>
    );
  }

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
      {/* Page Title */}
      <div className="flex items-center justify-between mb-8 border-b border-slate-800 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Shopping Cart</h1>
          <p className="text-xs text-slate-400 mt-1">
            {totalCount} {totalCount === 1 ? 'item' : 'items'} in your cart
          </p>
        </div>
        <Link
          to="/products"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-400 hover:text-indigo-300 transition-colors"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Continue Shopping</span>
        </Link>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
        {/* CART ITEMS LIST */}
        <div className="lg:col-span-2 space-y-4">
          {cartItems.map((item) => {
            const product = item.product;
            const quantity = item.quantity;
            const itemSubtotal = (product.price || 0) * quantity;
            return (
              <div
                key={item.id || product.id}
                className="flex gap-4 p-4 rounded-2xl bg-slate-950/70 border border-slate-800/80 hover:border-slate-700 transition-colors"
              >
                <Link
                  to={`/products/${product.id}`}
                  className="shrink-0 w-20 h-20 sm:w-24 sm:h-24 rounded-xl overflow-hidden bg-slate-900 border border-slate-800 flex items-center justify-center"
                >
                  <img
                    src={product.images[0] || product.imageUrl}
                    alt={product.name}
                    className="w-full h-full object-cover hover:scale-105 transition-transform"
                    onError={(e) => {
                      e.currentTarget.style.display = 'none';
                      e.currentTarget.parentElement.innerHTML += '<span style="font-size:10px;color:#64748b;text-align:center;padding:4px;">No Image</span>';
                    }}
                  />
                </Link>

                {/* Product Info */}
                <div className="flex-1 min-w-0 space-y-1.5">
                  <div className="flex items-start justify-between gap-2">
                    <Link
                      to={`/products/${product.id}`}
                      className="text-sm font-semibold text-white hover:text-indigo-300 transition-colors line-clamp-2 leading-snug"
                    >
                      {product.name}
                    </Link>
                    <button
                      onClick={() => handleRemove(product, item.id)}
                      className="shrink-0 p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                      title="Remove item"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>

                  {product.storeName && (
                    <p className="text-xs text-slate-400">
                      Sold by{' '}
                      <span className="text-slate-300 font-medium">{product.storeName}</span>
                    </p>
                  )}

                  <p className="text-xs font-medium text-slate-400">
                    Unit Price:{' '}
                    <span className="text-white font-bold">${product.price?.toFixed(2)}</span>
                  </p>

                  <div className="flex items-center justify-between pt-1 flex-wrap gap-2">
                    <div className="flex items-center bg-slate-900 rounded-xl border border-slate-700/80 p-1">
                      <button
                        onClick={() => handleDecrease(item)}
                        disabled={quantity <= 1}
                        className="p-1 text-slate-400 hover:text-white disabled:opacity-30 transition-colors"
                      >
                        <Minus className="w-3.5 h-3.5" />
                      </button>
                      <span className="w-8 text-center text-sm font-bold text-white">
                        {quantity}
                      </span>
                      <button
                        onClick={() => handleIncrease(item)}
                        disabled={quantity >= (product.stock || 99)}
                        className="p-1 text-slate-400 hover:text-white disabled:opacity-30 transition-colors"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <span className="text-sm font-bold text-indigo-300">
                      ${itemSubtotal.toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* ORDER SUMMARY SIDEBAR */}
        <div className="lg:col-span-1 space-y-4 sticky top-24">
          <div className="p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 space-y-4">
            <h3 className="text-base font-bold text-white border-b border-slate-800 pb-3">
              Order Summary
            </h3>

            <div className="space-y-2.5 text-sm">
              <div className="flex justify-between text-slate-300">
                <span>Items ({totalCount})</span>
                <span className="font-semibold text-white">${subtotal.toFixed(2)}</span>
              </div>

              <div className="flex justify-between text-slate-300">
                <span>Estimated Shipping</span>
                <span className="font-semibold">
                  {shippingFee === 0 ? (
                    <span className="text-emerald-400 font-bold">FREE</span>
                  ) : (
                    <span className="text-white">${shippingFee.toFixed(2)}</span>
                  )}
                </span>
              </div>

              {shippingFee > 0 && (
                <p className="text-[11px] text-slate-500 italic">
                  Free shipping on orders over $200
                </p>
              )}
            </div>

            <div className="border-t border-slate-800 pt-3 flex justify-between items-center">
              <span className="font-bold text-white text-sm">Grand Total</span>
              <span className="text-xl font-black text-indigo-300">${grandTotal.toFixed(2)}</span>
            </div>

            <Button
              variant="primary"
              size="lg"
              className="w-full shadow-lg shadow-indigo-600/25"
              onClick={() => navigate('/checkout')}
              icon={ArrowRight}
              iconPosition="right"
            >
              Proceed to Checkout
            </Button>

            <Button
              variant="ghost"
              size="md"
              className="w-full"
              onClick={() => navigate('/products')}
              icon={ShoppingBag}
            >
              Continue Shopping
            </Button>
          </div>

          <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-400">
            <div className="p-2.5 rounded-xl bg-slate-950/40 border border-slate-800/60 text-center">
              🔒 Secure Checkout
            </div>
            <div className="p-2.5 rounded-xl bg-slate-950/40 border border-slate-800/60 text-center">
              🔄 30-Day Returns
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Cart;
