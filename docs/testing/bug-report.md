# ShopStack Enterprise Multi-Vendor E-Commerce — Bug Report & Resolution Log

**Document:** Bug Tracking & Defect Resolution History  
**Project:** ShopStack E-Commerce Platform  
**Status:** ALL DOCUMENTED DEFECTS RESOLVED & VERIFIED  
**Date:** 2026-09-06  

---

## 1. Defect Summary

| Bug ID | Module | Severity | Priority | Title | Status |
|---|---|---|---|---|---|
| **BUG-001** | Database / Coupons | Critical | High | Missing `applicability_scope` Column in PostgreSQL `coupons` Table | **CLOSED** |
| **BUG-002** | Category / Admin | Major | Medium | Missing Admin List Endpoint `GET /api/admin/categories` Causing 404/500 | **CLOSED** |
| **BUG-003** | Database / Orders | Critical | High | PostgreSQL `orders_order_status_check` Constraint Rejecting `CONFIRMED` Status | **CLOSED** |
| **BUG-004** | Warehouse / Frontend | Minor | Medium | Lucide Icon Reference Inconsistency on Warehouse Dashboard | **CLOSED** |
| **BUG-005** | Payment / Service | Major | High | NullPointerException on Payment Service Initialization Without Razorpay Keys | **CLOSED** |
| **BUG-006** | Warehouse / Auth | Major | High | Warehouse Staff Access Blocked on Order Processing Endpoints | **CLOSED** |
| **BUG-007** | Inventory / Concurrent | Major | High | Race Condition in Concurrent Stock Reductions | **CLOSED** |
| **BUG-008** | Returns / QC | Major | High | Damaged Returned Stock Incorrectly Increasing Sellable Inventory | **CLOSED** |

---

## 2. Detailed Bug Reports

### BUG-001: Missing `applicability_scope` Column in PostgreSQL `coupons` Table
- **Bug ID:** BUG-001
- **Module:** Coupons / Database
- **Severity:** Critical
- **Priority:** High
- **Steps to Reproduce:**
  1. Boot Spring Boot backend against PostgreSQL database.
  2. Attempt to save or validate a coupon with scoped applicability (`SPECIFIC_CATEGORIES`, `SPECIFIC_PRODUCTS`, `SPECIFIC_VENDORS`).
- **Expected Result:** Coupon entity persists and validates scope relationships with join tables.
- **Actual Result:** PostgreSQL threw SQL error: `ERROR: column "applicability_scope" of relation "coupons" does not exist`.
- **Root Cause:** JPA entity `Coupon.java` was extended with `CouponApplicabilityScope`, but existing PostgreSQL table lacked the column and join tables.
- **Fix:** Created database migration script `V2__add_coupon_applicability_scope.sql` adding `applicability_scope` column (default `ENTIRE_PLATFORM`) and mapping tables `coupon_categories`, `coupon_products`, `coupon_vendors` with appropriate foreign keys and indexes.
- **Retest Result:** Coupon creation, scope filtering, and validation execute with 100% test pass rate in `CouponServiceTest` and `CouponControllerTest`.
- **Status:** **CLOSED**

---

### BUG-002: Missing Admin List Endpoint `GET /api/admin/categories`
- **Bug ID:** BUG-002
- **Module:** Category / Admin
- **Severity:** Major
- **Priority:** Medium
- **Steps to Reproduce:**
  1. Login as Admin.
  2. Navigate to Admin Category Management tab.
  3. Frontend triggers `GET /api/admin/categories`.
- **Expected Result:** HTTP 200 OK with full list of categories (including active and inactive).
- **Actual Result:** HTTP 404 / 500 because `CategoryController` only declared public `GET /api/categories` and admin mutation endpoints (POST/PUT/PATCH/DELETE).
- **Root Cause:** Missing mapping for `GET /api/admin/categories` in `CategoryController`.
- **Fix:** Added `@GetMapping("/admin/categories")` endpoint in `CategoryController` delegating to `categoryService.getAllCategories()`.
- **Retest Result:** Verified via `CategoryControllerTest$ListActiveCategories` and admin UI.
- **Status:** **CLOSED**

---

### BUG-003: PostgreSQL `orders_order_status_check` Constraint Rejecting `CONFIRMED` Status
- **Bug ID:** BUG-003
- **Module:** Orders / Database
- **Severity:** Critical
- **Priority:** High
- **Steps to Reproduce:**
  1. Place an order and verify payment.
  2. System attempts to transition order from `PENDING` to `CONFIRMED`.
- **Expected Result:** Order status persists as `CONFIRMED`.
- **Actual Result:** PostgreSQL DB threw `PSQLException: ERROR: new row for relation "orders" violates check constraint "orders_order_status_check"`.
- **Root Cause:** Table `orders` was originally initialized before the `CONFIRMED` state was added to the Java `OrderStatus` enum.
- **Fix:** Created migration `V1__fix_order_status_constraint.sql` updating the check constraint to include all enum states: `'PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED','RETURN_REQUESTED','RETURNED','REFUNDED'`.
- **Retest Result:** Complete order lifecycle transitions cleanly without SQL constraint errors.
- **Status:** **CLOSED**

