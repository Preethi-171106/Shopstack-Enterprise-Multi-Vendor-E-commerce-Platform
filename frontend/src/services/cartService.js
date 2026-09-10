import api from './api';
import { normalizeProduct } from './productService';

export const normalizeCartItem = (item) => {
  if (!item) return null;
  return {
    id: item.id,
    quantity: item.quantity,
    subtotal: item.subtotal ? parseFloat(item.subtotal) : 0,
    product: normalizeProduct({
      id: item.productId,
      name: item.productName,
      slug: item.productSlug,
      price: item.price,
      imageUrl: item.imageUrl,
      vendorProfileId: item.vendorProfileId,
      storeName: item.storeName,
      stockQuantity: item.stockQuantity,
      active: item.active,
    }),
  };
};

export const cartService = {
  /**
   * Returns current user cart.
   * Endpoint: GET /api/cart
   */
  getCart: async () => {
    const response = await api.get('/cart');
    const data = response.data;
    return {
      items: (data.items || []).map(normalizeCartItem),
      subtotal: data.subtotal ? parseFloat(data.subtotal) : 0,
      totalItems: data.totalItems ?? 0,
      totalQuantity: data.totalQuantity ?? 0,
    };
  },

  /**
   * Adds an item to the shopping cart.
   * Endpoint: POST /api/cart/items
   */
  addToCart: async (productId, quantity = 1) => {
    const response = await api.post('/cart/items', {
      productId: Number(productId),
      quantity: Number(quantity),
    });
    const data = response.data;
    return {
      items: (data.items || []).map(normalizeCartItem),
      subtotal: data.subtotal ? parseFloat(data.subtotal) : 0,
      totalItems: data.totalItems ?? 0,
      totalQuantity: data.totalQuantity ?? 0,
    };
  },

  /**
   * Updates cart item quantity.
   * Endpoint: PUT /api/cart/items/{id}
   */
  updateCartItemQuantity: async (itemId, quantity) => {
    const response = await api.put(`/cart/items/${itemId}`, {
      quantity: Number(quantity),
    });
    const data = response.data;
    return {
      items: (data.items || []).map(normalizeCartItem),
      subtotal: data.subtotal ? parseFloat(data.subtotal) : 0,
      totalItems: data.totalItems ?? 0,
      totalQuantity: data.totalQuantity ?? 0,
    };
  },

  /**
   * Removes a single cart item.
   * Endpoint: DELETE /api/cart/items/{id}
   */
  removeCartItem: async (itemId) => {
    await api.delete(`/cart/items/${itemId}`);
  },

  /**
   * Clears all cart items.
   * Endpoint: DELETE /api/cart
   */
  clearCart: async () => {
    await api.delete('/cart');
  },
};

export default cartService;
