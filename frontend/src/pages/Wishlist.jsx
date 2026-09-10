import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Heart, ShoppingBag, Trash2, CheckCircle2, Star } from 'lucide-react';
import {
  selectWishlistItems,
  fetchWishlistThunk,
  removeFromWishlistThunk,
  removeFromWishlist,
} from '../store/slices/wishlistSlice';
import { addToCart, addToCartThunk } from '../store/slices/cartSlice';
import EmptyState from '../components/common/EmptyState';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import { useApp } from '../context/AppContext';
import { useAuth } from '../context/AuthContext';

const Wishlist = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { showNotification } = useApp();
  const { isAuthenticated } = useAuth();

  const wishlistItems = useSelector(selectWishlistItems);

  useEffect(() => {
    if (isAuthenticated) {
      dispatch(fetchWishlistThunk());
    }
  }, [dispatch, isAuthenticated]);

  const handleAddToCart = (product) => {
    if (!product.inStock) {
      showNotification(`"${product.name}" is currently out of stock.`, 'warning');
      return;
    }
    if (isAuthenticated) {
      dispatch(addToCartThunk({ productId: product.id, quantity: 1 }));
    } else {
      dispatch(addToCart(product));
    }
    showNotification(`"${product.name}" added to cart`, 'success', 'Cart Updated');
  };

  const handleRemove = (product) => {
    if (isAuthenticated) {
      dispatch(removeFromWishlistThunk(product.id));
    } else {
      dispatch(removeFromWishlist(product.id));
    }
    showNotification(`"${product.name}" removed from wishlist`, 'info');
  };

  if (wishlistItems.length === 0) {
    return (
      <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 w-full">
        <div className="mb-8">
          <h1 className="text-3xl font-extrabold text-white tracking-tight">My Wishlist</h1>
        </div>
        <EmptyState
          icon={Heart}
          title="Your wishlist is empty"
          description="Save items you love to your wishlist. Review them anytime and easily move them to your cart when you're ready to buy."
          actionLabel="Discover Products"
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
          <h1 className="text-3xl font-extrabold text-white tracking-tight">My Wishlist</h1>
          <p className="text-xs text-slate-400 mt-1">
            {wishlistItems.length} saved {wishlistItems.length === 1 ? 'item' : 'items'}
          </p>
        </div>
      </div>

      {/* Wishlist Product Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {wishlistItems.map((product) => {
          const discountPercent = product.originalPrice
            ? Math.round(
                ((product.originalPrice - product.price) / product.originalPrice) * 100
              )
            : 0;

          return (
            <div
              key={product.id}
              className="group relative bg-slate-950/80 border border-slate-800/80 hover:border-indigo-500/40 rounded-2xl overflow-hidden transition-all duration-300 hover:shadow-xl hover:shadow-indigo-500/10 flex flex-col"
            >
              {/* Product Image */}
              <div className="relative aspect-[4/3] overflow-hidden bg-slate-900">
                <img
                  src={product.images?.[0] || product.imageUrl}
                  alt={product.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />

                {/* Badges */}
                <div className="absolute top-3 left-3 flex flex-col gap-1.5">
                  {product.badge && <Badge color="indigo" size="sm">{product.badge}</Badge>}
                  {discountPercent > 0 && (
                    <Badge color="rose" size="sm">{discountPercent}% OFF</Badge>
                  )}
                </div>

                <button
                  onClick={() => handleRemove(product)}
                  className="absolute top-3 right-3 p-2 rounded-full bg-rose-500/20 border border-rose-500/40 text-rose-400 hover:bg-rose-500/30 transition-all backdrop-blur-md"
                  title="Remove from Wishlist"
                >
                  <Heart className="w-4 h-4 fill-rose-500" />
                </button>
              </div>

              {/* Content */}
              <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
                <div className="space-y-2">
                  {product.storeName && (
                    <div className="flex items-center gap-1 text-xs text-slate-400 truncate">
                      <span className="font-medium">{product.storeName}</span>
                      <CheckCircle2 className="w-3 h-3 text-indigo-400 shrink-0" />
                    </div>
                  )}

                  <h3 className="font-semibold text-sm text-white line-clamp-2 leading-snug">
                    {product.name}
                  </h3>

                  <div className="flex items-center gap-1.5 text-xs">
                    <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                    <span className="font-semibold text-slate-200">{product.rating}</span>
                    <span className="text-slate-500">({product.reviewCount})</span>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800/80 space-y-3">
                  <div className="flex items-baseline gap-2">
                    <span className="text-base font-bold text-white">
                      ${product.price?.toFixed(2)}
                    </span>
                    {product.originalPrice && (
                      <span className="text-xs text-slate-500 line-through">
                        ${product.originalPrice.toFixed(2)}
                      </span>
                    )}
                  </div>

                  <div className="flex gap-2">
                    <Button
                      variant="primary"
                      size="sm"
                      disabled={!product.inStock}
                      onClick={() => handleAddToCart(product)}
                      icon={ShoppingBag}
                      className="flex-1"
                    >
                      {product.inStock ? 'Add to Cart' : 'Out of Stock'}
                    </Button>
                    <button
                      onClick={() => handleRemove(product)}
                      className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 border border-slate-700 transition-colors"
                      title="Remove"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default Wishlist;
