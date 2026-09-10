# Milestone 4b / Task 2: Coupons & Promotions Management Module

**Module:** Task 2 — Coupons & Promotions Management  
**Platform:** ShopStack – Enterprise Multi-Vendor E-Commerce Platform  
**Architecture:** Spring Boot 3.3.2 (Java 21) · PostgreSQL · Spring Security + JWT · React 18 + Vite 5

---

## 1. Overview

The Coupons & Promotions Module provides a complete enterprise-grade promotion lifecycle:
1. **Admin Promotion Supervision**: Full CRUD, status toggle (activate / deactivate), search & filter (`ALL`, `ACTIVE`, `INACTIVE`, `EXPIRED`), real-time analytics aggregation, and granular order redemption audit history.
2. **Discount Type Flexibility**:
   - `PERCENTAGE` (e.g. `SAVE10` for 10% discount, optionally capped with maximum discount limit).
   - `FIXED_AMOUNT` (e.g. `FLAT500` for flat ₹500 discount, capped to order total).
   - Backward-compatibility preserved for `FLAT`.
3. **Multi-Constraint Validation Engine**:
   - Active status check (`active == true`).
   - Validity schedule window (`startDate <= now <= expiryDate`).
   - Platform usage limits (`usedCount < usageLimit`).
   - Customer-level frequency limits (`userUsage < perUserLimit`).
   - Minimum spend requirements (`subtotal >= minimumOrderAmount`).
4. **Checkout Integration & Server-Side Security**:
   - Authoritative discount calculation performed on backend (`POST /api/coupons/validate`).
   - Cart subtotal, shipping fee, discount deduction, and total payable rendered with instant reactive feedback.
5. **Usage Tracking & Audit**:
   - Every placed order with a coupon creates an immutable `CouponUsage` record (`coupon_id`, `user_id`, `order_id`, `discount_amount`, `used_at`).
   - Campaign `usedCount` is atomically incremented on order placement.

---

## 2. Database Design

### `coupons` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Unique coupon identifier |
| `code` | `VARCHAR(50)` | `UNIQUE, NOT NULL` | Uppercase coupon code (e.g. `SAVE10`) |
| `name` | `VARCHAR(100)` | `NOT NULL` | Campaign title |
| `description` | `VARCHAR(500)` | `NULL` | Campaign description |
| `discount_type` | `VARCHAR(50)` | `NOT NULL` | `PERCENTAGE` or `FIXED_AMOUNT` |
| `discount_value` | `NUMERIC(10,2)` | `NOT NULL` | Percentage rate or flat amount |
| `applicability_scope` | `VARCHAR(50)` | `NOT NULL, DEFAULT 'ENTIRE_PLATFORM'` | `ENTIRE_PLATFORM`, `SPECIFIC_CATEGORIES`, `SPECIFIC_PRODUCTS`, or `SPECIFIC_VENDORS` (aliases `PLATFORM`, `CATEGORY`, `PRODUCT`, `VENDOR` supported) |
| `minimum_order_amount` | `NUMERIC(10,2)` | `NULL` | Minimum qualifying subtotal |
| `maximum_discount` | `NUMERIC(10,2)` | `NULL` | Maximum discount cap (for percentage) |
| `usage_limit` | `INTEGER` | `NULL` | Total global usage cap |
| `used_count` | `INTEGER` | `NOT NULL, DEFAULT 0` | Current redemption counter |
| `per_user_limit` | `INTEGER` | `DEFAULT 1` | Maximum redemptions per customer |
| `start_date` | `TIMESTAMP` | `NOT NULL` | Campaign activation timestamp |
| `expiry_date` | `TIMESTAMP` | `NOT NULL` | Expiration timestamp |
| `active` | `BOOLEAN` | `NOT NULL, DEFAULT TRUE` | Administrative status toggle |
| `created_at` | `TIMESTAMP` | `NOT NULL` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | `NULL` | Last modification timestamp |

### Join Tables for Scoped Applicability
- `coupon_categories (coupon_id BIGINT REFERENCES coupons(id) ON DELETE CASCADE, category_id BIGINT REFERENCES categories(id) ON DELETE CASCADE, PRIMARY KEY(coupon_id, category_id))`
- `coupon_products (coupon_id BIGINT REFERENCES coupons(id) ON DELETE CASCADE, product_id BIGINT REFERENCES products(id) ON DELETE CASCADE, PRIMARY KEY(coupon_id, product_id))`
- `coupon_vendors (coupon_id BIGINT REFERENCES coupons(id) ON DELETE CASCADE, vendor_profile_id BIGINT REFERENCES vendor_profiles(id) ON DELETE CASCADE, PRIMARY KEY(coupon_id, vendor_profile_id))`

