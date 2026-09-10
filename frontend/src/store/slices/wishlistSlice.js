import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import wishlistService from '../../services/wishlistService';

export const fetchWishlistThunk = createAsyncThunk(
  'wishlist/fetchWishlist',
  async (_, { rejectWithValue }) => {
    try {
      const items = await wishlistService.getWishlist();
      return items;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

export const toggleWishlistThunk = createAsyncThunk(
  'wishlist/toggleWishlistBackend',
  async (product, { getState, rejectWithValue }) => {
    try {
      const { wishlist } = getState();
      const exists = wishlist.items.some((item) => String(item.id) === String(product.id));
      if (exists) {
        await wishlistService.removeFromWishlist(product.id);
      } else {
        await wishlistService.addToWishlist(product.id);
      }
      const updated = await wishlistService.getWishlist();
      return updated;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

export const removeFromWishlistThunk = createAsyncThunk(
  'wishlist/removeFromWishlistBackend',
  async (productId, { rejectWithValue, dispatch }) => {
    try {
      await wishlistService.removeFromWishlist(productId);
      const updated = await wishlistService.getWishlist();
      return updated;
    } catch (error) {
      return rejectWithValue(error.userMessage || error.message);
    }
  }
);

const initialState = {
  items: [], // Array of product objects
  status: 'idle',
  error: null,
};

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState,
  reducers: {
    setWishlistData: (state, action) => {
      state.items = action.payload || [];
    },
    addToWishlist: (state, action) => {
      const product = action.payload;
      const exists = state.items.some((item) => item.id === product.id);
      if (!exists) {
        state.items.push(product);
      }
    },

    removeFromWishlist: (state, action) => {
      const productId = action.payload;
      state.items = state.items.filter((item) => item.id !== productId);
    },

    toggleWishlist: (state, action) => {
      const product = action.payload;
      const index = state.items.findIndex((item) => item.id === product.id);
      if (index >= 0) {
        state.items.splice(index, 1);
      } else {
        state.items.push(product);
      }
    },

    clearWishlist: (state) => {
      state.items = [];
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchWishlistThunk.fulfilled, (state, action) => {
        state.items = action.payload || [];
      })
      .addCase(toggleWishlistThunk.fulfilled, (state, action) => {
        state.items = action.payload || [];
      })
      .addCase(removeFromWishlistThunk.fulfilled, (state, action) => {
        state.items = action.payload || [];
      });
  },
});

export const {
  setWishlistData,
  addToWishlist,
  removeFromWishlist,
  toggleWishlist,
  clearWishlist,
} = wishlistSlice.actions;

// Selectors
export const selectWishlistItems = (state) => state.wishlist.items;
export const selectWishlistCount = (state) => state.wishlist.items.length;

export default wishlistSlice.reducer;
