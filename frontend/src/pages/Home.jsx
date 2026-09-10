import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  Sparkles,
  Zap,
  TrendingUp,
  Store,
  Users,
} from 'lucide-react';
import categoryService from '../services/categoryService';
import productService from '../services/productService';
import CategoryCard from '../components/common/CategoryCard';
import ProductCard from '../components/common/ProductCard';
import Button from '../components/common/Button';
import Loading from '../components/common/Loading';

const Home = () => {
  const navigate = useNavigate();
  const [selectedTab, setSelectedTab] = useState('all');
  const [categories, setCategories] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadHomeData = async () => {
      setLoading(true);
      try {
        const [catData, prodData] = await Promise.all([
          categoryService.getCategories(),
          productService.getProducts({ size: 20 }),
        ]);
        setCategories(catData || []);
        setProducts(prodData.content || []);
      } catch (err) {
        console.error('Home data load error:', err);
      } finally {
        setLoading(false);
      }
    };

    loadHomeData();
  }, []);

  // Filter featured products based on active tab
  const featuredProducts = products.filter((prod) => {
    if (selectedTab === 'all') return true;
    return prod.categoryId === selectedTab;
  });

  const featuredCategories = categories.slice(0, 4);
  const heroProduct = products[0] || {
    id: 1,
    name: 'Featured Marketplace Item',
    price: 199.99,
    images: ['https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80'],
  };

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[400px]">
        <Loading label="Loading Marketplace..." />
      </div>
    );
  }

  return (
    <div className="flex-1 flex flex-col space-y-16 py-8 sm:py-12 overflow-hidden">
      
      {/* ========================================================================= */}
      {/* 1. HERO SECTION */}
      {/* ========================================================================= */}
      <section className="relative px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        <div className="absolute -top-12 left-1/2 -translate-x-1/2 w-[600px] h-[300px] bg-indigo-600/15 blur-[120px] rounded-full pointer-events-none" />
        <div className="absolute top-1/3 -right-20 w-[350px] h-[250px] bg-violet-600/10 blur-[100px] rounded-full pointer-events-none" />

        <div className="relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          {/* Hero Text & CTAs */}
          <div className="lg:col-span-7 space-y-6 text-center lg:text-left">
            
            <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold shadow-inner">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              <span>Next-Generation Enterprise Multi-Vendor Platform</span>
            </div>

            <h1 className="text-4xl sm:text-6xl font-black tracking-tight text-white leading-[1.1]">
              Discover &amp; Shop <br className="hidden sm:inline" />
              <span className="bg-gradient-to-r from-white via-slate-200 to-indigo-400 bg-clip-text text-transparent">
                Curated Products
              </span>{' '}
              From Verified Sellers.
            </h1>

            <p className="text-base sm:text-lg text-slate-300 max-w-2xl mx-auto lg:mx-0 leading-relaxed">
              ShopStack brings together top independent vendors, trending fashion, high-tech electronics, and handcrafted goods in one seamless marketplace experience.
            </p>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
              <Button
                variant="primary"
                size="lg"
                onClick={() => navigate('/products')}
                icon={ArrowRight}
                iconPosition="right"
                className="w-full sm:w-auto shadow-lg shadow-indigo-600/25"
              >
                Explore Marketplace
              </Button>

              <Button
                variant="outline"
                size="lg"
                onClick={() => {
                  const categoriesEl = document.getElementById('categories-section');
                  if (categoriesEl) categoriesEl.scrollIntoView({ behavior: 'smooth' });
                }}
                className="w-full sm:w-auto"
              >
                Browse Categories
              </Button>
            </div>

            {/* Quick Metrics Bar */}
            <div className="pt-8 grid grid-cols-3 gap-4 border-t border-slate-800/80 max-w-lg mx-auto lg:mx-0">
              <div>
                <p className="text-2xl font-black text-white">{products.length * 10 || 50}+</p>
                <p className="text-xs text-slate-400">Curated Products</p>
              </div>
              <div>
                <p className="text-2xl font-black text-white">{categories.length || 5}</p>
                <p className="text-xs text-slate-400">Active Categories</p>
              </div>
              <div>
                <p className="text-2xl font-black text-emerald-400">99.9%</p>
                <p className="text-xs text-slate-400">Buyer Satisfaction</p>
              </div>
            </div>

          </div>

          {/* Hero Visual Card / Banner */}
          <div className="lg:col-span-5 relative">
            <div className="relative rounded-3xl overflow-hidden border border-slate-800 bg-slate-950/80 p-3 shadow-2xl">
              <div className="relative aspect-[4/3] rounded-2xl overflow-hidden">
                <img
                  src={heroProduct.images[0]}
                  alt={heroProduct.name}
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/30 to-transparent" />
                
                {/* Floating Product Tag */}
                <div className="absolute bottom-4 left-4 right-4 p-4 rounded-xl backdrop-blur-md bg-slate-900/90 border border-slate-700/80 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-semibold text-indigo-400 uppercase tracking-wider block">
                      Featured Marketplace Item
                    </span>
                    <h4 className="text-sm font-bold text-white truncate max-w-[200px]">
                      {heroProduct.name}
                    </h4>
                    <span className="text-xs font-bold text-emerald-400">${heroProduct.price?.toFixed(2)}</span>
                  </div>
                  <Link
                    to={`/products/${heroProduct.id}`}
                    className="p-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white transition-colors"
                  >
                    <ArrowRight className="w-4 h-4" />
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ========================================================================= */}
      {/* 2. PRODUCT CATEGORIES SECTION */}
      {/* ========================================================================= */}
      <section id="categories-section" className="px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-8">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-indigo-400 mb-1">
              <Store className="w-4 h-4" />
              <span>Explore Departments</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Shop by Category
            </h2>
          </div>
          <Link
            to="/products"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-indigo-400 hover:text-indigo-300 transition-colors"
          >
            <span>View All Categories</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredCategories.map((category) => (
            <CategoryCard key={category.id} category={category} />
          ))}
        </div>
      </section>

      {/* ========================================================================= */}
      {/* 3. PROMOTIONAL BANNER SECTION */}
      {/* ========================================================================= */}
      <section className="px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
        <div className="relative rounded-3xl overflow-hidden border border-indigo-500/30 bg-gradient-to-r from-indigo-950 via-purple-950 to-slate-950 p-8 sm:p-12 shadow-2xl">
          <div className="absolute top-0 right-0 -mt-8 -mr-8 w-64 h-64 bg-indigo-500/20 blur-3xl rounded-full pointer-events-none" />
          
          <div className="relative z-10 max-w-2xl space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/20 border border-amber-500/30 text-amber-300 text-xs font-bold uppercase tracking-wider">
              <Zap className="w-3.5 h-3.5 fill-amber-300" />
              <span>SPECIAL PROMOTION</span>
            </div>

            <h3 className="text-3xl sm:text-4xl font-extrabold text-white leading-tight">
              Exclusive Welcome Offer
            </h3>

            <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
              Get an extra 10% discount on your first order. Use promo code{' '}
              <span className="font-mono px-2 py-0.5 rounded bg-slate-900 border border-indigo-500/50 text-indigo-300 font-bold">
                SHOP10
              </span>{' '}
              at checkout.
            </p>

            <div className="pt-2">
              <Button
                variant="primary"
                size="md"
                onClick={() => navigate('/products')}
                icon={ArrowRight}
                iconPosition="right"
              >
                Shop Now
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* ========================================================================= */}
      {/* 4. FEATURED PRODUCTS SECTION */}
      {/* ========================================================================= */}
      <section className="px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full space-y-8">
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-indigo-400 mb-1">
              <TrendingUp className="w-4 h-4" />
              <span>Curated Selection</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Featured Products
            </h2>
          </div>

          {/* Quick Filter Tabs */}
          <div className="flex items-center gap-2 overflow-x-auto pb-2 md:pb-0 scrollbar-none">
            <button
              onClick={() => setSelectedTab('all')}
              className={`px-3.5 py-1.5 text-xs font-semibold rounded-xl transition-all shrink-0 ${
                selectedTab === 'all'
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                  : 'bg-slate-800/80 text-slate-400 hover:text-white'
              }`}
            >
              All Products
            </button>
            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => setSelectedTab(cat.id)}
                className={`px-3.5 py-1.5 text-xs font-semibold rounded-xl transition-all shrink-0 ${
                  selectedTab === cat.id
                    ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                    : 'bg-slate-800/80 text-slate-400 hover:text-white'
                }`}
              >
                {cat.name}
              </button>
            ))}
          </div>
        </div>

        {/* Featured Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredProducts.slice(0, 8).map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>

        <div className="text-center pt-4">
          <Button
            variant="outline"
            size="md"
            onClick={() => navigate('/products')}
            icon={ArrowRight}
            iconPosition="right"
          >
            Browse Full Product Catalog
          </Button>
        </div>
      </section>

    </div>
  );
};

export default Home;
