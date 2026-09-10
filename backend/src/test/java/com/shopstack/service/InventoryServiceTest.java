package com.shopstack.service;

import com.shopstack.dto.inventory.InventoryCreateRequest;
import com.shopstack.dto.inventory.InventoryResponse;
import com.shopstack.dto.inventory.InventoryThresholdUpdateRequest;
import com.shopstack.dto.inventory.StockAddRequest;
import com.shopstack.dto.inventory.StockAdjustmentRequest;
import com.shopstack.dto.inventory.StockReleaseRequest;
import com.shopstack.dto.inventory.StockReserveRequest;
import com.shopstack.dto.inventory.StockReturnRequest;
import com.shopstack.entity.Category;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.Product;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.StockMovementType;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.entity.VendorStatus;
import com.shopstack.exception.DuplicateInventoryException;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.InventoryNotFoundException;
import com.shopstack.exception.InvalidStockOperationException;
import com.shopstack.exception.ProductOwnershipException;
import com.shopstack.mapper.InventoryMapper;
import com.shopstack.mapper.StockMovementMapper;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VendorProfileRepository vendorProfileRepository;

    @Spy
    private InventoryMapper inventoryMapper = new InventoryMapper();

    @Spy
    private StockMovementMapper stockMovementMapper = new StockMovementMapper();

    @InjectMocks
    private InventoryService inventoryService;

    private User warehouseUser;
    private User vendorUser;
    private User otherVendorUser;
    private VendorProfile vendorProfile;
    private VendorProfile otherVendorProfile;
    private Category category;
    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        warehouseUser = User.builder()
                .id(1L)
                .email("warehouse@shopstack.com")
                .role(UserRole.WAREHOUSE_STAFF)
                .build();

        vendorUser = User.builder()
                .id(2L)
                .email("vendor@shopstack.com")
                .role(UserRole.VENDOR)
                .build();

        otherVendorUser = User.builder()
                .id(3L)
                .email("othervendor@shopstack.com")
                .role(UserRole.VENDOR)
                .build();

        vendorProfile = VendorProfile.builder()
                .id(10L)
                .user(vendorUser)
                .storeName("Tech Store")
                .status(VendorStatus.APPROVED)
                .build();

        otherVendorProfile = VendorProfile.builder()
                .id(20L)
                .user(otherVendorUser)
                .storeName("Other Store")
                .status(VendorStatus.APPROVED)
                .build();

        category = Category.builder()
                .id(100L)
                .name("Electronics")
                .slug("electronics")
                .build();

        product = Product.builder()
                .id(500L)
                .name("Wireless Headset")
                .sku("HEADSET-001")
                .slug("wireless-headset")
                .price(new BigDecimal("99.99"))
                .vendorProfile(vendorProfile)
                .category(category)
                .stockQuantity(50)
                .build();

        inventory = Inventory.builder()
                .id(1000L)
                .product(product)
                .totalStock(50)
                .reservedStock(10)
                .availableStock(40)
                .lowStockThreshold(15)
                .version(1)
                .build();
    }

    private void authenticateUser(User user, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("Inventory Creation Tests")
    class CreateInventoryTests {

        @Test
        @DisplayName("Should successfully create inventory and record initial stock movement")
        void createInventory_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");
            InventoryCreateRequest request = new InventoryCreateRequest(500L, 100, 20);

            when(productRepository.findById(500L)).thenReturn(Optional.of(product));
            when(inventoryRepository.existsByProductId(500L)).thenReturn(false);
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> {
                Inventory inv = i.getArgument(0);
                inv.setId(1001L);
                return inv;
            });
            when(userRepository.findByEmailIgnoreCase("warehouse@shopstack.com")).thenReturn(Optional.of(warehouseUser));

            InventoryResponse response = inventoryService.createInventory(request);

            assertThat(response).isNotNull();
            assertThat(response.productId()).isEqualTo(500L);
            assertThat(response.totalStock()).isEqualTo(100);
            assertThat(response.availableStock()).isEqualTo(100);
            assertThat(response.lowStockThreshold()).isEqualTo(20);

            ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
            verify(stockMovementRepository).save(movementCaptor.capture());
            StockMovement movement = movementCaptor.getValue();
            assertThat(movement.getMovementType()).isEqualTo(StockMovementType.IN);
            assertThat(movement.getQuantity()).isEqualTo(100);
            assertThat(movement.getPreviousStock()).isEqualTo(0);
            assertThat(movement.getNewStock()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should throw DuplicateInventoryException when inventory already exists")
        void createInventory_Duplicate_ThrowsException() {
            InventoryCreateRequest request = new InventoryCreateRequest(500L, 50, 10);
            when(productRepository.findById(500L)).thenReturn(Optional.of(product));
            when(inventoryRepository.existsByProductId(500L)).thenReturn(true);

            assertThatThrownBy(() -> inventoryService.createInventory(request))
                    .isInstanceOf(DuplicateInventoryException.class)
                    .hasMessageContaining("Inventory record already exists");
        }

        @Test
        @DisplayName("Should throw InvalidStockOperationException when negative initial stock is passed")
        void createInventory_NegativeInitialStock_ThrowsException() {
            InventoryCreateRequest request = new InventoryCreateRequest(500L, -10, 10);
            when(productRepository.findById(500L)).thenReturn(Optional.of(product));
            when(inventoryRepository.existsByProductId(500L)).thenReturn(false);

            assertThatThrownBy(() -> inventoryService.createInventory(request))
                    .isInstanceOf(InvalidStockOperationException.class)
                    .hasMessageContaining("Initial stock cannot be negative");
        }
    }

    @Nested
    @DisplayName("Stock IN, OUT, RESERVE, RELEASE, ADJUSTMENT & RETURN Operations")
    class StockOperationsTests {

        @Test
        @DisplayName("Add Stock (IN): Should increase totalStock and availableStock cleanly")
        void addStock_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");
            StockAddRequest request = new StockAddRequest(30, "PO-9912", "Supplier shipment received");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));
            when(userRepository.findByEmailIgnoreCase("warehouse@shopstack.com")).thenReturn(Optional.of(warehouseUser));

            InventoryResponse response = inventoryService.addStock(500L, request);

            assertThat(response.totalStock()).isEqualTo(80); // 50 + 30
            assertThat(response.reservedStock()).isEqualTo(10);
            assertThat(response.availableStock()).isEqualTo(70); // 80 - 10

            verify(stockMovementRepository).save(any(StockMovement.class));
        }

        @Test
        @DisplayName("Reserve Stock (RESERVED): Should increase reservedStock and decrease availableStock")
        void reserveStock_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");
            StockReserveRequest request = new StockReserveRequest(15, "ORD-1001", "Order placed");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

            InventoryResponse response = inventoryService.reserveStock(500L, request);

            assertThat(response.totalStock()).isEqualTo(50);
            assertThat(response.reservedStock()).isEqualTo(25); // 10 + 15
            assertThat(response.availableStock()).isEqualTo(25); // 50 - 25
        }

        @Test
        @DisplayName("Reserve Stock: Should throw InsufficientStockException when available stock is too low")
        void reserveStock_InsufficientStock_ThrowsException() {
            StockReserveRequest request = new StockReserveRequest(50, "ORD-1002", "Order placed");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.reserveStock(500L, request))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Insufficient available stock");
        }

        @Test
        @DisplayName("Release Reserved Stock (RELEASED): Should decrease reservedStock and restore availableStock")
        void releaseReservedStock_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");
            StockReleaseRequest request = new StockReleaseRequest(5, "ORD-1001", "Order cancelled");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

            InventoryResponse response = inventoryService.releaseReservedStock(500L, request);

            assertThat(response.reservedStock()).isEqualTo(5); // 10 - 5
            assertThat(response.availableStock()).isEqualTo(45); // 50 - 5
        }

        @Test
        @DisplayName("Release Reserved Stock: Should throw InvalidStockOperationException if releasing more than reserved")
        void releaseReservedStock_ExceedsReserved_ThrowsException() {
            StockReleaseRequest request = new StockReleaseRequest(20, "ORD-1001", "Order cancelled");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.releaseReservedStock(500L, request))
                    .isInstanceOf(InvalidStockOperationException.class)
                    .hasMessageContaining("Cannot release 20 units from reserved stock");
        }

        @Test
        @DisplayName("Deduct Fulfilled Stock (OUT): Should decrease totalStock and reservedStock on shipment")
        void deductFulfilledStock_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

            InventoryResponse response = inventoryService.deductFulfilledStock(500L, 5, "SHP-300", "Shipment dispatched");

            assertThat(response.totalStock()).isEqualTo(45); // 50 - 5
            assertThat(response.reservedStock()).isEqualTo(5); // 10 - 5
            assertThat(response.availableStock()).isEqualTo(40); // 45 - 5
        }

        @Test
        @DisplayName("Stock ADJUSTMENT: Should override totalStock and recalculate availableStock safely")
        void adjustStock_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");
            StockAdjustmentRequest request = new StockAdjustmentRequest(60, "AUDIT-1", "Stocktake reconciliation");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

            InventoryResponse response = inventoryService.adjustStock(500L, request);

            assertThat(response.totalStock()).isEqualTo(60);
            assertThat(response.availableStock()).isEqualTo(50); // 60 - 10
        }

        @Test
        @DisplayName("Stock ADJUSTMENT: Should throw InvalidStockOperationException if new total is below reserved")
        void adjustStock_BelowReserved_ThrowsException() {
            StockAdjustmentRequest request = new StockAdjustmentRequest(5, "AUDIT-2", "Reduction");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));

            assertThatThrownBy(() -> inventoryService.adjustStock(500L, request))
                    .isInstanceOf(InvalidStockOperationException.class)
                    .hasMessageContaining("cannot be less than currently reserved stock");
        }

        @Test
        @DisplayName("Record Return Stock (RETURN): Should increase physical and available stock")
        void recordReturnStock_Success() {
            authenticateUser(warehouseUser, "ROLE_WAREHOUSE_STAFF");
            StockReturnRequest request = new StockReturnRequest(2, "RMA-55", "Defect free return");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

            InventoryResponse response = inventoryService.recordReturnStock(500L, request);

            assertThat(response.totalStock()).isEqualTo(52); // 50 + 2
            assertThat(response.availableStock()).isEqualTo(42); // 52 - 10
        }
    }

    @Nested
    @DisplayName("Vendor Ownership & Access Protection Tests")
    class VendorAccessTests {

        @Test
        @DisplayName("Vendor viewing own product inventory should succeed")
        void getInventoryByProductId_VendorOwnProduct_Success() {
            authenticateUser(vendorUser, "ROLE_VENDOR");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(userRepository.findByEmailIgnoreCase("vendor@shopstack.com")).thenReturn(Optional.of(vendorUser));
            when(vendorProfileRepository.findByUserId(2L)).thenReturn(Optional.of(vendorProfile));

            InventoryResponse response = inventoryService.getInventoryByProductId(500L);

            assertThat(response).isNotNull();
            assertThat(response.productId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Vendor attempting to access another vendor's inventory should throw ProductOwnershipException")
        void getInventoryByProductId_VendorOtherProduct_ThrowsException() {
            authenticateUser(otherVendorUser, "ROLE_VENDOR");

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(userRepository.findByEmailIgnoreCase("othervendor@shopstack.com")).thenReturn(Optional.of(otherVendorUser));
            when(vendorProfileRepository.findByUserId(3L)).thenReturn(Optional.of(otherVendorProfile));

            assertThatThrownBy(() -> inventoryService.getInventoryByProductId(500L))
                    .isInstanceOf(ProductOwnershipException.class)
                    .hasMessageContaining("belongs to a different vendor");
        }
    }

    @Nested
    @DisplayName("Low Stock Detection & Calculation Tests")
    class LowStockTests {

        @Test
        @DisplayName("isLowStock should return true when totalStock <= lowStockThreshold")
        void isLowStock_Calculation() {
            Inventory lowStockInventory = Inventory.builder()
                    .totalStock(10)
                    .lowStockThreshold(10)
                    .build();

            assertThat(lowStockInventory.isLowStock()).isTrue();

            lowStockInventory.setTotalStock(11);
            assertThat(lowStockInventory.isLowStock()).isFalse();
        }

        @Test
        @DisplayName("Update low stock threshold should modify setting successfully")
        void updateLowStockThreshold_Success() {
            InventoryThresholdUpdateRequest request = new InventoryThresholdUpdateRequest(60);

            when(inventoryRepository.findByProductId(500L)).thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

            InventoryResponse response = inventoryService.updateLowStockThreshold(500L, request);

            assertThat(response.lowStockThreshold()).isEqualTo(60);
            assertThat(response.isLowStock()).isTrue(); // 50 <= 60 is true
        }
    }
}
