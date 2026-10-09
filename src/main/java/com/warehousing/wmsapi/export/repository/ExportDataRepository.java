package com.warehousing.wmsapi.export.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExportDataRepository {
    private final JdbcTemplate jdbcTemplate;

    public void streamStocks(UUID warehouseId, Consumer<List<String>> rowConsumer) {
        PreparedStatementCreator statementCreator = connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    SELECT w.code warehouse_code, wl.code location_code, p.sku, p.name product_name, p.unit,
                        wli.quantity, wli.updated_at
                    FROM warehouse_location_items wli JOIN warehouses w ON w.id = wli.warehouse_id
                    JOIN warehouse_locations wl ON wl.id = wli.warehouse_location_id
                    JOIN products p ON p.id = wli.product_id
                    WHERE wli.warehouse_id = ? AND wli.deleted_at IS NULL AND w.deleted_at IS NULL
                        AND wl.deleted_at IS NULL AND p.deleted_at IS NULL
                    ORDER BY wl.code, p.sku
                    """);
            statement.setFetchSize(500);
            statement.setObject(1, warehouseId);
            return statement;
        };
        RowCallbackHandler rowHandler = result -> rowConsumer.accept(values(result));
        jdbcTemplate.query(statementCreator, rowHandler);
    }

    public void streamMovements(UUID warehouseId, LocalDate fromDate, LocalDate toDate,
                                Consumer<List<String>> rowConsumer) {
        PreparedStatementCreator statementCreator = connection -> {
            PreparedStatement statement = connection.prepareStatement("""
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
                    """);
            statement.setFetchSize(500);
            statement.setObject(1, warehouseId);
            statement.setObject(2, fromDate == null ? null : fromDate.atStartOfDay().atOffset(ZoneOffset.UTC));
            statement.setObject(3, toDate == null ? null : toDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            return statement;
        };
        RowCallbackHandler rowHandler = result -> rowConsumer.accept(values(result));
        jdbcTemplate.query(statementCreator, rowHandler);
    }

    private List<String> values(ResultSet row) throws SQLException {
        int count = row.getMetaData().getColumnCount();
        List<String> values = new ArrayList<>(count);
        for (int index = 1; index <= count; index++) {
            Object value = row.getObject(index);
            values.add(value == null ? "" : value.toString());
        }
        return values;
    }
}
