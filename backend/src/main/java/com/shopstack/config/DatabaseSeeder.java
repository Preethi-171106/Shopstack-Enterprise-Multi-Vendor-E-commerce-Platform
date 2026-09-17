package com.shopstack.config;

import com.shopstack.entity.Category;
import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponDiscountType;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.entity.VendorStatus;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.entity.WarehouseStaffProfile;
import com.shopstack.entity.WarehouseStaffStatus;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.CouponRepository;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import com.shopstack.repository.WarehouseInventoryRepository;
import com.shopstack.repository.WarehouseRepository;
import com.shopstack.repository.WarehouseStaffProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DatabaseSeeder — Seeds the database with essential reference data on application startup.
 *
 * <p>Runs after the Spring context is fully initialised. All seed operations are idempotent:
 * if the data already exists it is not duplicated.
 *
 * <p>Active on all profiles except {@code test} (so unit/integration tests start with a
 * clean H2 in-memory database).
 */
@Component
@Profile("!test")
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CategoryRepository categoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final WarehouseStaffProfileRepository warehouseStaffProfileRepository;
    private final CouponRepository couponRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(
            CategoryRepository categoryRepository,
            WarehouseRepository warehouseRepository,
            UserRepository userRepository,
            VendorProfileRepository vendorProfileRepository,
            WarehouseStaffProfileRepository warehouseStaffProfileRepository,
            CouponRepository couponRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            WarehouseInventoryRepository warehouseInventoryRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.categoryRepository = categoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.warehouseStaffProfileRepository = warehouseStaffProfileRepository;
        this.couponRepository = couponRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.warehouseInventoryRepository = warehouseInventoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[DatabaseSeeder] Starting database idempotency check and reference data initialization...");
        seedCategories();
        seedWarehouses();
        seedUsers();
        seedCoupons();
        seedProductsAndInventories();
        log.info("[DatabaseSeeder] Reference data initialization completed successfully.");
    }

    // -------------------------------------------------------------------------
    // Category seeding
    // -------------------------------------------------------------------------

    private void seedCategories() {
        List<CategorySeed> parentSeeds = List.of(
            new CategorySeed("Electronics", "electronics", "Electronics and digital devices", null),
            new CategorySeed("Gadgets", "gadgets", "Smart gadgets and accessories", null),
            new CategorySeed("Home & Kitchen", "home-kitchen", "Household essentials and cooking tools", null),
            new CategorySeed("Fashion", "fashion", "Apparel, footwear, and accessories", null),
            new CategorySeed("Beauty & Personal Care", "beauty-personal-care", "Skincare, hair care, makeup and grooming", null),
            new CategorySeed("Sports & Fitness", "sports-fitness", "Sports, fitness and outdoor gear", null),
            new CategorySeed("Books & Stationery", "books-stationery", "Books, stationery, and office supplies", null),
            new CategorySeed("Toys & Games", "toys-games", "Toys, games and creative fun", null),
            new CategorySeed("Automotive", "automotive", "Car and bike accessories", null),
            new CategorySeed("Groceries", "groceries", "Snacks, essentials and household supplies", null)
        );

        List<CategorySeed> childSeeds = List.of(
            new CategorySeed("Mobiles", "mobiles", "Smartphones and mobile phones", "electronics"),
            new CategorySeed("Laptops", "laptops", "Laptops and notebooks", "electronics"),
            new CategorySeed("Tablets", "tablets", "Tablets and productivity devices", "electronics"),
            new CategorySeed("Cameras", "cameras", "Digital cameras and photography gear", "electronics"),
            new CategorySeed("Televisions", "televisions", "Smart TVs and home entertainment", "electronics"),
            new CategorySeed("Audio", "audio", "Speakers, earbuds and headsets", "electronics"),
            new CategorySeed("Electronics Accessories", "electronics-accessories", "Chargers, adapters, and accessories", "electronics"),
            new CategorySeed("Smart Watches", "smart-watches", "Smart watches and wearables", "gadgets"),
            new CategorySeed("Fitness Trackers", "fitness-trackers", "Trackers and health bands", "gadgets"),
            new CategorySeed("Smart Home", "smart-home", "Home automation and devices", "gadgets"),
            new CategorySeed("Power Banks", "power-banks", "Portable charging solutions", "gadgets"),
            new CategorySeed("Other Gadgets", "other-gadgets", "Miscellaneous tech gadgets", "gadgets"),
            new CategorySeed("Kitchen Utensils", "kitchen-utensils", "Everyday kitchen tools", "home-kitchen"),
            new CategorySeed("Cookware", "cookware", "Cooking pots, pans and sets", "home-kitchen"),
            new CategorySeed("Dining", "dining", "Tableware and dinner essentials", "home-kitchen"),
            new CategorySeed("Home Appliances", "home-appliances", "Major household appliance products", "home-kitchen"),
            new CategorySeed("Storage", "storage", "Storage and organization solutions", "home-kitchen"),
            new CategorySeed("Men's Clothing", "mens-clothing", "Clothing for men", "fashion"),
            new CategorySeed("Women's Clothing", "womens-clothing", "Fashion for women", "fashion"),
            new CategorySeed("Kids Clothing", "kids-clothing", "Kids fashion and essentials", "fashion"),
            new CategorySeed("Footwear", "footwear", "Shoes and sandals", "fashion"),
            new CategorySeed("Fashion Accessories", "fashion-accessories", "Bags, belts and accessories", "fashion"),
            new CategorySeed("Skincare", "skincare", "Skin care essentials", "beauty-personal-care"),
            new CategorySeed("Hair Care", "hair-care", "Hair care and styling products", "beauty-personal-care"),
            new CategorySeed("Makeup", "makeup", "Makeup essentials and cosmetics", "beauty-personal-care"),
            new CategorySeed("Grooming", "grooming", "Grooming and personal care", "beauty-personal-care"),
            new CategorySeed("Sports Equipment", "sports-equipment", "Outdoor and sports gear", "sports-fitness"),
            new CategorySeed("Fitness Equipment", "fitness-equipment", "Gym and workout gear", "sports-fitness"),
            new CategorySeed("Outdoor", "outdoor", "Outdoor goods and accessories", "sports-fitness"),
            new CategorySeed("Sports Accessories", "sports-accessories", "Performance accessories", "sports-fitness"),
            new CategorySeed("Books", "books", "Books and reading collections", "books-stationery"),
            new CategorySeed("Notebooks", "notebooks", "Writing and planning notebooks", "books-stationery"),
            new CategorySeed("Pens", "pens", "Pens, stationery and writing tools", "books-stationery"),
            new CategorySeed("Office Supplies", "office-supplies", "Office essentials and supplies", "books-stationery"),
            new CategorySeed("Toys", "toys", "Fun and playful toys", "toys-games"),
            new CategorySeed("Board Games", "board-games", "Board games and family play", "toys-games"),
            new CategorySeed("Video Games", "video-games", "Video games and gaming accessories", "toys-games"),
            new CategorySeed("Educational Toys", "educational-toys", "Learning-focused toys", "toys-games"),
            new CategorySeed("Car Accessories", "car-accessories", "Accessories for cars and vehicles", "automotive"),
            new CategorySeed("Bike Accessories", "bike-accessories", "Bike gear and accessories", "automotive"),
            new CategorySeed("Car Care", "car-care", "Cleaning and care products for cars", "automotive"),
            new CategorySeed("Tools", "tools", "Vehicle repair and service tools", "automotive"),
            new CategorySeed("Snacks", "snacks", "Quick nibbles and snacks", "groceries"),
            new CategorySeed("Beverages", "beverages", "Drinks and refreshments", "groceries"),
            new CategorySeed("Cooking Essentials", "cooking-essentials", "Kitchen basics and groceries", "groceries"),
            new CategorySeed("Household Supplies", "household-supplies", "Household cleaning and care items", "groceries")
        );

        for (CategorySeed parentSeed : parentSeeds) {
            Category parent = categoryRepository.findBySlug(parentSeed.slug())
                    .orElseGet(() -> categoryRepository.save(Category.builder().name(parentSeed.name()).slug(parentSeed.slug()).description(parentSeed.description()).active(true).build()));
            if (!parent.getName().equals(parentSeed.name())) {
                parent.setName(parentSeed.name());
                parent.setDescription(parentSeed.description());
                categoryRepository.save(parent);
            }
        }

        int created = 0;
        for (CategorySeed childSeed : childSeeds) {
            Category parent = categoryRepository.findBySlug(childSeed.parentSlug()).orElse(null);
            if (parent == null) continue;
            Category child = categoryRepository.findBySlug(childSeed.slug()).orElse(null);
            if (child == null) {
                categoryRepository.save(Category.builder()
                        .name(childSeed.name())
                        .slug(childSeed.slug())
                        .description(childSeed.description())
                        .parentCategory(parent)
                        .active(true)
                        .build());
                created++;
            } else if (!parent.getId().equals(child.getParentCategory() != null ? child.getParentCategory().getId() : null)) {
                child.setParentCategory(parent);
                child.setDescription(childSeed.description());
                categoryRepository.save(child);
            }
        }

        log.info("[DatabaseSeeder] ensured {} child categories exist.", created);
    }

    // -------------------------------------------------------------------------
    // Warehouse seeding
    // -------------------------------------------------------------------------

    private void seedWarehouses() {
        List<WarehouseSeed> warehouseSeeds = List.of(
            new WarehouseSeed("WH-BLR-01", "Bangalore Central Fulfillment Center", "Electronic City Phase 1, Hosur Road", "Bangalore", "Karnataka", "560100", "India"),
            new WarehouseSeed("WH-BOM-01", "Mumbai Logistics & Cargo Depot", "MIDC Industrial Area, Andheri East", "Mumbai", "Maharashtra", "400093", "India"),
            new WarehouseSeed("WH-MAA-01", "Chennai Coastal Distribution Hub", "Grand Southern Trunk Road, Guindy", "Chennai", "Tamil Nadu", "600032", "India")
        );

        for (WarehouseSeed ws : warehouseSeeds) {
            if (!warehouseRepository.existsByWarehouseCodeIgnoreCase(ws.code())) {
                warehouseRepository.save(Warehouse.builder()
                        .warehouseCode(ws.code())
                        .name(ws.name())
                        .address(ws.address())
                        .city(ws.city())
                        .state(ws.state())
                        .postalCode(ws.postalCode())
                        .country(ws.country())
                        .active(true)
                        .build());
                log.info("[DatabaseSeeder] Created warehouse facility: {}", ws.code());
            }
        }
    }

    // -------------------------------------------------------------------------
    // User & Profile seeding
    // -------------------------------------------------------------------------

    private void seedUsers() {
        // 1. Admin users
        seedUserIfAbsent("admin@shopstack.com", "Admin@123", "ShopStack", "Admin", UserRole.ADMIN, null);
        seedUserIfAbsent("admin@gmail.com", "Admin@123", "Main", "Administrator", UserRole.ADMIN, null);

        // 2. Vendors with approved profiles
        seedVendorIfAbsent("vendor@shopstack.com", "Vendor@123", "Aura", "Technologies", "Aura Tech Solutions", "Premium consumer electronics, audio gear, and high-performance gadgets.");
        seedVendorIfAbsent("vendor@gmail.com", "Vendor@123", "Alex", "Vance", "TechNova Solutions", "Next-generation gadgets and smart computing solutions.");
        seedVendorIfAbsent("fashionvendor@shopstack.com", "Vendor@123", "Elena", "Rostova", "Urban Thread Co.", "Sustainable street fashion, luxury apparel, and hand-tailored leather goods.");
        seedVendorIfAbsent("homevendor@shopstack.com", "Vendor@123", "Kari", "Lindqvist", "Nordic Nest Design", "Minimalist Scandinavian furniture, artisanal ceramic decor, and ambient lighting.");
        seedVendorIfAbsent("sportsvendor@shopstack.com", "Vendor@123", "Marcus", "Vance", "Apex Motion Sports", "Pro-grade athletic apparel, outdoor expedition gear, and fitness tracking technology.");

        // 3. Warehouse Staff
        Warehouse blrWarehouse = warehouseRepository.findByWarehouseCodeIgnoreCase("WH-BLR-01").orElse(null);
        seedWarehouseStaffIfAbsent("staff@shopstack.com", "Staff@123", "Rajesh", "Kumar", blrWarehouse);

        // 4. Customers
        seedUserIfAbsent("customer@shopstack.com", "Cust@123", "John", "Doe", UserRole.CUSTOMER, "+91-9876543210");
        seedUserIfAbsent("testcust@shopstack.com", "Cust@123", "Jane", "Smith", UserRole.CUSTOMER, "+91-9876543211");
    }

    private User seedUserIfAbsent(String email, String rawPassword, String firstName, String lastName, UserRole role, String phone) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User newUser = userRepository.save(User.builder()
                    .email(email.toLowerCase().trim())
                    .password(passwordEncoder.encode(rawPassword))
                    .firstName(firstName)
                    .lastName(lastName)
                    .phoneNumber(phone)
                    .role(role)
                    .enabled(true)
                    .build());
            log.info("[DatabaseSeeder] Seeded user account: {} [{}]", email, role);
            return newUser;
        });
    }

    private void seedVendorIfAbsent(String email, String rawPassword, String firstName, String lastName, String storeName, String description) {
        User user = seedUserIfAbsent(email, rawPassword, firstName, lastName, UserRole.VENDOR, "+91-9888877777");
        if (!vendorProfileRepository.existsByUserId(user.getId())) {
            vendorProfileRepository.save(VendorProfile.builder()
                    .user(user)
                    .storeName(storeName)
                    .storeDescription(description)
                    .businessEmail(email)
                    .businessPhone("+91-9888877777")
                    .businessAddress("100 Innovation Boulevard")
                    .city("Bangalore")
                    .state("Karnataka")
                    .country("India")
                    .postalCode("560001")
                    .status(VendorStatus.APPROVED)
                    .build());
            log.info("[DatabaseSeeder] Created approved vendor profile for: {}", storeName);
        }
    }

    private void seedWarehouseStaffIfAbsent(String email, String rawPassword, String firstName, String lastName, Warehouse warehouse) {
        User user = seedUserIfAbsent(email, rawPassword, firstName, lastName, UserRole.WAREHOUSE_STAFF, "+91-9999900000");
        if (warehouseStaffProfileRepository.findByUserId(user.getId()).isEmpty()) {
            warehouseStaffProfileRepository.save(WarehouseStaffProfile.builder()
                    .user(user)
                    .warehouse(warehouse)
                    .status(WarehouseStaffStatus.ACTIVE)
                    .approvedAt(LocalDateTime.now())
                    .build());
            log.info("[DatabaseSeeder] Created active warehouse staff profile for: {}", email);
        }
    }

    // -------------------------------------------------------------------------
    // Coupon seeding
    // -------------------------------------------------------------------------

    private void seedCoupons() {
        seedCouponIfAbsent("SHOP10", "Exclusive Welcome Offer", "Get 10% discount on all orders above $50", CouponDiscountType.PERCENTAGE, new BigDecimal("10.00"), new BigDecimal("50.00"), null);
        seedCouponIfAbsent("SAVE20", "Mega Savings Discount", "Save 20% on all orders above $100 up to $50", CouponDiscountType.PERCENTAGE, new BigDecimal("20.00"), new BigDecimal("100.00"), new BigDecimal("50.00"));
        seedCouponIfAbsent("WELCOME15", "First Order Special", "Flat 15% discount for new customers", CouponDiscountType.PERCENTAGE, new BigDecimal("15.00"), new BigDecimal("30.00"), null);
    }

    private void seedCouponIfAbsent(String code, String name, String description, CouponDiscountType type, BigDecimal value, BigDecimal minOrder, BigDecimal maxDiscount) {
        if (!couponRepository.existsByCodeIgnoreCase(code)) {
            couponRepository.save(Coupon.builder()
                    .code(code.toUpperCase().trim())
                    .name(name)
                    .description(description)
                    .discountType(type)
                    .discountValue(value)
                    .minimumOrderAmount(minOrder)
                    .maximumDiscount(maxDiscount)
                    .usageLimit(1000)
                    .perUserLimit(5)
                    .applicabilityScope(CouponApplicabilityScope.ENTIRE_PLATFORM)
                    .startDate(LocalDateTime.now().minusDays(30))
                    .expiryDate(LocalDateTime.now().plusYears(1))
                    .active(true)
                    .build());
            log.info("[DatabaseSeeder] Seeded promotional coupon: {}", code);
        }
    }

    // -------------------------------------------------------------------------
    // Products & Inventories seeding
    // -------------------------------------------------------------------------

    private void seedProductsAndInventories() {
        VendorProfile defaultVendor = vendorProfileRepository.findByUserEmailIgnoreCase("vendor@shopstack.com")
                .or(() -> vendorProfileRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    User user = seedUserIfAbsent("vendor@shopstack.com", "Vendor@123", "Aura", "Technologies", UserRole.VENDOR, "+91-9888877777");
                    return vendorProfileRepository.save(VendorProfile.builder()
                            .user(user)
                            .storeName("Aura Tech Solutions")
                            .storeDescription("Premium consumer electronics, audio gear, and high-performance gadgets.")
                            .businessEmail("vendor@shopstack.com")
                            .businessPhone("+91-9888877777")
                            .businessAddress("100 Innovation Boulevard")
                            .city("Bangalore")
                            .state("Karnataka")
                            .country("India")
                            .postalCode("560001")
                            .status(VendorStatus.APPROVED)
                            .build());
                });

        Optional<VendorProfile> fashionVendor = vendorProfileRepository.findByUserEmailIgnoreCase("fashionvendor@shopstack.com");
        Optional<VendorProfile> homeVendor = vendorProfileRepository.findByUserEmailIgnoreCase("homevendor@shopstack.com");
        Optional<VendorProfile> sportsVendor = vendorProfileRepository.findByUserEmailIgnoreCase("sportsvendor@shopstack.com");

        Warehouse blr = warehouseRepository.findByWarehouseCodeIgnoreCase("WH-BLR-01").orElse(null);
        Warehouse bom = warehouseRepository.findByWarehouseCodeIgnoreCase("WH-BOM-01").orElse(null);
        Warehouse maa = warehouseRepository.findByWarehouseCodeIgnoreCase("WH-MAA-01").orElse(null);

        List<ProductSeed> productSeeds = List.of(
            new ProductSeed(
                "Aura Studio Wireless Noise-Canceling Headphones",
                "aura-studio-wireless-headphones",
                "AURA-ANC-HEADPHONE-01",
                "Experience studio-grade acoustic clarity with active noise cancellation, custom 40mm drivers, and 45 hours of continuous battery life.",
                new BigDecimal("249.99"),
                new BigDecimal("299.99"),
                50,
                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80",
                "audio",
                defaultVendor,
                true
            ),
            new ProductSeed(
                "Smart Watch Series X",
                "smart-watch-series-x",
                "AURA-WATCH-SERIESX-02",
                "Sleek AMOLED smartwatch with continuous health monitoring, GPS tracking, custom watch faces, and 50m water resistance.",
                new BigDecimal("199.00"),
                new BigDecimal("249.00"),
                60,
                "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80",
                "smart-watches",
                defaultVendor,
                true
            ),
            new ProductSeed(
                "Urban Heritage Raw Denim Jacket",
                "urban-heritage-denim-jacket",
                "URBAN-DENIM-JACKET-03",
                "Crafted from 100% organic heavy selvedge cotton. Features classic brass hardware, custom contrast stitching, and a tailored classic fit.",
                new BigDecimal("129.50"),
                new BigDecimal("160.00"),
                40,
                "https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80",
                "mens-clothing",
                fashionVendor.orElse(defaultVendor),
                true
            ),
            new ProductSeed(
                "Nordic Oak & Ceramic Table Lamp",
                "nordic-oak-ceramic-table-lamp",
                "NORDIC-OAK-LAMP-04",
                "Handcrafted ceramic base with natural Scandinavian solid oak accent. Includes warm dimmable LED bulb and woven linen lampshade.",
                new BigDecimal("89.99"),
                new BigDecimal("110.00"),
                35,
                "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=800&q=80",
                "home-kitchen",
                homeVendor.orElse(defaultVendor),
                true
            ),
            new ProductSeed(
                "Apex Precision Pro Mechanical Keyboard",
                "apex-precision-mechanical-keyboard",
                "AURA-MECH-KEYBOARD-05",
                "Hot-swappable mechanical switches, solid CNC aluminum frame, per-key RGB backlighting, and tri-mode wireless connection.",
                new BigDecimal("159.99"),
                new BigDecimal("189.99"),
                45,
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=800&q=80",
                "electronics-accessories",
                defaultVendor,
                true
            ),
            new ProductSeed(
                "Apex TrailBlazer Ergonomic Hiking Backpack 35L",
                "apex-trailblazer-backpack-35l",
                "APEX-HIKING-BAG-06",
                "Lightweight weatherproof ripstop nylon backpack with ergonomic air-mesh ventilation, hydration bladder pocket, and integrated rain cover.",
                new BigDecimal("119.00"),
                new BigDecimal("145.00"),
                50,
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=800&q=80",
                "sports-fitness",
                sportsVendor.orElse(defaultVendor),
                false
            ),
            new ProductSeed(
                "Urban Italian Leather Weekend Duffle",
                "urban-italian-leather-duffle",
                "URBAN-LEATHER-DUFFLE-07",
                "Full-grain Italian cognac leather duffle bag with reinforced brass hardware, shoe compartment, and adjustable padded shoulder strap.",
                new BigDecimal("210.00"),
                new BigDecimal("260.00"),
                25,
                "https://images.unsplash.com/photo-1547949003-9792a18a2601?auto=format&fit=crop&w=800&q=80",
                "fashion-accessories",
                fashionVendor.orElse(defaultVendor),
                true
            ),
            new ProductSeed(
                "Nordic Cast Iron Dutch Oven (5.5 Qt)",
                "nordic-cast-iron-dutch-oven",
                "NORDIC-DUTCH-OVEN-08",
                "Heavyweight enamelled cast iron Dutch oven designed for braising, baking, searing, and slow cooking on any stovetop or oven.",
                new BigDecimal("135.00"),
                new BigDecimal("170.00"),
                40,
                "https://images.unsplash.com/photo-1585515320310-259814833e62?auto=format&fit=crop&w=800&q=80",
                "cookware",
                homeVendor.orElse(defaultVendor),
                false
            ),
            new ProductSeed(
                "Aura Ultra-Light 4K Drone System",
                "aura-ultra-light-4k-drone",
                "AURA-4K-DRONE-09",
                "Sub-249 gram folding drone with 4K 60fps HDR video camera, 3-axis mechanical gimbal, 38-min flight time, and obstacle avoidance.",
                new BigDecimal("499.00"),
                new BigDecimal("599.00"),
                30,
                "https://images.unsplash.com/photo-1527977966376-1c8408f9f108?auto=format&fit=crop&w=800&q=80",
                "gadgets",
                defaultVendor,
                false
            ),
            new ProductSeed(
                "Apex Pro Non-Slip Yoga & Fitness Mat",
                "apex-pro-yoga-mat",
                "APEX-YOGA-MAT-10",
                "Eco-conscious natural tree rubber yoga mat featuring high-density grip, alignment guide lines, and carrying strap.",
                new BigDecimal("45.00"),
                new BigDecimal("60.00"),
                60,
                "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=800&q=80",
                "fitness-equipment",
                sportsVendor.orElse(defaultVendor),
                false
            ),
            new ProductSeed(
                "Urban Oversized Merino Wool Knit Sweater",
                "urban-merino-wool-sweater",
                "URBAN-MERINO-SWEATER-11",
                "Cozy 100% extra-fine Merino wool sweater with ribbed crew neck, dropped shoulders, and relaxed slouchy fit.",
                new BigDecimal("98.00"),
                new BigDecimal("125.00"),
                35,
                "https://images.unsplash.com/photo-1576566588028-4147f3842f27?auto=format&fit=crop&w=800&q=80",
                "womens-clothing",
                fashionVendor.orElse(defaultVendor),
                false
            ),
            new ProductSeed(
                "Nordic Botanical Glass Cold Brew Pitcher",
                "nordic-glass-cold-brew-pitcher",
                "NORDIC-GLASS-PITCHER-12",
                "High borosilicate heat-resistant glass carafe with fine mesh stainless steel filter for smooth cold brew coffee and iced teas.",
                new BigDecimal("39.99"),
                new BigDecimal("49.99"),
                55,
                "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?auto=format&fit=crop&w=800&q=80",
                "dining",
                homeVendor.orElse(defaultVendor),
                false
            )
        );

        for (ProductSeed ps : productSeeds) {
            if (!productRepository.existsBySku(ps.sku())) {
                Category cat = categoryRepository.findBySlug(ps.categorySlug())
                        .or(() -> categoryRepository.findBySlug("electronics"))
                        .or(() -> categoryRepository.findAll().stream().findFirst())
                        .orElseGet(() -> categoryRepository.save(Category.builder()
                                .name("General Electronics")
                                .slug("electronics")
                                .description("General category")
                                .active(true)
                                .build()));

                if (cat == null) continue;

                Product savedProduct = productRepository.save(Product.builder()
                        .name(ps.name())
                        .slug(ps.slug())
                        .sku(ps.sku())
                        .description(ps.description())
                        .price(ps.price())
                        .originalPrice(ps.originalPrice())
                        .stockQuantity(ps.stockQuantity())
                        .imageUrl(ps.imageUrl())
                        .category(cat)
                        .vendorProfile(ps.vendor())
                        .active(true)
                        .featured(ps.featured())
                        .rating(new BigDecimal("4.8"))
                        .reviewCount(15)
                        .build());

                // Seed Inventory entity
                if (!inventoryRepository.existsByProductId(savedProduct.getId())) {
                    inventoryRepository.save(Inventory.builder()
                            .product(savedProduct)
                            .totalStock(ps.stockQuantity())
                            .reservedStock(0)
                            .availableStock(ps.stockQuantity())
                            .lowStockThreshold(10)
                            .build());
                }

                // Seed WarehouseInventory distribution across BLR, BOM, MAA
                if (blr != null && warehouseInventoryRepository.findByWarehouseIdAndProductId(blr.getId(), savedProduct.getId()).isEmpty()) {
                    int blrQty = (int) (ps.stockQuantity() * 0.5);
                    warehouseInventoryRepository.save(WarehouseInventory.builder()
                            .warehouse(blr)
                            .product(savedProduct)
                            .totalQuantity(blrQty)
                            .reservedQuantity(0)
                            .availableQuantity(blrQty)
                            .lowStockThreshold(5)
                            .build());
                }
                if (bom != null && warehouseInventoryRepository.findByWarehouseIdAndProductId(bom.getId(), savedProduct.getId()).isEmpty()) {
                    int bomQty = (int) (ps.stockQuantity() * 0.3);
                    warehouseInventoryRepository.save(WarehouseInventory.builder()
                            .warehouse(bom)
                            .product(savedProduct)
                            .totalQuantity(bomQty)
                            .reservedQuantity(0)
                            .availableQuantity(bomQty)
                            .lowStockThreshold(5)
                            .build());
                }
                if (maa != null && warehouseInventoryRepository.findByWarehouseIdAndProductId(maa.getId(), savedProduct.getId()).isEmpty()) {
                    int maaQty = ps.stockQuantity() - (int) (ps.stockQuantity() * 0.5) - (int) (ps.stockQuantity() * 0.3);
                    warehouseInventoryRepository.save(WarehouseInventory.builder()
                            .warehouse(maa)
                            .product(savedProduct)
                            .totalQuantity(maaQty)
                            .reservedQuantity(0)
                            .availableQuantity(maaQty)
                            .lowStockThreshold(5)
                            .build());
                }

                log.info("[DatabaseSeeder] Seeded catalog product: {} [SKU: {}]", ps.name(), ps.sku());
            }
        }
    }

    private record CategorySeed(String name, String slug, String description, String parentSlug) {}
    private record WarehouseSeed(String code, String name, String address, String city, String state, String postalCode, String country) {}
    private record ProductSeed(
        String name,
        String slug,
        String sku,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        int stockQuantity,
        String imageUrl,
        String categorySlug,
        VendorProfile vendor,
        boolean featured
    ) {}
}
