package com.warehousing.wmsapi.adjustment.service;

import com.warehousing.wmsapi.adjustment.enums.StockAdjustmentStatus;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentCreateRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentItemRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentListItemResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentRejectRequest;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentResponse;
import com.warehousing.wmsapi.adjustment.dto.StockAdjustmentUpdateRequest;
import com.warehousing.wmsapi.adjustment.repository.StockAdjustmentRepository;
import com.warehousing.wmsapi.adjustment.repository.StockAdjustmentRepository.Header;
import com.warehousing.wmsapi.adjustment.repository.StockAdjustmentRepository.ItemInput;
import com.warehousing.wmsapi.adjustment.repository.StockAdjustmentRepository.ItemRow;
import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inventory.StockBalanceService;
import com.warehousing.wmsapi.inventory.StockMovementService;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAdjustmentService {
    private final StockAdjustmentRepository repository;
    private final WarehouseService warehouseService;
    private final StockBalanceService stockBalanceService;
    private final StockMovementService stockMovementService;

    public StockAdjustmentService(StockAdjustmentRepository repository, WarehouseService warehouseService,
            StockBalanceService stockBalanceService, StockMovementService stockMovementService) {
        this.repository = repository;
        this.warehouseService = warehouseService;
        this.stockBalanceService = stockBalanceService;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockAdjustmentResponse create(Authentication authentication, StockAdjustmentCreateRequest request) {
        WarehouseEntity warehouse = activeWarehouse(authentication, request.warehouseId());
        String reason = normalizedReason(request.reason());
        List<ItemInput> items = validateItems(warehouse.getId(), request.items());
        UUID creatorId = repository.activeUserId(authentication.getName());
        UUID id = repository.insert(warehouse.getId(), referenceNumber(), request.adjustmentType(), reason, creatorId);
        repository.insertItems(id, items);
        repository.audit(creatorId, "CREATE", id, "Stock adjustment draft created. Reason: " + reason);
        return response(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentListItemResponse> list(Authentication authentication, BasePageRequest request) {
        return repository.list(authentication.getName(), request);
    }

    @Transactional(readOnly = true)
    public StockAdjustmentResponse get(Authentication authentication, UUID id) {
        Header header = requireHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        return repository.response(header);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockAdjustmentResponse update(Authentication authentication, UUID id,
            StockAdjustmentUpdateRequest request) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        ensureEditable(header);
        String reason = normalizedReason(request.reason());
        List<ItemInput> items = validateItems(header.warehouseId(), request.items());
        repository.replaceItems(id, items);
        repository.updateHeader(id, request.adjustmentType(), reason);
        repository.audit(repository.activeUserId(authentication.getName()), "UPDATE", id,
                "Stock adjustment draft updated. Reason: " + reason);
        return response(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public void delete(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != StockAdjustmentStatus.PENDING || header.submittedAt() != null) {
            throw conflict("ADJUSTMENT_NOT_DRAFT", "Only an unsubmitted stock adjustment draft can be deleted.");
        }
        repository.softDelete(id);
        repository.audit(repository.activeUserId(authentication.getName()), "DELETE", id,
                "Stock adjustment draft deleted.");
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockAdjustmentResponse submit(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != StockAdjustmentStatus.PENDING || header.submittedAt() != null) {
            throw conflict("ADJUSTMENT_NOT_DRAFT", "Only a pending adjustment draft can be submitted.");
        }
        if (header.reason().isBlank()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "ADJUSTMENT_REASON_REQUIRED",
                    "A reason is required before submitting the adjustment.");
        }
        if (repository.itemCount(id) == 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "ADJUSTMENT_ITEMS_REQUIRED",
                    "Add at least one item before submitting the adjustment.");
        }
        repository.submit(id);
        repository.audit(repository.activeUserId(authentication.getName()), "REQUEST_APPROVAL", id,
                "Stock adjustment submitted for approval. Reason: " + header.reason());
        return response(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockAdjustmentResponse approve(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() == StockAdjustmentStatus.APPROVED || repository.hasMovements(id)) {
            throw conflict("ADJUSTMENT_ALREADY_APPROVED", "This stock adjustment has already been approved.");
        }
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        List<ItemRow> items = repository.findItems(id);
        validateCurrentItems(header.warehouseId(), items);
        List<StockBalanceService.AdjustmentLine> lines = items.stream().map(item ->
                new StockBalanceService.AdjustmentLine(item.productId(), item.locationId(), item.quantityChange())).toList();
        List<Integer> stockAfters = stockBalanceService.applyAdjustments(header.warehouseId(), lines);
        for (int index = 0; index < items.size(); index++) {
            ItemRow item = items.get(index);
            stockMovementService.recordAdjustment(header.warehouseId(), item.locationId(), item.productId(),
                    item.quantityChange(), stockAfters.get(index), id, item.id(), reviewerId);
        }
        repository.approve(id, reviewerId);
        repository.audit(reviewerId, "APPROVE", id,
                "Stock adjustment approved by " + authentication.getName() + ". Reason: " + header.reason());
        return response(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockAdjustmentResponse reject(Authentication authentication, UUID id, StockAdjustmentRejectRequest request) {
        Header header = repository.lockHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        String note = request.rejectionNote().trim();
        repository.reject(id, reviewerId, note);
        repository.audit(reviewerId, "REJECT", id, "Stock adjustment rejected: " + note);
        return response(id);
    }

    private List<ItemInput> validateItems(UUID warehouseId, List<StockAdjustmentItemRequest> requestedItems) {
        Set<UUID> productIds = new HashSet<>();
        for (StockAdjustmentItemRequest item : requestedItems) {
            if (item.quantityChange() == 0) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_ADJUSTMENT_QUANTITY",
                        "Adjustment quantity change must not be zero.");
            }
            if (!productIds.add(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "DUPLICATE_ADJUSTMENT_PRODUCT",
                        "An adjustment can contain only one line for each product.");
            }
            if (!repository.isActiveLocationInWarehouse(item.warehouseLocationId(), warehouseId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_ADJUSTMENT_LOCATION",
                        "Adjustment locations must be active and belong to the adjustment warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Choose an existing active product for every adjustment item.");
            }
        }
        return requestedItems.stream().map(item -> new ItemInput(item.productId(), item.warehouseLocationId(),
                item.quantityChange(), item.notes())).toList();
    }

    private void validateCurrentItems(UUID warehouseId, List<ItemRow> items) {
        for (ItemRow item : items) {
            if (!repository.isActiveLocationInWarehouse(item.locationId(), warehouseId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_ADJUSTMENT_LOCATION",
                        "Adjustment locations must remain active and belong to the adjustment warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Adjustment products must remain active at approval time.");
            }
        }
    }

    private String normalizedReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "ADJUSTMENT_REASON_REQUIRED",
                    "A reason is required for every stock adjustment.");
        }
        return reason.trim();
    }

    private WarehouseEntity activeWarehouse(Authentication authentication, UUID warehouseId) {
        warehouseService.requireAccess(authentication, warehouseId);
        WarehouseEntity warehouse = warehouseService.find(warehouseId);
        if (!warehouse.isActive()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INACTIVE_WAREHOUSE",
                    "Choose an active warehouse.");
        }
        return warehouse;
    }

    private Header requireHeader(UUID id) {
        return repository.findHeader(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "STOCK_ADJUSTMENT_NOT_FOUND", "Stock adjustment was not found."));
    }

    private void ensureOwner(Authentication authentication, Header header) {
        if (!repository.activeUserId(authentication.getName()).equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ADJUSTMENT_OWNER_REQUIRED",
                    "Only the adjustment creator can change this request.");
        }
    }

    private void ensureEditable(Header header) {
        boolean draft = header.status() == StockAdjustmentStatus.PENDING && header.submittedAt() == null;
        if (!draft && header.status() != StockAdjustmentStatus.REJECTED) {
            throw conflict("ADJUSTMENT_NOT_EDITABLE", "Only an unsubmitted or rejected adjustment can be revised.");
        }
    }

    private void ensureSubmittedPending(Header header) {
        if (header.status() != StockAdjustmentStatus.PENDING || header.submittedAt() == null) {
            throw conflict("ADJUSTMENT_NOT_REVIEWABLE", "Only a submitted pending adjustment can be reviewed.");
        }
    }

    private void ensureReviewerIsNotCreator(Header header, UUID reviewerId) {
        if (reviewerId.equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ADJUSTMENT_SELF_REVIEW_FORBIDDEN",
                    "The adjustment creator cannot review their own request.");
        }
    }

    private StockAdjustmentResponse response(UUID id) {
        return repository.response(requireHeader(id));
    }

    private String referenceNumber() {
        return "ADJ-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
