# ShopStack Enterprise Multi-Vendor E-Commerce — Detailed Test Cases Catalog

**Document:** Test Cases Specification  
**Project:** ShopStack E-Commerce Platform  
**Coverage:** Milestone 1 through Milestone 4  
**Date:** 2026-09-06  

---

## 1. Authentication & Security Test Cases (Phase 3 & 4)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-AUTH-001** | Auth | Valid Customer Registration | System running, unique email | 1. Send `POST /api/auth/register` with firstName, lastName, email, password, role=CUSTOMER.<br>2. Inspect response. | HTTP 201 Created, JWT token and user info returned, role is CUSTOMER, password excluded. | HTTP 201 Created, JWT token present, password hashed. | **PASS** |
| **TC-AUTH-002** | Auth | Valid Vendor Registration | System running, unique email | 1. Send `POST /api/auth/register` with role=VENDOR and business details.<br>2. Inspect response. | HTTP 201 Created, VendorProfile created in PENDING status. | HTTP 201 Created, VendorProfile linked. | **PASS** |
| **TC-AUTH-003** | Auth | Valid Login | User exists in DB | 1. Send `POST /api/auth/login` with correct email & password. | HTTP 200 OK, returns Bearer token, role, user ID, expiry. | HTTP 200 OK, valid JWT returned. | **PASS** |
| **TC-AUTH-004** | Auth | Invalid Password | User exists in DB | 1. Send `POST /api/auth/login` with correct email and wrong password. | HTTP 401 Unauthorized, "Bad credentials" or invalid password error message. | HTTP 401 Unauthorized returned. | **PASS** |
| **TC-AUTH-005** | Auth | Non-existing User Login | User does not exist | 1. Send `POST /api/auth/login` with unregistered email `ghost@shopstack.com`. | HTTP 401 Unauthorized / 404 User Not Found error. | HTTP 401 Unauthorized returned. | **PASS** |
| **TC-AUTH-006** | Auth | Missing Email | N/A | 1. Send `POST /api/auth/login` or register with null/blank email. | HTTP 400 Bad Request, validation error specifying email is required. | HTTP 400 Bad Request, validation error returned. | **PASS** |
| **TC-AUTH-007** | Auth | Missing Password | N/A | 1. Send `POST /api/auth/login` with null/empty password. | HTTP 400 Bad Request, validation error specifying password is required. | HTTP 400 Bad Request, validation error returned. | **PASS** |
| **TC-AUTH-008** | Auth | Invalid Email Format | N/A | 1. Send `POST /api/auth/register` with `not-an-email`. | HTTP 400 Bad Request, field validation failure for email format. | HTTP 400 Bad Request returned. | **PASS** |
| **TC-AUTH-009** | Auth | Duplicate Email Registration | Email already registered | 1. Register user with existing email address. | HTTP 400 / 409 Conflict, "Email is already registered" message. | HTTP 400 Bad Request, duplicate email rejected. | **PASS** |
| **TC-AUTH-010** | Auth | Protected Endpoint Without Token | Authenticated endpoint | 1. Call `GET /api/profile` or `GET /api/cart` without `Authorization` header. | HTTP 401 Unauthorized. | HTTP 401 Unauthorized returned. | **PASS** |
| **TC-AUTH-011** | Auth | Invalid / Expired JWT | Token expired or corrupt | 1. Call protected endpoint with malformed Bearer token `Bearer invalid_xyz`. | HTTP 401 Unauthorized. | HTTP 401 Unauthorized returned. | **PASS** |
| **TC-AUTH-012** | Auth | Staff Self-Registration Block | Unauthenticated | 1. Send `POST /api/auth/register` with role=WAREHOUSE_STAFF. | HTTP 400 Bad Request / 403 Forbidden; staff creation allowed only by Admin. | HTTP 400 Bad Request, role restricted. | **PASS** |

---

## 2. Role-Based Access Control (RBAC) Matrix Test Cases (Phase 4)

