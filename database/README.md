# ShopStack Database Setup Guide (PostgreSQL)

This guide walks you through setting up PostgreSQL for the ShopStack backend.

## 1. Prerequisites

- PostgreSQL 15+ installed on your machine.
- `psql` command-line utility or pgAdmin 4 UI tool.

---

## 2. Database Creation Instructions

### Option A: Command Line (`psql`)

Open PowerShell or Command Prompt and run:

```bash
# Connect to PostgreSQL default server (you will be prompted for password)
psql -U postgres
```

Inside the `psql` shell, execute:

```sql
-- Create the ShopStack database
CREATE DATABASE shopstack_db;

-- Verify database list
\l

-- Exit psql shell
\q
```

### Option B: pgAdmin 4 (GUI)

1. Open pgAdmin 4.
2. Expand **Servers** -> Right-click **PostgreSQL** server -> Select **Create** -> **Database...**
3. Set **Database** name to: `shopstack_db`
4. Click **Save**.

---

## 3. Environment Variables Configuration

Set the environment variables before running the Spring Boot application:

### PowerShell (Windows)

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/shopstack_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_actual_postgresql_password"
```

### Command Prompt (Windows)

```cmd
set DB_URL=jdbc:postgresql://localhost:5432/shopstack_db
set DB_USERNAME=postgres
set DB_PASSWORD=your_actual_postgresql_password
```

---

## 4. Connection Details Summary

| Setting | Value |
| --- | --- |
| Database Host | `localhost` |
| Database Port | `5432` |
| Database Name | `shopstack_db` |
| JDBC URL | `jdbc:postgresql://localhost:5432/shopstack_db` |

---

## 5. Database Migrations

Database migration scripts are stored in:
- `backend/src/main/resources/db/migration/`
- `database/`

To apply migration scripts manually via `psql`:

```bash
# Apply V1 (Order status constraints)
psql -U postgres -d shopstack_db -f backend/src/main/resources/db/migration/V1__fix_order_status_constraint.sql

# Apply V2 (Coupon applicability scope & eligibility mapping tables)
psql -U postgres -d shopstack_db -f backend/src/main/resources/db/migration/V2__add_coupon_applicability_scope.sql
```

