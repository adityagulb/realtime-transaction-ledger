package com.aditya.processor.service;

import com.aditya.processor.dto.TransactionRequest;
import com.aditya.processor.dto.TransactionResponse;
import com.aditya.processor.validation.ValidationChain;
import com.aditya.processor.validation.ValidationException;

import org.springframework.stereotype.Service;

@Service
public class TransactionProcessingService {
    private final ValidationChain chain;
    private final TransactionWriter writer;
    private final RejectionAuditService audit;

    public TransactionProcessingService(ValidationChain chain, TransactionWriter writer, RejectionAuditService audit) {
        this.chain = chain;
        this.writer = writer;
        this.audit = audit;
    }

    public TransactionResponse process(TransactionRequest r) {
        try {
            chain.validate(r);
            return writer.persist(r);
        } catch (
                ValidationException e) {
            audit.record(r, e.code(), e.getMessage());
            throw e;
        }
    }
}
