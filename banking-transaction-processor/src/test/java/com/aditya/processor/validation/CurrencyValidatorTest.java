package com.aditya.processor.validation;

import com.aditya.processor.dto.TransactionRequest;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyValidatorTest {
    private final CurrencyValidator v = new CurrencyValidator();

    @Test
    void acceptsSupportedCurrency() {
        assertDoesNotThrow(() -> v.validate(new TransactionRequest("t1", BigDecimal.ONE, "USD", LocalDate.now().plusDays(1))));
    }

    @Test
    void rejectsUnsupportedCurrency() {
        ValidationException e = assertThrows(ValidationException.class, () -> v.validate(new TransactionRequest("t1", BigDecimal.ONE, "XYZ", LocalDate.now().plusDays(1))));
        assertEquals("INVALID_CURRENCY", e.code());
    }
}
