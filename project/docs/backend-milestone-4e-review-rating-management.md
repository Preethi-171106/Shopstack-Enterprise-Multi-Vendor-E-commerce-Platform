# Backend Milestone 4E — Review & Rating Management

## 1. Milestone Completion Report

The Review & Rating Management module is fully implemented with customer, public, vendor, and admin capabilities. Customers can create, edit, delete, and view their own reviews. Public users can view approved product reviews, average ratings, and rating distribution. Vendors can view reviews for their own products and view rating summaries. Admins can view all reviews, approve, reject, and delete inappropriate reviews. Business rules enforce verified-purchaser-only reviews, one review per product per customer, 1-5 star ratings, no reviews on cancelled orders, and automatic recalculation of product averageRating and totalReviews whenever a review is created, approved, rejected, or deleted. All 160 tests pass (94 previous + 66 new).

---

## 2. Files Created

### Review Module

| File | Purpose |
|------|---------|
| `review/entity/Review.java` | Review JPA entity with unique constraint on (user_id, product_id) |
| `review/enums/ReviewStatus.java` | Review status enum: PENDING, APPROVED, REJECTED |
| `review/repository/ReviewRepository.java` | 12 query methods including aggregate queries for rating distribution |
| `review/dto/CreateReviewRequest.java` | Create review request with validation (rating 1-5, required fields) |
| `review/dto/UpdateReviewRequest.java` | Update review request with validation |
| `review/dto/ReviewResponse.java` | Review response record with user and product details |
| `review/dto/ReviewSummaryResponse.java` | Rating summary with average, total, and star distribution |
| `review/mapper/ReviewMapper.java` | Static mapper from entity to response DTO |
| `review/service/ReviewService.java` | Core business logic with all business rules |
| `review/controller/ReviewController.java` | 6 customer + public endpoints |
| `review/controller/VendorReviewController.java` | 3 vendor endpoints |
| `review/controller/AdminReviewController.java` | 4 admin endpoints |

### Exception

| File | Purpose |
|------|---------|
| `common/exception/ConflictException.java` | 409 Conflict exception for duplicate reviews |

### Tests

| File | Tests | Description |
|------|-------|-------------|
| `review/service/ReviewServiceTest.java` | 30 | Service: create, update, delete, ownership, duplicate, cancelled order, product not in order, approve, reject, recalculation, vendor access, summary |
| `review/controller/ReviewControllerTest.java` | 17 | Customer + public: 201, 200, 204, 400, 401, 403, 404, 409 |
| `review/controller/VendorReviewControllerTest.java` | 7 | Vendor: 200, 401, 403, 404 |
| `review/controller/AdminReviewControllerTest.java` | 9 | Admin: 200, 204, 401, 403, 404 |

---

## 3. Files Modified

| File | Change |
|------|--------|
| `product/entity/Product.java` | Added `averageRating` (Double) and `totalReviews` (Long) fields with @Builder.Default |
| `order/repository/OrderRepository.java` | Added `countDeliveredOrderItemsByUserAndProduct()` for verified purchaser check |
| `security/SecurityConfig.java` | Added `GET /api/reviews/product/**` as permitAll for public access |
| `common/exception/GlobalExceptionHandler.java` | Added ConflictException handler returning 409 |
| `walkthrough.md` | Updated with Milestone 4E section |

---

## 4. Database Schema

### New Table: reviews

```
reviews
├── id (PK, BIGINT, auto-generated)
├── user_id (FK → users.id, NOT NULL)
├── product_id (FK → products.id, NOT NULL)
├── order_id (FK → orders.id, NOT NULL)
├── rating (INT, NOT NULL, 1-5)
├── title (VARCHAR(255), NOT NULL)
├── comment (VARCHAR(2000), NOT NULL)
├── status (VARCHAR(10), NOT NULL, default 'PENDING')
├── created_at (TIMESTAMP, NOT NULL)
├── updated_at (TIMESTAMP)
└── UNIQUE CONSTRAINT (user_id, product_id) — one review per user per product
```

### Modified Table: products

```
products (added columns)
├── average_rating (DECIMAL(3,2), default 0.00)
└── total_reviews (BIGINT, NOT NULL, default 0)
```

---

## 5. Entity Relationship Diagram

