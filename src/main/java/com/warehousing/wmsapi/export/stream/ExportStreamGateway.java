package com.warehousing.wmsapi.export.stream;

import java.util.List;
import java.util.UUID;

public interface ExportStreamGateway {
    void ensureConsumerGroup();
    void publish(UUID jobId);
    List<StreamMessage> claimPending(String consumerName, int limit);
    List<StreamMessage> readNew(String consumerName, int limit);
    void acknowledge(String messageId);

    record StreamMessage(String messageId, UUID jobId, long deliveryCount) { }
}
