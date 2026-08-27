package com.aditya.processor.service;

import com.aditya.processor.dto.TransactionRequest;
import com.aditya.processor.domain.RejectedTransaction;
import com.aditya.processor.repo.RejectedTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Service
public class RejectionAuditService {
    private final RejectedTransactionRepository repo;
    private final ObjectMapper mapper;

    public RejectionAuditService(RejectedTransactionRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(TransactionRequest r, String code, String message) {
        String payload;
        try {
            payload = mapper.writeValueAsString(r);
        } catch (JsonProcessingException e) {
            payload = "{ \"serializationError\" :true}";
        }
        repo.save(new RejectedTransaction(UUID.randomUUID(), r.transactionId(), code, message, payload, Instant.now()));
    }
}
