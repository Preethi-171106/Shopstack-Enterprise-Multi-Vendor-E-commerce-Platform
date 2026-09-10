# Milestone 5: Admin Dashboard and Reporting Modules

**Module:** Task 1 — Admin Dashboard and Reporting  
**Platform:** ShopStack – Enterprise Multi-Vendor E-Commerce Platform  
**Author:** DeepMind Antigravity Pair Programmer  

---

## Overview

The Admin Dashboard and Reporting module is the command center for platform administrators. It provides end-to-end marketplace supervision, vendor verification, revenue and commission auditing, order lifecycle monitoring, operational system diagnostic checks, and real-time business report generation with instant CSV exports.

All data is directly computed from live PostgreSQL tables with strict Role-Based Access Control (`ROLE_ADMIN`).

---

## REST API Endpoints

### 1. Admin Dashboard & Marketplace Analytics
- `GET /api/admin/dashboard` & `GET /api/admin/analytics/dashboard`
  - Returns unified `AdminDashboardStats`: customers, vendors, pending approvals, products, categories, orders breakdown, total gross revenue, platform commission (5%), vendor net payouts, low-stock count, and return requests.
- `GET /api/admin/analytics/trends`
  - Returns `MarketplaceTrendsResponse`: daily and monthly revenue and order trends, order status distribution map, category sales breakdown, and vendor performance leaderboard.

### 2. Vendor Management
- `GET /api/admin/vendors` & `GET /api/admin/users/vendors`
  - Returns all registered vendor profiles and their status.
- `GET /api/admin/vendors/{id}` & `GET /api/admin/users/vendors/{id}/details`
  - Returns `VendorDetailResponse`: contact info, store profile, live gross sales, commission paid, net earnings, total products, and recent catalog items.
- `PATCH /api/admin/vendors/{id}/approve`
  - Approves a vendor store (sets `VendorStatus.APPROVED`).
- `PATCH /api/admin/vendors/{id}/reject`
  - Rejects a vendor store (sets `VendorStatus.REJECTED`).
- `PATCH /api/admin/vendors/{id}/suspend`
  - Suspends a vendor store (sets `VendorStatus.SUSPENDED`).

### 3. Order Monitoring
- `GET /api/admin/orders?status={status}` & `GET /api/orders/admin?status={status}`
  - Lists all orders across the marketplace with optional `OrderStatus` filter (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`).
- `PUT /api/admin/orders/{id}/status`
  - Transitions order lifecycle state with transition validation and customer notifications.

### 4. Commission Management
- `GET /api/admin/commissions`
  - Lists all item-level commission records.
- `GET /api/admin/commissions/summary`
  - Returns `CommissionSummaryResponse`: platform commission total, gross sales processed, vendor net payouts, default rate (5.0%), and per-vendor commission breakdown.
- `GET /api/admin/commissions/total`
  - Returns total platform commission.

### 5. System Health Monitoring
- `GET /api/admin/system/health`
  - Safe operational health endpoint returning live PostgreSQL connectivity check (`isValid(2)`), JVM memory stats (used, free, max), active thread count, and uptime. Zero exposure of database passwords, JWT secrets, or API keys.

### 6. Business Reports & CSV Export
- `GET /api/admin/reports/sales`
- `GET /api/admin/reports/orders`
- `GET /api/admin/reports/vendors`
- `GET /api/admin/reports/commissions`
- `GET /api/admin/reports/products`
- `GET /api/admin/reports/{type}/export` & `GET /api/admin/reports/export?type={type}`
  - Generates and streams standard CSV files (`Content-Type: text/csv`) with timestamped filenames for immediate download.

---

## Security Matrix

| Endpoint Pattern | Unauthenticated | CUSTOMER | VENDOR | WAREHOUSE_STAFF | ADMIN |
|---|---|---|---|---|---|
| `/api/admin/dashboard` | 401 | 403 | 403 | 403 | 200 OK |
| `/api/admin/analytics/**` | 401 | 403 | 403 | 403 | 200 OK |
| `/api/admin/vendors/**` | 401 | 403 | 403 | 403 | 200 OK |
| `/api/admin/orders/**` | 401 | 403 | 403 | 403 | 200 OK |
| `/api/admin/commissions/**` | 401 | 403 | 403 | 403 | 200 OK |
| `/api/admin/system/health` | 401 | 403 | 403 | 403 | 200 OK |
| `/api/admin/reports/**` | 401 | 403 | 403 | 403 | 200 OK |

---

## Verification Results

- **Backend Tests:** 253 / 253 Passed (0 failures, 0 errors)
- **Frontend Build:** 1709 modules built cleanly with Vite (0 errors)
