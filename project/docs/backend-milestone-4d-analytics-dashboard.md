# Backend Milestone 4D — Analytics & Dashboard Management

## 1. Milestone Completion Report

The Analytics & Dashboard Management module provides comprehensive real-time business intelligence for the ShopStack platform. It includes an admin dashboard with 15 platform-wide metrics, a vendor dashboard with 12 vendor-specific metrics, sales analytics (daily/weekly/monthly/yearly), chart-ready JSON for revenue and orders, top lists for products/categories/vendors, and statistics for payments, shipments, and coupons. All queries use JPQL aggregates (COUNT, SUM, GROUP BY) to avoid N+1 problems. Vendor ownership is enforced at both service and controller levels.

---

## 2. Files Created

### Analytics Module

| File | Purpose |
|------|---------|
| `analytics/dto/AdminDashboardResponse.java` | Admin dashboard response (15 metrics) |
| `analytics/dto/VendorDashboardResponse.java` | Vendor dashboard response (12 metrics) |
| `analytics/dto/SalesSummaryResponse.java` | Sales summary with data points |
| `analytics/dto/SalesDataPoint.java` | Single data point for sales charts |
| `analytics/dto/RevenueChartResponse.java` | Revenue chart data |
| `analytics/dto/OrdersChartResponse.java` | Orders chart data |
| `analytics/dto/ChartPoint.java` | Generic chart point (date + value) |
| `analytics/dto/CategoryAnalyticsResponse.java` | Category analytics |
| `analytics/dto/VendorAnalyticsResponse.java` | Vendor analytics |
| `analytics/dto/ProductAnalyticsResponse.java` | Product analytics |
| `analytics/dto/PaymentAnalyticsResponse.java` | Payment method statistics |
| `analytics/dto/InventorySummary.java` | Inventory summary |
| `analytics/dto/CouponUsageResponse.java` | Coupon usage statistics |
| `analytics/dto/OrderStatusResponse.java` | Order status count |
| `analytics/dto/ShipmentStatusResponse.java` | Shipment status count |
| `analytics/dto/DashboardFilterRequest.java` | Date range filter with validation |
| `analytics/service/AnalyticsService.java` | Core business logic with aggregate queries |
| `analytics/controller/AdminAnalyticsController.java` | 14 admin endpoints |
| `analytics/controller/VendorAnalyticsController.java` | 6 vendor endpoints |

### Supporting Entities & Repositories

| File | Purpose |
|------|---------|
| `order/enums/OrderStatus.java` | Order status enum (8 values) |
| `order/entity/Order.java` | Order JPA entity |
| `order/entity/OrderItem.java` | Order item JPA entity |
| `order/repository/OrderRepository.java` | Aggregate queries (COUNT, SUM, GROUP BY) |
| `product/enums/ProductStatus.java` | Product status enum |
| `product/entity/Product.java` | Product JPA entity |
| `product/repository/ProductRepository.java` | Top selling, low stock, out of stock queries |
| `category/entity/Category.java` | Category JPA entity |
| `category/repository/CategoryRepository.java` | Category repository |
| `vendor/entity/Vendor.java` | Vendor JPA entity |
| `vendor/repository/VendorRepository.java` | Vendor repository |
| `payment/enums/PaymentMethod.java` | Payment method enum |
| `payment/enums/PaymentStatus.java` | Payment status enum |
| `payment/entity/Payment.java` | Payment JPA entity |
| `payment/repository/PaymentRepository.java` | Payment aggregate queries |
| `shipment/enums/ShipmentStatus.java` | Shipment status enum |
| `shipment/entity/Shipment.java` | Shipment JPA entity |
| `shipment/repository/ShipmentRepository.java` | Shipment aggregate queries |
| `coupon/enums/CouponStatus.java` | Coupon status enum |
| `coupon/entity/Coupon.java` | Coupon JPA entity |
| `coupon/repository/CouponRepository.java` | Coupon usage statistics query |

### Tests

| File | Tests | Description |
|------|-------|-------------|
| `analytics/service/AnalyticsServiceTest.java` | 25 | Service: dashboard, sales, charts, top lists, stats, inventory, validation |
| `analytics/controller/AdminAnalyticsControllerTest.java` | 16 | Admin controller: 200, 401, 403 |
| `analytics/controller/VendorAnalyticsControllerTest.java` | 9 | Vendor controller: 200, 401, 403, 404, ownership |

