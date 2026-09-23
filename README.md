# ShopStack – Enterprise Multi-Vendor E-Commerce Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18.3.1-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5.4.11-646CFF.svg)](https://vitejs.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)](https://www.postgresql.org/)
[![Neon](https://img.shields.io/badge/Neon-PostgreSQL-00E599.svg)](https://neon.tech/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3.4.17-38B2AC.svg)](https://tailwindcss.com/)
[![Redux Toolkit](https://img.shields.io/badge/Redux%20Toolkit-2.5.0-764ABC.svg)](https://redux-toolkit.js.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> An enterprise-grade, multi-vendor e-commerce and supply chain management platform engineered with modern layered architecture, robust role-based access control (RBAC), multi-warehouse fulfillment logistics, server-side coupon computation, integrated Razorpay payment workflows, and real-time event-driven notifications.

---

## 1. Project Title

**ShopStack – Enterprise Multi-Vendor E-Commerce Platform**  
*Full-Stack Multi-Tenant Marketplace with Multi-Warehouse Logistics, RBAC & End-to-End Order Fulfillment*

---

## 2. Project Overview

**ShopStack** is a distributed, production-ready multi-vendor e-commerce marketplace solution developed to solve complex challenges in multi-tenant commerce, supply chain management, and order fulfillment. Designed with high cohesion and loose coupling, ShopStack bridges the gap between digital consumer shopping, vendor storefront management, multi-warehouse operational logistics, and enterprise administrative governance.

The platform provides a complete ecosystem for four distinct user personas (**Customer**, **Vendor**, **Warehouse Staff**, and **Admin**), enforcing strict authorization boundaries and data isolation. Key architectural highlights include:
- **Layered Enterprise Architecture**: Strict `Controller → Service → Repository → Database` separation on the backend, paired with a componentized React + Redux Toolkit architecture on the frontend.
- **Supply Chain & Multi-Warehouse Fulfillment**: Dynamic order allocation across regional physical warehouses, picking/packing fulfillment stations, carrier dispatch bays, and quality-checked return/refund pipelines.
- **Enterprise Promotions Engine**: Scoped server-side coupon calculations supporting platform-wide, category-specific, product-specific, and vendor-specific discounts with usage limit audits.
- **High-Integrity Data Layer**: Optimistic locking concurrency control for inventory balances, immutable audit logs for stock movements, and automated Flyway schema migrations.

---

## 3. Live Demo

Experience the live deployed storefront and role-based portals:

- **Frontend Application (Vercel)**:  
  [https://shopstack-enterprise-multi-vendor-e-commerce-platfor-3lccjl5ct.vercel.app](https://shopstack-enterprise-multi-vendor-e-commerce-platfor-3lccjl5ct.vercel.app)

---

## 4. Backend API

The backend REST API is hosted on Render and serves all application endpoints:

- **Base Backend URL**:  
  [https://shopstack-enterprise-multi-vendor-e.onrender.com](https://shopstack-enterprise-multi-vendor-e.onrender.com)

- **Backend Health Check API**:  
  [https://shopstack-enterprise-multi-vendor-e.onrender.com/api/health](https://shopstack-enterprise-multi-vendor-e.onrender.com/api/health)

- **Swagger / OpenAPI Documentation (Local / Dev)**:  
  `http://localhost:8080/swagger-ui.html` | `/v3/api-docs`

---

## 5. Key Features

- **Multi-Tenant Marketplace**: Independent storefronts for verified vendors with automated commission tracking and product isolation.
- **Role-Based Access Control (RBAC)**: Fine-grained security matrix enforced via Spring Security `@PreAuthorize` across all APIs.
- **Dynamic Multi-Warehouse Logistics**: Automated and manual order item allocations to physical warehouses based on stock availability.
- **Warehouse Operational Workflows**: Dedicated workstations for picking queue, packing validation, dispatch staging, and inventory adjustments.
- **Return & Refund Lifecycle**: Strict state-machine validation allowing return requests only on `DELIVERED` orders with integrated quality check (QC) dispositions (Restock, Quarantine, Reject).
- **Server-Side Coupon Engine**: High-performance discount validation supporting `PERCENTAGE` and `FIXED_AMOUNT` types with scopes (`ENTIRE_PLATFORM`, `SPECIFIC_CATEGORIES`, `SPECIFIC_PRODUCTS`, `SPECIFIC_VENDORS`).
- **Inventory Concurrency & Auditability**: Optimistic locking (`@Version`) preventing overselling under high concurrency, paired with an immutable `StockMovement` ledger.
- **Event-Driven Notifications**: User-isolated in-app notification pipeline triggered across order state transitions, shipment milestones, stock alerts, and vendor approvals.
- **Integrated Payment Gateway**: Razorpay payment order generation and cryptographic HMAC-SHA256 signature verification.
- **Responsive UI/UX**: Built with React 18, Tailwind CSS, Lucide icons, and Redux Toolkit with responsive dashboards for all four roles.

---

## 6. User Roles

ShopStack defines four distinct user roles, each equipped with dedicated views, permissions, and business capabilities:

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                             SHOPSTACK USER ROLES                              │
├─────────────────┬─────────────────┬─────────────────────┬─────────────────────┤
│    CUSTOMER     │     VENDOR      │   WAREHOUSE STAFF   │        ADMIN        │
├─────────────────┼─────────────────┼─────────────────────┼─────────────────────┤
│ • Product search│ • Profile setup │ • Picking stations  │ • Global analytics  │
│ • Wishlist/Cart │ • Product CRUD  │ • Packing queue     │ • Category manager  │
│ • Coupon checkout│ • Stock alerts  │ • Carrier dispatch  │ • Vendor approvals  │
│ • Order tracking│ • Order metrics │ • Return QC inspect │ • Staff management  │
│ • Return request│ • Vendor balance│ • Stock adjustments │ • Coupon engine     │
│ • In-app alerts │ • Cross-boundary│ • Audit log review  │ • Commission rules  │
│                 │   data blocked  │                     │ • System audit logs │
└─────────────────┴─────────────────┴─────────────────────┴─────────────────────┘
```

### 1. Customer (`ROLE_CUSTOMER`)
- Self-registration and secure authentication.
- Public catalog browsing with full-text search, category filters, and price ranges.
- Persistent Cart and Wishlist management with cross-session synchronization.
- Seamless multi-item checkout with coupon validation and Razorpay payment.
- Real-time order tracking with detailed shipment milestone history.
- Return request initiation for `DELIVERED` orders with return reasons.
- In-app notification center for order updates, shipping alerts, and promotions.

### 2. Vendor (`ROLE_VENDOR`)
- Vendor profile registration, business onboarding, and store profile management.
- Catalog management: Create, update, toggle availability, and delete products (with SKU, category, stock, price, and images).
- Cross-vendor boundary enforcement: Vendors can only access and modify their own inventory and orders.
- Order and shipment oversight for items originating from their vendor catalog.
- Vendor sales metrics, revenue monitoring, and commission deductions.

### 3. Warehouse Staff (`ROLE_WAREHOUSE_STAFF`)
- Dedicated operational dashboard for regional warehouse logistics.
- **Picking Station**: Real-time queue of pending warehouse allocations ready for pick-pack cycles.
- **Packing Station**: Packaging confirmation and parcel weight verification.
- **Carrier Dispatch Bay**: Carrier assignment, tracking number generation, and dispatch handover.
- **Return Inspection (QC)**: Physical return intake with pass/fail evaluation and inventory routing (`RESTOCK` vs `QUARANTINE`).
- **Inventory Adjustments**: Manual stock counts, reconciliation logs, and location bin tracking.

### 4. Administrator (`ROLE_ADMIN`)
- Master executive control center with macro marketplace KPIs (GMV, net revenue, active orders, vendor counts).
- Vendor approval pipeline: Review, approve, reject, or suspend vendor storefronts.
- Warehouse staff lifecycle management: Provision staff accounts, assign warehouse locations, and approve staff access.
- Category hierarchy management: Full CRUD operations for platform-wide product categories.
- Advanced Coupon Engine: Create promotional codes with complex validity rules, discount limits, and scope restrictions.
- Platform commissions and financial reporting with exportable data summaries.

---

## 7. Technology Stack

### Backend
| Technology | Version | Description |
|---|---|---|
| **Java** | `21 (LTS)` | Core runtime utilizing modern Java features |
| **Spring Boot** | `3.3.2` | Core backend framework and dependency injection container |
| **Spring Security** | `6.x` | Enterprise authentication and method-level authorization (`@EnableMethodSecurity`) |
| **JJWT (Java JWT)** | `0.12.6` | Stateless token creation, HMAC-SHA256 signing, and validation |
| **Spring Data JPA** | `3.3.2` | Persistence layer abstraction with Hibernate ORM |
| **PostgreSQL Driver** | `42.7.3` | JDBC connection driver for PostgreSQL and Neon DB |
| **Flyway Migration** | Automated | Database schema version control and continuous evolution |
| **SpringDoc OpenAPI** | `2.6.0` | Swagger UI and OpenAPI 3.0 automated API documentation |
| **Razorpay Java SDK** | `1.4.8` | Payment order creation, webhook handling, and HMAC verification |
| **Spring Boot Mail** | `3.3.2` | SMTP email dispatch for password reset and notifications |
| **Lombok** | `1.18.x` | Boilerplate reduction for models, DTOs, and loggers |
| **JUnit 5 & Mockito** | `5.10.x` | Comprehensive unit, integration, and security testing |
| **H2 Database** | `2.2.x` | In-memory database for isolated, fast test execution |

### Frontend
| Technology | Version | Description |
|---|---|---|
| **React** | `18.3.1` | Declarative UI library with functional components & hooks |
| **Vite** | `5.4.11` | Ultra-fast next-generation frontend build tool |
| **Redux Toolkit** | `2.5.0` | Centralized state management (auth, cart, wishlist, notifications) |
| **React-Redux** | `9.2.0` | Official React bindings for Redux store |
| **React Router DOM** | `7.1.3` | Client-side routing with nested routes and protected guards |
| **Tailwind CSS** | `3.4.17` | Utility-first responsive design and modern styling system |
| **Axios** | `1.19.0` | HTTP client with request/response interceptors for JWT tokens |
| **Lucide React** | `0.474.0` | Clean, modern icon library |

### Infrastructure & Database
| Component | Provider / Technology | Description |
|---|---|---|
| **Local Database** | PostgreSQL 16 (Local / Docker) | Local relational persistence running on port `5432` |
| **Production Database**| Neon PostgreSQL | Serverless cloud PostgreSQL with connection pooling |
| **Frontend Hosting** | Vercel | Global edge CDN deployment for React SPA |
| **Backend Hosting** | Render | Fully managed containerized Spring Boot runtime |
| **Containerization** | Docker & Docker Compose | Multi-stage production container builds |

---

## 8. System Architecture

ShopStack follows a decoupled, cloud-native architecture. The React SPA communicates with the Spring Boot REST API over HTTPS with JWT bearer tokens.

```
┌────────────────────────────────────────────────────────────────────────┐
│                              CLIENT TIER                               │
│                         React 18 Single Page App                       │
│                     (Hosted on Vercel Edge Network)                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTPS / REST (JWT Auth)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             GATEWAY / API                              │
│                      Spring Security + CORS Filter                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             BACKEND TIER                               │
│                  Spring Boot 3.3.2 (Hosted on Render)                  │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                        Controller Layer                        │   │
│   │  (AuthController, OrderController, WarehouseController, etc.)  │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│   ┌───────────────────────────────▼────────────────────────────────┐   │
│   │                         Service Layer                          │   │
│   │  (Business Logic, Concurrency Locks, RBAC, State Machines)    │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│   ┌───────────────────────────────▼────────────────────────────────┐   │
│   │                        Repository Layer                        │   │
│   │            (Spring Data JPA / Hibernate ORM Queries)           │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
└───────────────────────────────────┼────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             DATABASE TIER                              │
│               Neon PostgreSQL (Production) / Local PostgreSQL          │
│     [Flyway Migrations: V1 -> V7 | Optimistic Locking | Constraints]   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Major Modules

```
com.shopstack/
├── auth / security        # JWT token generation, UserDetailsService, SecurityConfig, RBAC
├── user                   # User entities, profile management, password reset tokens
├── vendor                 # Vendor onboarding, store profiles, status management
├── category               # Product categories hierarchy and active status filters
├── product                # Product catalog, multi-attribute SKUs, price, image links
├── inventory              # Central & warehouse stock levels, optimistic locks, movements
├── cart & wishlist        # Persistent user shopping baskets and favorites
├── coupon                 # Scoped discount calculation engine and usage ledger
├── order                  # Multi-item order placement, state transitions, totals calculation
├── payment                # Razorpay order generation, HMAC verification, refund records
├── warehouse              # Multi-warehouse facilities, allocations, picking, packing, dispatch
├── shipment               # Tracking numbers, milestone events, courier integration
├── return & refund        # Return request validation, QC inspections, refund issuance
├── notification           # Event-driven in-app notifications and user alert preferences
└── analytics & reports    # GMV, platform commission, sales breakdowns, CSV exports
```

---

## 10. Order Lifecycle

ShopStack enforces an immutable, strictly validated state machine for all orders. Invalid transitions (e.g., trying to shift an order from `DELIVERED` back to `CONFIRMED`) are rejected at the service boundary.

```
       [Customer Places Order]
                  │
                  ▼
              PENDING ─────────(Payment / Admin / Customer Cancel)─────────► CANCELLED
                  │
                  ▼
              CONFIRMED (Stock Allocated to Warehouse)
                  │
                  ▼
              PROCESSING (Picking & Packing at Warehouse)
                  │
                  ▼
               SHIPPED (Carrier Dispatched with Tracking #)
                  │
                  ▼
              DELIVERED (Final Delivery Confirmed)
                  │
                  ▼
         [Return Eligible Window]
```

### State Machine Rules:
1. **PENDING**: Created upon checkout. Stock is reserved. Can be cancelled by customer or admin.
2. **CONFIRMED**: Payment verified or Cash-on-Delivery confirmed. Items routed to warehouse allocation queue.
3. **PROCESSING**: Warehouse staff claims items in the picking queue and executes packaging.
4. **SHIPPED**: Carrier tracking number assigned and tracking milestone dispatched.
5. **DELIVERED**: Final delivery confirmed. Enables customer return eligibility.
6. **CANCELLED**: Terminated order. Allocated stock is released back to inventory automatically via stock movement ledger.

---

## 11. Return and Refund Lifecycle

Returns in ShopStack are strictly protected to prevent fraudulent claims:

```
[Order DELIVERED] ──► Customer Submits Return ──► Status: REQUESTED
                             │
                             ├─► Admin / Staff Rejection ──► REJECTED
                             │
                             ▼
                     Status: APPROVED
                             │
                             ▼
                     Item Received at Warehouse
                             │
                             ▼
                 [Quality Check (QC) Inspection]
                             │
             ┌───────────────┴───────────────┐
             ▼                               ▼
       [QC PASSED]                      [QC FAILED]
      Restock Item                   Quarantine / Damage
             │                               │
             └───────────────┬───────────────┘
                             │
                             ▼
                     Status: RETURNED
                             │
                             ▼
                     Refund Generated ──► Status: REFUNDED
```

- **Prerequisite Validation**: Returns can **only** be initiated if the order is in `DELIVERED` status.
- **Duplicate Prevention**: Duplicate return requests on the same order produce an HTTP `409 Conflict`.
- **Warehouse QC Disposition**: Returned goods are inspected by warehouse personnel and categorized as `RESTOCK` (returns to available inventory) or `DAMAGED_DISPOSAL` (quarantined).

---

## 12. Warehouse Workflow

ShopStack includes a comprehensive warehouse logistics subsystem enabling real-time order fulfillment:

```
1. ALLOCATION QUEUE       WarehouseOrderController -> Auto-assigns items to warehouse inventory
         │
2. PICKING STATION        Staff reviews pick list -> Verifies physical shelf bin & SKU barcode
         │
3. PACKING STATION        Item packaging -> Box dimensions & weight recorded
         │
4. DISPATCH BAY           Carrier assignment -> Tracking Number generated -> Status: SHIPPED
         │
5. AUDIT LOGGING          Every change recorded in stock_movements (IMMUTABLE AUDIT TRAIL)
```

- **Multi-Warehouse Allocation**: Tracks inventory across distinct regional warehouses (`warehouses` and `warehouse_inventories` tables).
- **Stock Movement Ledger**: Every stock addition, adjustment, reservation, release, and return records an immutable `StockMovement` row with timestamp, actor ID, and movement type.

---

## 13. Authentication and Security

Security is built into every layer of ShopStack:

- **Stateless Authentication**: JJWT (Java JWT) tokens signed with HMAC-SHA256 containing user ID, email, and authoritative role claims.
- **Password Security**: Passwords are encrypted using Spring Security's `BCryptPasswordEncoder` with salt rounds. Plaintext passwords are never stored or logged.
- **Role-Based Access Control (RBAC)**: Fine-grained method security using `@PreAuthorize("hasRole('ADMIN')")`, `@PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")`, etc.
- **Cross-Vendor Isolation**: Data access logic ensures vendors cannot read, edit, or delete products or orders belonging to competing vendors.
- **Sensitive Data Redaction**: Password hashes, JWT secrets, and payment credentials are never serialized in API responses.
- **CORS Protection**: Strict cross-origin resource sharing configured via `CORS_ALLOWED_ORIGINS` to only permit authorized frontend origins.

---

## 14. Database

ShopStack is designed for PostgreSQL, utilizing relational integrity, foreign keys, index optimization, and versioned migrations.

### Entity Relationship & Core Tables
| Table Name | Primary Key | Description | Key Constraints & Indexes |
|---|---|---|---|
| `users` | `id` (BIGSERIAL) | User accounts & credentials | `UNIQUE(email)`, Role enum |
| `vendor_profiles` | `id` (BIGSERIAL) | Vendor store details & approval status | `UNIQUE(user_id)`, `FK(user_id)` |
| `categories` | `id` (BIGSERIAL) | Product category taxonomy | `UNIQUE(slug)` |
| `products` | `id` (BIGSERIAL) | Product catalog items | `UNIQUE(sku)`, `UNIQUE(slug)`, `FK(category_id)` |
| `inventories` | `id` (BIGSERIAL) | Product stock balances | `UNIQUE(product_id)`, `@Version` optimistic lock |
| `stock_movements` | `id` (BIGSERIAL) | Immutable inventory audit ledger | `FK(product_id)`, `FK(performed_by_user_id)` |
| `warehouses` | `id` (BIGSERIAL) | Physical warehouse locations | `UNIQUE(code)` |
| `warehouse_inventories` | `id` (BIGSERIAL) | Per-warehouse stock levels | `UNIQUE(warehouse_id, product_id)` |
| `warehouse_staff_profiles` | `id` (BIGSERIAL) | Staff affiliation & approval | `UNIQUE(user_id)`, `FK(warehouse_id)` |
| `orders` | `id` (BIGSERIAL) | Customer order headers | `UNIQUE(order_number)`, `FK(user_id)` |
| `order_items` | `id` (BIGSERIAL) | Line items in an order | `FK(order_id)`, `FK(product_id)` |
| `order_item_warehouse_allocations` | `id` (BIGSERIAL) | Warehouse assignment per item | `FK(order_item_id)`, `FK(warehouse_id)` |
| `payments` | `id` (BIGSERIAL) | Payment records & gateway IDs | `UNIQUE(order_id)`, `FK(order_id)` |
| `shipments` | `id` (BIGSERIAL) | Shipment dispatch records | `UNIQUE(tracking_number)`, `FK(order_id)` |
| `tracking_events` | `id` (BIGSERIAL) | Chronological delivery checkpoints | `FK(shipment_id)` |
| `coupons` | `id` (BIGSERIAL) | Promotional discount engine | `UNIQUE(code)`, Scope enum |
| `coupon_usages` | `id` (BIGSERIAL) | User discount redemption tracking | `FK(coupon_id)`, `FK(user_id)`, `FK(order_id)` |
| `return_requests` | `id` (BIGSERIAL) | Product return requests | `UNIQUE(order_id)`, `FK(order_id)` |
| `refunds` | `id` (BIGSERIAL) | Financial refund logs | `FK(payment_id)`, `FK(return_request_id)` |
| `notifications` | `id` (BIGSERIAL) | In-app user notifications | `FK(user_id)`, Read status index |

### Automated Flyway Migrations
Database migrations are located in `backend/src/main/resources/db/migration/`:
- `V1__fix_order_status_constraint.sql`: Expands order status enum checks.
- `V2__add_coupon_applicability_scope.sql`: Adds scoped promotions and mapping tables (`coupon_categories`, `coupon_products`, `coupon_vendors`).
- `V3__add_multi_warehouse_allocation.sql`: Establishes multi-warehouse logistics schema and item allocations.
- `V4__complete_warehouse_return_workflow.sql`: Integrates warehouse return intake and quality control fields.
- `V5__update_stock_movements_constraint_and_sync.sql`: Enhances audit movement type constraints and synchronization triggers.
- `V6__update_refund_return_notification_constraints.sql`: Adds notification constraints for refund and return events.
- `V7__warehouse_staff_registration_and_approval.sql`: Introduces warehouse staff onboarding and admin approval workflows.

---

## 15. API Overview

ShopStack exposes over 60 RESTful endpoints organized by functional domain:

### Authentication & Profiles (`/api/auth`, `/api/users`)
- `POST /api/auth/register` — Register a new customer account
- `POST /api/auth/login` — Authenticate and receive JWT token
- `GET /api/auth/me` — Retrieve current authenticated user profile
- `POST /api/auth/forgot-password` — Request password reset email
- `POST /api/auth/reset-password` — Complete password reset with token

### Catalog & Search (`/api/products`, `/api/categories`)
- `GET /api/products` — Paginated product search with filters (category, price, keyword)
- `GET /api/products/{id}` — Retrieve detailed product information
- `POST /api/products` — Create new product (*Vendor / Admin*)
- `PUT /api/products/{id}` — Update product details (*Vendor / Admin*)
- `GET /api/categories` — List active product categories

### Shopping Cart & Wishlist (`/api/cart`, `/api/wishlist`)
- `GET /api/cart` — Fetch user's active shopping cart
- `POST /api/cart/items` — Add product SKU to cart
- `PUT /api/cart/items/{itemId}` — Update item quantity in cart
- `DELETE /api/cart/items/{itemId}` — Remove item from cart
- `GET /api/wishlist` — Retrieve customer wishlist items
- `POST /api/wishlist/items` — Toggle product in wishlist

### Coupons & Promotions (`/api/coupons`, `/api/admin/coupons`)
- `POST /api/coupons/validate` — Server-side coupon verification and discount calculation
- `GET /api/admin/coupons` — List all promotional coupons (*Admin*)
- `POST /api/admin/coupons` — Create scoped promotional coupon (*Admin*)
- `PATCH /api/admin/coupons/{id}/toggle-status` — Activate / Deactivate coupon (*Admin*)

### Orders & Checkout (`/api/orders`)
- `POST /api/orders` — Convert cart to confirmed order with coupon deductions
- `GET /api/orders` — List authenticated user's order history
- `GET /api/orders/{id}` — Fetch single order details with item breakdown
- `PATCH /api/orders/{id}/cancel` — Cancel pending order and release reserved stock

### Payments (`/api/payments`)
- `POST /api/payments/create/{orderId}` — Create Razorpay payment transaction order
- `POST /api/payments/verify` — Verify cryptographic HMAC-SHA256 payment signature

### Multi-Warehouse Operations (`/api/warehouse`)
- `GET /api/warehouse/orders/fulfillment-queue` — View pending order fulfillment items
- `POST /api/warehouse/orders/{orderId}/pick` — Mark order items as picked
- `POST /api/warehouse/orders/{orderId}/pack` — Complete item packaging
- `POST /api/warehouse/orders/{orderId}/dispatch` — Assign carrier and dispatch shipment
- `GET /api/warehouse/inventory` — View warehouse stock levels and bin locations
- `POST /api/warehouse/inventory/adjust` — Execute manual stock adjustments with audit logging

### Shipments & Tracking (`/api/shipments`)
- `GET /api/shipments/tracking/{trackingNumber}` — Public shipment timeline lookup
- `POST /api/shipments` — Create shipment record (*Warehouse Staff / Admin*)
- `PUT /api/shipments/{id}/status` — Update shipment transit status and add milestone

### Returns & Refunds (`/api/returns`, `/api/refunds`)
- `POST /api/returns` — Request return on `DELIVERED` order (*Customer*)
- `GET /api/returns` — List user return requests
- `POST /api/warehouse/returns/{returnId}/inspect` — Warehouse QC inspection (*Pass / Fail*)
- `POST /api/admin/returns/{returnId}/refund` — Approve refund disbursement (*Admin*)

### Notifications (`/api/notifications`)
- `GET /api/notifications` — Retrieve user in-app notifications
- `PATCH /api/notifications/{id}/read` — Mark notification as read
- `PATCH /api/notifications/read-all` — Mark all notifications as read

### Admin Governance & Analytics (`/api/admin`)
- `GET /api/admin/analytics/dashboard` — Platform macro KPIs (GMV, orders, revenue)
- `GET /api/admin/reports/sales/csv` — Stream exportable CSV sales reports
- `POST /api/admin/users/staff` — Provision warehouse staff accounts
- `PATCH /api/admin/vendors/{id}/status` — Approve or reject vendor profiles

---

## 16. Project Structure

```
ShopStack Ecomerce/
├── .env.example                     # Environment variables template
├── docker-compose.yml               # Multi-container orchestration (PostgreSQL, Backend, Frontend)
├── README.md                        # Primary project documentation
├── database/
│   ├── README.md                    # Dedicated database setup & Flyway migration guide
│   ├── V2__add_coupon_applicability_scope.sql
│   ├── V3__add_multi_warehouse_allocation.sql
│   ├── V4__complete_warehouse_return_workflow.sql
│   ├── V5__update_stock_movements_constraint_and_sync.sql
│   ├── V6__update_refund_return_notification_constraints.sql
│   └── V7__warehouse_staff_registration_and_approval.sql
├── backend/
│   ├── pom.xml                      # Maven dependencies (Spring Boot, JJWT, Postgres, Razorpay)
│   ├── Dockerfile                   # Multi-stage JDK build -> JRE runtime container
│   └── src/
│       ├── main/
│       │   ├── java/com/shopstack/
│       │   │   ├── config/          # SecurityConfig, CorsConfig, OpenApiConfig
│       │   │   ├── controller/      # 30 REST Controllers for all domains
│       │   │   ├── dto/             # Request / Response Transfer Objects
│       │   │   ├── entity/          # 44 JPA Entities, Enums & Listeners
│       │   │   ├── exception/       # GlobalExceptionHandler & Custom Exceptions
│       │   │   ├── mapper/          # Entity-to-DTO conversion mappers
│       │   │   ├── repository/      # Spring Data JPA Data Repositories
│       │   │   ├── security/        # JwtAuthFilter, JwtTokenProvider, CustomUserDetailsService
│       │   │   ├── service/         # Transactional Business Logic & Service Interfaces
│       │   │   └── util/            # Helper utilities (CSV export, signature verifier)
│       │   └── resources/
│       │       ├── application.properties        # Main configuration with env placeholders
│       │       ├── application-local.properties  # Local developer overrides
│       │       └── db/migration/                 # Flyway SQL migration scripts (V1 through V7)
│       └── test/
│           └── java/com/shopstack/               # 30 Test Classes (Controllers, Services, RBAC)
└── frontend/
    ├── package.json                 # Node dependencies (React 18, Vite 5, Redux, Tailwind)
    ├── vite.config.js               # Vite configuration
    ├── Dockerfile                   # Node build -> Nginx serving container
    ├── nginx.conf                   # Nginx reverse proxy and SPA routing
    └── src/
        ├── components/              # Reusable UI components (Navbar, Footer, Modals, Cards)
        ├── context/                 # Context providers
        ├── hooks/                   # Custom React hooks
        ├── layouts/                 # MainLayout with header and footer
        ├── pages/                   # Storefront pages (Home, Products, Cart, Checkout, etc.)
        │   └── dashboard/           # Role dashboards (Admin, Customer, Vendor, Warehouse)
        ├── routes/                  # AppRoutes & ProtectedRoute component
        ├── services/                # Axios API service clients
        ├── store/                   # Redux Toolkit slices (authSlice, cartSlice, etc.)
        └── utils/                   # Formatting, token helpers, constants
```

---

## 17. Local Development Setup

Follow these steps to run ShopStack on your local workstation:

### Prerequisites
- **Java Development Kit (JDK)**: Version 21 installed (`java -version`)
- **Apache Maven**: Version 3.8+ installed (`mvn -v`)
- **Node.js**: Version 18+ and `npm` installed (`node -v`)
- **PostgreSQL**: Version 15+ running locally on port `5432`

---

### Step 1: Database Setup
1. Start your local PostgreSQL server.
2. Create the application database using `psql` or pgAdmin:
   ```sql
   CREATE DATABASE shopstack_db;
   ```

---

### Step 2: Backend Setup
1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Set your local environment variables in your terminal or configure `.env`:
   ```bash
   # Windows PowerShell
   $env:DB_URL="jdbc:postgresql://localhost:5432/shopstack_db"
   $env:DB_USERNAME="postgres"
   $env:DB_PASSWORD="your_actual_postgresql_password"
   $env:JWT_SECRET="your_secure_base64_jwt_secret_minimum_32_bytes_long"
   $env:CORS_ALLOWED_ORIGINS="http://localhost:5173"
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
   > **Note**: Flyway will automatically execute all pending migrations on startup. The backend will initialize on `http://localhost:8080`.

---

### Step 3: Frontend Setup
1. Open a separate terminal and navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
2. Install node dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. Open your browser and navigate to:
   ```
   http://localhost:5173
   ```

---

## 18. Production Deployment

ShopStack is deployed in a modern, production-grade cloud topology:

```
[User Browser]
      │
      ▼
[Vercel Global Edge CDN]
  • React 18 SPA (Vite Build)
  • URL: https://shopstack-enterprise-multi-vendor-e-commerce-platfor-3lccjl5ct.vercel.app
      │
      ▼ HTTPS (Bearer JWT)
[Render Cloud Backend]
  • Spring Boot 3.3.2 Container (Java 21 Runtime)
  • Environment Variable Configuration
  • URL: https://shopstack-enterprise-multi-vendor-e.onrender.com
      │
      ▼ TLS JDBC Connection
[Neon Serverless PostgreSQL]
  • High Availability Cloud PostgreSQL
  • Automatic Schema Migrations via Flyway
```

### Production Configuration Notes:
- **CORS Allowed Origins**: The backend on Render is configured with `CORS_ALLOWED_ORIGINS` matching the Vercel deployment URL to secure cross-origin API interactions.
- **Connection Security**: The backend connects to Neon PostgreSQL over SSL/TLS with connection pooling.
- **Secret Management**: All secrets (database credentials, JWT signing key, payment gateway keys) are injected securely via cloud provider environment variables.

---

## 19. Environment Variables

Create a `.env` file in the root directory by copying `.env.example`:

```bash
cp .env.example .env
```

| Variable Name | Required | Default / Example Value | Description |
|---|---|---|---|
| `DB_URL` | Yes | `jdbc:postgresql://localhost:5432/shopstack_db` | PostgreSQL JDBC connection URL |
| `DB_USERNAME` | Yes | `postgres` | Database username |
| `DB_PASSWORD` | Yes | `your_actual_postgresql_password` | Database password |
| `JWT_SECRET` | Yes | `your_secure_base64_jwt_secret_min_32_bytes` | Base64-encoded 256-bit JWT signing secret |
| `JWT_EXPIRATION` | No | `86400000` | Token expiration in milliseconds (default: 24h) |
| `CORS_ALLOWED_ORIGINS`| Yes | `http://localhost:5173` | Allowed frontend origins (comma-separated) |
| `JPA_DDL_AUTO` | No | `update` (dev) / `validate` (prod) | Hibernate schema auto-management |
| `RAZORPAY_KEY_ID` | No | `rzp_test_your_key_id` | Razorpay payment gateway API Key ID |
| `RAZORPAY_KEY_SECRET` | No | `your_razorpay_key_secret` | Razorpay payment gateway API Secret |
| `MAIL_HOST` | No | `smtp.gmail.com` | SMTP host for outgoing password reset emails |
| `MAIL_PORT` | No | `587` | SMTP port |
| `MAIL_USERNAME` | No | `your_email@gmail.com` | SMTP mail username |
| `MAIL_PASSWORD` | No | `your_app_password` | SMTP mail application password |

> [!SECURITY NOTE]
> Never commit actual passwords, private API keys, or JWT secrets to GitHub. Always use environment variables in production.

---

## 20. Testing

ShopStack maintains a high level of code reliability with extensive test coverage across controllers, services, security boundaries, and warehouse workflows.

```
Total Backend Tests : 374 Tests
Pass Rate           : 374 / 374 PASS (100%)
Failures / Errors   : 0 Failures, 0 Errors, 0 Skipped
Frontend Modules    : 1,714 modules transformed with 0 errors
```

### Test Suite Components:
- **Unit & Service Tests**: Test individual business logic rules in isolation with Mockito mocks (`OrderServiceTest`, `CouponServiceTest`, `InventoryServiceTest`, `ReturnServiceTest`, `PaymentServiceTest`, etc.).
- **Controller & MockMvc Tests**: Validate REST API endpoints, DTO validations, HTTP response status codes, and JSON serialization.
- **Role & Security Authorization Tests**: Enforce RBAC rules to ensure non-permitted roles receive HTTP `403 Forbidden` across all protected endpoints.
- **Warehouse Integration & Lifecycle Tests**: Validate multi-warehouse allocation logic, pick/pack flows, and staff lifecycle operations.
- **In-Memory H2 Database**: Automated tests run on an in-memory H2 database engine, preventing dependency on live database instances during builds.

### Executing Backend Tests:
```bash
cd backend
mvn clean test
```

### Building & Verifying Frontend:
```bash
cd frontend
npm run build
```

---

## 21. Docker

ShopStack provides Docker container definitions for complete multi-container orchestration.

### Running with Docker Compose:
1. Ensure Docker Desktop is installed and running.
2. Copy the environment template:
   ```bash
   cp .env.example .env
   ```
3. Set your `DB_PASSWORD` and `JWT_SECRET` in `.env`.
4. Build and start all services:
   ```bash
   docker compose up --build
   ```

### Orchestrated Services:
- **`shopstack-postgres`**: PostgreSQL 16 Alpine container on port `5432` with healthcheck.
- **`shopstack-backend`**: Multi-stage Spring Boot container on port `8080` that waits for database health.
- **`shopstack-frontend`**: Nginx Alpine container serving the optimized React SPA on port `80` with API proxying.

---

## 22. GitHub Repository

The complete source code, documentation, and migration scripts are maintained in the official repository:

- **Repository**:  
  [https://github.com/Preethi-171106/Shopstack-Enterprise-Multi-Vendor-E-commerce-Platform](https://github.com/Preethi-171106/Shopstack-Enterprise-Multi-Vendor-E-commerce-Platform)

---

## 23. Future Enhancements

- **Real-Time WebSockets**: Migrate notification polling to live WebSocket / STOMP channels for instant push alerts.
- **Elasticsearch Integration**: Integrate Elasticsearch for sub-millisecond full-text product search with faceted filters and fuzzy matching.
- **Automated Carrier API Integration**: Connect carrier APIs (e.g., FedEx, Delhivery, Blue Dart) for automated label printing and live GPS tracking.
- **Advanced Warehouse Barcode Scanning**: Mobile camera and handheld barcode scanner support for picking/packing confirmation.
- **Vendor Payout Automation**: Automated banking transfer APIs for periodic vendor commission payouts.
- **Redis Caching Layer**: Cache frequent product catalog and category queries with distributed Redis caching.

---

## Author & Acknowledgments

- **Developer**: Preethi R
- **Program**: Infosys Springboard Internship 7.0
- **Project**: ShopStack Enterprise Multi-Vendor E-Commerce Platform
