# Backend Milestone 3B-1 — Category Management

**ShopStack Enterprise Multi-Vendor E-Commerce Platform**
**Module:** Category Management
**Date:** 2026-07-23

---

## 1. Objective

Implement full CRUD Category Management for the ShopStack platform. This milestone enables admins to organise products into logical categories (create, update, status toggle, delete) while public users can browse all active categories.

---

## 2. Real-World Category Concept

In any e-commerce platform, **categories** serve as a navigation and discovery layer:

| Role | Interaction |
|------|-------------|
| **Admin** | Creates categories like "Electronics" or "Home & Kitchen", marks them active or inactive, updates names/descriptions, and deletes obsolete ones |
| **Vendor** | Lists products under active categories |
| **Customer / Public** | Browses active categories to discover products |

**Key Rules:**
- Only **active** categories are publicly visible
- Names must be **unique** (case-insensitive): `Electronics`, `electronics`, and `ELECTRONICS` are treated as duplicates
- Slugs are **auto-generated** from names: `"Home & Kitchen"` → `"home-kitchen"`
- Deactivating a category is a **soft change** — the record is preserved in the database

---

## 3. Architecture

The implementation follows the established ShopStack layered pattern:

```
HTTP Request
    ↓
CategoryController      (/api/admin/categories,  /api/categories)
    ↓
CategoryService         (business logic, slug generation, uniqueness checks)
    ↓
CategoryRepository      (Spring Data JPA, custom query methods)
    ↓
PostgreSQL              (categories table)
    ↑
CategoryMapper          (Category entity → CategoryResponse DTO)
```

**Package Structure:**

```
com.shopstack
├── entity/
│   └── Category.java                              [NEW]
├── repository/
│   └── CategoryRepository.java                   [NEW]
├── dto/
│   └── category/
│       ├── CategoryCreateRequest.java             [NEW]
│       ├── CategoryUpdateRequest.java             [NEW]
│       ├── CategoryStatusRequest.java             [NEW]
│       └── CategoryResponse.java                 [NEW]
├── mapper/
│   └── CategoryMapper.java                       [NEW]
├── service/
│   └── CategoryService.java                      [NEW]
├── controller/
│   └── CategoryController.java                   [NEW]
├── exception/
│   ├── CategoryAlreadyExistsException.java       [NEW]
│   └── CategoryNotFoundException.java            [NEW]
│   └── GlobalExceptionHandler.java               [MODIFIED]
└── config/
    └── SecurityConfig.java                       [MODIFIED]
```

---

## 4. Database Schema

**Table: `categories`**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incremented |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE | Case-insensitive uniqueness enforced at service layer |
| `slug` | VARCHAR(120) | NOT NULL, UNIQUE | Auto-generated from name |
| `description` | VARCHAR(1000) | NULLABLE | Optional |
| `image_url` | TEXT | NULLABLE | Optional; validated if provided |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Soft status flag |
| `created_at` | TIMESTAMP | NOT NULL | Set on creation |
| `updated_at` | TIMESTAMP | — | Updated on every save |

> **Note:** Hibernate auto-creates the `categories` table via `spring.jpa.hibernate.ddl-auto=update`.

**Equivalent DDL:**
```sql
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(1000),
    image_url   TEXT,
    active      BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP
);
```

---

## 5. API Endpoints

### 5.1 Admin Endpoints — ROLE_ADMIN Required

#### POST /api/admin/categories — Create Category
```
Method:       POST
URL:          http://localhost:8080/api/admin/categories
Headers:      Authorization: Bearer <ADMIN_JWT>
              Content-Type: application/json

Request Body:
{
  "name": "Electronics",
  "description": "All consumer electronics",
  "imageUrl": "https://example.com/electronics.jpg"
}

Success Response (201 Created):
{
  "id": 1,
  "name": "Electronics",
  "slug": "electronics",
  "description": "All consumer electronics",
  "imageUrl": "https://example.com/electronics.jpg",
  "active": true,
  "createdAt": "2026-07-23T18:30:00",
  "updatedAt": "2026-07-23T18:30:00"
}
```

#### PUT /api/admin/categories/{id} — Update Category
```
Method:       PUT
URL:          http://localhost:8080/api/admin/categories/1
Headers:      Authorization: Bearer <ADMIN_JWT>
              Content-Type: application/json

Request Body:
{
  "name": "Home & Kitchen",
  "description": "Kitchen and home appliances",
  "imageUrl": null
}

Success Response (200 OK):
{
  "id": 1,
  "name": "Home & Kitchen",
  "slug": "home-kitchen",
  "description": "Kitchen and home appliances",
  "imageUrl": null,
  "active": true,
  "createdAt": "2026-07-23T18:30:00",
  "updatedAt": "2026-07-23T18:35:00"
}
```

#### PATCH /api/admin/categories/{id}/status — Toggle Status
```
Method:       PATCH
URL:          http://localhost:8080/api/admin/categories/1/status
Headers:      Authorization: Bearer <ADMIN_JWT>
              Content-Type: application/json

Request Body (deactivate):    { "active": false }
Request Body (activate):      { "active": true }

Success Response (200 OK):
{
  "id": 1,
  "name": "Electronics",
  "slug": "electronics",
  "active": false,
  ...
}
```

