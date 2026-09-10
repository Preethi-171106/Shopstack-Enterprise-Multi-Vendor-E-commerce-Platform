package com.shopstack.service;

import com.shopstack.dto.commission.CommissionResponse;
import com.shopstack.dto.commission.CommissionSummaryResponse;
import com.shopstack.dto.commission.VendorRevenueResponse;
import com.shopstack.entity.Commission;
import com.shopstack.entity.CommissionStatus;
import com.shopstack.entity.OrderItem;
import com.shopstack.entity.VendorProfile;
import com.shopstack.repository.CommissionRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CommissionService — handles commission calculation, storage, and vendor revenue queries.
 *
 * <p>Default platform commission rate: 5% of each order item's sale amount.
 */
@Service
public class CommissionService {

    private static final Logger log = LoggerFactory.getLogger(CommissionService.class);

    /** Default platform commission rate (5%). */
    public static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.05");

    private final CommissionRepository commissionRepository;
    private final VendorProfileRepository vendorProfileRepository;

    public CommissionService(CommissionRepository commissionRepository,
                             VendorProfileRepository vendorProfileRepository) {
        this.commissionRepository = commissionRepository;
        this.vendorProfileRepository = vendorProfileRepository;
    }

    /**
     * Creates a Commission record for a single OrderItem.
     * Called by OrderService when an order is confirmed.
     */
    @Transactional
    public Commission createCommission(OrderItem item) {
        BigDecimal saleAmount = item.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal commissionAmount = saleAmount
                .multiply(DEFAULT_COMMISSION_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal vendorNet = saleAmount.subtract(commissionAmount);

        Commission commission = Commission.builder()
                .vendorProfile(item.getVendorProfile())
                .orderItem(item)
                .saleAmount(saleAmount)
                .commissionRate(DEFAULT_COMMISSION_RATE)
                .commissionAmount(commissionAmount)
                .vendorNetAmount(vendorNet)
                .status(CommissionStatus.PENDING)
                .build();

        Commission saved = commissionRepository.save(commission);
        log.debug("[CommissionService] Created commission id={} for orderItem id={} amount={}",
                saved.getId(), item.getId(), commissionAmount);
        return saved;
    }

    /** Returns all commissions (admin view). */
    @Transactional(readOnly = true)
    public List<CommissionResponse> getAllCommissions() {
        return commissionRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Returns comprehensive platform commission summary with per-vendor breakdown (admin view). */
    @Transactional(readOnly = true)
    public CommissionSummaryResponse getCommissionSummary() {
        List<Commission> all = commissionRepository.findAll();

        BigDecimal totalPlatformCommission = all.stream()
                .map(c -> c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGrossSales = all.stream()
                .map(c -> c.getSaleAmount() != null ? c.getSaleAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVendorPayouts = all.stream()
                .map(c -> c.getVendorNetAmount() != null ? c.getVendorNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<VendorProfile> vendors = vendorProfileRepository.findAll();
        List<CommissionSummaryResponse.VendorCommissionItem> breakdown = vendors.stream()
                .map(v -> {
                    List<Commission> vendorComms = commissionRepository.findByVendorProfileId(v.getId());
                    BigDecimal sales = vendorComms.stream()
                            .map(c -> c.getSaleAmount() != null ? c.getSaleAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal comms = vendorComms.stream()
                            .map(c -> c.getCommissionAmount() != null ? c.getCommissionAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal net = vendorComms.stream()
                            .map(c -> c.getVendorNetAmount() != null ? c.getVendorNetAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new CommissionSummaryResponse.VendorCommissionItem(
                            v.getId(),
                            v.getStoreName(),
                            sales,
                            comms,
                            net,
                            vendorComms.size()
                    );
                })
                .collect(Collectors.toList());

        return CommissionSummaryResponse.builder()
                .totalPlatformCommission(totalPlatformCommission)
                .totalGrossSales(totalGrossSales)
                .totalVendorPayouts(totalVendorPayouts)
                .defaultCommissionRatePercent(new BigDecimal("5.00"))
                .totalCommissionRecords(all.size())
                .vendorBreakdown(breakdown)
                .build();
    }

    /** Returns commissions for the currently authenticated vendor. */
    @Transactional(readOnly = true)
    public List<CommissionResponse> getMyCommissions() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var vendorProfile = vendorProfileRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new com.shopstack.exception.VendorProfileNotFoundException(
                        "No vendor profile found for: " + email));
        return commissionRepository.findByVendorProfileId(vendorProfile.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Returns revenue summary for the currently authenticated vendor. */
    @Transactional(readOnly = true)
    public VendorRevenueResponse getMyRevenue() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var vendorProfile = vendorProfileRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new com.shopstack.exception.VendorProfileNotFoundException(
                        "No vendor profile found for: " + email));

        Long id = vendorProfile.getId();
        BigDecimal totalSales      = commissionRepository.sumSalesByVendor(id);
        BigDecimal totalCommission = commissionRepository.findByVendorProfileId(id)
                .stream()
                .map(Commission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet        = commissionRepository.sumVendorNetByVendor(id);
        long orderCount            = commissionRepository.findByVendorProfileId(id).size();

        return VendorRevenueResponse.builder()
                .vendorProfileId(id)
                .vendorStoreName(vendorProfile.getStoreName())
                .totalSales(totalSales != null ? totalSales : BigDecimal.ZERO)
                .totalCommission(totalCommission)
                .totalNetRevenue(totalNet != null ? totalNet : BigDecimal.ZERO)
                .orderCount(orderCount)
                .build();
    }

    /** Admin: total platform commission earned. */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPlatformCommission() {
        return commissionRepository.sumTotalCommissions();
    }

    private CommissionResponse toResponse(Commission c) {
        return CommissionResponse.builder()
                .id(c.getId())
                .vendorProfileId(c.getVendorProfile() != null ? c.getVendorProfile().getId() : null)
                .vendorStoreName(c.getVendorProfile() != null ? c.getVendorProfile().getStoreName() : null)
                .orderItemId(c.getOrderItem() != null ? c.getOrderItem().getId() : null)
                .productName(c.getOrderItem() != null ? c.getOrderItem().getProductNameSnapshot() : null)
                .saleAmount(c.getSaleAmount())
                .commissionRate(c.getCommissionRate())
                .commissionAmount(c.getCommissionAmount())
                .vendorNetAmount(c.getVendorNetAmount())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
