# Backend Milestone 4F — Report Export & Admin Reporting

**Project:** ShopStack Spring Boot Backend
**Milestone:** 4F — Report Export & Admin Reporting
**Stack:** Spring Boot 3.3.x · Spring Data JPA · PostgreSQL · Spring Security (JWT) · Apache POI · OpenCSV
**Build status:** `BUILD SUCCESS` (37 tests, 0 failures)

---

## 1. Overview

This milestone implements a complete **Report Export & Admin Reporting** module for the ShopStack
e-commerce backend. It adds seven report domains (sales, vendors, products, customers, payments,
coupons, inventory), three export formats (JSON, CSV, Excel `.xlsx`), and role-based access control
that restricts which reports each role may read and export.

The module follows the existing **Controller → Service → Repository → Entity → DTO → Mapper**
architecture. No completed milestone was modified for integration.

---

## 2. Database Usage

### 2.1 Tables involved

The reporting module is read-only against the existing domain schema. It introduces **no new
tables**; it aggregates over the tables below.

| Table | Purpose in reports | Key columns used |
|-------|--------------------|------------------|
| `users` | Customer counts, new customers, top spenders | `id`, `role`, `created_at` |
| `vendors` | Vendor summaries, vendor self-report scoping | `id`, `user_id`, `name` |
| `categories` | Product/inventory filtering | `id`, `name` |
| `products` | Best sellers, low/out-of-stock, stock valuation | `id`, `vendor_id`, `category_id`, `price`, `stock_quantity`, `low_stock_threshold` |
| `orders` | Sales revenue, order counts, cancelled/refunded, AOV, breakdown | `id`, `user_id`, `vendor_id`, `order_date`, `status`, `total_amount` |
| `order_items` | Best-selling products, product revenue | `product_id`, `product_name`, `quantity`, `subtotal` |
| `payments` | Payment status stats, payment-method statistics | `status`, `payment_method`, `amount`, `payment_date` |
| `coupons` | Coupon usage, total discount, most-used coupons | `id`, `code`, `usage_count`, `total_discount`, `discount_percentage` |

### 2.2 Aggregation strategy

Two aggregation patterns are used:

1. **JPQL scalar aggregates** for single-value metrics (revenue, counts, AOV, stock value):
   `SELECT COALESCE(SUM(...), 0) ...`. `COALESCE` guarantees a non-null zero instead of null on
   empty windows, so DTOs never carry null numerics.
2. **Native SQL with interface projections** for grouped metrics (revenue breakdown by day/week/
   month/year, payment-method stats, vendor summaries, customer summaries, coupon usage). Native
   SQL is used because the period breakdown uses PostgreSQL `to_char(order_date, :fmt)` with a
   runtime format string that cannot be expressed in JPQL.

### 2.3 Projection interfaces

Grouped queries return Spring Data projection interfaces (one per report shape), located in
`com.shopstack.repository`:

- `RevenueBreakdownProjection` — period label, revenue, order count
- `StatusCountProjection` — order status + count
- `VendorSummaryProjection` — vendor id/name, revenue, orders, products
- `ProductRevenueProjection` — product id/name, revenue, quantity sold
- `CustomerSummaryProjection` — customer id/name/email, total spent, orders
- `PaymentMethodStatProjection` — method, count, amount
- `CouponSummaryProjection` — coupon id/code, usage, discount, percentage
- `InventoryItemProjection` — product id/name, stock, threshold, unit price

These are mapped to DTOs by `ReportMapper` (pure transformation, no I/O).

### 2.4 Date window resolution

`com.shopstack.util.ReportDateResolver` converts a `ReportFilter` (from/to dates) plus an optional
preset (`DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`, `CUSTOM`) into an absolute
`[start, end)` `LocalDateTime` window and a SQL date-format string:

| Preset | Window | `to_char` format |
|--------|--------|------------------|
| DAILY | the given day | `YYYY-MM-DD` |
| WEEKLY | current ISO week (Mon–Sun) | `IYYY-IW` |
| MONTHLY | current calendar month (default) | `YYYY-MM` |
| YEARLY | current calendar year | `YYYY` |
| CUSTOM | explicit from→to | daily if ≤31 days else monthly |

The window is half-open (`end` is exclusive: `toDate + 1 day at 00:00`) so full days are included
without off-by-one errors. A `toDate` earlier than `fromDate` raises a `ReportException` (HTTP 400).

### 2.5 Auditing

