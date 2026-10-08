package com.warehousing.wmsapi.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.warehousing.wmsapi.common.error.BusinessException;
import com.warehousing.wmsapi.export.enums.ExportJobStatus;
import com.warehousing.wmsapi.export.enums.ExportReportType;
import com.warehousing.wmsapi.export.job.ExportStreamConsumer;
import com.warehousing.wmsapi.export.dto.ExportCreateRequest;
import com.warehousing.wmsapi.export.dto.ExportJobResponse;
import com.warehousing.wmsapi.export.repository.ExportJobRepository;
import com.warehousing.wmsapi.export.service.ExportJobService;
import com.warehousing.wmsapi.export.stream.ExportStreamGateway;
import com.warehousing.wmsapi.persistence.IntegrationTestDatabase;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@EnabledIf("isTestDatabaseConfigured")
class ExportJobIntegrationTest {
    private static final String SCHEMA = IntegrationTestDatabase.newSchema("wms_export_it_");
    private static final Path EXPORT_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"), SCHEMA);

    @Autowired private ExportJobService exportJobService;
    @Autowired private ExportJobRepository exportJobRepository;
    @Autowired private ExportStreamConsumer consumer;
    @Autowired private FakeStreamGateway streamGateway;
    @Autowired private FakeMailSender mailSender;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UUID warehouseId;
    private Authentication requester;
    private String requesterEmail;

