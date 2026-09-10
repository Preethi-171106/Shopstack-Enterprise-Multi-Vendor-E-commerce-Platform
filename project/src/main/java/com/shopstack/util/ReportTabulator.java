package com.shopstack.util;

import com.shopstack.dto.CouponReportDto;
import com.shopstack.dto.CustomerReportDto;
import com.shopstack.dto.InventoryReportDto;
import com.shopstack.dto.PaymentReportDto;
import com.shopstack.dto.ProductReportDto;
import com.shopstack.dto.ReportType;
import com.shopstack.dto.SalesReportDto;
import com.shopstack.dto.VendorReportDto;
import com.shopstack.exception.ReportException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a report DTO into tabular form (headers + ordered row maps) so the CSV
 * and Excel exporters can render it uniformly. Each report type has its own shape.
 */
public final class ReportTabulator {

    private ReportTabulator() {
    }

    public static Tabular tabulate(ReportType type, Object report) {
        return switch (type) {
            case SALES -> tabulateSales((SalesReportDto) report);
            case VENDORS -> tabulateVendors((VendorReportDto) report);
            case PRODUCTS -> tabulateProducts((ProductReportDto) report);
            case CUSTOMERS -> tabulateCustomers((CustomerReportDto) report);
            case PAYMENTS -> tabulatePayments((PaymentReportDto) report);
            case COUPONS -> tabulateCoupons((CouponReportDto) report);
            case INVENTORY -> tabulateInventory((InventoryReportDto) report);
        };
    }

    private static Tabular tabulateSales(SalesReportDto r) {
        List<String> headers = List.of("period", "revenue", "ordersCount");
        List<Map<String, ?>> rows = new ArrayList<>();
        for (SalesReportDto.PeriodBreakdown b : r.getBreakdown()) {
            rows.add(row("period", b.getPeriodLabel(),
                    "revenue", b.getRevenue(),
                    "ordersCount", b.getOrdersCount()));
        }
        if (rows.isEmpty()) {
            rows.add(row("period", r.getPeriod() == null ? "" : r.getPeriod().getFrom() + ".." + r.getPeriod().getTo(),
                    "revenue", r.getRevenue(),
                    "ordersCount", r.getOrdersCount()));
        }
        return new Tabular("Sales Report", headers, rows);
    }

    private static Tabular tabulateVendors(VendorReportDto r) {
        List<String> headers = List.of("vendorId", "vendorName", "revenue", "ordersCount", "productsCount");
        List<Map<String, ?>> rows = new ArrayList<>();
        for (VendorReportDto.VendorSummary v : r.getVendors()) {
            rows.add(row("vendorId", v.getVendorId(),
                    "vendorName", v.getVendorName(),
                    "revenue", v.getRevenue(),
                    "ordersCount", v.getOrdersCount(),
                    "productsCount", v.getProductsCount()));
        }
        return new Tabular("Vendor Report", headers, rows);
    }

    private static Tabular tabulateProducts(ProductReportDto r) {
        List<String> headers = List.of("productId", "productName", "revenue", "quantitySold");
        List<Map<String, ?>> rows = new ArrayList<>();
        for (ProductReportDto.ProductRevenue p : r.getProductRevenue()) {
            rows.add(row("productId", p.getProductId(),
                    "productName", p.getProductName(),
                    "revenue", p.getRevenue(),
                    "quantitySold", p.getQuantitySold()));
        }
        if (rows.isEmpty()) {
            for (ProductReportDto.ProductSummary p : r.getBestSellingProducts()) {
                rows.add(row("productId", p.getProductId(),
                        "productName", p.getProductName(),
                        "revenue", "",
                        "quantitySold", p.getQuantitySold()));
            }
        }
        return new Tabular("Product Report", headers, rows);
    }

    private static Tabular tabulateCustomers(CustomerReportDto r) {
        List<String> headers = List.of("customerId", "customerName", "email", "totalSpent", "ordersCount");
        List<Map<String, ?>> rows = new ArrayList<>();
        for (CustomerReportDto.CustomerSummary c : r.getTopCustomers()) {
            rows.add(row("customerId", c.getCustomerId(),
                    "customerName", c.getCustomerName(),
                    "email", c.getEmail(),
                    "totalSpent", c.getTotalSpent(),
                    "ordersCount", c.getOrdersCount()));
        }
        return new Tabular("Customer Report", headers, rows);
    }

    private static Tabular tabulatePayments(PaymentReportDto r) {
        List<String> headers = List.of("paymentMethod", "count", "amount");
        List<Map<String, ?>> rows = new ArrayList<>();
        for (PaymentReportDto.PaymentMethodStat s : r.getPaymentMethodStats()) {
            rows.add(row("paymentMethod", s.getPaymentMethod(),
                    "count", s.getCount(),
                    "amount", s.getAmount()));
        }
        if (rows.isEmpty()) {
            rows.add(row("paymentMethod", "SUCCESSFUL", "count", r.getSuccessfulPayments(), "amount", r.getSuccessfulAmount()));
            rows.add(row("paymentMethod", "FAILED", "count", r.getFailedPayments(), "amount", ""));
            rows.add(row("paymentMethod", "REFUNDED", "count", r.getRefundedPayments(), "amount", r.getRefundedAmount()));
        }
        return new Tabular("Payment Report", headers, rows);
    }

    private static Tabular tabulateCoupons(CouponReportDto r) {
        List<String> headers = List.of("couponId", "code", "usageCount", "totalDiscount", "discountPercentage");
        List<Map<String, ?>> rows = new ArrayList<>();
        for (CouponReportDto.CouponSummary c : r.getCoupons()) {
            rows.add(row("couponId", c.getCouponId(),
                    "code", c.getCode(),
                    "usageCount", c.getUsageCount(),
                    "totalDiscount", c.getTotalDiscount(),
                    "discountPercentage", c.getDiscountPercentage()));
        }
        return new Tabular("Coupon Report", headers, rows);
    }

    private static Tabular tabulateInventory(InventoryReportDto r) {
        List<String> headers = List.of("productId", "productName", "stockQuantity", "lowStockThreshold",
                "unitPrice", "stockValue", "status");
        List<Map<String, ?>> rows = new ArrayList<>();
        List<InventoryReportDto.InventoryItem> items = r.getSummary();
        if (items == null || items.isEmpty()) {
            items = new ArrayList<>();
            if (r.getLowStockItems() != null) items.addAll(r.getLowStockItems());
            if (r.getOutOfStockItems() != null) items.addAll(r.getOutOfStockItems());
        }
        for (InventoryReportDto.InventoryItem i : items) {
            rows.add(row("productId", i.getProductId(),
                    "productName", i.getProductName(),
                    "stockQuantity", i.getStockQuantity(),
                    "lowStockThreshold", i.getLowStockThreshold(),
                    "unitPrice", i.getUnitPrice(),
                    "stockValue", i.getStockValue(),
                    "status", i.getStatus()));
        }
        return new Tabular("Inventory Report", headers, rows);
    }

    @SafeVarargs
    private static Map<String, ?> row(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new ReportException("Invalid row construction");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    public record Tabular(String sheetName, List<String> headers, List<Map<String, ?>> rows) {
    }
}
