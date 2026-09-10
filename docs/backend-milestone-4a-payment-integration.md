# Backend Milestone 4A — Payment Integration

**ShopStack Enterprise Multi-Vendor E-Commerce Platform**
**Module:** Payment Integration (Razorpay)
**Date:** 2026-08-01

---

## 1. Objective

Implement full payment lifecycle management for the ShopStack platform using the Razorpay payment gateway. This milestone enables customers to create payments for orders, verify payment signatures, view payment details, and request refunds — while admins can view all platform transactions.

---

## 2. Real-World Payment Concept

In an e-commerce platform, **payments** bridge the gap between a placed order and fulfilled delivery:

| Role | Interaction |
|------|-------------|
| **Customer** | Initiates payment for an order, completes Razorpay checkout, verifies signature, views payment status, requests refunds |
| **Admin** | Views all payment transactions across the platform, monitors payment health |
| **Vendor** | No direct payment access (vendors receive payouts separately) |
| **Warehouse** | No payment access |

**Key Rules:**
- Each order can have exactly **one** payment (One-to-One)
- Payments are created via **Razorpay Order API** with amount in smallest currency unit (paise)
- Verification uses **HMAC-SHA256** signature validation
- Only **SUCCESS** payments can be refunded
- Refunding a payment sets order status to **CANCELLED**
- Credentials are **never hardcoded** — loaded from environment variables

---

## 3. Architecture

The implementation follows the established ShopStack layered pattern:

```
HTTP Request
    ↓
PaymentController       (/api/payments/*)
AdminPaymentController  (/api/admin/payments/*)
    ↓
PaymentService          (interface)
PaymentServiceImpl      (business logic, Razorpay integration, ownership checks)
    ↓
PaymentRepository       (Spring Data JPA, custom query methods)
    ↓
PostgreSQL              (payments table, FK → orders)
    ↑
PaymentMapper           (Payment entity → PaymentResponse DTO)
```

**Package Structure:**

```
com.shopstack
├── entity/
│   ├── Payment.java                                [EXISTING]
│   ├── PaymentStatus.java                          [EXISTING]
│   ├── PaymentMethod.java                          [EXISTING]
│   └── PaymentGateway.java                         [EXISTING]
├── repository/
│   └── PaymentRepository.java                      [EXISTING]
├── dto/
│   └── payment/
│       ├── PaymentCreateRequest.java               [EXISTING]
│       ├── PaymentVerifyRequest.java                [EXISTING]
│       ├── PaymentRefundRequest.java                [EXISTING]
│       └── PaymentResponse.java                    [EXISTING]
├── mapper/
│   └── PaymentMapper.java                          [EXISTING]
├── service/
│   ├── PaymentService.java                         [EXISTING — interface]
│   └── PaymentServiceImpl.java                     [EXISTING — implementation]
├── controller/
│   ├── PaymentController.java                      [EXISTING]
│   └── AdminPaymentController.java                 [EXISTING]
├── exception/
│   ├── PaymentNotFoundException.java               [EXISTING]
│   ├── DuplicatePaymentException.java              [EXISTING]
│   ├── PaymentVerificationException.java           [EXISTING]
│   ├── RefundNotAllowedException.java              [EXISTING]
│   └── GlobalExceptionHandler.java                 [MODIFIED — all 4 exceptions integrated]
└── config/
    ├── RazorpayConfig.java                         [EXISTING]
    └── SecurityConfig.java                         [MODIFIED — payment RBAC rules]
```

---

## 4. Database Schema

**Table: `payments`**

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | BIGSERIAL | PRIMARY KEY | Auto-incremented |
| `order_id` | BIGINT | NOT NULL, UNIQUE, FK → `orders(id)` | One-to-One with Order |
| `payment_id` | VARCHAR(100) | NOT NULL, UNIQUE | Internal payment identifier (PAY-XXXX) |
| `gateway_order_id` | VARCHAR(100) | NULLABLE | Razorpay Order ID (order_xxx) |
| `gateway_transaction_id` | VARCHAR(100) | NULLABLE | Razorpay Payment ID (pay_xxx) |
| `amount` | DECIMAL(10,2) | NOT NULL | Payment amount |
| `currency` | VARCHAR(10) | NOT NULL, DEFAULT 'INR' | Currency code |
| `payment_method` | VARCHAR(50) | NOT NULL | CARD, UPI, NET_BANKING, WALLET, COD |
| `gateway` | VARCHAR(50) | NOT NULL, DEFAULT 'RAZORPAY' | Payment gateway |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'CREATED' | Payment lifecycle status |
| `failure_reason` | VARCHAR(500) | NULLABLE | Failure/refund reason |
| `created_at` | TIMESTAMP | NOT NULL | Set on creation |
| `updated_at` | TIMESTAMP | — | Updated on every save |