| Test ID | Role | Target Endpoint | HTTP Method | Expected Status | Actual Status | Result |
|---|---|---|---|---|---|---|
| **TC-RBAC-001** | CUSTOMER | `/api/admin/users` | GET | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-002** | CUSTOMER | `/api/admin/warehouses` | POST | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-003** | CUSTOMER | `/api/vendor/products` | POST | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-004** | CUSTOMER | `/api/warehouse/overview` | GET | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-005** | CUSTOMER | `/api/cart` | GET | 200 OK | 200 OK | **PASS** |
| **TC-RBAC-006** | VENDOR | `/api/admin/dashboard/stats` | GET | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-007** | VENDOR | `/api/warehouse/inventory/adjust` | POST | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-008** | VENDOR | `/api/vendor/products` | POST / GET | 200 OK / 201 Created | 200 OK / 201 Created | **PASS** |
| **TC-RBAC-009** | WAREHOUSE_STAFF | `/api/admin/coupons` | POST | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-010** | WAREHOUSE_STAFF | `/api/admin/users` | GET | 403 Forbidden | 403 Forbidden | **PASS** |
| **TC-RBAC-011** | WAREHOUSE_STAFF | `/api/warehouse/overview` | GET | 200 OK | 200 OK | **PASS** |
| **TC-RBAC-012** | WAREHOUSE_STAFF | `/api/warehouse/orders/fulfillment-queue` | GET | 200 OK | 200 OK | **PASS** |
| **TC-RBAC-013** | ADMIN | `/api/admin/dashboard/stats` | GET | 200 OK | 200 OK | **PASS** |
| **TC-RBAC-014** | ADMIN | `/api/admin/users/staff` | POST | 201 Created | 201 Created | **PASS** |
| **TC-RBAC-015** | ADMIN | `/api/admin/warehouses` | GET / POST | 200 OK / 201 Created | 200 OK / 201 Created | **PASS** |

---

## 3. Customer & Shopping Experience Test Cases (Phase 5 & 9)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-CUST-001** | Catalog | Browse Products Paginated | Active products exist | 1. Send `GET /api/products?page=0&size=10`. | HTTP 200 OK, paginated list of active products with vendor, category, and price details. | HTTP 200 OK, paginated payload returned. | **PASS** |
| **TC-CUST-002** | Catalog | Search Products by Keyword | Products with "Shirt" exist | 1. Send `GET /api/products/search?keyword=Shirt`. | HTTP 200 OK, list of matching products filtered by name or description. | HTTP 200 OK, matching items returned. | **PASS** |
| **TC-CUST-003** | Catalog | Filter Products by Category | Category slug exists | 1. Send `GET /api/products?category=electronics`. | HTTP 200 OK, products belonging exclusively to category returned. | HTTP 200 OK, categorized products returned. | **PASS** |
| **TC-CART-001** | Cart | Add In-Stock Product | Product stock > 5 | 1. Send `POST /api/cart/items` with productId=1, quantity=2. | HTTP 200/201, product added, subtotal calculated. | HTTP 200 OK, item added to cart. | **PASS** |
| **TC-CART-002** | Cart | Add Out-of-Stock Product | Product stock = 0 | 1. Send `POST /api/cart/items` with out-of-stock productId. | HTTP 400 Bad Request, "Product is out of stock". | HTTP 400 Bad Request, out of stock rejected. | **PASS** |
| **TC-CART-003** | Cart | Quantity Exceeds Available Stock | Product stock = 3 | 1. Send `POST /api/cart/items` with quantity=10. | HTTP 400 Bad Request, "Requested quantity exceeds available stock". | HTTP 400 Bad Request, excess quantity rejected. | **PASS** |
| **TC-CART-004** | Cart | Update Cart Item Quantity | Item in cart | 1. Send `PUT /api/cart/items/{id}` with quantity=4. | HTTP 200 OK, quantity updated, subtotal recalculated. | HTTP 200 OK, cart item quantity modified. | **PASS** |
| **TC-CART-005** | Cart | Remove Item from Cart | Item in cart | 1. Send `DELETE /api/cart/items/{id}`. | HTTP 200 OK / 204 No Content, item removed from cart. | HTTP 200 OK, item deleted. | **PASS** |
| **TC-CART-006** | Cart | Clear Entire Cart | Cart has items | 1. Send `DELETE /api/cart/clear`. | HTTP 200 OK, cart becomes empty. | HTTP 200 OK, cart emptied. | **PASS** |
| **TC-WISH-001** | Wishlist | Add Product to Wishlist | Authenticated customer | 1. Send `POST /api/wishlist/{productId}`. | HTTP 200/201, product added to customer's wishlist. | HTTP 200 OK, product in wishlist. | **PASS** |
| **TC-WISH-002** | Wishlist | Duplicate Wishlist Item | Item already in wishlist | 1. Send `POST /api/wishlist/{productId}` for same item. | Handled gracefully without DB constraint violation. | HTTP 200 OK, idempotent addition. | **PASS** |
| **TC-WISH-003** | Wishlist | Remove from Wishlist | Item in wishlist | 1. Send `DELETE /api/wishlist/{productId}`. | HTTP 200 OK, item removed. | HTTP 200 OK, item removed. | **PASS** |

