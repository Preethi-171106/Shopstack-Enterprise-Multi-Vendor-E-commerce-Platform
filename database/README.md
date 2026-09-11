# ShopStack Database Architecture & Setup Guide

This document provides a comprehensive guide for configuring, managing, and evolving the PostgreSQL database for the **ShopStack – Enterprise Multi-Vendor E-Commerce Platform** across both local development and cloud production environments.

---

## 1. Overview & Database Architecture

ShopStack utilizes a relational PostgreSQL database architecture designed to support high transaction volumes, multi-tenant vendor operations, granular role-based access control, multi-warehouse order fulfillment, and immutable audit logs.

### Database Environments
- **Local Development**: PostgreSQL 15+ running locally on `localhost:5432` or via Docker Compose.
- **Production Environment**: Serverless cloud **Neon PostgreSQL** with SSL/TLS encryption and managed connection pooling.

---

## 2. Local PostgreSQL Setup

### Prerequisites
- PostgreSQL 15+ installed on your operating system (or PostgreSQL running via Docker).
- `psql` command-line utility or a GUI tool such as **pgAdmin 4** or **DBeaver**.

---

## 3. Database Creation

### Option A: Command-Line (`psql`)
1. Connect to your local PostgreSQL server:
   ```bash
   psql -U postgres
   ```
2. Execute the database creation command:
   ```sql
   -- Create the dedicated ShopStack database
   CREATE DATABASE shopstack_db;

   -- Verify the database exists
   \l

   -- Exit the psql shell
   \q
   ```

### Option B: pgAdmin 4 (GUI)
1. Open pgAdmin 4 and connect to your local server.
2. In the left navigation tree, right-click **Databases** -> select **Create** -> **Database...**
3. Enter `shopstack_db` in the **Database** name field.
4. Click **Save**.

---

## 4. Environment Variables Configuration

The backend application connects to the database via standard Spring Boot datasource properties mapped to environment variables. Sensitive values must never be hardcoded in version control.

### Local Development Configuration

#### Windows PowerShell:
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/shopstack_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_actual_postgresql_password"
```

#### Windows Command Prompt (cmd):
```cmd
set DB_URL=jdbc:postgresql://localhost:5432/shopstack_db
set DB_USERNAME=postgres
set DB_PASSWORD=your_actual_postgresql_password
```

#### Linux / macOS (Bash / Zsh):
```bash
export DB_URL="jdbc:postgresql://localhost:5432/shopstack_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_actual_postgresql_password"
```

### Connection Details Summary
| Setting | Local Development Value | Production (Neon) Value |
|---|---|---|
| **Host** | `localhost` | `*.neon.tech` endpoint |
| **Port** | `5432` | `5432` |
| **Database Name** | `shopstack_db` | `neondb` or configured database |
| **Driver** | `org.postgresql.Driver` | `org.postgresql.Driver` |
| **JDBC URL Format** | `jdbc:postgresql://localhost:5432/shopstack_db` | `jdbc:postgresql://<neon-host>:5432/<dbname>?sslmode=require` |

---

## 5. Database Schema & Flyway Migrations

ShopStack utilizes versioned database migrations to manage schema evolution reliably.

### Automatic Migration Execution
> [!NOTE]
> **Flyway executes automatically upon backend application startup.**  
> When the Spring Boot application initializes (`mvn spring-boot:run` or Docker container startup), Flyway automatically inspects the database, compares applied versions against the migration directory, and executes any pending migration scripts in order. **You do not need to execute these scripts manually using `psql`.**

### Migration Script Locations
1. **Primary Application Migration Scripts**:  
   `backend/src/main/resources/db/migration/`
2. **Reference & Standalone Copies**:  
   `database/` directory in the repository root.

### Migration Version Log
| Version | Script File | Description |
|---|---|---|
| **V1** | `V1__fix_order_status_constraint.sql` | Updates `orders_order_status_check` constraint to support all order lifecycle statuses (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`, `REFUNDED`). |
| **V2** | `V2__add_coupon_applicability_scope.sql` | Adds `applicability_scope` column to `coupons` table along with mapping junction tables: `coupon_categories`, `coupon_products`, and `coupon_vendors`. |
| **V3** | `V3__add_multi_warehouse_allocation.sql` | Introduces `warehouses`, `warehouse_inventories`, and `order_item_warehouse_allocations` tables for multi-location warehouse routing. |
| **V4** | `V4__complete_warehouse_return_workflow.sql` | Extends `return_requests` with warehouse return intake columns, staff audit keys, and quality check dispositions (`RESTOCK`, `QUARANTINE`, `REJECT`). |
| **V5** | `V5__update_stock_movements_constraint_and_sync.sql` | Refines `stock_movements` movement type enum constraints and auto-sync triggers. |
| **V6** | `V6__update_refund_return_notification_constraints.sql` | Updates check constraints for refund, return, and notification event types. |
| **V7** | `V7__warehouse_staff_registration_and_approval.sql` | Adds `warehouse_staff_profiles` table and status checks for warehouse staff onboarding and admin approvals. |

---

## 6. Production Neon PostgreSQL Configuration

In production, ShopStack connects to a serverless Neon PostgreSQL cluster:

1. **Datasource URL**: Includes SSL requirements (`?sslmode=require`):
   ```
   DB_URL=jdbc:postgresql://<neon-subdomain>.neon.tech/<dbname>?sslmode=require
   ```
2. **Connection Pooling**: HikariCP is configured in Spring Boot (`maximum-pool-size=5`, `connection-timeout=20000`) to manage serverless connection limits efficiently.
3. **Hibernate DDL Auto**: Configured to `validate` in production to ensure database schema changes are strictly governed by versioned migration scripts rather than dynamic ORM schema generation:
   ```properties
   spring.jpa.hibernate.ddl-auto=validate
   ```

---

## 7. Database Tables Summary

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CORE RELATIONAL ENTITIES                        │
├───────────────────┬───────────────────┬────────────────────────────────┤
│ Category & Product│ Orders & Payments │ Warehouse & Inventory          │
├───────────────────┼───────────────────┼────────────────────────────────┤
│ • categories      │ • orders          │ • warehouses                   │
│ • products        │ • order_items     │ • warehouse_inventories        │
│ • inventories     │ • payments        │ • warehouse_staff_profiles     │
│ • stock_movements │ • shipments       │ • order_item_warehouse_allocs │
│ • coupons         │ • tracking_events │ • return_requests              │
│ • coupon_usages   │ • refunds         │ • notifications                │
└───────────────────┴───────────────────┴────────────────────────────────┘
```

- **Optimistic Locking**: The `inventories` table uses a `@Version` field to prevent race conditions during high-frequency concurrent checkout events.
- **Audit Trails**: The `stock_movements` table functions as an append-only ledger tracking all stock additions, deductions, allocations, and returns with timestamp and actor ID.

---

## 8. Important Security Notes

> [!SECURITY NOTICE]
> - **Never hardcode credentials**: Always supply `DB_USERNAME` and `DB_PASSWORD` via environment variables.
> - **Do not commit `.env` files**: Keep `.env` included in `.gitignore`.
> - **Production Encryption**: Always use SSL (`sslmode=require`) when connecting to remote or cloud database instances.
> - **Role Separation**: Ensure production database users follow the principle of least privilege.
