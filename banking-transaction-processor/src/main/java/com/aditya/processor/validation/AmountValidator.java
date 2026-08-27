package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(10)
public class AmountValidator implements TransactionValidator {
    public void validate(TransactionRequest r) {
        if (r.amount() == null || r.amount().compareTo(BigDecimal.ZERO) <= 0)
            throw new ValidationException("INVALID_AMOUNT", "Transaction amount must be positive");
    }
}
