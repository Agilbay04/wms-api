package com.warehousing.wmsapi.common.pagination;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/** Common query parameters for paginated list endpoints. */
@Getter
@Setter
public class BasePageRequest {
    @Schema(description = "One-based page number.", defaultValue = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @NotNull(message = "page is required when provided")
    @Min(value = 1, message = "page must be at least one")
    private Integer page = 1;

    @Schema(description = "Number of records per page (1-100).", defaultValue = "10",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @NotNull(message = "size is required when provided")
    @Min(value = 1, message = "size must be at least one")
    @Max(value = 100, message = "size must not exceed 100")
    private Integer size = 10;

    @Schema(description = "Field used to sort the results.", defaultValue = "created_at",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String sort = "created_at";

    @Schema(description = "Sort direction: asc or desc.", defaultValue = "desc",
            allowableValues = {"asc", "desc"}, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Pattern(regexp = "(?i)^(asc|desc)$", message = "order must be either asc or desc")
    private String order = "desc";

    @Schema(description = "Optional text filter.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100, message = "search must not exceed 100 characters")
    private String search;

    public static BasePageRequest of(int page, int size, String sort, String order, String search) {
        BasePageRequest request = new BasePageRequest();
        request.setPage(page);
        request.setSize(size);
        request.setSort(sort);
        request.setOrder(order);
        request.setSearch(search);
        return request;
    }

}
