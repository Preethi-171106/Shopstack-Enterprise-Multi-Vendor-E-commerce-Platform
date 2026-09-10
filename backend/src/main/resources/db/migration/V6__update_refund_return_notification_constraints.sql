-- Migration: V6__update_refund_return_notification_constraints.sql
-- Description: Updates check constraints on refunds, returns, notifications, and coupons to match all entity enums.

-- 1. Update refunds table status check constraint
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refunds' AND column_name = 'status'
    ) THEN
        ALTER TABLE refunds ALTER COLUMN status TYPE VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE refunds DROP CONSTRAINT IF EXISTS refunds_status_check;
EXCEPTION
    WHEN OTHERS THEN NULL;
END $$;

ALTER TABLE refunds ADD CONSTRAINT refunds_status_check
    CHECK (status IN (
        'PENDING',
        'PROCESSING',
        'SUCCESS',
        'PROCESSED',
        'FAILED'
    ));

-- 2. Update returns table status check constraint
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'returns' AND column_name = 'status'
    ) THEN
        ALTER TABLE returns ALTER COLUMN status TYPE VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE returns DROP CONSTRAINT IF EXISTS returns_status_check;
EXCEPTION
    WHEN OTHERS THEN NULL;
END $$;

ALTER TABLE returns ADD CONSTRAINT returns_status_check
    CHECK (status IN (
        'REQUESTED',
        'APPROVED',
        'REJECTED',
        'IN_TRANSIT',
        'ITEM_RECEIVED',
        'QC_PENDING',
        'QC_COMPLETED',
        'REFUND_INITIATED',
        'REFUNDED',
        'CLOSED'
    ));

-- 3. Update notifications table notification_type check constraint
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'notifications' AND column_name = 'notification_type'
    ) THEN
        ALTER TABLE notifications ALTER COLUMN notification_type TYPE VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_notification_type_check;
EXCEPTION
    WHEN OTHERS THEN NULL;
END $$;

ALTER TABLE notifications ADD CONSTRAINT notifications_notification_type_check
    CHECK (notification_type IN (
        'ORDER_PLACED',
        'ORDER_CONFIRMED',
        'ORDER_PROCESSING',
        'ORDER_SHIPPED',
        'ORDER_OUT_FOR_DELIVERY',
        'ORDER_DELIVERED',
        'ORDER_CANCELLED',
        'PAYMENT_SUCCESS',
        'PAYMENT_FAILED',
        'RETURN_REQUESTED',
        'RETURN_APPROVED',
        'REFUND_PROCESSED',
        'LOW_STOCK',
        'VENDOR_APPROVED',
        'VENDOR_REJECTED',
        'VENDOR_SUSPENDED',
        'VENDOR_REGISTERED',
        'COUPON_AVAILABLE',
        'PROMOTION',
        'SYSTEM',
        'WAREHOUSE_ALERT',
        'GENERAL'
    ));

-- 4. Update coupons table discount_type check constraint
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'coupons' AND column_name = 'discount_type'
    ) THEN
        ALTER TABLE coupons ALTER COLUMN discount_type TYPE VARCHAR(50);
    END IF;
END $$;

DO $$
BEGIN
    ALTER TABLE coupons DROP CONSTRAINT IF EXISTS coupons_discount_type_check;
EXCEPTION
    WHEN OTHERS THEN NULL;
END $$;

ALTER TABLE coupons ADD CONSTRAINT coupons_discount_type_check
    CHECK (discount_type IN (
        'PERCENTAGE',
        'FIXED_AMOUNT',
        'FLAT'
    ));