`com.shopstack.config.CreatedAtAuditor` is a JPA `@EntityListeners` entity listener that stamps
`createdAt` on first persistence, so seed/test data does not need to set it manually. It silently
skips entities that lack a `createdAt` field.

---

## 3. Architecture

```
HTTP request (Bearer JWT)
        │
        ▼
JwtAuthenticationFilter          ← validates token, builds AuthenticatedUser + ROLE_* authority
        │
        ▼
SecurityFilterChain              ← authorizeHttpRequests: path → role mapping (RBAC)
        │
        ▼
AdminReportController /          ← binds query params → ReportFilter, calls ReportService
VendorReportController /
WarehouseReportController
        │
        ▼
ReportService                    ← resolves date window, scopes vendorId by role,
        │                          calls repositories, maps projections → DTOs
        ├─ OrderRepository            (sales aggregates + breakdown)
        ├─ OrderItemRepository        (product revenue / best sellers)
        ├─ ProductRepository          (inventory classification + stock value)
        ├─ VendorRepository           (vendor summaries, top vendors)
        ├─ PaymentRepository          (payment status + method stats)
        ├─ CouponRepository           (coupon usage + totals)
        └─ UserRepository             (customer totals + top spenders)
        │
        ▼
ReportMapper / ReportTabulator   ← pure transformation: projections → DTOs → tabular rows
        │
        ▼
CsvExporter / ExcelExporter      ← render tabular rows to CSV / .xlsx
        │
        ▼
HTTP response (JSON / CSV / .xlsx with Content-Disposition: attachment)
```

### 3.1 Layer responsibilities

| Layer | Package | Responsibility |
|-------|---------|----------------|
| Controller | `com.shopstack.controller` | HTTP binding, query-param → `ReportFilter`, response wrapping, export header writing |
| Service | `com.shopstack.service` | Date resolution, vendor scoping, repository orchestration, preset parsing |
| Repository | `com.shopstack.repository` | JPQL + native SQL aggregates, projection interfaces |
| Entity | `com.shopstack.entity` | JPA-mapped domain tables + enums (`UserRole`, `OrderStatus`, `PaymentStatus`) |
| DTO | `com.shopstack.dto` | Immutable report response shapes + `ReportFilter` + `ReportType` + `ApiResponse` |
| Mapper | `com.shopstack.mapper` | Projection → DTO mapping, null-safe numeric helpers |
| Util | `com.shopstack.util` | Date resolution, CSV/Excel/tabular rendering |
| Security | `com.shopstack.security` | JWT provider, auth filter, `SecurityHelper` for role/user-id lookup |
| Exception | `com.shopstack.exception` | `ResourceNotFoundException`, `AccessDeniedException`, `ReportException`, global handler |
| Config | `com.shopstack.config` | `SecurityConfig` (RBAC + JWT filter + CORS + 401 entry point), `CreatedAtAuditor` |

### 3.2 Why projections + mappers instead of DTOs directly from queries

Spring Data interface projections keep the query layer thin and the mapping centralized in
`ReportMapper`. This avoids scattering DTO-construction logic across repositories and lets every
report DTO be assembled from a few reusable, null-safe mapping functions. `ReportMapper.nz()`,
`nzLong()`, `nzInt()` guarantee that empty windows produce `0` / `0.00` instead of NPEs.

---

## 4. API Documentation

Base URL: `http://localhost:8080`

All report endpoints require an `Authorization: Bearer <jwt>` header. Common query parameters:

| Param | Type | Notes |
|-------|------|-------|
| `fromDate` | `YYYY-MM-DD` | Start of custom range (inclusive) |
| `toDate` | `YYYY-MM-DD` | End of custom range (inclusive) |
| `preset` | string | `daily` / `weekly` / `monthly` / `yearly` / `custom`; defaults to monthly |
| `vendorId` | long | Admin/warehouse only; vendors are auto-scoped to their own |
| `categoryId` | long | Filters products/inventory |
| `status` | string | Order/payment status filter (e.g. `PAID`, `SUCCESSFUL`) |

### 4.1 Admin report endpoints (`/api/admin/reports/**`) — role: ADMIN

