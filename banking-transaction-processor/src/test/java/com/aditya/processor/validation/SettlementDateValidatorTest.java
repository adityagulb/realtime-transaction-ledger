package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class SettlementDateValidatorTest {
    private final SettlementDateValidator v = new SettlementDateValidator();

    @Test
    void rejectsPastDate() {
        assertThrows(ValidationException.class, () -> v.validate(new TransactionRequest("t", BigDecimal.ONE, "USD", LocalDate.now(Clock.systemUTC()).minusDays(1))));
    }

    @Test
    void acceptsTodayUtc() {
        assertDoesNotThrow(() -> v.validate(new TransactionRequest("t", BigDecimal.ONE, "USD", LocalDate.now(Clock.systemUTC()))));
    }
}
