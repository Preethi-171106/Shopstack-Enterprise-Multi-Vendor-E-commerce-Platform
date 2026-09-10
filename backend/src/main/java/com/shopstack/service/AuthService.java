package com.shopstack.service;

import com.shopstack.dto.LoginRequest;
import com.shopstack.dto.LoginResponse;
import com.shopstack.dto.RegisterRequest;
import com.shopstack.dto.UserResponse;
import com.shopstack.dto.warehouse.WarehouseStaffProfileResponse;
import com.shopstack.dto.warehouse.WarehouseStaffRegisterRequest;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.entity.VendorStatus;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseStaffProfile;
import com.shopstack.entity.WarehouseStaffStatus;
import com.shopstack.exception.EmailAlreadyExistsException;
import com.shopstack.exception.InvalidCredentialsException;
import com.shopstack.exception.UserDisabledException;
import com.shopstack.exception.WarehouseStaffAccountStatusException;
import com.shopstack.mapper.UserMapper;
import com.shopstack.mapper.WarehouseStaffMapper;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import com.shopstack.repository.WarehouseRepository;
import com.shopstack.repository.WarehouseStaffProfileRepository;
import com.shopstack.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService — Business service managing authentication and registration flows.
 */
@Service
public class AuthService {

    /** Roles that the standard public registration endpoint may NOT assign. */
    private static final java.util.Set<String> RESTRICTED_ROLES =
            java.util.Set.of("WAREHOUSE_STAFF");

    /** Roles the public endpoint accepts. */
    private static final java.util.Set<String> ALLOWED_PUBLIC_ROLES =
            java.util.Set.of("CUSTOMER", "VENDOR", "ADMIN");

    private final UserRepository userRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStaffProfileRepository warehouseStaffProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final WarehouseStaffMapper warehouseStaffMapper;
    private final JwtService jwtService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            VendorProfileRepository vendorProfileRepository,
            WarehouseRepository warehouseRepository,
            WarehouseStaffProfileRepository warehouseStaffProfileRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            WarehouseStaffMapper warehouseStaffMapper,
            JwtService jwtService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseStaffProfileRepository = warehouseStaffProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.warehouseStaffMapper = warehouseStaffMapper;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
    }

    /**
     * Registers a new user as either a CUSTOMER (default) or VENDOR.
     */
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        // ── 1. Determine and validate the requested role ──────────────────────
        String rawRole = request.registrationRole();
        UserRole assignedRole;

        if (rawRole == null || rawRole.isBlank()) {
            assignedRole = UserRole.CUSTOMER;
        } else if (RESTRICTED_ROLES.contains(rawRole)) {
            throw new IllegalArgumentException(
                    "Invalid account type '" + rawRole + "'. "
                    + "WAREHOUSE_STAFF accounts must register through the dedicated warehouse staff portal.");
        } else if (ALLOWED_PUBLIC_ROLES.contains(rawRole)) {
            assignedRole = UserRole.valueOf(rawRole);
        } else {
            throw new IllegalArgumentException(
                    "Unknown account type '" + rawRole + "'. "
                    + "Valid values are: CUSTOMER, VENDOR, ADMIN.");
        }

        // ── 2. Email uniqueness check ─────────────────────────────────────────
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    "An account with email '" + normalizedEmail + "' already exists.");
        }

        // ── 3. Hash the password ──────────────────────────────────────────────
        String hashedPassword = passwordEncoder.encode(request.password());

        // ── 4. Build and save the User ────────────────────────────────────────
        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(normalizedEmail)
                .password(hashedPassword)
                .phoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null)
                .role(assignedRole)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // ── 5. If VENDOR, auto-create a minimal VendorProfile (PENDING) ───────
        if (assignedRole == UserRole.VENDOR) {
            String storeName = (savedUser.getFirstName() + " " + savedUser.getLastName()).trim() + "'s Store";
            VendorProfile vendorProfile = VendorProfile.builder()
                    .user(savedUser)
                    .storeName(storeName)
                    .status(VendorStatus.PENDING)
                    .build();
            vendorProfileRepository.save(vendorProfile);
        }

        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Dedicated Warehouse Staff registration with PENDING approval status.
     */
    @Transactional
    public WarehouseStaffProfileResponse registerWarehouseStaff(WarehouseStaffRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(
                    "An account with email '" + normalizedEmail + "' already exists.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .password(hashedPassword)
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .role(UserRole.WAREHOUSE_STAFF)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        Warehouse preferredWarehouse = null;
        if (request.getWarehouseId() != null) {
            preferredWarehouse = warehouseRepository.findById(request.getWarehouseId()).orElse(null);
        }

        WarehouseStaffProfile staffProfile = WarehouseStaffProfile.builder()
                .user(savedUser)
                .warehouse(preferredWarehouse)
                .status(WarehouseStaffStatus.PENDING)
                .build();

        WarehouseStaffProfile savedProfile = warehouseStaffProfileRepository.save(staffProfile);

        // Broadcast alert notification to administrators
        try {
            userRepository.findByRole(UserRole.ADMIN).forEach(admin -> {
                notificationService.send(
                        admin,
                        NotificationType.WAREHOUSE_ALERT,
                        "New Warehouse Staff Registration",
                        "Staff registration submitted by " + savedUser.getFirstName() + " " + savedUser.getLastName() + " (" + savedUser.getEmail() + ") is awaiting approval.",
                        savedProfile.getId()
                );
            });
        } catch (Exception e) {
            // Non-critical notification failure does not roll back registration
        }

        return warehouseStaffMapper.toResponse(savedProfile);
    }

    /**
     * Authenticates a user and generates a JWT access token.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            throw new UserDisabledException("User account is disabled");
        }

        // Warehouse Staff Approval Status Verification
        if (user.getRole() == UserRole.WAREHOUSE_STAFF) {
            var profileOpt = warehouseStaffProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                WarehouseStaffProfile profile = profileOpt.get();
                if (profile.getStatus() == WarehouseStaffStatus.PENDING) {
                    throw new WarehouseStaffAccountStatusException("Your Warehouse Staff account is awaiting administrator approval.");
                } else if (profile.getStatus() == WarehouseStaffStatus.REJECTED) {
                    throw new WarehouseStaffAccountStatusException("Your Warehouse Staff registration was rejected.");
                } else if (profile.getStatus() == WarehouseStaffStatus.SUSPENDED) {
                    throw new WarehouseStaffAccountStatusException("Your Warehouse Staff account is currently suspended.");
                }
            }
        }

        String accessToken = jwtService.generateToken(user);
        long expiresIn = jwtService.getExpirationMs();
        UserResponse userResponse = userMapper.toUserResponse(user);

        return new LoginResponse(accessToken, expiresIn, userResponse);
    }

    /**
     * Fetches details of the currently authenticated user by email.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        return userMapper.toUserResponse(user);
    }
}
