package com.aditya.analytics.sink;

import com.aditya.analytics.model.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "app.sink",
        havingValue = "s3"
)
public class S3AnalyticsSink implements AnalyticsSink {

    private static final Logger log =
            LoggerFactory.getLogger(S3AnalyticsSink.class);

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'")
                    .withZone(ZoneOffset.UTC);

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    private final String bucket;
    private final String prefix;
    private final int batchSize;

    private final List<TransactionEvent> buffer = new ArrayList<>();

    public S3AnalyticsSink(
            S3Client s3Client,
            ObjectMapper objectMapper,
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.s3.prefix}") String prefix,
            @Value("${app.batch.max-records:500}") int batchSize) {

        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.bucket = bucket;
        this.prefix = prefix;
        this.batchSize = batchSize;
    }

    @Override
    public synchronized void write(TransactionEvent event) {

        buffer.add(event);

        log.debug(
                "Added event {} to S3 batch. Current batch size={}",
                event.eventId(),
                buffer.size()
        );

        if (buffer.size() >= batchSize) {
            flush();
        }
    }

    /**
     * Flush partially filled batches so low traffic does not leave
     * events permanently in memory.
     */
    @Scheduled(
            fixedDelayString = "${app.batch.flush-interval-ms:30000}"
    )
    public synchronized void scheduledFlush() {

        if (!buffer.isEmpty()) {
            log.debug(
                    "Scheduled flush triggered. Batch size={}",
                    buffer.size()
            );

            flush();
        }
    }

    private void flush() {

        if (buffer.isEmpty()) {
            return;
        }

        /*
         * Take a snapshot.
         *
         * We do NOT clear the real buffer until S3 upload succeeds.
         */
        List<TransactionEvent> batch =
                new ArrayList<>(buffer);

        String body = toNdJson(batch);
        String objectKey = buildObjectKey();

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType("application/x-ndjson")
                        .build();

        try {

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(
                            body.getBytes(StandardCharsets.UTF_8)
                    )
            );

            /*
             * Remove exactly the events successfully persisted.
             */
            buffer.subList(0, batch.size()).clear();

            log.info(
                    "Uploaded {} transaction events to s3://{}/{}",
                    batch.size(),
                    bucket,
                    objectKey
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Failed to upload transaction batch to S3. " +
                            "Batch remains in memory for retry. size={}",
                    batch.size(),
                    ex
            );

            throw ex;
        }
    }

    private String toNdJson(
            List<TransactionEvent> events) {

        StringBuilder builder = new StringBuilder();

        for (TransactionEvent event : events) {

            try {

                builder.append(
                        objectMapper.writeValueAsString(event)
                );

                builder.append('\n');

            } catch (JsonProcessingException ex) {

                throw new IllegalStateException(
                        "Unable to serialize transaction event "
                                + event.eventId(),
                        ex
                );
            }
        }

        return builder.toString();
    }

    private String buildObjectKey() {

        Instant now = Instant.now(Clock.systemUTC());

        String year =
                String.format("%04d",
                        now.atZone(ZoneOffset.UTC).getYear());

        String month =
                String.format("%02d",
                        now.atZone(ZoneOffset.UTC).getMonthValue());

        String day =
                String.format("%02d",
                        now.atZone(ZoneOffset.UTC).getDayOfMonth());

        String hour =
                String.format("%02d",
                        now.atZone(ZoneOffset.UTC).getHour());

        String timestamp =
                FILE_DATE_FORMAT.format(now);

        return String.format(
                "%s/year=%s/month=%s/day=%s/hour=%s/" +
                        "transactions-%s-%s.json",
                normalizePrefix(prefix),
                year,
                month,
                day,
                hour,
                timestamp,
                UUID.randomUUID()
        );
    }

    private String normalizePrefix(String value) {

        if (value.endsWith("/")) {
            return value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value;
    }
}