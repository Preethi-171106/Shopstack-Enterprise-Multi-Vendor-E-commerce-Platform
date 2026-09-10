import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as api from '../lib/api';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { user } = useAuth();
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!user) {
      setCart(null);
      return;
    }
    setLoading(true);
    try {
      const c = await api.ensureCart(user.id);
      setCart(c);
    } catch {
      setCart(null);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addItem = useCallback(
    async (productId, quantity = 1) => {
      if (!user) return;
      const c = cart || (await api.ensureCart(user.id));
      await api.addCartItem(c.id, productId, quantity);
      await refresh();
    },
    [user, cart, refresh]
  );

  const updateItem = useCallback(
    async (itemId, quantity) => {
      await api.updateCartItem(itemId, quantity);
      await refresh();
    },
    [refresh]
  );

  const removeItem = useCallback(
    async (itemId) => {
      await api.deleteCartItem(itemId);
      await refresh();
    },
    [refresh]
  );

  const clear = useCallback(async () => {
    if (cart) await api.clearCart(cart.id);
    await refresh();
  }, [cart, refresh]);

  const itemCount = cart?.items?.reduce((sum, it) => sum + it.quantity, 0) || 0;
  const total = cart?.items?.reduce(
    (sum, it) => sum + (it.product?.price || 0) * it.quantity,
    0
  ) || 0;

  return (
    <CartContext.Provider
      value={{ cart, loading, itemCount, total, refresh, addItem, updateItem, removeItem, clear }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
