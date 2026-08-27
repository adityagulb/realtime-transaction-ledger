package com.aditya.processor.service;

import com.aditya.processor.dto.TransactionRequest;
import com.aditya.processor.validation.ValidationChain;
import com.aditya.processor.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionProcessingServiceTest {
    @Test
    void auditsValidationFailure() {
        ValidationChain chain = mock(ValidationChain.class);
        TransactionWriter writer = mock(TransactionWriter.class);
        RejectionAuditService audit = mock(RejectionAuditService.class);
        TransactionRequest r = new TransactionRequest("dup", BigDecimal.TEN, "USD", LocalDate.now());
        doThrow(new ValidationException("DUPLICATE_TRANSACTION", "duplicate")).when(chain).validate(r);
        TransactionProcessingService s = new TransactionProcessingService(chain, writer, audit);
        assertThrows(ValidationException.class, () -> s.process(r));
        verify(audit).record(r, "DUPLICATE_TRANSACTION", "duplicate");
        verifyNoInteractions(writer);
    }
}