```
┌──────────┐     ┌──────────┐     ┌──────────────┐
│  users   │     │ products  │    │   orders     │
│ id (PK)  │◄──┐ │ id (PK)   │    │ id (PK)      │
└──────────┘   │ │ average_  │    │ customer_id  │──► users
               │ │ rating    │    │ vendor_id    │──► vendors
               │ │ total_    │    │ status       │
               │ │ reviews   │    └──────────────┘
               │ └──────────┘          │
               │      ▲                │
               │      │                │
               │  ┌───┴────────┐       │
               └──│  reviews   │───────┘
                  │ id (PK)    │
                  │ user_id    │──► users
                  │ product_id │──► products
                  │ order_id    │──► orders
                  │ rating     │
                  │ title      │
                  │ comment    │
                  │ status     │
                  │ created_at │
                  │ updated_at │
                  └────────────┘
                  UNIQUE(user_id, product_id)
```

---

## 6. Review Workflow

### Customer Review Flow

1. Customer purchases a product (order with DELIVERED/CONFIRMED/SHIPPED status)
2. Customer submits a review with rating (1-5), title, and comment
3. Service validates:
   - Order belongs to the customer
   - Order is not cancelled
   - Product was in the order
   - No existing review for this user+product
4. Review is saved with PENDING status
5. Admin reviews and approves/rejects the review
6. Upon approval, product's averageRating and totalReviews are recalculated

### Admin Moderation Flow

1. Admin views all reviews (paginated, all statuses)
2. Admin approves a review → status changes to APPROVED → product rating recalculated
3. Admin rejects a review → status changes to REJECTED → product rating recalculated
4. Admin deletes a review → review removed → product rating recalculated

### Vendor Review Flow

1. Vendor requests reviews → controller resolves vendor ID from JWT
2. Service returns only reviews for products owned by this vendor
3. Vendor can view rating summary for their own products only

### Public Review Flow

1. Any user (no authentication) can view approved reviews for a product
2. Any user can view the rating summary (average, total, distribution)
3. Only APPROVED reviews are visible publicly

---

## 7. Rating Calculation Logic

### When Review is Created