**Indexes:**

| Index Name | Column(s) | Unique |
|-----------|-----------|--------|
| `idx_payments_order_id` | `order_id` | ✅ |
| `idx_payments_payment_id` | `payment_id` | ✅ |
| `idx_payments_gateway_order_id` | `gateway_order_id` | ❌ |
| `idx_payments_status` | `status` | ❌ |

**Equivalent DDL:**
```sql
CREATE TABLE payments (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    payment_id              VARCHAR(100) NOT NULL UNIQUE,
    gateway_order_id        VARCHAR(100),
    gateway_transaction_id  VARCHAR(100),
    amount                  DECIMAL(10,2) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_method          VARCHAR(50) NOT NULL,
    gateway                 VARCHAR(50) NOT NULL DEFAULT 'RAZORPAY',
    status                  VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    failure_reason          VARCHAR(500),
    created_at              TIMESTAMP NOT NULL,
    updated_at              TIMESTAMP
);

CREATE UNIQUE INDEX idx_payments_order_id ON payments(order_id);
CREATE UNIQUE INDEX idx_payments_payment_id ON payments(payment_id);
CREATE INDEX idx_payments_gateway_order_id ON payments(gateway_order_id);
CREATE INDEX idx_payments_status ON payments(status);
```

> **Note:** Hibernate auto-creates the `payments` table via `spring.jpa.hibernate.ddl-auto=update`.

---

## 5. Enums

### PaymentStatus
```java
PENDING     // Payment initiated but not yet created with gateway
CREATED     // Razorpay order created, awaiting customer payment
SUCCESS     // Payment verified successfully via signature
FAILED      // Signature verification failed
CANCELLED   // Payment cancelled
REFUNDED    // Successful payment refunded
```

### PaymentMethod
```java
CARD         // Credit/Debit card payment
UPI          // Unified Payments Interface
NET_BANKING  // Internet banking
WALLET       // Digital wallet (Paytm, PhonePe, etc.)
COD          // Cash on Delivery
```

### PaymentGateway
```java
RAZORPAY     // Razorpay payment gateway
```

---

## 6. API Endpoints

### 6.1 Customer Endpoints — ROLE_CUSTOMER Required

#### POST /api/payments/create/{orderId} — Create Payment

```
Method:       POST
URL:          http://localhost:8080/api/payments/create/{orderId}
Headers:      Authorization: Bearer <CUSTOMER_JWT>
              Content-Type: application/json

Request Body:
{
  "paymentMethod": "UPI",
  "currency": "INR"
}

Success Response (201 Created):
{
  "id": 1,
  "orderId": 10,
  "orderNumber": "ORD-20240001",
  "paymentId": "PAY-ABC123DEF456GH",
  "gatewayOrderId": "order_OPm1FgT1EXXXXX",
  "amount": 999.99,
  "currency": "INR",
  "paymentMethod": "UPI",
  "gateway": "RAZORPAY",
  "status": "CREATED",
  "createdAt": "2026-08-01T10:30:00",
  "updatedAt": "2026-08-01T10:30:00"
}
```

#### POST /api/payments/verify — Verify Payment Signature

```
Method:       POST
URL:          http://localhost:8080/api/payments/verify
Headers:      Authorization: Bearer <CUSTOMER_JWT>
              Content-Type: application/json

Request Body:
{
  "orderId": 10,
  "razorpayOrderId": "order_OPm1FgT1EXXXXX",
  "razorpayPaymentId": "pay_OPm1FgT1EYYYYY",
  "razorpaySignature": "abc123signaturedef456"
}

Success Response (200 OK — valid signature):
{
  "id": 1,
  "orderId": 10,
  "orderNumber": "ORD-20240001",
  "paymentId": "PAY-ABC123DEF456GH",
  "gatewayOrderId": "order_OPm1FgT1EXXXXX",
  "gatewayTransactionId": "pay_OPm1FgT1EYYYYY",
  "status": "SUCCESS",
  ...
}

Failure Response (200 OK — invalid signature):
{
  "status": "FAILED",
  "failureReason": "Razorpay signature verification failed",
  ...
}
```

