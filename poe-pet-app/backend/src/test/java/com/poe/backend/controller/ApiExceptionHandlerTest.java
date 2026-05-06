package com.poe.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for API error normalization.
 *
 * These tests are intentionally "no Spring context": they validate mapping logic only.
 */
class ApiExceptionHandlerTest {
    @Test
    void apiExceptionUsesItsStatus() {
        ApiExceptionHandler h = new ApiExceptionHandler();
        ResponseEntity<?> res = h.handle(new ApiException(HttpStatus.NOT_FOUND, "Missing"));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void runtimeExceptionDefaultsToBadRequest() {
        ApiExceptionHandler h = new ApiExceptionHandler();
        ResponseEntity<?> res = h.handle(new RuntimeException("Oops"));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }
}

