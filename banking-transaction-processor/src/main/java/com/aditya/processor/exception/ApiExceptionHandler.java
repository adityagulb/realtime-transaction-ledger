package com.aditya.processor.exception;

import com.aditya.processor.dto.ErrorResponse;
import com.aditya.processor.validation.ValidationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ErrorResponse> validation(ValidationException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.code(), e.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> bean(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("SCHEMA_VALIDATION_FAILED", e.getBindingResult().getAllErrors().get(0).getDefaultMessage(), Instant.now()));
    }
}
