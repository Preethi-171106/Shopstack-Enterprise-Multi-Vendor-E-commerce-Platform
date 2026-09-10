package com.shopstack.service;

import com.shopstack.dto.coupon.ApplicableCouponResponse;
import com.shopstack.dto.coupon.CouponAnalyticsResponse;
import com.shopstack.dto.coupon.CouponApplyRequest;
import com.shopstack.dto.coupon.CouponCreateRequest;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.dto.coupon.CouponUpdateRequest;
import com.shopstack.dto.coupon.CouponUsageResponse;
import com.shopstack.dto.coupon.CouponValidateRequest;
import com.shopstack.dto.coupon.CouponValidateResponse;
import com.shopstack.entity.CartItem;
import com.shopstack.entity.Category;
import com.shopstack.entity.Coupon;
import com.shopstack.entity.CouponApplicabilityScope;
import com.shopstack.entity.CouponDiscountType;
import com.shopstack.entity.CouponUsage;
import com.shopstack.entity.Order;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.VendorProfile;
import com.shopstack.exception.CouponAlreadyExistsException;
import com.shopstack.exception.CouponExpiredException;
import com.shopstack.exception.CouponInactiveException;
import com.shopstack.exception.CouponMinimumAmountException;
import com.shopstack.exception.CouponNotFoundException;
import com.shopstack.exception.CouponUsageExceededException;
import com.shopstack.exception.InvalidCouponException;
import com.shopstack.exception.OrderNotFoundException;
import com.shopstack.mapper.CouponMapper;
import com.shopstack.mapper.CouponUsageMapper;
import com.shopstack.repository.CartItemRepository;
import com.shopstack.repository.CategoryRepository;
import com.shopstack.repository.CouponRepository;
import com.shopstack.repository.CouponUsageRepository;
import com.shopstack.repository.OrderRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final CouponMapper couponMapper;
    private final CouponUsageMapper couponUsageMapper;

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new CouponAlreadyExistsException("Coupon with code " + request.getCode() + " already exists.");
        }

        validateCouponConfiguration(
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMaximumDiscount(),
                request.getStartDate(),
                request.getExpiryDate(),
                request.getApplicabilityScope(),
                request.getCategoryIds(),
                request.getProductIds(),
                request.getVendorIds()
        );

        CouponApplicabilityScope scope = request.getApplicabilityScope() != null
                ? request.getApplicabilityScope()
                : CouponApplicabilityScope.ENTIRE_PLATFORM;

        Set<Category> categories = resolveCategories(scope, request.getCategoryIds());
        Set<Product> products = resolveProducts(scope, request.getProductIds());
        Set<VendorProfile> vendors = resolveVendors(scope, request.getVendorIds());

        Coupon coupon = Coupon.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(request.getMinimumOrderAmount())
                .maximumDiscount(request.getMaximumDiscount())
                .usageLimit(request.getUsageLimit())
                .perUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1)
                .applicabilityScope(scope)
                .eligibleCategories(categories)
                .eligibleProducts(products)
                .eligibleVendors(vendors)
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .active(true)
                .usedCount(0)
                .build();

        coupon = couponRepository.save(coupon);
        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponUpdateRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id " + id));

        validateCouponConfiguration(
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMaximumDiscount(),
                request.getStartDate(),
                request.getExpiryDate(),
                request.getApplicabilityScope(),
                request.getCategoryIds(),
                request.getProductIds(),
                request.getVendorIds()
        );

        CouponApplicabilityScope scope = request.getApplicabilityScope() != null
                ? request.getApplicabilityScope()
                : CouponApplicabilityScope.ENTIRE_PLATFORM;

        Set<Category> categories = resolveCategories(scope, request.getCategoryIds());
        Set<Product> products = resolveProducts(scope, request.getProductIds());
        Set<VendorProfile> vendors = resolveVendors(scope, request.getVendorIds());

        coupon.setName(request.getName());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setMaximumDiscount(request.getMaximumDiscount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setPerUserLimit(request.getPerUserLimit() != null ? request.getPerUserLimit() : 1);
        coupon.setApplicabilityScope(scope);
        coupon.setEligibleCategories(categories);
        coupon.setEligibleProducts(products);
        coupon.setEligibleVendors(vendors);
        coupon.setStartDate(request.getStartDate());
        coupon.setExpiryDate(request.getExpiryDate());

        coupon = couponRepository.save(coupon);
        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new CouponNotFoundException("Coupon not found with id " + id);
        }
        couponRepository.deleteById(id);
    }

    @Override
    @Transactional
    public CouponResponse enableCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id " + id));
        coupon.setActive(true);
        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse disableCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id " + id));
        coupon.setActive(false);
        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id " + id));
        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with code " + code));
        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons() {
        return couponMapper.toResponseList(couponRepository.findAllByOrderByCreatedAtDesc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons(String search, String status) {
        List<Coupon> coupons;
        if (search != null && !search.isBlank()) {
            coupons = couponRepository.searchCoupons(search.trim());
        } else {
            coupons = couponRepository.findAllByOrderByCreatedAtDesc();
        }

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            LocalDateTime now = LocalDateTime.now();
            coupons = coupons.stream().filter(c -> {
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    return Boolean.TRUE.equals(c.getActive()) && (c.getExpiryDate() == null || now.isBefore(c.getExpiryDate()));
                } else if ("INACTIVE".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)) {
                    return Boolean.FALSE.equals(c.getActive());
                } else if ("EXPIRED".equalsIgnoreCase(status)) {
                    return c.getExpiryDate() != null && now.isAfter(c.getExpiryDate());
                }
                return true;
            }).collect(Collectors.toList());
        }

        return couponMapper.toResponseList(coupons);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listAvailableCoupons() {
        String email = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            email = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> activeCoupons = couponRepository.findActiveCoupons(now);

        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return activeCoupons.stream()
                    .filter(c -> c.getUsageLimit() == null || c.getUsedCount() == null || c.getUsedCount() < c.getUsageLimit())
                    .map(couponMapper::toResponse)
                    .collect(Collectors.toList());
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return activeCoupons.stream()
                    .filter(c -> c.getUsageLimit() == null || c.getUsedCount() == null || c.getUsedCount() < c.getUsageLimit())
                    .map(couponMapper::toResponse)
                    .collect(Collectors.toList());
        }

        User user = userOpt.get();
        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal cartSubtotal = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Coupon> applicable = new ArrayList<>();
        for (Coupon coupon : activeCoupons) {
            if (Boolean.FALSE.equals(coupon.getActive())) {
                continue;
            }
            if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
                continue;
            }
            if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
                continue;
            }
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                continue;
            }
            if (coupon.getPerUserLimit() != null) {
                int userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
                if (userUsage >= coupon.getPerUserLimit()) {
                    continue;
                }
            }
            if (coupon.getMinimumOrderAmount() != null && cartSubtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
                continue;
            }

            BigDecimal eligibleSubtotal = calculateEligibleSubtotal(coupon, cartItems);
            if (eligibleSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                applicable.add(coupon);
            }
        }

        return couponMapper.toResponseList(applicable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicableCouponResponse> getApplicableCoupons() {
        String email = null;
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            email = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return getApplicableCoupons(email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicableCouponResponse> getApplicableCoupons(String userEmail) {
        String effectiveEmail = userEmail;
        if ((effectiveEmail == null || effectiveEmail.isBlank() || "anonymousUser".equals(effectiveEmail))
                && SecurityContextHolder.getContext().getAuthentication() != null) {
            effectiveEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        if (effectiveEmail == null || effectiveEmail.isBlank() || "anonymousUser".equals(effectiveEmail)) {
            return Collections.emptyList();
        }

        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(effectiveEmail);
        if (userOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User user = userOpt.get();

        List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        BigDecimal cartSubtotal = cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime now = LocalDateTime.now();
        List<Coupon> activeCoupons = couponRepository.findActiveCoupons(now);
        List<ApplicableCouponResponse> applicableList = new ArrayList<>();

        for (Coupon coupon : activeCoupons) {
            if (Boolean.FALSE.equals(coupon.getActive())) {
                continue;
            }
            if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
                continue;
            }
            if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
                continue;
            }
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                continue;
            }
            if (coupon.getPerUserLimit() != null) {
                int userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
                if (userUsage >= coupon.getPerUserLimit()) {
                    continue;
                }
            }
            if (coupon.getMinimumOrderAmount() != null && cartSubtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
                continue;
            }

            BigDecimal eligibleSubtotal = calculateEligibleSubtotal(coupon, cartItems);
            if (eligibleSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal estimatedDiscount = calculateDiscountValue(coupon, eligibleSubtotal);
                String message = buildOfferMessage(coupon);
                applicableList.add(couponMapper.toApplicableResponse(coupon, eligibleSubtotal, estimatedDiscount, message));
            }
        }

        return applicableList;
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse applyCoupon(CouponApplyRequest request) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCode())
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with code " + request.getCode()));

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.FALSE.equals(coupon.getActive())) {
            throw new CouponInactiveException("Coupon is disabled.");
        }

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new CouponInactiveException("Coupon is not yet active.");
        }

        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            throw new CouponExpiredException("Coupon has expired.");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponUsageExceededException("Coupon usage limit has been reached.");
        }

        if (coupon.getMinimumOrderAmount() != null && request.getOrderTotal().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new CouponMinimumAmountException("Order total is less than the minimum required amount for this coupon.");
        }

        if (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCouponException("Coupon discount value is invalid.");
        }

        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE
                && coupon.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidCouponException("Coupon discount percentage cannot exceed 100%.");
        }

        String requestedUserEmail = request.getUserEmail();
        String effectiveUserEmail = requestedUserEmail;
        if ((effectiveUserEmail == null || effectiveUserEmail.isBlank())
                && SecurityContextHolder.getContext().getAuthentication() != null) {
            effectiveUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        }
        final String resolvedUserEmail = effectiveUserEmail;

        if (coupon.getPerUserLimit() != null && resolvedUserEmail != null && !resolvedUserEmail.isBlank() && !"anonymousUser".equals(resolvedUserEmail)) {
            userRepository.findByEmailIgnoreCase(resolvedUserEmail).ifPresent(user -> {
                int userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
                if (userUsage >= coupon.getPerUserLimit()) {
                    throw new CouponUsageExceededException("You have reached the maximum usage limit for this coupon.");
                }

                // Check cart eligibility if user has items in cart
                List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
                if (!cartItems.isEmpty()) {
                    BigDecimal eligibleSubtotal = calculateEligibleSubtotal(coupon, cartItems);
                    if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidCouponException("This coupon is not applicable to the products in your cart.");
                    }
                }
            });
        }

        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponValidateResponse validateCoupon(CouponValidateRequest request) {
        if (request.getCode() == null || request.getCode().trim().isBlank()) {
            throw new InvalidCouponException("Coupon code is required.");
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(request.getCode().trim())
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with code " + request.getCode()));

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.FALSE.equals(coupon.getActive())) {
            throw new CouponInactiveException("Coupon is disabled.");
        }

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new CouponInactiveException("Coupon is not yet active.");
        }

        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            throw new CouponExpiredException("Coupon has expired.");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponUsageExceededException("Coupon usage limit has been reached.");
        }

        if (coupon.getMinimumOrderAmount() != null && request.getOrderTotal().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new CouponMinimumAmountException("Order total is less than the minimum required amount of ₹" + coupon.getMinimumOrderAmount() + " for this coupon.");
        }

        if (coupon.getDiscountValue() == null || coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCouponException("Coupon discount value is invalid.");
        }

        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE
                && coupon.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidCouponException("Coupon discount percentage cannot exceed 100%.");
        }

        String effectiveUserEmail = request.getUserEmail();
        if ((effectiveUserEmail == null || effectiveUserEmail.isBlank())
                && SecurityContextHolder.getContext().getAuthentication() != null) {
            effectiveUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        BigDecimal eligibleSubtotal = request.getOrderTotal();

        if (effectiveUserEmail != null && !effectiveUserEmail.isBlank() && !"anonymousUser".equals(effectiveUserEmail)) {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(effectiveUserEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (coupon.getPerUserLimit() != null) {
                    int userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
                    if (userUsage >= coupon.getPerUserLimit()) {
                        throw new CouponUsageExceededException("You have reached the maximum usage limit (" + coupon.getPerUserLimit() + ") for this coupon.");
                    }
                }

                // If user has cart items, validate product/category/vendor targeting & calculate eligible portion
                List<CartItem> cartItems = cartItemRepository.findByUserId(user.getId());
                if (!cartItems.isEmpty()) {
                    eligibleSubtotal = calculateEligibleSubtotal(coupon, cartItems);
                    if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new InvalidCouponException("This coupon is not applicable to the products in your cart.");
                    }
                }
            }
        }

        BigDecimal discountAmount = calculateDiscountValue(coupon, eligibleSubtotal);
        BigDecimal finalTotal = request.getOrderTotal().subtract(discountAmount).max(BigDecimal.ZERO);

        return CouponValidateResponse.builder()
                .valid(true)
                .code(coupon.getCode())
                .name(coupon.getName())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .applicabilityScope(coupon.getApplicabilityScope() != null ? coupon.getApplicabilityScope() : CouponApplicabilityScope.ENTIRE_PLATFORM)
                .eligibleSubtotal(eligibleSubtotal)
                .discountAmount(discountAmount)
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maximumDiscount(coupon.getMaximumDiscount())
                .finalTotal(finalTotal)
                .message("Coupon applied successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCoupon(Coupon coupon, String userEmail) {
        LocalDateTime now = LocalDateTime.now();

        if (Boolean.FALSE.equals(coupon.getActive())) {
            throw new CouponInactiveException("Coupon is disabled.");
        }

        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            throw new CouponInactiveException("Coupon is not yet active.");
        }

        if (coupon.getExpiryDate() != null && now.isAfter(coupon.getExpiryDate())) {
            throw new CouponExpiredException("Coupon has expired.");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new CouponUsageExceededException("Coupon usage limit has been reached.");
        }

        if (coupon.getPerUserLimit() != null && userEmail != null && !userEmail.isBlank() && !"anonymousUser".equals(userEmail)) {
            User user = userRepository.findByEmailIgnoreCase(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));
            int userUsage = couponUsageRepository.countByCouponAndUser(coupon, user);
            if (userUsage >= coupon.getPerUserLimit()) {
                throw new CouponUsageExceededException("You have reached the maximum usage limit for this coupon.");
            }
        }
    }

    @Override
    @Transactional
    public void recordCouponUsage(Coupon coupon, Long orderId, String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id " + orderId));

        BigDecimal discount = order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                ? order.getDiscountAmount()
                : calculateDiscountValue(coupon, order.getSubtotalAmount() != null ? order.getSubtotalAmount() : order.getTotalAmount());

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .order(order)
                .discountAmount(discount)
                .build();

        couponUsageRepository.save(usage);

        coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponUsageResponse> getCouponUsages(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new CouponNotFoundException("Coupon not found with id " + couponId);
        }
        List<CouponUsage> usages = couponUsageRepository.findByCouponIdOrderByUsedAtDesc(couponId);
        return couponUsageMapper.toResponseList(usages);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponAnalyticsResponse getCouponAnalytics() {
        List<Coupon> allCoupons = couponRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long totalCoupons = allCoupons.size();
        long activeCoupons = allCoupons.stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()) && (c.getExpiryDate() == null || now.isBefore(c.getExpiryDate())))
                .count();
        long expiredCoupons = allCoupons.stream()
                .filter(c -> c.getExpiryDate() != null && now.isAfter(c.getExpiryDate()))
                .count();
        long disabledCoupons = allCoupons.stream()
                .filter(c -> Boolean.FALSE.equals(c.getActive()))
                .count();

        long totalUsageCount = couponUsageRepository.countTotalUsages();
        BigDecimal totalDiscountGiven = couponUsageRepository.sumTotalDiscountAmount();

        Coupon topCoupon = allCoupons.stream()
                .filter(c -> c.getUsedCount() != null && c.getUsedCount() > 0)
                .max(Comparator.comparingInt(Coupon::getUsedCount))
                .orElse(null);

        List<CouponAnalyticsResponse.CouponPerformanceDto> breakdown = allCoupons.stream().map(c -> {
            BigDecimal couponDiscountTotal = couponUsageRepository.sumDiscountAmountByCouponId(c.getId());
            String status = Boolean.FALSE.equals(c.getActive()) ? "DISABLED" :
                    (c.getExpiryDate() != null && now.isAfter(c.getExpiryDate()) ? "EXPIRED" : "ACTIVE");

            return CouponAnalyticsResponse.CouponPerformanceDto.builder()
                    .couponId(c.getId())
                    .code(c.getCode())
                    .name(c.getName())
                    .discountType(c.getDiscountType() != null ? c.getDiscountType().name() : "PERCENTAGE")
                    .discountValue(c.getDiscountValue())
                    .usedCount(c.getUsedCount() != null ? c.getUsedCount() : 0)
                    .usageLimit(c.getUsageLimit())
                    .totalDiscountAmount(couponDiscountTotal)
                    .status(status)
                    .build();
        }).collect(Collectors.toList());

        return CouponAnalyticsResponse.builder()
                .totalCoupons(totalCoupons)
                .activeCoupons(activeCoupons)
                .expiredCoupons(expiredCoupons)
                .disabledCoupons(disabledCoupons)
                .totalUsageCount(totalUsageCount)
                .totalDiscountGiven(totalDiscountGiven != null ? totalDiscountGiven : BigDecimal.ZERO)
                .topCouponCode(topCoupon != null ? topCoupon.getCode() : "N/A")
                .topCouponUsageCount(topCoupon != null && topCoupon.getUsedCount() != null ? topCoupon.getUsedCount() : 0)
                .couponBreakdown(breakdown)
                .build();
    }

    private void validateCouponConfiguration(
            CouponDiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maximumDiscount,
            LocalDateTime startDate,
            LocalDateTime expiryDate,
            CouponApplicabilityScope scope,
            Set<Long> categoryIds,
            Set<Long> productIds,
            Set<Long> vendorIds
    ) {
        if (discountType == null) {
            throw new InvalidCouponException("Discount type is required.");
        }
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCouponException("Discount value must be greater than zero.");
        }
        if (discountType == CouponDiscountType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidCouponException("Percentage discount cannot exceed 100%.");
        }
        if (maximumDiscount != null && maximumDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidCouponException("Maximum discount must be greater than zero.");
        }
        if (startDate != null && expiryDate != null && startDate.isAfter(expiryDate)) {
            throw new InvalidCouponException("Start date cannot be after expiry date.");
        }

        if (scope != null && scope.isCategory()) {
            if (categoryIds == null || categoryIds.isEmpty()) {
                throw new InvalidCouponException("At least one eligible category is required for CATEGORY scope coupon.");
            }
        } else if (scope != null && scope.isProduct()) {
            if (productIds == null || productIds.isEmpty()) {
                throw new InvalidCouponException("At least one eligible product is required for PRODUCT scope coupon.");
            }
        } else if (scope != null && scope.isVendor()) {
            if (vendorIds == null || vendorIds.isEmpty()) {
                throw new InvalidCouponException("At least one eligible vendor is required for VENDOR scope coupon.");
            }
        }
    }

    private Set<Category> resolveCategories(CouponApplicabilityScope scope, Set<Long> categoryIds) {
        if (scope == null || !scope.isCategory() || categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.isEmpty()) {
            throw new InvalidCouponException("None of the specified categories were found.");
        }
        return new HashSet<>(categories);
    }

    private Set<Product> resolveProducts(CouponApplicabilityScope scope, Set<Long> productIds) {
        if (scope == null || !scope.isProduct() || productIds == null || productIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Product> products = productRepository.findAllById(productIds);
        if (products.isEmpty()) {
            throw new InvalidCouponException("None of the specified products were found.");
        }
        return new HashSet<>(products);
    }

    private Set<VendorProfile> resolveVendors(CouponApplicabilityScope scope, Set<Long> vendorIds) {
        if (scope == null || !scope.isVendor() || vendorIds == null || vendorIds.isEmpty()) {
            return new HashSet<>();
        }
        List<VendorProfile> vendors = vendorProfileRepository.findAllById(vendorIds);
        if (vendors.isEmpty()) {
            throw new InvalidCouponException("None of the specified vendors were found.");
        }
        return new HashSet<>(vendors);
    }

    private BigDecimal calculateEligibleSubtotal(Coupon coupon, List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        CouponApplicabilityScope scope = coupon.getApplicabilityScope() != null
                ? coupon.getApplicabilityScope()
                : CouponApplicabilityScope.ENTIRE_PLATFORM;

        BigDecimal eligibleSubtotal = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            if (item == null || item.getProduct() == null) continue;
            Product product = item.getProduct();
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

            boolean isEligible;
            if (scope.isPlatform()) {
                isEligible = true;
            } else if (scope.isCategory()) {
                isEligible = product.getCategory() != null && coupon.getEligibleCategories() != null
                        && coupon.getEligibleCategories().stream().anyMatch(c -> c.getId().equals(product.getCategory().getId()));
            } else if (scope.isProduct()) {
                isEligible = coupon.getEligibleProducts() != null
                        && coupon.getEligibleProducts().stream().anyMatch(p -> p.getId().equals(product.getId()));
            } else if (scope.isVendor()) {
                isEligible = product.getVendorProfile() != null && coupon.getEligibleVendors() != null
                        && coupon.getEligibleVendors().stream().anyMatch(v -> v.getId().equals(product.getVendorProfile().getId()));
            } else {
                isEligible = true;
            }

            if (isEligible) {
                eligibleSubtotal = eligibleSubtotal.add(lineTotal);
            }
        }

        return eligibleSubtotal;
    }

    private BigDecimal calculateDiscountValue(Coupon coupon, BigDecimal eligibleSubtotal) {
        if (eligibleSubtotal == null || eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == CouponDiscountType.PERCENTAGE) {
            discount = eligibleSubtotal.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscount() != null && discount.compareTo(coupon.getMaximumDiscount()) > 0) {
                discount = coupon.getMaximumDiscount();
            }
        } else if (coupon.getDiscountType() == CouponDiscountType.FIXED_AMOUNT || coupon.getDiscountType() == CouponDiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        }

        if (discount.compareTo(eligibleSubtotal) > 0) {
            discount = eligibleSubtotal;
        }

        return discount.max(BigDecimal.ZERO);
    }

    private String buildOfferMessage(Coupon coupon) {
        CouponApplicabilityScope scope = coupon.getApplicabilityScope() != null
                ? coupon.getApplicabilityScope()
                : CouponApplicabilityScope.ENTIRE_PLATFORM;
        boolean isPct = coupon.getDiscountType() == CouponDiscountType.PERCENTAGE;
        String valStr = isPct ? coupon.getDiscountValue() + "% OFF" : "₹" + coupon.getDiscountValue() + " OFF";

        if (scope.isCategory()) {
            return valStr + " on eligible category items";
        } else if (scope.isProduct()) {
            return valStr + " on eligible products";
        } else if (scope.isVendor()) {
            return valStr + " on eligible vendor products";
        } else {
            return valStr + " on eligible orders";
        }
    }
}