---

## 4. Vendor Module & Catalog Management (Phase 6 & 7)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-VEND-001** | Vendor | Vendor Profile Retrieval | Approved vendor login | 1. Send `GET /api/vendor/profile`. | HTTP 200 OK, returns business name, GST/tax ID, contact info. | HTTP 200 OK, vendor profile returned. | **PASS** |
| **TC-VEND-002** | Vendor | Create Product with Valid Data | Approved vendor | 1. Send `POST /api/vendor/products` with SKU, title, price, categoryId, initialStock. | HTTP 201 Created, product created with status ACTIVE/PENDING. | HTTP 201 Created, product saved. | **PASS** |
| **TC-VEND-003** | Vendor | Create Product with Negative Price | Approved vendor | 1. Send `POST /api/vendor/products` with price=-50.00. | HTTP 400 Bad Request, validation failure on price. | HTTP 400 Bad Request returned. | **PASS** |
| **TC-VEND-004** | Vendor | Update Own Product | Product belongs to vendor | 1. Send `PUT /api/vendor/products/{id}` with updated title/price. | HTTP 200 OK, product updated successfully. | HTTP 200 OK, changes persisted. | **PASS** |
| **TC-VEND-005** | Vendor | Cross-Vendor Product Modification | Product belongs to Vendor A | 1. Vendor B sends `PUT /api/vendor/products/{VendorA_ProductId}`. | HTTP 403 Forbidden / 404 Not Found, cross-vendor access blocked. | HTTP 403 Forbidden returned. | **PASS** |
| **TC-CAT-001** | Category | Admin Create Category | Admin role | 1. Send `POST /api/admin/categories` with name="Smart Home", slug="smart-home". | HTTP 201 Created, category saved with active=true. | HTTP 201 Created, category saved. | **PASS** |
| **TC-CAT-002** | Category | Public Active Categories List | Categories exist | 1. Send `GET /api/categories`. | HTTP 200 OK, list of active categories returned. | HTTP 200 OK, active list returned. | **PASS** |
| **TC-CAT-003** | Category | Admin Toggle Category Status | Admin role | 1. Send `PATCH /api/admin/categories/{id}/status?active=false`. | HTTP 200 OK, category deactivated. | HTTP 200 OK, status toggled. | **PASS** |

---

## 5. Inventory & Stock Movement Test Cases (Phase 8)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-INV-001** | Inventory | Stock Increase / Receipt | Inventory record exists | 1. Send stock adjustment with type=RECEIPT, quantity=+50. | Total stock and available stock incremented by 50, `StockMovement` audit logged. | Stock incremented, audit record created. | **PASS** |
| **TC-INV-002** | Inventory | Stock Reservation upon Order | In-stock product (10 units) | 1. Place order for 2 units. | Available stock becomes 8, reserved stock becomes 2. | Available stock decremented, reserved incremented. | **PASS** |
| **TC-INV-003** | Inventory | Prevent Negative Stock Adjustment | Available stock = 5 | 1. Send adjustment with type=SCRAP, quantity=10. | HTTP 400 Bad Request, "Insufficient stock for reduction". | HTTP 400 Bad Request, rejected. | **PASS** |
| **TC-INV-004** | Inventory | Stock Movement Audit Immutability | Stock operations executed | 1. Query `GET /api/warehouse/inventory/movements`. | Immutable chronologically ordered history of movements with reason, type, and user ID. | List of stock movements returned. | **PASS** |
| **TC-INV-005** | Inventory | Optimistic Locking on Concurrent Edits | Inventory entity has @Version | 1. Simulate concurrent conflicting stock updates. | `ObjectOptimisticLockingFailureException` caught, prevents data corruption. | Optimistic locking verified in service test. | **PASS** |

---

