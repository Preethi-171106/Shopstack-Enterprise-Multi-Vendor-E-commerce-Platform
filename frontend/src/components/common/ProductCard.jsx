import React from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Star, Heart, ShoppingBag, CheckCircle2, Eye } from 'lucide-react';
import Badge from './Badge';
import Button from './Button';
import { useApp } from '../../context/AppContext';
import { useAuth } from '../../context/AuthContext';
import { addToCart, addToCartThunk } from '../../store/slices/cartSlice';
import { toggleWishlist, toggleWishlistThunk, selectWishlistItems } from '../../store/slices/wishlistSlice';

const ProductCard = ({ product }) => {
  const dispatch = useDispatch();
  const { showNotification } = useApp();
  const { isAuthenticated } = useAuth();
  const wishlistItems = useSelector(selectWishlistItems);

  const isWishlisted = wishlistItems.some((item) => String(item.id) === String(product.id));

  const handleAddToCart = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (isAuthenticated) {
      dispatch(addToCartThunk({ productId: product.id, quantity: 1 }));
    } else {
      dispatch(addToCart({ product, quantity: 1 }));
    }
    showNotification(`"${product.name}" added to cart`, 'success', 'Cart Updated');
  };

  const handleToggleWishlist = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (isAuthenticated) {
      dispatch(toggleWishlistThunk(product));
    } else {
      dispatch(toggleWishlist(product));
    }
    showNotification(
      isWishlisted ? `Removed "${product.name}" from wishlist` : `Added "${product.name}" to wishlist`,
      'info',
      'Wishlist Updated'
    );
  };

  const discountPercent = product.originalPrice
    ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
    : 0;

  return (
    <div className="group relative bg-slate-950/80 border border-slate-800/80 hover:border-indigo-500/40 rounded-2xl overflow-hidden transition-all duration-300 hover:shadow-xl hover:shadow-indigo-500/10 flex flex-col h-full">
      {/* Product Image Box */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-slate-900">
        <img
          src={product.images[0]}
          alt={product.name}
          className="w-full h-full object-cover object-center group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
          onError={(e) => {
            e.currentTarget.style.display = 'none';
            e.currentTarget.parentElement.classList.add('flex', 'items-center', 'justify-center');
            const fallback = document.createElement('span');
            fallback.textContent = 'Image unavailable';
            fallback.className = 'text-xs text-slate-500 text-center px-2';
            e.currentTarget.parentElement.appendChild(fallback);
          }}
        />

        {/* Floating Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5 z-10">
          {product.badge && (
            <Badge color="indigo" size="sm">
              {product.badge}
            </Badge>
          )}
          {discountPercent > 0 && (
            <Badge color="rose" size="sm">
              {discountPercent}% OFF
            </Badge>
          )}
        </div>

        {/* Wishlist Floating Button */}
        <button
          type="button"
          onClick={handleToggleWishlist}
          className={`absolute top-3 right-3 z-10 p-2 rounded-full backdrop-blur-md border transition-all duration-200 ${
            isWishlisted
              ? 'bg-rose-500/20 border-rose-500/40 text-rose-400'
              : 'bg-slate-900/60 border-slate-700/60 text-slate-300 hover:text-white hover:bg-slate-800'
          }`}
          title={isWishlisted ? 'Remove from Wishlist' : 'Add to Wishlist'}
        >
          <Heart className={`w-4 h-4 ${isWishlisted ? 'fill-rose-500 text-rose-500' : ''}`} />
        </button>

        {/* Quick View Overlay */}
        <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center pointer-events-none group-hover:pointer-events-auto">
          <Link
            to={`/products/${product.id}`}
            className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl bg-slate-900/90 text-white text-xs font-semibold border border-slate-700 hover:border-indigo-500 shadow-lg transition-transform transform translate-y-2 group-hover:translate-y-0"
          >
            <Eye className="w-3.5 h-3.5" />
            <span>View Details</span>
          </Link>
        </div>
      </div>

      {/* Content Container */}
      <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
        <div className="space-y-2">
          {/* Vendor Snippet */}
          {product.vendor && (
            <div className="flex items-center justify-between text-xs text-slate-400">
              <span className="flex items-center gap-1 truncate font-medium hover:text-slate-300">
                {product.vendor.name}
                {product.vendor.verified && (
                  <CheckCircle2 className="w-3 h-3 text-indigo-400 shrink-0" />
                )}
              </span>
              <span className="text-[11px] text-slate-500 shrink-0">{product.categoryName}</span>
            </div>
          )}

          {/* Title */}
          <h3 className="font-semibold text-sm text-white line-clamp-2 leading-snug group-hover:text-indigo-300 transition-colors">
            <Link to={`/products/${product.id}`}>{product.name}</Link>
          </h3>

          {/* Rating */}
          <div className="flex items-center gap-1.5 text-xs">
            <div className="flex items-center text-amber-400">
              <Star className="w-3.5 h-3.5 fill-amber-400" />
              <span className="ml-1 font-semibold text-slate-200">{product.rating}</span>
            </div>
            <span className="text-slate-500">({product.reviewCount})</span>
          </div>
        </div>

        {/* Pricing & Stock & Action Button */}
        <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between gap-2">
          <div>
            <div className="flex items-baseline gap-1.5">
              <span className="text-base font-bold text-white">${product.price.toFixed(2)}</span>
              {product.originalPrice && (
                <span className="text-xs text-slate-500 line-through">
                  ${product.originalPrice.toFixed(2)}
                </span>
              )}
            </div>
            <span
              className={`text-[10px] font-medium block ${
                product.inStock ? 'text-emerald-400' : 'text-rose-400'
              }`}
            >
              {product.inStock ? 'In Stock' : 'Out of Stock'}
            </span>
          </div>

          <Button
            variant="primary"
            size="sm"
            disabled={!product.inStock}
            onClick={handleAddToCart}
            icon={ShoppingBag}
          >
            Add
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
