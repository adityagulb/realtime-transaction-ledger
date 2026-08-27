package com.aditya.processor.outbox;

import com.aditya.processor.domain.OutboxStatus;
import com.aditya.processor.domain.OutboxEvent;
import com.aditya.processor.repo.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OutboxPollerTest {
    @Test
    void marksPublishedAfterKafkaAck() throws Exception {
        OutboxEventRepository repo = mock(OutboxEventRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        OutboxEvent e = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "TX", "{}");
        when(repo.lockNextBatch(10)).thenReturn(List.of(e));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        new OutboxPoller(repo, kafka, "transaction-events", 10).publish();
        assertEquals(OutboxStatus.PUBLISHED, e.getStatus());
    }
}
