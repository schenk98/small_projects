package com.poe.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poe.backend.security.CurrentUser;
import com.poe.backend.service.AppService;

/**
 * Authentication and account-management endpoints.
 *
 * Most routes here are intentionally public (register/login/verify/reset). Be careful when adding
 * new endpoints under `/auth/*`: the {@link com.poe.backend.security.AuthInterceptor} currently
 * allows `/auth/` without requiring a Bearer token.
 */
@RestController
public class AuthController {
    private final AppService appService;

    public AuthController(AppService appService) {
        this.appService = appService;
    }

    @PostMapping("/auth/register")
    /** Register a new account and send verification email. */
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.register(payload.get("email"), payload.get("password")));
    }

    @GetMapping("/auth/verify-email")
    /** Verify email using a token from an email link. */
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(appService.verifyEmail(token));
    }

    @PostMapping("/auth/login")
    /** Login and receive access+refresh tokens (requires verified email). */
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.login(payload.get("email"), payload.get("password")));
    }

    @PostMapping("/auth/refresh")
    /** Rotate tokens using a refresh token. */
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.refresh(payload.get("refreshToken")));
    }

    @PostMapping("/auth/forgot-password")
    /** Request a reset-password email (returns generic message to avoid account enumeration). */
    public ResponseEntity<?> forgot(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.forgotPassword(payload.get("email")));
    }

    @PostMapping("/auth/reset-password")
    /** Reset password using a reset token + new password. */
    public ResponseEntity<?> reset(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.resetPassword(payload.get("token"), payload.get("newPassword")));
    }

    @GetMapping("/auth/me")
    /** Return account identity info for the current access token. */
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(appService.me(CurrentUser.get()));
    }
}
