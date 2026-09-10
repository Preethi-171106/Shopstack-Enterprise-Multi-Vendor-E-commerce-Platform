import api from './api';

export const normalizeCategory = (cat) => {
  if (!cat) return null;
  return {
    id: String(cat.id),
    rawId: cat.id,
    name: cat.name || 'Unnamed Category',
    slug: cat.slug || '',
    description: cat.description || 'Explore products in this category',
    image: cat.imageUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=800&q=80',
    imageUrl: cat.imageUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=800&q=80',
    active: cat.active !== false,
    featured: true,
    productCount: '10+',
  };
};

export const categoryService = {
  /**
   * Fetches all active product categories.
   * Endpoint: GET /api/categories
   */
  getCategories: async () => {
    const response = await api.get('/categories');
    const categories = Array.isArray(response.data) ? response.data : [];
    return categories.map(normalizeCategory);
  },

  /**
   * Fetches a single category by ID.
   * Endpoint: GET /api/categories/{id}
   */
  getCategoryById: async (id) => {
    const response = await api.get(`/categories/${id}`);
    return normalizeCategory(response.data);
  },
};

export default categoryService;
