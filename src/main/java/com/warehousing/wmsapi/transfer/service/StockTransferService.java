package com.warehousing.wmsapi.transfer.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inventory.StockBalanceService;
import com.warehousing.wmsapi.inventory.StockMovementService;
import com.warehousing.wmsapi.transfer.enums.StockTransferStatus;
import com.warehousing.wmsapi.transfer.dto.StockTransferCreateRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferItemRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferListItemResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferResponse;
import com.warehousing.wmsapi.transfer.dto.StockTransferRejectRequest;
import com.warehousing.wmsapi.transfer.dto.StockTransferUpdateRequest;
import com.warehousing.wmsapi.transfer.repository.StockTransferRepository;
import com.warehousing.wmsapi.transfer.repository.StockTransferRepository.Header;
import com.warehousing.wmsapi.transfer.repository.StockTransferRepository.ItemInput;
import com.warehousing.wmsapi.transfer.repository.StockTransferRepository.ItemRow;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockTransferService {
    private final StockTransferRepository repository;
    private final WarehouseService warehouseService;
    private final StockBalanceService stockBalanceService;
    private final StockMovementService stockMovementService;

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockTransferResponse create(Authentication authentication, StockTransferCreateRequest request) {
        activeWarehouse(authentication, request.warehouseId());
        List<ItemInput> items = validateItems(request.warehouseId(), request.items());
        UUID creatorId = repository.activeUserId(authentication.getName());
        UUID id = repository.insert(request.warehouseId(), referenceNumber(), request.notes(), creatorId);
        repository.insertItems(id, items);
        repository.audit(creatorId, "CREATE", id, "Stock transfer draft created.");
        return response(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockTransferListItemResponse> list(Authentication authentication, BasePageRequest request) {
        return repository.list(authentication.getName(), request);
    }

    @Transactional(readOnly = true)
    public StockTransferResponse get(Authentication authentication, UUID id) {
        Header header = requireHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        return repository.response(header);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockTransferResponse update(Authentication authentication, UUID id, StockTransferUpdateRequest request) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        ensureEditable(header);
        List<ItemInput> items = validateItems(header.warehouseId(), request.items());
        repository.replaceItems(id, items);
        repository.updateHeader(id, request.notes());
        repository.audit(repository.activeUserId(authentication.getName()), "UPDATE", id,
                "Stock transfer draft updated.");
        return response(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public void delete(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != StockTransferStatus.PENDING || header.submittedAt() != null) {
            throw conflict("TRANSFER_NOT_DRAFT", "Only an unsubmitted stock transfer draft can be deleted.");
        }
        repository.softDelete(id);
        repository.audit(repository.activeUserId(authentication.getName()), "DELETE", id,
                "Stock transfer draft deleted.");
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockTransferResponse submit(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != StockTransferStatus.PENDING || header.submittedAt() != null) {
            throw conflict("TRANSFER_NOT_DRAFT", "Only a pending stock transfer draft can be submitted.");
        }
        if (repository.itemCount(id) == 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "TRANSFER_ITEMS_REQUIRED",
                    "Add at least one item before submitting the transfer.");
        }
        repository.submit(id);
        repository.audit(repository.activeUserId(authentication.getName()), "REQUEST_APPROVAL", id,
                "Stock transfer submitted for approval.");
        return response(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockTransferResponse approve(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() == StockTransferStatus.APPROVED || repository.hasApproval(id)) {
            throw conflict("TRANSFER_ALREADY_APPROVED", "This stock transfer has already been approved.");
        }
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);

        List<ItemRow> items = repository.findItems(id);
        validateCurrentItems(header.warehouseId(), items);
        List<StockBalanceService.TransferLine> balanceLines = items.stream().map(item ->
                new StockBalanceService.TransferLine(item.productId(), item.sourceLocationId(),
                        item.destinationLocationId(), item.quantity())).toList();
        List<StockBalanceService.TransferBalanceResult> results = stockBalanceService.moveStock(
                header.warehouseId(), balanceLines);
        for (int index = 0; index < items.size(); index++) {
            ItemRow item = items.get(index);
            StockBalanceService.TransferBalanceResult result = results.get(index);
            stockMovementService.recordTransfer(header.warehouseId(), item.sourceLocationId(), item.productId(),
                    "OUT", item.quantity(), result.sourceAfter(), id, item.id(), reviewerId);
            stockMovementService.recordTransfer(header.warehouseId(), item.destinationLocationId(), item.productId(),
                    "IN", item.quantity(), result.destinationAfter(), id, item.id(), reviewerId);
        }
        repository.approve(id, reviewerId);
        repository.audit(reviewerId, "APPROVE", id, "Stock transfer approved and posted.");
        return response(id);
    }

    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public StockTransferResponse reject(Authentication authentication, UUID id, StockTransferRejectRequest request) {
        Header header = repository.lockHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        String note = request.rejectionNote().trim();
        repository.reject(id, reviewerId, note);
        repository.audit(reviewerId, "REJECT", id, "Stock transfer rejected: " + note);
        return response(id);
    }

    private List<ItemInput> validateItems(UUID warehouseId, List<StockTransferItemRequest> requestedItems) {
        Set<UUID> productIds = new HashSet<>();
        for (StockTransferItemRequest item : requestedItems) {
            if (item.quantity() < 1) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_TRANSFER_QUANTITY",
                        "Transfer quantity must be positive.");
            }
            if (!productIds.add(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "DUPLICATE_TRANSFER_PRODUCT",
                        "A stock transfer can contain only one line for each product.");
            }
            if (item.sourceWarehouseLocationId().equals(item.destinationWarehouseLocationId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "TRANSFER_LOCATIONS_MUST_DIFFER",
                        "Source and destination locations must be different.");
            }
            if (!repository.isActiveLocationInWarehouse(item.sourceWarehouseLocationId(), warehouseId)
                    || !repository.isActiveLocationInWarehouse(item.destinationWarehouseLocationId(), warehouseId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_TRANSFER_LOCATION",
                        "Transfer locations must be active and belong to the transfer warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Choose an existing active product for every transfer item.");
            }
        }
        return requestedItems.stream().map(item -> new ItemInput(item.productId(), item.sourceWarehouseLocationId(),
                item.destinationWarehouseLocationId(), item.quantity(), item.notes())).toList();
    }

    private void validateCurrentItems(UUID warehouseId, List<ItemRow> items) {
        for (ItemRow item : items) {
            if (!repository.isActiveLocationInWarehouse(item.sourceLocationId(), warehouseId)
                    || !repository.isActiveLocationInWarehouse(item.destinationLocationId(), warehouseId)) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_TRANSFER_LOCATION",
                        "Transfer locations must remain active and belong to the transfer warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Transfer products must remain active at approval time.");
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
        return repository.findHeader(id).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "STOCK_TRANSFER_NOT_FOUND", "Stock transfer was not found."));
    }

    private void ensureOwner(Authentication authentication, Header header) {
        if (!repository.activeUserId(authentication.getName()).equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "TRANSFER_OWNER_REQUIRED",
                    "Only the stock transfer creator can change this request.");
        }
    }

    private void ensureEditable(Header header) {
        boolean draft = header.status() == StockTransferStatus.PENDING && header.submittedAt() == null;
        if (!draft && header.status() != StockTransferStatus.REJECTED) {
            throw conflict("TRANSFER_NOT_EDITABLE", "Only an unsubmitted or rejected transfer can be revised.");
        }
    }

    private void ensureSubmittedPending(Header header) {
        if (header.status() != StockTransferStatus.PENDING || header.submittedAt() == null) {
            throw conflict("TRANSFER_NOT_REVIEWABLE", "Only a submitted pending transfer can be reviewed.");
        }
    }

    private void ensureReviewerIsNotCreator(Header header, UUID reviewerId) {
        if (reviewerId.equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "TRANSFER_SELF_REVIEW_FORBIDDEN",
                    "The transfer creator cannot review their own request.");
        }
    }

    private StockTransferResponse response(UUID id) {
        return repository.response(requireHeader(id));
    }

    private String referenceNumber() {
        return "TRF-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
