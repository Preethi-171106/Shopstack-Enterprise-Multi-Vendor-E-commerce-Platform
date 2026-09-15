import api from './api';
import { MOCK_PRODUCTS } from '../data/mockData';

const LOCAL_PRODUCTS_KEY = 'shopstack_custom_products';

/**
 * Retrieves locally added / cached products from localStorage.
 */
export const getLocalProducts = () => {
  try {
    const raw = localStorage.getItem(LOCAL_PRODUCTS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch (err) {
    console.warn('Failed to read local products from localStorage:', err);
    return [];
  }
};

/**
 * Saves or updates a locally created product in localStorage.
 */
export const saveLocalProduct = (product) => {
  try {
    const current = getLocalProducts();
    const existingIndex = current.findIndex((p) => String(p.id) === String(product.id) || (p.sku && p.sku === product.sku));
    let updated;
    if (existingIndex >= 0) {
      updated = [...current];
      updated[existingIndex] = { ...updated[existingIndex], ...product };
    } else {
      updated = [product, ...current];
    }
    localStorage.setItem(LOCAL_PRODUCTS_KEY, JSON.stringify(updated));
    return updated;
  } catch (err) {
    console.warn('Failed to save local product to localStorage:', err);
    return [];
  }
};

/**
 * Deletes a locally created product from localStorage.
 */
export const deleteLocalProduct = (productId) => {
  try {
    const current = getLocalProducts();
    const updated = current.filter((p) => String(p.id) !== String(productId));
    localStorage.setItem(LOCAL_PRODUCTS_KEY, JSON.stringify(updated));
    return updated;
  } catch (err) {
    console.warn('Failed to delete local product from localStorage:', err);
    return [];
  }
};

/**
 * Normalizes a Product (backend ProductResponse or MockProduct) into a consistent UI format.
 */
export const normalizeProduct = (p) => {
  if (!p) return null;
  const price = typeof p.price === 'number' ? p.price : parseFloat(p.price || 0);
  const originalPrice = p.originalPrice
    ? (typeof p.originalPrice === 'number' ? p.originalPrice : parseFloat(p.originalPrice))
    : null;
  const stockQuantity = p.stockQuantity ?? p.stock ?? 15;
  const imageUrl =
    p.imageUrl ||
    (Array.isArray(p.images) && p.images[0]) ||
    'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80';
  const images = Array.isArray(p.images) && p.images.length > 0 ? p.images : [imageUrl];

  const vendorName = p.vendor?.name || p.storeName || 'Aura Tech Solutions';
  const vendorId = p.vendor?.id || p.vendorProfileId || 'v-1';

  return {
    id: p.id,
    name: p.name || 'Unnamed Product',
    slug: p.slug || '',
    description: p.description || '',
    price: price,
    originalPrice: originalPrice,
    stock: stockQuantity,
    stockQuantity: stockQuantity,
    inStock: p.active !== false && p.inStock !== false && stockQuantity > 0,
    imageUrl: imageUrl,
    images: images,
    features: Array.isArray(p.features) ? p.features : [],
    rating: p.rating ? parseFloat(p.rating) : 4.8,
    reviewCount: p.reviewCount ?? 15,
    badge: p.badge || (p.featured || p.isFeatured ? 'Featured' : null),
    isFeatured: Boolean(p.featured || p.isFeatured),
    categoryId: p.categoryId ? String(p.categoryId) : 'all',
    categoryName: p.categoryName || (typeof p.category === 'object' ? p.category?.name : p.category) || 'General',
    categorySlug: p.categorySlug || '',
    vendorProfileId: vendorId,
    storeName: vendorName,
    vendor: {
      id: vendorId,
      name: vendorName,
      verified: true,
      category: p.categoryName || 'General',
      rating: p.vendor?.rating || 4.9,
    },
    specifications: p.specifications || (p.sku ? { SKU: p.sku, Store: vendorName } : { Store: vendorName }),
    sku: p.sku || `SKU-${p.id}`,
  };
};

/**
 * Returns complete fallback product dataset (mock products + custom local products).
 */
const getFallbackProducts = () => {
  const local = getLocalProducts().map(normalizeProduct);
  const mock = MOCK_PRODUCTS.map(normalizeProduct);

  // Combine custom local products first, then mock products
  const combined = [...local];
  for (const m of mock) {
    if (!combined.some((c) => String(c.id) === String(m.id) || (c.slug && c.slug === m.slug))) {
      combined.push(m);
    }
  }
  return combined;
};

export const productService = {
  /**
   * Fetches public products with optional search and category filters.
   * Seamlessly falls back to mock/local products if backend returns empty or fails.
   * Endpoint: GET /api/products
   */
  getProducts: async ({ search = '', categoryId = null, page = 0, size = 20 } = {}) => {
    let backendProducts = [];
    let totalElements = 0;
    let totalPages = 1;

    try {
      const params = {};
      if (search && search.trim()) params.search = search.trim();
      if (categoryId && categoryId !== 'all') params.categoryId = categoryId;
      params.page = page;
      params.size = size;

      const response = await api.get('/products', { params });
      const data = response.data;
      const rawContent = data.content || (Array.isArray(data) ? data : []);
      backendProducts = rawContent.map(normalizeProduct);
      totalElements = data.totalElements ?? backendProducts.length;
      totalPages = data.totalPages ?? 1;
      console.log(`[productService] Backend returned ${backendProducts.length} products (totalElements=${totalElements})`);
    } catch (err) {
      // Log the FULL error so DevTools in production exposes the real failure reason
      // (CORS block, Render cold-start timeout, network error, etc.)
      console.error('[productService] API getProducts failed — falling back to local/mock data.', {
        message: err?.message,
        code: err?.code,
        status: err?.response?.status,
        url: err?.config?.url,
        baseURL: err?.config?.baseURL,
      });
    }

    // If backend returned products, merge with any local products
    if (backendProducts.length > 0) {
      const local = getLocalProducts().map(normalizeProduct);
      const merged = [...local, ...backendProducts];
      const unique = Array.from(new Map(merged.map((item) => [String(item.id), item])).values());
      return {
        content: unique,
        totalElements: unique.length,
        totalPages: Math.ceil(unique.length / size) || 1,
        pageNumber: page,
        size: size,
      };
    }

    // Fallback mode: filter local + mock products
    const fallbackAll = getFallbackProducts();
    let filtered = fallbackAll;

    if (search && search.trim()) {
      const query = search.trim().toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.name.toLowerCase().includes(query) ||
          p.description.toLowerCase().includes(query) ||
          p.categoryName.toLowerCase().includes(query) ||
          p.storeName.toLowerCase().includes(query)
      );
    }

    if (categoryId && categoryId !== 'all') {
      filtered = filtered.filter(
        (p) =>
          String(p.categoryId) === String(categoryId) ||
          p.categorySlug === categoryId ||
          p.categoryName?.toLowerCase().includes(String(categoryId).toLowerCase())
      );
    }

    const start = page * size;
    const paginated = filtered.slice(start, start + size);

    return {
      content: paginated,
      totalElements: filtered.length,
      totalPages: Math.ceil(filtered.length / size) || 1,
      pageNumber: page,
      size: size,
    };
  },

  /**
   * Fetches a single public product by ID or slug.
   * Endpoint: GET /api/products/{id}
   */
  getProductById: async (id) => {
    try {
      const response = await api.get(`/products/${id}`);
      if (response.data) {
        return normalizeProduct(response.data);
      }
    } catch (err) {
      // Backend not found or offline; proceed to fallback lookup
    }

    // Check in local and mock fallback products
    const fallbackList = getFallbackProducts();
    const found = fallbackList.find(
      (p) =>
        String(p.id) === String(id) ||
        (p.slug && p.slug.toLowerCase() === String(id).toLowerCase()) ||
        String(p.id) === `prod-${id}` ||
        `prod-${p.id}` === String(id)
    );

    return found ? normalizeProduct(found) : null;
  },

  /**
   * Helper to persist a vendor product locally and to backend if available.
   */
  saveProduct: (product) => {
    return saveLocalProduct(product);
  },

  /**
   * Helper to delete a product locally.
   */
  deleteProduct: (productId) => {
    return deleteLocalProduct(productId);
  },
};

// Export individual functions for backward compatibility
export const getProducts = async (params) => {
  return await productService.getProducts(params);
};

export const getProductById = async (id) => {
  return await productService.getProductById(id);
};

export default productService;