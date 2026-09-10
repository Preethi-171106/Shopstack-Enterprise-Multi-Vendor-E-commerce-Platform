package com.shopstack.analytics.dto;

import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record DashboardFilterRequest(
        LocalDate startDate,
        LocalDate endDate
) {
    @AssertTrue(message = "startDate must be before or equal to endDate")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.isAfter(endDate);
    }
}
