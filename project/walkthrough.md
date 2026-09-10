# ShopStack Backend Walkthrough

## Project Architecture

ShopStack follows a layered architecture with strict separation of concerns:

```
Controller → Service → Repository → Entity
                ↕            ↕
               DTO        Mapper
```

### Layers

- **Controller** — REST endpoints, request validation, HTTP response handling
- **Service** — Business logic, transaction management, RBAC enforcement
- **Repository** — Spring Data JPA interfaces for data access
- **Entity** — JPA-mapped domain models
- **DTO** — Data Transfer Objects for API request/response
- **Mapper** — Converts between entities and DTOs

### Technology Stack

- Java 21
- Spring Boot 3.2.5
- Spring Security with JWT authentication
- Spring Data JPA / Hibernate
- PostgreSQL (production), H2 (tests)
- Maven

---

## Completed Milestones

### Milestone 1 — Authentication & JWT
JWT-based authentication with login endpoint, token generation, and filter-based authorization.

### Milestone 2 — User Management
User registration, profile management, role-based access control (CUSTOMER, VENDOR, ADMIN, WAREHOUSE_STAFF).

### Milestone 3 — Vendor Management
Vendor onboarding, approval workflow, vendor profile management.

### Milestone 4A — Category & Product Management
Category tree, product CRUD, vendor-product association.

### Milestone 4B — Cart, Wishlist & Checkout
Shopping cart, wishlist, checkout flow.

### Milestone 5 — Order Management
Order creation, status tracking, order history.

### Milestone 6 — Shipment & Tracking
Shipment creation, tracking numbers, delivery status.

### Milestone 7 — Inventory Management
Stock levels, low-stock alerts, warehouse integration.

### Milestone 8 — Payment Integration
Payment processing, payment status, transaction records.

### Milestone 9 — Coupon & Promotion Management
Coupon creation, assignment, validation, promotional campaigns.

### Milestone 4C — Notification Management

Implements a complete notification system with:

- **13 notification types** covering orders, payments, shipments, returns, refunds, coupons, stock, system, and promotions
- **2 delivery channels**: IN_APP and EMAIL
- **2 statuses**: UNREAD and READ
- **Automatic notification creation** triggered by business events (order placed, payment success/failure, shipment created/delivered, coupon assigned, low stock, return requested, refund completed)
- **Customer endpoints**: list, list unread, get by ID, mark as read, mark all as read, delete, delete all
- **Admin endpoints**: list all, get by ID, send system notification, broadcast to all users
- **Ownership enforcement**: users can only access their own notifications
- **Role-based security**: `/api/admin/**` requires ROLE_ADMIN
- **43 tests** covering service, customer controller, and admin controller with full status code and ownership verification

#### Key Files

- `notification/entity/Notification.java` — JPA entity with user relationship
- `notification/service/NotificationService.java` — Business logic + 9 auto-creation hooks
- `notification/controller/NotificationController.java` — 7 customer endpoints
- `notification/controller/AdminNotificationController.java` — 4 admin endpoints
- `notification/repository/NotificationRepository.java` — Custom queries for unread, mark-all-read, delete-all

#### API Endpoints

**Customer:**
- `GET /api/notifications` — paginated list
- `GET /api/notifications/unread` — unread only
- `GET /api/notifications/{id}` — single notification
- `PATCH /api/notifications/{id}/read` — mark as read
- `PATCH /api/notifications/read-all` — mark all as read
- `DELETE /api/notifications/{id}` — delete one
- `DELETE /api/notifications` — delete all

**Admin:**
- `GET /api/admin/notifications` — all notifications
- `GET /api/admin/notifications/{id}` — any notification
- `POST /api/admin/notifications/system` — system notification to a user
- `POST /api/admin/notifications/broadcast` — broadcast to all users

See `docs/backend-milestone-4c-notification-management.md` for full details.

### Milestone 4D — Analytics & Dashboard Management

Implements a complete analytics and dashboard system with:

- **Admin dashboard** with 15 platform-wide metrics (customers, vendors, products, categories, orders, revenue, payments, order status counts, coupons, inventory)
- **Vendor dashboard** with 12 vendor-scoped metrics (store name, products, orders, revenue, monthly/today revenue, top selling products, low stock, inventory summary)
- **Sales analytics**: daily, weekly, monthly, yearly sales reports with date-range filtering
- **Chart-ready JSON**: revenue chart and orders chart with date-value data points
- **Top lists**: top selling products, top categories, top vendors
- **Statistics**: payment method, order status, shipment status, coupon usage
- **JPQL aggregate queries** (COUNT, SUM, GROUP BY, ORDER BY) optimized to avoid N+1 problems
- **Vendor ownership enforcement**: vendors can only see their own analytics data
- **Role-based security**: `/api/admin/analytics/**` requires ROLE_ADMIN, `/api/vendor/analytics/**` requires ROLE_VENDOR
- **94 tests** (43 notification + 51 analytics) covering service, admin controller, vendor controller with full status code, ownership, and validation verification

