# ShopStack — Warehouse & Staff Management REST API Specification

This document provides technical documentation for all Warehouse and Staff Management REST API endpoints in the ShopStack backend.

---

## 1. Warehouse Staff Authentication & Registration Endpoints

### 1.1 Register Warehouse Staff (Public)
- **Method**: `POST`
- **URL**: `/api/auth/register/warehouse-staff`
- **Access**: Public
- **Request Body**:
```json
{
  "firstName": "Arun",
  "lastName": "Kumar",
  "email": "arun.staff@warehouse.shopstack.com",
  "password": "SecurePassword123!",
  "phoneNumber": "+91 9876543210",
  "warehouseId": 1
}
```
- **Responses**:
  - `201 Created`: Returns `WarehouseStaffProfileResponse` with status `PENDING`.
  - `400 Bad Request`: Validation failure (empty names, invalid email, password $< 8$ chars).
  - `409 Conflict`: Email already exists.

### 1.2 User Login (with Staff Status Guard)
- **Method**: `POST`
- **URL**: `/api/auth/login`
- **Access**: Public
- **Request Body**:
```json
{
  "email": "arun.staff@warehouse.shopstack.com",
  "password": "SecurePassword123!"
}
```
- **Responses**:
  - `200 OK`: When `status === 'ACTIVE'`, returns JWT Access Token and user profile with `role: WAREHOUSE_STAFF`.
  - `403 Forbidden`: If status is `PENDING`, `REJECTED`, or `SUSPENDED`, returns exact reason message.
  - `401 Unauthorized`: Invalid credentials.

---

## 2. Administrator Warehouse Staff Operations

### 2.1 List All Warehouse Staff
- **Method**: `GET`
- **URL**: `/api/admin/users/warehouse-staff`
- **Access**: `ADMIN`
- **Response**: Array of `WarehouseStaffProfileResponse` objects.

### 2.2 Approve Warehouse Staff
- **Method**: `PATCH`
- **URL**: `/api/admin/users/{userId}/warehouse-staff/approve`
- **Access**: `ADMIN`
- **Response**: `200 OK` with updated profile (`status: ACTIVE`, `approvedByName`, `approvedAt`).

### 2.3 Reject Warehouse Staff
- **Method**: `PATCH`
- **URL**: `/api/admin/users/{userId}/warehouse-staff/reject`
- **Access**: `ADMIN`
- **Request Body**:
```json
{
  "reason": "Incomplete background documentation"
}
```
- **Response**: `200 OK` with updated profile (`status: REJECTED`, `rejectionReason`).

### 2.4 Suspend / Reactivate Warehouse Staff
- **Method**: `PATCH`
- **URL**: `/api/admin/users/{userId}/warehouse-staff/suspend` or `/reactivate`
- **Access**: `ADMIN`
- **Response**: `200 OK` with updated profile status (`SUSPENDED` / `ACTIVE`).

---

## 3. Warehouse Operational Endpoints

### 3.1 Warehouse Order Picking
- **Method**: `PATCH`
- **URL**: `/api/warehouse/orders/{orderId}/pick`
- **Access**: `WAREHOUSE_STAFF`, `ADMIN`
- **Response**: `200 OK` (order status transitions to `PICKED`).

### 3.2 Warehouse Order Packing
- **Method**: `PATCH`
- **URL**: `/api/warehouse/orders/{orderId}/pack`
- **Access**: `WAREHOUSE_STAFF`, `ADMIN`
- **Request Body**:
```json
{
  "weightKg": 1.25,
  "lengthCm": 25.0,
  "widthCm": 15.0,
  "heightCm": 10.0,
  "packageType": "BOX_MEDIUM"
}
```
- **Response**: `200 OK` (order status transitions to `PACKED` / `READY_FOR_SHIPMENT`).

### 3.3 Return Quality Control (QC) Inspection
- **Method**: `POST`
- **URL**: `/api/returns/{returnId}/qc`
- **Access**: `WAREHOUSE_STAFF`, `ADMIN`
- **Request Body**:
```json
{
  "qcStatus": "ACCEPTED", // or "DAMAGED"
  "remarks": "Item intact, tags preserved, restocked in WH-CENTRAL"
}
```
- **Response**: `200 OK` (Restocks inventory if `ACCEPTED`, moves to quarantine if `DAMAGED`, triggers automated customer refund).