#### GET /api/payments/order/{orderId} — Get Payment by Order

```
Method:   GET
URL:      http://localhost:8080/api/payments/order/{orderId}
Headers:  Authorization: Bearer <CUSTOMER_JWT>

Success Response (200 OK):
{
  "id": 1,
  "orderId": 10,
  "orderNumber": "ORD-20240001",
  "paymentId": "PAY-ABC123DEF456GH",
  "status": "SUCCESS",
  ...
}
```

#### POST /api/payments/refund/{orderId} — Refund Payment

```
Method:       POST
URL:          http://localhost:8080/api/payments/refund/{orderId}
Headers:      Authorization: Bearer <CUSTOMER_JWT>
              Content-Type: application/json

Request Body (optional):
{
  "reason": "Changed my mind"
}

Success Response (200 OK):
{
  "id": 1,
  "orderId": 10,
  "status": "REFUNDED",
  "failureReason": "Refund reason: Changed my mind",
  ...
}
```

---

### 6.2 Admin Endpoints — ROLE_ADMIN Required

#### GET /api/admin/payments — List All Payments

```
Method:   GET
URL:      http://localhost:8080/api/admin/payments
Headers:  Authorization: Bearer <ADMIN_JWT>

Success Response (200 OK):
[
  {
    "id": 1,
    "orderId": 10,
    "orderNumber": "ORD-20240001",
    "paymentId": "PAY-ABC123DEF456GH",
    "status": "SUCCESS",
    ...
  },
  ...
]
```

#### GET /api/admin/payments/{id} — Get Payment by ID

```
Method:   GET
URL:      http://localhost:8080/api/admin/payments/1
Headers:  Authorization: Bearer <ADMIN_JWT>

Success Response (200 OK):
{
  "id": 1,
  "orderId": 10,
  "orderNumber": "ORD-20240001",
  ...
}
```

---

## 7. Authorization Matrix

| Endpoint | CUSTOMER | ADMIN | VENDOR | WAREHOUSE | PUBLIC |
|----------|----------|-------|--------|-----------|--------|
| POST /api/payments/create/{orderId} | ✅ 201 | ❌ 403 | ❌ 403 | ❌ 403 | ❌ 401 |
| POST /api/payments/verify | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /api/payments/order/{orderId} | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 | ❌ 401 |
| POST /api/payments/refund/{orderId} | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /api/admin/payments | ❌ 403 | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 401 |
| GET /api/admin/payments/{id} | ❌ 403 | ✅ 200 | ❌ 403 | ❌ 403 | ❌ 401 |

> **Ownership enforcement:** Customers can only access payments linked to their own orders. Cross-user access is blocked by `AccessDeniedException` at the service layer.

---

## 8. Validation & Error Handling

### Input Validation

| Field | Rule | HTTP |
|-------|------|------|
| `paymentMethod` | Required (NotNull), must be valid enum | 400 |
| `currency` | Optional; defaults to "INR" | — |
| `orderId` (verify) | Required (NotNull) | 400 |
| `razorpayOrderId` (verify) | Required (NotBlank) | 400 |
| `razorpayPaymentId` (verify) | Required (NotBlank) | 400 |
| `razorpaySignature` (verify) | Required (NotBlank) | 400 |
| `reason` (refund) | Optional | — |

### Error Response Format

```json
{
  "timestamp": "2026-08-01T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "A payment already exists for order ID: 10",
  "path": "/api/payments/create/10"
}
```

### HTTP Error Codes

| Scenario | Code | Exception Class | Message |
|----------|------|-----------------|---------|
| Duplicate payment for order | 409 | `DuplicatePaymentException` | "A payment already exists for order ID: X" |
| Payment not found | 404 | `PaymentNotFoundException` | "No payment found for order ID: X" |
| Order not found | 404 | `OrderNotFoundException` | "Order not found with ID: X" |
| Razorpay API failure | 400 | `PaymentVerificationException` | "Failed to create Razorpay order: ..." |
| Refund non-SUCCESS payment | 400 | `RefundNotAllowedException` | "Refund is only allowed for payments with SUCCESS status" |
| Validation errors | 400 | `MethodArgumentNotValidException` | Field-level errors |
| Not authenticated | 401 | — | "Authentication required" |
| Wrong role | 403 | — | "Access Denied" |
| Not owner of order | 403 | `AccessDeniedException` | "You do not have permission to ..." |

