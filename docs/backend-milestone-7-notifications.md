# Backend Milestone 7 — Notifications Module

## Overview
The **Notifications Module** provides real-time, in-app event-driven notifications across all ShopStack user roles: **Customers**, **Vendors**, **Warehouse Staff**, and **Platform Administrators**. 

The system leverages PostgreSQL for persistence, Spring Data JPA, JWT-based owner isolation, and seamless hook integration into existing domain business events (Order lifecycle, Shipment tracking, Vendor onboarding/status, Returns & Refunds, and Inventory thresholds).

---

## 1. Domain Entities & Database Schema

### `Notification` Entity
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, Auto-increment | Unique notification record identifier |
| `user_id` | `BIGINT` | FK to `users`, NOT NULL | Target recipient user |
| `title` | `VARCHAR(200)` | NOT NULL | Headline shown in bell dropdown / toasts |
| `message` | `VARCHAR(1000)` | NOT NULL | Body description with contextual details |
| `notification_type` | `VARCHAR(50)` | NOT NULL | Enum category identifier |
| `read_status` | `BOOLEAN` | NOT NULL, Default: `false` | Read (`true`) / Unread (`false`) state |
| `reference_id` | `BIGINT` | NULL | Contextual foreign ID (e.g. orderId, returnId, productId) |
| `created_at` | `TIMESTAMP` | NOT NULL | Audit timestamp |

### Indexes
- `idx_notifications_user_id` on `user_id`
- `idx_notifications_read_status` on `read_status`
- `idx_notifications_created_at` on `created_at`

---

## 2. Notification Types & Event Triggers

| Category | Type | Event Trigger | Recipient(s) |
|---|---|---|---|
| **Order** | `ORDER_PLACED` | Customer checkout completion | Customer, Vendor(s), Warehouse Staff |
| **Order** | `ORDER_CONFIRMED` | Order status moved to `CONFIRMED` | Customer |
| **Order** | `ORDER_PROCESSING` | Order status moved to `PROCESSING` | Customer, Warehouse Staff |
| **Order** | `ORDER_SHIPPED` | Order/Shipment status moved to `SHIPPED` | Customer |
| **Order** | `ORDER_OUT_FOR_DELIVERY` | Shipment status moved to `OUT_FOR_DELIVERY` | Customer |
| **Order** | `ORDER_DELIVERED` | Shipment status moved to `DELIVERED` | Customer |
| **Order** | `ORDER_CANCELLED` | Order cancellation | Customer, Vendor(s) |
| **Payment** | `PAYMENT_SUCCESS` | Successful payment capture | Customer |
| **Payment** | `PAYMENT_FAILED` | Failed payment attempt | Customer |
| **Returns** | `RETURN_REQUESTED` | Customer submits return request | Customer, Admin |
| **Returns** | `RETURN_APPROVED` | Admin/Vendor approves return | Customer |
| **Refunds** | `REFUND_PROCESSED` | Admin initiates refund | Customer |
| **Vendor** | `VENDOR_APPROVED` | Admin approves vendor application | Vendor |
| **Vendor** | `VENDOR_REJECTED` | Admin rejects vendor application | Vendor |
| **Vendor** | `VENDOR_SUSPENDED` | Admin suspends vendor account | Vendor |
| **Inventory** | `LOW_STOCK` | Stock level drops at or below threshold | Vendor, Admin |
| **System** | `SYSTEM` | Platform notices / maintenance | Target role / Broadcast |
| **Promotions** | `PROMOTION` / `COUPON_AVAILABLE` | Coupon or marketing campaign | Customer / Broadcast |

---

## 3. REST API Specification

Base Path: `/api/notifications` (Requires any authenticated JWT role)

| Method | Endpoint | Query / Path Params | Description | Response Code |
|---|---|---|---|---|
| `GET` | `/api/notifications` | `filter=ALL\|UNREAD`, `page=0`, `size=20` | Paginated notifications for current user | `200 OK` |
| `GET` | `/api/notifications/unread-count` | — | Unread count for current user | `200 OK` |
| `PATCH` | `/api/notifications/{id}/read` | `id` (Long) | Mark specific notification as read | `204 No Content` |
| `PATCH` | `/api/notifications/read-all` | — | Mark all user's notifications as read | `200 OK` |
| `PATCH` | `/api/notifications/mark-all-read` | — | Alias for read-all | `200 OK` |
| `DELETE` | `/api/notifications/{id}` | `id` (Long) | Delete specific notification | `204 No Content` |
| `DELETE` | `/api/notifications` | — | Delete all notifications for current user | `200 OK` |

---

## 4. Security & Isolation Matrix

- **Unauthenticated requests**: Rejected with `401 Unauthorized`.
- **User isolation**: All queries and mutations are scoped to `user.getId()` derived from the authenticated JWT token.
- **Cross-user access**: Attempting to read/modify/delete another user's notification returns `404 Not Found` (no private ID leakage).

---

## 5. Verification Results

- **Automated Tests**: `302 / 302 PASS` (0 failures, 0 errors).
- **Frontend Build**: `npm run build` SUCCESS (`1712 modules transformed`).
