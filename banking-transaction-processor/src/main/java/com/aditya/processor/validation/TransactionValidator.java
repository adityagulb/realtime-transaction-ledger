package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;

public interface TransactionValidator {
    void validate(TransactionRequest request);
}
