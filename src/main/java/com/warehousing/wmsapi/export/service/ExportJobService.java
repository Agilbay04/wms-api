package com.warehousing.wmsapi.export.service;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.export.enums.ExportJobStatus;
import com.warehousing.wmsapi.export.dto.ExportCreateRequest;
import com.warehousing.wmsapi.export.dto.ExportJobResponse;
import com.warehousing.wmsapi.export.repository.ExportJobRepository;
import com.warehousing.wmsapi.export.repository.ExportJobRepository.Job;
import com.warehousing.wmsapi.export.stream.ExportStreamGateway;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ExportJobService {
    private final ExportJobRepository repository;
    private final ExportStreamGateway streamGateway;
    private final WarehouseService warehouseService;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Transactional
    public ExportJobResponse create(Authentication authentication, ExportCreateRequest request) {
        validateDates(request.fromDate(), request.toDate());
        warehouseService.requireAccess(authentication, request.warehouseId());
        UUID requesterId = repository.activeUserId(authentication.getName());
        UUID id = repository.insert(requesterId, request.warehouseId(), request.reportType(), filters(request));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    streamGateway.publish(id);
                } catch (RuntimeException exception) {
                    repository.fail(id, "Export could not be queued.");
                }
            }
        });
        return response(repository.require(id));
    }

    @Transactional(readOnly = true)
    public ExportJobResponse get(Authentication authentication, UUID id) {
        Job job = ownedJob(authentication, id);
        return response(job);
    }

    @Transactional(readOnly = true)
    public Path download(Authentication authentication, UUID id, Path exportRoot) {
        Job job = ownedJob(authentication, id);
        if (job.status() != ExportJobStatus.COMPLETED || job.outputPath() == null || job.emailSentAt() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "EXPORT_NOT_READY",
                    "The export file is available after its email has been sent.");
        }
        Path root = exportRoot.toAbsolutePath().normalize();
        Path file = Path.of(job.outputPath()).toAbsolutePath().normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "EXPORT_FILE_NOT_FOUND", "Export file was not found.");
        }
        return file;
    }

    private Job ownedJob(Authentication authentication, UUID id) {
        Job job = repository.find(id).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "EXPORT_JOB_NOT_FOUND", "Export job was not found."));
        UUID requester = repository.activeUserId(authentication.getName());
        if (!requester.equals(job.userId())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "EXPORT_JOB_NOT_FOUND", "Export job was not found.");
        }
        return job;
    }

    private ExportJobResponse response(Job job) {
        return new ExportJobResponse(job.id(), job.warehouseId(), job.reportType(), job.status(),
                job.errorMessage(), job.attemptCount(), job.createdAt(), job.completedAt());
    }

    private String filters(ExportCreateRequest request) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("from_date", request.fromDate());
        values.put("to_date", request.toDate());
        try {
            return JSON_MAPPER.writeValueAsString(values);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Export filters could not be serialized.", exception);
        }
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE",
                    "from_date must be earlier than or equal to to_date.");
        }
    }
}
