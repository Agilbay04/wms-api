package com.warehousing.wmsapi.inbound.service;

import com.warehousing.wmsapi.common.api.PageResponse;
import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.common.pagination.BasePageRequest;
import com.warehousing.wmsapi.inbound.dto.InboundCreateRequest;
import com.warehousing.wmsapi.inbound.dto.InboundItemRequest;
import com.warehousing.wmsapi.inbound.dto.InboundListItemResponse;
import com.warehousing.wmsapi.inbound.dto.InboundPutawayItemRequest;
import com.warehousing.wmsapi.inbound.dto.InboundPutawayRequest;
import com.warehousing.wmsapi.inbound.dto.InboundRejectRequest;
import com.warehousing.wmsapi.inbound.dto.InboundResponse;
import com.warehousing.wmsapi.inbound.dto.InboundUpdateRequest;
import com.warehousing.wmsapi.inbound.entity.InboundStatus;
import com.warehousing.wmsapi.inbound.repository.InboundRepository;
import com.warehousing.wmsapi.inbound.repository.InboundRepository.Header;
import com.warehousing.wmsapi.inbound.repository.InboundRepository.InboundItemInput;
import com.warehousing.wmsapi.inbound.repository.InboundRepository.InboundItemRow;
import com.warehousing.wmsapi.inventory.StockBalanceService;
import com.warehousing.wmsapi.inventory.StockMovementService;
import com.warehousing.wmsapi.warehouse.entity.WarehouseEntity;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundServiceImpl implements InboundService {
    private final InboundRepository repository;
    private final WarehouseService warehouseService;
    private final StockBalanceService stockBalanceService;
    private final StockMovementService stockMovementService;

    public InboundServiceImpl(InboundRepository repository, WarehouseService warehouseService,
                              StockBalanceService stockBalanceService,
                              StockMovementService stockMovementService) {
        this.repository = repository;
        this.warehouseService = warehouseService;
        this.stockBalanceService = stockBalanceService;
        this.stockMovementService = stockMovementService;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public InboundResponse create(Authentication authentication, InboundCreateRequest request) {
        activeWarehouse(authentication, request.warehouseId());
        List<InboundItemInput> items = validateItems(request.items());
        UUID userId = repository.activeUserId(authentication.getName());
        UUID id = repository.insert(request.warehouseId(), newReferenceNumber(), request.purchaseOrderNumber(),
                request.notes(), userId);
        repository.insertItems(id, items);
        repository.audit(userId, "CREATE", id, "Inbound draft created.");
        return response(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundListItemResponse> list(Authentication authentication, BasePageRequest request) {
        return repository.list(authentication.getName(), request);
    }

    @Override
    @Transactional(readOnly = true)
    public InboundResponse get(Authentication authentication, UUID id) {
        Header header = requireHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        return repository.response(header);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public InboundResponse update(Authentication authentication, UUID id, InboundUpdateRequest request) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        ensureEditable(header);
        List<InboundItemInput> items = validateItems(request.items());
        repository.replaceItems(id, items);
        repository.updateHeader(id, request.purchaseOrderNumber(), request.notes());
        repository.audit(repository.activeUserId(authentication.getName()), "UPDATE", id,
                "Inbound draft updated.");
        return response(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public void delete(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != InboundStatus.PENDING || header.submittedAt() != null) {
            throw conflict("INBOUND_NOT_DRAFT", "Only an unsubmitted inbound draft can be deleted.");
        }
        repository.softDelete(id);
        repository.audit(repository.activeUserId(authentication.getName()), "DELETE", id,
                "Inbound draft deleted.");
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public InboundResponse submit(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        ensureOwner(authentication, header);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != InboundStatus.PENDING || header.submittedAt() != null) {
            throw conflict("INBOUND_NOT_DRAFT", "Only a pending draft can be submitted.");
        }
        if (repository.itemCount(id) == 0) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INBOUND_ITEMS_REQUIRED",
                    "Add at least one item before submitting the inbound.");
        }
        repository.submit(id);
        repository.audit(repository.activeUserId(authentication.getName()), "REQUEST_APPROVAL", id,
                "Inbound submitted for approval.");
        return response(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public InboundResponse approve(Authentication authentication, UUID id) {
        Header header = repository.lockHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        repository.approve(id, reviewerId);
        repository.audit(reviewerId, "APPROVE", id, "Inbound approved.");
        return response(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public InboundResponse reject(Authentication authentication, UUID id, InboundRejectRequest request) {
        Header header = repository.lockHeader(id);
        warehouseService.requireAccess(authentication, header.warehouseId());
        ensureSubmittedPending(header);
        UUID reviewerId = repository.activeUserId(authentication.getName());
        ensureReviewerIsNotCreator(header, reviewerId);
        repository.reject(id, reviewerId, request.rejectionNote().trim());
        repository.audit(reviewerId, "REJECT", id, "Inbound rejected: " + request.rejectionNote().trim());
        return response(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "dashboards", allEntries = true)
    public InboundResponse putaway(Authentication authentication, UUID id, InboundPutawayRequest request) {
        Header header = repository.lockHeader(id);
        activeWarehouse(authentication, header.warehouseId());
        if (header.status() != InboundStatus.APPROVED) {
            throw conflict("INBOUND_NOT_APPROVED", "Only an approved inbound can be put away.");
        }
        if (repository.hasPutaway(id)) {
            throw conflict("INBOUND_ALREADY_PUT_AWAY", "This inbound has already been put away.");
        }

        UUID userId = repository.activeUserId(authentication.getName());
        List<InboundItemRow> inboundItems = repository.findItems(id);
        Map<UUID, UUID> locationByItem = locationsByItem(request.items());
        Set<UUID> requiredIds = inboundItems.stream()
                .map(item -> item.id())
                .collect(java.util.stream.Collectors.toSet());
        if (requiredIds.isEmpty() || !requiredIds.equals(locationByItem.keySet())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PUTAWAY_ITEMS",
                    "Provide exactly one destination location for every inbound item.");
        }

        for (InboundItemRow item : inboundItems) {
            UUID locationId = locationByItem.get(item.id());
            if (!repository.isActiveLocationInWarehouse(locationId, header.warehouseId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PUTAWAY_LOCATION",
                        "Every putaway location must be active and belong to the inbound warehouse.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INACTIVE_PRODUCT",
                        "Inbound items must reference active products when put away.");
            }
            int stockAfter = stockBalanceService.addStock(header.warehouseId(), locationId, item.productId(),
                    item.quantity());
            stockMovementService.recordInbound(header.warehouseId(), locationId, item.productId(), item.quantity(),
                    stockAfter, id, item.id(), userId);
        }
        repository.audit(userId, "UPDATE", id, "Inbound putaway completed.");
        return response(id);
    }

    private List<InboundItemInput> validateItems(List<InboundItemRequest> requestedItems) {
        Set<UUID> productIds = new HashSet<>();
        for (InboundItemRequest item : requestedItems) {
            if (!productIds.add(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "DUPLICATE_INBOUND_PRODUCT",
                        "An inbound can contain only one line for each product.");
            }
            if (!repository.isActiveProduct(item.productId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_PRODUCT",
                        "Choose an existing active product for every inbound item.");
            }
        }
        return requestedItems.stream().map(item -> new InboundItemInput(
                item.productId(), item.quantity(), item.notes())).toList();
    }

    private Map<UUID, UUID> locationsByItem(List<InboundPutawayItemRequest> requestedItems) {
        Map<UUID, UUID> locations = new HashMap<>();
        for (InboundPutawayItemRequest item : requestedItems) {
            if (locations.putIfAbsent(item.inboundItemId(), item.warehouseLocationId()) != null) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "DUPLICATE_PUTAWAY_ITEM",
                        "Each inbound item can be assigned only one putaway location.");
            }
        }
        return locations;
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
                new BusinessException(HttpStatus.NOT_FOUND, "INBOUND_NOT_FOUND", "Inbound was not found."));
    }

    private void ensureOwner(Authentication authentication, Header header) {
        if (!repository.activeUserId(authentication.getName()).equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "INBOUND_OWNER_REQUIRED",
                    "Only the inbound creator can change this request.");
        }
    }

    private void ensureEditable(Header header) {
        boolean draft = header.status() == InboundStatus.PENDING && header.submittedAt() == null;
        if (!draft && header.status() != InboundStatus.REJECTED) {
            throw conflict("INBOUND_NOT_EDITABLE", "Only an unsubmitted or rejected inbound can be revised.");
        }
    }

    private void ensureSubmittedPending(Header header) {
        if (header.status() != InboundStatus.PENDING || header.submittedAt() == null) {
            throw conflict("INBOUND_NOT_REVIEWABLE", "Only a submitted pending inbound can be reviewed.");
        }
    }

    private void ensureReviewerIsNotCreator(Header header, UUID reviewerId) {
        if (reviewerId.equals(header.createdByUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "INBOUND_SELF_REVIEW_FORBIDDEN",
                    "The inbound creator cannot review their own request.");
        }
    }

    private InboundResponse response(UUID id) {
        return repository.response(repository.findHeader(id).orElseThrow(() ->
                new BusinessException(HttpStatus.NOT_FOUND, "INBOUND_NOT_FOUND", "Inbound was not found.")));
    }

    private String newReferenceNumber() {
        return "INB-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
