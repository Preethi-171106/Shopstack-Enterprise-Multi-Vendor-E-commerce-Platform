import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as api from '../lib/api';
import { useAuth } from './AuthContext';

const WishlistContext = createContext(null);

export function WishlistProvider({ children }) {
  const { user } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!user) {
      setItems([]);
      return;
    }
    setLoading(true);
    try {
      const data = await api.getWishlist(user.id);
      setItems(data);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const add = useCallback(
    async (productId) => {
      if (!user) return;
      await api.addWishlistItem(user.id, productId);
      await refresh();
    },
    [user, refresh]
  );

  const remove = useCallback(
    async (id) => {
      await api.removeWishlistItem(id);
      await refresh();
    },
    [refresh]
  );

  const has = useCallback(
    (productId) => items.some((i) => i.product_id === productId),
    [items]
  );

  const toggle = useCallback(
    async (productId) => {
      const existing = items.find((i) => i.product_id === productId);
      if (existing) await remove(existing.id);
      else await add(productId);
    },
    [items, add, remove]
  );

  return (
    <WishlistContext.Provider value={{ items, loading, count: items.length, add, remove, has, toggle, refresh }}>
      {children}
    </WishlistContext.Provider>
  );
}

export function useWishlist() {
  const ctx = useContext(WishlistContext);
  if (!ctx) throw new Error('useWishlist must be used within WishlistProvider');
  return ctx;
}