    public static boolean isTestDatabaseConfigured() { return IntegrationTestDatabase.isConfigured(); }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        IntegrationTestDatabase.configure(registry, SCHEMA);
        registry.add("app.export-directory", EXPORT_DIRECTORY::toString);
        registry.add("app.export-poll-delay", () -> "3600000");
    }

    @AfterAll
    static void cleanSchema() throws Exception {
        IntegrationTestDatabase.dropSchema(SCHEMA);
        if (Files.exists(EXPORT_DIRECTORY)) {
            try (var paths = Files.walk(EXPORT_DIRECTORY)) {
                for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @BeforeEach
    void setUp() {
        streamGateway.reset();
        mailSender.reset();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        requesterEmail = "export-user-" + suffix + "@integration.test";
        UUID userId = jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, requesterEmail, "Export user");
        requester = authentication(requesterEmail);
        warehouseId = jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "EXPORT-WH-" + suffix, "Export warehouse");
        jdbcTemplate.update("INSERT INTO user_warehouses(user_id, warehouse_id) VALUES (?, ?)", userId, warehouseId);
        UUID categoryId = jdbcTemplate.queryForObject("INSERT INTO product_categories(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "EXPORT-CAT-" + suffix, "Export category");
        UUID productId = jdbcTemplate.queryForObject("INSERT INTO products(product_category_id, sku, name, unit) VALUES (?, ?, ?, 'pcs') RETURNING id",
                UUID.class, categoryId, "EXPORT-SKU-" + suffix, "Export product");
        UUID locationId = jdbcTemplate.queryForObject("INSERT INTO warehouse_locations(warehouse_id, code, name) VALUES (?, ?, ?) RETURNING id",
                UUID.class, warehouseId, "EXPORT-LOC-" + suffix, "Export location");
        jdbcTemplate.update("INSERT INTO warehouse_location_items(warehouse_id, warehouse_location_id, product_id, quantity) VALUES (?, ?, ?, 9)",
                warehouseId, locationId, productId);
    }

    @Test
    void queuedJobIsProcessedToCompletionAndOnlyItsRequesterCanReadOrDownloadIt() throws Exception {
        ExportJobResponse queued = exportJobService.create(requester,
                new ExportCreateRequest(warehouseId, ExportReportType.STOCKS, null, null));
        assertEquals(ExportJobStatus.QUEUED, queued.status());
        assertEquals(1, streamGateway.queuedCount());

        UUID secondUserId = jdbcTemplate.queryForObject("INSERT INTO users(email, name, password) VALUES (?, ?, 'test') RETURNING id",
                UUID.class, "other-" + UUID.randomUUID() + "@integration.test", "Other export user");
        Authentication otherUser = authentication(jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, secondUserId));
        BusinessException hidden = assertThrows(BusinessException.class, () -> exportJobService.get(otherUser, queued.id()));
        assertEquals("EXPORT_JOB_NOT_FOUND", hidden.getCode());

        consumer.poll();
        ExportJobResponse completed = exportJobService.get(requester, queued.id());
        assertEquals(ExportJobStatus.COMPLETED, completed.status());
        assertEquals(1, mailSender.sentCount());
        assertEquals(1, streamGateway.acknowledgedCount());
        Path file = exportJobService.download(requester, queued.id(), EXPORT_DIRECTORY);
        assertTrue(Files.readString(file).contains("EXPORT-SKU-"));
        assertThrows(BusinessException.class, () -> exportJobService.download(otherUser, queued.id(), EXPORT_DIRECTORY));
    }

    @Test
    void failedDeliveryRetriesThreeTimesThenStoresSafeFailureAndAcknowledges() {
        mailSender.failNext(3);
        ExportJobResponse queued = exportJobService.create(requester,
                new ExportCreateRequest(warehouseId, ExportReportType.STOCKS, null, null));

        consumer.poll();
        ExportJobResponse retrying = exportJobService.get(requester, queued.id());
        assertEquals(ExportJobStatus.QUEUED, retrying.status());
        assertEquals(1, retrying.attemptCount());
        assertEquals(0, streamGateway.acknowledgedCount());

        consumer.poll();
        consumer.poll();
        ExportJobResponse failed = exportJobService.get(requester, queued.id());
        assertEquals(ExportJobStatus.FAILED, failed.status());
        assertEquals(3, failed.attemptCount());
        assertEquals("Export failed after 3 attempts.", failed.errorMessage());
        assertEquals(1, streamGateway.acknowledgedCount());
        assertFalse(failed.errorMessage().contains("localhost"));
    }

    @Test
    void exportRequiresWarehouseAccessAndRunningStateIsVisibleToOwner() {
        UUID otherWarehouse = jdbcTemplate.queryForObject("INSERT INTO warehouses(code, name) VALUES (?, ?) RETURNING id",
                UUID.class, "EXPORT-DENIED-" + UUID.randomUUID(), "Not assigned");
        BusinessException forbidden = assertThrows(BusinessException.class, () -> exportJobService.create(requester,
                new ExportCreateRequest(otherWarehouse, ExportReportType.MOVEMENTS, null, null)));
        assertEquals("WAREHOUSE_FORBIDDEN", forbidden.getCode());

        ExportJobResponse queued = exportJobService.create(requester,
                new ExportCreateRequest(warehouseId, ExportReportType.MOVEMENTS, null, null));
        exportJobRepository.start(queued.id());
        assertEquals(ExportJobStatus.RUNNING, exportJobService.get(requester, queued.id()).status());
    }

    private static Authentication authentication(String email) {
        return new UsernamePasswordAuthenticationToken(email, "integration-test", List.of());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ExportTestConfiguration {
        @Bean @Primary FakeStreamGateway fakeStreamGateway() { return new FakeStreamGateway(); }
        @Bean @Primary FakeMailSender fakeMailSender() { return new FakeMailSender(); }
    }

    static class FakeStreamGateway implements ExportStreamGateway {
        private final ArrayDeque<StreamMessage> newMessages = new ArrayDeque<>();
        private final List<StreamMessage> pending = new ArrayList<>();
        private int sequence;
        private int acknowledgements;

        @Override public void ensureConsumerGroup() { }

        @Override public synchronized void publish(UUID jobId) {
            newMessages.add(new StreamMessage("message-" + ++sequence, jobId, 1));
        }

        @Override public synchronized List<StreamMessage> claimPending(String consumerName, int limit) {
            List<StreamMessage> claimed = new ArrayList<>();
            for (StreamMessage message : pending) {
                if (claimed.size() == limit) break;
                claimed.add(new StreamMessage(message.messageId(), message.jobId(), message.deliveryCount() + 1));
            }
            return claimed;
        }

        @Override public synchronized List<StreamMessage> readNew(String consumerName, int limit) {
            List<StreamMessage> read = new ArrayList<>();
            while (!newMessages.isEmpty() && read.size() < limit) {
                StreamMessage message = newMessages.removeFirst();
                pending.add(message);
                read.add(message);
            }
            return read;
        }

        @Override public synchronized void acknowledge(String messageId) {
            pending.removeIf(message -> message.messageId().equals(messageId));
            acknowledgements++;
        }

        synchronized int queuedCount() { return newMessages.size(); }
        synchronized int acknowledgedCount() { return acknowledgements; }

        synchronized void reset() {
            newMessages.clear(); pending.clear(); sequence = 0; acknowledgements = 0;
        }
    }

    static class FakeMailSender implements JavaMailSender {
        private final AtomicInteger failuresRemaining = new AtomicInteger();
        private final AtomicInteger sends = new AtomicInteger();

        @Override public MimeMessage createMimeMessage() {
            return new MimeMessage(Session.getInstance(new Properties()));
        }
        @Override public MimeMessage createMimeMessage(InputStream inputStream) {
            throw new UnsupportedOperationException("MIME parsing is not used in this fake sender.");
        }
        @Override public void send(MimeMessage... messages) {
            sends.addAndGet(messages.length);
            if (failuresRemaining.getAndUpdate(value -> Math.max(0, value - messages.length)) > 0) {
                throw new MailSendException("Fake Mailpit failure");
            }
        }
        @Override public void send(SimpleMailMessage... messages) {
            throw new UnsupportedOperationException("Simple messages are not used in this fake sender.");
        }
        int sentCount() { return sends.get(); }
        void failNext(int count) { failuresRemaining.set(count); }
        void reset() { failuresRemaining.set(0); sends.set(0); }
    }
}
