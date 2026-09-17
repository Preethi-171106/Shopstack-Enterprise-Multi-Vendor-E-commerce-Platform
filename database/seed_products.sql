-- =========================================================================
-- ShopStack PostgreSQL Seed Script: Categories, Vendors, Warehouses, Products & Inventory
-- Safe to execute multiple times (uses ON CONFLICT clauses / WHERE NOT EXISTS)
-- =========================================================================

-- 1. Insert Core Parent Categories
INSERT INTO categories (id, name, slug, description, image_url, active, created_at, updated_at)
VALUES 
    (1, 'Electronics', 'electronics', 'Smartphones, laptops, audio gear, and cutting-edge devices', 'https://images.unsplash.com/photo-1498049794561-7780e7231661?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW()),
    (2, 'Gadgets', 'gadgets', 'Smart gadgets, smart home, and wearable electronics', 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW()),
    (3, 'Home & Kitchen', 'home-kitchen', 'Modern furniture, home decor, cookware, and smart living', 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW()),
    (4, 'Fashion', 'fashion', 'Trending styles, apparel, footwear, and designer accessories', 'https://images.unsplash.com/photo-1445205170230-053b83016050?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW()),
    (5, 'Beauty & Personal Care', 'beauty-personal-care', 'Skincare, organic cosmetics, fragrances, and self-care items', 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW()),
    (6, 'Sports & Fitness', 'sports-fitness', 'Fitness equipment, athletic wear, camping gear, and accessories', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW()),
    (7, 'Books & Stationery', 'books-stationery', 'Bestsellers, educational material, and notebooks', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=600&q=80', true, NOW(), NOW())
ON CONFLICT (slug) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description;

-- Insert Subcategories
INSERT INTO categories (name, slug, description, parent_category_id, active, created_at, updated_at)
VALUES 
    ('Audio', 'audio', 'Speakers, headphones, earbuds and headsets', 1, true, NOW(), NOW()),
    ('Smart Watches', 'smart-watches', 'Smart watches, fitness bands and wearables', 2, true, NOW(), NOW()),
    ('Men''s Clothing', 'mens-clothing', 'Men''s apparel, jackets, and streetwear', 4, true, NOW(), NOW()),
    ('Women''s Clothing', 'womens-clothing', 'Women''s fashion, dresses, and knitwear', 4, true, NOW(), NOW()),
    ('Fashion Accessories', 'fashion-accessories', 'Bags, leather goods, and travel duffles', 4, true, NOW(), NOW()),
    ('Cookware', 'cookware', 'Dutch ovens, pots, pans, and chef tools', 3, true, NOW(), NOW()),
    ('Dining', 'dining', 'Tableware, cold brew pitchers, and glassware', 3, true, NOW(), NOW()),
    ('Electronics Accessories', 'electronics-accessories', 'Keyboards, mice, and computer peripherals', 1, true, NOW(), NOW()),
    ('Fitness Equipment', 'fitness-equipment', 'Yoga mats, resistance bands, and home gym gear', 6, true, NOW(), NOW())
ON CONFLICT (slug) DO NOTHING;

-- 2. Insert Warehouses
INSERT INTO warehouses (warehouse_code, name, address, city, state, postal_code, country, active, created_at, updated_at)
VALUES 
    ('WH-BLR-01', 'Bangalore Central Fulfillment Center', 'Electronic City Phase 1, Hosur Road', 'Bangalore', 'Karnataka', '560100', 'India', true, NOW(), NOW()),
    ('WH-BOM-01', 'Mumbai Logistics & Cargo Depot', 'MIDC Industrial Area, Andheri East', 'Mumbai', 'Maharashtra', '400093', 'India', true, NOW(), NOW()),
    ('WH-MAA-01', 'Chennai Coastal Distribution Hub', 'Grand Southern Trunk Road, Guindy', 'Chennai', 'Tamil Nadu', '600032', 'India', true, NOW(), NOW())
ON CONFLICT (warehouse_code) DO NOTHING;

-- 3. Insert Users (BCrypt hash for 'Vendor@123' / 'Admin@123')
INSERT INTO users (email, password, first_name, last_name, phone_number, role, enabled, created_at, updated_at)
VALUES 
    ('admin@shopstack.com', '$2a$10$wT8KzU4/9tK7d5H6wL7Ww.VfB5Pz9C7d8q5s8w9L5q7W5h8K7q5e.', 'ShopStack', 'Admin', '+91-9999911111', 'ADMIN', true, NOW(), NOW()),
    ('vendor@shopstack.com', '$2a$10$wT8KzU4/9tK7d5H6wL7Ww.VfB5Pz9C7d8q5s8w9L5q7W5h8K7q5e.', 'Aura', 'Technologies', '+91-9888877777', 'VENDOR', true, NOW(), NOW()),
    ('fashionvendor@shopstack.com', '$2a$10$wT8KzU4/9tK7d5H6wL7Ww.VfB5Pz9C7d8q5s8w9L5q7W5h8K7q5e.', 'Elena', 'Rostova', '+91-9888877778', 'VENDOR', true, NOW(), NOW()),
    ('homevendor@shopstack.com', '$2a$10$wT8KzU4/9tK7d5H6wL7Ww.VfB5Pz9C7d8q5s8w9L5q7W5h8K7q5e.', 'Kari', 'Lindqvist', '+91-9888877779', 'VENDOR', true, NOW(), NOW()),
    ('sportsvendor@shopstack.com', '$2a$10$wT8KzU4/9tK7d5H6wL7Ww.VfB5Pz9C7d8q5s8w9L5q7W5h8K7q5e.', 'Marcus', 'Vance', '+91-9888877780', 'VENDOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- 4. Insert Vendor Profiles
INSERT INTO vendor_profiles (user_id, store_name, store_description, business_email, business_phone, business_address, city, state, country, postal_code, status, created_at, updated_at)
SELECT u.id, 'Aura Tech Solutions', 'Premium consumer electronics, audio gear, and high-performance computing gadgets.', 'vendor@shopstack.com', '+91-9888877777', '100 Innovation Boulevard', 'Bangalore', 'Karnataka', 'India', '560001', 'APPROVED', NOW(), NOW()
FROM users u WHERE u.email = 'vendor@shopstack.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO vendor_profiles (user_id, store_name, store_description, business_email, business_phone, business_address, city, state, country, postal_code, status, created_at, updated_at)
SELECT u.id, 'Urban Thread Co.', 'Sustainable street fashion, luxury apparel, and hand-tailored leather goods.', 'fashionvendor@shopstack.com', '+91-9888877778', '45 Broadway Fashion Ave', 'New York', 'NY', 'USA', '10001', 'APPROVED', NOW(), NOW()
FROM users u WHERE u.email = 'fashionvendor@shopstack.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO vendor_profiles (user_id, store_name, store_description, business_email, business_phone, business_address, city, state, country, postal_code, status, created_at, updated_at)
SELECT u.id, 'Nordic Nest Design', 'Minimalist Scandinavian furniture, artisanal ceramic decor, and ambient lighting.', 'homevendor@shopstack.com', '+91-9888877779', '12 Fjord Way', 'Seattle', 'WA', 'USA', '98101', 'APPROVED', NOW(), NOW()
FROM users u WHERE u.email = 'homevendor@shopstack.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO vendor_profiles (user_id, store_name, store_description, business_email, business_phone, business_address, city, state, country, postal_code, status, created_at, updated_at)
SELECT u.id, 'Apex Motion Sports', 'Pro-grade athletic apparel, outdoor expedition gear, and fitness tracking technology.', 'sportsvendor@shopstack.com', '+91-9888877780', '78 Mountain Ridge Rd', 'Denver', 'CO', 'USA', '80201', 'APPROVED', NOW(), NOW()
FROM users u WHERE u.email = 'sportsvendor@shopstack.com'
ON CONFLICT (user_id) DO NOTHING;

-- 5. Insert All 12 Products
-- Product 1: Headphones
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Aura Studio Wireless Noise-Canceling Headphones', 
    'aura-studio-wireless-headphones', 
    'AURA-ANC-HEADPHONE-01', 
    'Experience studio-grade acoustic clarity with active noise cancellation, custom 40mm drivers, and 45 hours of continuous battery life.', 
    249.99, 299.99, 50, 
    'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, true, 4.90, 128, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'audio' AND u.email = 'vendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 2: Smart Watch
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Smart Watch Series X', 
    'smart-watch-series-x', 
    'AURA-WATCH-SERIESX-02', 
    'Sleek AMOLED smartwatch with continuous health monitoring, GPS tracking, custom watch faces, and 50m water resistance.', 
    199.00, 249.00, 60, 
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, true, 4.70, 94, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'smart-watches' AND u.email = 'vendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 3: Raw Denim Jacket
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Urban Heritage Raw Denim Jacket', 
    'urban-heritage-denim-jacket', 
    'URBAN-DENIM-JACKET-03', 
    'Crafted from 100% organic heavy selvedge cotton. Features classic brass hardware, custom contrast stitching, and a tailored classic fit.', 
    129.50, 160.00, 40, 
    'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, true, 4.80, 64, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'mens-clothing' AND u.email = 'fashionvendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 4: Ceramic Table Lamp
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Nordic Oak & Ceramic Table Lamp', 
    'nordic-oak-ceramic-table-lamp', 
    'NORDIC-OAK-LAMP-04', 
    'Handcrafted ceramic base with natural Scandinavian solid oak accent. Includes warm dimmable LED bulb and woven linen lampshade.', 
    89.99, 110.00, 35, 
    'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, true, 4.90, 48, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'home-kitchen' AND u.email = 'homevendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 5: Mechanical Keyboard
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Apex Precision Pro Mechanical Keyboard', 
    'apex-precision-mechanical-keyboard', 
    'AURA-MECH-KEYBOARD-05', 
    'Hot-swappable mechanical switches, solid CNC aluminum frame, per-key RGB backlighting, and tri-mode wireless connection.', 
    159.99, 189.99, 45, 
    'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, true, 4.80, 112, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'electronics-accessories' AND u.email = 'vendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 6: Hiking Backpack
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Apex TrailBlazer Ergonomic Hiking Backpack 35L', 
    'apex-trailblazer-backpack-35l', 
    'APEX-HIKING-BAG-06', 
    'Lightweight weatherproof ripstop nylon backpack with ergonomic air-mesh ventilation, hydration bladder pocket, and integrated rain cover.', 
    119.00, 145.00, 50, 
    'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, false, 4.60, 78, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'sports-fitness' AND u.email = 'sportsvendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 7: Italian Leather Duffle
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Urban Italian Leather Weekend Duffle', 
    'urban-italian-leather-duffle', 
    'URBAN-LEATHER-DUFFLE-07', 
    'Full-grain Italian cognac leather duffle bag with reinforced brass hardware, shoe compartment, and adjustable padded shoulder strap.', 
    210.00, 260.00, 25, 
    'https://images.unsplash.com/photo-1547949003-9792a18a2601?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, true, 4.90, 52, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'fashion-accessories' AND u.email = 'fashionvendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 8: Cast Iron Dutch Oven
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Nordic Cast Iron Dutch Oven (5.5 Qt)', 
    'nordic-cast-iron-dutch-oven', 
    'NORDIC-DUTCH-OVEN-08', 
    'Heavyweight enamelled cast iron Dutch oven designed for braising, baking, searing, and slow cooking on any stovetop or oven.', 
    135.00, 170.00, 40, 
    'https://images.unsplash.com/photo-1585515320310-259814833e62?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, false, 4.90, 89, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'cookware' AND u.email = 'homevendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 9: 4K Drone
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Aura Ultra-Light 4K Drone System', 
    'aura-ultra-light-4k-drone', 
    'AURA-4K-DRONE-09', 
    'Sub-249 gram folding drone with 4K 60fps HDR video camera, 3-axis mechanical gimbal, 38-min flight time, and obstacle avoidance.', 
    499.00, 599.00, 30, 
    'https://images.unsplash.com/photo-1527977966376-1c8408f9f108?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, false, 4.80, 165, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'gadgets' AND u.email = 'vendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 10: Yoga Mat
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Apex Pro Non-Slip Yoga & Fitness Mat', 
    'apex-pro-yoga-mat', 
    'APEX-YOGA-MAT-10', 
    'Eco-conscious natural tree rubber yoga mat featuring high-density grip, alignment guide lines, and carrying strap.', 
    45.00, 60.00, 60, 
    'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, false, 4.70, 110, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'fitness-equipment' AND u.email = 'sportsvendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 11: Merino Wool Sweater
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Urban Oversized Merino Wool Knit Sweater', 
    'urban-merino-wool-sweater', 
    'URBAN-MERINO-SWEATER-11', 
    'Cozy 100% extra-fine Merino wool sweater with ribbed crew neck, dropped shoulders, and relaxed slouchy fit.', 
    98.00, 125.00, 35, 
    'https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, false, 4.80, 41, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'womens-clothing' AND u.email = 'fashionvendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- Product 12: Cold Brew Pitcher
INSERT INTO products (name, slug, sku, description, price, original_price, stock_quantity, image_url, category_id, vendor_profile_id, active, featured, rating, review_count, created_at, updated_at)
SELECT 
    'Nordic Botanical Glass Cold Brew Pitcher', 
    'nordic-glass-cold-brew-pitcher', 
    'NORDIC-GLASS-PITCHER-12', 
    'High borosilicate heat-resistant glass carafe with fine mesh stainless steel filter for smooth cold brew coffee and iced teas.', 
    39.99, 49.99, 55, 
    'https://images.unsplash.com/photo-1517256064527-09c73fc73e38?auto=format&fit=crop&w=800&q=80', 
    c.id, vp.id, true, false, 4.90, 75, NOW(), NOW()
FROM categories c, vendor_profiles vp JOIN users u ON vp.user_id = u.id
WHERE c.slug = 'dining' AND u.email = 'homevendor@shopstack.com'
ON CONFLICT (sku) DO UPDATE SET name = EXCLUDED.name, price = EXCLUDED.price, active = true;

-- 6. Insert Inventories for all products
INSERT INTO inventories (product_id, total_stock, reserved_stock, available_stock, low_stock_threshold, version)
SELECT p.id, p.stock_quantity, 0, p.stock_quantity, 10, 0
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM inventories inv WHERE inv.product_id = p.id);

-- 7. Insert Warehouse Inventory Distribution (50% BLR, 30% BOM, 20% MAA)
INSERT INTO warehouse_inventories (warehouse_id, product_id, total_quantity, reserved_quantity, available_quantity, low_stock_threshold, version)
SELECT w.id, p.id, (p.stock_quantity * 0.5)::int, 0, (p.stock_quantity * 0.5)::int, 5, 0
FROM products p, warehouses w
WHERE w.warehouse_code = 'WH-BLR-01'
AND NOT EXISTS (SELECT 1 FROM warehouse_inventories wi WHERE wi.warehouse_id = w.id AND wi.product_id = p.id);

INSERT INTO warehouse_inventories (warehouse_id, product_id, total_quantity, reserved_quantity, available_quantity, low_stock_threshold, version)
SELECT w.id, p.id, (p.stock_quantity * 0.3)::int, 0, (p.stock_quantity * 0.3)::int, 5, 0
FROM products p, warehouses w
WHERE w.warehouse_code = 'WH-BOM-01'
AND NOT EXISTS (SELECT 1 FROM warehouse_inventories wi WHERE wi.warehouse_id = w.id AND wi.product_id = p.id);

INSERT INTO warehouse_inventories (warehouse_id, product_id, total_quantity, reserved_quantity, available_quantity, low_stock_threshold, version)
SELECT w.id, p.id, (p.stock_quantity - (p.stock_quantity * 0.5)::int - (p.stock_quantity * 0.3)::int), 0, (p.stock_quantity - (p.stock_quantity * 0.5)::int - (p.stock_quantity * 0.3)::int), 5, 0
FROM products p, warehouses w
WHERE w.warehouse_code = 'WH-MAA-01'
AND NOT EXISTS (SELECT 1 FROM warehouse_inventories wi WHERE wi.warehouse_id = w.id AND wi.product_id = p.id);

-- 8. Insert Promotional Coupons
INSERT INTO coupons (code, name, description, discount_type, discount_value, minimum_order_amount, maximum_discount, usage_limit, per_user_limit, applicability_scope, start_date, expiry_date, active, created_at, updated_at)
VALUES 
    ('SHOP10', 'Exclusive Welcome Offer', 'Get 10% discount on all orders above $50', 'PERCENTAGE', 10.00, 50.00, NULL, 1000, 5, 'ENTIRE_PLATFORM', NOW() - INTERVAL '30 days', NOW() + INTERVAL '1 year', true, NOW(), NOW()),
    ('SAVE20', 'Mega Savings Discount', 'Save 20% on all orders above $100 up to $50', 'PERCENTAGE', 20.00, 100.00, 50.00, 500, 2, 'ENTIRE_PLATFORM', NOW() - INTERVAL '30 days', NOW() + INTERVAL '1 year', true, NOW(), NOW()),
    ('WELCOME15', 'First Order Special', 'Flat 15% discount for new customers', 'PERCENTAGE', 15.00, 30.00, NULL, 500, 1, 'ENTIRE_PLATFORM', NOW() - INTERVAL '30 days', NOW() + INTERVAL '1 year', true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;
