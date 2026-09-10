package com.shopstack.service;

import com.shopstack.dto.analytics.AdminDashboardStats;
import com.shopstack.dto.analytics.MarketplaceTrendsResponse;
import com.shopstack.dto.analytics.VendorDashboardStats;
import com.shopstack.entity.OrderStatus;
import com.shopstack.entity.ReturnStatus;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorStatus;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.CommissionRepository;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.ReturnRequestRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AnalyticsService — builds real aggregate statistics from the PostgreSQL database
 * for admin and vendor dashboards. All figures come directly from JPA queries.
 */
@Service
public class AnalyticsService {

    private final UserRepository         userRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final CategoryRepository     categoryRepository;
    private final ProductRepository      productRepository;
    private final InventoryRepository    inventoryRepository;
    private final OrderRepository        orderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final CommissionRepository   commissionRepository;

    public AnalyticsService(
            UserRepository userRepository,
            VendorProfileRepository vendorProfileRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderRepository orderRepository,
            ReturnRequestRepository returnRequestRepository,
            CommissionRepository commissionRepository
    ) {
        this.userRepository          = userRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.categoryRepository      = categoryRepository;
        this.productRepository       = productRepository;
        this.inventoryRepository     = inventoryRepository;
        this.orderRepository         = orderRepository;
        this.returnRequestRepository = returnRequestRepository;
        this.commissionRepository    = commissionRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getAdminStats() {
        long totalProducts  = productRepository.count();
        long activeProducts = productRepository.findAll().stream().filter(p -> p.isActive()).count();
        long inventoryRecs  = inventoryRepository.count();
        long lowStock       = inventoryRepository.findAll().stream().filter(com.shopstack.entity.Inventory::isLowStock).count();

        long totalOrders        = orderRepository.count();
        long pendingOrders      = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.PENDING).count();
        long confirmedOrders    = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.CONFIRMED).count();
        long processingOrders   = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.PROCESSING).count();
        long shippedOrders      = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.SHIPPED).count();
        long deliveredOrders    = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED).count();
        long cancelledOrders    = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.CANCELLED).count();
        long returnOrders       = orderRepository.findAll().stream().filter(o -> o.getOrderStatus() == OrderStatus.RETURN_REQUESTED).count();

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = commissionRepository.sumTotalCommissions();

        long totalReturns   = returnRequestRepository.count();
        long pendingReturns = returnRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReturnStatus.REQUESTED).count();

        return AdminDashboardStats.builder()
                .totalCustomers(userRepository.findByRole(UserRole.CUSTOMER).size())
                .totalVendors(userRepository.findByRole(UserRole.VENDOR).size())
                .pendingVendorApprovals(vendorProfileRepository.findAll().stream()
                        .filter(v -> v.getStatus() == VendorStatus.PENDING).count())
                .totalWarehouseStaff(userRepository.findByRole(UserRole.WAREHOUSE_STAFF).size())
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalCategories(categoryRepository.count())
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .confirmedOrders(confirmedOrders)
                .processingOrders(processingOrders)
                .shippedOrders(shippedOrders)
                .deliveredOrders(deliveredOrders)
                .cancelledOrders(cancelledOrders)
                .returnRequestedOrders(returnOrders)
                .totalRevenue(totalRevenue)
                .totalPlatformCommission(totalCommission != null ? totalCommission : BigDecimal.ZERO)
                .totalVendorPayouts(totalRevenue.subtract(totalCommission != null ? totalCommission : BigDecimal.ZERO))
                .lowStockProducts(lowStock)
                .inventoryRecords(inventoryRecs)
                .totalReturnRequests(totalReturns)
                .pendingReturnRequests(pendingReturns)
                .build();
    }

    @Transactional(readOnly = true)
    public MarketplaceTrendsResponse getMarketplaceTrends() {
        var allOrders = orderRepository.findAll();

        // 1. Order status distribution
        Map<String, Long> statusDist = new LinkedHashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            long count = allOrders.stream().filter(o -> o.getOrderStatus() == st).count();
            statusDist.put(st.name(), count);
        }

        // 2. Daily sales trends (sorted by date)
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<com.shopstack.entity.Order>> byDay = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getOrderStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().format(dayFmt), TreeMap::new, Collectors.toList()));

        List<MarketplaceTrendsResponse.DailyTrend> dailyTrends = byDay.entrySet().stream()
                .map(e -> {
                    BigDecimal rev = e.getValue().stream()
                            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new MarketplaceTrendsResponse.DailyTrend(e.getKey(), rev, e.getValue().size());
                })
                .collect(Collectors.toList());

        // 3. Monthly sales trends
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<com.shopstack.entity.Order>> byMonth = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getOrderStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().format(monthFmt), TreeMap::new, Collectors.toList()));

        List<MarketplaceTrendsResponse.MonthlyTrend> monthlyTrends = byMonth.entrySet().stream()
                .map(e -> {
                    BigDecimal rev = e.getValue().stream()
                            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new MarketplaceTrendsResponse.MonthlyTrend(e.getKey(), rev, e.getValue().size());
                })
                .collect(Collectors.toList());

        // 4. Category breakdown
        var allCategories = categoryRepository.findAll();
        var allProducts = productRepository.findAll();
        List<MarketplaceTrendsResponse.CategorySales> categorySales = allCategories.stream()
                .map(cat -> {
                    long prodCount = allProducts.stream()
                            .filter(p -> p.getCategory() != null && cat.getId().equals(p.getCategory().getId()))
                            .count();
                    return new MarketplaceTrendsResponse.CategorySales(cat.getName(), prodCount, BigDecimal.ZERO);
                })
                .collect(Collectors.toList());

        // 5. Vendor Performance
        var allVendors = vendorProfileRepository.findAll();
        List<MarketplaceTrendsResponse.VendorSalesPerformance> topVendors = allVendors.stream()
                .map(vp -> {
                    BigDecimal sales = commissionRepository.sumSalesByVendor(vp.getId());
                    long orderCount = commissionRepository.findByVendorProfileId(vp.getId()).size();
                    return new MarketplaceTrendsResponse.VendorSalesPerformance(
                            vp.getId(),
                            vp.getStoreName(),
                            sales != null ? sales : BigDecimal.ZERO,
                            orderCount
                    );
                })
                .sorted((v1, v2) -> v2.getTotalSales().compareTo(v1.getTotalSales()))
                .collect(Collectors.toList());

        return MarketplaceTrendsResponse.builder()
                .dailySalesTrend(dailyTrends)
                .monthlySalesTrend(monthlyTrends)
                .orderStatusDistribution(statusDist)
                .topCategories(categorySales)
                .topVendors(topVendors)
                .build();
    }

    @Transactional(readOnly = true)
    public VendorDashboardStats getVendorStats() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var vendorProfile = vendorProfileRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new com.shopstack.exception.VendorProfileNotFoundException(
                        "No vendor profile found for: " + email));
        Long vpId = vendorProfile.getId();

        long totalProducts  = productRepository.findByVendorProfileId(vpId).size();
        long activeProducts = productRepository.findByVendorProfileId(vpId).stream()
                .filter(p -> p.isActive()).count();
        long lowStock = inventoryRepository.findAll().stream()
                .filter(i -> i.getProduct() != null
                          && i.getProduct().getVendorProfile() != null
                          && vpId.equals(i.getProduct().getVendorProfile().getId())
                          && i.isLowStock())
                .count();

        var allOrders = orderRepository.findAll();
        long totalOrders = allOrders.stream()
                .filter(o -> o.getItems().stream()
                        .anyMatch(item -> item.getVendorProfile() != null
                                && vpId.equals(item.getVendorProfile().getId())))
                .count();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.PENDING
                        && o.getItems().stream().anyMatch(item -> item.getVendorProfile() != null
                                && vpId.equals(item.getVendorProfile().getId())))
                .count();
        long deliveredOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED
                        && o.getItems().stream().anyMatch(item -> item.getVendorProfile() != null
                                && vpId.equals(item.getVendorProfile().getId())))
                .count();

        BigDecimal totalSales     = commissionRepository.sumSalesByVendor(vpId);
        BigDecimal commissionPaid = commissionRepository.findByVendorProfileId(vpId).stream()
                .map(c -> c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netRevenue     = commissionRepository.sumVendorNetByVendor(vpId);

        return VendorDashboardStats.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .lowStockProducts(lowStock)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .deliveredOrders(deliveredOrders)
                .totalSales(totalSales != null ? totalSales : BigDecimal.ZERO)
                .totalCommissionPaid(commissionPaid)
                .netRevenue(netRevenue != null ? netRevenue : BigDecimal.ZERO)
                .build();
    }
}
