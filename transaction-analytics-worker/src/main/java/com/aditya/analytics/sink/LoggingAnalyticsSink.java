package com.aditya.analytics.sink;

import com.aditya.analytics.model.TransactionEvent;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sink", havingValue = "log", matchIfMissing = true)
public class LoggingAnalyticsSink implements AnalyticsSink {
    private static final Logger log = LoggerFactory.getLogger(LoggingAnalyticsSink.class);

    public void write(TransactionEvent e) {
        log.info("Analytics event {} for transaction {}", e.eventId(), e.transactionId());
    }
}
