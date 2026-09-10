import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { StorefrontLayout, VendorLayout, AdminLayout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { PageLoader } from './components/Loader';

// Public + customer
import { HomePage } from './pages/HomePage';
import { CategoriesPage } from './pages/CategoriesPage';
import { ProductListingPage } from './pages/ProductListingPage';
import { ProductDetailsPage } from './pages/ProductDetailsPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { OrdersPage } from './pages/OrdersPage';
import { OrderDetailsPage } from './pages/OrderDetailsPage';
import { WishlistPage } from './pages/WishlistPage';
import { ProfilePage } from './pages/ProfilePage';
import { NotificationsPage } from './pages/NotificationsPage';
import { PaymentsPage } from './pages/PaymentsPage';
import { ShipmentTrackingPage } from './pages/ShipmentTrackingPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { NotFoundPage } from './pages/NotFoundPage';

// Vendor
import { VendorDashboardPage } from './pages/vendor/VendorDashboardPage';
import { VendorProductsPage } from './pages/vendor/VendorProductsPage';
import { VendorInventoryPage } from './pages/vendor/VendorInventoryPage';
import { VendorOrdersPage } from './pages/vendor/VendorOrdersPage';

// Admin pages (lazy-loaded so recharts only loads for admin routes)
const AdminDashboardPage = lazy(() =>
  import('./pages/admin/AdminDashboardPage').then((m) => ({ default: m.AdminDashboardPage }))
);
const AdminUsersPage = lazy(() =>
  import('./pages/admin/AdminUsersPage').then((m) => ({ default: m.AdminUsersPage }))
);
const AdminVendorsPage = lazy(() =>
  import('./pages/admin/AdminVendorsPage').then((m) => ({ default: m.AdminVendorsPage }))
);
const AdminProductsPage = lazy(() =>
  import('./pages/admin/AdminProductsPage').then((m) => ({ default: m.AdminProductsPage }))
);
const AdminCouponsPage = lazy(() =>
  import('./pages/admin/AdminCouponsPage').then((m) => ({ default: m.AdminCouponsPage }))
);
const AdminReportsPage = lazy(() =>
  import('./pages/admin/AdminReportsPage').then((m) => ({ default: m.AdminReportsPage }))
);
const AdminAnalyticsPage = lazy(() =>
  import('./pages/admin/AdminAnalyticsPage').then((m) => ({ default: m.AdminAnalyticsPage }))
);

const SuspenseLoader = () => <PageLoader label="Loading page…" />;

function HomeOrDashboard() {
  const { isAuthenticated, role, loading } = useAuth();
  if (loading) return null;
  if (isAuthenticated && role === 'ADMIN') return <Navigate to="/admin" replace />;
  if (isAuthenticated && role === 'VENDOR') return <Navigate to="/vendor" replace />;
  return <HomePage />;
}

export default function App() {
  return (
    <Routes>
      <Route element={<StorefrontLayout />}>
        <Route index element={<HomeOrDashboard />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="products" element={<ProductListingPage />} />
        <Route path="products/:id" element={<ProductDetailsPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />

        <Route path="cart" element={<CartPage />} />
        <Route
          path="checkout"
          element={
            <ProtectedRoute roles={['CUSTOMER', 'ADMIN']}>
              <CheckoutPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="orders/:id"
          element={
            <ProtectedRoute>
              <OrderDetailsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="wishlist"
          element={
            <ProtectedRoute>
              <WishlistPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="notifications"
          element={
            <ProtectedRoute>
              <NotificationsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="payments"
          element={
            <ProtectedRoute>
              <PaymentsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="shipments/:orderId"
          element={
            <ProtectedRoute>
              <ShipmentTrackingPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>

      {/* Vendor */}
      <Route
        path="vendor"
        element={
          <ProtectedRoute roles={['VENDOR']}>
            <VendorLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<VendorDashboardPage />} />
        <Route path="products" element={<VendorProductsPage />} />
        <Route path="inventory" element={<VendorInventoryPage />} />
        <Route path="orders" element={<VendorOrdersPage />} />
      </Route>

      {/* Admin */}
      <Route
        path="admin"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Suspense fallback={<SuspenseLoader />}><AdminDashboardPage /></Suspense>} />
        <Route path="users" element={<Suspense fallback={<SuspenseLoader />}><AdminUsersPage /></Suspense>} />
        <Route path="vendors" element={<Suspense fallback={<SuspenseLoader />}><AdminVendorsPage /></Suspense>} />
        <Route path="products" element={<Suspense fallback={<SuspenseLoader />}><AdminProductsPage /></Suspense>} />
        <Route path="coupons" element={<Suspense fallback={<SuspenseLoader />}><AdminCouponsPage /></Suspense>} />
        <Route path="reports" element={<Suspense fallback={<SuspenseLoader />}><AdminReportsPage /></Suspense>} />
        <Route path="analytics" element={<Suspense fallback={<SuspenseLoader />}><AdminAnalyticsPage /></Suspense>} />
      </Route>
    </Routes>
  );
}
