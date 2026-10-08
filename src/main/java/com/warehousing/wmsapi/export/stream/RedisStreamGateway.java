package com.warehousing.wmsapi.export.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamGateway implements ExportStreamGateway {
    public static final String STREAM = "wms:report-exports";
    public static final String GROUP = "wms-report-export-workers";
    private static final Duration CLAIM_IDLE = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public RedisStreamGateway(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void ensureConsumerGroup() {
        try {
            operations().createGroup(STREAM, ReadOffset.from("0-0"), GROUP);
        } catch (DataAccessException exception) {
            if (!isExistingGroupError(exception)) throw exception;
        }
    }

    private boolean isExistingGroupError(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("BUSYGROUP")) return true;
            cause = cause.getCause();
        }
        return false;
    }

    @Override
    public void publish(UUID jobId) {
        operations().add(STREAM, Map.of("job_id", jobId.toString()));
    }

    @Override
    public List<StreamMessage> claimPending(String consumerName, int limit) {
        StreamOperations<String, String, String> operations = operations();
        PendingMessages pending = operations.pending(STREAM, GROUP, Range.unbounded(), limit,
                CLAIM_IDLE);
        List<RecordId> ids = new ArrayList<>();
        for (PendingMessage message : pending) ids.add(message.getId());
        if (ids.isEmpty()) return List.of();
        RecordId[] recordIds = ids.toArray(new RecordId[0]);
        List<MapRecord<String, String, String>> claimed = operations
                .claim(STREAM, GROUP, consumerName, CLAIM_IDLE, recordIds);
        List<StreamMessage> result = new ArrayList<>();
        for (MapRecord<String, String, String> record : claimed) {
            result.add(message(record, 1));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<StreamMessage> readNew(String consumerName, int limit) {
        List<MapRecord<String, String, String>> records = operations().read(
                Consumer.from(GROUP, consumerName), StreamReadOptions.empty().count(limit),
                StreamOffset.create(STREAM, ReadOffset.lastConsumed()));
        if (records == null || records.isEmpty()) return List.of();
        List<StreamMessage> messages = new ArrayList<>();
        for (MapRecord<String, String, String> record : records) messages.add(message(record, 1));
        return messages;
    }

    @Override
    public void acknowledge(String messageId) {
        operations().acknowledge(STREAM, GROUP, RecordId.of(messageId));
    }

    private StreamMessage message(MapRecord<String, String, String> record, long deliveryCount) {
        String id = record.getValue().get("job_id");
        if (id == null) throw new IllegalStateException("Export stream entry does not contain a job ID.");
        return new StreamMessage(record.getId().getValue(), UUID.fromString(id), deliveryCount);
    }

    private StreamOperations<String, String, String> operations() {
        return redisTemplate.<String, String>opsForStream();
    }
}
