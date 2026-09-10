# ShopStack Frontend Documentation

**Project:** ShopStack Enterprise Multi-Vendor E-Commerce Platform — React Frontend
**Stack:** React 18 · Vite 5 · React Router 6 · Tailwind CSS 3 · Axios · Supabase JS · Recharts · Lucide Icons
**Build:** `npm run build` → success (production bundle in `dist/`)

---

## 1. Overview

The ShopStack frontend is a complete multi-vendor e-commerce single-page application built with
React and Vite. It connects to a Supabase backend (PostgreSQL + Auth) for all e-commerce data
and to the ShopStack Spring Boot backend for admin report/analytics APIs. There is no mock data —
every screen is wired to live API calls.

### Roles supported
- **Customer** — browse, search, wishlist, cart, checkout, orders, payments, shipments, reviews, profile, notifications.
- **Vendor** — dashboard, products CRUD, inventory management, orders.
- **Admin** — dashboard, users, vendors, products, coupons, reports, analytics.
- **Warehouse** — inventory reports (via backend API; storefront shows role-aware routing).

---

## 2. Tech Stack & Dependencies

| Dependency | Purpose |
|-----------|---------|
| `react`, `react-dom` | UI framework |
| `react-router-dom` | Client-side routing |
| `@supabase/supabase-js` | Auth + data layer (PostgreSQL) |
| `axios` | Spring Boot backend HTTP client (reports) |
| `tailwindcss`, `postcss`, `autoprefixer` | Styling |
| `recharts` | Admin dashboard & analytics charts |
| `lucide-react` | Icon set |
| `vite`, `@vitejs/plugin-react` | Build tooling |

---

## 3. Project Structure

```
project/
├── index.html                  # HTML entry
├── package.json
├── vite.config.js              # Vite config + manual chunks
├── tailwind.config.js          # Tailwind theme (brand color system)
├── postcss.config.js
├── public/favicon.svg
├── src/
│   ├── main.jsx                # App bootstrap + providers
│   ├── App.jsx                 # Routes (protected + role-based)
│   ├── index.css               # Tailwind layers + component classes
│   ├── lib/
│   │   ├── supabase.js         # Supabase client singleton
│   │   ├── backend.js          # Axios instance (Spring Boot reports)
│   │   ├── api.js              # All Supabase data operations
│   │   └── adminApi.js         # Admin edge-function calls
│   ├── context/
│   │   ├── AuthContext.jsx     # JWT session, profile, role
│   │   ├── CartContext.jsx     # Cart state + actions
│   │   ├── WishlistContext.jsx # Wishlist state + actions
│   │   └── ToastContext.jsx    # Toast notifications
│   ├── hooks/
│   │   └── useFetch.js         # Data-fetch hook (loading/error/refetch)
│   ├── components/
│   │   ├── Navbar.jsx          # Top nav with search, cart, wishlist badges
│   │   ├── Layout.jsx          # Storefront / Vendor / Admin layouts
│   │   ├── ProtectedRoute.jsx  # Auth + role guard
│   │   ├── ProductCard.jsx     # Reusable product card
│   │   ├── Rating.jsx          # Star rating display/input
│   │   ├── Loader.jsx          # Spinner / page / button loaders
│   │   ├── States.jsx          # Error & empty states
│   │   └── DataTable.jsx       # Table + StatCard
│   └── pages/
│       ├── HomePage.jsx
│       ├── CategoriesPage.jsx
│       ├── ProductListingPage.jsx
│       ├── ProductDetailsPage.jsx
│       ├── CartPage.jsx
│       ├── CheckoutPage.jsx
│       ├── OrdersPage.jsx
│       ├── OrderDetailsPage.jsx
│       ├── WishlistPage.jsx
│       ├── ProfilePage.jsx
│       ├── NotificationsPage.jsx
│       ├── PaymentsPage.jsx
│       ├── ShipmentTrackingPage.jsx
│       ├── LoginPage.jsx
│       ├── RegisterPage.jsx
│       ├── NotFoundPage.jsx
│       ├── vendor/
│       │   ├── VendorDashboardPage.jsx
│       │   ├── VendorProductsPage.jsx
│       │   ├── VendorInventoryPage.jsx
│       │   └── VendorOrdersPage.jsx
│       └── admin/
│           ├── AdminDashboardPage.jsx
│           ├── AdminUsersPage.jsx
│           ├── AdminVendorsPage.jsx
│           ├── AdminProductsPage.jsx
│           ├── AdminCouponsPage.jsx
│           ├── AdminReportsPage.jsx
│           └── AdminAnalyticsPage.jsx
├── supabase/functions/
│   └── admin-data/index.ts     # Edge function (admin data access)
└── docs/                       # Documentation
```

---

## 4. Authentication & JWT

### Flow
1. **Register** (`/register`) → `supabase.auth.signUp()` with role in user metadata. A `profiles`
   row is upserted with the chosen role (`CUSTOMER` or `VENDOR`).
2. **Login** (`/login`) → `supabase.auth.signInWithPassword()`. On success, `AuthContext` loads
   the profile and stores the session.
