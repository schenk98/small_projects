package com.thearena.controller;

import com.thearena.exception.ApiException;
import com.thearena.model.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return build(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed.", request.getRequestURI(), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Malformed JSON body.", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ApiErrorResponse> handleUnknown(Exception exception, HttpServletRequest request) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Unexpected server error.",
                request.getRequestURI(),
                Map.of()
        );
    }

    private org.springframework.http.ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, String> details
    ) {
        return org.springframework.http.ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                details
        ));
    }
}
