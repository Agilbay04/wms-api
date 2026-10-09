package com.warehousing.wmsapi.export.job;

import com.warehousing.wmsapi.export.service.ExportJobProcessor;
import com.warehousing.wmsapi.export.stream.ExportStreamGateway;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExportStreamConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportStreamConsumer.class);
    private static final int BATCH_SIZE = 10;
    private final String consumerName = "worker-" + UUID.randomUUID();
    private final ExportStreamGateway streamGateway;
    private final ExportJobProcessor processor;

    @Scheduled(fixedDelayString = "${app.export-poll-delay:1000}")
    public void poll() {
        try {
            streamGateway.ensureConsumerGroup();
            process(streamGateway.claimPending(consumerName, BATCH_SIZE));
            process(streamGateway.readNew(consumerName, BATCH_SIZE));
        } catch (RuntimeException exception) {
            LOGGER.warn("Export stream polling failed; the next scheduled poll will retry.", exception);
        }
    }

    private void process(List<ExportStreamGateway.StreamMessage> messages) {
        for (ExportStreamGateway.StreamMessage message : messages) {
            try {
                if (processor.process(message.jobId())) streamGateway.acknowledge(message.messageId());
            } catch (RuntimeException exception) {
                LOGGER.warn("Export stream message remains pending for retry.");
            }
        }
    }
}
