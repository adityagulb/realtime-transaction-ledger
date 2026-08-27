package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationChain {
    private final List<TransactionValidator> validators;

    public ValidationChain(List<TransactionValidator> validators) {
        this.validators = validators;
    }

    public void validate(TransactionRequest request) {
        validators.forEach(v -> v.validate(request));
    }
}