---

## 9. Razorpay Integration

### Configuration

**`RazorpayConfig.java`** — Provides a `RazorpayClient` Spring Bean:

```java
@Configuration
public class RazorpayConfig {
    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}
```

**`application.properties`:**
```properties
razorpay.key.id=${RAZORPAY_KEY_ID:rzp_test_mockkeyid}
razorpay.key.secret=${RAZORPAY_KEY_SECRET:mocksecret123456789}
```

### Payment Lifecycle Flow

```
┌──────────────────────────────────────────────────────────────┐
│  1. Customer places order (OrderStatus = PENDING)            │
│                                                              │
│  2. POST /api/payments/create/{orderId}                      │
│     → Validate order exists                                  │
│     → Check ownership                                        │
│     → Check no duplicate payment                             │
│     → Create Razorpay Order (amount in paise)                │
│     → Save Payment (status = CREATED)                        │
│     → Return gatewayOrderId to frontend                      │
│                                                              │
│  3. Frontend opens Razorpay checkout with gatewayOrderId     │
│     → Customer completes payment in Razorpay                 │
│     → Razorpay returns: razorpay_order_id,                   │
│       razorpay_payment_id, razorpay_signature                │
│                                                              │
│  4. POST /api/payments/verify                                │
│     → Verify HMAC-SHA256 signature                           │
│     → If valid:  status = SUCCESS, OrderStatus = PROCESSING  │
│     → If invalid: status = FAILED                            │
│                                                              │
│  5. (Optional) POST /api/payments/refund/{orderId}           │
│     → Only SUCCESS payments can be refunded                  │
│     → status = REFUNDED, OrderStatus = CANCELLED             │
└──────────────────────────────────────────────────────────────┘
```

### Signature Verification

```java
JSONObject attributes = new JSONObject();
attributes.put("razorpay_order_id", request.getRazorpayOrderId());
attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
attributes.put("razorpay_signature", request.getRazorpaySignature());
boolean valid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
```

### Internal Payment ID Format

```java
"PAY-" + UUID.randomUUID().toString()
    .replace("-", "")
    .substring(0, 16)
    .toUpperCase()
// Example: PAY-ABC123DEF456GH
```

---

## 10. Security Configuration

**`SecurityConfig.java`** — Updated RBAC rules:

```java
// Customer Payment endpoints (ROLE_CUSTOMER only)
.requestMatchers("/api/payments", "/api/payments/**").hasRole("CUSTOMER")
// Admin Payment management endpoints (ROLE_ADMIN only)
.requestMatchers("/api/admin/payments", "/api/admin/payments/**").hasRole("ADMIN")
```

---

## 11. Testing

### Test Suite Overview

**Controller Tests:** `PaymentControllerTest.java` — MockMvc integration tests (18 test cases)

| Group | Tests |
|-------|-------|
| **CreatePayment** | CUSTOMER success (201), VENDOR forbidden (403), WAREHOUSE forbidden (403), Unauthenticated (401), Duplicate payment (409), Order not found (404) |
| **VerifyPayment** | CUSTOMER success (200), ADMIN forbidden (403) |
| **GetPaymentByOrder** | CUSTOMER success (200), Payment not found (404), VENDOR forbidden (403) |
| **RefundPayment** | CUSTOMER refund success (200), Refund not allowed (400) |
| **AdminPaymentList** | ADMIN list all (200), CUSTOMER forbidden (403), VENDOR forbidden (403) |
| **AdminPaymentById** | ADMIN get by ID (200), Payment not found (404) |

**Service Tests:** `PaymentServiceTest.java` — Unit tests (14 test cases)

| Group | Tests |
|-------|-------|
| **createPayment()** | Order not found, Duplicate payment, Access denied (wrong owner) |
| **getPaymentByOrder()** | Success, Payment not found, Access denied |
| **verifyPayment()** | Invalid signature → FAILED status, Payment not found |
| **refundPayment()** | Success → REFUNDED, Non-SUCCESS → RefundNotAllowed, Already REFUNDED → error, No payment found |
| **Admin operations** | Get all payments, Get by ID, ID not found |