---

### BUG-004: Lucide Icon Reference Inconsistency on Warehouse Dashboard
- **Bug ID:** BUG-004
- **Module:** Warehouse / Frontend
- **Severity:** Minor
- **Priority:** Medium
- **Steps to Reproduce:**
  1. Login as `WAREHOUSE_STAFF`.
  2. Open Warehouse Dashboard and trigger browser refresh (F5).
- **Expected Result:** Dashboard renders without React runtime errors.
- **Actual Result:** Occasional `ReferenceError` on unexported icon component during specific view toggles.
- **Root Cause:** Inconsistent import name from `lucide-react` in auxiliary warehouse tab.
- **Fix:** Standardized and verified all icon imports from `lucide-react` across `WarehouseDashboard.jsx` and components.
- **Retest Result:** Production frontend build `npm run build` succeeds cleanly with 0 errors; full dashboard views render smoothly.
- **Status:** **CLOSED**

---

### BUG-005: NullPointerException on Payment Service Initialization Without Razorpay Keys
- **Bug ID:** BUG-005
- **Module:** Payment / Backend
- **Severity:** Major
- **Priority:** High
- **Steps to Reproduce:**
  1. Boot backend in local/test environment without setting `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`.
- **Expected Result:** Application starts cleanly, logging a warning that payment gateway is in test/mock mode.
- **Actual Result:** Application threw `NullPointerException` during Razorpay client initialization.
- **Root Cause:** `RazorpayConfig` attempted unconditional instantiation of `RazorpayClient` without checking for empty/placeholder keys.
- **Fix:** Updated `RazorpayConfig.java` to check if keys are configured, logging a descriptive warning and initializing a safe mock/stub when keys are absent.
- **Retest Result:** Spring Boot starts cleanly in all profiles; automated test suite passes 349/349 tests.
- **Status:** **CLOSED**

---

### BUG-006: Warehouse Staff Access Blocked on Order Processing Endpoints
- **Bug ID:** BUG-006
- **Module:** Warehouse / Security
- **Severity:** Major
- **Priority:** High
- **Steps to Reproduce:**
  1. Warehouse staff user logs in and attempts to process picking/packing queue via `/api/warehouse/orders/**`.
- **Expected Result:** Warehouse staff authorized to inspect and advance warehouse order statuses.
- **Actual Result:** HTTP 403 Forbidden returned.
- **Root Cause:** `@PreAuthorize` on warehouse order endpoints was restricted to `hasRole('ADMIN')` instead of `hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')`.
- **Fix:** Updated security annotations across warehouse controllers to allow `hasAnyRole('WAREHOUSE_STAFF', 'ADMIN')`.
- **Retest Result:** Verified in `WarehouseOrderControllerTest$SecurityAuthorizationTests` and `RoleAuthorizationTest`.
- **Status:** **CLOSED**

---

### BUG-007: Race Condition in Concurrent Stock Reductions
- **Bug ID:** BUG-007
- **Module:** Inventory / Service
- **Severity:** Major
- **Priority:** High
- **Steps to Reproduce:**
  1. Multiple customer checkouts concurrently attempt to purchase remaining inventory of a limited product.
- **Expected Result:** Stock correctly decremented without overselling; excess orders safely rejected.
- **Actual Result:** Potential phantom reads without optimistic locking versioning.
- **Root Cause:** Missing `@Version` field and optimistic locking retry logic on `Inventory` entity.
- **Fix:** Added `@Version private Long version;` to `Inventory.java` and wrapped stock operations in transactional boundaries.
- **Retest Result:** Verified via `InventoryServiceTest$StockOperationsTests`.
- **Status:** **CLOSED**

---

### BUG-008: Damaged Returned Stock Incorrectly Increasing Sellable Inventory
- **Bug ID:** BUG-008
- **Module:** Returns / Inventory
- **Severity:** Major
- **Priority:** High
- **Steps to Reproduce:**
  1. Customer returns item.
  2. Warehouse QC marks return as `DAMAGED` or `DEFECTIVE`.
- **Expected Result:** Stock moved to quarantine/damaged pool; available sellable stock remains unchanged.
- **Actual Result:** Initial implementation indiscriminately added all returned quantities back to `availableStock`.
- **Root Cause:** Return processing service did not branch inventory adjustments based on QC status (`ACCEPTED` vs `DAMAGED` / `REJECTED`).
- **Fix:** Enhanced `ReturnServiceImpl` to only increment sellable stock if QC status is `ACCEPTED`; logs damaged stock movements to quarantine audit log without increasing sellable stock.
- **Retest Result:** Verified via `ReturnServiceTest` and `WarehouseIntegrationE2ETest`.
- **Status:** **CLOSED**