## 6. Coupon & Promotions Engine Test Cases (Phase 14)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-CPN-001** | Coupon | Percentage Coupon Validation | Active 10% coupon, min order $50 | 1. Validate on cart total $100.00. | Discount = $10.00, applicable total = $90.00. | Discount calculated correctly. | **PASS** |
| **TC-CPN-002** | Coupon | Fixed Amount Coupon Validation | Active $20 coupon, min order $60 | 1. Validate on cart total $100.00. | Discount = $20.00, applicable total = $80.00. | Discount calculated correctly. | **PASS** |
| **TC-CPN-003** | Coupon | Expired Coupon Application | Coupon with past expiry date | 1. Send `POST /api/coupons/validate` with expired code. | HTTP 400 Bad Request, "Coupon has expired". | HTTP 400 Bad Request, rejected. | **PASS** |
| **TC-CPN-004** | Coupon | Minimum Order Threshold Not Met | Coupon requires $100 min order | 1. Validate on cart total $45.00. | HTTP 400 Bad Request, "Order does not meet minimum order amount". | HTTP 400 Bad Request, threshold enforced. | **PASS** |
| **TC-CPN-005** | Coupon | Scoped Scope: Specific Categories | Scope=SPECIFIC_CATEGORIES | 1. Apply coupon on cart with items from non-applicable category. | Discount = 0 / Coupon invalid for selected items. | Non-applicable category rejected. | **PASS** |
| **TC-CPN-006** | Coupon | Usage Limit Exhaustion | Usage limit reached | 1. Apply coupon whose currentUsage >= maxUsage. | HTTP 400 Bad Request, "Coupon usage limit has been reached". | HTTP 400 Bad Request, limit enforced. | **PASS** |

---

## 7. Checkout, Payment & Order Lifecycle Test Cases (Phase 10 & 11)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-CHK-001** | Checkout | Empty Cart Checkout | Cart is empty | 1. Send `POST /api/orders/checkout` with empty cart. | HTTP 400 Bad Request, "Cart cannot be empty". | HTTP 400 Bad Request, rejected. | **PASS** |
| **TC-CHK-002** | Checkout | Valid Checkout with Address & Coupon | Valid cart items | 1. Send checkout request with shipping address and validated coupon. | HTTP 201 Created, Order created in PENDING status, cart cleared. | HTTP 201 Created, Order created. | **PASS** |
| **TC-PAY-001** | Payment | Payment Initiation | PENDING order exists | 1. Send `POST /api/payments/create/{orderId}`. | Payment entity created in INITIATED/PENDING status, returns gateway order ID. | Payment initialized. | **PASS** |
| **TC-PAY-002** | Payment | Payment Verification Signature | Payment initiated | 1. Send `POST /api/payments/verify` with valid orderId, paymentId, signature. | Payment marked SUCCESS, Order transitions to CONFIRMED. | Payment verified, order confirmed. | **PASS** |
| **TC-PAY-003** | Payment | Payment Verification Invalid Signature | Tampered signature | 1. Send `POST /api/payments/verify` with fraudulent signature. | HTTP 400 Bad Request, "Invalid payment signature", payment marked FAILED. | Signature verification failure caught. | **PASS** |
| **TC-ORD-001** | Order | Valid Lifecycle State Machine | PENDING order | 1. PENDING $\to$ CONFIRMED $\to$ PROCESSING $\to$ SHIPPED $\to$ DELIVERED. | Order status updates cleanly at each stage. | Valid transitions accepted. | **PASS** |
| **TC-ORD-002** | Order | Invalid State Transition | DELIVERED order | 1. Attempt transition from DELIVERED $\to$ CONFIRMED. | HTTP 400 Bad Request, "Invalid order status transition". | HTTP 400 Bad Request, transition rejected. | **PASS** |
| **TC-ORD-003** | Order | Order Cancellation | PENDING / CONFIRMED order | 1. Customer cancels order before fulfillment. | Order marked CANCELLED, reserved stock released back to available. | Order cancelled, stock restored. | **PASS** |

---