### Run Payment Tests
```powershell
cd "c:\Users\PREETHI\OneDrive\Desktop\ShopStack Ecomerce\backend"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
& ".\..\..\maven-temp\apache-maven-3.9.6\bin\mvn.cmd" clean test "-Dtest=PaymentControllerTest,PaymentServiceTest"
```

### Run All Tests
```powershell
& ".\..\..\maven-temp\apache-maven-3.9.6\bin\mvn.cmd" clean test
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Tests run: XX, Failures: 0, Errors: 0, Skipped: 0
```

---

## 12. Postman Testing Guide

### Step 1: Login as Customer and Get JWT

```
POST http://localhost:8080/api/auth/login
{
  "email": "customer@shopstack.com",
  "password": "your_password"
}
→ Copy the token from "token" field
```

### Step 2: Create a Payment for an Order

```
POST http://localhost:8080/api/payments/create/{orderId}
Authorization: Bearer <customer_token>
Content-Type: application/json

{
  "paymentMethod": "UPI",
  "currency": "INR"
}
→ Expect 201 Created
→ Note the gatewayOrderId for Razorpay checkout
→ Note the paymentId (internal reference)
```

### Step 3: Test Duplicate Payment (same order)

```
POST http://localhost:8080/api/payments/create/{orderId}
Authorization: Bearer <customer_token>
{
  "paymentMethod": "CARD"
}
→ Expect 409 Conflict: "A payment already exists for order ID: X"
```

### Step 4: Verify Payment (after Razorpay checkout completes)

```
POST http://localhost:8080/api/payments/verify
Authorization: Bearer <customer_token>
{
  "orderId": 10,
  "razorpayOrderId": "order_xxxxxxxxx",
  "razorpayPaymentId": "pay_xxxxxxxxx",
  "razorpaySignature": "signature_from_razorpay"
}
→ Expect 200 OK with status = "SUCCESS" (valid signature)
→ Or status = "FAILED" (invalid signature)
```

### Step 5: Get Payment Details by Order

```
GET http://localhost:8080/api/payments/order/{orderId}
Authorization: Bearer <customer_token>
→ Expect 200 OK with full payment details
```

### Step 6: Refund a Successful Payment

```
POST http://localhost:8080/api/payments/refund/{orderId}
Authorization: Bearer <customer_token>
{
  "reason": "Changed my mind"
}
→ Expect 200 OK with status = "REFUNDED"
```

### Step 7: Test Refund on Non-SUCCESS Payment

```
POST http://localhost:8080/api/payments/refund/{orderId}
Authorization: Bearer <customer_token>
→ Expect 400 Bad Request: "Refund is only allowed for payments with SUCCESS status"
```

### Step 8: Admin — List All Payments

```
POST http://localhost:8080/api/auth/login
{ "email": "admin@shopstack.com", "password": "admin_password" }
→ Copy admin token

GET http://localhost:8080/api/admin/payments
Authorization: Bearer <admin_token>
→ Expect 200 OK with array of all payment records
```

### Step 9: Admin — Get Payment by ID

```
GET http://localhost:8080/api/admin/payments/1
Authorization: Bearer <admin_token>
→ Expect 200 OK with payment details
```

### Step 10: Test RBAC — VENDOR/WAREHOUSE Forbidden

```
POST http://localhost:8080/api/payments/create/1
Authorization: Bearer <vendor_token>
{ "paymentMethod": "UPI" }
→ Expect 403 Forbidden

GET http://localhost:8080/api/admin/payments
Authorization: Bearer <customer_token>
→ Expect 403 Forbidden
```

---

## 13. PostgreSQL Verification

After testing, verify the `payments` table in pgAdmin or psql:

```sql
-- Connect to shopstack_db
\c shopstack_db

-- View all payments
SELECT id, payment_id, order_id, status, amount, currency, payment_method
FROM payments ORDER BY id;

-- Count by status
SELECT status, COUNT(*) FROM payments GROUP BY status;

-- Find payment for specific order
SELECT * FROM payments WHERE order_id = 10;

-- Verify indexes exist
SELECT indexname, tablename FROM pg_indexes WHERE tablename = 'payments';
```

