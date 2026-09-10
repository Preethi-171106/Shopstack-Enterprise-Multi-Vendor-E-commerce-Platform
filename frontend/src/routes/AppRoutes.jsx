import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import Home from '../pages/Home';
import Products from '../pages/Products';
import ProductDetails from '../pages/ProductDetails';
import Cart from '../pages/Cart';
import Wishlist from '../pages/Wishlist';
import Checkout from '../pages/Checkout';
import OrderConfirmation from '../pages/OrderConfirmation';
import Login from '../pages/Login';
import Register from '../pages/Register';
import WarehouseStaffRegister from '../pages/WarehouseStaffRegister';
import ForgotPassword from '../pages/ForgotPassword';
import ResetPassword from '../pages/ResetPassword';
import Profile from '../pages/Profile';
import CustomerDashboard from '../pages/dashboard/CustomerDashboard';
import VendorDashboard from '../pages/dashboard/VendorDashboard';
import AdminDashboard from '../pages/dashboard/AdminDashboard';
import WarehouseDashboard from '../pages/dashboard/WarehouseDashboard';
import NotificationsPage from '../pages/NotificationsPage';
import ProtectedRoute from '../components/common/ProtectedRoute';
import NotFound from '../pages/NotFound';

/**
 * ShopStack Route Configuration with Protected Routes & Role Dashboards
 */
const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="products" element={<Products />} />
        <Route path="products/:id" element={<ProductDetails />} />
        <Route path="cart" element={<Cart />} />
        <Route path="wishlist" element={<Wishlist />} />
        <Route path="checkout" element={<Checkout />} />
        <Route path="order-confirmation" element={<OrderConfirmation />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="register/warehouse-staff" element={<WarehouseStaffRegister />} />
        <Route path="forgot-password" element={<ForgotPassword />} />
        <Route path="reset-password" element={<ResetPassword />} />

        {/* Protected Notifications Route */}
        <Route
          path="notifications"
          element={
            <ProtectedRoute>
              <NotificationsPage />
            </ProtectedRoute>
          }
        />

        {/* Protected Customer Routes */}
        <Route
          path="profile"
          element={
            <ProtectedRoute>
              <Profile />
            </ProtectedRoute>
          }
        />
        <Route
          path="dashboard/customer"
          element={
            <ProtectedRoute allowedRoles={['CUSTOMER', 'ADMIN']}>
              <CustomerDashboard />
            </ProtectedRoute>
          }
        />

        {/* Protected Vendor Dashboard */}
        <Route
          path="dashboard/vendor"
          element={
            <ProtectedRoute allowedRoles={['VENDOR', 'ADMIN']}>
              <VendorDashboard />
            </ProtectedRoute>
          }
        />

        {/* Protected Admin Dashboard */}
        <Route
          path="dashboard/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />

        {/* Protected Warehouse Dashboard */}
        <Route
          path="dashboard/warehouse"
          element={
            <ProtectedRoute allowedRoles={['WAREHOUSE_STAFF', 'ADMIN']}>
              <WarehouseDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="warehouse"
          element={
            <ProtectedRoute allowedRoles={['WAREHOUSE_STAFF', 'ADMIN']}>
              <WarehouseDashboard />
            </ProtectedRoute>
          }
        />

        {/* Catch-all 404 */}
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
};

export default AppRoutes;
