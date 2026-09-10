import { configureStore } from '@reduxjs/toolkit';
import appReducer from './slices/appSlice';
import cartReducer from './slices/cartSlice';
import wishlistReducer from './slices/wishlistSlice';

export const store = configureStore({
  reducer: {
    app: appReducer,
    cart: cartReducer,
    wishlist: wishlistReducer,
  },
});

export default store;
