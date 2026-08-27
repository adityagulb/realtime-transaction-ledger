package com.aditya.analytics.model;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record TransactionEvent(UUID eventId, UUID ledgerId, String transactionId, BigDecimal amount, String currency,
                               LocalDate settlementDate, String status, Instant occurredAt) {
}
