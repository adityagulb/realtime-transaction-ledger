package com.aditya.analytics.consumer;

import com.aditya.analytics.model.TransactionEvent;
import com.aditya.analytics.sink.AnalyticsSink;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.mockito.Mockito.*;

class TransactionEventConsumerTest {
    @Test
    void parsesAndForwardsEvent() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        AnalyticsSink sink = mock(AnalyticsSink.class);
        TransactionEvent event = new TransactionEvent(UUID.randomUUID(), UUID.randomUUID(), "tx1", BigDecimal.TEN, "USD", LocalDate.now(), "ACCEPTED", Instant.now());
        new TransactionEventConsumer(mapper, sink).consume(mapper.writeValueAsString(event));
        verify(sink).write(event);
    }

    @Test
    void shouldDeserializeAndSendEventToSink() throws Exception {

        AnalyticsSink sink = mock(AnalyticsSink.class);

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();

        TransactionEventConsumer consumer =
                new TransactionEventConsumer(
                        mapper,
                        sink
                );

        String json = """
            {
              "eventId":"11111111-1111-1111-1111-111111111111",
              "ledgerId":"22222222-2222-2222-2222-222222222222",
              "transactionId":"txn-1001",
              "amount":100.50,
              "currency":"INR",
              "settlementDate":"2026-09-05",
              "status":"COMPLETED",
              "occurredAt":"2026-09-05T07:30:00Z"
            }
            """;

        consumer.consume(json);

        verify(sink).write(
                argThat(event ->
                        event.transactionId()
                                .equals("txn-1001")
                                &&
                                event.amount()
                                        .compareTo(
                                                new BigDecimal("100.50")
                                        ) == 0
                )
        );
    }
}
