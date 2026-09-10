package com.shopstack.service;

import com.shopstack.dto.*;
import com.shopstack.entity.*;
import com.shopstack.exception.AccessDeniedException;
import com.shopstack.exception.ReportException;
import com.shopstack.mapper.ReportMapper;
import com.shopstack.repository.*;
import com.shopstack.security.SecurityHelper;
import com.shopstack.util.ReportDateResolver;
import com.shopstack.util.ReportTabulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    static final int TOP_LIMIT = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepoForLookup;
    private final SecurityHelper securityHelper;

    public ReportService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         ProductRepository productRepository,
                         VendorRepository vendorRepository,
                         PaymentRepository paymentRepository,
                         CouponRepository couponRepository,
                         UserRepository userRepository,
                         SecurityHelper securityHelper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.vendorRepository = vendorRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
        this.vendorRepoForLookup = vendorRepository;
        this.securityHelper = securityHelper;
    }

    // ---- Sales ----

    public SalesReportDto salesReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportDateResolver.Resolved window = ReportDateResolver.resolve(filter, preset);
        LocalDateTime start = window.start();
        LocalDateTime end = window.end();
        Long vendorId = scopedVendorId(filter);

        BigDecimal revenue;
        Long ordersCount;
        Long cancelled;
        Long refunded;
        BigDecimal aov;
        if (vendorId != null) {
            revenue = orderRepository.sumRevenueByVendorBetween(vendorId, start, end);
            ordersCount = orderRepository.countByVendorBetween(vendorId, start, end);
            cancelled = orderRepository.countByVendorAndStatusBetween(vendorId, OrderStatus.CANCELLED, start, end);
            refunded = orderRepository.countByVendorAndStatusBetween(vendorId, OrderStatus.REFUNDED, start, end);
            aov = orderRepository.avgOrderValueByVendorBetween(vendorId, start, end);
        } else {
            revenue = orderRepository.sumRevenueBetween(start, end);
            ordersCount = orderRepository.countOrdersBetween(start, end);
            cancelled = orderRepository.countByStatusBetween(OrderStatus.CANCELLED, start, end);
            refunded = orderRepository.countByStatusBetween(OrderStatus.REFUNDED, start, end);
            aov = orderRepository.averageOrderValueBetween(start, end);
        }

        List<RevenueBreakdownProjection> breakdown = orderRepository.revenueBreakdown(
                start, end, window.format(), filter != null ? filter.getStatus() : null, vendorId);
        List<StatusCountProjection> statusCounts = orderRepository.statusCounts(start, end);

        return SalesReportDto.builder()
                .period(SalesReportDto.LocalDateRange.builder()
                        .from(start.toLocalDate().toString())
                        .to(end.toLocalDate().minusDays(1).toString())
                        .build())
                .revenue(ReportMapper.nz(revenue))
                .ordersCount(ordersCount == null ? 0L : ordersCount)
                .cancelledOrders(cancelled == null ? 0L : cancelled)
                .refundedOrders(refunded == null ? 0L : refunded)
                .averageOrderValue(ReportMapper.nz(aov))
                .breakdown(ReportMapper.toBreakdown(breakdown))
                .statusCounts(ReportMapper.toStatusCounts(statusCounts))
                .build();
    }

    // ---- Vendor ----

    public VendorReportDto vendorReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportDateResolver.Resolved window = ReportDateResolver.resolve(filter, preset);
        Long vendorId = scopedVendorId(filter);

        List<VendorSummaryProjection> summaries = vendorRepository.vendorSummaries(
                window.start(), window.end(), vendorId);
        List<VendorSummaryProjection> top = vendorRepository.topVendors(
                window.start(), window.end(), TOP_LIMIT);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalOrders = 0L;
        long totalProducts = 0L;
        for (VendorSummaryProjection s : summaries) {
            totalRevenue = totalRevenue.add(ReportMapper.nz(s.getRevenue()));
            totalOrders += ReportMapper.nzLong(s.getOrdersCount());
            totalProducts += ReportMapper.nzLong(s.getProductsCount());
        }

        return VendorReportDto.builder()
                .vendors(ReportMapper.toVendorSummaries(summaries))
                .topVendors(ReportMapper.toVendorSummaries(top))
                .totalVendorRevenue(totalRevenue)
                .totalVendorOrders(totalOrders)
                .totalVendorProducts(totalProducts)
                .build();
    }

    // ---- Product ----

    public ProductReportDto productReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportDateResolver.Resolved window = ReportDateResolver.resolve(filter, preset);
        Long vendorId = scopedVendorId(filter);
        Long categoryId = filter != null ? filter.getCategoryId() : null;

        List<ProductRevenueProjection> revenueRows = orderItemRepository.productRevenue(
                window.start(), window.end(), vendorId, categoryId);
        List<Product> products = productRepository.findForInventory(vendorId, categoryId);

        List<ProductReportDto.ProductSummary> bestSelling = ReportMapper.toBestSelling(revenueRows);
        List<ProductReportDto.ProductRevenue> revenue = ReportMapper.toProductRevenue(revenueRows);

        List<ProductReportDto.ProductSummary> lowStock = products.stream()
                .filter(p -> p.getStockQuantity() > 0 && p.getStockQuantity() <= p.getLowStockThreshold())
                .map(this::toSummary)
                .toList();
        List<ProductReportDto.ProductSummary> outOfStock = products.stream()
                .filter(p -> p.getStockQuantity() <= 0)
                .map(this::toSummary)
                .toList();

        BigDecimal totalRevenue = revenue.stream()
                .map(ProductReportDto.ProductRevenue::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ProductReportDto.builder()
                .bestSellingProducts(bestSelling)
                .lowStockProducts(lowStock)
                .outOfStockProducts(outOfStock)
                .productRevenue(revenue)
                .totalProductRevenue(totalRevenue)
                .build();
    }

    private ProductReportDto.ProductSummary toSummary(Product p) {
        return ProductReportDto.ProductSummary.builder()
                .productId(p.getId())
                .productName(p.getName())
                .stockQuantity(p.getStockQuantity())
                .price(p.getPrice())
                .build();
    }

    // ---- Customer ----

    public CustomerReportDto customerReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportDateResolver.Resolved window = ReportDateResolver.resolve(filter, preset);
        LocalDateTime start = window.start();
        LocalDateTime end = window.end();

        long total = userRepository.countByRole(UserRole.CUSTOMER);
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(UserRole.CUSTOMER, start, end);
        BigDecimal totalSpending = userRepository.totalCustomerSpendingBetween(start, end);
        List<CustomerSummaryProjection> top = userRepository.topCustomers(start, end, TOP_LIMIT);

        BigDecimal avg = total == 0 ? BigDecimal.ZERO
                : ReportMapper.nz(totalSpending).divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP);

        return CustomerReportDto.builder()
                .totalCustomers(total)
                .newCustomers(newCustomers)
                .totalCustomerSpending(ReportMapper.nz(totalSpending))
                .averageCustomerSpending(avg)
                .topCustomers(ReportMapper.toCustomerSummaries(top))
                .build();
    }

    // ---- Payment ----

    public PaymentReportDto paymentReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportDateResolver.Resolved window = ReportDateResolver.resolve(filter, preset);
        LocalDateTime start = window.start();
        LocalDateTime end = window.end();
        String status = filter != null ? filter.getStatus() : null;

        long successful = paymentRepository.countByStatusAndPaymentDateBetween(PaymentStatus.SUCCESSFUL, start, end);
        long failed = paymentRepository.countByStatusAndPaymentDateBetween(PaymentStatus.FAILED, start, end);
        long refunded = paymentRepository.countByStatusAndPaymentDateBetween(PaymentStatus.REFUNDED, start, end);
        BigDecimal successAmount = paymentRepository.sumAmountByStatusBetween(PaymentStatus.SUCCESSFUL, start, end);
        BigDecimal refundedAmount = paymentRepository.sumAmountByStatusBetween(PaymentStatus.REFUNDED, start, end);
        List<PaymentMethodStatProjection> methodStats = paymentRepository.paymentMethodStats(start, end, status);

        return PaymentReportDto.builder()
                .successfulPayments(successful)
                .failedPayments(failed)
                .refundedPayments(refunded)
                .successfulAmount(ReportMapper.nz(successAmount))
                .refundedAmount(ReportMapper.nz(refundedAmount))
                .paymentMethodStats(ReportMapper.toPaymentMethodStats(methodStats))
                .build();
    }

    // ---- Coupon ----

    public CouponReportDto couponReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportDateResolver.Resolved window = ReportDateResolver.resolve(filter, preset);
        List<CouponSummaryProjection> rows = couponRepository.couponUsageBetween(
                window.start(), window.end());
        BigDecimal totalDiscount = couponRepository.totalDiscountAll();
        Long totalUsage = couponRepository.totalUsageAll();

        return CouponReportDto.builder()
                .coupons(ReportMapper.toCouponSummaries(rows))
                .totalDiscount(ReportMapper.nz(totalDiscount))
                .totalUsage(totalUsage == null ? 0L : totalUsage)
                .build();
    }

    // ---- Inventory ----

    public InventoryReportDto inventoryReport(ReportFilter filter) {
        Long vendorId = scopedVendorId(filter);
        Long categoryId = filter != null ? filter.getCategoryId() : null;
        List<Product> products = productRepository.findForInventory(vendorId, categoryId);

        List<InventoryReportDto.InventoryItem> items = products.stream()
                .map(p -> ReportMapper.toInventoryItem(toProjection(p)))
                .toList();
        List<InventoryReportDto.InventoryItem> lowStock = items.stream()
                .filter(i -> "LOW_STOCK".equals(i.getStatus())).toList();
        List<InventoryReportDto.InventoryItem> outOfStock = items.stream()
                .filter(i -> "OUT_OF_STOCK".equals(i.getStatus())).toList();

        BigDecimal stockValue = items.stream()
                .map(InventoryReportDto.InventoryItem::getStockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InventoryReportDto.builder()
                .totalProducts((long) items.size())
                .lowStockCount((long) lowStock.size())
                .outOfStockCount((long) outOfStock.size())
                .stockValue(stockValue)
                .lowStockItems(lowStock)
                .outOfStockItems(outOfStock)
                .summary(items)
                .build();
    }

    private InventoryItemProjection toProjection(Product p) {
        return new InventoryItemProjection() {
            @Override public Long getProductId() { return p.getId(); }
            @Override public String getProductName() { return p.getName(); }
            @Override public Integer getStockQuantity() { return p.getStockQuantity(); }
            @Override public Integer getLowStockThreshold() { return p.getLowStockThreshold(); }
            @Override public BigDecimal getUnitPrice() { return p.getPrice(); }
        };
    }

    // ---- Export ----

    public ReportTabulator.Tabular tabulate(ReportType type, ReportFilter filter,
                                             ReportDateResolver.Preset preset) {
        Object report = switch (type) {
            case SALES -> salesReport(filter, preset);
            case VENDORS -> vendorReport(filter, preset);
            case PRODUCTS -> productReport(filter, preset);
            case CUSTOMERS -> customerReport(filter, preset);
            case PAYMENTS -> paymentReport(filter, preset);
            case COUPONS -> couponReport(filter, preset);
            case INVENTORY -> inventoryReport(filter);
        };
        return ReportTabulator.tabulate(type, report);
    }

    // ---- Vendor scoping ----

    /**
     * If the caller is a VENDOR, force the vendorId to their own vendor record,
     * ignoring any client-supplied vendorId. ADMIN/WAREHOUSE may pass any vendorId.
     */
    Long scopedVendorId(ReportFilter filter) {
        String role = securityHelper.currentRole();
        if ("VENDOR".equalsIgnoreCase(role)) {
            Long userId = securityHelper.currentUserId();
            return vendorRepoForLookup.findByUserId(userId)
                    .map(Vendor::getId)
                    .orElseThrow(() -> new AccessDeniedException("Vendor profile not found for current user"));
        }
        return filter != null ? filter.getVendorId() : null;
    }

    public VendorReportDto vendorSelfReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportFilter scoped = ensureVendorScoped(filter);
        return vendorReport(scoped, preset);
    }

    public ProductReportDto vendorSelfProductReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportFilter scoped = ensureVendorScoped(filter);
        return productReport(scoped, preset);
    }

    public InventoryReportDto vendorSelfInventoryReport(ReportFilter filter) {
        ReportFilter scoped = ensureVendorScoped(filter);
        return inventoryReport(scoped);
    }

    public SalesReportDto vendorSelfSalesReport(ReportFilter filter, ReportDateResolver.Preset preset) {
        ReportFilter scoped = ensureVendorScoped(filter);
        return salesReport(scoped, preset);
    }

    private ReportFilter ensureVendorScoped(ReportFilter filter) {
        if (!securityHelper.hasRole("VENDOR")) {
            throw new AccessDeniedException("Only vendors may access self reports");
        }
        Long vendorId = scopedVendorId(filter);
        ReportFilter scoped = filter == null ? new ReportFilter() : filter;
        scoped.setVendorId(vendorId);
        return scoped;
    }

    public static ReportDateResolver.Preset presetFromString(String preset) {
        if (preset == null || preset.isBlank()) {
            return null;
        }
        try {
            return ReportDateResolver.Preset.valueOf(preset.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ReportException("Invalid preset: " + preset
                    + " (allowed: daily, weekly, monthly, yearly, custom)");
        }
    }

    public ReportFilter normalizeFilter(LocalDate fromDate, LocalDate toDate, Long vendorId,
                                        Long categoryId, String status) {
        ReportFilter filter = new ReportFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setVendorId(vendorId);
        filter.setCategoryId(categoryId);
        filter.setStatus(status);
        return filter;
    }
}
