package com.warehousing.wmsapi.export.service;

import com.warehousing.wmsapi.export.enums.ExportJobStatus;
import com.warehousing.wmsapi.export.repository.ExportJobRepository;
import com.warehousing.wmsapi.export.repository.ExportJobRepository.Job;
import com.warehousing.wmsapi.warehouse.service.WarehouseService;
import java.nio.file.Path;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class ExportJobProcessor {
    private static final int MAX_ATTEMPTS = 3;
    private final ExportJobRepository repository;
    private final ExportStorageService storageService;
    private final ExportMailService mailService;
    private final WarehouseService warehouseService;

    public ExportJobProcessor(ExportJobRepository repository, ExportStorageService storageService,
            ExportMailService mailService, WarehouseService warehouseService) {
        this.repository = repository;
        this.storageService = storageService;
        this.mailService = mailService;
        this.warehouseService = warehouseService;
    }

    /** Returns true only when the stream entry is terminal and safe to acknowledge. */
    public boolean process(java.util.UUID jobId) {
        Job job = repository.find(jobId).orElse(null);
        if (job == null || job.status() == ExportJobStatus.COMPLETED || job.status() == ExportJobStatus.FAILED) {
            return true;
        }
        repository.start(jobId);
        job = repository.require(jobId);
        try {
            var authentication = new UsernamePasswordAuthenticationToken(job.requesterEmail(), "export-worker", List.of());
            warehouseService.requireAccess(authentication, job.warehouseId());
            Path output = job.outputPath() == null ? storageService.generate(job) : Path.of(job.outputPath());
            if (job.outputPath() == null) repository.output(jobId, output.toString());
            if (job.emailSentAt() == null) {
                mailService.send(job, output);
                repository.markEmailSent(jobId);
            }
            repository.complete(jobId);
            return true;
        } catch (RuntimeException exception) {
            if (job.attemptCount() >= MAX_ATTEMPTS) {
                repository.fail(jobId, "Export failed after " + MAX_ATTEMPTS + " attempts.");
                return true;
            }
            repository.retry(jobId, "Export processing failed. The system will retry automatically.");
            return false;
        }
    }
}
