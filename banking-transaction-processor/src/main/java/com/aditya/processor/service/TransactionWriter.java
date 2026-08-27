package com.aditya.processor.service;

import com.aditya.processor.dto.TransactionRequest;
import com.aditya.processor.dto.TransactionResponse;
import com.aditya.processor.domain.LedgerTransaction;
import com.aditya.processor.domain.OutboxEvent;
import com.aditya.processor.repo.DeduplicationKeyRepository;
import com.aditya.processor.repo.LedgerTransactionRepository;
import com.aditya.processor.repo.OutboxEventRepository;
import com.aditya.processor.validation.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class TransactionWriter {
    private final LedgerTransactionRepository ledger;
    private final OutboxEventRepository outbox;
    private final DeduplicationKeyRepository dedup;
    private final ObjectMapper mapper;
    private final Duration window;

    public TransactionWriter(LedgerTransactionRepository ledger, OutboxEventRepository outbox, DeduplicationKeyRepository dedup, ObjectMapper mapper, @Value("${app.duplicate-window:PT15M}") Duration window) {
        this.ledger = ledger;
        this.outbox = outbox;
        this.dedup = dedup;
        this.mapper = mapper;
        this.window = window;
    }

    @Transactional
    public TransactionResponse persist(TransactionRequest r) {
        Instant now = Instant.now();
        if (dedup.claim(r.transactionId(), now.plus(window)) == 0)
            throw new ValidationException("DUPLICATE_TRANSACTION", "Duplicate transaction ID within sliding window");
        UUID id = UUID.randomUUID();
        LedgerTransaction tx = new LedgerTransaction(id, r.transactionId(), r.amount(), r.currency().toUpperCase(), r.settlementDate(), "ACCEPTED", now);
        ledger.save(tx);
        try {
            String payload = mapper.writeValueAsString(new TransactionEvent(UUID.randomUUID(), id, r.transactionId(), r.amount(), r.currency().toUpperCase(), r.settlementDate(), "ACCEPTED", now));
            outbox.save(new OutboxEvent(UUID.randomUUID(), id, "TRANSACTION_ACCEPTED", payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize outbox event", e);
        }
        return new TransactionResponse(id, r.transactionId(), "ACCEPTED", now);
    }

    public record TransactionEvent(UUID eventId, UUID ledgerId, String transactionId, java.math.BigDecimal amount,
                                   String currency, LocalDate settlementDate, String status, Instant occurredAt) {
    }
}
