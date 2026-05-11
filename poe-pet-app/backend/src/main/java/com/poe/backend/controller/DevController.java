package com.poe.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poe.backend.security.CurrentUser;
import com.poe.backend.service.AppService;
import com.poe.backend.service.AiGatewayClient;

/**
 * Sandbox endpoints for authenticated privileged users only (configured email or DB flag).
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {
    private final AppService appService;
    private final AiGatewayClient ai;

    public DevController(AppService appService, AiGatewayClient ai) {
        this.appService = appService;
        this.ai = ai;
    }

    @PostMapping("/grant-coins")
    /** Grant coins to the current user (privileged only). */
    public ResponseEntity<?> grantCoins(@RequestBody(required = false) Map<String, Integer> body) {
        int amt = body == null || body.get("amount") == null ? 1000 : Math.max(0, body.get("amount"));
        return ResponseEntity.ok(appService.devGrantCoins(CurrentUser.get(), amt));
    }

    @PostMapping("/refill-stats")
    /** Set pet stats to 100/100/100 for quick testing (privileged only). */
    public ResponseEntity<?> refill() {
        return ResponseEntity.ok(appService.devRefillStats(CurrentUser.get()));
    }

    @PostMapping("/set-stats")
    /** Set pet stats as fractions (0..1) for quick testing (privileged only). */
    public ResponseEntity<?> setStats(@RequestBody Map<String, Double> body) {
        double h = body.getOrDefault("hungerPercent", 1.0);
        double ha = body.getOrDefault("happinessPercent", 1.0);
        double e = body.getOrDefault("energyPercent", 1.0);
        return ResponseEntity.ok(appService.devSetStats(CurrentUser.get(), h, ha, e));
    }

    @GetMapping("/ai/config")
    /** Show effective AI gateway config (privileged only). */
    public ResponseEntity<?> aiConfig() {
        if (!appService.isPrivileged(CurrentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        return ResponseEntity.ok(ai.getConfig());
    }

    @PostMapping("/ai/config")
    /** Update AI gateway config at runtime (privileged only). */
    public ResponseEntity<?> setAiConfig(@RequestBody Map<String, String> body) {
        if (!appService.isPrivileged(CurrentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        ai.setConfig(body.getOrDefault("baseUrl", ""), body.getOrDefault("apiKey", ""));
        return ResponseEntity.ok(Map.of("ok", true, "config", ai.getConfig()));
    }

    @GetMapping("/ai/health")
    /** Proxy AI gateway health check (privileged only). */
    public ResponseEntity<?> aiHealth() {
        if (!appService.isPrivileged(CurrentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        try {
            return ResponseEntity.ok(ai.health());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/ai/ready")
    /** Proxy AI gateway readiness check (privileged only). */
    public ResponseEntity<?> aiReady() {
        if (!appService.isPrivileged(CurrentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        try {
            return ResponseEntity.ok(ai.ready());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/ai/chat-test")
    /** Developer-only chat test that builds the fixed context prefix from current pet stats. */
    public ResponseEntity<?> aiChat(@RequestBody Map<String, Object> body) {
        if (!appService.isPrivileged(CurrentUser.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }
        String message = body.get("message") != null ? String.valueOf(body.get("message")) : "";
        String petName = body.get("petName") != null ? String.valueOf(body.get("petName")) : "Pet";
        return ResponseEntity.ok(appService.devAiChatTest(CurrentUser.get(), petName, message));
    }
}
