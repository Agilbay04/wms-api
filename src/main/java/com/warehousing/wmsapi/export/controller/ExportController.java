package com.warehousing.wmsapi.export.controller;

import com.warehousing.wmsapi.common.api.ApiResponse;
import com.warehousing.wmsapi.config.AppProperties;
import com.warehousing.wmsapi.export.dto.ExportCreateRequest;
import com.warehousing.wmsapi.export.dto.ExportJobResponse;
import com.warehousing.wmsapi.export.service.ExportJobService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Report exports", description = "Queue report exports, inspect their status, and download completed CSV files.")
@RequestMapping("/api/v1/reports/exports")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {
    private final ExportJobService service;
    private final AppProperties properties;

    public ExportController(ExportJobService service, AppProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'EXPORT')")
    public ResponseEntity<ApiResponse<ExportJobResponse>> create(Authentication authentication,
            @Valid @RequestBody ExportCreateRequest request) {
        ExportJobResponse job = service.create(authentication, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(HttpStatus.ACCEPTED, "Report export queued.", job));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'EXPORT')")
    public ApiResponse<ExportJobResponse> get(Authentication authentication, @PathVariable UUID id) {
        return ApiResponse.success(HttpStatus.OK, "Export job retrieved.", service.get(authentication, id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@permissionService.hasPermission(authentication, 'REPORTS', 'EXPORT')")
    public ResponseEntity<Resource> download(Authentication authentication, @PathVariable UUID id) {
        Path file = service.download(authentication, id, Path.of(properties.exportDirectory()));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(new FileSystemResource(file));
    }
}
