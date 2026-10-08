package com.warehousing.wmsapi.inventory;

import java.util.UUID;
import com.warehousing.wmsapi.common.error.BusinessException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockBalanceService {
    private final StockBalanceRepository repository;

    public StockBalanceService(StockBalanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int addStock(UUID warehouseId, UUID locationId, UUID productId, int quantity) {
        StockBalanceRepository.Balance balance = repository.lockOrCreate(warehouseId, locationId, productId);
        int updatedQuantity = balance.quantity() + quantity;
        repository.updateQuantity(balance.id(), updatedQuantity);
        return updatedQuantity;
    }

    @Transactional
    public int deductStock(UUID locationId, UUID productId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Stock deduction quantity must be positive.");
        }
        StockBalanceRepository.Balance balance = repository.lock(locationId, productId);
        if (balance == null || balance.quantity() < quantity) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "There is not enough stock to complete this operation.");
        }
        int updatedQuantity = balance.quantity() - quantity;
        repository.updateQuantity(balance.id(), updatedQuantity);
        return updatedQuantity;
    }

    @Transactional
    public List<TransferBalanceResult> moveStock(UUID warehouseId, List<TransferLine> lines) {
        List<BalanceKey> keys = new ArrayList<>();
        for (TransferLine line : lines) {
            if (line.sourceLocationId().equals(line.destinationLocationId())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "TRANSFER_LOCATIONS_MUST_DIFFER",
                        "Source and destination locations must be different.");
            }
            if (line.quantity() < 1) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_TRANSFER_QUANTITY",
                        "Transfer quantity must be positive.");
            }
            keys.add(new BalanceKey(line.sourceLocationId(), line.productId()));
            keys.add(new BalanceKey(line.destinationLocationId(), line.productId()));
        }
        Comparator<BalanceKey> stableOrder = Comparator.comparing((BalanceKey key) -> key.locationId())
                .thenComparing(key -> key.productId());
        Map<BalanceKey, StockBalanceRepository.Balance> locked = new HashMap<>();
        for (BalanceKey key : keys.stream().distinct().sorted(stableOrder).toList()) {
            locked.put(key, repository.lockOrCreate(warehouseId, key.locationId(), key.productId()));
        }
        for (TransferLine line : lines) {
            StockBalanceRepository.Balance source = locked.get(new BalanceKey(line.sourceLocationId(), line.productId()));
            if (source.quantity() < line.quantity()) {
                throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                        "There is not enough source stock to complete this transfer.");
            }
        }
        List<TransferBalanceResult> result = new ArrayList<>();
        for (TransferLine line : lines) {
            BalanceKey sourceKey = new BalanceKey(line.sourceLocationId(), line.productId());
            BalanceKey destinationKey = new BalanceKey(line.destinationLocationId(), line.productId());
            StockBalanceRepository.Balance source = locked.get(sourceKey);
            StockBalanceRepository.Balance destination = locked.get(destinationKey);
            int sourceAfter = source.quantity() - line.quantity();
            int destinationAfter = destination.quantity() + line.quantity();
            repository.updateQuantity(source.id(), sourceAfter);
            repository.updateQuantity(destination.id(), destinationAfter);
            locked.put(sourceKey, new StockBalanceRepository.Balance(source.id(), warehouseId,
                    source.locationId(), source.productId(), sourceAfter));
            locked.put(destinationKey, new StockBalanceRepository.Balance(destination.id(), warehouseId,
                    destination.locationId(), destination.productId(), destinationAfter));
            result.add(new TransferBalanceResult(sourceAfter, destinationAfter));
        }
        return result;
    }

    @Transactional
    public List<Integer> applyAdjustments(UUID warehouseId, List<AdjustmentLine> lines) {
        List<BalanceKey> keys = lines.stream()
                .map(line -> new BalanceKey(line.locationId(), line.productId())).distinct()
                .sorted(Comparator.comparing((BalanceKey key) -> key.locationId())
                        .thenComparing(key -> key.productId()))
                .toList();
        Map<BalanceKey, StockBalanceRepository.Balance> locked = new HashMap<>();
        for (BalanceKey key : keys) {
            locked.put(key, repository.lockOrCreate(warehouseId, key.locationId(), key.productId()));
        }
        List<Integer> updatedQuantities = new ArrayList<>();
        for (AdjustmentLine line : lines) {
            if (line.quantityChange() == 0) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_ADJUSTMENT_QUANTITY",
                        "Adjustment quantity change must not be zero.");
            }
            StockBalanceRepository.Balance balance = locked.get(new BalanceKey(line.locationId(), line.productId()));
            long updated = (long) balance.quantity() + line.quantityChange();
            if (updated < 0) {
                throw new BusinessException(HttpStatus.CONFLICT, "NEGATIVE_STOCK_ADJUSTMENT",
                        "An adjustment cannot reduce stock below zero.");
            }
            if (updated > Integer.MAX_VALUE) {
                throw new BusinessException(HttpStatus.CONFLICT, "STOCK_QUANTITY_OVERFLOW",
                        "Adjustment would exceed the supported stock quantity.");
            }
            updatedQuantities.add((int) updated);
        }
        for (int index = 0; index < lines.size(); index++) {
            AdjustmentLine line = lines.get(index);
            BalanceKey key = new BalanceKey(line.locationId(), line.productId());
            StockBalanceRepository.Balance balance = locked.get(key);
            int updated = updatedQuantities.get(index);
            repository.updateQuantity(balance.id(), updated);
            locked.put(key, new StockBalanceRepository.Balance(balance.id(), warehouseId,
                    balance.locationId(), balance.productId(), updated));
        }
        return updatedQuantities;
    }

    public record TransferLine(UUID productId, UUID sourceLocationId, UUID destinationLocationId, int quantity) {
    }

    public record TransferBalanceResult(int sourceAfter, int destinationAfter) {
    }

    public record AdjustmentLine(UUID productId, UUID locationId, int quantityChange) {
    }

    private record BalanceKey(UUID locationId, UUID productId) {
    }
}
