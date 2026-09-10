package com.shopstack.service;

import com.shopstack.dto.LoginRequest;
import com.shopstack.dto.warehouse.WarehouseStaffProfileResponse;
import com.shopstack.dto.warehouse.WarehouseStaffRegisterRequest;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseStaffProfile;
import com.shopstack.entity.WarehouseStaffStatus;
import com.shopstack.exception.EmailAlreadyExistsException;
import com.shopstack.exception.WarehouseStaffAccountStatusException;
import com.shopstack.mapper.UserMapper;
import com.shopstack.mapper.WarehouseStaffMapper;
import com.shopstack.repository.CommissionRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import com.shopstack.repository.WarehouseRepository;
import com.shopstack.repository.WarehouseStaffProfileRepository;
import com.shopstack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WarehouseStaffServiceLifecycleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VendorProfileRepository vendorProfileRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseStaffProfileRepository warehouseStaffProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private WarehouseStaffMapper warehouseStaffMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CommissionRepository commissionRepository;

    private AuthService authService;
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                vendorProfileRepository,
                warehouseRepository,
                warehouseStaffProfileRepository,
                passwordEncoder,
                userMapper,
                warehouseStaffMapper,
                jwtService,
                notificationService
        );

        adminUserService = new AdminUserService(
                userRepository,
                vendorProfileRepository,
                productRepository,
                commissionRepository,
                warehouseStaffProfileRepository,
                userMapper,
                null,
                warehouseStaffMapper,
                null,
                passwordEncoder,
                notificationService
        );
    }

    @Test
    @DisplayName("Staff Registration saves User with WAREHOUSE_STAFF role and PENDING profile")
    void registerStaff_SavesUserAndPendingProfile() {
        WarehouseStaffRegisterRequest req = new WarehouseStaffRegisterRequest();
        req.setFirstName("Priya");
        req.setLastName("Sharma");
        req.setEmail("priya@warehouse.com");
        req.setPassword("StaffPass123!");
        req.setWarehouseId(1L);

        given(userRepository.existsByEmailIgnoreCase("priya@warehouse.com")).willReturn(false);
        given(passwordEncoder.encode("StaffPass123!")).willReturn("encodedPassword");

        Warehouse wh = new Warehouse();
        wh.setId(1L);
        wh.setWarehouseCode("WH-CENTRAL");
        given(warehouseRepository.findById(1L)).willReturn(Optional.of(wh));

        User savedUser = new User();
        savedUser.setId(55L);
        savedUser.setEmail("priya@warehouse.com");
        savedUser.setRole(UserRole.WAREHOUSE_STAFF);
        savedUser.setEnabled(true);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        WarehouseStaffProfile savedProfile = WarehouseStaffProfile.builder()
                .id(12L)
                .user(savedUser)
                .warehouse(wh)
                .status(WarehouseStaffStatus.PENDING)
                .build();
        given(warehouseStaffProfileRepository.save(any(WarehouseStaffProfile.class))).willReturn(savedProfile);

        WarehouseStaffProfileResponse expectedResp = WarehouseStaffProfileResponse.builder()
                .id(12L)
                .userId(55L)
                .email("priya@warehouse.com")
                .status(WarehouseStaffStatus.PENDING)
                .warehouseCode("WH-CENTRAL")
                .build();
        given(warehouseStaffMapper.toResponse(savedProfile)).willReturn(expectedResp);

        WarehouseStaffProfileResponse result = authService.registerWarehouseStaff(req);

        assertThat(result.getStatus()).isEqualTo(WarehouseStaffStatus.PENDING);
        assertThat(result.getWarehouseCode()).isEqualTo("WH-CENTRAL");
        verify(userRepository).save(any(User.class));
        verify(warehouseStaffProfileRepository).save(any(WarehouseStaffProfile.class));
    }

    @Test
    @DisplayName("Staff Registration fails on duplicate email")
    void registerStaff_DuplicateEmail_ThrowsException() {
        WarehouseStaffRegisterRequest req = new WarehouseStaffRegisterRequest();
        req.setEmail("existing@shopstack.com");

        given(userRepository.existsByEmailIgnoreCase("existing@shopstack.com")).willReturn(true);

        assertThatThrownBy(() -> authService.registerWarehouseStaff(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("Login blocks PENDING warehouse staff")
    void login_PendingWarehouseStaff_ThrowsForbidden() {
        User staffUser = new User();
        staffUser.setId(60L);
        staffUser.setEmail("pending@staff.com");
        staffUser.setPassword("encoded");
        staffUser.setRole(UserRole.WAREHOUSE_STAFF);
        staffUser.setEnabled(true);

        given(userRepository.findByEmailIgnoreCase("pending@staff.com")).willReturn(Optional.of(staffUser));
        given(passwordEncoder.matches("rawPass", "encoded")).willReturn(true);

        WarehouseStaffProfile profile = WarehouseStaffProfile.builder()
                .id(15L)
                .user(staffUser)
                .status(WarehouseStaffStatus.PENDING)
                .build();
        given(warehouseStaffProfileRepository.findByUserId(60L)).willReturn(Optional.of(profile));

        LoginRequest loginReq = new LoginRequest("pending@staff.com", "rawPass");

        assertThatThrownBy(() -> authService.login(loginReq))
                .isInstanceOf(WarehouseStaffAccountStatusException.class)
                .hasMessageContaining("awaiting administrator approval");
    }

    @Test
    @DisplayName("Admin approval transitions PENDING profile to ACTIVE")
    void adminApprove_TransitionsToActive() {
        User staffUser = new User();
        staffUser.setId(70L);
        staffUser.setEmail("staff70@warehouse.com");
        staffUser.setRole(UserRole.WAREHOUSE_STAFF);
        staffUser.setEnabled(true);

        User adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@shopstack.com");
        adminUser.setFirstName("Super");
        adminUser.setLastName("Admin");

        given(userRepository.findByEmailIgnoreCase("admin@shopstack.com")).willReturn(Optional.of(adminUser));

        WarehouseStaffProfile profile = WarehouseStaffProfile.builder()
                .id(20L)
                .user(staffUser)
                .status(WarehouseStaffStatus.PENDING)
                .build();
        given(warehouseStaffProfileRepository.findByUserId(70L)).willReturn(Optional.of(profile));
        given(warehouseStaffProfileRepository.save(any(WarehouseStaffProfile.class))).willReturn(profile);

        WarehouseStaffProfileResponse expectedResp = WarehouseStaffProfileResponse.builder()
                .id(20L)
                .userId(70L)
                .status(WarehouseStaffStatus.ACTIVE)
                .build();
        given(warehouseStaffMapper.toResponse(any(WarehouseStaffProfile.class))).willReturn(expectedResp);

        WarehouseStaffProfileResponse approved = adminUserService.approveWarehouseStaff(70L, "admin@shopstack.com");

        assertThat(approved.getStatus()).isEqualTo(WarehouseStaffStatus.ACTIVE);
        assertThat(profile.getStatus()).isEqualTo(WarehouseStaffStatus.ACTIVE);
    }
}
