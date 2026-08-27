package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.*;

@Component
@Order(30)
public class SettlementDateValidator implements TransactionValidator {
    public void validate(TransactionRequest r) {
        LocalDate today = LocalDate.now(Clock.systemUTC());
        if (r.settlementDate().isBefore(today))
            throw new ValidationException("INVALID_SETTLEMENT_DATE", "Settlement date cannot be earlier than current UTC day");
    }
}
