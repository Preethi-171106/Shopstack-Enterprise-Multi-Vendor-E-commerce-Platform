# ShopStack Enterprise Multi-Vendor E-Commerce — Master Test Plan

## 1. Document Overview

- **Project:** ShopStack – Enterprise Multi-Vendor E-Commerce Platform
- **Milestone:** Milestone 4 – Task 2: Testing, Quality Assurance, Regression, Security, Bug Documentation & Test Validation
- **Version:** 1.0.0
- **Status:** APPROVED & EXECUTED
- **Date:** 2026-09-06

---

## 2. Test Objectives

The primary objective of Milestone 4 Task 2 is to validate the functional correctness, security, performance, data integrity, and cross-role workflows of the ShopStack enterprise platform.

Key validation goals include:
1. **Functional Correctness:** Verify all e-commerce domain operations (Catalog, Cart, Checkout, Order, Fulfillment, Returns, Refunds, Coupons, Notifications).
2. **API Correctness & Robustness:** Verify HTTP status codes, JSON response structures, error handling, validation constraints, and database persistence.
3. **Role-Based Access Control (RBAC):** Strictly enforce authorization boundaries across all 4 system roles (`CUSTOMER`, `VENDOR`, `WAREHOUSE_STAFF`, `ADMIN`).
4. **Security & Authentication:** Verify JWT generation/validation, BCrypt password hashing, session expiry handling, and isolation of vendor/customer data.
5. **Inventory Consistency:** Prevent overselling, negative stock, race conditions, ensure optimistic locking, and audit stock movements.
6. **Multi-Step E2E Order-to-Refund Lifecycle:** Validate complete flow from customer checkout through warehouse fulfillment to return QC and refund ledgering.
7. **Build & Regression Verification:** Ensure 100% passing backend automated test suites and zero-warning production frontend build.

---

## 3. Scope of Testing

### 3.1 In Scope
- **Backend Services & REST Controllers (Spring Boot 3.3.2):**
  - `AuthController` & `AuthService`
  - `CategoryController` & `CategoryService`
  - `ProductController` & `ProductService`
  - `InventoryController`, `WarehouseInventoryController`, `VendorInventoryController` & `InventoryService`
  - `CartController` & `CartService`
  - `WishlistController` & `WishlistService`
  - `CouponController`, `AdminCouponController` & `CouponServiceImpl`
  - `OrderController` & `OrderService`
  - `PaymentController`, `AdminPaymentController` & `PaymentServiceImpl`
  - `WarehouseOrderController`, `WarehouseAllocationController`, `ShipmentController` & `WarehouseManagementService` / `WarehouseAllocationService`
  - `ReturnController` & `ReturnServiceImpl`
  - `RefundController` & `RefundService`
  - `NotificationController` & `NotificationService`
  - `AdminDashboardController` & `AdminUserService`
- **Security & Authorization:**
  - `SecurityConfig`, `JwtAuthenticationFilter`, `CustomUserDetailsService`
  - Method-level `@PreAuthorize` security across all sensitive endpoints
- **Frontend Single Page Application (React 18 + Vite 5):**
  - Production build compilation and bundle analysis
  - Route configurations, `ProtectedRoute` guards, role redirections
  - State management (Redux Toolkit & React Context)
  - UI Dashboards (`CustomerDashboard`, `VendorDashboard`, `AdminDashboard`, `WarehouseDashboard`)
- **Regression Testing:**
  - Automated JUnit 5 / Mockito / MockMvc regression suite (349 tests)

### 3.2 Out of Scope
- Direct live Razorpay payment gateway settlement (mocked in automated tests; live keys configurable via environment variables).
- Docker container daemon execution on host machines lacking Docker Desktop engine.

---

## 4. Test Strategy & Methodology

```
+-------------------------------------------------------------------------+
|                              TEST PYRAMID                               |
+-------------------------------------------------------------------------+
|                  [ Manual & End-to-End Workflow Tests ]                 |
|             Customer -> Order -> Warehouse -> Return -> Refund          |
+-------------------------------------------------------------------------+
|               [ Integration & WebMvc Security Tests ]                   |
|       MockMvc REST Endpoints + RBAC Matrices + JWT Security Filters     |
+-------------------------------------------------------------------------+
|                     [ Service & Business Logic Tests ]                  |
|    Optimistic Locking + Coupon Scopes + Inventory Math + Order States   |
+-------------------------------------------------------------------------+
```

### 4.1 Unit Testing
- Test isolated business logic in service classes using Mockito and JUnit 5.
- Validate calculation edge cases (e.g. percentage discount, fixed discount cap, minimum purchase threshold, stock deduction/restoration).