---

## 3. Files Modified

| File | Change |
|------|--------|
| `security/SecurityConfig.java` | Added `/api/vendor/**` → `ROLE_VENDOR` authorization rule |
| `common/repository/UserRepository.java` | Added `countByRole()` native query for role-based user counting |
| `walkthrough.md` | Updated with Milestone 4D documentation |

---

## 4. Database Schema

### New Tables

```
categories
├── id (PK), name, description, is_active, created_at, updated_at

vendors
├── id (PK), user_id (FK), store_name, store_description,
│   contact_phone, is_active, created_at, updated_at

products
├── id (PK), name, description, sku, price, stock_quantity,
│   low_stock_threshold, category_id (FK), vendor_id (FK),
│   status, created_at, updated_at

orders
├── id (PK), order_number, customer_id (FK), vendor_id (FK),
│   total_amount, status, created_at

order_items
├── id (PK), order_id (FK), product_id (FK),
│   quantity, unit_price, subtotal

payments
├── id (PK), payment_number, order_id (FK), amount,
│   method, status, created_at

shipments
├── id (PK), tracking_number, order_id (FK), carrier,
│   status, created_at, delivered_at

coupons
├── id (PK), code, description, discount_type, discount_value,
│   min_order_amount, max_uses, used_count, status,
│   valid_from, valid_until, created_at
```

---

## 5. Entity Relationship Diagram

```
┌──────────┐     ┌──────────┐     ┌──────────────┐
│  users   │     │ categories │    │   vendors    │
│ id (PK)  │◄──┐ │ id (PK)   │    │ id (PK)      │
└──────────┘   │ └──────────┘    │ user_id (FK)  │
               │                  └──────────────┘
               │                        │
               │     ┌──────────────┐   │
               ├─────│   products   │───┘
               │     │ id (PK)      │
               │     │ category_id  │───► categories
               │     │ vendor_id    │───► vendors
               │     └──────────────┘
               │           │
               │     ┌──────────────┐     ┌──────────────┐
               ├─────│ order_items  │────►│   orders     │
               │     │ id (PK)      │     │ id (PK)      │
               │     │ order_id     │◄────│ customer_id  │──► users
               │     │ product_id   │     │ vendor_id    │──► vendors
               │     └──────────────┘     │ total_amount  │
               │                          │ status       │
               │                          └──────────────┘
               │                                │
               │     ┌──────────────┐     ┌──────────────┐
               │     │  payments    │     │  shipments   │
               │     │ id (PK)      │     │ id (PK)      │
               │     │ order_id     │◄────│ order_id     │
               │     │ amount       │     │ status       │
               │     │ method       │     └──────────────┘
               │     │ status       │
               │     └──────────────┘
               │
               │     ┌──────────────┐
               └────►│  coupons     │
                     │ id (PK)      │
                     │ code         │
                     │ status       │
                     └──────────────┘
```

---

## 6. Analytics Workflow

### Admin Analytics Flow

1. Admin requests dashboard → service aggregates counts from all repositories
2. Admin requests sales report → service queries orders grouped by date
3. Admin requests chart data → service returns date-value pairs for frontend charts
4. Admin requests top lists → service uses JPQL GROUP BY + ORDER BY + SUM
5. Admin requests statistics → service groups by status/method and returns counts

### Vendor Analytics Flow

1. Vendor requests dashboard → controller resolves vendor ID from JWT user ID
2. Controller calls `vendorRepository.findByUserId()` to get vendor profile
3. Service filters all queries by `vendorId` — only vendor's own data is returned
4. Revenue calculations use vendor-scoped SUM queries with date range filters
5. Top selling products filtered by `order.vendor.id`

---

## 7. Dashboard Explanation

### Admin Dashboard (15 metrics)

| Metric | Source | Query |
|--------|--------|-------|
| Total Customers | `users` table | `COUNT` WHERE role = ROLE_CUSTOMER |
| Total Vendors | `users` table | `COUNT` WHERE role = ROLE_VENDOR |
| Total Products | `products` table | `COUNT` all |
| Total Categories | `categories` table | `COUNT` all |
| Total Orders | `orders` table | `COUNT` all |
| Total Revenue | `orders` table | `SUM(totalAmount)` WHERE status in (DELIVERED, CONFIRMED, SHIPPED) |
| Total Payments | `payments` table | `COUNT` WHERE status = SUCCESS |
| Pending Orders | `orders` table | `COUNT` WHERE status = PENDING |
| Delivered Orders | `orders` table | `COUNT` WHERE status = DELIVERED |
| Cancelled Orders | `orders` table | `COUNT` WHERE status = CANCELLED |
| Returned Orders | `orders` table | `COUNT` WHERE status = RETURNED |
| Total Coupons | `coupons` table | `COUNT` all |
| Active Coupons | `coupons` table | `COUNT` WHERE status = ACTIVE |
| Low Stock Products | `products` table | `COUNT` WHERE stock > 0 AND stock <= threshold |
| Out of Stock Products | `products` table | `COUNT` WHERE stock = 0 |

