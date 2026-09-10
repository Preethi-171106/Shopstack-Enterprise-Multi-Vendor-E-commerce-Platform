import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  appName: 'ShopStack',
  tagline: 'Enterprise Multi-Vendor E-Commerce Platform',
  systemStatus: 'Online - Milestone 1 Initialized',
  vendorCountPlaceholder: 0,
  productCountPlaceholder: 0,
};

const appSlice = createSlice({
  name: 'app',
  initialState,
  reducers: {
    setSystemStatus: (state, action) => {
      state.systemStatus = action.payload;
    },
  },
});

export const { setSystemStatus } = appSlice.actions;
export default appSlice.reducer;
