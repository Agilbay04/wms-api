package com.warehousing.wmsapi.export.service;

import com.warehousing.wmsapi.config.AppProperties;
import com.warehousing.wmsapi.export.enums.ExportReportType;
import com.warehousing.wmsapi.export.repository.ExportDataRepository;
import com.warehousing.wmsapi.export.repository.ExportJobRepository.Job;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportStorageService {
    private final ExportDataRepository repository;
    private final AppProperties properties;
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

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
        if (job.reportType() == ExportReportType.STOCKS) {
            writer.write("warehouse_code,location_code,sku,product_name,unit,quantity,updated_at\n");
            repository.streamStocks(job.warehouseId(), row -> writeCsvRow(writer, row));
        } else {
            writer.write("warehouse_code,location_code,sku,product_name,movement_type,movement_direction,quantity,stock_after,source_entity_type,source_entity_id,occurred_at\n");
            repository.streamMovements(job.warehouseId(), fromDate, toDate, row -> writeCsvRow(writer, row));
        }
    }

    private LocalDate date(Job job, String field) {
        try {
            String value = JSON_MAPPER.readTree(job.filtersJson()).path(field).asString("");
            return value.isBlank() ? null : LocalDate.parse(value);
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalStateException("Export job filters are invalid.", exception);
        }
    }

    private void writeCsvRow(BufferedWriter writer, java.util.List<String> values) {
        try {
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) writer.write(',');
                writer.write(escape(values.get(index)));
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
