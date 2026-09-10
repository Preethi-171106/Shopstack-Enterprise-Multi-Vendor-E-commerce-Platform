import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  Star,
  ShieldCheck,
  Truck,
  RotateCcw,
  Heart,
  ShoppingBag,
  Minus,
  Plus,
  CheckCircle2,
  Share2,
  Package,
  MapPin,
} from 'lucide-react';
import productService from '../services/productService';
import ProductCard from '../components/common/ProductCard';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';
import EmptyState from '../components/common/EmptyState';
import Loading from '../components/common/Loading';
import { useApp } from '../context/AppContext';
import { addToCart, addToCartThunk } from '../store/slices/cartSlice';
import { toggleWishlist, toggleWishlistThunk, selectWishlistItems } from '../store/slices/wishlistSlice';
import { useAuth } from '../context/AuthContext';

const ProductDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { showNotification } = useApp();
  const { isAuthenticated } = useAuth();

  const [product, setProduct] = useState(null);
  const [relatedProducts, setRelatedProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeImage, setActiveImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [activeTab, setActiveTab] = useState('specs');
  const [imgError, setImgError] = useState(false);

  const wishlistItems = useSelector(selectWishlistItems);

  useEffect(() => {
    const fetchProduct = async () => {
      setLoading(true);
      try {
        const prod = await productService.getProductById(id);
        setProduct(prod);

        if (prod && prod.categoryId) {
          const related = await productService.getProducts({
            categoryId: prod.categoryId,
            size: 4,
          });
          setRelatedProducts(
            (related.content || []).filter((p) => String(p.id) !== String(prod.id))
          );
        }
      } catch (err) {
        console.error('Error loading product details:', err);
        setProduct(null);
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [id]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[400px]">
        <Loading label="Loading product details..." />
      </div>
    );
  }

  if (!product) {
    return (
      <div className="flex-1 max-w-7xl mx-auto px-4 py-16 w-full">
        <EmptyState
          title="Product Not Found"
          description="The requested product could not be located in our marketplace database."
          actionLabel="Back to Products"
          onAction={() => navigate('/products')}
        />
      </div>
    );
  }

  const isWishlisted = wishlistItems.some((item) => String(item.id) === String(product.id));
  const mainImage = product.images[activeImage] || product.images[0] || product.imageUrl;
  const discountPercent = product.originalPrice
    ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
    : 0;

  const handleQuantityChange = (delta) => {
    setQuantity((prev) => Math.max(1, Math.min(product.stock || 99, prev + delta)));
  };

  const handleAddToCart = () => {
    if (isAuthenticated) {
      dispatch(addToCartThunk({ productId: product.id, quantity }));
    } else {
      dispatch(addToCart({ product, quantity }));
    }
    showNotification(
      `Added ${quantity} x "${product.name}" to cart`,
      'success',
      'Cart Updated'
    );
  };

  const handleToggleWishlist = () => {
    if (isAuthenticated) {
      dispatch(toggleWishlistThunk(product));
    } else {
      dispatch(toggleWishlist(product));
    }
    showNotification(
      isWishlisted
        ? `Removed "${product.name}" from wishlist`
        : `Added "${product.name}" to wishlist`,
      'info',
      'Wishlist Updated'
    );
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      showNotification('Product link copied to clipboard!', 'info');
    }
  };

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-12">
      
      {/* Breadcrumb Navigation */}
      <nav className="flex items-center gap-2 text-xs text-slate-400">
        <Link to="/" className="hover:text-white transition-colors">
          Home
        </Link>
        <span>/</span>
        <Link to="/products" className="hover:text-white transition-colors">
          Products
        </Link>
        <span>/</span>
        <Link
          to={`/products?category=${product.categoryId}`}
          className="hover:text-white transition-colors truncate max-w-[150px]"
        >
          {product.categoryName}
        </Link>
        <span>/</span>
        <span className="text-slate-200 font-semibold truncate max-w-[200px]">
          {product.name}
        </span>
      </nav>

      {/* Main Grid: Gallery Left + Details Right */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
        
        {/* LEFT: GALLERY AREA */}
        <div className="lg:col-span-6 space-y-4">
          <div className="relative aspect-[4/3] rounded-3xl overflow-hidden bg-slate-950 border border-slate-800 shadow-xl">
            {imgError ? (
              <div className="w-full h-full flex items-center justify-center">
                <span className="text-slate-500 text-sm">Image unavailable</span>
              </div>
            ) : (
              <img
                src={mainImage}
                alt={product.name}
                className="w-full h-full object-cover object-center transition-all duration-300"
                onError={() => setImgError(true)}
              />
            )}

            <div className="absolute top-4 left-4 flex flex-col gap-2 z-10">
              {product.badge && <Badge color="indigo">{product.badge}</Badge>}
              {discountPercent > 0 && <Badge color="rose">{discountPercent}% OFF</Badge>}
            </div>

            <button
              onClick={handleShare}
              className="absolute top-4 right-4 p-2.5 rounded-full bg-slate-900/80 backdrop-blur-md border border-slate-700 text-slate-300 hover:text-white hover:bg-slate-800 transition-all shadow-md"
              title="Share Link"
            >
              <Share2 className="w-4 h-4" />
            </button>
          </div>

          {/* Thumbnails */}
          {product.images.length > 1 && (
            <div className="flex items-center gap-3 overflow-x-auto pb-2">
              {product.images.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setActiveImage(idx)}
                  className={`relative w-20 h-20 rounded-xl overflow-hidden border-2 transition-all shrink-0 ${
                    activeImage === idx
                      ? 'border-indigo-500 ring-2 ring-indigo-500/30'
                      : 'border-slate-800 hover:border-slate-700 opacity-70 hover:opacity-100'
                  }`}
                >
                <img src={img} alt="" className="w-full h-full object-cover"
                  onError={(e) => { e.currentTarget.style.opacity = '0.2'; }} />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT: PRODUCT INFO & ACTION */}
        <div className="lg:col-span-6 space-y-6">
          
          <div className="flex items-center justify-between gap-4">
            <Badge color="slate" size="md">
              {product.categoryName}
            </Badge>
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <span>Sold by:</span>
              <span className="font-semibold text-slate-200 flex items-center gap-1">
                {product.storeName}
                <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
              </span>
            </div>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-white leading-tight">
            {product.name}
          </h1>

          <div className="flex items-center gap-3 text-xs">
            <div className="flex items-center gap-1 text-amber-400">
              <Star className="w-4 h-4 fill-amber-400" />
              <span className="font-bold text-sm text-white">{product.rating}</span>
            </div>
            <span className="text-slate-500">&bull;</span>
            <span className="text-slate-400 font-medium">{product.reviewCount} Verified Reviews</span>
            <span className="text-slate-500">&bull;</span>
            <span className="text-emerald-400 font-medium flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              In Stock ({product.stock} units)
            </span>
          </div>

          <div className="p-4 rounded-2xl bg-slate-950/70 border border-slate-800/80 flex items-baseline gap-3">
            <span className="text-3xl font-black text-white">${product.price.toFixed(2)}</span>
            {product.originalPrice && (
              <span className="text-base text-slate-500 line-through font-medium">
                ${product.originalPrice.toFixed(2)}
              </span>
            )}
            {discountPercent > 0 && (
              <span className="text-xs font-bold text-rose-400 bg-rose-500/10 px-2.5 py-1 rounded-full border border-rose-500/20 ml-auto">
                Save ${(product.originalPrice - product.price).toFixed(2)}
              </span>
            )}
          </div>

          <p className="text-sm text-slate-300 leading-relaxed">{product.description}</p>

          <div className="space-y-4 pt-4 border-t border-slate-800">
            <div className="flex items-center gap-4">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-300">
                Quantity:
              </span>
              <div className="flex items-center bg-slate-900 rounded-xl border border-slate-700/80 p-1">
                <button
                  onClick={() => handleQuantityChange(-1)}
                  disabled={quantity <= 1}
                  className="p-1.5 text-slate-400 hover:text-white disabled:opacity-30"
                >
                  <Minus className="w-4 h-4" />
                </button>
                <span className="w-10 text-center text-sm font-bold text-white">{quantity}</span>
                <button
                  onClick={() => handleQuantityChange(1)}
                  disabled={quantity >= product.stock}
                  className="p-1.5 text-slate-400 hover:text-white disabled:opacity-30"
                >
                  <Plus className="w-4 h-4" />
                </button>
              </div>
              <span className="text-xs text-slate-500 font-mono">Max {product.stock} per order</span>
            </div>

            <div className="flex flex-col sm:flex-row gap-3">
              <Button
                variant="primary"
                size="lg"
                onClick={handleAddToCart}
                disabled={!product.inStock}
                icon={ShoppingBag}
                className="flex-1 shadow-lg shadow-indigo-600/25"
              >
                Add to Cart
              </Button>

              <Button
                variant={isWishlisted ? 'danger' : 'outline'}
                size="lg"
                onClick={handleToggleWishlist}
                icon={Heart}
              >
                {isWishlisted ? 'Wishlisted' : 'Wishlist'}
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-4 text-xs text-slate-400">
            <div className="flex items-center gap-2 p-3 rounded-xl bg-slate-900/60 border border-slate-800">
              <Truck className="w-4 h-4 text-indigo-400 shrink-0" />
              <span>Fast Express Dispatch</span>
            </div>
            <div className="flex items-center gap-2 p-3 rounded-xl bg-slate-900/60 border border-slate-800">
              <RotateCcw className="w-4 h-4 text-indigo-400 shrink-0" />
              <span>30-Day Money Back</span>
            </div>
          </div>

        </div>
      </div>

      {/* BOTTOM TABS AREA */}
      <div className="space-y-6 pt-8 border-t border-slate-800">
        <div className="flex items-center gap-4 border-b border-slate-800 pb-2">
          <button
            onClick={() => setActiveTab('specs')}
            className={`pb-2 text-sm font-bold transition-all relative ${
              activeTab === 'specs'
                ? 'text-indigo-400 border-b-2 border-indigo-500'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Technical Specifications
          </button>
          <button
            onClick={() => setActiveTab('vendor')}
            className={`pb-2 text-sm font-bold transition-all relative ${
              activeTab === 'vendor'
                ? 'text-indigo-400 border-b-2 border-indigo-500'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Vendor Details
          </button>
        </div>

        {activeTab === 'specs' && (
          <div className="max-w-3xl rounded-2xl overflow-hidden border border-slate-800 bg-slate-950/70">
            <table className="w-full text-left text-xs">
              <tbody>
                {product.specifications &&
                  Object.entries(product.specifications).map(([key, val], idx) => (
                    <tr
                      key={key}
                      className={idx % 2 === 0 ? 'bg-slate-900/40' : 'bg-slate-950/40'}
                    >
                      <td className="px-4 py-3 font-semibold text-slate-300 w-1/3 border-r border-slate-800">
                        {key}
                      </td>
                      <td className="px-4 py-3 text-slate-400">{val}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'vendor' && (
          <div className="max-w-2xl p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-indigo-600 flex items-center justify-center font-extrabold text-white text-lg">
                {product.storeName ? product.storeName.charAt(0).toUpperCase() : 'V'}
              </div>
              <div>
                <h3 className="text-base font-bold text-white">{product.storeName}</h3>
                <p className="text-xs text-slate-400">{product.categoryName} Vendor Partner</p>
              </div>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Verified marketplace store partner selling quality products on ShopStack.
            </p>
          </div>
        )}
      </div>

      {/* RELATED PRODUCTS */}
      {relatedProducts.length > 0 && (
        <div className="space-y-6 pt-12 border-t border-slate-800">
          <h3 className="text-xl font-bold text-white tracking-tight">You May Also Like</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {relatedProducts.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        </div>
      )}

    </div>
  );
};

export default ProductDetails;
