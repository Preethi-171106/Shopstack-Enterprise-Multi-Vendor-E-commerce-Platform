-- Migration: V2__add_coupon_applicability_scope.sql
-- Description: Add applicability_scope column to coupons table and create coupon eligibility mapping tables

-- 1. Safely add applicability_scope to coupons table if not already present
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'coupons' 
          AND column_name = 'applicability_scope'
    ) THEN
        ALTER TABLE coupons 
        ADD COLUMN applicability_scope VARCHAR(50) NOT NULL DEFAULT 'ENTIRE_PLATFORM';
    END IF;
END $$;

-- 2. Ensure existing coupons with old aliases or null are updated to canonical values
UPDATE coupons SET applicability_scope = 'ENTIRE_PLATFORM' WHERE applicability_scope = 'PLATFORM' OR applicability_scope IS NULL;
UPDATE coupons SET applicability_scope = 'SPECIFIC_CATEGORIES' WHERE applicability_scope = 'CATEGORY' OR applicability_scope = 'CATEGORIES';
UPDATE coupons SET applicability_scope = 'SPECIFIC_PRODUCTS' WHERE applicability_scope = 'PRODUCT' OR applicability_scope = 'PRODUCTS';
UPDATE coupons SET applicability_scope = 'SPECIFIC_VENDORS' WHERE applicability_scope = 'VENDOR' OR applicability_scope = 'VENDORS';

-- Ensure default constraint is set to canonical ENTIRE_PLATFORM
ALTER TABLE coupons ALTER COLUMN applicability_scope SET DEFAULT 'ENTIRE_PLATFORM';

-- 3. Create join table for coupon eligible categories
CREATE TABLE IF NOT EXISTS coupon_categories (
    coupon_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_coupon_categories PRIMARY KEY (coupon_id, category_id),
    CONSTRAINT fk_coupon_categories_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- 4. Create join table for coupon eligible products
CREATE TABLE IF NOT EXISTS coupon_products (
    coupon_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT pk_coupon_products PRIMARY KEY (coupon_id, product_id),
    CONSTRAINT fk_coupon_products_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_products_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- 5. Create join table for coupon eligible vendors
CREATE TABLE IF NOT EXISTS coupon_vendors (
    coupon_id BIGINT NOT NULL,
    vendor_profile_id BIGINT NOT NULL,
    CONSTRAINT pk_coupon_vendors PRIMARY KEY (coupon_id, vendor_profile_id),
    CONSTRAINT fk_coupon_vendors_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_vendors_vendor FOREIGN KEY (vendor_profile_id) REFERENCES vendor_profiles(id) ON DELETE CASCADE
);

-- 6. Add indexes for high-performance lookups
CREATE INDEX IF NOT EXISTS idx_coupons_scope ON coupons(applicability_scope);
CREATE INDEX IF NOT EXISTS idx_coupon_categories_category_id ON coupon_categories(category_id);
CREATE INDEX IF NOT EXISTS idx_coupon_products_product_id ON coupon_products(product_id);
CREATE INDEX IF NOT EXISTS idx_coupon_vendors_vendor_id ON coupon_vendors(vendor_profile_id);
