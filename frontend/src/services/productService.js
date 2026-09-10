import api from './api';

/**
 * Normalizes a backend ProductResponse into a format consistent with all UI components.
 */
export const normalizeProduct = (p) => {
  if (!p) return null;
  const price = typeof p.price === 'number' ? p.price : parseFloat(p.price || 0);
  const originalPrice = p.originalPrice ? (typeof p.originalPrice === 'number' ? p.originalPrice : parseFloat(p.originalPrice)) : null;
  const stockQuantity = p.stockQuantity ?? p.stock ?? 10;
  const imageUrl = p.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80';
  
  return {
    id: p.id,
    name: p.name || 'Unnamed Product',
    slug: p.slug || '',
    description: p.description || '',
    price: price,
    originalPrice: originalPrice,
    stock: stockQuantity,
    stockQuantity: stockQuantity,
    inStock: p.active !== false && stockQuantity > 0,
    imageUrl: imageUrl,
    images: [imageUrl],
    rating: p.rating ? parseFloat(p.rating) : 4.5,
    reviewCount: p.reviewCount ?? 0,
    badge: p.featured ? 'Featured' : null,
    isFeatured: Boolean(p.featured),
    categoryId: p.categoryId ? String(p.categoryId) : 'all',
    categoryName: p.categoryName || 'General',
    categorySlug: p.categorySlug || '',
    vendorProfileId: p.vendorProfileId,
    storeName: p.storeName || 'Verified Vendor',
    vendor: {
      id: p.vendorProfileId,
      name: p.storeName || 'Verified Vendor',
      verified: true,
      category: p.categoryName || 'General',
    },
    specifications: p.sku ? { SKU: p.sku, Store: p.storeName || 'Verified Vendor' } : { Store: p.storeName || 'Verified Vendor' },
  };
};

export const productService = {
  /**
   * Fetches public products with optional search and category filters.
   * Endpoint: GET /api/products
   */
  getProducts: async ({ search = '', categoryId = null, page = 0, size = 20 } = {}) => {
    const params = {};
    if (search && search.trim()) params.search = search.trim();
    if (categoryId && categoryId !== 'all') params.categoryId = categoryId;
    params.page = page;
    params.size = size;

    const response = await api.get('/products', { params });
    const data = response.data;
    
    // Spring Page object vs raw list
    const content = data.content || (Array.isArray(data) ? data : []);
    const normalizedContent = content.map(normalizeProduct);

    return {
      content: normalizedContent,
      totalElements: data.totalElements ?? normalizedContent.length,
      totalPages: data.totalPages ?? 1,
      pageNumber: data.number ?? page,
      size: data.size ?? size,
    };
  },

  /**
   * Fetches a single public product by ID.
   * Endpoint: GET /api/products/{id}
   */
  getProductById: async (id) => {
    const response = await api.get(`/products/${id}`);
    return normalizeProduct(response.data);
  },
};

// Export individual functions for backward compatibility
export const getProducts = async (params) => {
  const result = await productService.getProducts(params);
  return result;
};

export const getProductById = async (id) => {
  return await productService.getProductById(id);
};

export default productService;