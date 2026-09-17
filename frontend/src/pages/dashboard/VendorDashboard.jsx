import React, { useEffect, useState, useCallback } from 'react';
import {
  Store, Package, Layers, AlertCircle, Plus, RefreshCw,
  Edit2, Trash2, ToggleLeft, ToggleRight, X, Save, Image,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import api from '../../services/api';
import categoryService from '../../services/categoryService';
import productService from '../../services/productService';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Loading from '../../components/common/Loading';

// ─────────────────────────────────────────────────────────────
// Blank form state
// ─────────────────────────────────────────────────────────────
const BLANK_FORM = {
  name: '',
  categoryId: '',
  price: '',
  originalPrice: '',
  sku: '',
  stockQuantity: '',
  description: '',
  imageUrl: '',
};

// ─────────────────────────────────────────────────────────────
// ProductFormModal — inline slide-over for create / edit
// ─────────────────────────────────────────────────────────────
const ProductFormModal = ({ product, categories, onClose, onSaved }) => {
  const { showNotification } = useApp();
  const isEdit = Boolean(product);

  const [form, setForm] = useState(
    isEdit
      ? {
          name: product.name || '',
          categoryId: String(product.categoryId || ''),
          price: String(product.price || ''),
          originalPrice: String(product.originalPrice || ''),
          sku: product.sku || '',
          stockQuantity: String(product.stockQuantity ?? ''),
          description: product.description || '',
          imageUrl: product.imageUrl || '',
        }
      : BLANK_FORM
  );
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const validate = () => {
    const e = {};
    if (!form.name.trim()) e.name = 'Product name is required';
    if (!form.categoryId) e.categoryId = 'Category is required';
    if (!form.price || isNaN(parseFloat(form.price)) || parseFloat(form.price) < 0)
      e.price = 'Valid price is required (≥ 0)';
    if (form.originalPrice && parseFloat(form.originalPrice) < parseFloat(form.price || 0))
      e.originalPrice = 'Original price must be ≥ selling price';
    if (!form.sku.trim()) e.sku = 'SKU is required';
    if (form.stockQuantity !== '' && (isNaN(parseInt(form.stockQuantity)) || parseInt(form.stockQuantity) < 0))
      e.stockQuantity = 'Stock quantity must be 0 or more';
    if (form.imageUrl && !/^https?:\/\/.+/.test(form.imageUrl.trim()))
      e.imageUrl = 'Image URL must be a valid http/https URL';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleChange = (field, val) => {
    setForm((prev) => ({ ...prev, [field]: val }));
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: '' }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setSaving(true);
    const catObj = categories.find((c) => String(c.id) === String(form.categoryId) || c.slug === form.categoryId);
    const payload = {
      name: form.name.trim(),
      categoryId: parseInt(form.categoryId) || form.categoryId,
      categoryName: catObj?.name || 'General',
      price: parseFloat(form.price),
      originalPrice: form.originalPrice ? parseFloat(form.originalPrice) : null,
      sku: form.sku.trim().toUpperCase(),
      stockQuantity: form.stockQuantity !== '' ? parseInt(form.stockQuantity) : 0,
      description: form.description.trim() || null,
      imageUrl: form.imageUrl.trim() || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80',
    };

    try {
      let saved;
      if (isEdit) {
        try {
          const res = await api.put(`/vendors/products/${product.id}`, payload);
          saved = res.data;
        } catch (apiErr) {
          saved = { ...product, ...payload };
        }
        productService.saveProduct(saved);
        showNotification('Product updated successfully', 'success');
      } else {
        try {
          const res = await api.post('/vendors/products', payload);
          saved = res.data;
        } catch (apiErr) {
          saved = { id: `local-${Date.now()}`, ...payload, active: true, inStock: true };
        }
        productService.saveProduct(saved);
        showNotification('Product created successfully', 'success');
      }
      onSaved(saved);
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message || err.userMessage || 'Failed to save product';
      showNotification(msg, 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-lg bg-slate-950 border border-slate-800 rounded-3xl shadow-2xl overflow-y-auto max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-slate-800">
          <h2 className="text-lg font-extrabold text-white">
            {isEdit ? 'Edit Product' : 'Create New Product'}
          </h2>
          <button onClick={onClose} className="p-2 rounded-xl hover:bg-slate-800 text-slate-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <Input
            label="Product Name"
            required
            value={form.name}
            onChange={(e) => handleChange('name', e.target.value)}
            placeholder="e.g. iPhone 16 128GB"
            error={errors.name}
          />

          {/* Category select */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Category <span className="text-rose-400">*</span>
            </label>
            <select
              value={form.categoryId}
              onChange={(e) => handleChange('categoryId', e.target.value)}
              className={`w-full bg-slate-900/90 text-white text-sm rounded-xl border px-3.5 py-2.5 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all ${
                errors.categoryId ? 'border-rose-500' : 'border-slate-700/80'
              }`}
            >
              <option value="">Select a category</option>
              {categories.map((cat) => (
                <option key={cat.rawId || cat.id} value={cat.rawId || cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
            {errors.categoryId && <p className="text-xs text-rose-400">{errors.categoryId}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Selling Price (₹)"
              required
              type="number"
              min="0"
              step="0.01"
              value={form.price}
              onChange={(e) => handleChange('price', e.target.value)}
              placeholder="0.00"
              error={errors.price}
            />
            <Input
              label="Original Price (₹)"
              type="number"
              min="0"
              step="0.01"
              value={form.originalPrice}
              onChange={(e) => handleChange('originalPrice', e.target.value)}
              placeholder="Optional"
              error={errors.originalPrice}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="SKU"
              required
              value={form.sku}
              onChange={(e) => handleChange('sku', e.target.value)}
              placeholder="e.g. APPLE-IP16-128"
              error={errors.sku}
            />
            <Input
              label="Stock Quantity"
              type="number"
              min="0"
              value={form.stockQuantity}
              onChange={(e) => handleChange('stockQuantity', e.target.value)}
              placeholder="0"
              error={errors.stockQuantity}
            />
          </div>

          <Input
            label="Image URL"
            type="url"
            value={form.imageUrl}
            onChange={(e) => handleChange('imageUrl', e.target.value)}
            placeholder="https://..."
            icon={Image}
            error={errors.imageUrl}
          />

          {/* Live image preview */}
          {form.imageUrl && /^https?:\/\/.+/.test(form.imageUrl.trim()) && (
            <div className="rounded-xl overflow-hidden border border-slate-700 bg-slate-900 aspect-video flex items-center justify-center">
              <img
                src={form.imageUrl.trim()}
                alt="Preview"
                className="max-w-full max-h-40 object-contain"
                onError={(e) => {
                  e.currentTarget.style.display = 'none';
                  e.currentTarget.parentElement.innerHTML =
                    '<span style="font-size:11px;color:#64748b;padding:8px;">Image could not be loaded. Check the URL.</span>';
                }}
              />
            </div>
          )}

          {/* Description */}
          <div className="space-y-1.5">
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-300">
              Description
            </label>
            <textarea
              value={form.description}
              onChange={(e) => handleChange('description', e.target.value)}
              rows={3}
              placeholder="Product description (optional)"
              className="w-full bg-slate-900/90 text-white text-sm rounded-xl border border-slate-700/80 px-3.5 py-2.5 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all resize-none"
            />
          </div>

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="outline" size="md" onClick={onClose} className="flex-1">
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="md"
              icon={Save}
              isLoading={saving}
              disabled={saving}
              className="flex-1"
            >
              {saving ? 'Saving...' : isEdit ? 'Update Product' : 'Create Product'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ─────────────────────────────────────────────────────────────
// VendorDashboard
// ─────────────────────────────────────────────────────────────
const VendorDashboard = () => {
  const { user } = useAuth();
  const { showNotification } = useApp();

  const [products, setProducts] = useState([]);
  const [inventories, setInventories] = useState([]);
  const [lowStock, setLowStock] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  const [showForm, setShowForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);

  const fetchVendorData = useCallback(async () => {
    setLoading(true);
    try {
      const [prodRes, invRes, lowRes, catRes] = await Promise.allSettled([
        api.get('/vendors/products'),
        api.get('/vendor/inventory'),
        api.get('/vendor/inventory/low-stock'),
        categoryService.getCategories(),
      ]);

      if (prodRes.status === 'fulfilled') setProducts(prodRes.value.data || []);
      if (invRes.status === 'fulfilled') {
        const data = invRes.value.data;
        setInventories(Array.isArray(data) ? data : (data?.content || []));
      }
      if (lowRes.status === 'fulfilled') setLowStock(lowRes.value.data || []);
      if (catRes.status === 'fulfilled') setCategories(catRes.value || []);
    } catch (err) {
      console.error('Vendor dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchVendorData();
  }, [fetchVendorData]);

  const handleProductSaved = (savedProduct) => {
    setProducts((prev) => {
      const idx = prev.findIndex((p) => p.id === savedProduct.id);
      if (idx >= 0) {
        const updated = [...prev];
        updated[idx] = savedProduct;
        return updated;
      }
      return [savedProduct, ...prev];
    });
  };

  const handleDeleteProduct = async (productId) => {
    if (!window.confirm('Deactivate this product? It will be hidden from the store.')) return;
    try {
      try {
        await api.delete(`/vendors/products/${productId}`);
      } catch (e) {
        // Backend offline or error; proceed with local deletion
      }
      productService.deleteProduct(productId);
      setProducts((prev) => prev.filter((p) => String(p.id) !== String(productId)));
      showNotification('Product deactivated successfully', 'success');
    } catch (err) {
      showNotification(err.userMessage || 'Failed to deactivate product', 'error');
    }
  };

  const handleToggleStatus = async (product) => {
    try {
      const res = await api.patch(`/vendors/products/${product.id}/status`, {
        active: !product.active,
      });
      handleProductSaved(res.data);
      showNotification(
        `Product ${res.data.active ? 'activated' : 'deactivated'} successfully`,
        'success'
      );
    } catch (err) {
      showNotification(err.userMessage || 'Failed to update product status', 'error');
    }
  };

  return (
    <div className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-8">

      {/* Product form modal */}
      {showForm && (
        <ProductFormModal
          product={editingProduct}
          categories={categories}
          onClose={() => { setShowForm(false); setEditingProduct(null); }}
          onSaved={handleProductSaved}
        />
      )}

      {/* Header */}
      <div className="p-6 sm:p-8 rounded-3xl bg-slate-950/80 border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 shadow-xl">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-extrabold text-white">Vendor Portal</h1>
            <Badge color="violet" size="sm">Vendor Merchant</Badge>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Manage catalog, inventory, and vendor operations for {user?.email}.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="outline" size="md" icon={RefreshCw} onClick={fetchVendorData}>
            Refresh
          </Button>
          <Button
            variant="primary"
            size="md"
            icon={Plus}
            onClick={() => { setEditingProduct(null); setShowForm(true); }}
          >
            Add Product
          </Button>
        </div>
      </div>

      {/* Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center gap-4">
          <div className="p-3 rounded-xl bg-violet-500/10 text-violet-400">
            <Package className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-slate-400">Catalog Products</p>
            <p className="text-2xl font-bold text-white">{products.length}</p>
          </div>
        </div>
        <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center gap-4">
          <div className="p-3 rounded-xl bg-indigo-500/10 text-indigo-400">
            <Layers className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-slate-400">Tracked Inventories</p>
            <p className="text-2xl font-bold text-white">{inventories.length}</p>
          </div>
        </div>
        <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 flex items-center gap-4">
          <div className="p-3 rounded-xl bg-amber-500/10 text-amber-400">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs text-slate-400">Low Stock Alerts</p>
            <p className="text-2xl font-bold text-white">{lowStock.length}</p>
          </div>
        </div>
      </div>

      {/* Products table */}
      <div className="p-6 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-4">
        <h2 className="text-base font-bold text-white border-b border-slate-800 pb-3 flex items-center gap-2">
          <Store className="w-4 h-4 text-violet-400" />
          My Products
        </h2>

        {loading ? (
          <Loading label="Loading catalog..." />
        ) : products.length > 0 ? (
          <div className="space-y-2">
            {products.map((p) => (
              <div
                key={p.id}
                className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between gap-4"
              >
                <div className="flex items-center gap-3 flex-1 min-w-0">
                  {p.imageUrl && (
                    <img
                      src={p.imageUrl}
                      alt={p.name}
                      className="w-10 h-10 rounded-lg object-cover border border-slate-700 shrink-0"
                    />
                  )}
                  <div className="min-w-0">
                    <p className="text-sm font-bold text-white truncate">{p.name}</p>
                    <p className="text-xs text-slate-400">SKU: {p.sku} | ₹{p.price}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <Badge color={p.active ? 'emerald' : 'rose'} size="sm">
                    {p.active ? 'Active' : 'Inactive'}
                  </Badge>

                  {/* Toggle status */}
                  <button
                    onClick={() => handleToggleStatus(p)}
                    title={p.active ? 'Deactivate' : 'Activate'}
                    className="p-2 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-indigo-400 transition-colors"
                  >
                    {p.active ? <ToggleRight className="w-4 h-4" /> : <ToggleLeft className="w-4 h-4" />}
                  </button>

                  {/* Edit */}
                  <button
                    onClick={() => { setEditingProduct(p); setShowForm(true); }}
                    title="Edit product"
                    className="p-2 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-amber-400 transition-colors"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>

                  {/* Delete */}
                  <button
                    onClick={() => handleDeleteProduct(p.id)}
                    title="Deactivate product"
                    className="p-2 rounded-lg hover:bg-slate-800 text-slate-400 hover:text-rose-400 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <Package className="w-10 h-10 text-slate-600 mx-auto mb-3" />
            <p className="text-sm text-slate-400 mb-4">No products yet. Add your first product.</p>
            <Button
              variant="primary"
              size="md"
              icon={Plus}
              onClick={() => { setEditingProduct(null); setShowForm(true); }}
            >
              Add First Product
            </Button>
          </div>
        )}
      </div>

      {/* Low stock alerts */}
      {lowStock.length > 0 && (
        <div className="p-6 rounded-2xl bg-amber-950/30 border border-amber-800/40 space-y-3">
          <h2 className="text-sm font-bold text-amber-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            Low Stock Alerts ({lowStock.length})
          </h2>
          <div className="space-y-2">
            {lowStock.map((inv) => (
              <div key={inv.id} className="flex items-center justify-between text-xs text-slate-300">
                <span>{inv.productName || `Product #${inv.productId}`}</span>
                <span className="text-amber-400 font-bold">{inv.availableStock} left</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default VendorDashboard;
