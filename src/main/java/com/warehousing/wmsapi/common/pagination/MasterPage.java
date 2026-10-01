package com.warehousing.wmsapi.common.pagination;

import com.warehousing.wmsapi.common.error.BusinessException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class MasterPage {
    private MasterPage() {
    }

    public static PageRequest of(int page, int size, String sort, Set<String> allowedSorts) {
        if (page < 1 || size < 1 || size > 100 || !allowedSorts.contains(sort)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST",
                    "Use page >= 1, size 1-100, and a supported sort field.");
        }
        return PageRequest.of(page - 1, size, Sort.by(sort).ascending());
    }
}
