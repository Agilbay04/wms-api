package com.warehousing.wmsapi.common.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.warehousing.wmsapi.common.error.BusinessException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MasterPageTest {
    private static final Set<String> SORTS = Set.of("code", "name");

    @Test
    void acceptsAllowListedSortAndBoundedSize() {
        var request = MasterPage.of(2, 50, "name", SORTS);
        assertEquals(1, request.getPageNumber());
        assertEquals(50, request.getPageSize());
        assertEquals("name", request.getSort().iterator().next().getProperty());
    }

    @Test
    void mapsFirstPublicPageToFirstJpaPage() {
        assertEquals(0, MasterPage.of(1, 10, "code", SORTS).getPageNumber());
    }

    @Test
    void rejectsInvalidPageSizeAndSort() {
        assertEquals("INVALID_PAGE_REQUEST", assertThrows(BusinessException.class,
                () -> MasterPage.of(1, 10, "deletedAt", SORTS)).getCode());
        assertEquals("INVALID_PAGE_REQUEST", assertThrows(BusinessException.class,
                () -> MasterPage.of(0, 10, "code", SORTS)).getCode());
        assertEquals("INVALID_PAGE_REQUEST", assertThrows(BusinessException.class,
                () -> MasterPage.of(1, 0, "code", SORTS)).getCode());
        assertEquals("INVALID_PAGE_REQUEST", assertThrows(BusinessException.class,
                () -> MasterPage.of(1, 101, "code", SORTS)).getCode());
    }
}