### Vendor Dashboard (12 metrics)

| Metric | Source | Query |
|--------|--------|-------|
| Store Name | `vendors` table | Vendor profile |
| Total Products | `products` table | `COUNT` WHERE vendor_id = ? |
| Active Products | `products` table | `COUNT` WHERE vendor_id = ? AND status = ACTIVE |
| Total Orders | `orders` table | `COUNT` WHERE vendor_id = ? |
| Pending Orders | `orders` table | `COUNT` WHERE vendor_id = ? AND status = PENDING |
| Delivered Orders | `orders` table | `COUNT` WHERE vendor_id = ? AND status = DELIVERED |
| Revenue | `orders` table | `SUM(totalAmount)` WHERE vendor_id = ? AND status = DELIVERED |
| Monthly Revenue | `orders` table | `SUM` WHERE vendor_id = ? AND created_at in current month |
| Today's Revenue | `orders` table | `SUM` WHERE vendor_id = ? AND created_at = today |
| Top Selling Products | `order_items` | `GROUP BY product ORDER BY SUM(quantity) DESC` |
| Low Stock Products | `products` table | WHERE vendor_id = ? AND stock <= threshold |
| Inventory Summary | `products` table | Total / low stock / out of stock / in stock counts |

---

## 8. API Endpoint Table

### Admin Endpoints (`/api/admin/analytics`)

| Method | Endpoint | Description | Status Codes |
|--------|----------|-------------|--------------|
| GET | `/dashboard` | Admin dashboard summary | 200, 401, 403 |
| GET | `/sales/daily` | Daily sales report | 200, 400, 401, 403 |
| GET | `/sales/weekly` | Weekly sales report | 200, 401, 403 |
| GET | `/sales/monthly` | Monthly sales report | 200, 401, 403 |
| GET | `/sales/yearly` | Yearly sales report | 200, 401, 403 |
| GET | `/charts/revenue` | Revenue chart data | 200, 400, 401, 403 |
| GET | `/charts/orders` | Orders chart data | 200, 400, 401, 403 |
| GET | `/top-products` | Top selling products | 200, 401, 403 |
| GET | `/top-categories` | Top categories | 200, 401, 403 |
| GET | `/top-vendors` | Top vendors | 200, 401, 403 |
| GET | `/payments` | Payment method statistics | 200, 401, 403 |
| GET | `/shipments` | Shipment status statistics | 200, 401, 403 |
| GET | `/inventory` | Inventory summary | 200, 401, 403 |
| GET | `/coupons` | Coupon usage statistics | 200, 401, 403 |

### Vendor Endpoints (`/api/vendor/analytics`)

| Method | Endpoint | Description | Status Codes |
|--------|----------|-------------|--------------|
| GET | `/dashboard` | Vendor dashboard summary | 200, 401, 403, 404 |
| GET | `/sales` | Vendor sales report | 200, 400, 401, 403 |
| GET | `/products` | Vendor's product list | 200, 401, 403 |
| GET | `/orders` | Vendor's order status stats | 200, 401, 403 |
| GET | `/inventory` | Vendor's inventory summary | 200, 401, 403 |
| GET | `/top-products` | Vendor's top selling products | 200, 401, 403 |

---

## 9. Security Rules

| Role | Access |
|------|--------|
| `ROLE_ADMIN` | Full access to `/api/admin/analytics/**` — all platform analytics |
| `ROLE_VENDOR` | Access to `/api/vendor/analytics/**` — only own vendor's data |
| `ROLE_CUSTOMER` | Forbidden (403) on all analytics endpoints |
| `ROLE_WAREHOUSE_STAFF` | Forbidden (403) on all analytics endpoints |
| Unauthenticated | Unauthorized (401) on all analytics endpoints |

### Vendor Ownership Enforcement