3. **Session persistence** — Supabase persists the JWT in localStorage and auto-refreshes.
4. **Logout** → `supabase.auth.signOut()` clears session + profile state.

### AuthContext API
```
useAuth() → {
  user, profile, loading, isAuthenticated, role,
  login(email, password), register({email,password,fullName,role}),
  logout(), refreshProfile()
}
```

### Protected routes
`<ProtectedRoute roles={['ADMIN']}>` checks authentication and role. Unauthenticated users are
redirected to `/login` (preserving the intended destination). Wrong-role users are redirected to `/`.

---

## 5. Data Layer

### Supabase (primary)
`src/lib/api.js` exports typed async functions for every entity:
- Auth: `signUp`, `signIn`, `signOut`, `getSession`, `getProfile`, `upsertProfile`
- Categories: `listCategories`, `createCategory`, `getCategoryBySlug`
- Products: `listProducts`, `getProduct`, `listVendorProducts`, `createProduct`, `updateProduct`, `deleteProduct`
- Vendors: `listVendors`, `getVendorByUserId`, `createVendor`, `updateVendor`
- Cart: `getCart`, `ensureCart`, `addCartItem`, `updateCartItem`, `deleteCartItem`, `clearCart`
- Wishlist: `getWishlist`, `addWishlistItem`, `removeWishlistItem`
- Orders: `listOrders`, `getOrder`, `createOrder`, `listVendorOrders`
- Payments: `listPayments`, `createPayment`
- Shipments: `getShipmentByOrder`
- Addresses: `listAddresses`, `createAddress`
- Reviews: `listReviews`, `createReview`
- Coupons: `listCoupons`, `createCoupon`, `updateCoupon`, `deleteCoupon`
- Notifications: `listNotifications`, `markNotificationRead`, `createNotification`

### Spring Boot backend (reports)
`src/lib/backend.js` is an axios instance pointing at `VITE_BACKEND_API_URL` (default
`http://localhost:8080`). It attaches the backend JWT from `localStorage` and is used by
`AdminReportsPage` to call `/api/admin/reports/*` and export endpoints. When the backend is
unreachable, the reports page falls back to a live-data summary.

### Admin edge function
`src/lib/adminApi.js` calls the `admin-data` edge function (service-role, admin-verified) for
`/users`, `/vendors`, `/products`, and `/analytics` aggregates that the anon-key client cannot
read due to RLS.

---

## 6. Database Schema

Supabase PostgreSQL with RLS enabled on every table. See the migration
`shopstack_ecommerce_schema` for full DDL.

| Table | Owner scoping | Notes |
|-------|---------------|-------|
| `profiles` | own row | 1:1 with `auth.users`, holds `role` |
| `vendors` | owner (vendor user) | store record |
| `categories` | public read | admin manages |
| `products` | public read; vendor writes | linked to vendor + category |
| `carts` / `cart_items` | customer owns | |
| `orders` / `order_items` | customer owns | |
| `payments` | customer owns | |
| `shipments` | customer owns | tracking info |
| `coupons` | public read; admin manages | |
| `wishlist_items` | customer owns | unique (user, product) |
| `addresses` | customer owns | |
| `reviews` | public read; author writes | |
| `notifications` | user owns | |

RLS policies use `auth.uid()` ownership checks. Public catalog tables (`categories`, `products`,
`coupons`, `vendors`, `reviews`) allow `anon, authenticated` SELECT so browsing works without
login.

---

## 7. Routing

| Path | Component | Access |
|------|-----------|--------|
| `/` | HomePage (or dashboard redirect by role) | Public |
| `/categories` | CategoriesPage | Public |
| `/products` | ProductListingPage (search + filters) | Public |
| `/products/:id` | ProductDetailsPage (reviews) | Public |
| `/login` | LoginPage | Public |
| `/register` | RegisterPage | Public |
| `/cart` | CartPage | Public (auth-gated content) |
| `/checkout` | CheckoutPage | Authenticated |
| `/orders` | OrdersPage | Authenticated |
| `/orders/:id` | OrderDetailsPage | Authenticated |
| `/wishlist` | WishlistPage | Authenticated |
| `/profile` | ProfilePage | Authenticated |
| `/notifications` | NotificationsPage | Authenticated |
| `/payments` | PaymentsPage | Authenticated |
| `/shipments/:orderId` | ShipmentTrackingPage | Authenticated |
| `/vendor` | VendorDashboardPage | VENDOR |
| `/vendor/products` | VendorProductsPage | VENDOR |
| `/vendor/inventory` | VendorInventoryPage | VENDOR |
| `/vendor/orders` | VendorOrdersPage | VENDOR |
| `/admin` | AdminDashboardPage | ADMIN |
| `/admin/users` | AdminUsersPage | ADMIN |
| `/admin/vendors` | AdminVendorsPage | ADMIN |
| `/admin/products` | AdminProductsPage | ADMIN |
| `/admin/coupons` | AdminCouponsPage | ADMIN |
| `/admin/reports` | AdminReportsPage | ADMIN |
| `/admin/analytics` | AdminAnalyticsPage | ADMIN |
| `*` | NotFoundPage | Public |

