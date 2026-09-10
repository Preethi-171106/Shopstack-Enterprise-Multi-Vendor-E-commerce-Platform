# Milestone 3 – Task 4: Multi-Warehouse Allocation Workflow

## Overview
ShopStack now supports physical multi-warehouse infrastructure with independent per-warehouse inventory, intelligent deterministic allocation algorithms, fine-grained state tracking, and end-to-end fulfillment workflows.

---

## Architecture & Lifecycle

### Fulfillment State Machine
```
Order Confirmed
       │
       ▼
Check Stock across Multiple Warehouses
       │
       ▼
Warehouse Selection (Highest Available Stock → Lowest ID)
       │
       ▼
ALLOCATED (Reserved in WarehouseInventory & StockMovement logged)
       │
       ▼ (Pick Station: /api/warehouse/allocations/{id}/pick)
    PICKED
       │
       ▼ (Packing Station: /api/warehouse/allocations/{id}/pack)
    PACKED
       │
       ▼ (Dispatch Bay: /api/warehouse/allocations/{id}/ready-for-shipment)
READY_FOR_SHIPMENT
       │
       ▼ (When all order allocations are READY_FOR_SHIPMENT)
Parent Shipment transitioned to READY_TO_SHIP & Customer Notified
```

---

## Database Entities & Schema

1. **`warehouses`**:
   - `id`, `warehouse_code` (unique, e.g. `WH-BLR-01`), `name`, `address`, `city`, `state`, `postal_code`, `country`, `active`, `created_at`, `updated_at`.
2. **`warehouse_inventories`**:
   - `id`, `warehouse_id` (FK), `product_id` (FK), `total_quantity`, `reserved_quantity`, `available_quantity`, `low_stock_threshold`, `version` (optimistic lock), `updated_at`.
   - Unique constraint: `(warehouse_id, product_id)`.
3. **`order_item_warehouse_allocations`**:
   - `id`, `order_item_id` (FK), `warehouse_id` (FK), `warehouse_inventory_id` (FK), `allocated_quantity`, `allocation_status` (`ALLOCATED`, `PICKED`, `PACKED`, `READY_FOR_SHIPMENT`), `allocated_at`, `picked_at`, `packed_at`, `ready_for_shipment_at`, `notes`, `created_at`, `updated_at`.
4. **`stock_movements`**:
   - Enhanced with `warehouse_id` (FK) and `movement_type` (`RESERVED`, `RELEASED`, `IN`, `OUT`, `ADJUSTMENT`, `DAMAGE`).

---

## REST APIs

### 1. Physical Warehouse Management (`/api/admin/warehouses`) — Role: `ADMIN`
- `GET /api/admin/warehouses` — List all physical warehouses (supports `?search=`).
- `GET /api/admin/warehouses/active` — List active operational warehouses.
- `GET /api/admin/warehouses/{id}` — Get single warehouse details with SKU count and on-hand stock.
- `POST /api/admin/warehouses` — Create a new warehouse facility.
- `PUT /api/admin/warehouses/{id}` — Update warehouse details.
- `PATCH /api/admin/warehouses/{id}/activate` — Activate facility.
- `PATCH /api/admin/warehouses/{id}/deactivate` — Deactivate facility.
- `GET /api/admin/warehouses/{id}/inventory` — Get inventory levels for a specific warehouse.
- `POST /api/admin/warehouses/{id}/inventory` — Add/restock product inventory at a warehouse.
- `POST /api/admin/warehouses/{id}/inventory/{productId}/adjust` — Adjust stock at a warehouse.

### 2. Multi-Warehouse Allocations (`/api/warehouse/allocations`) — Role: `WAREHOUSE_STAFF`, `ADMIN`
- `GET /api/warehouse/allocations` — List item allocations (filter by `warehouseId` and/or `status`).
- `GET /api/warehouse/allocations/picking` — Allocations in `ALLOCATED` state awaiting pick.
- `GET /api/warehouse/allocations/packing` — Allocations in `PICKED` state awaiting packing.
- `GET /api/warehouse/allocations/ready-to-ship` — Allocations in `PACKED` state awaiting dispatch staging.
- `GET /api/warehouse/allocations/{id}` — Get allocation details.
- `POST /api/warehouse/allocations/{id}/pick` — Mark item picked (`ALLOCATED` $\to$ `PICKED`).
- `POST /api/warehouse/allocations/{id}/pack` — Mark item packed with weight/dimension details (`PICKED` $\to$ `PACKED`).
- `POST /api/warehouse/allocations/{id}/ready-for-shipment` — Stage item for carrier pickup (`PACKED` $\to$ `READY_FOR_SHIPMENT`).

---

## Test Verification
- **Unit Tests**:
  - `WarehouseAllocationServiceTest` (5 workflow & priority allocation tests)
  - `WarehouseManagementServiceTest` (5 CRUD & stock synchronization tests)
- **Integration Tests**:
  - `AdminWarehouseControllerTest` (6 MockMvc RBAC & endpoint tests)
  - `WarehouseAllocationControllerTest` (4 MockMvc lifecycle tests)
  - `OrderServiceTest` (7 order placement & checkout allocation tests)
- **Total Backend Tests**: 334/334 Passing with 0 failures, 0 errors.
- **Frontend Build**: Vite production build completed cleanly with 0 errors.