- `VendorAnalyticsController.getCurrentVendorId()` resolves the vendor ID from the authenticated user's JWT
- `vendorRepository.findByUserId(userId)` maps user → vendor profile
- If no vendor profile exists → 404 ResourceNotFoundException
- All service queries are scoped by `vendorId` — a vendor cannot see another vendor's data
- `SecurityUtils.hasRole("ROLE_VENDOR")` check prevents non-vendors from accessing vendor endpoints

---

## 10. Repository Aggregate Queries

### OrderRepository

| Method | Query Type | Description |
|--------|-----------|-------------|
| `countByStatus(OrderStatus)` | COUNT | Count orders by status |
| `countByVendorId(Long)` | COUNT | Count orders by vendor |
| `countByVendorIdAndStatus(Long, OrderStatus)` | COUNT | Count by vendor and status |
| `sumTotalRevenue()` | SUM | Total platform revenue |
| `sumRevenueByVendor(Long)` | SUM | Revenue for a vendor |
| `sumRevenueByVendorAndDateRange(Long, Instant, Instant)` | SUM | Vendor revenue in date range |
| `sumRevenueByDateRange(Instant, Instant)` | SUM | Platform revenue in date range |
| `findDailySalesStats(Instant, Instant)` | GROUP BY DATE | Daily order count + revenue |
| `findDailySalesStatsByVendor(Long, Instant, Instant)` | GROUP BY DATE | Vendor daily sales |
| `findMonthlySalesStats(Instant, Instant)` | GROUP BY YYYY-MM | Monthly order count + revenue |
| `countGroupByStatus()` | GROUP BY | Order status distribution |
| `countGroupByStatusByVendor(Long)` | GROUP BY | Vendor order status distribution |

### ProductRepository

| Method | Query Type | Description |
|--------|-----------|-------------|
| `countByStatus(ProductStatus)` | COUNT | Products by status |
| `countByVendorId(Long)` | COUNT | Products by vendor |
| `findOutOfStockProducts()` | WHERE | Products with 0 stock |
| `findLowStockProducts()` | WHERE | Products below threshold |
| `findTopSellingProducts(Pageable)` | GROUP BY + ORDER BY | Top selling products globally |
| `findTopSellingProductsByVendor(Long, Pageable)` | GROUP BY + ORDER BY | Top selling for vendor |

### PaymentRepository

| Method | Query Type | Description |
|--------|-----------|-------------|
| `countByStatus(PaymentStatus)` | COUNT | Payments by status |
| `sumSuccessfulPayments()` | SUM | Total successful payment amount |
| `countSuccessfulGroupByMethod()` | GROUP BY | Payment method distribution |
| `countGroupByStatus()` | GROUP BY | Payment status distribution |

### ShipmentRepository

| Method | Query Type | Description |
|--------|-----------|-------------|
| `countByStatus(ShipmentStatus)` | COUNT | Shipments by status |
| `countGroupByStatus()` | GROUP BY | Shipment status distribution |

### CouponRepository

| Method | Query Type | Description |
|--------|-----------|-------------|
| `countByStatus(CouponStatus)` | COUNT | Coupons by status |
| `findCouponUsageStats()` | ORDER BY | Coupon usage ordered by used count |

---

## 11. Performance Optimizations

1. **Aggregate queries over in-memory filtering** — All counts and sums use JPQL `COUNT()`, `SUM()`, and `GROUP BY` directly in the database, avoiding loading entities into memory.

2. **No N+1 queries** — Dashboard metrics use individual count queries rather than loading all entities and filtering in Java. Each metric is a single database round-trip.

3. **Lazy loading for relationships** — All `@ManyToOne` and `@OneToOne` relationships use `FetchType.LAZY` to avoid unnecessary joins when only aggregate data is needed.

4. **Pageable for top lists** — `findTopSellingProducts(Pageable)` limits results at the database level using `LIMIT` (via PageRequest), not in memory.

5. **Date-range filtering in SQL** — Sales queries filter by `createdAt >= :start AND createdAt < :end` in JPQL, not in Java.

6. **Read-only transactions** — `AnalyticsService` is annotated `@Transactional(readOnly = true)` at class level, allowing Hibernate to skip dirty checking.

7. **Null-safe BigDecimal handling** — `nullSafeSum()` converts null SUM results to `BigDecimal.ZERO`, preventing NPEs when no data exists.

