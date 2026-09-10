import React, { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  SlidersHorizontal,
  Search,
  RotateCcw,
  Star,
  Check,
  Filter,
  X,
} from 'lucide-react';
import productService from '../services/productService';
import categoryService from '../services/categoryService';
import ProductCard from '../components/common/ProductCard';
import EmptyState from '../components/common/EmptyState';
import Loading from '../components/common/Loading';
import Input from '../components/common/Input';
import Button from '../components/common/Button';
import Badge from '../components/common/Badge';

const Products = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // State filters
  const [searchQuery, setSearchQuery] = useState(searchParams.get('search') || '');
  const [selectedCategory, setSelectedCategory] = useState(searchParams.get('category') || 'all');
  const [maxPrice, setMaxPrice] = useState(100000);
  const [minRating, setMinRating] = useState(0);
  const [inStockOnly, setInStockOnly] = useState(false);
  const [sortBy, setSortBy] = useState('featured');
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);

  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // Load Categories on mount
  useEffect(() => {
    const fetchCategories = async () => {
      try {
        const catData = await categoryService.getCategories();
        setCategories(catData || []);
      } catch (err) {
        console.error('Error fetching categories:', err);
      }
    };
    fetchCategories();
  }, []);

  // Sync state with URL params
  useEffect(() => {
    const urlCategory = searchParams.get('category');
    const urlSearch = searchParams.get('search');
    if (urlCategory) setSelectedCategory(urlCategory);
    if (urlSearch !== null) setSearchQuery(urlSearch);
  }, [searchParams]);

  // Fetch Products whenever search or category changes
  useEffect(() => {
    const fetchProductsData = async () => {
      setLoading(true);
      try {
        const catIdFilter = selectedCategory !== 'all' ? selectedCategory : null;
        const result = await productService.getProducts({
          search: searchQuery,
          categoryId: catIdFilter,
          size: 100,
        });
        setProducts(result.content || []);
      } catch (error) {
        console.error('Error fetching products:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProductsData();
  }, [searchQuery, selectedCategory]);

  // Client-side Price, Rating & Sort Logic
  const filteredProducts = useMemo(() => {
    return products
      .filter((product) => {
        // Price Filter
        if (product.price > maxPrice) return false;
        // Rating Filter
        if (minRating > 0 && product.rating < minRating) return false;
        // In Stock Filter
        if (inStockOnly && !product.inStock) return false;
        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'price-low') return a.price - b.price;
        if (sortBy === 'price-high') return b.price - a.price;
        if (sortBy === 'rating') return b.rating - a.rating;
        if (sortBy === 'newest') return Number(b.id) - Number(a.id);
        return (b.isFeatured ? 1 : 0) - (a.isFeatured ? 1 : 0);
      });
  }, [products, maxPrice, minRating, inStockOnly, sortBy]);

  // Active filters counter
  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (searchQuery.trim()) count++;
    if (selectedCategory !== 'all') count++;
    if (maxPrice < 100000) count++;
    if (minRating > 0) count++;
    if (inStockOnly) count++;
    return count;
  }, [searchQuery, selectedCategory, maxPrice, minRating, inStockOnly]);

  const handleResetFilters = () => {
    setSearchQuery('');
    setSelectedCategory('all');
    setMaxPrice(100000);
    setMinRating(0);
    setInStockOnly(false);
    setSortBy('featured');
    setSearchParams({});
  };

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-6">
      
      {/* Header Title & Search Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">Marketplace Products</h1>
          <p className="text-xs text-slate-400 mt-1">
            Discover {filteredProducts.length} premium products from verified global vendors.
          </p>
        </div>

        <div className="w-full md:w-80">
          <Input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onClear={searchQuery ? () => setSearchQuery('') : null}
            placeholder="Search catalog..."
            icon={Search}
          />
        </div>
      </div>

      {/* Main Layout: Sidebar Filters + Products Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 items-start">
        
        {/* DESKTOP SIDEBAR FILTERS */}
        <aside className="hidden lg:block lg:col-span-1 space-y-6 p-5 rounded-2xl bg-slate-950/70 border border-slate-800/80 sticky top-24">
          <div className="flex items-center justify-between border-b border-slate-800 pb-4">
            <div className="flex items-center gap-2 font-bold text-sm text-white">
              <SlidersHorizontal className="w-4 h-4 text-indigo-400" />
              <span>Filters</span>
              {activeFiltersCount > 0 && (
                <Badge color="indigo" size="sm">
                  {activeFiltersCount}
                </Badge>
              )}
            </div>
            {activeFiltersCount > 0 && (
              <button
                onClick={handleResetFilters}
                className="text-xs text-slate-400 hover:text-rose-400 flex items-center gap-1 transition-colors"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset</span>
              </button>
            )}
          </div>

          {/* Category Filter */}
          <div className="space-y-2.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Department / Category
            </label>
            <div className="space-y-1 max-h-56 overflow-y-auto pr-1">
              <button
                onClick={() => {
                  setSelectedCategory('all');
                  setSearchParams(searchQuery ? { search: searchQuery } : {});
                }}
                className={`w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition-colors flex items-center justify-between ${
                  selectedCategory === 'all'
                    ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                    : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                }`}
              >
                <span>All Categories</span>
                {selectedCategory === 'all' && <Check className="w-3.5 h-3.5" />}
              </button>

              {categories.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => {
                    setSelectedCategory(cat.id);
                    setSearchParams(searchQuery ? { category: cat.id, search: searchQuery } : { category: cat.id });
                  }}
                  className={`w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition-colors flex items-center justify-between ${
                    selectedCategory === cat.id
                      ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                      : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                  }`}
                >
                  <span className="truncate">{cat.name}</span>
                  {selectedCategory === cat.id && <Check className="w-3.5 h-3.5 shrink-0" />}
                </button>
              ))}
            </div>
          </div>

          {/* Price Range Filter */}
          <div className="space-y-2.5 pt-4 border-t border-slate-800">
            <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-slate-300">
              <span>Max Price</span>
              <span className="text-indigo-400 font-mono font-bold">${maxPrice}</span>
            </div>
            <input
              type="range"
              min="10"
              max="100000"
              step="50"
              value={maxPrice}
              onChange={(e) => setMaxPrice(Number(e.target.value))}
              className="w-full accent-indigo-500 bg-slate-800 rounded-lg cursor-pointer"
            />
            <div className="flex justify-between text-[10px] text-slate-500 font-mono">
              <span>$10</span>
              <span>$100,000</span>
            </div>
          </div>

          {/* Rating Filter */}
          <div className="space-y-2.5 pt-4 border-t border-slate-800">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Minimum Rating
            </label>
            <div className="space-y-1">
              {[0, 4.5, 4.0].map((stars) => (
                <button
                  key={stars}
                  onClick={() => setMinRating(stars)}
                  className={`w-full text-left px-3 py-2 rounded-xl text-xs font-medium transition-colors flex items-center justify-between ${
                    minRating === stars
                      ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30'
                      : 'text-slate-400 hover:bg-slate-900 hover:text-slate-200'
                  }`}
                >
                  <span className="flex items-center gap-1">
                    {stars === 0 ? (
                      'Any Rating'
                    ) : (
                      <>
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <span>{stars} &amp; Above</span>
                      </>
                    )}
                  </span>
                  {minRating === stars && <Check className="w-3.5 h-3.5" />}
                </button>
              ))}
            </div>
          </div>

          {/* Stock Filter */}
          <div className="pt-4 border-t border-slate-800">
            <label className="flex items-center gap-2.5 cursor-pointer text-xs font-semibold text-slate-300">
              <input
                type="checkbox"
                checked={inStockOnly}
                onChange={(e) => setInStockOnly(e.target.checked)}
                className="w-4 h-4 rounded border-slate-700 bg-slate-900 text-indigo-600 focus:ring-indigo-500"
              />
              <span>In Stock Items Only</span>
            </label>
          </div>
        </aside>

        {/* PRODUCTS AREA */}
        <div className="lg:col-span-3 space-y-6">
          
          {/* Controls Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 p-4 rounded-2xl bg-slate-950/70 border border-slate-800/80">
            <button
              onClick={() => setMobileFilterOpen(true)}
              className="lg:hidden inline-flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-700 text-xs font-semibold text-white"
            >
              <Filter className="w-4 h-4 text-indigo-400" />
              <span>Filters</span>
              {activeFiltersCount > 0 && (
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] flex items-center justify-center font-bold">
                  {activeFiltersCount}
                </span>
              )}
            </button>

            <div className="text-xs text-slate-400 font-medium">
              Showing <span className="text-white font-bold">{filteredProducts.length}</span> results
            </div>

            <div className="flex items-center gap-2 text-xs font-medium ml-auto">
              <span className="text-slate-400 hidden sm:inline">Sort By:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="bg-slate-900 border border-slate-700/80 rounded-xl px-3 py-2 text-white focus:outline-none focus:border-indigo-500 cursor-pointer"
              >
                <option value="featured">Featured First</option>
                <option value="price-low">Price: Low to High</option>
                <option value="price-high">Price: High to Low</option>
                <option value="rating">Highest Rated</option>
                <option value="newest">Newest Arrivals</option>
              </select>
            </div>
          </div>

          {/* Active Filter Pills */}
          {activeFiltersCount > 0 && (
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <span className="text-slate-400 font-medium">Active:</span>

              {searchQuery && (
                <Badge color="indigo" size="sm">
                  Search: "{searchQuery}"
                </Badge>
              )}

              {selectedCategory !== 'all' && (
                <Badge color="indigo" size="sm">
                  Category:{' '}
                  {categories.find((c) => c.id === selectedCategory)?.name || selectedCategory}
                </Badge>
              )}

              {maxPrice < 100000 && (
                <Badge color="indigo" size="sm">
                  Max: ${maxPrice}
                </Badge>
              )}

              {minRating > 0 && (
                <Badge color="indigo" size="sm">
                  Rating: {minRating}+ ★
                </Badge>
              )}

              {inStockOnly && (
                <Badge color="emerald" size="sm">
                  In Stock Only
                </Badge>
              )}

              <button
                onClick={handleResetFilters}
                className="text-xs text-rose-400 hover:underline font-semibold ml-2"
              >
                Clear All
              </button>
            </div>
          )}

          {/* Products Grid / Loading / Empty */}
          {loading ? (
            <div className="py-16">
              <Loading label="Fetching catalog products..." />
            </div>
          ) : filteredProducts.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-6">
              {filteredProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <EmptyState
              title="No products matched your criteria"
              description="We couldn't find any products matching your current filters. Try resetting search query or clearing price ranges."
              onAction={handleResetFilters}
              actionLabel="Reset All Filters"
            />
          )}

        </div>
      </div>

      {/* MOBILE FILTERS MODAL */}
      {mobileFilterOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm lg:hidden flex justify-end">
          <div className="w-full max-w-xs bg-slate-900 h-full p-6 space-y-6 overflow-y-auto border-l border-slate-800">
            <div className="flex items-center justify-between border-b border-slate-800 pb-4">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <Filter className="w-4 h-4 text-indigo-400" />
                <span>Filter Products</span>
              </h3>
              <button
                onClick={() => setMobileFilterOpen(false)}
                className="text-slate-400 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-semibold uppercase text-slate-300">Category</label>
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-xl px-3 py-2 text-xs text-white"
              >
                <option value="all">All Categories</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between text-xs text-slate-300">
                <span>Max Price</span>
                <span className="font-bold text-indigo-400">${maxPrice}</span>
              </div>
              <input
                type="range"
                min="10"
                max="100000"
                step="50"
                value={maxPrice}
                onChange={(e) => setMaxPrice(Number(e.target.value))}
                className="w-full accent-indigo-500"
              />
            </div>

            <div className="pt-6 border-t border-slate-800 flex gap-2">
              <Button
                variant="outline"
                size="sm"
                className="w-full"
                onClick={() => {
                  handleResetFilters();
                  setMobileFilterOpen(false);
                }}
              >
                Reset
              </Button>
              <Button
                variant="primary"
                size="sm"
                className="w-full"
                onClick={() => setMobileFilterOpen(false)}
              >
                Apply
              </Button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default Products;