### 4.2 Integration & API Testing
- Use Spring Boot `@WebMvcTest` and `@SpringBootTest` with MockMvc.
- Validate request validation (`@Valid`), HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`).

### 4.3 Security & RBAC Testing
- Execute role-based boundary tests (`RoleAuthorizationTest`, `InventoryControllerTest$RbacAndSecurityTests`, `WarehouseOrderControllerTest$SecurityAuthorizationTests`).
- Verify unauthenticated requests return `401 Unauthorized` and unauthorized role actions return `403 Forbidden`.

### 4.4 Regression Testing
- Re-execute the complete Maven test suite after any code modifications to ensure no existing capabilities are broken.

---

## 5. Test Environment & Tools

| Component | Specification |
|---|---|
| **OS** | Windows 11 / x64 |
| **Java SDK** | OpenJDK 21.0.12 (Eclipse Adoptium) |
| **Build Tool (Backend)** | Apache Maven 3.9.16 |
| **Testing Frameworks** | JUnit Jupiter 5.10.x, Mockito 5.11.x, Spring Boot Test 3.3.2 |
| **In-Memory Database** | H2 (in-memory test mode with PostgreSQL compatibility mode) |
| **Database (Dev/Prod)** | PostgreSQL 16 Alpine |
| **Frontend Framework** | Node.js / React 18 / Vite 5 / Tailwind CSS / Redux Toolkit |
| **HTTP Client** | Axios 1.19.0 / REST MockMvc |

---

## 6. Test Inventory Summary

| Module | Test File(s) | Test Type | Total Tests |
|---|---|---|---|
| **Authentication & Users** | `AuthControllerTest`, `RoleAuthorizationTest` | MockMvc / Spring Security | 23 |
| **Vendor Management** | `VendorProfileControllerTest` | MockMvc / Service | 8 |
| **Categories** | `CategoryControllerTest` | MockMvc / WebMvc | 17 |
| **Products** | `ProductControllerTest` | MockMvc / WebMvc | 25 |
| **Inventory & Movements** | `InventoryControllerTest`, `InventoryServiceTest` | MockMvc / Unit Mockito | 31 |
| **Cart** | `CartControllerTest`, `CartServiceTest` | MockMvc / Unit Mockito | 13 |
| **Wishlist** | `WishlistControllerTest` | MockMvc / WebMvc | 8 |
| **Coupons & Promotions** | `CouponControllerTest`, `CouponServiceTest` | MockMvc / Business Logic | 34 |
| **Orders & Fulfillment** | `OrderServiceTest`, `WarehouseOrderControllerTest`, `WarehouseAllocationServiceTest`, `WarehouseAllocationControllerTest` | MockMvc / Unit Mockito | 28 |
| **Payments & Refunds** | `PaymentControllerTest`, `PaymentServiceTest`, `ReturnServiceTest` | MockMvc / Mockito | 34 |
| **Shipments & Tracking** | `ShipmentControllerTest` | MockMvc / WebMvc | 16 |
| **Warehouse Operations** | `WarehouseInventoryControllerTest`, `WarehouseManagementServiceTest`, `AdminWarehouseControllerTest`, `WarehouseIntegrationE2ETest` | MockMvc / E2E Integration | 33 |
| **Notifications** | `NotificationControllerTest` | MockMvc / Event Dispatch | 12 |
| **Admin Dashboard** | `AdminDashboardControllerTest` | MockMvc / WebMvc | 15 |
| **Frontend Build** | Vite production build (`npm run build`) | Static & Module Analysis | 1,712 modules |
| **Total Automated Tests** | **349 Tests Across 26 Test Suites** | **100% Passing** | **349 / 349** |

---

## 7. Entry & Exit Criteria

### 7.1 Entry Criteria
- All source code for Milestone 1 through Milestone 4 Task 1 implemented and committed.
- Local database schema and migrations properly configured.
- Automated test suites compiling cleanly with Maven and Java 21.

### 7.2 Exit Criteria
- 100% of automated backend test cases execute and pass without error (`0 Failures, 0 Errors, 0 Skipped`).
- Frontend builds with zero compilation/bundling errors (`npm run build` exits with code 0).
- All role boundaries (`CUSTOMER`, `VENDOR`, `WAREHOUSE_STAFF`, `ADMIN`) strictly validated.
- Full E2E ordering, warehouse fulfillment, return QC, and refund workflow documented and verified.
- Comprehensive test case specifications, bug history log, and final summary report produced and checked in.