| Method | Path | Returns |
|--------|------|---------|
| GET | `/api/admin/reports/sales` | `ApiResponse<SalesReportDto>` |
| GET | `/api/admin/reports/vendors` | `ApiResponse<VendorReportDto>` |
| GET | `/api/admin/reports/products` | `ApiResponse<ProductReportDto>` |
| GET | `/api/admin/reports/customers` | `ApiResponse<CustomerReportDto>` |
| GET | `/api/admin/reports/payments` | `ApiResponse<PaymentReportDto>` |
| GET | `/api/admin/reports/coupons` | `ApiResponse<CouponReportDto>` |
| GET | `/api/admin/reports/inventory` | `ApiResponse<InventoryReportDto>` |
| GET | `/api/admin/reports/export/csv?type=SALES` | `text/csv` attachment |
| GET | `/api/admin/reports/export/excel?type=INVENTORY` | `.xlsx` attachment |
| GET | `/api/admin/reports/export/json?type=PAYMENTS` | `ApiResponse<Object>` |
| GET | `/api/admin/reports/presets` | Available preset enum values |

### 4.2 Vendor report endpoints (`/api/vendor/reports/**`) — role: VENDOR

| Method | Path | Returns |
|--------|------|---------|
| GET | `/api/vendor/reports` | `ApiResponse<VendorReportDto>` (own only) |
| GET | `/api/vendor/reports/sales` | `ApiResponse<SalesReportDto>` (own only) |
| GET | `/api/vendor/reports/products` | `ApiResponse<ProductReportDto>` (own only) |
| GET | `/api/vendor/reports/inventory` | `ApiResponse<InventoryReportDto>` (own only) |
| GET | `/api/vendor/reports/export/csv?type=SALES` | `text/csv` attachment |
| GET | `/api/vendor/reports/export/excel?type=PRODUCTS` | `.xlsx` attachment |

### 4.3 Warehouse report endpoints (`/api/warehouse/reports/**`) — role: WAREHOUSE

| Method | Path | Returns |
|--------|------|---------|
| GET | `/api/warehouse/reports/inventory` | `ApiResponse<InventoryReportDto>` |
| GET | `/api/warehouse/reports/inventory/export/csv` | `text/csv` attachment |
| GET | `/api/warehouse/reports/inventory/export/excel` | `.xlsx` attachment |

### 4.4 Auth endpoint (`/api/auth/**`) — public

| Method | Path | Body | Returns |
|--------|------|------|---------|
| POST | `/api/auth/login` | `LoginRequest{email,password}` | `ApiResponse<LoginResponse{token,...}>` |

### 4.5 Export `type` parameter

`type` accepts: `SALES`, `VENDORS`, `PRODUCTS`, `CUSTOMERS`, `PAYMENTS`, `COUPONS`, `INVENTORY`
(defined in `com.shopstack.dto.ReportType`).

### 4.6 Response shapes (JSON)

**SalesReportDto**
```json
{
  "period": { "from": "2024-01-01", "to": "2024-01-31" },
  "revenue": 1250.00,
  "ordersCount": 10,
  "cancelledOrders": 2,
  "refundedOrders": 1,
  "averageOrderValue": 125.00,
  "breakdown": [{ "periodLabel": "2024-01", "revenue": 1250.00, "ordersCount": 10 }],
  "statusCounts": [{ "status": "PAID", "count": 7 }]
}
```

**VendorReportDto** — `vendors[]`, `topVendors[]` (each: `vendorId, vendorName, revenue,
ordersCount, productsCount`), plus `totalVendorRevenue`, `totalVendorOrders`, `totalVendorProducts`.

**ProductReportDto** — `bestSellingProducts[]`, `lowStockProducts[]`, `outOfStockProducts[]`,
`productRevenue[]` (`productId, productName, revenue, quantitySold`), `totalProductRevenue`.

**CustomerReportDto** — `totalCustomers`, `newCustomers`, `totalCustomerSpending`,
`averageCustomerSpending`, `topCustomers[]` (`customerId, customerName, email, totalSpent,
ordersCount`).

**PaymentReportDto** — `successfulPayments`, `failedPayments`, `refundedPayments`,
`successfulAmount`, `refundedAmount`, `paymentMethodStats[]` (`paymentMethod, count, amount`).

**CouponReportDto** — `coupons[]` (`couponId, code, usageCount, totalDiscount,
discountPercentage`), `totalDiscount`, `totalUsage`.

**InventoryReportDto** — `totalProducts`, `lowStockCount`, `outOfStockCount`, `stockValue`,
`lowStockItems[]`, `outOfStockItems[]`, `summary[]` (each: `productId, productName, stockQuantity,
lowStockThreshold, unitPrice, stockValue, status` where status ∈ `IN_STOCK|LOW_STOCK|OUT_OF_STOCK`).

