package com.warehousing.wmsapi.common.pagination;

import com.warehousing.wmsapi.common.error.BusinessException;
import java.util.Set;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class MasterPage {
    private MasterPage() {
    }

    public static PageRequest of(int page, int size, String sort, Set<String> allowedSorts) {
        return of(page, size, sort, "asc", allowedSorts);
    }

    public static PageRequest of(int page, int size, String sort, String order, Set<String> allowedSorts) {
        String property = "created_at".equals(sort) ? "createdAt" : sort;
        if (page < 1 || size < 1 || size > 100 || !allowedSorts.contains(property)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported sort field.");
        }
        return PageRequest.of(page - 1, size, Sort.by(parseDirection(order), property));
    }

    public static Sort.Direction parseDirection(String order) {
        if (order == null) {
            throw invalidOrder();
        }
        return switch (order.toLowerCase(Locale.ROOT)) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw invalidOrder();
        };
    }

    public static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private static BusinessException invalidOrder() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                "Order must be either 'asc' or 'desc'.");
    }
}
