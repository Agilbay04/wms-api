package com.warehousing.wmsapi.export.service;

import com.warehousing.wmsapi.config.AppProperties;
import com.warehousing.wmsapi.export.enums.ExportReportType;
import com.warehousing.wmsapi.export.repository.ExportJobRepository.Job;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportStorageService {
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties properties;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    public ExportStorageService(JdbcTemplate jdbcTemplate, AppProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Path generate(Job job) {
        Path root = Path.of(properties.exportDirectory()).toAbsolutePath().normalize();
        Path output = root.resolve(job.id() + ".csv").normalize();
        if (!output.startsWith(root)) throw new IllegalStateException("Export output path is invalid.");
        try {
            Files.createDirectories(root);
            try (BufferedWriter writer = Files.newBufferedWriter(output)) {
                writeRows(job, writer);
            }
            return output;
        } catch (IOException | UncheckedIOException exception) {
            throw new IllegalStateException("Export file generation failed.", exception);
        }
    }

    private void writeRows(Job job, BufferedWriter writer) throws IOException {
        LocalDate fromDate = date(job, "from_date");
        LocalDate toDate = date(job, "to_date");
        String sql;
        if (job.reportType() == ExportReportType.STOCKS) {
            sql = """
                    SELECT w.code warehouse_code, wl.code location_code, p.sku, p.name product_name, p.unit,
                        wli.quantity, wli.updated_at
                    FROM warehouse_location_items wli JOIN warehouses w ON w.id = wli.warehouse_id
                    JOIN warehouse_locations wl ON wl.id = wli.warehouse_location_id
                    JOIN products p ON p.id = wli.product_id
                    WHERE wli.warehouse_id = ? AND wli.deleted_at IS NULL AND w.deleted_at IS NULL
                        AND wl.deleted_at IS NULL AND p.deleted_at IS NULL
                    ORDER BY wl.code, p.sku
                    """;
            writer.write("warehouse_code,location_code,sku,product_name,unit,quantity,updated_at\n");
        } else {
            sql = """
                    SELECT w.code warehouse_code, wl.code location_code, p.sku, p.name product_name,
                        sm.movement_type, sm.movement_direction, sm.quantity, sm.stock_after,
                        sm.source_entity_type, sm.source_entity_id, sm.occurred_at
                    FROM stock_movements sm JOIN warehouses w ON w.id = sm.warehouse_id
                    JOIN warehouse_locations wl ON wl.id = sm.warehouse_location_id
                    JOIN products p ON p.id = sm.product_id
                    WHERE sm.warehouse_id = ? AND sm.deleted_at IS NULL AND w.deleted_at IS NULL
                        AND wl.deleted_at IS NULL AND p.deleted_at IS NULL
                        AND sm.occurred_at >= COALESCE(?::timestamptz, '-infinity'::timestamptz)
                        AND sm.occurred_at < COALESCE(?::timestamptz, 'infinity'::timestamptz)
                    ORDER BY sm.occurred_at, sm.id
                    """;
            writer.write("warehouse_code,location_code,sku,product_name,movement_type,movement_direction,quantity,stock_after,source_entity_type,source_entity_id,occurred_at\n");
        }
        PreparedStatementCreator statementCreator = connection -> prepare(connection, job, sql, fromDate, toDate);
        RowCallbackHandler rowWriter = resultSet -> writeCsvRow(writer, resultSet);
        try {
            jdbcTemplate.query(statementCreator, rowWriter);
        } catch (UncheckedIOException exception) {
            throw exception;
        }
    }

    private PreparedStatement prepare(java.sql.Connection connection, Job job, String sql,
            LocalDate fromDate, LocalDate toDate) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setFetchSize(500);
        statement.setObject(1, job.warehouseId());
        if (job.reportType() == ExportReportType.MOVEMENTS) {
            statement.setObject(2, fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC));
            statement.setObject(3, toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        }
        return statement;
    }

    private LocalDate date(Job job, String field) {
        try {
            String value = JSON_MAPPER.readTree(job.filtersJson()).path(field).asString("");
            return value.isBlank() ? null : LocalDate.parse(value);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Export job filters are invalid.", exception);
        }
    }

    private void writeCsvRow(BufferedWriter writer, ResultSet row) throws SQLException {
        try {
            int columns = row.getMetaData().getColumnCount();
            for (int index = 1; index <= columns; index++) {
                if (index > 1) writer.write(',');
                Object value = row.getObject(index);
                writer.write(escape(value == null ? "" : value.toString()));
            }
            writer.write('\n');
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String escape(String value) {
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")
                ? "\"" + escaped + "\"" : escaped;
    }
}