#### DELETE /api/admin/categories/{id} — Delete Category
```
Method:       DELETE
URL:          http://localhost:8080/api/admin/categories/1
Headers:      Authorization: Bearer <ADMIN_JWT>

Success Response:  204 No Content (empty body)
Error Response:    404 Not Found
```

---

### 5.2 Public Endpoints — No Authentication Required

#### GET /api/categories — List Active Categories
```
Method:   GET
URL:      http://localhost:8080/api/categories

Success Response (200 OK):
[
  {
    "id": 1,
    "name": "Electronics",
    "slug": "electronics",
    "description": "All consumer electronics",
    "imageUrl": null,
    "active": true,
    "createdAt": "2026-07-23T18:30:00",
    "updatedAt": "2026-07-23T18:30:00"
  },
  {
    "id": 2,
    "name": "Home & Kitchen",
    "slug": "home-kitchen",
    ...
  }
]
```

#### GET /api/categories/{id} — Get Active Category by ID
```
Method:   GET
URL:      http://localhost:8080/api/categories/1

Success Response (200 OK):  { "id": 1, "name": "Electronics", ... }
Error Response (404):       Category is inactive or does not exist
```

---

## 6. Authorization Matrix

| Endpoint | ADMIN | VENDOR | CUSTOMER | PUBLIC |
|----------|-------|--------|----------|--------|
| POST /api/admin/categories | ✅ 201 | ❌ 403 | ❌ 403 | ❌ 401 |
| PUT /api/admin/categories/{id} | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 401 |
| PATCH /api/admin/categories/{id}/status | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 401 |
| DELETE /api/admin/categories/{id} | ✅ 204 | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /api/categories | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| GET /api/categories/{id} (active) | ✅ 200 | ✅ 200 | ✅ 200 | ✅ 200 |
| GET /api/categories/{id} (inactive) | ✅ 200* | ❌ 404 | ❌ 404 | ❌ 404 |

> *Admin fetches inactive categories via `findById` not `findByIdAndActiveTrue` — NOTE: in the current implementation, even admins get 404 for inactive categories on the public endpoint. This is by design for Milestone 3B-1; admin-specific view of all categories may be added in a future milestone.

---

## 7. Validation & Error Handling

### Input Validation

| Field | Rule | HTTP |
|-------|------|------|
| `name` | Required, max 100 chars | 400 |
| `description` | Optional, max 1000 chars | 400 |
| `imageUrl` | Optional; if provided must match `^(https?://.*)?$` | 400 |
| `active` | Required (Boolean), not null | 400 |

### Error Response Format

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Category with name 'electronics' already exists",
  "path": "/api/admin/categories",
  "timestamp": "2026-07-23T18:30:00"
}
```

### HTTP Error Codes

| Scenario | Code | Message |
|----------|------|---------|
| Duplicate name (case-insensitive) | 409 | "Category with name '...' already exists" |
| Category not found | 404 | "Category with ID X not found" |
| Invalid input / blank name | 400 | Validation error with field details |
| Unauthenticated admin access | 401 | "Authentication required" |
| CUSTOMER/VENDOR admin access | 403 | "Access Denied" |

---

## 8. Slug Generation Algorithm

The backend generates URL-safe slugs automatically from the category name:

```java
name.toLowerCase()
    .replaceAll("[^a-z0-9]+", "-")   // Replace non-alphanumeric sequences with hyphen
    .replaceAll("^-+|-+$", "");       // Strip leading/trailing hyphens
```

| Input Name | Generated Slug |
|------------|----------------|
| `Electronics` | `electronics` |
| `Home & Kitchen` | `home-kitchen` |
| `Men's Clothing` | `men-s-clothing` |
| `Books & Stationery` | `books-stationery` |
| `  Sports & Fitness  ` | `sports-fitness` |

---

## 9. Security Configuration Changes

**`SecurityConfig.java`** updated with new rules (existing rules preserved):

```java
// Public category endpoints (no JWT required)
.requestMatchers("/api/categories", "/api/categories/**").permitAll()
// Admin category management endpoints (ROLE_ADMIN only)
.requestMatchers("/api/admin/categories", "/api/admin/categories/**").hasRole("ADMIN")
```

---

## 10. Testing

### Automated Test Suite
**File:** `CategoryControllerTest.java` (38 test cases)

**Organized by `@Nested` groups:**
- `CreateCategory` — ADMIN success, CUSTOMER/VENDOR/unauth forbidden/unauthorized, 409 duplicate, 400 invalid input
- `UpdateCategory` — ADMIN success, RBAC, 404 missing, 409 duplicate name
- `UpdateCategoryStatus` — activate/deactivate, RBAC, 404 missing, 400 null active
- `DeleteCategory` — ADMIN success, RBAC, 404 missing
- `ListActiveCategories` — public access, empty list
- `GetActiveCategoryById` — public access, inactive → 404, missing → 404

