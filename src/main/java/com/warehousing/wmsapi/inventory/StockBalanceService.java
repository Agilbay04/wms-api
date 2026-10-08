package com.warehousing.wmsapi.inventory;

import java.util.UUID;
import com.warehousing.wmsapi.common.error.BusinessException;
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
}
