package com.shopstack.mapper;

import com.shopstack.dto.CustomerReportDto;
import com.shopstack.dto.InventoryReportDto;
import com.shopstack.dto.PaymentReportDto;
import com.shopstack.dto.ProductReportDto;
import com.shopstack.dto.SalesReportDto;
import com.shopstack.dto.VendorReportDto;
import com.shopstack.dto.CouponReportDto;
import com.shopstack.repository.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Maps projection rows and raw aggregates into immutable report DTOs.
 * Pure transformation — no I/O, no business logic.
 */
public final class ReportMapper {

    private ReportMapper() {
    }

    public static List<SalesReportDto.PeriodBreakdown> toBreakdown(List<RevenueBreakdownProjection> rows) {
        return rows.stream()
                .map(r -> SalesReportDto.PeriodBreakdown.builder()
                        .periodLabel(r.getPeriodLabel())
                        .revenue(nz(r.getRevenue()))
                        .ordersCount(r.getOrdersCount())
                        .build())
                .toList();
    }

    public static List<SalesReportDto.StatusCount> toStatusCounts(List<StatusCountProjection> rows) {
        return rows.stream()
                .map(r -> SalesReportDto.StatusCount.builder()
                        .status(r.getStatus())
                        .count(r.getCount())
                        .build())
                .toList();
    }

    public static List<VendorReportDto.VendorSummary> toVendorSummaries(List<VendorSummaryProjection> rows) {
        return rows.stream()
                .map(r -> VendorReportDto.VendorSummary.builder()
                        .vendorId(r.getVendorId())
                        .vendorName(r.getVendorName())
                        .revenue(nz(r.getRevenue()))
                        .ordersCount(nzLong(r.getOrdersCount()))
                        .productsCount(nzLong(r.getProductsCount()))
                        .build())
                .toList();
    }

    public static List<ProductReportDto.ProductRevenue> toProductRevenue(List<ProductRevenueProjection> rows) {
        return rows.stream()
                .map(r -> ProductReportDto.ProductRevenue.builder()
                        .productId(r.getProductId())
                        .productName(r.getProductName())
                        .revenue(nz(r.getRevenue()))
                        .quantitySold(nzLong(r.getQuantitySold()))
                        .build())
                .toList();
    }

    public static List<ProductReportDto.ProductSummary> toBestSelling(List<ProductRevenueProjection> rows) {
        return rows.stream()
                .map(r -> ProductReportDto.ProductSummary.builder()
                        .productId(r.getProductId())
                        .productName(r.getProductName())
                        .quantitySold(nzLong(r.getQuantitySold()))
                        .build())
                .toList();
    }

    public static List<CustomerReportDto.CustomerSummary> toCustomerSummaries(List<CustomerSummaryProjection> rows) {
        return rows.stream()
                .map(r -> CustomerReportDto.CustomerSummary.builder()
                        .customerId(r.getCustomerId())
                        .customerName(r.getCustomerName())
                        .email(r.getEmail())
                        .totalSpent(nz(r.getTotalSpent()))
                        .ordersCount(nzLong(r.getOrdersCount()))
                        .build())
                .toList();
    }

    public static List<PaymentReportDto.PaymentMethodStat> toPaymentMethodStats(List<PaymentMethodStatProjection> rows) {
        return rows.stream()
                .map(r -> PaymentReportDto.PaymentMethodStat.builder()
                        .paymentMethod(r.getPaymentMethod())
                        .count(r.getCount())
                        .amount(nz(r.getAmount()))
                        .build())
                .toList();
    }

    public static List<CouponReportDto.CouponSummary> toCouponSummaries(List<CouponSummaryProjection> rows) {
        return rows.stream()
                .map(r -> CouponReportDto.CouponSummary.builder()
                        .couponId(r.getCouponId())
                        .code(r.getCode())
                        .usageCount(nzLong(r.getUsageCount()))
                        .totalDiscount(nz(r.getTotalDiscount()))
                        .discountPercentage(r.getDiscountPercentage())
                        .build())
                .toList();
    }

    public static InventoryReportDto.InventoryItem toInventoryItem(InventoryItemProjection p) {
        BigDecimal value = nz(p.getUnitPrice()).multiply(BigDecimal.valueOf(nzInt(p.getStockQuantity())));
        return InventoryReportDto.InventoryItem.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .stockQuantity(nzInt(p.getStockQuantity()))
                .lowStockThreshold(nzInt(p.getLowStockThreshold()))
                .unitPrice(nz(p.getUnitPrice()))
                .stockValue(value)
                .status(inventoryStatus(p))
                .build();
    }

    public static String inventoryStatus(InventoryItemProjection p) {
        int qty = nzInt(p.getStockQuantity());
        if (qty <= 0) {
            return "OUT_OF_STOCK";
        }
        if (qty <= nzInt(p.getLowStockThreshold())) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    public static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO.setScale(2) : v;
    }

    public static long nzLong(Long v) {
        return v == null ? 0L : v;
    }

    public static int nzInt(Integer v) {
        return v == null ? 0 : v;
    }
}
