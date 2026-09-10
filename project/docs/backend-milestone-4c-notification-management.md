# Backend Milestone 4C — Notification Management

## Overview

This milestone implements the complete **Notification Management** module for the ShopStack Enterprise Multi-Vendor E-Commerce Platform. It provides in-app notification creation, delivery, read tracking, and admin broadcast capabilities, with automatic notification generation triggered by key business events.

---

## 1. Notification Types

| Enum | Description |
|------|-------------|
| `ORDER_PLACED` | Customer placed a new order |
| `ORDER_CONFIRMED` | Order confirmed by vendor/admin |
| `PAYMENT_SUCCESS` | Payment processed successfully |
| `PAYMENT_FAILED` | Payment processing failed |
| `ORDER_SHIPPED` | Shipment created and dispatched |
| `ORDER_DELIVERED` | Shipment delivered to customer |
| `RETURN_REQUESTED` | Customer requested a return |
| `RETURN_APPROVED` | Return request approved |
| `REFUND_SUCCESS` | Refund completed |
| `COUPON_RECEIVED` | Coupon assigned to user |
| `LOW_STOCK` | Product inventory below threshold |
| `SYSTEM` | System-wide notification |
| `PROMOTION` | Promotional broadcast |

## 2. Delivery Channels

| Enum | Description |
|------|-------------|
| `IN_APP` | In-application notification (default) |
| `EMAIL` | Email notification |

## 3. Notification Status

| Enum | Description |
|------|-------------|
| `UNREAD` | Notification not yet read (default) |
| `READ` | Notification marked as read |

---

## 4. Database Design

### `notifications` Table

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK, auto-generated |
| `user_id` | BIGINT | FK → `users.id`, NOT NULL |
| `title` | VARCHAR(255) | NOT NULL |
| `message` | VARCHAR(2000) | NOT NULL |
| `notification_type` | VARCHAR(30) | NOT NULL, enum |
| `channel` | VARCHAR(15) | NOT NULL, enum |
| `status` | VARCHAR(10) | NOT NULL, enum (default `UNREAD`) |
| `reference_id` | VARCHAR | Nullable, links to source entity |
| `reference_type` | VARCHAR(20) | Nullable, enum |
| `created_at` | TIMESTAMP | NOT NULL, auto-generated |
| `read_at` | TIMESTAMP | Nullable, set when read |

### Entity Relationships

```
User (1) ──── (N) Notification
  └─ A user owns many notifications
  └─ Each notification belongs to exactly one user
```

---

## 5. Entity Relationship Diagram

```
┌──────────────┐         ┌────────────────────┐
│    users     │         │   notifications    │
├──────────────┤         ├────────────────────┤
│ id (PK)      │◄───────┐│ id (PK)            │
│ email        │        └│ user_id (FK)        │
│ password     │         │ title              │
│ first_name   │         │ message            │
│ last_name    │         │ notification_type  │
│ phone        │         │ channel            │
│ created_at   │         │ status             │
│ updated_at   │         │ reference_id       │
└──────────────┘         │ reference_type     │
                         │ created_at         │
                         │ read_at            │
                         └────────────────────┘
```

---

## 6. Notification Workflow

### Automatic Notification Creation

Notifications are automatically created when the following business events occur:

| Event | Notification Type | Reference Type | Service Method |
|-------|-------------------|----------------|----------------|
| Order placed | `ORDER_PLACED` | `ORDER` | `notifyOrderPlaced()` |
| Payment successful | `PAYMENT_SUCCESS` | `PAYMENT` | `notifyPaymentSuccess()` |
| Payment failed | `PAYMENT_FAILED` | `PAYMENT` | `notifyPaymentFailed()` |
| Shipment created | `ORDER_SHIPPED` | `SHIPMENT` | `notifyShipmentCreated()` |
| Shipment delivered | `ORDER_DELIVERED` | `SHIPMENT` | `notifyShipmentDelivered()` |
| Coupon assigned | `COUPON_RECEIVED` | `COUPON` | `notifyCouponReceived()` |
| Low stock detected | `LOW_STOCK` | `PRODUCT` | `notifyLowStock()` |
| Return requested | `RETURN_REQUESTED` | `RETURN` | `notifyReturnRequested()` |
| Refund completed | `REFUND_SUCCESS` | `REFUND` | `notifyRefundCompleted()` |

### Read Flow

1. Notification created with status `UNREAD`
2. User views notification → status remains `UNREAD`
3. User marks notification as read → status becomes `READ`, `read_at` timestamp set
4. User marks all as read → all `UNREAD` notifications for user become `READ`

