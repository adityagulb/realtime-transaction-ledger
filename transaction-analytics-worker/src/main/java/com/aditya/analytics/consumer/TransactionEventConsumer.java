package com.aditya.analytics.consumer;

import com.aditya.analytics.model.TransactionEvent;
import com.aditya.analytics.sink.AnalyticsSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {
    private final ObjectMapper mapper;
    private final AnalyticsSink sink;

    public TransactionEventConsumer(ObjectMapper mapper, AnalyticsSink sink) {
        this.mapper = mapper;
        this.sink = sink;
    }

    @KafkaListener(topics = "${app.kafka.topic:transaction-events}", groupId = "${app.kafka.group:transaction-analytics}")
    public void consume(String json) throws Exception {
        TransactionEvent e = mapper.readValue(json, TransactionEvent.class);
        sink.write(e);
    }
}
