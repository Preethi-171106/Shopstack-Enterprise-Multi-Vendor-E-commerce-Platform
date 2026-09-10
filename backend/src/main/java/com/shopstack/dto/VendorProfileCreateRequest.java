package com.shopstack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * VendorProfileCreateRequest — Input DTO for creating a new vendor store profile.
 */
public record VendorProfileCreateRequest(

        @NotBlank(message = "Store name is required")
        @Size(max = 150, message = "Store name cannot exceed 150 characters")
        String storeName,

        @Size(max = 2000, message = "Store description cannot exceed 2000 characters")
        String storeDescription,

        @Email(message = "Invalid business email address format")
        @Size(max = 150, message = "Business email cannot exceed 150 characters")
        String businessEmail,

        @Pattern(regexp = "^$|^[0-9+\\-\\s()]{7,20}$", message = "Invalid business phone number format")
        String businessPhone,

        @Size(max = 255, message = "Business address cannot exceed 255 characters")
        String businessAddress,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        String country,

        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        String postalCode
) {
}