### Broadcast Flow

1. Admin sends broadcast request with title, message, and optional type
2. System creates a notification for every registered user
3. Each user receives the notification with status `UNREAD`

---

## 7. API Endpoint Table

### Customer Endpoints

| Method | Endpoint | Description | Auth | Status Codes |
|--------|----------|-------------|------|--------------|
| `GET` | `/api/notifications` | List user's notifications (paginated) | `ROLE_CUSTOMER` | 200, 401 |
| `GET` | `/api/notifications/unread` | List user's unread notifications | `ROLE_CUSTOMER` | 200, 401 |
| `GET` | `/api/notifications/{id}` | Get a specific notification | `ROLE_CUSTOMER` | 200, 401, 403, 404 |
| `PATCH` | `/api/notifications/{id}/read` | Mark a notification as read | `ROLE_CUSTOMER` | 200, 401, 403, 404 |
| `PATCH` | `/api/notifications/read-all` | Mark all notifications as read | `ROLE_CUSTOMER` | 204, 401 |
| `DELETE` | `/api/notifications/{id}` | Delete a notification | `ROLE_CUSTOMER` | 204, 401, 403, 404 |
| `DELETE` | `/api/notifications` | Delete all user's notifications | `ROLE_CUSTOMER` | 204, 401 |

### Admin Endpoints

| Method | Endpoint | Description | Auth | Status Codes |
|--------|----------|-------------|------|--------------|
| `GET` | `/api/admin/notifications` | List all notifications (paginated) | `ROLE_ADMIN` | 200, 401, 403 |
| `GET` | `/api/admin/notifications/{id}` | Get any notification by ID | `ROLE_ADMIN` | 200, 401, 403, 404 |
| `POST` | `/api/admin/notifications/system` | Send system notification to a user | `ROLE_ADMIN` | 201, 400, 401, 403 |
| `POST` | `/api/admin/notifications/broadcast` | Broadcast to all users | `ROLE_ADMIN` | 201, 400, 401, 403 |

---

## 8. Security Rules

| Role | Permissions |
|------|-------------|
| `ROLE_CUSTOMER` | Read, mark as read, and delete only their own notifications |
| `ROLE_ADMIN` | Full access to all notifications; send system notifications; broadcast to all users |
| `ROLE_VENDOR` | Can receive vendor-targeted notifications (e.g., low stock) |
| `ROLE_WAREHOUSE_STAFF` | Can receive warehouse-targeted notifications (e.g., low stock) |

### Ownership Enforcement

- `getNotificationForUser(userId, notificationId)` — throws `UnauthorizedActionException` (403) if `notification.user.id != userId`
- `markAsRead(userId, notificationId)` — throws `UnauthorizedActionException` (403) if `notification.user.id != userId`
- `deleteNotification(userId, notificationId)` — throws `UnauthorizedActionException` (403) if `notification.user.id != userId`

### Endpoint Security

- `/api/admin/**` — requires `ROLE_ADMIN` (enforced in `SecurityConfig`)
- All other `/api/**` endpoints — require authentication
- `/api/auth/**` — public (login endpoint)

---

## 9. Postman Testing Guide

### Setup

1. **Login as Customer**
   ```
   POST /api/auth/login
   Body: { "email": "customer@shopstack.com", "password": "password" }
   ```
   Save the `token` from the response.

2. **Login as Admin**
   ```
   POST /api/auth/login
   Body: { "email": "admin@shopstack.com", "password": "password" }
   ```
   Save the `token` from the response.

### Customer Tests

3. **Get My Notifications**
   ```
   GET /api/notifications?page=0&size=10
   Authorization: Bearer <customer-token>
   ```
   Expected: `200 OK` with paginated list.

4. **Get Unread Notifications**
   ```
   GET /api/notifications/unread
   Authorization: Bearer <customer-token>
   ```
   Expected: `200 OK` with only unread notifications.

5. **Get Specific Notification**
   ```
   GET /api/notifications/1
   Authorization: Bearer <customer-token>
   ```
   Expected: `200 OK` or `404 Not Found`.

6. **Mark Notification as Read**
   ```
   PATCH /api/notifications/1/read
   Authorization: Bearer <customer-token>
   ```
   Expected: `200 OK` with status `READ`.

7. **Mark All as Read**
   ```
   PATCH /api/notifications/read-all
   Authorization: Bearer <customer-token>
   ```
   Expected: `204 No Content`.