Every JSON report is wrapped in `ApiResponse{success, message, data, timestamp}`.

### 4.7 Export file names

| Endpoint | Filename |
|----------|----------|
| admin CSV | `<type>-report.csv` |
| admin Excel | `<type>-report.xlsx` |
| vendor CSV | `vendor-<type>-report.csv` |
| vendor Excel | `vendor-<type>-report.xlsx` |
| warehouse CSV | `inventory-report.csv` |
| warehouse Excel | `inventory-report.xlsx` |

All export responses set `Content-Disposition: attachment; filename="..."`.

---

## 5. Security Rules

### 5.1 RBAC matrix

| Role | Admin reports | Vendor reports | Warehouse reports | Auth |
|------|--------------|----------------|-------------------|------|
| ADMIN | Full access (all 7 + exports) | Denied | Denied | login |
| VENDOR | Denied | Own reports only (sales, products, inventory, overview + exports) | Denied | login |
| CUSTOMER | Denied | Denied | Denied | login |
| WAREHOUSE | Denied | Denied | Inventory only (+ exports) | login |
| (no token) | 401 | 401 | 401 | public |

### 5.2 Enforcement layers

1. **URL-level (`SecurityConfig.authorizeHttpRequests`)** — path prefix → required role:
   - `/api/admin/reports/**` → `hasRole("ADMIN")`
   - `/api/vendor/reports/**` → `hasRole("VENDOR")`
   - `/api/warehouse/reports/**` → `hasRole("WAREHOUSE")`
   - `/api/auth/**` → `permitAll()`
   - everything else → `authenticated()`
   A missing/invalid token reaches a `HttpStatusEntryPoint` returning **401 Unauthorized**.

2. **Data-level (`ReportService.scopedVendorId` / `ensureVendorScoped`)** — when the caller is a
   `VENDOR`, the service ignores any client-supplied `vendorId` and forces it to the vendor record
   linked to the JWT's `uid` (looked up via `VendorRepository.findByUserId`). A vendor with no
   profile row gets `AccessDeniedException` → 403. This prevents a vendor from reading another
   vendor's sales/products/inventory even if they crafted a request with a foreign `vendorId`.

3. **Method-level (`SecurityHelper.requireRole`)** — available for fine-grained checks; the service
   uses `hasRole("VENDOR")` before applying self-scope so a non-vendor hitting a self-report helper
   is rejected.

### 5.3 JWT

`JwtTokenProvider` (jjwt 0.12.x) signs HS256 tokens carrying claims `uid`, `role`, `sub=email`.
`JwtAuthenticationFilter` runs once per request, validates the token, and installs an
`AuthenticatedUser` principal with a single `ROLE_<role>` granted authority. Security is stateless
(`SessionCreationPolicy.STATELESS`); CSRF is disabled; CORS is open with credential support.

---

## 6. Report Generation Flow

1. **Request** arrives with query params (`fromDate`, `toDate`, `preset`, `vendorId`,
   `categoryId`, `status`) and a Bearer token.
2. **Filter** — the controller calls `ReportService.normalizeFilter(...)` to build a `ReportFilter`
   (validation annotations: `@PastOrPresent` on dates).
3. **Date window** — `ReportDateResolver.resolve(filter, preset)` produces a half-open
   `[start, end)` window and a SQL format string; invalid ranges raise `ReportException` (400).
4. **Vendor scoping** — `scopedVendorId(filter)` resolves the effective `vendorId` (own for
   VENDOR, client-supplied otherwise).
5. **Aggregation** — the service calls the relevant repository methods:
   - Sales: revenue/count/AOV (vendor-scoped or global), status counts, period breakdown.
   - Vendors: per-vendor revenue/orders/products + top-N.
   - Products: product revenue from `order_items` + low/out-of-stock from `products`.
   - Customers: role counts, new-customer count, total/average spending, top-N spenders.
   - Payments: status counts + amounts + per-method stats.
   - Coupons: usage rows + totals.
   - Inventory: classify each product (low/out/in) and value `price × stock`.
6. **Mapping** — `ReportMapper` converts projection rows to DTOs with null-safe numerics.
7. **Response** — the controller wraps the DTO in `ApiResponse.success(...)` and returns JSON.

For exports, step 7 is replaced by `ReportTabulator.tabulate(type, dto)` → `CsvExporter` /
`ExcelExporter`, writing the bytes with the appropriate content type and attachment header.

---