## 8. Warehouse Operations & Fulfillment Test Cases (Phase 12)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-WH-001** | Warehouse | Dashboard KPI Overview | Warehouse Staff login | 1. Send `GET /api/warehouse/overview`. | HTTP 200 OK, returns pendingAllocations, pickingQueue, readyToShip, lowStockCount. | HTTP 200 OK, KPI response returned. | **PASS** |
| **TC-WH-002** | Warehouse | Order Allocation to Facility | CONFIRMED order | 1. Send `POST /api/warehouse/allocations` with orderId, warehouseId. | Allocation record created with status ALLOCATED. | Allocation created successfully. | **PASS** |
| **TC-WH-003** | Warehouse | Item Picking Confirmation | ALLOCATED order | 1. Send `POST /api/warehouse/picking/{id}/complete`. | Status transitions to PICKED. | Status updated to PICKED. | **PASS** |
| **TC-WH-004** | Warehouse | Item Packing Confirmation | PICKED order | 1. Send `POST /api/warehouse/packing/{id}/complete` with package weight/dimensions. | Status transitions to PACKED. | Status updated to PACKED. | **PASS** |
| **TC-WH-005** | Warehouse | Carrier Dispatch & Tracking Generation | PACKED order | 1. Send `POST /api/shipments` with carrier name, tracking number. | Shipment created in SHIPPED status, tracking event created, customer notified. | Shipment created with tracking number. | **PASS** |
| **TC-WH-006** | Warehouse | Shipment Tracking Public Lookup | Shipment dispatched | 1. Send `GET /api/shipments/tracking/{trackingNumber}`. | HTTP 200 OK, chronological tracking timeline returned. | Tracking timeline returned. | **PASS** |

---

## 9. Returns & Refund Management Test Cases (Phase 13)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-RET-001** | Returns | Return on DELIVERED Order | Order status = DELIVERED | 1. Customer submits `POST /api/returns` with reason, itemId, quantity. | HTTP 201 Created, ReturnRequest created in REQUESTED status. | HTTP 201 Created, return registered. | **PASS** |
| **TC-RET-002** | Returns | Return on Non-DELIVERED Order | Order status = PROCESSING | 1. Customer submits `POST /api/returns` for non-delivered order. | HTTP 400 Bad Request, "Returns are only allowed for DELIVERED status". | HTTP 400 Bad Request, rejected. | **PASS** |
| **TC-RET-003** | Returns | Duplicate Return Request | Active return request exists | 1. Submit second return request for same order. | HTTP 409 Conflict / 400 Bad Request, duplicate rejected. | HTTP 409 Conflict returned. | **PASS** |
| **TC-RET-004** | Returns | Admin Approval of Return | Return status = REQUESTED | 1. Admin sends `PATCH /api/returns/{id}/approve`. | Return status becomes APPROVED, warehouse notified for intake. | Status updated to APPROVED. | **PASS** |
| **TC-RET-005** | Returns | Warehouse QC - Accepted Return | Return arrived at warehouse | 1. Warehouse inspects and submits QC status ACCEPTED. | Returned stock added back to sellable inventory, refund triggered. | Stock returned to sellable pool. | **PASS** |
| **TC-RET-006** | Returns | Warehouse QC - Damaged Return | Return arrived damaged | 1. Warehouse submits QC status DAMAGED/DEFECTIVE. | Stock routed to DAMAGED quarantine (not added to sellable stock). | Quarantine stock logged, sellable unchanged. | **PASS** |
| **TC-REF-001** | Refund | Refund Generation upon QC Acceptance | QC status = ACCEPTED | 1. System records refund record with refund amount, orderId. | Refund status becomes COMPLETED/PROCESSED, customer notified. | Refund ledger entry recorded. | **PASS** |

---

## 10. Admin & Reporting Test Cases (Phase 15)

| Test ID | Module | Scenario | Precondition | Test Steps | Expected Result | Actual Result | Status |
|---|---|---|---|---|---|---|---|
| **TC-ADM-001** | Admin | Marketplace Summary Stats | Admin login | 1. Send `GET /api/admin/dashboard/stats`. | Total users, total vendors, total orders, GMV returned. | HTTP 200 OK, summary stats returned. | **PASS** |
| **TC-ADM-002** | Admin | Create Warehouse Staff Account | Admin login | 1. Send `POST /api/admin/users/staff` with staff details. | HTTP 201 Created, user role is `WAREHOUSE_STAFF`. | Staff user created. | **PASS** |
| **TC-ADM-003** | Admin | System Health Monitoring | Admin login | 1. Send `GET /api/admin/system/health`. | Database, memory, and service status returned. | HTTP 200 OK, health report returned. | **PASS** |
| **TC-ADM-004** | Admin | Vendor Status Management | Pending vendor profile | 1. Send `PATCH /api/admin/vendors/{id}/status?status=APPROVED`. | Vendor status becomes APPROVED, vendor notified. | Vendor status updated. | **PASS** |