---

## 14. Common Errors & Resolutions

| Error | Cause | Fix |
|-------|-------|-----|
| `409 Conflict` on create | Order already has a payment | Use a different order or delete existing payment |
| `400 Bad Request` on create | `paymentMethod` is null or invalid | Provide a valid enum: UPI, CARD, NET_BANKING, WALLET, COD |
| `400 Bad Request` on verify | Missing razorpay fields | Provide all 4 fields: orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature |
| `400 Bad Request` on refund | Payment not in SUCCESS status | Can only refund SUCCESS payments |
| `401 Unauthorized` | No or expired JWT token | Login again with fresh token |
| `403 Forbidden` | Wrong role or not owner | Use CUSTOMER role for payments; ADMIN for admin endpoints |
| `404 Not Found` | Order or payment doesn't exist | Verify IDs are correct |
| `500 Internal Server Error` | Razorpay API failure or DB issue | Check Razorpay credentials and PostgreSQL connection |

---

## 15. Files Summary

### Source Files

| File | Purpose |
|------|---------|
| `entity/Payment.java` | JPA entity with @OneToOne to Order, indexes, lifecycle callbacks |
| `entity/PaymentStatus.java` | PENDING, CREATED, SUCCESS, FAILED, CANCELLED, REFUNDED |
| `entity/PaymentMethod.java` | CARD, UPI, NET_BANKING, WALLET, COD |
| `entity/PaymentGateway.java` | RAZORPAY |
| `repository/PaymentRepository.java` | findByPaymentId, findByOrderId, existsByOrderId, findAllByOrderByCreatedAtDesc |
| `dto/payment/PaymentCreateRequest.java` | paymentMethod (required), currency (optional) |
| `dto/payment/PaymentVerifyRequest.java` | orderId, razorpayOrderId, razorpayPaymentId, razorpaySignature |
| `dto/payment/PaymentRefundRequest.java` | reason (optional) |
| `dto/payment/PaymentResponse.java` | Full payment DTO with all fields |
| `mapper/PaymentMapper.java` | Payment → PaymentResponse mapping |
| `service/PaymentService.java` | Service interface |
| `service/PaymentServiceImpl.java` | Full implementation with Razorpay SDK integration |
| `controller/PaymentController.java` | Customer endpoints (4 routes) |
| `controller/AdminPaymentController.java` | Admin endpoints (2 routes) |
| `config/RazorpayConfig.java` | RazorpayClient bean with injected credentials |
| `exception/PaymentNotFoundException.java` | 404 - Payment not found |
| `exception/DuplicatePaymentException.java` | 409 - Duplicate payment |
| `exception/PaymentVerificationException.java` | 400 - Razorpay verification failed |
| `exception/RefundNotAllowedException.java` | 400 - Refund not allowed |

### Modified Files

| File | Change |
|------|--------|
| `exception/GlobalExceptionHandler.java` | Added DuplicatePaymentException (409), PaymentNotFoundException (404), PaymentVerificationException (400), RefundNotAllowedException (400) |
| `config/SecurityConfig.java` | Added `/api/payments/**` → CUSTOMER, `/api/admin/payments/**` → ADMIN |
| `pom.xml` | Added `com.razorpay:razorpay-java:1.4.8` dependency |
| `application.properties` | Added `razorpay.key.id` and `razorpay.key.secret` |

### Test Files

| File | Purpose |
|------|---------|
| `test/controller/PaymentControllerTest.java` | MockMvc integration tests (18 test cases) |
| `test/service/PaymentServiceTest.java` | Mockito unit tests (14 test cases) |

---

## 16. Dependencies

**Maven (pom.xml):**
```xml
<!-- Razorpay Java SDK: Payment Gateway Integration -->
<dependency>
    <groupId>com.razorpay</groupId>
    <artifactId>razorpay-java</artifactId>
    <version>1.4.8</version>
</dependency>
```

---

## 17. Environment Variables

| Variable | Description | Default (Dev) |
|----------|-------------|---------------|
| `RAZORPAY_KEY_ID` | Razorpay API Key ID | `rzp_test_mockkeyid` |
| `RAZORPAY_KEY_SECRET` | Razorpay API Key Secret | `mocksecret123456789` |

> **Production:** Replace defaults with actual Razorpay production credentials. Never commit real credentials.
