package com.poe.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    /**
     * Normalize common backend errors into the frontend's expected shape:
     * `{ "error": "<message>" }`.
     *
     * Note: today this treats all {@link RuntimeException}s as HTTP 400. As the API grows,
     * we should differentiate validation errors (400), unauthorized (401), and forbidden (403).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handle(RuntimeException ex) {
        if (ex instanceof ApiException api) {
            return ResponseEntity.status(api.status).body(Map.of("error", api.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
