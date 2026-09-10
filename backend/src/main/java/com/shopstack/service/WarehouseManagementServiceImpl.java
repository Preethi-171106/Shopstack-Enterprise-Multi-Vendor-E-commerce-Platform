package com.shopstack.service;

import com.shopstack.dto.warehouse.WarehouseCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryCreateRequest;
import com.shopstack.dto.warehouse.WarehouseInventoryResponse;
import com.shopstack.dto.warehouse.WarehouseInventoryUpdateRequest;
import com.shopstack.dto.warehouse.WarehouseResponse;
import com.shopstack.dto.warehouse.WarehouseUpdateRequest;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.Product;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.StockMovementType;
import com.shopstack.entity.User;
import com.shopstack.entity.Warehouse;
import com.shopstack.entity.WarehouseInventory;
import com.shopstack.exception.InvalidStockOperationException;
import com.shopstack.exception.ProductNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseManagementServiceImpl implements WarehouseManagementService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseInventoryMapper warehouseInventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getAllWarehouses(String search) {
        List<Warehouse> warehouses = (search != null && !search.isBlank())
                ? warehouseRepository.searchWarehouses(search.trim())
                : warehouseRepository.findAll();

        return warehouses.stream()
                .map(this::enrichWarehouseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getActiveWarehouses() {
        return warehouseRepository.findByActiveTrue().stream()
                .map(this::enrichWarehouseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + id + " not found"));
        return enrichWarehouseResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse createWarehouse(WarehouseCreateRequest request) {
        String code = request.getWarehouseCode().trim().toUpperCase();
        if (warehouseRepository.existsByWarehouseCodeIgnoreCase(code)) {
            throw new WarehouseAlreadyExistsException("Warehouse with code '" + code + "' already exists");
        }

        Warehouse warehouse = warehouseMapper.toEntity(request);
        Warehouse saved = warehouseRepository.save(warehouse);
        return warehouseMapper.toResponse(saved, 0, 0L);
    }

    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseUpdateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + id + " not found"));

        warehouseMapper.updateEntityFromRequest(warehouse, request);
        Warehouse saved = warehouseRepository.save(warehouse);
        return enrichWarehouseResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseResponse activateWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + id + " not found"));
        warehouse.setActive(true);
        Warehouse saved = warehouseRepository.save(warehouse);
        return enrichWarehouseResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseResponse deactivateWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + id + " not found"));
        warehouse.setActive(false);
        Warehouse saved = warehouseRepository.save(warehouse);
        return enrichWarehouseResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseInventoryResponse> getWarehouseInventories(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new WarehouseNotFoundException("Warehouse with ID " + warehouseId + " not found");
        }
        return warehouseInventoryRepository.findByWarehouseId(warehouseId).stream()
                .map(warehouseInventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseInventoryResponse> getAllWarehouseInventories() {
        return warehouseInventoryRepository.findAll().stream()
                .map(warehouseInventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarehouseInventoryResponse addOrUpdateProductStock(Long warehouseId, WarehouseInventoryCreateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + warehouseId + " not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + request.getProductId() + " not found"));

        int qty = request.getInitialQuantity() != null ? request.getInitialQuantity() : 0;
        int threshold = request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10;

        Optional<WarehouseInventory> existingOpt = warehouseInventoryRepository.findByWarehouseIdAndProductId(warehouseId, product.getId());
        WarehouseInventory wi;
        int previousQty = 0;

        if (existingOpt.isPresent()) {
            wi = existingOpt.get();
            previousQty = wi.getTotalQuantity();
            wi.setTotalQuantity(previousQty + qty);
            wi.setLowStockThreshold(threshold);
        } else {
            wi = WarehouseInventory.builder()
                    .warehouse(warehouse)
                    .product(product)
                    .totalQuantity(qty)
                    .reservedQuantity(0)
                    .availableQuantity(qty)
                    .lowStockThreshold(threshold)
                    .version(0)
                    .build();
        }

        wi.recalculateAvailableQuantity();
        WarehouseInventory saved = warehouseInventoryRepository.save(wi);

        // Synchronize Global Inventory
        syncGlobalInventory(product);

        // Record stock movement
        User currentUser = resolveOptionalUser();
        recordWarehouseMovement(
                product,
                warehouse,
                StockMovementType.IN,
                qty,
                previousQty,
                saved.getTotalQuantity(),
                "RESTOCK-" + warehouse.getWarehouseCode(),
                "Stock added to warehouse " + warehouse.getWarehouseCode(),
                currentUser
        );

        return warehouseInventoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseInventoryResponse adjustProductStock(Long warehouseId, Long productId, WarehouseInventoryUpdateRequest request) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + warehouseId + " not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + productId + " not found"));

        WarehouseInventory wi = warehouseInventoryRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new InvalidStockOperationException("Product is not stocked in warehouse " + warehouse.getWarehouseCode()));

        int previousStock = wi.getTotalQuantity();
        int newTotalStock;
        int delta;
        StockMovementType movementType = StockMovementType.ADJUSTMENT;
        String adjType = request.getAdjustmentType() != null ? request.getAdjustmentType().trim().toUpperCase() : "SET";
        int qty = request.getQuantity();

        switch (adjType) {
            case "ADD", "STOCK_IN" -> {
                if (qty <= 0) throw new InvalidStockOperationException("Adjustment quantity must be positive");
                newTotalStock = previousStock + qty;
                delta = qty;
                movementType = StockMovementType.IN;
            }
            case "REMOVE", "STOCK_OUT" -> {
                if (qty <= 0) throw new InvalidStockOperationException("Adjustment quantity must be positive");
                if (previousStock - qty < 0) {
                    throw new InvalidStockOperationException("Cannot remove stock: total quantity cannot be negative");
                }
                if (previousStock - qty < wi.getReservedQuantity()) {
                    throw new InvalidStockOperationException("Cannot remove stock below reserved quantity (" + wi.getReservedQuantity() + ")");
                }
                newTotalStock = previousStock - qty;
                delta = qty;
                movementType = StockMovementType.OUT;
            }
            case "DAMAGE" -> {
                if (qty <= 0) throw new InvalidStockOperationException("Adjustment quantity must be positive");
                if (previousStock - qty < wi.getReservedQuantity()) {
                    throw new InvalidStockOperationException("Cannot write off damage below reserved quantity (" + wi.getReservedQuantity() + ")");
                }
                newTotalStock = Math.max(0, previousStock - qty);
                delta = qty;
                movementType = StockMovementType.DAMAGE;
            }
            case "SET", "CORRECTION", "ADJUSTMENT" -> {
                if (qty < 0) throw new InvalidStockOperationException("Stock quantity cannot be negative");
                if (qty < wi.getReservedQuantity()) {
                    throw new InvalidStockOperationException("New total stock (" + qty + ") cannot be less than reserved quantity (" + wi.getReservedQuantity() + ")");
                }
                newTotalStock = qty;
                delta = Math.abs(newTotalStock - previousStock);
                movementType = StockMovementType.ADJUSTMENT;
            }
            default -> throw new InvalidStockOperationException("Unsupported adjustment type: " + adjType);
        }

        wi.setTotalQuantity(newTotalStock);
        wi.recalculateAvailableQuantity();
        WarehouseInventory saved = warehouseInventoryRepository.save(wi);

        // Synchronize Global Inventory
        syncGlobalInventory(product);

        // Record stock movement
        User currentUser = resolveOptionalUser();
        String notes = request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason()
                : "Stock adjustment (" + adjType + ") in " + warehouse.getWarehouseCode();

        recordWarehouseMovement(
                product,
                warehouse,
                movementType,
                delta,
                previousStock,
                newTotalStock,
                "ADJ-" + warehouse.getWarehouseCode(),
                notes,
                currentUser
        );

        return warehouseInventoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public com.shopstack.dto.warehouse.StockDistributionResponse distributeStock(com.shopstack.dto.warehouse.StockDistributionRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + request.getProductId() + " not found"));

        int globalTotalStock = product.getStockQuantity();
        Optional<Inventory> globalInvOpt = inventoryRepository.findByProductId(product.getId());
        if (globalInvOpt.isPresent()) {
            globalTotalStock = Math.max(globalTotalStock, globalInvOpt.get().getTotalStock());
        }

        List<WarehouseInventory> existingWhInvs = warehouseInventoryRepository.findByProductId(product.getId());
        int currentAllocated = existingWhInvs.stream().mapToInt(WarehouseInventory::getTotalQuantity).sum();
        int unallocatedStock = Math.max(0, globalTotalStock - currentAllocated);

        int totalRequestedToDistribute = request.getDistributions().stream()
                .mapToInt(com.shopstack.dto.warehouse.WarehouseDistributionItem::getQuantity)
                .sum();

        if (totalRequestedToDistribute > unallocatedStock) {
            throw new InvalidStockOperationException(
                    "Cannot distribute " + totalRequestedToDistribute + " units. Only " + unallocatedStock +
                    " unallocated central stock available (Global Total: " + globalTotalStock + ", Already Allocated: " + currentAllocated + ")."
            );
        }

        User currentUser = resolveOptionalUser();
        String transferBatchId = "TRF-PROD-" + product.getId() + "-" + System.currentTimeMillis();

        for (com.shopstack.dto.warehouse.WarehouseDistributionItem item : request.getDistributions()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }

            Warehouse warehouse = warehouseRepository.findById(item.getWarehouseId())
                    .orElseThrow(() -> new WarehouseNotFoundException("Warehouse with ID " + item.getWarehouseId() + " not found"));

            if (!warehouse.isActive()) {
                throw new InvalidStockOperationException("Cannot distribute stock to inactive warehouse: " + warehouse.getName());
            }

            Optional<WarehouseInventory> wiOpt = warehouseInventoryRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId());
            WarehouseInventory wi;
            int prevStock = 0;

            if (wiOpt.isPresent()) {
                wi = wiOpt.get();
                prevStock = wi.getTotalQuantity();
                wi.setTotalQuantity(prevStock + item.getQuantity());
            } else {
                wi = WarehouseInventory.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .totalQuantity(item.getQuantity())
                        .reservedQuantity(0)
                        .availableQuantity(item.getQuantity())
                        .lowStockThreshold(10)
                        .version(0)
                        .build();
            }

            wi.recalculateAvailableQuantity();
            WarehouseInventory saved = warehouseInventoryRepository.save(wi);

            // Record immutable StockMovement of type WAREHOUSE_TRANSFER
            String notes = request.getNotes() != null && !request.getNotes().isBlank()
                    ? request.getNotes()
                    : "Distributed " + item.getQuantity() + " units from central vendor stock to " + warehouse.getName() + " (" + warehouse.getWarehouseCode() + ")";

            recordWarehouseMovement(
                    product,
                    warehouse,
                    StockMovementType.WAREHOUSE_TRANSFER,
                    item.getQuantity(),
                    prevStock,
                    saved.getTotalQuantity(),
                    transferBatchId,
                    notes,
                    currentUser
            );
        }

        // Global inventory sync
        syncGlobalInventory(product);

        return getDistributionOverview(product.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public com.shopstack.dto.warehouse.StockDistributionResponse getDistributionOverview(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + productId + " not found"));

        int globalTotalStock = product.getStockQuantity();
        Optional<Inventory> globalInvOpt = inventoryRepository.findByProductId(product.getId());
        if (globalInvOpt.isPresent()) {
            globalTotalStock = Math.max(globalTotalStock, globalInvOpt.get().getTotalStock());
        }

        List<Warehouse> activeWarehouses = warehouseRepository.findByActiveTrue();
        List<WarehouseInventory> existingWhInvs = warehouseInventoryRepository.findByProductId(productId);
        Map<Long, WarehouseInventory> invByWarehouseId = existingWhInvs.stream()
                .collect(Collectors.toMap(wi -> wi.getWarehouse().getId(), wi -> wi, (a, b) -> a));

        List<WarehouseInventoryResponse> responses = new ArrayList<>();
        int totalAllocated = 0;

        for (Warehouse wh : activeWarehouses) {
            WarehouseInventory wi = invByWarehouseId.get(wh.getId());
            if (wi != null) {
                totalAllocated += wi.getTotalQuantity();
                responses.add(warehouseInventoryMapper.toResponse(wi));
            } else {
                responses.add(WarehouseInventoryResponse.builder()
                        .warehouseId(wh.getId())
                        .warehouseCode(wh.getWarehouseCode())
                        .warehouseName(wh.getName())
                        .productId(product.getId())
                        .productName(product.getName())
                        .productSku(product.getSku())
                        .productImageUrl(product.getImageUrl())
                        .productPrice(product.getPrice())
                        .totalQuantity(0)
                        .reservedQuantity(0)
                        .availableQuantity(0)
                        .lowStockThreshold(10)
                        .damagedQuantity(0)
                        .quarantineQuantity(0)
                        .lowStock(false)
                        .outOfStock(true)
                        .build());
            }
        }

        int unallocated = Math.max(0, globalTotalStock - totalAllocated);

        return com.shopstack.dto.warehouse.StockDistributionResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .totalGlobalStock(globalTotalStock)
                .totalWarehouseAllocatedStock(totalAllocated)
                .unallocatedCentralStock(unallocated)
                .warehouseInventories(responses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseInventoryResponse> getDamagedAndQuarantineInventories(Long warehouseId) {
        List<WarehouseInventory> list;
        if (warehouseId != null) {
            list = warehouseInventoryRepository.findDamagedAndQuarantineByWarehouseId(warehouseId);
        } else {
            list = warehouseInventoryRepository.findAllDamagedAndQuarantineInventories();
        }
        return list.stream().map(warehouseInventoryMapper::toResponse).collect(Collectors.toList());
    }

    private void syncGlobalInventory(Product product) {
        List<WarehouseInventory> allWhInv = warehouseInventoryRepository.findByProductId(product.getId());
        int sumTotal = allWhInv.stream().mapToInt(WarehouseInventory::getTotalQuantity).sum();
        int sumReserved = allWhInv.stream().mapToInt(WarehouseInventory::getReservedQuantity).sum();

        Optional<Inventory> globalOpt = inventoryRepository.findByProductId(product.getId());
        int computedGlobalTotal = product.getStockQuantity();
        if (globalOpt.isPresent()) {
            computedGlobalTotal = Math.max(computedGlobalTotal, globalOpt.get().getTotalStock());
        }
        final int finalGlobalTotal = Math.max(computedGlobalTotal, sumTotal);

        Inventory globalInv = globalOpt.orElseGet(() -> Inventory.builder()
                .product(product)
                .totalStock(finalGlobalTotal)
                .reservedStock(sumReserved)
                .availableStock(Math.max(0, finalGlobalTotal - sumReserved))
                .lowStockThreshold(10)
                .version(0)
                .build());

        globalInv.setTotalStock(finalGlobalTotal);
        globalInv.setReservedStock(sumReserved);
        globalInv.recalculateAvailableStock();
        inventoryRepository.save(globalInv);

        product.setStockQuantity(finalGlobalTotal);
        productRepository.save(product);
    }

    private void recordWarehouseMovement(
            Product product,
            Warehouse warehouse,
            StockMovementType type,
            int quantity,
            int previousStock,
            int newStock,
            String referenceId,
            String notes,
            User performedBy
    ) {
        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .movementType(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .referenceId(referenceId)
                .notes(notes)
                .performedBy(performedBy)
                .build();
        stockMovementRepository.save(movement);
    }

    private WarehouseResponse enrichWarehouseResponse(Warehouse w) {
        List<WarehouseInventory> invs = warehouseInventoryRepository.findByWarehouseId(w.getId());
        int totalSkus = invs.size();
        long totalStock = invs.stream().mapToLong(WarehouseInventory::getTotalQuantity).sum();
        return warehouseMapper.toResponse(w, totalSkus, totalStock);
    }

    private User resolveOptionalUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equalsIgnoreCase("anonymousUser")) {
                return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
