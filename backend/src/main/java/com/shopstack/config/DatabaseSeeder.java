package com.shopstack.config;

import com.shopstack.entity.Category;
import com.shopstack.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public DatabaseSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
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

    private record CategorySeed(String name, String slug, String description, String parentSlug) {}
}
