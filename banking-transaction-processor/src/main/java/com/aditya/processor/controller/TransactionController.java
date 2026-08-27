package com.aditya.processor.controller;

import com.aditya.processor.dto.TransactionRequest;
import com.aditya.processor.dto.TransactionResponse;
import com.aditya.processor.service.TransactionProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionProcessingService service;

    public TransactionController(TransactionProcessingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> ingest(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.process(request));
    }

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World";
    }
}
