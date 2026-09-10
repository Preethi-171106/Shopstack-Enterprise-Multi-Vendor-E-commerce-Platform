package com.shopstack.service;

import com.shopstack.dto.report.*;
import com.shopstack.entity.*;
import com.shopstack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminReportService — aggregates live PostgreSQL data to generate business reports
 * and format them into downloadable CSV exports.
 */
@Service
public class AdminReportService {

    private final OrderRepository orderRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final ProductRepository productRepository;
    private final CommissionRepository commissionRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;

    public AdminReportService(
            OrderRepository orderRepository,
            VendorProfileRepository vendorProfileRepository,
            ProductRepository productRepository,
            CommissionRepository commissionRepository,
            InventoryRepository inventoryRepository,
            PaymentRepository paymentRepository
    ) {
        this.orderRepository = orderRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.productRepository = productRepository;
        this.commissionRepository = commissionRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse generateSalesReport() {
        List<Order> validOrders = orderRepository.findAll().stream()
                .filter(o -> o.getOrderStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        BigDecimal totalGross = validOrders.stream()
                .map(o -> o.getSubtotalAmount() != null ? o.getSubtotalAmount() : (o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = validOrders.stream()
                .map(o -> o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = validOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = validOrders.size();
        BigDecimal avgOrderValue = count > 0
                ? totalNet.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SalesReportResponse.SalesRecord> records = validOrders.stream()
                .map(o -> {
                    Optional<Payment> p = paymentRepository.findByOrderId(o.getId());
                    String pMethod = p.map(pay -> pay.getPaymentMethod() != null ? pay.getPaymentMethod().name() : "N/A").orElse("N/A");
                    return SalesReportResponse.SalesRecord.builder()
                            .orderNumber(o.getOrderNumber())
                            .orderDate(o.getCreatedAt())
                            .customerEmail(o.getUser() != null ? o.getUser().getEmail() : "N/A")
                            .subtotal(o.getSubtotalAmount() != null ? o.getSubtotalAmount() : o.getTotalAmount())
                            .discount(o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO)
                            .totalAmount(o.getTotalAmount())
                            .paymentMethod(pMethod)
                            .orderStatus(o.getOrderStatus() != null ? o.getOrderStatus().name() : "N/A")
                            .build();
                })
                .collect(Collectors.toList());

        return SalesReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalGrossRevenue(totalGross)
                .totalDiscountAmount(totalDiscount)
                .totalNetRevenue(totalNet)
                .totalOrdersCount(count)
                .averageOrderValue(avgOrderValue)
                .records(records)
                .build();
    }

    @Transactional(readOnly = true)
    public OrderReportResponse generateOrderReport() {
        List<Order> allOrders = orderRepository.findAll();

        BigDecimal totalValue = allOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus st : OrderStatus.values()) {
            statusCounts.put(st.name(), allOrders.stream().filter(o -> o.getOrderStatus() == st).count());
        }

        List<OrderReportResponse.OrderRecord> records = allOrders.stream()
                .map(o -> {
                    Optional<Payment> p = paymentRepository.findByOrderId(o.getId());
                    String pStatus = p.map(pay -> pay.getStatus() != null ? pay.getStatus().name() : "PENDING").orElse("PENDING");
                    String cName = o.getUser() != null ? (o.getUser().getFirstName() + " " + o.getUser().getLastName()).trim() : "N/A";
                    String cEmail = o.getUser() != null ? o.getUser().getEmail() : "N/A";
                    int itemsCount = o.getItems() != null ? o.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0;

                    return OrderReportResponse.OrderRecord.builder()
                            .orderId(o.getId())
                            .orderNumber(o.getOrderNumber())
                            .createdAt(o.getCreatedAt())
                            .customerName(cName)
                            .customerEmail(cEmail)
                            .totalItems(itemsCount)
                            .totalAmount(o.getTotalAmount())
                            .orderStatus(o.getOrderStatus() != null ? o.getOrderStatus().name() : "N/A")
                            .paymentStatus(pStatus)
                            .build();
                })
                .collect(Collectors.toList());

        return OrderReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalOrders(allOrders.size())
                .totalOrderValue(totalValue)
                .statusCounts(statusCounts)
                .records(records)
                .build();
    }

    @Transactional(readOnly = true)
    public VendorReportResponse generateVendorReport() {
        List<VendorProfile> vendors = vendorProfileRepository.findAll();

        BigDecimal totalCommissions = commissionRepository.sumTotalCommissions();
        if (totalCommissions == null) totalCommissions = BigDecimal.ZERO;

        List<VendorReportResponse.VendorRecord> records = vendors.stream()
                .map(v -> {
                    List<Product> products = productRepository.findByVendorProfileId(v.getId());
                    List<Commission> comms = commissionRepository.findByVendorProfileId(v.getId());

                    BigDecimal sales = comms.stream()
                            .map(c -> c.getSaleAmount() != null ? c.getSaleAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal commPaid = comms.stream()
                            .map(c -> c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal net = comms.stream()
                            .map(c -> c.getVendorNetAmount() != null ? c.getVendorNetAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long soldCount = comms.size();

                    return VendorReportResponse.VendorRecord.builder()
                            .vendorId(v.getId())
                            .storeName(v.getStoreName())
                            .ownerEmail(v.getUser() != null ? v.getUser().getEmail() : "N/A")
                            .status(v.getStatus() != null ? v.getStatus().name() : "N/A")
                            .totalProducts(products.size())
                            .itemsSold(soldCount)
                            .grossSales(sales)
                            .commissionPaid(commPaid)
                            .netEarnings(net)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalSalesSum = records.stream()
                .map(VendorReportResponse.VendorRecord::getGrossSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeCount = vendors.stream()
                .filter(v -> v.getStatus() == VendorStatus.APPROVED)
                .count();

        return VendorReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalVendorsCount(vendors.size())
                .activeVendorsCount(activeCount)
                .totalMarketplaceGrossSales(totalSalesSum)
                .totalPlatformCommissionGenerated(totalCommissions)
                .records(records)
                .build();
    }

    @Transactional(readOnly = true)
    public CommissionReportResponse generateCommissionReport() {
        List<Commission> commissions = commissionRepository.findAll();

        BigDecimal totalSales = commissions.stream()
                .map(c -> c.getSaleAmount() != null ? c.getSaleAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalComms = commissions.stream()
                .map(c -> c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNet = commissions.stream()
                .map(c -> c.getVendorNetAmount() != null ? c.getVendorNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CommissionReportResponse.CommissionRecord> records = commissions.stream()
                .map(c -> CommissionReportResponse.CommissionRecord.builder()
                        .commissionId(c.getId())
                        .orderItemId(c.getOrderItem() != null ? c.getOrderItem().getId() : null)
                        .vendorStoreName(c.getVendorProfile() != null ? c.getVendorProfile().getStoreName() : "N/A")
                        .productName(c.getOrderItem() != null ? c.getOrderItem().getProductNameSnapshot() : "N/A")
                        .saleAmount(c.getSaleAmount())
                        .commissionRate(c.getCommissionRate())
                        .commissionAmount(c.getCommissionAmount())
                        .vendorNetAmount(c.getVendorNetAmount())
                        .status(c.getStatus() != null ? c.getStatus().name() : "N/A")
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return CommissionReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalSalesProcessed(totalSales)
                .totalCommissionCollected(totalComms)
                .totalVendorPayouts(totalNet)
                .totalTransactions(commissions.size())
                .records(records)
                .build();
    }

    @Transactional(readOnly = true)
    public ProductReportResponse generateProductReport() {
        List<Product> products = productRepository.findAll();

        long activeCount = products.stream().filter(Product::isActive).count();
        long lowStockCount = inventoryRepository.findAll().stream().filter(Inventory::isLowStock).count();
        long outOfStockCount = inventoryRepository.findAll().stream().filter(i -> i.getAvailableStock() <= 0).count();

        List<ProductReportResponse.ProductRecord> records = products.stream()
                .map(p -> {
                    Optional<Inventory> inv = inventoryRepository.findByProductId(p.getId());
                    int stock = inv.map(Inventory::getAvailableStock).orElse(p.getStockQuantity());
                    boolean isLow = inv.map(Inventory::isLowStock).orElse(stock <= 10);

                    return ProductReportResponse.ProductRecord.builder()
                            .productId(p.getId())
                            .sku(p.getSku())
                            .name(p.getName())
                            .categoryName(p.getCategory() != null ? p.getCategory().getName() : "N/A")
                            .vendorStoreName(p.getVendorProfile() != null ? p.getVendorProfile().getStoreName() : "N/A")
                            .price(p.getPrice())
                            .stockQuantity(stock)
                            .active(p.isActive())
                            .lowStock(isLow)
                            .build();
                })
                .collect(Collectors.toList());

        return ProductReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .totalProducts(products.size())
                .activeProducts(activeCount)
                .lowStockProducts(lowStockCount)
                .outOfStockProducts(outOfStockCount)
                .records(records)
                .build();
    }

    /**
     * Generates downloadable CSV content for the specified report type.
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(String reportType) {
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        switch (reportType.toLowerCase().trim()) {
            case "sales" -> {
                SalesReportResponse sales = generateSalesReport();
                csv.append("Order Number,Order Date,Customer Email,Subtotal (INR),Discount (INR),Total Amount (INR),Payment Method,Order Status\n");
                for (SalesReportResponse.SalesRecord r : sales.getRecords()) {
                    csv.append(String.format("\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,\"%s\",\"%s\"\n",
                            escape(r.getOrderNumber()),
                            r.getOrderDate() != null ? r.getOrderDate().format(dtf) : "N/A",
                            escape(r.getCustomerEmail()),
                            r.getSubtotal() != null ? r.getSubtotal() : BigDecimal.ZERO,
                            r.getDiscount() != null ? r.getDiscount() : BigDecimal.ZERO,
                            r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO,
                            escape(r.getPaymentMethod()),
                            escape(r.getOrderStatus())
                    ));
                }
            }
            case "orders" -> {
                OrderReportResponse orders = generateOrderReport();
                csv.append("Order ID,Order Number,Order Date,Customer Name,Customer Email,Items Count,Total Amount (INR),Order Status,Payment Status\n");
                for (OrderReportResponse.OrderRecord r : orders.getRecords()) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,\"%s\",\"%s\"\n",
                            r.getOrderId(),
                            escape(r.getOrderNumber()),
                            r.getCreatedAt() != null ? r.getCreatedAt().format(dtf) : "N/A",
                            escape(r.getCustomerName()),
                            escape(r.getCustomerEmail()),
                            r.getTotalItems(),
                            r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO,
                            escape(r.getOrderStatus()),
                            escape(r.getPaymentStatus())
                    ));
                }
            }
            case "vendors" -> {
                VendorReportResponse vendors = generateVendorReport();
                csv.append("Vendor ID,Store Name,Owner Email,Status,Total Products,Items Sold,Gross Sales (INR),Commission Paid (INR),Net Earnings (INR)\n");
                for (VendorReportResponse.VendorRecord r : vendors.getRecords()) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%d,%d,%.2f,%.2f,%.2f\n",
                            r.getVendorId(),
                            escape(r.getStoreName()),
                            escape(r.getOwnerEmail()),
                            escape(r.getStatus()),
                            r.getTotalProducts(),
                            r.getItemsSold(),
                            r.getGrossSales() != null ? r.getGrossSales() : BigDecimal.ZERO,
                            r.getCommissionPaid() != null ? r.getCommissionPaid() : BigDecimal.ZERO,
                            r.getNetEarnings() != null ? r.getNetEarnings() : BigDecimal.ZERO
                    ));
                }
            }
            case "commissions" -> {
                CommissionReportResponse comms = generateCommissionReport();
                csv.append("Commission ID,Order Item ID,Vendor Store,Product Name,Sale Amount (INR),Commission Rate,Commission Amount (INR),Vendor Net (INR),Status,Created At\n");
                for (CommissionReportResponse.CommissionRecord r : comms.getRecords()) {
                    csv.append(String.format("%d,%s,\"%s\",\"%s\",%.2f,%.4f,%.2f,%.2f,\"%s\",\"%s\"\n",
                            r.getCommissionId(),
                            r.getOrderItemId() != null ? r.getOrderItemId().toString() : "N/A",
                            escape(r.getVendorStoreName()),
                            escape(r.getProductName()),
                            r.getSaleAmount() != null ? r.getSaleAmount() : BigDecimal.ZERO,
                            r.getCommissionRate() != null ? r.getCommissionRate() : BigDecimal.ZERO,
                            r.getCommissionAmount() != null ? r.getCommissionAmount() : BigDecimal.ZERO,
                            r.getVendorNetAmount() != null ? r.getVendorNetAmount() : BigDecimal.ZERO,
                            escape(r.getStatus()),
                            r.getCreatedAt() != null ? r.getCreatedAt().format(dtf) : "N/A"
                    ));
                }
            }
            case "products" -> {
                ProductReportResponse prods = generateProductReport();
                csv.append("Product ID,SKU,Product Name,Category,Vendor Store,Price (INR),Stock Quantity,Active,Low Stock\n");
                for (ProductReportResponse.ProductRecord r : prods.getRecords()) {
                    csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%d,%b,%b\n",
                            r.getProductId(),
                            escape(r.getSku()),
                            escape(r.getName()),
                            escape(r.getCategoryName()),
                            escape(r.getVendorStoreName()),
                            r.getPrice() != null ? r.getPrice() : BigDecimal.ZERO,
                            r.getStockQuantity(),
                            r.isActive(),
                            r.isLowStock()
                    ));
                }
            }
            default -> throw new IllegalArgumentException("Unknown report type: " + reportType + ". Supported: sales, orders, vendors, commissions, products");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
