package com.shopstack.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CouponApplicabilityScope {
    ENTIRE_PLATFORM,
    SPECIFIC_CATEGORIES,
    SPECIFIC_PRODUCTS,
    SPECIFIC_VENDORS;

    public boolean isPlatform() {
        return this == ENTIRE_PLATFORM;
    }

    public boolean isCategory() {
        return this == SPECIFIC_CATEGORIES;
    }

    public boolean isProduct() {
        return this == SPECIFIC_PRODUCTS;
    }

    public boolean isVendor() {
        return this == SPECIFIC_VENDORS;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

    @JsonCreator
    public static CouponApplicabilityScope fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ENTIRE_PLATFORM;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "ENTIRE_PLATFORM", "PLATFORM", "ALL" -> ENTIRE_PLATFORM;
            case "SPECIFIC_CATEGORIES", "CATEGORIES", "CATEGORY" -> SPECIFIC_CATEGORIES;
            case "SPECIFIC_PRODUCTS", "PRODUCTS", "PRODUCT" -> SPECIFIC_PRODUCTS;
            case "SPECIFIC_VENDORS", "VENDORS", "VENDOR" -> SPECIFIC_VENDORS;
            default -> throw new IllegalArgumentException(
                    "Invalid coupon applicability scope: '" + value + "'. " +
                    "Accepted values are: ENTIRE_PLATFORM, SPECIFIC_CATEGORIES, SPECIFIC_PRODUCTS, SPECIFIC_VENDORS (or aliases PLATFORM, CATEGORY, PRODUCT, VENDOR)."
            );
        };
    }
}
