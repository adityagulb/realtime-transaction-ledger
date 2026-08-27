package com.aditya.processor.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "deduplication_key")
public class DeduplicationKey {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected DeduplicationKey() {
    }
}
