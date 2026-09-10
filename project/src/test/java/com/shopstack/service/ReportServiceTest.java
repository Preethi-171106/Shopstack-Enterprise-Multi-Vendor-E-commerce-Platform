package com.shopstack.service;

import com.shopstack.dto.*;
import com.shopstack.exception.AccessDeniedException;
import com.shopstack.exception.ReportException;
import com.shopstack.repository.*;
import com.shopstack.security.SecurityHelper;
import com.shopstack.util.ReportDateResolver;
import com.shopstack.util.ReportTabulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService unit tests")
class ReportServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRepository productRepository;
    @Mock VendorRepository vendorRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock CouponRepository couponRepository;
    @Mock UserRepository userRepository;
    @Mock SecurityHelper securityHelper;

    ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(orderRepository, orderItemRepository, productRepository,
                vendorRepository, paymentRepository, couponRepository, userRepository, securityHelper);
    }

    private ReportFilter filter() {
        ReportFilter f = new ReportFilter();
        f.setFromDate(LocalDate.of(2024, 1, 1));
        f.setToDate(LocalDate.of(2024, 1, 31));
        return f;
    }

    @Nested
    @DisplayName("Sales report")
    class Sales {
        @Test
        @DisplayName("aggregates revenue, counts, AOV and breakdown")
        void aggregatesSalesReport() {
            ReportFilter filter = filter();
            when(securityHelper.currentRole()).thenReturn("ADMIN");
            when(orderRepository.sumRevenueBetween(any(), any())).thenReturn(new BigDecimal("1250.00"));
            when(orderRepository.countOrdersBetween(any(), any())).thenReturn(10L);
            when(orderRepository.countByStatusBetween(eq(com.shopstack.entity.OrderStatus.CANCELLED), any(), any())).thenReturn(2L);
            when(orderRepository.countByStatusBetween(eq(com.shopstack.entity.OrderStatus.REFUNDED), any(), any())).thenReturn(1L);
            when(orderRepository.averageOrderValueBetween(any(), any())).thenReturn(new BigDecimal("125.00"));
            when(orderRepository.revenueBreakdown(any(), any(), anyString(), any(), any())).thenReturn(List.of());
            when(orderRepository.statusCounts(any(), any())).thenReturn(List.of());

            SalesReportDto dto = service.salesReport(filter, null);

            assertThat(dto.getRevenue()).isEqualByComparingTo("1250.00");
            assertThat(dto.getOrdersCount()).isEqualTo(10L);
            assertThat(dto.getCancelledOrders()).isEqualTo(2L);
            assertThat(dto.getRefundedOrders()).isEqualTo(1L);
            assertThat(dto.getAverageOrderValue()).isEqualByComparingTo("125.00");
            assertThat(dto.getBreakdown()).isEmpty();
            verify(orderRepository).sumRevenueBetween(any(), any());
        }

        @Test
        @DisplayName("rejects toDate before fromDate")
        void rejectsBadRange() {
            ReportFilter f = new ReportFilter();
            f.setFromDate(LocalDate.of(2024, 2, 10));
            f.setToDate(LocalDate.of(2024, 2, 1));
            assertThatThrownBy(() -> service.salesReport(f, null))
                    .isInstanceOf(ReportException.class)
                    .hasMessageContaining("toDate");
        }
    }

    @Nested
    @DisplayName("Vendor report")
    class Vendor {
        @Test
        @DisplayName("sums vendor revenue/orders/products and maps top vendors")
        void vendorReport() {
            when(securityHelper.currentRole()).thenReturn("ADMIN");
            VendorSummaryProjection row = mock(VendorSummaryProjection.class);
            when(row.getVendorId()).thenReturn(7L);
            when(row.getVendorName()).thenReturn("Acme");
            when(row.getRevenue()).thenReturn(new BigDecimal("500.00"));
            when(row.getOrdersCount()).thenReturn(3L);
            when(row.getProductsCount()).thenReturn(12L);
            when(vendorRepository.vendorSummaries(any(), any(), any())).thenReturn(List.of(row));
            when(vendorRepository.topVendors(any(), any(), anyInt())).thenReturn(List.of(row));

            VendorReportDto dto = service.vendorReport(filter(), null);

            assertThat(dto.getVendors()).hasSize(1);
            assertThat(dto.getVendors().get(0).getVendorName()).isEqualTo("Acme");
            assertThat(dto.getTotalVendorRevenue()).isEqualByComparingTo("500.00");
            assertThat(dto.getTotalVendorOrders()).isEqualTo(3L);
            assertThat(dto.getTotalVendorProducts()).isEqualTo(12L);
            assertThat(dto.getTopVendors()).hasSize(1);
        }

        @Test
        @DisplayName("VENDOR role is forced to own vendor id and ignores supplied vendorId")
        void vendorScoped() {
            when(securityHelper.hasRole("VENDOR")).thenReturn(true);
            when(securityHelper.currentRole()).thenReturn("VENDOR");
            when(securityHelper.currentUserId()).thenReturn(2L);
            com.shopstack.entity.Vendor v = com.shopstack.entity.Vendor.builder()
                    .id(42L).userId(2L).name("Self").build();
            when(vendorRepository.findByUserId(2L)).thenReturn(java.util.Optional.of(v));
            VendorSummaryProjection row = mock(VendorSummaryProjection.class);
            when(row.getVendorId()).thenReturn(42L);
            when(row.getVendorName()).thenReturn("Self");
            when(row.getRevenue()).thenReturn(BigDecimal.ZERO);
            when(row.getOrdersCount()).thenReturn(0L);
            when(row.getProductsCount()).thenReturn(0L);
            when(vendorRepository.vendorSummaries(any(), any(), eq(42L))).thenReturn(List.of(row));
            when(vendorRepository.topVendors(any(), any(), anyInt())).thenReturn(List.of());

            ReportFilter f = filter();
            f.setVendorId(999L);
            VendorReportDto dto = service.vendorSelfReport(f, null);

            assertThat(dto.getVendors().get(0).getVendorId()).isEqualTo(42L);
            verify(vendorRepository).vendorSummaries(any(), any(), eq(42L));
        }

        @Test
        @DisplayName("vendor with no profile is denied")
        void vendorNoProfile() {
            when(securityHelper.hasRole("VENDOR")).thenReturn(true);
            when(securityHelper.currentRole()).thenReturn("VENDOR");
            when(securityHelper.currentUserId()).thenReturn(2L);
            when(vendorRepository.findByUserId(2L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.vendorSelfReport(filter(), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Product report")
    class Product {
        @Test
        @DisplayName("maps best sellers, revenue, low/out of stock")
        void productReport() {
            when(securityHelper.currentRole()).thenReturn("ADMIN");
            ProductRevenueProjection rev = mock(ProductRevenueProjection.class);
            when(rev.getProductId()).thenReturn(1L);
            when(rev.getProductName()).thenReturn("Widget");
            when(rev.getRevenue()).thenReturn(new BigDecimal("100.00"));
            when(rev.getQuantitySold()).thenReturn(5L);
            when(orderItemRepository.productRevenue(any(), any(), any(), any())).thenReturn(List.of(rev));

            com.shopstack.entity.Category cat = com.shopstack.entity.Category.builder().id(1L).name("C").build();
            com.shopstack.entity.Vendor vendor = com.shopstack.entity.Vendor.builder().id(7L).name("V").build();
            com.shopstack.entity.Product low = com.shopstack.entity.Product.builder()
                    .id(10L).name("Low").price(new BigDecimal("5.00"))
                    .stockQuantity(2).lowStockThreshold(5).vendor(vendor).category(cat).build();
            com.shopstack.entity.Product out = com.shopstack.entity.Product.builder()
                    .id(11L).name("Out").price(new BigDecimal("9.00"))
                    .stockQuantity(0).lowStockThreshold(5).vendor(vendor).category(cat).build();
            com.shopstack.entity.Product ok = com.shopstack.entity.Product.builder()
                    .id(12L).name("Ok").price(new BigDecimal("3.00"))
                    .stockQuantity(50).lowStockThreshold(5).vendor(vendor).category(cat).build();
            when(productRepository.findForInventory(any(), any())).thenReturn(List.of(low, out, ok));

            ProductReportDto dto = service.productReport(filter(), null);

            assertThat(dto.getBestSellingProducts()).hasSize(1);
            assertThat(dto.getProductRevenue().get(0).getRevenue()).isEqualByComparingTo("100.00");
            assertThat(dto.getLowStockProducts()).hasSize(1);
            assertThat(dto.getOutOfStockProducts()).hasSize(1);
            assertThat(dto.getTotalProductRevenue()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("Customer report")
    class Customer {
        @Test
        @DisplayName("computes totals, new customers and top spenders")
        void customerReport() {
            when(userRepository.countByRole(com.shopstack.entity.UserRole.CUSTOMER)).thenReturn(4L);
            when(userRepository.countByRoleAndCreatedAtBetween(eq(com.shopstack.entity.UserRole.CUSTOMER), any(), any())).thenReturn(2L);
            when(userRepository.totalCustomerSpendingBetween(any(), any())).thenReturn(new BigDecimal("800.00"));
            CustomerSummaryProjection top = mock(CustomerSummaryProjection.class);
            when(top.getCustomerId()).thenReturn(3L);
            when(top.getCustomerName()).thenReturn("Alice");
            when(top.getEmail()).thenReturn("alice@x.com");
            when(top.getTotalSpent()).thenReturn(new BigDecimal("500.00"));
            when(top.getOrdersCount()).thenReturn(5L);
            when(userRepository.topCustomers(any(), any(), anyInt())).thenReturn(List.of(top));

            CustomerReportDto dto = service.customerReport(filter(), null);

            assertThat(dto.getTotalCustomers()).isEqualTo(4L);
            assertThat(dto.getNewCustomers()).isEqualTo(2L);
            assertThat(dto.getTotalCustomerSpending()).isEqualByComparingTo("800.00");
            assertThat(dto.getAverageCustomerSpending()).isEqualByComparingTo("200.00");
            assertThat(dto.getTopCustomers()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Payment report")
    class Payment {
        @Test
        @DisplayName("counts statuses and groups by method")
        void paymentReport() {
            when(paymentRepository.countByStatusAndPaymentDateBetween(eq(com.shopstack.entity.PaymentStatus.SUCCESSFUL), any(), any())).thenReturn(8L);
            when(paymentRepository.countByStatusAndPaymentDateBetween(eq(com.shopstack.entity.PaymentStatus.FAILED), any(), any())).thenReturn(2L);
            when(paymentRepository.countByStatusAndPaymentDateBetween(eq(com.shopstack.entity.PaymentStatus.REFUNDED), any(), any())).thenReturn(1L);
            when(paymentRepository.sumAmountByStatusBetween(eq(com.shopstack.entity.PaymentStatus.SUCCESSFUL), any(), any())).thenReturn(new BigDecimal("800.00"));
            when(paymentRepository.sumAmountByStatusBetween(eq(com.shopstack.entity.PaymentStatus.REFUNDED), any(), any())).thenReturn(new BigDecimal("50.00"));
            PaymentMethodStatProjection stat = mock(PaymentMethodStatProjection.class);
            when(stat.getPaymentMethod()).thenReturn("CARD");
            when(stat.getCount()).thenReturn(7L);
            when(stat.getAmount()).thenReturn(new BigDecimal("700.00"));
            when(paymentRepository.paymentMethodStats(any(), any(), any())).thenReturn(List.of(stat));

            PaymentReportDto dto = service.paymentReport(filter(), null);

            assertThat(dto.getSuccessfulPayments()).isEqualTo(8L);
            assertThat(dto.getFailedPayments()).isEqualTo(2L);
            assertThat(dto.getRefundedPayments()).isEqualTo(1L);
            assertThat(dto.getSuccessfulAmount()).isEqualByComparingTo("800.00");
            assertThat(dto.getRefundedAmount()).isEqualByComparingTo("50.00");
            assertThat(dto.getPaymentMethodStats()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Coupon report")
    class Coupon {
        @Test
        @DisplayName("maps coupon usage and totals")
        void couponReport() {
            CouponSummaryProjection c = mock(CouponSummaryProjection.class);
            when(c.getCouponId()).thenReturn(1L);
            when(c.getCode()).thenReturn("SAVE10");
            when(c.getUsageCount()).thenReturn(5L);
            when(c.getTotalDiscount()).thenReturn(new BigDecimal("50.00"));
            when(c.getDiscountPercentage()).thenReturn(new BigDecimal("10.00"));
            when(couponRepository.couponUsageBetween(any(), any())).thenReturn(List.of(c));
            when(couponRepository.totalDiscountAll()).thenReturn(new BigDecimal("50.00"));
            when(couponRepository.totalUsageAll()).thenReturn(5L);

            CouponReportDto dto = service.couponReport(filter(), null);

            assertThat(dto.getCoupons()).hasSize(1);
            assertThat(dto.getCoupons().get(0).getCode()).isEqualTo("SAVE10");
            assertThat(dto.getTotalDiscount()).isEqualByComparingTo("50.00");
            assertThat(dto.getTotalUsage()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Inventory report")
    class Inventory {
        @Test
        @DisplayName("classifies low/out of stock and values inventory")
        void inventoryReport() {
            when(securityHelper.currentRole()).thenReturn("ADMIN");
            com.shopstack.entity.Vendor vendor = com.shopstack.entity.Vendor.builder().id(7L).name("V").build();
            com.shopstack.entity.Category cat = com.shopstack.entity.Category.builder().id(1L).name("C").build();
            com.shopstack.entity.Product low = com.shopstack.entity.Product.builder()
                    .id(1L).name("Low").price(new BigDecimal("10.00"))
                    .stockQuantity(3).lowStockThreshold(5).vendor(vendor).category(cat).build();
            com.shopstack.entity.Product out = com.shopstack.entity.Product.builder()
                    .id(2L).name("Out").price(new BigDecimal("20.00"))
                    .stockQuantity(0).lowStockThreshold(5).vendor(vendor).category(cat).build();
            com.shopstack.entity.Product ok = com.shopstack.entity.Product.builder()
                    .id(3L).name("Ok").price(new BigDecimal("5.00"))
                    .stockQuantity(100).lowStockThreshold(5).vendor(vendor).category(cat).build();
            when(productRepository.findForInventory(any(), any())).thenReturn(List.of(low, out, ok));

            InventoryReportDto dto = service.inventoryReport(filter());

            assertThat(dto.getTotalProducts()).isEqualTo(3L);
            assertThat(dto.getLowStockCount()).isEqualTo(1L);
            assertThat(dto.getOutOfStockCount()).isEqualTo(1L);
            assertThat(dto.getStockValue()).isEqualByComparingTo("530.00");
        }
    }

    @Nested
    @DisplayName("Export tabulation")
    class Export {
        @Test
        @DisplayName("tabulate produces headers and rows for sales")
        void tabulateSales() {
            when(securityHelper.currentRole()).thenReturn("ADMIN");
            when(orderRepository.sumRevenueBetween(any(), any())).thenReturn(new BigDecimal("100.00"));
            when(orderRepository.countOrdersBetween(any(), any())).thenReturn(1L);
            when(orderRepository.countByStatusBetween(any(), any(), any())).thenReturn(0L);
            when(orderRepository.averageOrderValueBetween(any(), any())).thenReturn(new BigDecimal("100.00"));
            RevenueBreakdownProjection b = mock(RevenueBreakdownProjection.class);
            when(b.getPeriodLabel()).thenReturn("2024-01");
            when(b.getRevenue()).thenReturn(new BigDecimal("100.00"));
            when(b.getOrdersCount()).thenReturn(1L);
            when(orderRepository.revenueBreakdown(any(), any(), anyString(), any(), any())).thenReturn(List.of(b));
            when(orderRepository.statusCounts(any(), any())).thenReturn(List.of());

            ReportTabulator.Tabular tabular = service.tabulate(ReportType.SALES, filter(), null);

            assertThat(tabular.headers()).contains("period", "revenue", "ordersCount");
            assertThat(tabular.rows()).hasSize(1);
            assertThat(tabular.rows().get(0).get("period")).isEqualTo("2024-01");
        }
    }

    @Nested
    @DisplayName("Preset parsing")
    class Preset {
        @Test
        @DisplayName("null/blank preset resolves to null (defaults downstream)")
        void nullPreset() {
            assertThat(ReportService.presetFromString(null)).isNull();
            assertThat(ReportService.presetFromString("")).isNull();
        }

        @Test
        @DisplayName("valid preset string maps to enum")
        void validPreset() {
            assertThat(ReportService.presetFromString("monthly")).isEqualTo(ReportDateResolver.Preset.MONTHLY);
            assertThat(ReportService.presetFromString("WEEKLY")).isEqualTo(ReportDateResolver.Preset.WEEKLY);
        }

        @Test
        @DisplayName("invalid preset throws ReportException")
        void invalidPreset() {
            assertThatThrownBy(() -> ReportService.presetFromString("hourly"))
                    .isInstanceOf(ReportException.class);
        }
    }

    @Test
    @DisplayName("normalizeFilter copies all fields")
    void normalizeFilter() {
        ReportFilter f = service.normalizeFilter(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                7L, 2L, "PAID");
        assertThat(f.getFromDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(f.getToDate()).isEqualTo(LocalDate.of(2024, 1, 31));
        assertThat(f.getVendorId()).isEqualTo(7L);
        assertThat(f.getCategoryId()).isEqualTo(2L);
        assertThat(f.getStatus()).isEqualTo("PAID");
    }
}
