package com.aditya.processor.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(@NotBlank String transactionId,
                                 @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
                                 @NotBlank @Size(min = 3, max = 3) String currency, @NotNull LocalDate settlementDate) {
}