## 7. Export Workflow

```
ReportService.tabulate(type, filter, preset)
        │
        ├─ builds the report DTO (same as JSON flow)
        │
        ▼
ReportTabulator.tabulate(type, dto)
        │  ── picks a tabular shape per ReportType
        │  ── returns Tabular{sheetName, headers, rows}
        ▼
CsvExporter.export(headers, rows)        ExcelExporter.export(sheetName, headers, rows)
        │                                       │
        ▼                                       ▼
OpenCSV → String                          Apache POI XSSFWorkbook → byte[]
        │                                       │
        ▼                                       ▼
text/csv; attachment                       .xlsx; attachment
```

- **JSON** (`/export/json`): returns the same DTO as the report endpoint, wrapped in `ApiResponse`.
- **CSV** (`/export/csv`): OpenCSV writer, UTF-8, first row = headers, one row per record.
- **Excel** (`/export/excel`): Apache POI `XSSFWorkbook`, single sheet named per report, bold
  header row, auto-sized columns, numeric cells for numbers.

`ReportTabulator` flattens each report DTO into ordered `headers` + `List<Map<String,?>>` rows.
Each report type has its own tabular shape (e.g. sales → `period, revenue, ordersCount`;
inventory → `productId, productName, stockQuantity, lowStockThreshold, unitPrice, stockValue,
status`).

---

## 8. Postman Testing Guide

### 8.1 Obtain a token

```
POST /api/auth/login
Content-Type: application/json

{ "email": "admin@shopstack.com", "password": "<password>" }
```

Copy `data.token` from the response and use it as `Authorization: Bearer <token>` for all report
calls.

### 8.2 Admin — sales report (monthly default)

```
GET /api/admin/reports/sales?preset=monthly
Authorization: Bearer <admin-token>
```

### 8.3 Admin — sales report (custom range + vendor + status)

```
GET /api/admin/reports/sales?fromDate=2024-01-01&toDate=2024-01-31&vendorId=7&status=PAID
Authorization: Bearer <admin-token>
```

### 8.4 Admin — all other reports

```
GET /api/admin/reports/vendors?preset=monthly
GET /api/admin/reports/products?categoryId=2&preset=monthly
GET /api/admin/reports/customers?fromDate=2024-01-01&toDate=2024-06-30
GET /api/admin/reports/payments?status=SUCCESSFUL
GET /api/admin/reports/coupons
GET /api/admin/reports/inventory?vendorId=7&categoryId=2
Authorization: Bearer <admin-token>
```

### 8.5 Admin — exports

```
GET /api/admin/reports/export/csv?type=SALES&preset=monthly       → downloads sales-report.csv
GET /api/admin/reports/export/excel?type=INVENTORY                → downloads inventory-report.xlsx
GET /api/admin/reports/export/json?type=PAYMENTS&preset=monthly   → JSON body
Authorization: Bearer <admin-token>
```

In Postman, CSV/Excel calls will download a file (send-and-download). Verify the
`Content-Disposition` header and content type.

### 8.6 Vendor — own reports

```
GET /api/vendor/reports?preset=monthly          → own vendor summary
GET /api/vendor/reports/sales?preset=weekly     → own sales
GET /api/vendor/reports/products                → own products (low/out of stock)
GET /api/vendor/reports/inventory               → own inventory
GET /api/vendor/reports/export/csv?type=SALES   → own sales CSV
GET /api/vendor/reports/export/excel?type=PRODUCTS
Authorization: Bearer <vendor-token>
```

Note: a vendor cannot pass `vendorId`; it is auto-scoped to their own store. Supplying a foreign
`vendorId` is ignored.

### 8.7 Warehouse — inventory

```
GET /api/warehouse/reports/inventory?vendorId=7
GET /api/warehouse/reports/inventory/export/csv
GET /api/warehouse/reports/inventory/export/excel
Authorization: Bearer <warehouse-token>
```

### 8.8 Negative tests (RBAC)

| Call | Token | Expected |
|------|-------|----------|
| `/api/admin/reports/sales` | vendor token | 403 |
| `/api/admin/reports/inventory` | customer token | 403 |
| `/api/admin/reports/sales` | (none) | 401 |
| `/api/vendor/reports` | admin token | 403 |
| `/api/vendor/reports` | customer token | 403 |
| `/api/warehouse/reports/inventory` | vendor token | 403 |

### 8.9 Error cases

