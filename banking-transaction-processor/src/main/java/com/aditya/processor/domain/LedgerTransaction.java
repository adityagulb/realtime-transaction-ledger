package com.aditya.processor.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "ledger_transaction", indexes = {@Index(name = "idx_ledger_tx_created", columnList = "transaction_id,created_at")})
public class LedgerTransaction {
    @Id
    private UUID id;
    @Column(name = "transaction_id", nullable = false)
    private String transactionId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;
    @Column(nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(UUID id, String transactionId, BigDecimal amount, String currency, LocalDate settlementDate, String status, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.settlementDate = settlementDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
