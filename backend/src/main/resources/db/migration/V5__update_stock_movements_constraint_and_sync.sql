-- Migration: V5__update_stock_movements_constraint_and_sync.sql
-- Description: Updates stock_movements movement_type CHECK constraint and column length to support all StockMovementType enum values.

-- 1. Alter movement_type column length to VARCHAR(50)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'stock_movements' AND column_name = 'movement_type'
    ) THEN
        ALTER TABLE stock_movements ALTER COLUMN movement_type TYPE VARCHAR(50);
    END IF;
END $$;

-- 2. Drop existing movement_type check constraint if present
DO $$
BEGIN
    ALTER TABLE stock_movements DROP CONSTRAINT IF EXISTS stock_movements_movement_type_check;
EXCEPTION
    WHEN OTHERS THEN NULL;
END $$;

-- 3. Add updated movement_type check constraint matching all Java StockMovementType enum values
ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_movement_type_check
    CHECK (movement_type IN (
        'IN',
        'OUT',
        'ADJUSTMENT',
        'RESERVED',
        'RELEASED',
        'RETURN',
        'DAMAGE',
        'CORRECTION',
        'STOCK_IN',
        'STOCK_OUT',
        'WAREHOUSE_TRANSFER',
        'ALLOCATED',
        'PICKED',
        'PACKED',
        'READY_FOR_SHIPMENT',
        'RETURN_RECEIVED',
        'RETURN_ACCEPTED',
        'RETURN_DAMAGED',
        'QUARANTINE'
    ));
