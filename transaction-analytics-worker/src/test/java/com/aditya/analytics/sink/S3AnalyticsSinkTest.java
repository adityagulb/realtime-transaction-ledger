package com.aditya.analytics.sink;

import com.aditya.analytics.model.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3AnalyticsSinkTest {

    private S3Client s3Client;
    private ObjectMapper objectMapper;
    private S3AnalyticsSink sink;

    @BeforeEach
    void setUp() {

        s3Client = mock(S3Client.class);

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        sink = new S3AnalyticsSink(
                s3Client,
                objectMapper,
                "test-bucket",
                "banking-transactions",
                2
        );
    }

    @Test
    void shouldUploadWhenBatchSizeReached() {

        TransactionEvent event1 = createEvent("txn-1");
        TransactionEvent event2 = createEvent("txn-2");

        sink.write(event1);

        verifyNoInteractions(s3Client);

        sink.write(event2);

        verify(s3Client, times(1))
                .putObject(
                        any(PutObjectRequest.class),
                        any(RequestBody.class)
                );
    }

    @Test
    void shouldNotUploadBeforeBatchSizeReached() {

        sink.write(createEvent("txn-1"));

        verifyNoInteractions(s3Client);
    }

    @Test
    void shouldFlushPartialBatchOnScheduledFlush() {

        sink.write(createEvent("txn-1"));

        sink.scheduledFlush();

        verify(s3Client, times(1))
                .putObject(
                        any(PutObjectRequest.class),
                        any(RequestBody.class)
                );
    }

    @Test
    void shouldGenerateCorrectS3Request() {

        sink.write(createEvent("txn-1"));
        sink.write(createEvent("txn-2"));

        ArgumentCaptor<PutObjectRequest> captor =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        verify(s3Client)
                .putObject(
                        captor.capture(),
                        any(RequestBody.class)
                );

        PutObjectRequest request = captor.getValue();

        assertEquals("test-bucket", request.bucket());

        assertTrue(
                request.key()
                        .startsWith("banking-transactions/year=")
        );

        assertTrue(
                request.key()
                        .endsWith(".json")
        );

        assertEquals(
                "application/x-ndjson",
                request.contentType()
        );
    }

    @Test
    void shouldRetryBufferedEventsWhenUploadFails() {

        doThrow(new RuntimeException("S3 unavailable"))
                .when(s3Client)
                .putObject(
                        any(PutObjectRequest.class),
                        any(RequestBody.class)
                );

        sink.write(createEvent("txn-1"));

        assertThrows(
                RuntimeException.class,
                () -> sink.write(createEvent("txn-2"))
        );

        reset(s3Client);

        sink.scheduledFlush();

        verify(s3Client, times(1))
                .putObject(
                        any(PutObjectRequest.class),
                        any(RequestBody.class)
                );
    }

    private TransactionEvent createEvent(String transactionId) {

        return new TransactionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                transactionId,
                new BigDecimal("100.00"),
                "INR",
                LocalDate.of(2026, 9, 5),
                "COMPLETED",
                Instant.parse("2026-09-05T07:30:00Z")
        );
    }
}