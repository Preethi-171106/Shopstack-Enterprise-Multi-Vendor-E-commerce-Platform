package com.shopstack.analytics.service;

import com.shopstack.analytics.dto.*;
import com.shopstack.category.entity.Category;
import com.shopstack.category.repository.CategoryRepository;
import com.shopstack.common.exception.BadRequestException;
import com.shopstack.common.exception.ResourceNotFoundException;
import com.shopstack.coupon.enums.CouponStatus;
import com.shopstack.coupon.repository.CouponRepository;
import com.shopstack.order.enums.OrderStatus;
import com.shopstack.order.repository.OrderRepository;
import com.shopstack.payment.enums.PaymentStatus;
import com.shopstack.payment.repository.PaymentRepository;
import com.shopstack.product.entity.Product;
import com.shopstack.product.enums.ProductStatus;
import com.shopstack.product.repository.ProductRepository;
import com.shopstack.shipment.repository.ShipmentRepository;
import com.shopstack.common.repository.UserRepository;
import com.shopstack.vendor.entity.Vendor;
import com.shopstack.vendor.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AnalyticsService analyticsService;

    private Vendor vendor;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .id(1L)
                .storeName("TechStore")
                .active(true)
                .build();
    }

    // ===== Admin Dashboard =====

    @Test
    void getAdminDashboard_returnsAllMetrics() {
        when(userRepository.countByRole("ROLE_CUSTOMER")).thenReturn(100L);
        when(userRepository.countByRole("ROLE_VENDOR")).thenReturn(20L);
        when(productRepository.count()).thenReturn(500L);
        when(categoryRepository.count()).thenReturn(10L);
        when(orderRepository.count()).thenReturn(1000L);
        when(orderRepository.sumTotalRevenue()).thenReturn(BigDecimal.valueOf(50000));
        when(paymentRepository.countByStatus(PaymentStatus.SUCCESS)).thenReturn(800L);
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(50L);
        when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(700L);
        when(orderRepository.countByStatus(OrderStatus.CANCELLED)).thenReturn(100L);
        when(orderRepository.countByStatus(OrderStatus.RETURNED)).thenReturn(30L);
        when(couponRepository.count()).thenReturn(25L);
        when(couponRepository.countByStatus(CouponStatus.ACTIVE)).thenReturn(15L);
        when(productRepository.findLowStockProducts()).thenReturn(List.of());
        when(productRepository.findOutOfStockProducts()).thenReturn(List.of());

        AdminDashboardResponse result = analyticsService.getAdminDashboard();

        assertEquals(100, result.totalCustomers());
        assertEquals(20, result.totalVendors());
        assertEquals(500, result.totalProducts());
        assertEquals(10, result.totalCategories());
        assertEquals(1000, result.totalOrders());
        assertEquals(BigDecimal.valueOf(50000), result.totalRevenue());
        assertEquals(800, result.totalPayments());
        assertEquals(50, result.pendingOrders());
        assertEquals(700, result.deliveredOrders());
        assertEquals(100, result.cancelledOrders());
        assertEquals(30, result.returnedOrders());
        assertEquals(25, result.totalCoupons());
        assertEquals(15, result.activeCoupons());
    }

    @Test
    void getAdminDashboard_nullRevenue_returnsZero() {
        when(userRepository.countByRole(any())).thenReturn(0L);
        when(productRepository.count()).thenReturn(0L);
        when(categoryRepository.count()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumTotalRevenue()).thenReturn(null);
        when(paymentRepository.countByStatus(any())).thenReturn(0L);
        when(orderRepository.countByStatus(any())).thenReturn(0L);
        when(couponRepository.count()).thenReturn(0L);
        when(couponRepository.countByStatus(any())).thenReturn(0L);
        when(productRepository.findLowStockProducts()).thenReturn(List.of());
        when(productRepository.findOutOfStockProducts()).thenReturn(List.of());

        AdminDashboardResponse result = analyticsService.getAdminDashboard();

        assertEquals(BigDecimal.ZERO, result.totalRevenue());
    }

    // ===== Vendor Dashboard =====

    @Test
    void getVendorDashboard_returnsAllMetrics() {
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));
        when(productRepository.countByVendorId(1L)).thenReturn(50L);
        when(productRepository.countByVendorIdAndStatus(1L, ProductStatus.ACTIVE)).thenReturn(45L);
        when(orderRepository.countByVendorId(1L)).thenReturn(200L);
        when(orderRepository.countByVendorIdAndStatus(1L, OrderStatus.PENDING)).thenReturn(10L);
        when(orderRepository.countByVendorIdAndStatus(1L, OrderStatus.DELIVERED)).thenReturn(150L);
        when(orderRepository.sumRevenueByVendor(1L)).thenReturn(BigDecimal.valueOf(25000));
        when(orderRepository.sumRevenueByVendorAndDateRange(eq(1L), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000));
        when(productRepository.findTopSellingProductsByVendor(eq(1L), any())).thenReturn(List.of());
        when(productRepository.findLowStockProductsByVendorId(1L)).thenReturn(List.of());

        VendorDashboardResponse result = analyticsService.getVendorDashboard(1L);

        assertEquals("TechStore", result.storeName());
        assertEquals(50, result.totalProducts());
        assertEquals(45, result.activeProducts());
        assertEquals(200, result.totalOrders());
        assertEquals(10, result.pendingOrders());
        assertEquals(150, result.deliveredOrders());
        assertEquals(BigDecimal.valueOf(25000), result.revenue());
        assertNotNull(result.inventorySummary());
    }

    @Test
    void getVendorDashboard_vendorNotFound_throws() {
        when(vendorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> analyticsService.getVendorDashboard(999L));
    }

    // ===== Sales Analytics =====

    @Test
    void getDailySales_validRange_returnsSummary() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 2);
        when(orderRepository.findDailySalesStats(any(), any())).thenReturn(List.of());

        SalesSummaryResponse result = analyticsService.getDailySales(start, end);

        assertEquals(start, result.startDate());
        assertEquals(end, result.endDate());
        assertEquals(0, result.totalOrders());
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
    }

    @Test
    void getDailySales_nullDates_throwsBadRequest() {
        assertThrows(BadRequestException.class,
                () -> analyticsService.getDailySales(null, LocalDate.now()));
    }

    @Test
    void getDailySales_startAfterEnd_throwsBadRequest() {
        LocalDate start = LocalDate.of(2024, 12, 1);
        LocalDate end = LocalDate.of(2024, 1, 1);

        assertThrows(BadRequestException.class,
                () -> analyticsService.getDailySales(start, end));
    }

    @Test
    void getWeeklySales_returns7DayRange() {
        when(orderRepository.findDailySalesStats(any(), any())).thenReturn(List.of());

        SalesSummaryResponse result = analyticsService.getWeeklySales();

        assertNotNull(result.startDate());
        assertNotNull(result.endDate());
    }

    @Test
    void getMonthlySales_returnsCurrentMonth() {
        when(orderRepository.findDailySalesStats(any(), any())).thenReturn(List.of());

        SalesSummaryResponse result = analyticsService.getMonthlySales();

        assertEquals(YearMonth.now().atDay(1), result.startDate());
    }

    @Test
    void getYearlySales_returnsCurrentYear() {
        when(orderRepository.findMonthlySalesStats(any(), any())).thenReturn(List.of());

        SalesSummaryResponse result = analyticsService.getYearlySales();

        assertEquals(LocalDate.of(LocalDate.now().getYear(), 1, 1), result.startDate());
    }

    // ===== Chart Data =====

    @Test
    void getRevenueChart_validDates_returnsChart() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(orderRepository.findDailySalesStats(any(), any())).thenReturn(List.of());

        RevenueChartResponse result = analyticsService.getRevenueChart(start, end);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
    }

    @Test
    void getRevenueChart_invalidDates_throws() {
        assertThrows(BadRequestException.class,
                () -> analyticsService.getRevenueChart(null, null));
    }

    @Test
    void getOrdersChart_validDates_returnsChart() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(orderRepository.findDailySalesStats(any(), any())).thenReturn(List.of());

        OrdersChartResponse result = analyticsService.getOrdersChart(start, end);

        assertNotNull(result);
        assertEquals(0, result.totalOrders());
    }

    // ===== Top Lists =====

    @Test
    void getTopProducts_returnsList() {
        when(productRepository.findTopSellingProducts(any())).thenReturn(List.of());

        List<ProductAnalyticsResponse> result = analyticsService.getTopProducts(10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTopCategories_returnsList() {
        Category cat = Category.builder().id(1L).name("Electronics").active(true).build();
        when(categoryRepository.findAll()).thenReturn(List.of(cat));
        when(productRepository.findAll()).thenReturn(List.of());

        List<CategoryAnalyticsResponse> result = analyticsService.getTopCategories();

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).categoryName());
    }

    @Test
    void getTopVendors_returnsList() {
        when(vendorRepository.findAll()).thenReturn(List.of(vendor));
        when(productRepository.countByVendorId(1L)).thenReturn(50L);
        when(orderRepository.countByVendorId(1L)).thenReturn(200L);
        when(orderRepository.sumRevenueByVendor(1L)).thenReturn(BigDecimal.valueOf(25000));

        List<VendorAnalyticsResponse> result = analyticsService.getTopVendors();

        assertEquals(1, result.size());
        assertEquals("TechStore", result.get(0).storeName());
        assertEquals(50, result.get(0).productCount());
        assertEquals(200, result.get(0).orderCount());
    }

    @Test
    void getTopVendors_emptyDataset_returnsEmpty() {
        when(vendorRepository.findAll()).thenReturn(List.of());

        List<VendorAnalyticsResponse> result = analyticsService.getTopVendors();

        assertTrue(result.isEmpty());
    }

    // ===== Payment Analytics =====

    @Test
    void getPaymentStatistics_returnsList() {
        when(paymentRepository.countSuccessfulGroupByMethod())
                .thenReturn(List.<Object[]>of(new Object[]{"CREDIT_CARD", 500L, BigDecimal.valueOf(25000)}));

        List<PaymentAnalyticsResponse> result = analyticsService.getPaymentStatistics();

        assertEquals(1, result.size());
        assertEquals("CREDIT_CARD", result.get(0).method());
        assertEquals(500, result.get(0).count());
    }

    @Test
    void getPaymentStatistics_emptyDataset_returnsEmpty() {
        when(paymentRepository.countSuccessfulGroupByMethod()).thenReturn(List.of());

        List<PaymentAnalyticsResponse> result = analyticsService.getPaymentStatistics();

        assertTrue(result.isEmpty());
    }

    // ===== Order/Shipment/Coupon Stats =====

    @Test
    void getOrderStatusStatistics_returnsList() {
        when(orderRepository.countGroupByStatus())
                .thenReturn(List.<Object[]>of(new Object[]{"PENDING", 50L}));

        List<OrderStatusResponse> result = analyticsService.getOrderStatusStatistics();

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).status());
        assertEquals(50, result.get(0).count());
    }

    @Test
    void getShipmentStatusStatistics_returnsList() {
        when(shipmentRepository.countGroupByStatus())
                .thenReturn(List.<Object[]>of(new Object[]{"DELIVERED", 100L}));

        List<ShipmentStatusResponse> result = analyticsService.getShipmentStatusStatistics();

        assertEquals(1, result.size());
        assertEquals("DELIVERED", result.get(0).status());
    }

    @Test
    void getCouponUsageStatistics_returnsList() {
        when(couponRepository.findCouponUsageStats())
                .thenReturn(List.<Object[]>of(new Object[]{"SAVE10", "10% off", "PERCENTAGE", BigDecimal.TEN, "ACTIVE", 50, 100}));

        List<CouponUsageResponse> result = analyticsService.getCouponUsageStatistics();

        assertEquals(1, result.size());
        assertEquals("SAVE10", result.get(0).code());
        assertEquals(50, result.get(0).usedCount());
    }

    // ===== Inventory =====

    @Test
    void getAdminInventorySummary_returnsSummary() {
        when(productRepository.count()).thenReturn(100L);
        when(productRepository.findOutOfStockProducts()).thenReturn(List.of());
        when(productRepository.findLowStockProducts()).thenReturn(List.of());

        InventorySummary result = analyticsService.getAdminInventorySummary();

        assertEquals(100, result.totalProducts());
        assertEquals(100, result.inStockCount());
    }

    @Test
    void getVendorInventorySummary_returnsSummary() {
        when(productRepository.countByVendorId(1L)).thenReturn(50L);
        when(productRepository.findOutOfStockProductsByVendorId(1L)).thenReturn(List.of());
        when(productRepository.findLowStockProductsByVendorId(1L)).thenReturn(List.of());

        InventorySummary result = analyticsService.getVendorInventorySummary(1L);

        assertEquals(50, result.totalProducts());
        assertEquals(50, result.inStockCount());
    }

    // ===== Vendor Sales =====

    @Test
    void getVendorSales_validDates_returnsSummary() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(orderRepository.findDailySalesStatsByVendor(eq(1L), any(), any())).thenReturn(List.of());

        SalesSummaryResponse result = analyticsService.getVendorSales(1L, start, end);

        assertEquals(start, result.startDate());
        assertEquals(end, result.endDate());
    }

    @Test
    void getVendorSales_invalidDates_throws() {
        assertThrows(BadRequestException.class,
                () -> analyticsService.getVendorSales(1L, null, null));
    }
}
