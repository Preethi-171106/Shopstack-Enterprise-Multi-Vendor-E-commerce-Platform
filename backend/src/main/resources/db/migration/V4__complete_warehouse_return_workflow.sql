-- Migration: V4__complete_warehouse_return_workflow.sql
-- Description: Adds damaged and quarantine quantities to warehouse_inventories, adds return warehouse, item-level return details, and quality check fields to returns table.

-- 1. Add damaged_quantity and quarantine_quantity to warehouse_inventories
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'warehouse_inventories' AND column_name = 'damaged_quantity'
    ) THEN
        ALTER TABLE warehouse_inventories 
        ADD COLUMN damaged_quantity INTEGER NOT NULL DEFAULT 0 CHECK (damaged_quantity >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'warehouse_inventories' AND column_name = 'quarantine_quantity'
    ) THEN
        ALTER TABLE warehouse_inventories 
        ADD COLUMN quarantine_quantity INTEGER NOT NULL DEFAULT 0 CHECK (quarantine_quantity >= 0);
    END IF;
END $$;

-- 2. Add return warehouse, order_item, receiving, and QC inspection fields to returns table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'order_item_id'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN order_item_id BIGINT;

        ALTER TABLE returns 
        ADD CONSTRAINT fk_returns_order_item 
        FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE SET NULL;

        CREATE INDEX IF NOT EXISTS idx_returns_order_item_id ON returns(order_item_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'return_quantity'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN return_quantity INTEGER NOT NULL DEFAULT 1 CHECK (return_quantity > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'original_warehouse_id'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN original_warehouse_id BIGINT;

        ALTER TABLE returns 
        ADD CONSTRAINT fk_returns_original_warehouse 
        FOREIGN KEY (original_warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL;

        CREATE INDEX IF NOT EXISTS idx_returns_original_warehouse_id ON returns(original_warehouse_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'return_warehouse_id'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN return_warehouse_id BIGINT;

        ALTER TABLE returns 
        ADD CONSTRAINT fk_returns_return_warehouse 
        FOREIGN KEY (return_warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL;

        CREATE INDEX IF NOT EXISTS idx_returns_return_warehouse_id ON returns(return_warehouse_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'admin_notes'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN admin_notes VARCHAR(1000);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'return_received_date'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN return_received_date TIMESTAMP(6) WITHOUT TIME ZONE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'package_condition'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN package_condition VARCHAR(100);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'receiving_notes'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN receiving_notes VARCHAR(1000);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'qc_status'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN qc_status VARCHAR(50) DEFAULT 'PENDING';

        CREATE INDEX IF NOT EXISTS idx_returns_qc_status ON returns(qc_status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'qc_result'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN qc_result VARCHAR(50);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'qc_notes'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN qc_notes VARCHAR(1000);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'qc_inspector_id'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN qc_inspector_id BIGINT;

        ALTER TABLE returns 
        ADD CONSTRAINT fk_returns_qc_inspector 
        FOREIGN KEY (qc_inspector_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'qc_completed_at'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN qc_completed_at TIMESTAMP(6) WITHOUT TIME ZONE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'accepted_quantity'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN accepted_quantity INTEGER NOT NULL DEFAULT 0 CHECK (accepted_quantity >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'returns' AND column_name = 'damaged_quantity'
    ) THEN
        ALTER TABLE returns 
        ADD COLUMN damaged_quantity INTEGER NOT NULL DEFAULT 0 CHECK (damaged_quantity >= 0);
    END IF;
END $$;

-- 3. Update stock_movements index and size
CREATE INDEX IF NOT EXISTS idx_stock_movements_type ON stock_movements(movement_type);
