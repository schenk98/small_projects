package com.poe.backend.controller;

import org.springframework.http.HttpStatus;

/**
 * Small exception type to communicate an explicit HTTP status to the API layer.
 *
 * We still return the frontend-friendly error shape: {@code { "error": "<message>" }}.
 */
public class ApiException extends RuntimeException {
    public final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}