#### Key Files

- `analytics/service/AnalyticsService.java` — Core business logic with aggregate queries
- `analytics/controller/AdminAnalyticsController.java` — 14 admin endpoints
- `analytics/controller/VendorAnalyticsController.java` — 6 vendor endpoints
- `analytics/dto/` — 16 DTOs for dashboard, sales, charts, and statistics responses

#### API Endpoints

**Admin (`/api/admin/analytics`):**
- `GET /dashboard` — admin dashboard summary
- `GET /sales/daily` — daily sales with date range
- `GET /sales/weekly` — weekly sales
- `GET /sales/monthly` — monthly sales
- `GET /sales/yearly` — yearly sales
- `GET /charts/revenue` — revenue chart data
- `GET /charts/orders` — orders chart data
- `GET /top-products` — top selling products
- `GET /top-categories` — top categories
- `GET /top-vendors` — top vendors
- `GET /payments` — payment method statistics
- `GET /shipments` — shipment status statistics
- `GET /inventory` — inventory summary
- `GET /coupons` — coupon usage statistics

**Vendor (`/api/vendor/analytics`):**
- `GET /dashboard` — vendor dashboard summary
- `GET /sales` — vendor sales with date range
- `GET /products` — vendor's products
- `GET /orders` — vendor's order status statistics
- `GET /inventory` — vendor's inventory summary
- `GET /top-products` — vendor's top selling products

See `docs/backend-milestone-4d-analytics-dashboard.md` for full details.

### Milestone 4E — Review & Rating Management

Implements a complete review and rating system with:

- **Customer reviews**: create, edit, delete, and view own reviews with 1-5 star ratings
- **Public access**: view approved product reviews, average rating, and rating distribution (no auth required)
- **Vendor reviews**: view reviews for own products and rating summaries
- **Admin moderation**: view all reviews, approve, reject, and delete inappropriate reviews
- **Business rules**: verified purchasers only, one review per product per customer, no cancelled order reviews, automatic product rating recalculation
- **Unique constraint**: database-level enforcement of one review per user per product
- **Rating summary**: average rating, total reviews, and 5-star distribution (5/4/3/2/1-star counts)
- **Conflict handling**: 409 Conflict for duplicate reviews via ConflictException
- **160 tests** (94 previous + 66 new) covering service, customer controller, vendor controller, and admin controller with full status code and business rule verification

#### Key Files

- `review/entity/Review.java` — Review entity with unique(user_id, product_id) constraint
- `review/service/ReviewService.java` — Core business logic with verified purchaser checks and rating recalculation
- `review/controller/ReviewController.java` — 6 customer + public endpoints
- `review/controller/VendorReviewController.java` — 3 vendor endpoints
- `review/controller/AdminReviewController.java` — 4 admin endpoints
- `review/dto/ReviewSummaryResponse.java` — Rating summary with star distribution

#### API Endpoints

**Customer (`/api/reviews`):**
- `POST /api/reviews` — create review (201, 400, 409)
- `PUT /api/reviews/{id}` — update own review (200, 403, 404)
- `DELETE /api/reviews/{id}` — delete own review (204, 403, 404)
- `GET /api/reviews/my` — view own reviews (200)
- `GET /api/reviews/product/{productId}` — view product reviews (public, 200, 404)
- `GET /api/reviews/product/{productId}/summary` — view rating summary (public, 200, 404)

**Vendor (`/api/vendor/reviews`):**
- `GET /api/vendor/reviews` — view reviews for own products (200, 403, 404)
- `GET /api/vendor/reviews/product/{productId}` — view reviews for specific product (200, 403, 404)
- `GET /api/vendor/reviews/product/{productId}/summary` — view rating summary for own product (200, 403, 404)

**Admin (`/api/admin/reviews`):**
- `GET /api/admin/reviews` — view all reviews (200, 403)
- `PATCH /api/admin/reviews/{id}/approve` — approve review (200, 403, 404)
- `PATCH /api/admin/reviews/{id}/reject` — reject review (200, 403, 404)
- `DELETE /api/admin/reviews/{id}` — delete review (204, 403, 404)

See `docs/backend-milestone-4e-review-rating-management.md` for full details.
