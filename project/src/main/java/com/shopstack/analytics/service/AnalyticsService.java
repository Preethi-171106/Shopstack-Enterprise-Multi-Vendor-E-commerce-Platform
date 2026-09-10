package com.shopstack.analytics.service;

import com.shopstack.analytics.dto.*;
import com.shopstack.category.entity.Category;
import com.shopstack.category.repository.CategoryRepository;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.common.repository.UserRepository;
import com.shopstack.coupon.repository.CouponRepository;
import com.shopstack.order.entity.Order;
import com.shopstack.order.enums.OrderStatus;
import com.shopstack.order.repository.OrderRepository;
import com.shopstack.payment.enums.PaymentStatus;
import com.shopstack.payment.repository.PaymentRepository;
import com.shopstack.product.entity.Product;
import com.shopstack.product.enums.ProductStatus;
import com.shopstack.product.repository.ProductRepository;
import com.shopstack.shipment.repository.ShipmentRepository;
import com.shopstack.vendor.entity.Vendor;
import com.shopstack.vendor.repository.VendorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VendorRepository vendorRepository;
    private final PaymentRepository paymentRepository;
    private final ShipmentRepository shipmentRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public AnalyticsService(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            CategoryRepository categoryRepository,
                            VendorRepository vendorRepository,
                            PaymentRepository paymentRepository,
                            ShipmentRepository shipmentRepository,
                            CouponRepository couponRepository,
                            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.vendorRepository = vendorRepository;
        this.paymentRepository = paymentRepository;
        this.shipmentRepository = shipmentRepository;
        this.couponRepository = couponRepository;
        this.userRepository = userRepository;
    }

    // ===== Admin Dashboard =====

    public AdminDashboardResponse getAdminDashboard() {
        long totalCustomers = countUsersByRole("ROLE_CUSTOMER");
        long totalVendors = countUsersByRole("ROLE_VENDOR");
        long totalProducts = productRepository.count();
        long totalCategories = categoryRepository.count();
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = nullSafeSum(orderRepository.sumTotalRevenue());
        long totalPayments = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long deliveredOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long returnedOrders = orderRepository.countByStatus(OrderStatus.RETURNED);
        long totalCoupons = couponRepository.count();
        long activeCoupons = couponRepository.countByStatus(com.shopstack.coupon.enums.CouponStatus.ACTIVE);
        long lowStockProducts = productRepository.findLowStockProducts().size();
        long outOfStockProducts = productRepository.findOutOfStockProducts().size();

        return new AdminDashboardResponse(
                totalCustomers, totalVendors, totalProducts, totalCategories,
                totalOrders, totalRevenue, totalPayments,
                pendingOrders, deliveredOrders, cancelledOrders, returnedOrders,
                totalCoupons, activeCoupons, lowStockProducts, outOfStockProducts
        );
    }

    // ===== Vendor Dashboard =====

    public VendorDashboardResponse getVendorDashboard(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor", vendorId));

        long totalProducts = productRepository.countByVendorId(vendorId);
        long activeProducts = productRepository.countByVendorIdAndStatus(vendorId, ProductStatus.ACTIVE);
        long totalOrders = orderRepository.countByVendorId(vendorId);
        long pendingOrders = orderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.PENDING);
        long deliveredOrders = orderRepository.countByVendorIdAndStatus(vendorId, OrderStatus.DELIVERED);
        BigDecimal revenue = nullSafeSum(orderRepository.sumRevenueByVendor(vendorId));

        Instant monthStart = YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal monthlyRevenue = nullSafeSum(
                orderRepository.sumRevenueByVendorAndDateRange(vendorId, monthStart, monthEnd));

        Instant todayStart = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant todayEnd = LocalDate.now().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal todayRevenue = nullSafeSum(
                orderRepository.sumRevenueByVendorAndDateRange(vendorId, todayStart, todayEnd));

        List<ProductAnalyticsResponse> topProducts = getTopSellingProductsByVendor(vendorId, 5);
        List<ProductAnalyticsResponse> lowStock = getLowStockProductsByVendor(vendorId);
        InventorySummary inventorySummary = getVendorInventorySummary(vendorId);

        return new VendorDashboardResponse(
                vendor.getStoreName(), totalProducts, activeProducts,
                totalOrders, pendingOrders, deliveredOrders,
                revenue, monthlyRevenue, todayRevenue,
                topProducts, lowStock, inventorySummary
        );
    }

    // ===== Sales Analytics =====

    public SalesSummaryResponse getDailySales(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> raw = orderRepository.findDailySalesStats(start, end);
        List<SalesDataPoint> points = raw.stream()
                .map(row -> new SalesDataPoint(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        (Long) row[1],
                        nullSafeSum((BigDecimal) row[2])))
                .toList();

        long totalOrders = points.stream().mapToLong(SalesDataPoint::orderCount).sum();
        BigDecimal totalRevenue = points.stream()
                .map(SalesDataPoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new SalesSummaryResponse(startDate, endDate, totalOrders, totalRevenue, avgOrderValue, points);
    }

    public SalesSummaryResponse getDailySalesByVendor(Long vendorId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> raw = orderRepository.findDailySalesStatsByVendor(vendorId, start, end);
        List<SalesDataPoint> points = raw.stream()
                .map(row -> new SalesDataPoint(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        (Long) row[1],
                        nullSafeSum((BigDecimal) row[2])))
                .toList();

        long totalOrders = points.stream().mapToLong(SalesDataPoint::orderCount).sum();
        BigDecimal totalRevenue = points.stream()
                .map(SalesDataPoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new SalesSummaryResponse(startDate, endDate, totalOrders, totalRevenue, avgOrderValue, points);
    }

    public SalesSummaryResponse getWeeklySales() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusWeeks(1);
        return getDailySales(start, end);
    }

    public SalesSummaryResponse getMonthlySales() {
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = LocalDate.now();
        return getDailySales(start, end);
    }

    public SalesSummaryResponse getYearlySales() {
        LocalDate start = LocalDate.of(Year.now().getValue(), 1, 1);
        LocalDate end = LocalDate.now();
        return getMonthlySalesInternal(start, end);
    }

    private SalesSummaryResponse getMonthlySalesInternal(LocalDate start, LocalDate end) {
        Instant startInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = end.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Object[]> raw = orderRepository.findMonthlySalesStats(startInstant, endInstant);
        List<SalesDataPoint> points = raw.stream()
                .map(row -> new SalesDataPoint(
                        YearMonth.parse((String) row[0]).atDay(1),
                        (Long) row[1],
                        nullSafeSum((BigDecimal) row[2])))
                .toList();

        long totalOrders = points.stream().mapToLong(SalesDataPoint::orderCount).sum();
        BigDecimal totalRevenue = points.stream()
                .map(SalesDataPoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new SalesSummaryResponse(start, end, totalOrders, totalRevenue, avgOrderValue, points);
    }

    // ===== Chart Data =====

    public RevenueChartResponse getRevenueChart(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> raw = orderRepository.findDailySalesStats(start, end);
        List<ChartPoint> points = raw.stream()
                .map(row -> new ChartPoint(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        nullSafeSum((BigDecimal) row[2])))
                .toList();

        BigDecimal total = points.stream()
                .map(ChartPoint::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RevenueChartResponse(points, total);
    }

    public OrdersChartResponse getOrdersChart(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> raw = orderRepository.findDailySalesStats(start, end);
        List<ChartPoint> points = raw.stream()
                .map(row -> new ChartPoint(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        BigDecimal.valueOf((Long) row[1])))
                .toList();

        long total = points.stream()
                .mapToLong(p -> p.value().longValue())
                .sum();

        return new OrdersChartResponse(points, total);
    }

    // ===== Top Lists =====

    public List<ProductAnalyticsResponse> getTopProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> raw = productRepository.findTopSellingProducts(pageable);
        return raw.stream()
                .map(row -> new ProductAnalyticsResponse(
                        (Long) row[0],
                        (String) row[1],
                        row[2] != null ? (Long) row[2] : 0L,
                        nullSafeSum((BigDecimal) row[3]),
                        null, null, null))
                .toList();
    }

    public List<ProductAnalyticsResponse> getTopSellingProductsByVendor(Long vendorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> raw = productRepository.findTopSellingProductsByVendor(vendorId, pageable);
        return raw.stream()
                .map(row -> new ProductAnalyticsResponse(
                        (Long) row[0],
                        (String) row[1],
                        row[2] != null ? (Long) row[2] : 0L,
                        nullSafeSum((BigDecimal) row[3]),
                        null, null, null))
                .toList();
    }

    public List<CategoryAnalyticsResponse> getTopCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(cat -> {
                    long productCount = cat.getId() != null ? countProductsByCategory(cat.getId()) : 0L;
                    return new CategoryAnalyticsResponse(cat.getId(), cat.getName(), productCount, 0L, BigDecimal.ZERO);
                })
                .collect(Collectors.toList());
    }

    public List<VendorAnalyticsResponse> getTopVendors() {
        List<Vendor> vendors = vendorRepository.findAll();
        return vendors.stream()
                .map(vendor -> {
                    long productCount = productRepository.countByVendorId(vendor.getId());
                    long orderCount = orderRepository.countByVendorId(vendor.getId());
                    BigDecimal revenue = nullSafeSum(orderRepository.sumRevenueByVendor(vendor.getId()));
                    BigDecimal avgOrder = orderCount > 0
                            ? revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new VendorAnalyticsResponse(vendor.getId(), vendor.getStoreName(),
                            productCount, orderCount, revenue, avgOrder);
                })
                .toList();
    }

    // ===== Payment Analytics =====

    public List<PaymentAnalyticsResponse> getPaymentStatistics() {
        List<Object[]> raw = paymentRepository.countSuccessfulGroupByMethod();
        long total = raw.stream().mapToLong(row -> (Long) row[1]).sum();

        return raw.stream()
                .map(row -> {
                    String method = String.valueOf(row[0]);
                    long count = (Long) row[1];
                    BigDecimal amount = nullSafeSum((BigDecimal) row[2]);
                    String percentage = total > 0
                            ? String.format("%.1f", BigDecimal.valueOf(count * 100.0 / total)) + "%"
                            : "0%";
                    return new PaymentAnalyticsResponse(method, count, amount, percentage);
                })
                .toList();
    }

    // ===== Order Status Statistics =====

    public List<OrderStatusResponse> getOrderStatusStatistics() {
        List<Object[]> raw = orderRepository.countGroupByStatus();
        return raw.stream()
                .map(row -> new OrderStatusResponse(
                        String.valueOf(row[0]),
                        (Long) row[1]))
                .toList();
    }

    public List<OrderStatusResponse> getOrderStatusStatisticsByVendor(Long vendorId) {
        List<Object[]> raw = orderRepository.countGroupByStatusByVendor(vendorId);
        return raw.stream()
                .map(row -> new OrderStatusResponse(
                        String.valueOf(row[0]),
                        (Long) row[1]))
                .toList();
    }

    // ===== Shipment Status Statistics =====

    public List<ShipmentStatusResponse> getShipmentStatusStatistics() {
        List<Object[]> raw = shipmentRepository.countGroupByStatus();
        return raw.stream()
                .map(row -> new ShipmentStatusResponse(
                        String.valueOf(row[0]),
                        (Long) row[1]))
                .toList();
    }

    // ===== Coupon Usage Statistics =====

    public List<CouponUsageResponse> getCouponUsageStatistics() {
        List<Object[]> raw = couponRepository.findCouponUsageStats();
        return raw.stream()
                .map(row -> new CouponUsageResponse(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (BigDecimal) row[3],
                        String.valueOf(row[4]),
                        row[5] != null ? (Integer) row[5] : 0,
                        (Integer) row[6]))
                .toList();
    }

    // ===== Inventory =====

    public InventorySummary getAdminInventorySummary() {
        long total = productRepository.count();
        long outOfStock = productRepository.findOutOfStockProducts().size();
        long lowStock = productRepository.findLowStockProducts().size();
        long inStock = total - outOfStock;
        return new InventorySummary(total, lowStock, outOfStock, inStock);
    }

    public InventorySummary getVendorInventorySummary(Long vendorId) {
        long total = productRepository.countByVendorId(vendorId);
        long outOfStock = productRepository.findOutOfStockProductsByVendorId(vendorId).size();
        long lowStock = productRepository.findLowStockProductsByVendorId(vendorId).size();
        long inStock = total - outOfStock;
        return new InventorySummary(total, lowStock, outOfStock, inStock);
    }

    public List<ProductAnalyticsResponse> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(this::toProductAnalytics)
                .toList();
    }

    public List<ProductAnalyticsResponse> getLowStockProductsByVendor(Long vendorId) {
        return productRepository.findLowStockProductsByVendorId(vendorId).stream()
                .map(this::toProductAnalytics)
                .toList();
    }

    // ===== Vendor Sales =====

    public SalesSummaryResponse getVendorSales(Long vendorId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return getDailySalesByVendor(vendorId, startDate, endDate);
    }

    public List<ProductAnalyticsResponse> getVendorProducts(Long vendorId) {
        return productRepository.findAll().stream()
                .filter(p -> p.getVendor() != null && p.getVendor().getId().equals(vendorId))
                .map(this::toProductAnalytics)
                .toList();
    }

    // ===== Helpers =====

    private long countUsersByRole(String role) {
        return userRepository.countByRole(role);
    }

    private long countProductsByCategory(Long categoryId) {
        return productRepository.findAll().stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                .count();
    }

    private ProductAnalyticsResponse toProductAnalytics(Product p) {
        return new ProductAnalyticsResponse(
                p.getId(),
                p.getName(),
                0L,
                BigDecimal.ZERO,
                p.getStockQuantity(),
                p.getLowStockThreshold(),
                p.getStatus() != null ? p.getStatus().name() : null);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }
    }

    private BigDecimal nullSafeSum(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
