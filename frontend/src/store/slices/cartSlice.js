import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import cartService from '../../services/cartService';
import couponService from '../../services/couponService';

export const fetchCartThunk = createAsyncThunk('cart/fetchCart', async (_, { rejectWithValue }) => {
  try {
    const cart = await cartService.getCart();
    return cart;
  } catch (error) {
    return rejectWithValue(error.userMessage || error.message);
  }
});

export const addToCartThunk = createAsyncThunk(
  'cart/addToCartBackend',
  async ({ productId, quantity = 1 }, { rejectWithValue }) => {
    try {
      const cart = await cartService.addToCart(productId, quantity);
      return cart;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

export const updateQuantityThunk = createAsyncThunk(
  'cart/updateQuantityBackend',
  async ({ itemId, quantity }, { rejectWithValue }) => {
    try {
      const cart = await cartService.updateCartItemQuantity(itemId, quantity);
      return cart;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

export const removeFromCartThunk = createAsyncThunk(
  'cart/removeFromCartBackend',
  async (itemId, { rejectWithValue, dispatch }) => {
    try {
      await cartService.removeCartItem(itemId);
      dispatch(fetchCartThunk());
      return itemId;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

export const clearCartThunk = createAsyncThunk('cart/clearCartBackend', async (_, { rejectWithValue }) => {
  try {
    await cartService.clearCart();
    return [];
  } catch (error) {
    return rejectWithValue(error.userMessage || error.message);
  }
});

export const syncCartThunk = createAsyncThunk(
  'cart/syncCartBackend',
  async (_, { getState, rejectWithValue }) => {
    try {
      const state = getState();
      const localItems = state.cart.items || [];
      // Only push unpersisted guest items (guest items explicitly have isGuest === true)
      const guestItems = localItems.filter((item) => item.isGuest === true);
      for (const item of guestItems) {
        const productId = item.product?.id || item.productId;
        const quantity = item.quantity || 1;
        if (productId) {
          try {
            await cartService.addToCart(productId, quantity);
          } catch (e) {
            console.warn(`[syncCartThunk] Failed to sync item ${productId}:`, e);
          }
        }
      }
      const cart = await cartService.getCart();
      return cart;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

export const applyCouponThunk = createAsyncThunk('cart/applyCouponBackend', async (code, { getState, rejectWithValue }) => {
  try {
    const state = getState();
    const subtotal = selectCartSubtotal(state);
    const coupon = await couponService.applyCoupon(code, subtotal);
    return coupon;
  } catch (error) {
    return rejectWithValue(error.userMessage || error.message);
  }
});

const initialState = {
  items: [], // Array of { product, quantity, id }
  appliedCoupon: null, // { code, type, value, description }
  status: 'idle',
  error: null,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState,
  reducers: {
    setCartData: (state, action) => {
      state.items = action.payload.items || [];
    },
    addToCart: (state, action) => {
      const product = action.payload.product || action.payload;
      const addQty = action.payload.quantity || 1;
      const maxStock = product.stock || product.stockQuantity || 99;

      const existingIndex = state.items.findIndex(
        (item) => (item.product?.id || item.productId) === product.id
      );

      if (existingIndex >= 0) {
        const currentQty = state.items[existingIndex].quantity;
        const newQty = Math.min(maxStock, currentQty + addQty);
        state.items[existingIndex].quantity = newQty;
      } else {
        const initialQty = Math.min(maxStock, Math.max(1, addQty));
        state.items.push({
          id: `guest_${product.id}`,
          isGuest: true,
          product,
          quantity: initialQty,
        });
      }
    },

    removeFromCart: (state, action) => {
      const productId = action.payload;
      state.items = state.items.filter((item) => item.product.id !== productId && item.id !== productId);
    },

    increaseQuantity: (state, action) => {
      const productId = action.payload;
      const item = state.items.find((i) => i.product.id === productId || i.id === productId);
      if (item) {
        const maxStock = item.product.stock || 99;
        if (item.quantity < maxStock) {
          item.quantity += 1;
        }
      }
    },

    decreaseQuantity: (state, action) => {
      const productId = action.payload;
      const item = state.items.find((i) => i.product.id === productId || i.id === productId);
      if (item && item.quantity > 1) {
        item.quantity -= 1;
      }
    },

    applyCoupon: (state, action) => {
      const code = typeof action.payload === 'string' ? action.payload : action.payload?.code;
      if (code) {
        state.appliedCoupon = {
          code: code.toUpperCase(),
          type: 'percentage',
          value: 10,
          description: '10% discount applied',
        };
      }
    },

    setAppliedCoupon: (state, action) => {
      state.appliedCoupon = action.payload;
    },

    removeCoupon: (state) => {
      state.appliedCoupon = null;
    },

    clearCart: (state) => {
      state.items = [];
      state.appliedCoupon = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCartThunk.fulfilled, (state, action) => {
        state.items = action.payload.items || [];
      })
      .addCase(addToCartThunk.fulfilled, (state, action) => {
        state.items = action.payload.items || [];
      })
      .addCase(updateQuantityThunk.fulfilled, (state, action) => {
        state.items = action.payload.items || [];
      })
      .addCase(clearCartThunk.fulfilled, (state) => {
        state.items = [];
      })
      .addCase(syncCartThunk.fulfilled, (state, action) => {
        state.items = action.payload.items || [];
      })
      .addCase(applyCouponThunk.fulfilled, (state, action) => {
        state.appliedCoupon = action.payload;
      });
  },
});

export const {
  setCartData,
  addToCart,
  removeFromCart,
  increaseQuantity,
  decreaseQuantity,
  applyCoupon,
  setAppliedCoupon,
  removeCoupon,
  clearCart,
} = cartSlice.actions;

// Selectors
export const selectCartItems = (state) => state.cart.items;
export const selectAppliedCoupon = (state) => state.cart.appliedCoupon;

export const selectCartTotalCount = (state) =>
  state.cart.items.reduce((total, item) => total + item.quantity, 0);

export const selectCartSubtotal = (state) =>
  state.cart.items.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0);

export const selectCartDiscount = (state) => {
  const subtotal = selectCartSubtotal(state);
  const coupon = state.cart.appliedCoupon;
  if (!coupon || subtotal === 0) return 0;

  if (coupon.type === 'percentage' || coupon.discountType === 'PERCENTAGE') {
    const val = coupon.value || coupon.discountValue || 0;
    return (subtotal * val) / 100;
  }
  if (coupon.type === 'fixed' || coupon.discountType === 'FIXED') {
    const val = coupon.value || coupon.discountValue || 0;
    return Math.min(subtotal, val);
  }
  return 0;
};

export const selectCartShippingFee = (state) => {
  const subtotal = selectCartSubtotal(state);
  if (subtotal === 0 || subtotal >= 200) return 0;
  return 15.0;
};

export const selectCartGrandTotal = (state) => {
  const subtotal = selectCartSubtotal(state);
  if (subtotal === 0) return 0;
  const discount = selectCartDiscount(state);
  const shipping = selectCartShippingFee(state);
  return Math.max(0, subtotal - discount + shipping);
};

export default cartSlice.reducer;
