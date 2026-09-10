package com.shopstack.service;

import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryResponse;
import com.shopstack.dto.warehouse.WarehouseResponse;
import com.shopstack.dto.warehouse.WarehouseUpdateRequest;
import com.shopstack.entity.Product;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.exception.WarehouseAlreadyExistsException;
import com.shopstack.exception.WarehouseNotFoundException;
import com.shopstack.mapper.WarehouseInventoryMapper;
import com.shopstack.mapper.WarehouseMapper;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.WarehouseInventoryRepository;
import com.shopstack.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WarehouseManagementServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseInventoryRepository warehouseInventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private WarehouseMapper warehouseMapper = new WarehouseMapper();

    @Spy
    private WarehouseInventoryMapper warehouseInventoryMapper = new WarehouseInventoryMapper();

    @InjectMocks
    private WarehouseManagementServiceImpl warehouseManagementService;

    private Warehouse warehouse;
    private Product product;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .id(1L)
                .warehouseCode("WH-HYD-01")
                .name("Hyderabad Fulfillment Center")
                .address("HITEC City")
                .city("Hyderabad")
                .state("Telangana")
                .postalCode("500081")
                .country("India")
                .active(true)
                .build();

        product = Product.builder()
                .id(10L)
                .name("Smart Watch Series 7")
                .sku("SKU-SW-007")
                .price(BigDecimal.valueOf(12999.00))
                .stockQuantity(50)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create warehouse successfully when warehouseCode is unique")
    void shouldCreateWarehouseSuccessfully() {
        WarehouseCreateRequest req = WarehouseCreateRequest.builder()
                .warehouseCode("WH-HYD-01")
                .name("Hyderabad Fulfillment Center")
                .address("HITEC City")
                .city("Hyderabad")
                .state("Telangana")
                .postalCode("500081")
                .country("India")
                .build();

        when(warehouseRepository.existsByWarehouseCodeIgnoreCase("WH-HYD-01")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        WarehouseResponse res = warehouseManagementService.createWarehouse(req);

        assertThat(res).isNotNull();
        assertThat(res.getWarehouseCode()).isEqualTo("WH-HYD-01");
        assertThat(res.getName()).isEqualTo("Hyderabad Fulfillment Center");
    }

    @Test
    @DisplayName("Should throw WarehouseAlreadyExistsException on duplicate warehouseCode")
    void shouldThrowExceptionOnDuplicateCode() {
        WarehouseCreateRequest req = WarehouseCreateRequest.builder()
                .warehouseCode("WH-HYD-01")
                .name("Hyderabad Center")
                .address("HITEC City")
                .city("Hyderabad")
                .state("Telangana")
                .postalCode("500081")
                .build();

        when(warehouseRepository.existsByWarehouseCodeIgnoreCase("WH-HYD-01")).thenReturn(true);

        assertThatThrownBy(() -> warehouseManagementService.createWarehouse(req))
                .isInstanceOf(WarehouseAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should update warehouse details successfully")
    void shouldUpdateWarehouse() {
        WarehouseUpdateRequest req = WarehouseUpdateRequest.builder()
                .name("Updated Hyderabad Center")
                .address("New HITEC City Phase 2")
                .city("Hyderabad")
                .state("Telangana")
                .postalCode("500081")
                .country("India")
                .active(true)
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        WarehouseResponse res = warehouseManagementService.updateWarehouse(1L, req);

        assertThat(res.getName()).isEqualTo("Updated Hyderabad Center");
        assertThat(res.getAddress()).isEqualTo("New HITEC City Phase 2");
    }

    @Test
    @DisplayName("Should deactivate warehouse")
    void shouldDeactivateWarehouse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(warehouse);

        WarehouseResponse res = warehouseManagementService.deactivateWarehouse(1L);

        assertThat(res.getActive()).isFalse();
        assertThat(warehouse.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should add product stock to warehouse inventory and sync global stock")
    void shouldAddStockToWarehouseInventory() {
        WarehouseInventoryCreateRequest req = WarehouseInventoryCreateRequest.builder()
                .productId(10L)
                .initialQuantity(30)
                .lowStockThreshold(5)
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class)))
                .thenAnswer(inv -> {
                    WarehouseInventory wi = inv.getArgument(0);
                    wi.setId(555L);
                    return wi;
                });
        when(warehouseInventoryRepository.findByProductId(10L))
                .thenReturn(List.of(WarehouseInventory.builder().totalQuantity(30).reservedQuantity(0).build()));

        WarehouseInventoryResponse res = warehouseManagementService.addOrUpdateProductStock(1L, req);

        assertThat(res).isNotNull();
        assertThat(res.getTotalQuantity()).isEqualTo(30);
        assertThat(res.getAvailableQuantity()).isEqualTo(30);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should distribute unallocated stock to warehouses and record WAREHOUSE_TRANSFER movement")
    void shouldDistributeStockSuccessfully() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByActiveTrue()).thenReturn(List.of(warehouse));
        when(warehouseInventoryRepository.findByProductId(10L)).thenReturn(List.of());
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseInventoryRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(warehouseInventoryRepository.save(any(WarehouseInventory.class))).thenAnswer(inv -> inv.getArgument(0));

        com.shopstack.dto.warehouse.StockDistributionRequest request = com.shopstack.dto.warehouse.StockDistributionRequest.builder()
                .productId(10L)
                .distributions(List.of(
                        com.shopstack.dto.warehouse.WarehouseDistributionItem.builder()
                                .warehouseId(1L)
                                .quantity(25)
                                .build()
                ))
                .build();

        com.shopstack.dto.warehouse.StockDistributionResponse response = warehouseManagementService.distributeStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(10L);
        assertThat(response.getGlobalTotalStock()).isEqualTo(50);
        verify(stockMovementRepository).save(any(com.shopstack.entity.StockMovement.class));
    }

    @Test
    @DisplayName("Should retrieve distribution overview correctly")
    void shouldGetDistributionOverview() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findByActiveTrue()).thenReturn(List.of(warehouse));
        when(warehouseInventoryRepository.findByProductId(10L)).thenReturn(List.of(
                WarehouseInventory.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .totalQuantity(20)
                        .reservedQuantity(5)
                        .availableQuantity(15)
                        .build()
        ));

        com.shopstack.dto.warehouse.StockDistributionResponse overview = warehouseManagementService.getDistributionOverview(10L);

        assertThat(overview).isNotNull();
        assertThat(overview.getGlobalTotalStock()).isEqualTo(50);
        assertThat(overview.getAllocatedStock()).isEqualTo(20);
        assertThat(overview.getUnallocatedStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should retrieve all warehouse inventories across the network")
    void shouldGetAllWarehouseInventories() {
        WarehouseInventory wi = WarehouseInventory.builder()
                .id(100L)
                .warehouse(warehouse)
                .product(product)
                .totalQuantity(50)
                .reservedQuantity(10)
                .availableQuantity(40)
                .build();

        when(warehouseInventoryRepository.findAll()).thenReturn(List.of(wi));

        List<WarehouseInventoryResponse> list = warehouseManagementService.getAllWarehouseInventories();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getWarehouseCode()).isEqualTo("WH-HYD-01");
        assertThat(list.get(0).getTotalQuantity()).isEqualTo(50);
        assertThat(list.get(0).getAvailableQuantity()).isEqualTo(40);
    }
}
