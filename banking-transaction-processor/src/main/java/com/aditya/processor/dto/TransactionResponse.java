package com.aditya.processor.dto;
import java.time.Instant; import java.util.UUID;
public record TransactionResponse(UUID ledgerId,String transactionId,String status,Instant acceptedAt) {}
