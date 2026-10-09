package com.warehousing.wmsapi.export.repository;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.export.enums.ExportJobStatus;
import com.warehousing.wmsapi.export.enums.ExportReportType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExportJobRepository {
    private final JdbcTemplate jdbcTemplate;

    public UUID activeUserId(String email) {
        List<UUID> ids = jdbcTemplate.query("SELECT id FROM users WHERE email = ? AND deleted_at IS NULL AND is_active",
                (row, index) -> row.getObject("id", UUID.class), email);
        if (ids.isEmpty()) throw new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Current user was not found.");
        return ids.get(0);
    }

    public String activeUserEmail(UUID id) {
        List<String> emails = jdbcTemplate.query("SELECT email FROM users WHERE id = ? AND deleted_at IS NULL AND is_active",
                (row, index) -> row.getString("email"), id);
        if (emails.isEmpty()) throw notFound();
        return emails.get(0);
    }

    public UUID insert(UUID userId, UUID warehouseId, ExportReportType type, String filters) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO export_jobs(requested_by_user_id, warehouse_id, report_type, filters, status)
                VALUES (?, ?, ?, ?::jsonb, 'QUEUED') RETURNING id
                """, UUID.class, userId, warehouseId, type.name(), filters);
    }

    public Optional<Job> find(UUID id) {
        List<Job> jobs = jdbcTemplate.query(selectById() + " AND j.deleted_at IS NULL",
                (row, index) -> map(row), id);
        return jobs.stream().findFirst();
    }

    public Job require(UUID id) {
        return find(id).orElseThrow(ExportJobRepository::notFound);
    }

    public Job lock(UUID id) {
        List<Job> jobs = jdbcTemplate.query(selectById() + " AND j.deleted_at IS NULL FOR UPDATE",
                (row, index) -> map(row), id);
        if (jobs.isEmpty()) throw notFound();
        return jobs.get(0);
    }

    public void start(UUID id) {
        jdbcTemplate.update("""
                UPDATE export_jobs SET status = 'RUNNING', attempt_count = attempt_count + 1,
                    started_at = COALESCE(started_at, CURRENT_TIMESTAMP), error_message = NULL,
                    updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, id);
    }

    public void retry(UUID id, String safeError) {
        jdbcTemplate.update("UPDATE export_jobs SET status = 'QUEUED', error_message = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                safeError, id);
    }

    public void fail(UUID id, String safeError) {
        jdbcTemplate.update("UPDATE export_jobs SET status = 'FAILED', error_message = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                safeError, id);
    }

    public void output(UUID id, String path) {
        jdbcTemplate.update("UPDATE export_jobs SET output_path = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", path, id);
    }

    public void markEmailSent(UUID id) {
        jdbcTemplate.update("UPDATE export_jobs SET email_sent_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    }

    public void complete(UUID id) {
        jdbcTemplate.update("UPDATE export_jobs SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP, error_message = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    public record Job(UUID id, UUID userId, String requesterEmail, String requesterName, UUID warehouseId,
            ExportReportType reportType, String filtersJson, ExportJobStatus status, String outputPath,
            String errorMessage, int attemptCount, OffsetDateTime emailSentAt, OffsetDateTime createdAt,
            OffsetDateTime completedAt) { }

    private String selectById() {
        return """
                SELECT j.id, j.requested_by_user_id, u.email requester_email, u.name requester_name,
                    j.warehouse_id, j.report_type, j.filters::text filters_json, j.status, j.output_path,
                    j.error_message, j.attempt_count, j.email_sent_at, j.created_at, j.completed_at
                FROM export_jobs j JOIN users u ON u.id = j.requested_by_user_id WHERE j.id = ?
                """;
    }

    private Job map(java.sql.ResultSet row) throws java.sql.SQLException {
        return new Job(row.getObject("id", UUID.class), row.getObject("requested_by_user_id", UUID.class),
                row.getString("requester_email"), row.getString("requester_name"),
                row.getObject("warehouse_id", UUID.class), ExportReportType.valueOf(row.getString("report_type")),
                row.getString("filters_json"), ExportJobStatus.valueOf(row.getString("status")),
                row.getString("output_path"), row.getString("error_message"), row.getInt("attempt_count"),
                row.getObject("email_sent_at", OffsetDateTime.class), row.getObject("created_at", OffsetDateTime.class),
                row.getObject("completed_at", OffsetDateTime.class));
    }

    private static BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "EXPORT_JOB_NOT_FOUND", "Export job was not found.");
    }
}
