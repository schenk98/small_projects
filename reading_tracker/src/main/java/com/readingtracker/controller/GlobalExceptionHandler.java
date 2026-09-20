package com.readingtracker.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Unhandled exception while processing {}", request.getRequestURI(), ex);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Došlo k neočekávané chybě.");
        model.addAttribute("requestUri", request.getRequestURI());
        return "error";
    }
}

