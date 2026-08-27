package com.aditya.processor.outbox;

import com.aditya.processor.domain.OutboxEvent;
import com.aditya.processor.repo.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPoller {
    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final int batchSize;

    public OutboxPoller(OutboxEventRepository repo, KafkaTemplate<String, String> kafka, @Value("${app.kafka.topic:transaction-events}") String topic, @Value("${app.outbox.batch-size:100}") int batchSize) {
        this.repo = repo;
        this.kafka = kafka;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:2000}")
    @Transactional
    public void publish() {
        List<OutboxEvent> events = repo.lockNextBatch(batchSize);
        for (OutboxEvent event : events) {
            try {
                kafka.send(topic, event.getAggregateId().toString(), event.getPayload()).get();
                event.markPublished();
            } catch (Exception e) {
                event.markRetry();
            }
        }
    }
}