### Run Automated Tests
```powershell
cd "c:\Users\PREETHI\OneDrive\Desktop\ShopStack Ecomerce\backend"
mvn clean test
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

---

## 11. Postman Testing Guide

### Step 1: Login as Admin and get JWT
```
POST http://localhost:8080/api/auth/login
{
  "email": "admin@shopstack.com",
  "password": "your_password"
}
→ Copy the token from "token" field
```

### Step 2: Create Categories (ADMIN)
```
POST http://localhost:8080/api/admin/categories
Authorization: Bearer <token>
{
  "name": "Electronics",
  "description": "Consumer electronics and gadgets"
}
→ Expect 201 Created. Note the id and slug.
```

```
POST http://localhost:8080/api/admin/categories
Authorization: Bearer <token>
{
  "name": "Home & Kitchen",
  "description": "Kitchen and home products"
}
→ Expect 201 Created. Verify slug = "home-kitchen".
```

### Step 3: Test Duplicate Name (case-insensitive)
```
POST http://localhost:8080/api/admin/categories
Authorization: Bearer <token>
{ "name": "ELECTRONICS" }
→ Expect 409 Conflict.
```

### Step 4: Update Category
```
PUT http://localhost:8080/api/admin/categories/1
Authorization: Bearer <token>
{ "name": "Consumer Electronics", "description": "Updated description" }
→ Expect 200 OK. Verify slug = "consumer-electronics".
```

### Step 5: Public List (no token needed)
```
GET http://localhost:8080/api/categories
→ Expect 200 OK with array of active categories.
```

### Step 6: Deactivate Category
```
PATCH http://localhost:8080/api/admin/categories/1/status
Authorization: Bearer <token>
{ "active": false }
→ Expect 200 OK. active = false.
```

### Step 7: Verify Deactivated Category is Hidden
```
GET http://localhost:8080/api/categories/1
→ Expect 404 Not Found (inactive category not publicly visible).
```

### Step 8: Reactivate Category
```
PATCH http://localhost:8080/api/admin/categories/1/status
Authorization: Bearer <token>
{ "active": true }
→ Expect 200 OK. active = true.
```

### Step 9: Delete Category
```
DELETE http://localhost:8080/api/admin/categories/1
Authorization: Bearer <token>
→ Expect 204 No Content.
```

### Step 10: Verify Deletion
```
GET http://localhost:8080/api/categories/1
→ Expect 404 Not Found.
```

### Step 11: Test RBAC — CUSTOMER/VENDOR forbidden
Login as a customer or vendor, then:
```
POST http://localhost:8080/api/admin/categories
Authorization: Bearer <customer_or_vendor_token>
{ "name": "Test" }
→ Expect 403 Forbidden.
```

---

## 12. PostgreSQL Verification

After testing, verify the `categories` table in pgAdmin or psql:

```sql
-- Connect to shopstack_db
\c shopstack_db

-- View all categories
SELECT id, name, slug, active, created_at FROM categories ORDER BY id;

-- Count active categories
SELECT COUNT(*) FROM categories WHERE active = true;

-- Verify deleted categories are gone
SELECT * FROM categories WHERE id = 1;
```

---

## 13. Common Errors & Resolutions

| Error | Cause | Fix |
|-------|-------|-----|
| `409 Conflict` on create | Name already exists (case-insensitive) | Use a different name |
| `400 Bad Request` on create | `name` is blank or `imageUrl` is invalid URL format | Fix the request payload |
| `401 Unauthorized` on admin endpoint | No or expired JWT token | Login again and use the fresh token |
| `403 Forbidden` on admin endpoint | Authenticated as CUSTOMER or VENDOR | Login with an ADMIN account |
| `404 Not Found` on GET /api/categories/{id} | Category is inactive or deleted | Use ADMIN to reactivate it |
| `500 Internal Server Error` | DB connection issue or schema mismatch | Check PostgreSQL is running and `categories` table exists |

---

## 14. Files Created / Modified

### New Files
| File | Purpose |
|------|---------|
| `entity/Category.java` | JPA entity for `categories` table |
| `repository/CategoryRepository.java` | Spring Data JPA repository with custom queries |
| `dto/category/CategoryCreateRequest.java` | DTO for POST create endpoint |
| `dto/category/CategoryUpdateRequest.java` | DTO for PUT update endpoint |
| `dto/category/CategoryStatusRequest.java` | DTO for PATCH status endpoint |
| `dto/category/CategoryResponse.java` | DTO for all category API responses |
| `mapper/CategoryMapper.java` | Entity → Response DTO mapper |
| `service/CategoryService.java` | Business logic, slug generation, uniqueness checks |
| `controller/CategoryController.java` | REST endpoints |
| `exception/CategoryAlreadyExistsException.java` | Custom 409 exception |
| `exception/CategoryNotFoundException.java` | Custom 404 exception |
| `test/controller/CategoryControllerTest.java` | Full MockMvc test suite |

### Modified Files
| File | Change |
|------|--------|
| `exception/GlobalExceptionHandler.java` | Added handlers for `CategoryAlreadyExistsException` (409) and `CategoryNotFoundException` (404) |
| `config/SecurityConfig.java` | Added public `/api/categories/**` and admin `/api/admin/categories/**` security rules |