8. **Single round-trip for grouped stats** — `countGroupByStatus()` returns all status counts in one query instead of N separate count queries.

---

## 12. Postman Testing Guide

### Setup

1. **Login as Admin**
   ```
   POST /api/auth/login
   Body: { "email": "admin@shopstack.com", "password": "password" }
   ```
   Save the `token`.

2. **Login as Vendor**
   ```
   POST /api/auth/login
   Body: { "email": "vendor@shopstack.com", "password": "password" }
   ```
   Save the `token`.

### Admin Tests

3. **Get Admin Dashboard**
   ```
   GET /api/admin/analytics/dashboard
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK` with 15 metrics.

4. **Get Daily Sales**
   ```
   GET /api/admin/analytics/sales/daily?startDate=2024-01-01&endDate=2024-01-31
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK` with daily data points.

5. **Get Weekly Sales**
   ```
   GET /api/admin/analytics/sales/weekly
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK`.

6. **Get Monthly Sales**
   ```
   GET /api/admin/analytics/sales/monthly
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK`.

7. **Get Yearly Sales**
   ```
   GET /api/admin/analytics/sales/yearly
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK`.

8. **Get Revenue Chart**
   ```
   GET /api/admin/analytics/charts/revenue?startDate=2024-01-01&endDate=2024-01-31
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK` with chart points.

9. **Get Orders Chart**
   ```
   GET /api/admin/analytics/charts/orders?startDate=2024-01-01&endDate=2024-01-31
   Authorization: Bearer <admin-token>
   ```
   Expected: `200 OK` with chart points.

10. **Get Top Products**
    ```
    GET /api/admin/analytics/top-products?limit=10
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

11. **Get Top Categories**
    ```
    GET /api/admin/analytics/top-categories
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

12. **Get Top Vendors**
    ```
    GET /api/admin/analytics/top-vendors
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

13. **Get Payment Statistics**
    ```
    GET /api/admin/analytics/payments
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

14. **Get Shipment Statistics**
    ```
    GET /api/admin/analytics/shipments
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

15. **Get Inventory Summary**
    ```
    GET /api/admin/analytics/inventory
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

16. **Get Coupon Usage Statistics**
    ```
    GET /api/admin/analytics/coupons
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

### Vendor Tests

17. **Get Vendor Dashboard**
    ```
    GET /api/vendor/analytics/dashboard
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK` with vendor-scoped metrics.

18. **Get Vendor Sales**
    ```
    GET /api/vendor/analytics/sales?startDate=2024-01-01&endDate=2024-01-31
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`.

19. **Get Vendor Products**
    ```
    GET /api/vendor/analytics/products
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`.

20. **Get Vendor Orders**
    ```
    GET /api/vendor/analytics/orders
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`.

21. **Get Vendor Inventory**
    ```
    GET /api/vendor/analytics/inventory
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`.

22. **Get Vendor Top Products**
    ```
    GET /api/vendor/analytics/top-products?limit=5
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`.

### Negative Tests

23. **Customer Accessing Admin Analytics**
    ```
    GET /api/admin/analytics/dashboard
    Authorization: Bearer <customer-token>
    ```
    Expected: `403 Forbidden`.

24. **Customer Accessing Vendor Analytics**
    ```
    GET /api/vendor/analytics/dashboard
    Authorization: Bearer <customer-token>
    ```
    Expected: `403 Forbidden`.

25. **Unauthenticated Access**
    ```
    GET /api/admin/analytics/dashboard
    ```
    Expected: `401 Unauthorized`.

26. **Invalid Date Range**
    ```
    GET /api/admin/analytics/sales/daily?startDate=2024-12-01&endDate=2024-01-01
    Authorization: Bearer <admin-token>
    ```
    Expected: `400 Bad Request`.

---

## 13. Test Summary

```
Tests run: 94
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Test Breakdown

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `AnalyticsServiceTest` | 25 | Dashboard values, revenue calculations, sales reports, vendor ownership, admin permissions, empty dataset, date filters, top lists, payment/shipment/coupon stats, inventory |
| `AdminAnalyticsControllerTest` | 16 | All 14 admin endpoints (200), customer 403, unauthenticated 401 |
| `VendorAnalyticsControllerTest` | 9 | All 6 vendor endpoints (200), vendor not found 404, customer 403, unauthenticated 401 |
| **Previous tests** | 43 | All notification tests continue passing |
| **Total** | **94** | |
