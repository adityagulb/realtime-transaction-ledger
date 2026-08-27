package com.aditya.processor.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rejected_transaction")
public class RejectedTransaction {
    @Id
    private UUID id;
    @Column(name = "transaction_id")
    private String transactionId;
    @Column(name = "error_code", nullable = false)
    private String errorCode;
    @Column(name = "error_message", nullable = false)
    private String errorMessage;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RejectedTransaction() {
    }

    public RejectedTransaction(UUID id, String transactionId, String errorCode, String errorMessage, String payload, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}