8. **Delete Notification**
   ```
   DELETE /api/notifications/1
   Authorization: Bearer <customer-token>
   ```
   Expected: `204 No Content`.

9. **Delete All Notifications**
   ```
   DELETE /api/notifications
   Authorization: Bearer <customer-token>
   ```
   Expected: `204 No Content`.

### Admin Tests

10. **Get All Notifications**
    ```
    GET /api/admin/notifications
    Authorization: Bearer <admin-token>
    ```
    Expected: `200 OK`.

11. **Send System Notification**
    ```
    POST /api/admin/notifications/system
    Authorization: Bearer <admin-token>
    Content-Type: application/json
    Body: { "userId": 1, "title": "System Update", "message": "Maintenance scheduled." }
    ```
    Expected: `201 Created`.

12. **Broadcast Notification**
    ```
    POST /api/admin/notifications/broadcast
    Authorization: Bearer <admin-token>
    Content-Type: application/json
    Body: { "title": "Mega Sale!", "message": "50% off everything.", "notificationType": "PROMOTION" }
    ```
    Expected: `201 Created` with array of notifications.

### Negative Tests

13. **Access Admin Endpoint as Customer**
    ```
    GET /api/admin/notifications
    Authorization: Bearer <customer-token>
    ```
    Expected: `403 Forbidden`.

14. **Access Without Token**
    ```
    GET /api/notifications
    ```
    Expected: `401 Unauthorized`.

15. **Access Other User's Notification**
    ```
    GET /api/notifications/5  (notification owned by different user)
    Authorization: Bearer <customer-token>
    ```
    Expected: `403 Forbidden`.

---

## 10. Test Summary

### Test Files

| Test File | Tests | Description |
|-----------|-------|-------------|
| `NotificationServiceTest` | 21 | Service layer: CRUD, ownership, broadcast, auto-creation hooks |
| `NotificationControllerTest` | 12 | Customer controller: 200, 204, 401, 403, 404 |
| `AdminNotificationControllerTest` | 10 | Admin controller: 200, 201, 400, 401, 403 |
| **Total** | **43** | |

### Coverage

- **201 Created** — system notification, broadcast
- **200 OK** — list, get, mark as read
- **204 No Content** — mark all as read, delete, delete all
- **400 Bad Request** — validation errors (blank title/message)
- **401 Unauthorized** — missing authentication
- **403 Forbidden** — wrong role, ownership violation
- **404 Not Found** — notification does not exist
- **Ownership tests** — verified for get, mark as read, delete
- **Admin broadcast** — verified creates notification for every user
- **Read/Read-all** — verified status transition and timestamp
- **Unread list** — verified returns only unread notifications
- **Delete** — verified single and bulk delete

### Test Results

```
Tests run: 43
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

---

## Files Created

### Main Source

| File | Purpose |
|------|---------|
| `notification/enums/NotificationType.java` | Notification type enum (13 values) |
| `notification/enums/NotificationChannel.java` | Delivery channel enum |
| `notification/enums/NotificationStatus.java` | Read status enum |
| `notification/enums/ReferenceType.java` | Reference entity type enum |
| `notification/entity/Notification.java` | JPA entity |
| `notification/repository/NotificationRepository.java` | Spring Data JPA repository |
| `notification/dto/NotificationResponse.java` | Response DTO |
| `notification/dto/CreateNotificationRequest.java` | Create request DTO |
| `notification/dto/SystemNotificationRequest.java` | Admin system notification DTO |
| `notification/dto/BroadcastNotificationRequest.java` | Admin broadcast DTO |
| `notification/mapper/NotificationMapper.java` | Entity ↔ DTO mapper |
| `notification/service/NotificationService.java` | Business logic + auto-creation hooks |
| `notification/controller/NotificationController.java` | Customer endpoints |
| `notification/controller/AdminNotificationController.java` | Admin endpoints |
| `notification/exception/NotificationNotFoundException.java` | Custom exception |

### Test Source

| File | Purpose |
|------|---------|
| `notification/service/NotificationServiceTest.java` | Service unit tests |
| `notification/controller/NotificationControllerTest.java` | Customer controller tests |
| `notification/controller/AdminNotificationControllerTest.java` | Admin controller tests |

## Files Modified

| File | Change |
|------|--------|
| `common/exception/GlobalExceptionHandler.java` | Added handlers for notification exceptions |
| `security/SecurityConfig.java` | Secured `/api/admin/**` with `ROLE_ADMIN` |
