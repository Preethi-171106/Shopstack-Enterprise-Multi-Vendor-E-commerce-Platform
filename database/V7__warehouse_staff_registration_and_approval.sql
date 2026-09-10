-- Migration: V7__warehouse_staff_registration_and_approval.sql
-- Description: Creates warehouse_staff_profiles table for self-service registration and administrative approval lifecycle.

CREATE TABLE IF NOT EXISTS warehouse_staff_profiles (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    warehouse_id BIGINT REFERENCES warehouses(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    rejection_reason VARCHAR(255),
    approved_by_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_warehouse_staff_user ON warehouse_staff_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_staff_warehouse ON warehouse_staff_profiles(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_warehouse_staff_status ON warehouse_staff_profiles(status);