| Call | Expected | Reason |
|------|----------|--------|
| `fromDate=2024-02-10&toDate=2024-02-01` | 400 | toDate before fromDate |
| `preset=hourly` | 400 | invalid preset |

---

## 9. Test Summary

Run with `mvn clean test` → **BUILD SUCCESS**, **37 tests, 0 failures, 0 errors, 0 skipped**.

### 9.1 Test classes

| Class | Style | Tests | Covers |
|-------|-------|-------|--------|
| `ReportServiceTest` | Mockito unit (`@ExtendWith(MockitoExtension)`) | 17 | All 7 report types, vendor scoping, preset parsing, export tabulation, filter normalization, error paths |
| `AdminReportControllerTest` | MockMvc + `@SpringBootTest` + H2 + `@Transactional` | 14 | All 7 admin JSON endpoints, CSV/Excel/JSON exports, custom range, RBAC (vendor/customer/no-auth denied) |
| `VendorReportControllerTest` | MockMvc + `@SpringBootTest` + H2 + `@Transactional` | 8 | Vendor self reports (sales/products/inventory/overview), vendor CSV/Excel exports, RBAC (admin/customer denied) |

### 9.2 Test approach

- **Unit tests** (`ReportServiceTest`) mock every repository and `SecurityHelper` with Mockito,
  stubbing projection interfaces to assert the service aggregates and maps correctly — including
  vendor self-scoping (forced own `vendorId`, ignored client `vendorId`) and the "no vendor profile"
  denial path. Strict stubbing is used (no lenient), so unnecessary stubs fail the build.
- **Integration tests** (`AdminReportControllerTest`, `VendorReportControllerTest`) boot the full
  Spring context against an in-memory H2 database (PostgreSQL-compatibility mode) seeded per test
  with `@BeforeEach` + `@Transactional` rollback. Tokens are generated through the real
  `JwtTokenProvider` and bound to the actual seeded user id, so the `JwtAuthenticationFilter`
  authenticates end-to-end. MockMvc assertions check status, JSON paths, content types, and
  `Content-Disposition` headers.

### 9.3 Per-class results

```
AdminReportControllerTest$AccessControl   3  (vendor forbidden, customer forbidden, no-auth 401)
AdminReportControllerTest$Export          3  (csv, excel, json)
AdminReportControllerTest$Json            8  (sales, vendors, products, customers, payments, coupons, inventory, custom range)
VendorReportControllerTest$AccessControl  2  (admin forbidden, customer forbidden)
VendorReportControllerTest$Exports        2  (csv, excel)
VendorReportControllerTest$Self           4  (overview, sales, products, inventory)
ReportServiceTest$Sales                   2
ReportServiceTest$Vendor                  3
ReportServiceTest$Product                 1
ReportServiceTest$Customer                1
ReportServiceTest$Payment                 1
ReportServiceTest$Coupon                  1
ReportServiceTest$Inventory               1
ReportServiceTest$Export                  1
ReportServiceTest$Preset                  4  (null, valid x2, invalid)
ReportServiceTest (normalizeFilter)       1
                                   Total: 37
```

---

## 10. File Inventory

```
pom.xml
src/main/java/com/shopstack/
  ShopStackApplication.java
  config/      SecurityConfig.java, CreatedAtAuditor.java
  security/    SecurityHelper.java
  security/jwt JwtTokenProvider.java, JwtAuthenticationFilter.java, AuthenticatedUser.java
  controller/  AuthController.java, AdminReportController.java,
               VendorReportController.java, WarehouseReportController.java
  service/     ReportService.java
  repository/  (8 repositories + 8 projection interfaces)
  entity/      User, Vendor, Category, Product, Order, OrderItem, Payment, Coupon,
               UserRole, OrderStatus, PaymentStatus
  dto/         ReportFilter, ApiResponse, ReportType,
               SalesReportDto, VendorReportDto, ProductReportDto, CustomerReportDto,
               PaymentReportDto, CouponReportDto, InventoryReportDto,
               LoginRequest, LoginResponse
  mapper/      ReportMapper.java
  util/        ReportDateResolver.java, CsvExporter.java, ExcelExporter.java, ReportTabulator.java
  exception/   ResourceNotFoundException, AccessDeniedException, ReportException, GlobalExceptionHandler
src/main/resources/application.yml
src/test/java/com/shopstack/
  support/TestAuthTokens.java
  service/ReportServiceTest.java
  controller/AdminReportControllerTest.java, VendorReportControllerTest.java
src/test/resources/application.yml
```
