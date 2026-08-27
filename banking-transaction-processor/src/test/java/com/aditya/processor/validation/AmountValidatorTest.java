package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AmountValidatorTest {
    private final AmountValidator v = new AmountValidator();

    @Test
    void rejectsZero() {
        ValidationException e = assertThrows(ValidationException.class, () -> v.validate(new TransactionRequest("t", BigDecimal.ZERO, "USD", LocalDate.now())));
        assertEquals("INVALID_AMOUNT", e.code());
    }

    @Test
    void acceptsPositive() {
        assertDoesNotThrow(() -> v.validate(new TransactionRequest("t", BigDecimal.ONE, "USD", LocalDate.now())));
    }
}