Admin pages are lazy-loaded (`React.lazy` + `Suspense`) so the Recharts bundle only downloads
for admin users.

---

## 8. State Management

- **AuthContext** — session, profile, role, login/register/logout. Wraps the whole app.
- **CartContext** — cart + items, addItem/updateItem/removeItem/clear, derived itemCount + total.
  Auto-loads on login.
- **WishlistContext** — wishlist items, add/remove/toggle/has, count. Auto-loads on login.
- **ToastContext** — global toast notifications (success/error/info).
- **useFetch hook** — standard loading/error/data/refetch pattern used by list/detail pages.

Providers wrap in `main.jsx`: `ToastProvider > AuthProvider > CartProvider > WishlistProvider > App`.

---

## 9. UI & Design

- **Tailwind CSS** with a custom `brand` blue color ramp + `accent` teal, Inter font, 8px spacing
  system, custom component classes (`.btn-primary`, `.card`, `.badge-*`, `.input`, `.nav-link`).
- **Responsive** — mobile-first grids, collapsible mobile nav, horizontal-scroll dashboard nav.
- **Loading states** — `PageLoader`, `InlineLoader`, `ButtonLoader` across all async screens.
- **Error states** — `ErrorState` with retry on data-fetch failures.
- **Empty states** — `EmptyState` with icon + call-to-action on empty lists.
- **Toast notifications** — bottom-right, auto-dismiss, color-coded by type.
- **Animations** — fade-in on toasts, hover transitions on cards/buttons.

---

## 10. Forms & Validation

- **Login** — email format + password length ≥ 6, inline error messages.
- **Register** — full name required, email format, password ≥ 6, role selection.
- **Checkout** — address selection required, new-address form, payment method, coupon code.
- **Product CRUD** (vendor) — name, price, stock required; category select; image URL.
- **Coupon CRUD** (admin) — code, discount %, max discount, active toggle.
- **Review** — star rating input + comment.
- All forms show button-loading state during submission and toast feedback on success/error.

---

## 11. Build & Run

```bash
npm install        # install dependencies
npm run dev        # start dev server (http://localhost:5173)
npm run build      # production build → dist/
npm run preview    # preview the production build
```

### Environment variables (`.env`)
```
VITE_SUPABASE_URL=...        # Supabase project URL
VITE_SUPABASE_ANON_KEY=...   # Supabase anon key
VITE_BACKEND_API_URL=...     # optional: Spring Boot backend URL for reports
```

---

## 12. Edge Function

`supabase/functions/admin-data` (Deno) provides admin-only data access using the service role key.
It verifies the caller's JWT and profile role is `ADMIN` before serving:

| Route | Method | Returns |
|-------|--------|---------|
| `/admin-data/users` | GET | all profiles |
| `/admin-data/vendors` | GET | all vendors with owner email |
| `/admin-data/products` | GET | all products with vendor/category |
| `/admin-data/analytics` | GET | counts, revenue, revenue series, status counts |

CORS headers are set on every response (preflight, success, error).

---

## 13. Page Inventory

### Customer
| Page | Features |
|------|----------|
| Home | Hero, trust badges, category grid, featured products |
| Categories | Category cards linking to filtered product lists |
| Product Listing | Sidebar filters (category, sort, price range, in-stock), search, grid |
| Product Details | Image, price, stock status, qty selector, add to cart/buy now/wishlist, reviews + review form |
| Cart | Line items, qty update, remove, order summary (subtotal/shipping/tax/total) |
| Checkout | Address selection + new address, payment method, coupon, place order → creates order + payment + notification |
| Orders | Order history with status badges |
| Order Details | Line items, shipment info, address, payment status, track link |
| Wishlist | Saved products grid |
| Profile | Edit name/phone/avatar, view saved addresses, quick links |
| Notifications | List with unread indicator, mark-read |
| Payments | Payment history with status + amount |
| Shipment Tracking | Carrier, tracking #, 4-step progress timeline |

### Vendor
| Page | Features |
|------|----------|
| Dashboard | Store card, stat cards (revenue/orders/products/low stock), recent orders, stock alerts |
| Products | Product table + add/edit modal form + delete |
| Inventory | Stock value, low/out counts, inline stock quantity editing |
| Orders | Vendor's order table with customer + status |

### Admin
| Page | Features |
|------|----------|
| Dashboard | Stat cards, revenue area chart, order-status pie chart |
| Users | All users table with role badges |
| Vendors | All vendors table with owner email + status |
| Products | All products table with vendor/category/stock status |
| Coupons | Coupon table + add/edit modal + delete |
| Reports | Report type + preset selector, generate, CSV/Excel export, JSON output |
| Analytics | Revenue trend, platform bar chart, order-status pie chart |

---

## 14. Notes

- No mock data: every page fetches from Supabase or the Spring backend.
- The Spring Boot report backend (`/api/admin/reports/*`) is contacted via axios with a graceful
  fallback when it is not running in this environment.
- Admin pages are code-split to keep the initial customer bundle smaller.
- RLS enforces data isolation; the admin edge function uses the service role for cross-user reads.
