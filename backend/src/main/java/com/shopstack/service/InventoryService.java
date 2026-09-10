package com.shopstack.service;

import com.shopstack.dto.inventory.InventoryCreateRequest;
import com.shopstack.dto.inventory.InventoryResponse;
import com.shopstack.dto.inventory.InventoryThresholdUpdateRequest;
import com.shopstack.dto.inventory.StockAddRequest;
import com.shopstack.dto.inventory.StockAdjustmentRequest;
import com.shopstack.dto.inventory.StockMovementResponse;
import com.shopstack.dto.inventory.StockReleaseRequest;
import com.shopstack.dto.inventory.StockReserveRequest;
import com.shopstack.dto.inventory.StockReturnRequest;
import com.shopstack.entity.Inventory;
import com.shopstack.entity.NotificationType;
import com.shopstack.entity.Product;
import com.shopstack.entity.StockMovement;
import com.shopstack.entity.StockMovementType;
import com.shopstack.entity.User;
import com.shopstack.entity.UserRole;
import com.shopstack.entity.VendorProfile;
import com.shopstack.exception.DuplicateInventoryException;
import com.shopstack.exception.InsufficientStockException;
import com.shopstack.exception.InventoryNotFoundException;
import com.shopstack.exception.InvalidStockOperationException;
import com.shopstack.exception.ProductNotFoundException;
import com.shopstack.exception.ProductOwnershipException;
import com.shopstack.exception.VendorProfileNotFoundException;
import com.shopstack.mapper.InventoryMapper;
import com.shopstack.mapper.StockMovementMapper;
import com.shopstack.repository.InventoryRepository;
import com.shopstack.repository.ProductRepository;
import com.shopstack.repository.StockMovementRepository;
import com.shopstack.repository.UserRepository;
import com.shopstack.repository.VendorProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * InventoryService — Business service handling inventory management, stock reservations,
 * stock adjustments, low-stock detection, and immutable stock movement audit logs.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper stockMovementMapper;
    private final NotificationService notificationService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            VendorProfileRepository vendorProfileRepository,
            InventoryMapper inventoryMapper,
            StockMovementMapper stockMovementMapper,
            NotificationService notificationService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.inventoryMapper = inventoryMapper;
        this.stockMovementMapper = stockMovementMapper;
        this.notificationService = notificationService;
    }

    // =========================================================================
    // Inventory Retrieval
    // =========================================================================

    /**
     * Gets inventory record for a product ID.
     */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        validateVendorOwnershipIfVendor(inventory.getProduct());
        return inventoryMapper.toResponse(inventory);
    }

    /**
     * Gets inventory entity by product ID (internal use).
     */
    @Transactional(readOnly = true)
    public Inventory getInventoryEntityByProductId(Long productId) {
        return findInventoryByProductIdOrThrow(productId);
    }

    /**
     * Retrieves all inventory records (Admin/Warehouse Staff).
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves paged inventory records (Admin/Warehouse Staff).
     */
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventories(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(inventoryMapper::toResponse);
    }

    /**
     * Retrieves inventory records for products belonging to the authenticated vendor.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getVendorInventories() {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        return inventoryRepository.findByProductVendorProfileId(vendorProfile.getId()).stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves paged inventory records for products belonging to the authenticated vendor.
     */
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getVendorInventories(Pageable pageable) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        return inventoryRepository.findByProductVendorProfileId(vendorProfile.getId(), pageable)
                .map(inventoryMapper::toResponse);
    }

    /**
     * Retrieves all products flagged as low-stock (Warehouse Staff / Admin).
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventories() {
        return inventoryRepository.findLowStockInventories().stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all products flagged as out-of-stock (Warehouse Staff / Admin).
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getOutOfStockInventories() {
        return inventoryRepository.findOutOfStockInventories().stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves inventory records with search (by name or SKU) and stock status filtering.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoriesWithFilters(String search, String status) {
        List<Inventory> list;
        if (search != null && !search.trim().isEmpty()) {
            list = inventoryRepository.searchInventories(search.trim());
        } else {
            list = inventoryRepository.findAll();
        }

        return list.stream()
                .filter(inv -> {
                    if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
                        return true;
                    }
                    if (status.equalsIgnoreCase("LOW_STOCK")) {
                        return inv.isLowStock() && inv.getAvailableStock() > 0;
                    }
                    if (status.equalsIgnoreCase("OUT_OF_STOCK")) {
                        return inv.getAvailableStock() <= 0 || inv.getTotalStock() <= 0;
                    }
                    if (status.equalsIgnoreCase("IN_STOCK")) {
                        return inv.getAvailableStock() > 0 && !inv.isLowStock();
                    }
                    return true;
                })
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves low-stock products for the authenticated vendor.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> getVendorLowStockInventories() {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        return inventoryRepository.findLowStockInventoriesByVendorProfileId(vendorProfile.getId()).stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Inventory Creation & Initialization
    // =========================================================================

    /**
     * Initializes inventory for a product.
     */
    @Transactional
    public InventoryResponse createInventory(InventoryCreateRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + request.productId()));

        if (inventoryRepository.existsByProductId(product.getId())) {
            throw new DuplicateInventoryException("Inventory record already exists for product ID: " + product.getId());
        }

        int initialStock = request.initialStock() != null ? request.initialStock() : 0;
        int lowStockThreshold = request.lowStockThreshold() != null ? request.lowStockThreshold() : 10;

        if (initialStock < 0) {
            throw new InvalidStockOperationException("Initial stock cannot be negative");
        }
        if (lowStockThreshold < 0) {
            throw new InvalidStockOperationException("Low stock threshold cannot be negative");
        }

        Inventory inventory = Inventory.builder()
                .product(product)
                .totalStock(initialStock)
                .reservedStock(0)
                .availableStock(initialStock)
                .lowStockThreshold(lowStockThreshold)
                .version(0)
                .build();

        inventory.recalculateAvailableStock();
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Keep product's legacy stockQuantity in sync
        product.setStockQuantity(initialStock);
        productRepository.save(product);

        User currentUser = resolveOptionalAuthenticatedUser();

        if (initialStock > 0) {
            recordStockMovement(
                    product,
                    StockMovementType.IN,
                    initialStock,
                    0,
                    initialStock,
                    "INIT",
                    "Initial stock creation",
                    currentUser
            );
        }

        return inventoryMapper.toResponse(savedInventory);
    }

    /**
     * Helper to auto-create inventory if missing during product creation or stock operations.
     */
    @Transactional
    public Inventory getOrCreateInventoryForProduct(Product product) {
        return inventoryRepository.findByProductId(product.getId())
                .orElseGet(() -> {
                    int initialStock = Math.max(0, product.getStockQuantity());
                    Inventory newInventory = Inventory.builder()
                            .product(product)
                            .totalStock(initialStock)
                            .reservedStock(0)
                            .availableStock(initialStock)
                            .lowStockThreshold(10)
                            .version(0)
                            .build();
                    newInventory.recalculateAvailableStock();
                    return inventoryRepository.save(newInventory);
                });
    }

    // =========================================================================
    // Stock Operations (IN, OUT, ADJUSTMENT, RESERVED, RELEASED, RETURN)
    // =========================================================================

    /**
     * Stock IN — Adds quantity to existing physical stock.
     */
    @Transactional
    public InventoryResponse addStock(Long productId, StockAddRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidStockOperationException("Stock addition quantity must be positive");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        int previousStock = inventory.getTotalStock();
        int newTotalStock = previousStock + request.quantity();

        inventory.setTotalStock(newTotalStock);
        inventory.recalculateAvailableStock();
        Inventory saved = inventoryRepository.save(inventory);

        // Sync product entity
        syncProductStock(inventory.getProduct(), newTotalStock);

        User currentUser = resolveOptionalAuthenticatedUser();
        recordStockMovement(
                inventory.getProduct(),
                StockMovementType.IN,
                request.quantity(),
                previousStock,
                newTotalStock,
                request.referenceId(),
                request.notes() != null ? request.notes() : "Stock added to inventory",
                currentUser
        );

        return inventoryMapper.toResponse(saved);
    }

    /**
     * Stock ADJUSTMENT — Supports direct target stock overrides or delta adjustment operations (ADD, REMOVE, DAMAGE, CORRECTION, RETURN).
     */
    @Transactional
    public InventoryResponse adjustStock(Long productId, StockAdjustmentRequest request) {
        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        int previousStock = inventory.getTotalStock();
        int newTotalStock;
        int delta;
        StockMovementType movementType = StockMovementType.ADJUSTMENT;

        String adjType = request.adjustmentType() != null ? request.adjustmentType().trim().toUpperCase() : null;
        Integer qty = request.quantity();

        if (adjType != null && qty != null) {
            if (qty <= 0) {
                throw new InvalidStockOperationException("Adjustment quantity must be positive");
            }
            switch (adjType) {
                case "ADD", "STOCK_IN" -> {
                    newTotalStock = previousStock + qty;
                    delta = qty;
                    movementType = StockMovementType.IN;
                }
                case "REMOVE", "STOCK_OUT" -> {
                    if (previousStock - qty < 0) {
                        throw new InvalidStockOperationException("Cannot remove stock: resulting stock cannot be negative");
                    }
                    if (previousStock - qty < inventory.getReservedStock()) {
                        throw new InvalidStockOperationException("Cannot remove stock below currently reserved quantity (" + inventory.getReservedStock() + ")");
                    }
                    newTotalStock = previousStock - qty;
                    delta = qty;
                    movementType = StockMovementType.OUT;
                }
                case "DAMAGE" -> {
                    if (previousStock - qty < 0) {
                        throw new InvalidStockOperationException("Cannot write off damage: resulting stock cannot be negative");
                    }
                    if (previousStock - qty < inventory.getReservedStock()) {
                        throw new InvalidStockOperationException("Cannot write off damage below currently reserved quantity (" + inventory.getReservedStock() + ")");
                    }
                    newTotalStock = previousStock - qty;
                    delta = qty;
                    movementType = StockMovementType.DAMAGE;
                }
                case "RETURN" -> {
                    newTotalStock = previousStock + qty;
                    delta = qty;
                    movementType = StockMovementType.RETURN;
                }
                case "CORRECTION", "SET", "ADJUSTMENT" -> {
                    if (request.newTotalStock() != null) {
                        newTotalStock = request.newTotalStock();
                    } else {
                        newTotalStock = qty;
                    }
                    if (newTotalStock < 0) {
                        throw new InvalidStockOperationException("Adjusted stock quantity cannot be negative");
                    }
                    if (newTotalStock < inventory.getReservedStock()) {
                        throw new InvalidStockOperationException("Adjusted total stock (" + newTotalStock + ") cannot be less than currently reserved stock (" + inventory.getReservedStock() + ")");
                    }
                    delta = Math.abs(newTotalStock - previousStock);
                    movementType = StockMovementType.ADJUSTMENT;
                }
                default -> throw new InvalidStockOperationException("Unsupported adjustment type: " + adjType);
            }
        } else {
            // Direct target newTotalStock setting
            if (request.newTotalStock() == null || request.newTotalStock() < 0) {
                throw new InvalidStockOperationException("Adjusted stock quantity cannot be negative");
            }
            if (request.newTotalStock() < inventory.getReservedStock()) {
                throw new InvalidStockOperationException(
                        "Adjusted total stock (" + request.newTotalStock() +
                                ") cannot be less than currently reserved stock (" + inventory.getReservedStock() + ")"
                );
            }
            newTotalStock = request.newTotalStock();
            delta = Math.abs(newTotalStock - previousStock);
            movementType = StockMovementType.ADJUSTMENT;
        }

        inventory.setTotalStock(newTotalStock);
        inventory.recalculateAvailableStock();
        Inventory saved = inventoryRepository.save(inventory);

        // Sync product entity
        syncProductStock(inventory.getProduct(), newTotalStock);

        User currentUser = resolveOptionalAuthenticatedUser();
        String noteReason = request.reason() != null && !request.reason().isBlank()
                ? request.reason()
                : (request.notes() != null && !request.notes().isBlank() ? request.notes() : "Manual stock adjustment");

        recordStockMovement(
                inventory.getProduct(),
                movementType,
                delta,
                previousStock,
                newTotalStock,
                request.referenceId(),
                noteReason,
                currentUser
        );

        if (saved.isLowStock() && notificationService != null) {
            if (saved.getProduct() != null && saved.getProduct().getVendorProfile() != null && saved.getProduct().getVendorProfile().getUser() != null) {
                notificationService.send(
                        saved.getProduct().getVendorProfile().getUser(),
                        NotificationType.LOW_STOCK,
                        "Low Stock Alert: " + saved.getProduct().getName(),
                        "Product '" + saved.getProduct().getName() + "' (SKU: " + saved.getProduct().getSku() + ") is low on stock. Available: " + saved.getAvailableStock() + " units.",
                        saved.getProduct().getId()
                );
            }
        }

        return inventoryMapper.toResponse(saved);
    }

    /**
     * Stock RESERVED — Reserves available stock for a placed order.
     */
    @Transactional
    public InventoryResponse reserveStock(Long productId, StockReserveRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidStockOperationException("Reserve quantity must be positive");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);

        if (inventory.getAvailableStock() < request.quantity()) {
            throw new InsufficientStockException(
                    "Insufficient available stock for product ID " + productId +
                            ". Requested: " + request.quantity() + ", Available: " + inventory.getAvailableStock()
            );
        }

        int previousStock = inventory.getTotalStock();
        inventory.setReservedStock(inventory.getReservedStock() + request.quantity());
        inventory.recalculateAvailableStock();
        Inventory saved = inventoryRepository.save(inventory);

        User currentUser = resolveOptionalAuthenticatedUser();
        recordStockMovement(
                inventory.getProduct(),
                StockMovementType.RESERVED,
                request.quantity(),
                previousStock,
                previousStock,
                request.referenceId(),
                request.notes() != null ? request.notes() : "Stock reserved for order",
                currentUser
        );

        if (saved.isLowStock() && notificationService != null) {
            if (saved.getProduct() != null && saved.getProduct().getVendorProfile() != null && saved.getProduct().getVendorProfile().getUser() != null) {
                notificationService.send(
                        saved.getProduct().getVendorProfile().getUser(),
                        NotificationType.LOW_STOCK,
                        "Low Stock Alert: " + saved.getProduct().getName(),
                        "Product '" + saved.getProduct().getName() + "' (SKU: " + saved.getProduct().getSku() + ") is low on stock. Available: " + saved.getAvailableStock() + " units.",
                        saved.getProduct().getId()
                );
            }
        }

        return inventoryMapper.toResponse(saved);
    }

    /**
     * Stock RELEASED — Releases reserved stock back to the available pool (e.g. order cancelled).
     */
    @Transactional
    public InventoryResponse releaseReservedStock(Long productId, StockReleaseRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidStockOperationException("Release quantity must be positive");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        if (inventory.getReservedStock() < request.quantity()) {
            throw new InvalidStockOperationException(
                    "Cannot release " + request.quantity() + " units from reserved stock. Currently reserved: " +
                            inventory.getReservedStock()
            );
        }

        int previousStock = inventory.getTotalStock();
        inventory.setReservedStock(inventory.getReservedStock() - request.quantity());
        inventory.recalculateAvailableStock();
        Inventory saved = inventoryRepository.save(inventory);

        User currentUser = resolveOptionalAuthenticatedUser();
        recordStockMovement(
                inventory.getProduct(),
                StockMovementType.RELEASED,
                request.quantity(),
                previousStock,
                previousStock,
                request.referenceId(),
                request.notes() != null ? request.notes() : "Reserved stock released",
                currentUser
        );

        return inventoryMapper.toResponse(saved);
    }

    /**
     * Stock OUT — Deducts physical stock when an order is shipped/fulfilled.
     */
    @Transactional
    public InventoryResponse deductFulfilledStock(Long productId, int quantity, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new InvalidStockOperationException("Fulfillment deduction quantity must be positive");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        if (inventory.getTotalStock() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient physical stock to fulfill order. Product ID: " + productId +
                            ", Total stock: " + inventory.getTotalStock() + ", Required: " + quantity
            );
        }

        int previousStock = inventory.getTotalStock();
        int newTotalStock = previousStock - quantity;

        // Deduct from reserved stock as well if available, else zero out reserved
        int newReserved = Math.max(0, inventory.getReservedStock() - quantity);

        inventory.setTotalStock(newTotalStock);
        inventory.setReservedStock(newReserved);
        inventory.recalculateAvailableStock();
        Inventory saved = inventoryRepository.save(inventory);

        // Sync product entity
        syncProductStock(inventory.getProduct(), newTotalStock);

        User currentUser = resolveOptionalAuthenticatedUser();
        recordStockMovement(
                inventory.getProduct(),
                StockMovementType.OUT,
                quantity,
                previousStock,
                newTotalStock,
                referenceId,
                notes != null ? notes : "Stock deducted for order shipment",
                currentUser
        );

        return inventoryMapper.toResponse(saved);
    }

    /**
     * Stock RETURN — Records stock returned by customer, increasing available inventory.
     */
    @Transactional
    public InventoryResponse recordReturnStock(Long productId, StockReturnRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new InvalidStockOperationException("Returned stock quantity must be positive");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        int previousStock = inventory.getTotalStock();
        int newTotalStock = previousStock + request.quantity();

        inventory.setTotalStock(newTotalStock);
        inventory.recalculateAvailableStock();
        Inventory saved = inventoryRepository.save(inventory);

        // Sync product entity
        syncProductStock(inventory.getProduct(), newTotalStock);

        User currentUser = resolveOptionalAuthenticatedUser();
        recordStockMovement(
                inventory.getProduct(),
                StockMovementType.RETURN,
                request.quantity(),
                previousStock,
                newTotalStock,
                request.referenceId(),
                request.notes() != null ? request.notes() : "Customer return restocked",
                currentUser
        );

        return inventoryMapper.toResponse(saved);
    }

    /**
     * Updates low-stock threshold setting.
     */
    @Transactional
    public InventoryResponse updateLowStockThreshold(Long productId, InventoryThresholdUpdateRequest request) {
        if (request.lowStockThreshold() == null || request.lowStockThreshold() < 0) {
            throw new InvalidStockOperationException("Low stock threshold cannot be negative");
        }

        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        inventory.setLowStockThreshold(request.lowStockThreshold());
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    // =========================================================================
    // Stock Movement Audit History
    // =========================================================================

    /**
     * Gets stock movements for a specific product.
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getStockMovements(Long productId, Pageable pageable) {
        Inventory inventory = findInventoryByProductIdOrThrow(productId);
        validateVendorOwnershipIfVendor(inventory.getProduct());
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(stockMovementMapper::toResponse);
    }

    /**
     * Gets all stock movements (Admin/Warehouse Staff).
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getAllStockMovements(Pageable pageable) {
        return stockMovementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(stockMovementMapper::toResponse);
    }

    /**
     * Gets stock movements for all products of authenticated vendor.
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getVendorStockMovements(Pageable pageable) {
        VendorProfile vendorProfile = resolveAuthenticatedVendorProfile();
        return stockMovementRepository.findByProductVendorProfileIdOrderByCreatedAtDesc(vendorProfile.getId(), pageable)
                .map(stockMovementMapper::toResponse);
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private Inventory findInventoryByProductIdOrThrow(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory record not found for product ID: " + productId));
    }

    private void syncProductStock(Product product, int newTotalStock) {
        if (product != null) {
            product.setStockQuantity(newTotalStock);
            productRepository.save(product);
        }
    }

    private void recordStockMovement(
            Product product,
            StockMovementType movementType,
            int quantity,
            int previousStock,
            int newStock,
            String referenceId,
            String notes,
            User performedBy
    ) {
        StockMovement movement = StockMovement.builder()
                .product(product)
                .movementType(movementType)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .referenceId(referenceId)
                .notes(notes)
                .performedBy(performedBy)
                .build();
        stockMovementRepository.save(movement);
    }

    private User resolveOptionalAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(auth.getName()).orElse(null);
    }

    private VendorProfile resolveAuthenticatedVendorProfile() {
        User currentUser = resolveOptionalAuthenticatedUser();
        if (currentUser == null) {
            throw new VendorProfileNotFoundException("Unauthenticated user accessing vendor resource");
        }
        // Use findByUserId — we already have the resolved User object so this is the most direct lookup
        return vendorProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new VendorProfileNotFoundException("No vendor profile found for user: " + currentUser.getEmail()));
    }

    private void validateVendorOwnershipIfVendor(Product product) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        
        boolean isVendor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VENDOR"));

        if (isVendor) {
            VendorProfile currentVendor = resolveAuthenticatedVendorProfile();
            if (product.getVendorProfile() == null || !Objects.equals(product.getVendorProfile().getId(), currentVendor.getId())) {
                throw new ProductOwnershipException("Access denied. Product ID " + product.getId() + " belongs to a different vendor.");
            }
        }
    }
}
