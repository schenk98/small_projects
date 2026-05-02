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

@RestController
public class AuthController {
    private final AppService appService;

    public AuthController(AppService appService) {
        this.appService = appService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.register(payload.get("email"), payload.get("password")));
    }

    @GetMapping("/auth/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        return ResponseEntity.ok(appService.verifyEmail(token));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.login(payload.get("email"), payload.get("password")));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.refresh(payload.get("refreshToken")));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<?> forgot(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.forgotPassword(payload.get("email")));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<?> reset(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.resetPassword(payload.get("token"), payload.get("newPassword")));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.ok(appService.me(CurrentUser.get()));
    }
}
