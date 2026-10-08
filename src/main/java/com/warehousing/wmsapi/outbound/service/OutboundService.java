package com.warehousing.wmsapi.outbound.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inventory.StockBalanceService;
import com.warehousing.wmsapi.inventory.StockMovementService;
import com.warehousing.wmsapi.outbound.OutboundStatus;
import com.warehousing.wmsapi.outbound.dto.OutboundCreateRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundItemRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundListItemResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundRejectRequest;
import com.warehousing.wmsapi.outbound.dto.OutboundResponse;
import com.warehousing.wmsapi.outbound.dto.OutboundUpdateRequest;
import com.warehousing.wmsapi.outbound.repository.OutboundRepository;
import com.warehousing.wmsapi.outbound.repository.OutboundRepository.Header;
import com.warehousing.wmsapi.outbound.repository.OutboundRepository.ItemInput;
import com.warehousing.wmsapi.outbound.repository.OutboundRepository.ItemRow;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboundService {
    private final OutboundRepository repository;
    private final WarehouseService warehouseService;
    private final StockBalanceService stockBalanceService;
    private final StockMovementService stockMovementService;

    public OutboundService(OutboundRepository repository, WarehouseService warehouseService,
            StockBalanceService stockBalanceService, StockMovementService stockMovementService) {
        this.repository = repository;
        this.warehouseService = warehouseService;
        this.stockBalanceService = stockBalanceService;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public OutboundResponse create(Authentication authentication, OutboundCreateRequest request) {
        activeWarehouse(authentication, request.warehouseId());
        List<ItemInput> items = validateItems(request.warehouseId(), request.items());
        UUID creatorId = repository.activeUserId(authentication.getName());
        UUID id = repository.insert(request.warehouseId(), referenceNumber(), request.notes(), creatorId);
        repository.insertItems(id, items);
        repository.audit(creatorId, "CREATE", id, "Outbound draft created.");
        return response(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<OutboundListItemResponse> list(Authentication authentication, BasePageRequest request) {
        return repository.list(authentication.getName(), request);
    }

    @Transactional(readOnly = true)
    public OutboundResponse get(Authentication authentication, UUID id) {
        Header header = requireHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        return repository.response(header);
    }

    @Transactional
    public OutboundResponse update(Authentication authentication, UUID id, OutboundUpdateRequest request) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        ensureEditable(header);
        List<ItemInput> items = validateItems(header.warehouseId(), request.items());
        repository.replaceItems(id, items);
        repository.updateHeader(id, request.notes());
        repository.audit(repository.activeUserId(authentication.getName()), "UPDATE", id, "Outbound draft updated.");
        return response(id);
    }

    @Transactional
    public void delete(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != OutboundStatus.PENDING || header.submittedAt() != null) {
            throw conflict("OUTBOUND_NOT_DRAFT", "Only an unsubmitted outbound draft can be deleted.");
        }
        repository.softDelete(id);
        repository.audit(repository.activeUserId(authentication.getName()), "DELETE", id, "Outbound draft deleted.");
    }

    @Transactional
    public OutboundResponse submit(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != OutboundStatus.PENDING || header.submittedAt() != null) {
            throw conflict("OUTBOUND_NOT_DRAFT", "Only a pending outbound draft can be submitted.");
        }
        if (repository.itemCount(id) == 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "OUTBOUND_ITEMS_REQUIRED",
                    "Add at least one item before submitting the outbound.");
        }
        repository.submit(id);
        repository.audit(repository.activeUserId(authentication.getName()), "REQUEST_APPROVAL", id,
                "Outbound submitted for approval.");
        return response(id);
    }

    @Transactional
    public OutboundResponse approve(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() == OutboundStatus.APPROVED || repository.hasMovements(id)) {
            throw conflict("OUTBOUND_ALREADY_APPROVED", "This outbound has already been approved.");
        }
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        List<ItemRow> items = repository.findItems(id);
        validateCurrentItems(header.warehouseId(), items);
        List<StockBalanceService.DeductionLine> deductions = items.stream()
                .map(item -> new StockBalanceService.DeductionLine(item.productId(), item.locationId(), item.quantity()))
                .toList();
        List<Integer> stockAfters = stockBalanceService.deductAll(header.warehouseId(), deductions);
        for (int index = 0; index < items.size(); index++) {
            ItemRow item = items.get(index);
            stockMovementService.recordOutbound(header.warehouseId(), item.locationId(), item.productId(),
                    item.quantity(), stockAfters.get(index), id, item.id(), reviewerId);
        }
        repository.approve(id, reviewerId);
        repository.audit(reviewerId, "APPROVE", id, "Outbound approved and posted.");
        return response(id);
    }

    @Transactional
    public OutboundResponse reject(Authentication authentication, UUID id, OutboundRejectRequest request) {
        Header header = repository.lockHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        String note = request.rejectionNote().trim();
        repository.reject(id, reviewerId, note);
        repository.audit(reviewerId, "REJECT", id, "Outbound rejected: " + note);
        return response(id);
    }

    private List<ItemInput> validateItems(UUID warehouseId, List<OutboundItemRequest> requestedItems) {
        Set<UUID> productIds = new HashSet<>();
        for (OutboundItemRequest item : requestedItems) {
            if (item.quantity() < 1) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_OUTBOUND_QUANTITY",
                        "Outbound quantity must be positive.");
            }
            if (!productIds.add(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "DUPLICATE_OUTBOUND_PRODUCT",
                        "An outbound can contain only one line for each product.");
            }
            if (!repository.isActiveLocationInWarehouse(item.warehouseLocationId(), warehouseId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_OUTBOUND_LOCATION",
                        "Outbound locations must be active and belong to the selected warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Choose an existing active product for every outbound item.");
            }
        }
        return requestedItems.stream().map(item -> new ItemInput(item.productId(), item.warehouseLocationId(),
                item.quantity(), item.notes())).toList();
    }

    private void validateCurrentItems(UUID warehouseId, List<ItemRow> items) {
        for (ItemRow item : items) {
            if (!repository.isActiveLocationInWarehouse(item.locationId(), warehouseId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_OUTBOUND_LOCATION",
                        "Outbound locations must remain active and belong to the selected warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Outbound products must remain active at approval time.");
            }
        }
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
                "OUTBOUND_NOT_FOUND", "Outbound was not found."));
    }

    private void ensureOwner(Authentication authentication, Header header) {
        if (!repository.activeUserId(authentication.getName()).equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "OUTBOUND_OWNER_REQUIRED",
                    "Only the outbound creator can change this request.");
        }
    }

    private void ensureEditable(Header header) {
        boolean draft = header.status() == OutboundStatus.PENDING && header.submittedAt() == null;
        if (!draft && header.status() != OutboundStatus.REJECTED) {
            throw conflict("OUTBOUND_NOT_EDITABLE", "Only an unsubmitted or rejected outbound can be revised.");
        }
    }

    private void ensureSubmittedPending(Header header) {
        if (header.status() != OutboundStatus.PENDING || header.submittedAt() == null) {
            throw conflict("OUTBOUND_NOT_REVIEWABLE", "Only a submitted pending outbound can be reviewed.");
        }
    }

    private void ensureReviewerIsNotCreator(Header header, UUID reviewerId) {
        if (reviewerId.equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "OUTBOUND_SELF_REVIEW_FORBIDDEN",
                    "The outbound creator cannot review their own request.");
        }
    }

    private OutboundResponse response(UUID id) {
        return repository.response(requireHeader(id));
    }

    private String referenceNumber() {
        return "OUT-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
