import api from './api';
import { MOCK_CATEGORIES } from '../data/mockData';

export const normalizeCategory = (cat) => {
  if (!cat) return null;
  return {
    id: String(cat.id),
    rawId: cat.id,
    name: cat.name || 'Unnamed Category',
    slug: cat.slug || '',
    description: cat.description || 'Explore products in this category',
    image: cat.image || cat.imageUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=800&q=80',
    imageUrl: cat.image || cat.imageUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=800&q=80',
    active: cat.active !== false,
    featured: cat.featured ?? true,
    productCount: cat.productCount || '10+',
  };
};

export const categoryService = {
  /**
   * Fetches all active product categories.
   * Seamlessly falls back to MOCK_CATEGORIES if backend returns empty or fails.
   * Endpoint: GET /api/categories
   */
  getCategories: async () => {
    try {
      const response = await api.get('/categories');
      const categories = Array.isArray(response.data) ? response.data : [];
      if (categories.length > 0) {
        console.log(`[categoryService] Backend returned ${categories.length} categories`);
        return categories.map(normalizeCategory);
      }
      console.warn('[categoryService] Backend returned 0 categories — falling back to mock.');
    } catch (err) {
      // Log the FULL error so DevTools in production exposes the real failure reason
      console.error('[categoryService] API getCategories failed — falling back to mock categories.', {
        message: err?.message,
        code: err?.code,
        status: err?.response?.status,
        url: err?.config?.url,
        baseURL: err?.config?.baseURL,
      });
    }
    return MOCK_CATEGORIES.map(normalizeCategory);
  },

  /**
   * Fetches a single category by ID or slug.
   * Endpoint: GET /api/categories/{id}
   */
  getCategoryById: async (id) => {
    try {
      const response = await api.get(`/categories/${id}`);
      if (response.data) {
        return normalizeCategory(response.data);
      }
    } catch (err) {
      // Proceed to fallback lookup
    }

    const found = MOCK_CATEGORIES.find(
      (c) => String(c.id) === String(id) || (c.slug && c.slug.toLowerCase() === String(id).toLowerCase())
    );
    return found ? normalizeCategory(found) : null;
  },
};

export default categoryService;
