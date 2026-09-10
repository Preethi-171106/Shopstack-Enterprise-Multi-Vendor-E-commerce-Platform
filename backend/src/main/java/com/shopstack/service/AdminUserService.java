package com.shopstack.service;

import com.shopstack.dto.CreateStaffUserRequest;
import com.shopstack.dto.UserResponse;
import com.shopstack.dto.VendorProfileResponse;
import com.shopstack.dto.admin.VendorDetailResponse;
import com.shopstack.dto.product.ProductResponse;
import com.shopstack.entity.Product;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.entity.VendorStatus;
import com.shopstack.exception.EmailAlreadyExistsException;
import com.shopstack.exception.VendorProfileNotFoundException;
import com.shopstack.mapper.ProductMapper;
import com.shopstack.mapper.UserMapper;
import com.shopstack.mapper.VendorProfileMapper;
import com.shopstack.repository.CommissionRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import com.shopstack.dto.warehouse.WarehouseStaffProfileResponse;
import com.shopstack.entity.WarehouseStaffProfile;
import com.shopstack.entity.WarehouseStaffStatus;
import com.shopstack.mapper.WarehouseStaffMapper;
import com.shopstack.repository.WarehouseStaffProfileRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminUserService — Service for admin operations on users, vendor profiles, and warehouse staff.
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final ProductRepository productRepository;
    private final CommissionRepository commissionRepository;
    private final WarehouseStaffProfileRepository warehouseStaffProfileRepository;
    private final UserMapper userMapper;
    private final VendorProfileMapper vendorProfileMapper;
    private final WarehouseStaffMapper warehouseStaffMapper;
    private final ProductMapper productMapper;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AdminUserService(
            UserRepository userRepository,
            VendorProfileRepository vendorProfileRepository,
            ProductRepository productRepository,
            CommissionRepository commissionRepository,
            WarehouseStaffProfileRepository warehouseStaffProfileRepository,
            UserMapper userMapper,
            VendorProfileMapper vendorProfileMapper,
            WarehouseStaffMapper warehouseStaffMapper,
            ProductMapper productMapper,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.productRepository = productRepository;
        this.commissionRepository = commissionRepository;
        this.warehouseStaffProfileRepository = warehouseStaffProfileRepository;
        this.userMapper = userMapper;
        this.vendorProfileMapper = vendorProfileMapper;
        this.warehouseStaffMapper = warehouseStaffMapper;
        this.productMapper = productMapper;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    /**
     * Returns all users in the platform.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns users filtered by role string (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String roleStr) {
        UserRole role;
        try {
            role = UserRole.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr
                    + ". Valid values: CUSTOMER, VENDOR, ADMIN, WAREHOUSE_STAFF");
        }
        return userRepository.findByRole(role)
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single user by their DB id.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        return userMapper.toUserResponse(user);
    }

    /**
     * Enables or disables a user account.
     */
    @Transactional
    public UserResponse setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        user.setEnabled(enabled);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    /**
     * Returns all vendor profiles.
     */
    @Transactional(readOnly = true)
    public List<VendorProfileResponse> getAllVendorProfiles() {
        return vendorProfileRepository.findAll()
                .stream()
                .map(vendorProfileMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns comprehensive vendor details including live sales and product information.
     */
    @Transactional(readOnly = true)
    public VendorDetailResponse getVendorDetails(Long vendorProfileId) {
        VendorProfile profile = vendorProfileRepository.findById(vendorProfileId)
                .orElseThrow(() -> new VendorProfileNotFoundException(
                        "Vendor profile not found with ID: " + vendorProfileId));

        List<Product> products = productRepository.findByVendorProfileId(vendorProfileId);
        long activeProducts = products.stream().filter(Product::isActive).count();

        BigDecimal grossSales = commissionRepository.sumSalesByVendor(vendorProfileId);
        if (grossSales == null) grossSales = BigDecimal.ZERO;

        BigDecimal netEarnings = commissionRepository.sumVendorNetByVendor(vendorProfileId);
        if (netEarnings == null) netEarnings = BigDecimal.ZERO;

        BigDecimal commissionPaid = grossSales.subtract(netEarnings);
        long orderCount = commissionRepository.findByVendorProfileId(vendorProfileId).size();

        List<ProductResponse> recentProducts = products.stream()
                .limit(10)
                .map(productMapper::toResponse)
                .collect(Collectors.toList());

        User user = profile.getUser();

        return VendorDetailResponse.builder()
                .id(profile.getId())
                .userId(user != null ? user.getId() : null)
                .userEmail(profile.getBusinessEmail() != null ? profile.getBusinessEmail() : (user != null ? user.getEmail() : null))
                .userFullName(user != null ? user.getFirstName() + " " + user.getLastName() : null)
                .storeName(profile.getStoreName())
                .description(profile.getStoreDescription())
                .contactNumber(profile.getBusinessPhone())
                .address(profile.getBusinessAddress())
                .taxId(profile.getPostalCode())
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .totalProducts(products.size())
                .activeProducts(activeProducts)
                .totalOrdersCount(orderCount)
                .totalGrossSales(grossSales)
                .totalCommissionPaid(commissionPaid)
                .totalNetEarnings(netEarnings)
                .recentProducts(recentProducts)
                .build();
    }

    /**
     * Updates the approval status of a vendor profile and sends notifications.
     */
    @Transactional
    public VendorProfileResponse setVendorStatus(Long vendorProfileId, VendorStatus status) {
        VendorProfile profile = vendorProfileRepository.findById(vendorProfileId)
                .orElseThrow(() -> new VendorProfileNotFoundException(
                        "Vendor profile not found with ID: " + vendorProfileId));
        profile.setStatus(status);
        VendorProfile saved = vendorProfileRepository.save(profile);

        if (saved.getUser() != null && notificationService != null) {
            switch (status) {
                case APPROVED -> notificationService.send(
                        saved.getUser(),
                        com.shopstack.entity.NotificationType.VENDOR_APPROVED,
                        "Vendor Profile Approved",
                        "Congratulations! Your vendor profile for '" + saved.getStoreName() + "' has been approved. You can now publish products.",
                        saved.getId()
                );
                case REJECTED -> notificationService.send(
                        saved.getUser(),
                        com.shopstack.entity.NotificationType.VENDOR_REJECTED,
                        "Vendor Application Not Approved",
                        "Your vendor application for '" + saved.getStoreName() + "' was reviewed and rejected. Please contact support for details.",
                        saved.getId()
                );
                case SUSPENDED -> notificationService.send(
                        saved.getUser(),
                        com.shopstack.entity.NotificationType.VENDOR_SUSPENDED,
                        "Vendor Account Suspended",
                        "Your vendor account for '" + saved.getStoreName() + "' has been temporarily suspended by administration.",
                        saved.getId()
                );
                default -> {}
            }
        }

        return vendorProfileMapper.toResponse(saved);
    }

    /**
     * Lists all warehouse staff profiles.
     */
    @Transactional(readOnly = true)
    public List<WarehouseStaffProfileResponse> getAllWarehouseStaffProfiles() {
        return warehouseStaffProfileRepository.findAllWithDetails()
                .stream()
                .map(warehouseStaffMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single warehouse staff profile by profile ID or user ID.
     */
    @Transactional(readOnly = true)
    public WarehouseStaffProfileResponse getWarehouseStaffProfile(Long staffProfileOrUserId) {
        WarehouseStaffProfile profile = warehouseStaffProfileRepository.findById(staffProfileOrUserId)
                .or(() -> warehouseStaffProfileRepository.findByUserId(staffProfileOrUserId))
                .orElseThrow(() -> new RuntimeException("Warehouse staff profile not found with ID: " + staffProfileOrUserId));
        return warehouseStaffMapper.toResponse(profile);
    }

    /**
     * Approves a pending warehouse staff profile (sets status to ACTIVE).
     */
    @Transactional
    public WarehouseStaffProfileResponse approveWarehouseStaff(Long staffProfileOrUserId, String adminEmail) {
        WarehouseStaffProfile profile = warehouseStaffProfileRepository.findById(staffProfileOrUserId)
                .or(() -> warehouseStaffProfileRepository.findByUserId(staffProfileOrUserId))
                .orElseThrow(() -> new RuntimeException("Warehouse staff profile not found with ID: " + staffProfileOrUserId));

        User adminUser = null;
        if (adminEmail != null && !adminEmail.isBlank()) {
            adminUser = userRepository.findByEmailIgnoreCase(adminEmail.trim().toLowerCase()).orElse(null);
        }

        profile.setStatus(WarehouseStaffStatus.ACTIVE);
        profile.setApprovedBy(adminUser);
        profile.setApprovedAt(LocalDateTime.now());
        profile.setRejectionReason(null);

        WarehouseStaffProfile saved = warehouseStaffProfileRepository.save(profile);

        if (saved.getUser() != null && notificationService != null) {
            notificationService.send(
                    saved.getUser(),
                    com.shopstack.entity.NotificationType.GENERAL,
                    "Warehouse Staff Account Approved",
                    "Your Warehouse Staff registration has been approved by an administrator. You can now log in and access the warehouse fulfillment center.",
                    saved.getId()
            );
        }

        return warehouseStaffMapper.toResponse(saved);
    }

    /**
     * Rejects a warehouse staff application (sets status to REJECTED).
     */
    @Transactional
    public WarehouseStaffProfileResponse rejectWarehouseStaff(Long staffProfileOrUserId, String reason, String adminEmail) {
        WarehouseStaffProfile profile = warehouseStaffProfileRepository.findById(staffProfileOrUserId)
                .or(() -> warehouseStaffProfileRepository.findByUserId(staffProfileOrUserId))
                .orElseThrow(() -> new RuntimeException("Warehouse staff profile not found with ID: " + staffProfileOrUserId));

        User adminUser = null;
        if (adminEmail != null && !adminEmail.isBlank()) {
            adminUser = userRepository.findByEmailIgnoreCase(adminEmail.trim().toLowerCase()).orElse(null);
        }

        profile.setStatus(WarehouseStaffStatus.REJECTED);
        profile.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Application declined by administrator.");
        profile.setApprovedBy(adminUser);
        profile.setApprovedAt(LocalDateTime.now());

        WarehouseStaffProfile saved = warehouseStaffProfileRepository.save(profile);

        if (saved.getUser() != null && notificationService != null) {
            notificationService.send(
                    saved.getUser(),
                    com.shopstack.entity.NotificationType.GENERAL,
                    "Warehouse Staff Application Declined",
                    "Your Warehouse Staff application was reviewed and rejected. Reason: " + profile.getRejectionReason(),
                    saved.getId()
            );
        }

        return warehouseStaffMapper.toResponse(saved);
    }

    /**
     * Suspends an active warehouse staff account (sets status to SUSPENDED).
     */
    @Transactional
    public WarehouseStaffProfileResponse suspendWarehouseStaff(Long staffProfileOrUserId, String adminEmail) {
        WarehouseStaffProfile profile = warehouseStaffProfileRepository.findById(staffProfileOrUserId)
                .or(() -> warehouseStaffProfileRepository.findByUserId(staffProfileOrUserId))
                .orElseThrow(() -> new RuntimeException("Warehouse staff profile not found with ID: " + staffProfileOrUserId));

        profile.setStatus(WarehouseStaffStatus.SUSPENDED);
        WarehouseStaffProfile saved = warehouseStaffProfileRepository.save(profile);

        if (saved.getUser() != null && notificationService != null) {
            notificationService.send(
                    saved.getUser(),
                    com.shopstack.entity.NotificationType.GENERAL,
                    "Warehouse Staff Account Suspended",
                    "Your Warehouse Staff account has been suspended by administration.",
                    saved.getId()
            );
        }

        return warehouseStaffMapper.toResponse(saved);
    }

    /**
     * Reactivates a suspended warehouse staff account (sets status to ACTIVE).
     */
    @Transactional
    public WarehouseStaffProfileResponse reactivateWarehouseStaff(Long staffProfileOrUserId, String adminEmail) {
        WarehouseStaffProfile profile = warehouseStaffProfileRepository.findById(staffProfileOrUserId)
                .or(() -> warehouseStaffProfileRepository.findByUserId(staffProfileOrUserId))
                .orElseThrow(() -> new RuntimeException("Warehouse staff profile not found with ID: " + staffProfileOrUserId));

        profile.setStatus(WarehouseStaffStatus.ACTIVE);
        profile.setRejectionReason(null);
        WarehouseStaffProfile saved = warehouseStaffProfileRepository.save(profile);

        if (saved.getUser() != null && notificationService != null) {
            notificationService.send(
                    saved.getUser(),
                    com.shopstack.entity.NotificationType.GENERAL,
                    "Warehouse Staff Account Reactivated",
                    "Your Warehouse Staff account has been reactivated. You can now log in.",
                    saved.getId()
            );
        }

        return warehouseStaffMapper.toResponse(saved);
    }

    /**
     * Creates a new WAREHOUSE_STAFF user account. Only callable by ADMIN.
     */
    @Transactional
    public UserResponse createWarehouseStaff(CreateStaffUserRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    "A user with email '" + normalizedEmail + "' already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber() != null
                        ? request.getPhoneNumber().trim() : null)
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-create ACTIVE WarehouseStaffProfile for admin-created staff
        WarehouseStaffProfile staffProfile = WarehouseStaffProfile.builder()
                .user(savedUser)
                .status(WarehouseStaffStatus.ACTIVE)
                .approvedAt(LocalDateTime.now())
                .build();
        warehouseStaffProfileRepository.save(staffProfile);

        return userMapper.toUserResponse(savedUser);
    }
}