### Database Migrations
- `V1__fix_order_status_constraint.sql`: Orders status check constraint.
- `V2__add_coupon_applicability_scope.sql`: Safely adds `applicability_scope` column defaulting existing rows to `ENTIRE_PLATFORM`, creates `coupon_categories`, `coupon_products`, and `coupon_vendors` mapping tables with foreign keys and performance indexes.

### `coupon_usages` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Usage record identifier |
| `coupon_id` | `BIGINT` | `FK -> coupons(id), NOT NULL` | Redeemed coupon |
| `user_id` | `BIGINT` | `FK -> users(id), NOT NULL` | Customer who redeemed |
| `order_id` | `BIGINT` | `FK -> orders(id), UNIQUE, NOT NULL` | Associated order |
| `discount_amount` | `NUMERIC(10,2)` | `NOT NULL` | Exact discount applied |
| `used_at` | `TIMESTAMP` | `NOT NULL` | Timestamp of redemption |

---

## 3. REST API Specification

### 1. Customer Endpoints (`ROLE_CUSTOMER`)
- `GET /api/coupons/applicable`
  - Returns only promotional coupons applicable to the products currently in the customer's cart, with calculated `eligibleSubtotal` and `estimatedDiscount`.
- `POST /api/coupons/validate`
  - Body: `{ "code": "TECH20", "orderTotal": 2800.00 }`
  - Response: `{ "valid": true, "code": "TECH20", "discountType": "PERCENTAGE", "discountValue": 20.0, "applicabilityScope": "CATEGORY", "eligibleSubtotal": 1800.00, "discountAmount": 360.00, "finalTotal": 2440.00, "message": "Coupon applied successfully" }`
  - Error when cart is ineligible: `400 Bad Request` with `"This coupon is not applicable to the products in your cart."`
- `POST /api/coupons/apply`
  - Legacy apply endpoint for backward compatibility.
- `GET /api/coupons/available`
  - Returns active, unexpired promotional coupons.
- `GET /api/coupons/{code}`
  - Retrieves coupon terms by code.

### 2. Admin Endpoints (`ROLE_ADMIN`)
- `GET /api/admin/coupons?search={query}&status={ALL|ACTIVE|INACTIVE|EXPIRED}`
  - Lists all coupons with search & status filtering.
- `GET /api/admin/coupons/{id}`
  - Retrieves coupon details by ID including applicability scope and targeted IDs.
- `POST /api/admin/coupons`
  - Creates a new promotional campaign with scope (`PLATFORM`, `CATEGORY`, `PRODUCT`, `VENDOR`) and targeted IDs.
- `PUT /api/admin/coupons/{id}`
  - Updates an existing coupon campaign.
- `DELETE /api/admin/coupons/{id}`
  - Deletes a coupon.
- `PATCH /api/admin/coupons/{id}/activate` (alias `/enable`)
  - Activates a coupon.
- `PATCH /api/admin/coupons/{id}/deactivate` (alias `/disable`)
  - Deactivates a coupon.
- `GET /api/admin/coupons/{id}/usage`
  - Lists all redemption audit records for a coupon.
- `GET /api/admin/coupons/analytics`
  - Returns aggregated platform metrics (Total Coupons, Active Coupons, Expired Coupons, Total Usage, Total Discount Given, Top Performing Coupon, and per-coupon breakdown).

---

## 4. Security & RBAC Enforcement

| Role | Admin Endpoints (`/api/admin/coupons/**`) | Customer Endpoints (`/api/coupons/**`) |
|---|---|---|
| `ROLE_ADMIN` | ✅ Full Access (200 / 201 / 204) | ✅ Allowed |
| `ROLE_CUSTOMER` | ❌ 403 Forbidden | ✅ Allowed |
| `ROLE_VENDOR` | ❌ 403 Forbidden | ❌ 403 Forbidden |
| `ROLE_WAREHOUSE_STAFF` | ❌ 403 Forbidden | ❌ 403 Forbidden |
| Unauthenticated | ❌ 401 Unauthorized | ❌ 401 Unauthorized |

---

## 5. Verification Results

- **Backend Tests:** 276 / 276 Passed (0 failures, 0 errors)
- **Frontend Build:** 1710 modules transformed with Vite (0 errors)
- **RBAC & Security:** Verified with MockMvc tests for all 4 roles and unauthenticated requests.
- **Workflow:** End-to-end checkout coupon validation and admin lifecycle verified.
