import api from './api';
import { normalizeProduct } from './productService';

export const normalizeWishlistItem = (item) => {
  if (!item) return null;
  return {
    wishlistId: item.id,
    ...normalizeProduct({
      id: item.productId,
      name: item.productName,
      slug: item.productSlug,
      price: item.price,
      imageUrl: item.imageUrl,
      vendorProfileId: item.vendorProfileId,
      storeName: item.storeName,
      rating: item.rating,
      reviewCount: item.reviewCount,
      stockQuantity: item.stockQuantity,
      active: item.active,
    }),
  };
};

export const wishlistService = {
  /**
   * Retrieves saved wishlist items for authenticated user.
   * Endpoint: GET /api/wishlist
   */
  getWishlist: async () => {
    const response = await api.get('/wishlist');
    const items = Array.isArray(response.data) ? response.data : [];
    return items.map(normalizeWishlistItem);
  },

  /**
   * Adds a product to wishlist.
   * Endpoint: POST /api/wishlist/items/{productId}
   */
  addToWishlist: async (productId) => {
    const response = await api.post(`/wishlist/items/${productId}`);
    return normalizeWishlistItem(response.data);
  },

  /**
   * Removes a product from wishlist.
   * Endpoint: DELETE /api/wishlist/items/{productId}
   */
  removeFromWishlist: async (productId) => {
    await api.delete(`/wishlist/items/${productId}`);
  },

  /**
   * Clears entire wishlist.
   * Endpoint: DELETE /api/wishlist
   */
  clearWishlist: async () => {
    await api.delete('/wishlist');
  },
};

export default wishlistService;
