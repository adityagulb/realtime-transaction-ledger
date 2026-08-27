package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(20)
public class CurrencyValidator implements TransactionValidator {
    private static final Set<String> ALLOWED = Set.of("USD", "EUR", "GBP", "INR");

    public void validate(TransactionRequest r) {
        if (!ALLOWED.contains(r.currency().toUpperCase()))
            throw new ValidationException("INVALID_CURRENCY", "Unsupported currency: " + r.currency());
    }
}