- Review is saved with PENDING status
- Product rating is NOT recalculated (pending reviews don't count)

### When Review is Approved

- Review status changes to APPROVED
- Product's averageRating and totalReviews are recalculated:
  ```
  averageRating = AVG(rating) WHERE status = APPROVED
  totalReviews = COUNT(*) WHERE status = APPROVED
  ```
- Both values are saved to the Product entity

### When Review is Rejected

- Review status changes to REJECTED
- Product's averageRating and totalReviews are recalculated (excludes rejected)

### When Review is Deleted (by customer or admin)

- Review is removed from database
- Product's averageRating and totalReviews are recalculated

### When Review is Updated (by customer)

- Review content and rating are updated
- Review status resets to PENDING (requires re-approval)
- Product rating is NOT recalculated until re-approved

### Rating Distribution Calculation

```sql
SELECT rating, COUNT(*)
FROM reviews
WHERE product_id = ? AND status = 'APPROVED'
GROUP BY rating
```

Results are mapped to 5-star, 4-star, 3-star, 2-star, 1-star counts.

---

## 8. API Endpoint Table

### Customer Endpoints (`/api/reviews`)

| Method | Endpoint | Description | Auth | Status Codes |
|--------|----------|-------------|------|--------------|
| POST | `/api/reviews` | Create review | CUSTOMER | 201, 400, 401, 409 |
| PUT | `/api/reviews/{id}` | Update own review | CUSTOMER | 200, 400, 401, 403, 404 |
| DELETE | `/api/reviews/{id}` | Delete own review | CUSTOMER | 204, 401, 403, 404 |
| GET | `/api/reviews/my` | View own reviews | CUSTOMER | 200, 401 |
| GET | `/api/reviews/product/{productId}` | View product reviews | PUBLIC | 200, 404 |
| GET | `/api/reviews/product/{productId}/summary` | View rating summary | PUBLIC | 200, 404 |

### Vendor Endpoints (`/api/vendor/reviews`)

| Method | Endpoint | Description | Auth | Status Codes |
|--------|----------|-------------|------|--------------|
| GET | `/api/vendor/reviews` | View reviews for own products | VENDOR | 200, 401, 403, 404 |
| GET | `/api/vendor/reviews/product/{productId}` | View reviews for specific product | VENDOR | 200, 401, 403, 404 |
| GET | `/api/vendor/reviews/product/{productId}/summary` | View rating summary for own product | VENDOR | 200, 401, 403, 404 |

### Admin Endpoints (`/api/admin/reviews`)

| Method | Endpoint | Description | Auth | Status Codes |
|--------|----------|-------------|------|--------------|
| GET | `/api/admin/reviews` | View all reviews | ADMIN | 200, 401, 403 |
| PATCH | `/api/admin/reviews/{id}/approve` | Approve review | ADMIN | 200, 401, 403, 404 |
| PATCH | `/api/admin/reviews/{id}/reject` | Reject review | ADMIN | 200, 401, 403, 404 |
| DELETE | `/api/admin/reviews/{id}` | Delete review | ADMIN | 204, 401, 403, 404 |

---

## 9. Security Rules

| Role | Access |
|------|--------|
| `ROLE_CUSTOMER` | Create, edit, delete own reviews; view own reviews; view public product reviews |
| `ROLE_VENDOR` | View reviews for own products only; view rating summary for own products |
| `ROLE_ADMIN` | View all reviews; approve, reject, delete any review |
| `ROLE_WAREHOUSE_STAFF` | No access to review endpoints |
| Unauthenticated | Can view approved product reviews and rating summaries (GET only) |

### Security Configuration

- `GET /api/reviews/product/**` → permitAll (public access)
- `/api/admin/reviews/**` → hasRole("ADMIN")
- `/api/vendor/reviews/**` → hasRole("VENDOR")
- All other `/api/reviews/**` → authenticated

### Ownership Enforcement

- **Customer**: `review.user.id == currentUserId` checked in service for update/delete
- **Vendor**: `product.vendor.id == vendorId` checked in service for product summary
- **Vendor ID resolution**: JWT userId → `vendorRepository.findByUserId()` → vendor.id

---

## 10. Validation Rules

| Rule | Implementation | Error Code |
|------|----------------|------------|
| Rating between 1-5 | `@Min(1) @Max(5)` on DTO | 400 |
| Title required (max 255) | `@NotBlank @Size(max=255)` | 400 |
| Comment required (max 2000) | `@NotBlank @Size(max=2000)` | 400 |
| ProductId required | `@NotNull` | 400 |
| OrderId required | `@NotNull` | 400 |
| Only verified purchasers | Check order ownership + product in order | 403, 400 |
| One review per product per user | DB unique constraint + `existsByUserIdAndProductId` | 409 |
| No cancelled order reviews | Check `order.status != CANCELLED` | 400 |
| Product must be in order | Check `order.items` contains product | 400 |
| Customer can only edit own | `review.user.id == userId` | 403 |
| Customer can only delete own | `review.user.id == userId` | 403 |
| Vendor can only view own products | `product.vendor.id == vendorId` | 403 |

---

## 11. Postman Testing Guide

### Setup

1. **Login as Customer**
   ```
   POST /api/auth/login
   Body: { "email": "customer@shopstack.com", "password": "password" }
   ```

2. **Login as Vendor**
   ```
   POST /api/auth/login
   Body: { "email": "vendor@shopstack.com", "password": "password" }
   ```

3. **Login as Admin**
   ```
   POST /api/auth/login
   Body: { "email": "admin@shopstack.com", "password": "password" }
   ```

### Customer Tests

4. **Create Review**
   ```
   POST /api/reviews
   Authorization: Bearer <customer-token>
   Body: {
     "productId": 10,
     "orderId": 100,
     "rating": 5,
     "title": "Great product!",
     "comment": "Exceeded my expectations."
   }
   ```
   Expected: `201 Created`

5. **Create Duplicate Review**
   ```
   POST /api/reviews (same productId as above)
   Authorization: Bearer <customer-token>
   ```
   Expected: `409 Conflict`

6. **Create Review with Invalid Rating**
   ```
   POST /api/reviews
   Authorization: Bearer <customer-token>
   Body: { "productId": 10, "orderId": 100, "rating": 0, "title": "Bad", "comment": "Test" }
   ```
   Expected: `400 Bad Request`

7. **Update Own Review**
   ```
   PUT /api/reviews/1
   Authorization: Bearer <customer-token>
   Body: { "rating": 4, "title": "Updated", "comment": "Updated comment" }
   ```
   Expected: `200 OK`

8. **Update Other User's Review**
   ```
   PUT /api/reviews/2 (review owned by another user)
   Authorization: Bearer <customer-token>
   ```
   Expected: `403 Forbidden`

9. **Delete Own Review**
   ```
   DELETE /api/reviews/1
   Authorization: Bearer <customer-token>
   ```
   Expected: `204 No Content`

10. **Get My Reviews**
    ```
    GET /api/reviews/my?page=0&size=10
    Authorization: Bearer <customer-token>
    ```
    Expected: `200 OK`

### Public Tests

11. **Get Product Reviews (No Auth)**
    ```
    GET /api/reviews/product/10?page=0&size=10
    ```
    Expected: `200 OK` (only approved reviews)

12. **Get Rating Summary (No Auth)**
    ```
    GET /api/reviews/product/10/summary
    ```
    Expected: `200 OK` with averageRating, totalReviews, and star distribution

13. **Get Reviews for Nonexistent Product**
    ```
    GET /api/reviews/product/999
    ```
    Expected: `404 Not Found`

### Vendor Tests

14. **Get Reviews for Own Products**
    ```
    GET /api/vendor/reviews?page=0&size=10
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`

15. **Get Reviews for Specific Product**
    ```
    GET /api/vendor/reviews/product/10
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`

16. **Get Rating Summary for Own Product**
    ```
    GET /api/vendor/reviews/product/10/summary
    Authorization: Bearer <vendor-token>
    ```
    Expected: `200 OK`

17. **Get Rating Summary for Other Vendor's Product**
    ```
    GET /api/vendor/reviews/product/20 (owned by another vendor)
    Authorization: Bearer <vendor-token>
    ```
    Expected: `403 Forbidden`

### Admin Tests

18. **Get All Reviews**
    ```
    GET /api/admin/reviews?page=0&size=10
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK` (all statuses)

19. **Approve Review**
    ```
    PATCH /api/admin/reviews/1/approve
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK` with status APPROVED

20. **Reject Review**
    ```
    PATCH /api/admin/reviews/2/reject
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK` with status REJECTED

21. **Delete Review**
    ```
    DELETE /api/admin/reviews/1
    Authorization: Bearer <admin-token>
    ```
    Expected: `204 No Content`

22. **Approve Nonexistent Review**
    ```
    PATCH /api/admin/reviews/999/approve
    Authorization: Bearer <admin-token>
    ```
    Expected: `404 Not Found`

### Negative Tests

23. **Customer Accessing Admin Reviews**
    ```
    GET /api/admin/reviews
    Authorization: Bearer <customer-token>
    ```
    Expected: `403 Forbidden`

24. **Customer Accessing Vendor Reviews**
    ```
    GET /api/vendor/reviews
    Authorization: Bearer <customer-token>
    ```
    Expected: `403 Forbidden`

25. **Unauthenticated Create Review**
    ```
    POST /api/reviews (no auth header)
    ```
    Expected: `401 Unauthorized`

26. **Review Cancelled Order Product**
    ```
    POST /api/reviews
    Authorization: Bearer <customer-token>
    Body: { "productId": 10, "orderId": 200 (cancelled order), "rating": 5, "title": "Test", "comment": "Test" }
    ```
    Expected: `400 Bad Request`

---

## 12. Test Summary

```
Tests run: 160
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Test Breakdown

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `ReviewServiceTest` | 30 | Create (valid, user/product/order not found, wrong owner, cancelled, product not in order, duplicate), update (own, not owner, not found), delete (own, not owner, not found), get my reviews, get product reviews (approved only, not found), summary (with data, no data, not found), admin get all, approve (valid, not found), reject (valid, not found), admin delete (valid, not found), vendor reviews, vendor product reviews (not found), vendor summary (own, not own, not found) |
| `ReviewControllerTest` | 17 | Create (201, 409 duplicate, 400 invalid rating, 400 missing fields, 400 cancelled order), update (200, 403 not owner, 404 not found), delete (204, 403, 404), my reviews (200), public product reviews (200, 404), public summary (200, 404), unauthenticated (401) |
| `VendorReviewControllerTest` | 7 | Get own reviews (200), get product reviews (200), get summary (200), not own product (403), vendor not found (404), customer 403, unauthenticated 401 |
| `AdminReviewControllerTest` | 9 | Get all (200), approve (200, 404), reject (200, 404), delete (204, 404), customer 403, unauthenticated 401 |
| **Previous tests** | 94 | All notification + analytics tests continue passing |
| **Total** | **160** | |
