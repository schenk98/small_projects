package com.poe.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poe.backend.security.CurrentUser;
import com.poe.backend.service.AppService;

/**
 * Sandbox endpoints for authenticated privileged users only (configured email or DB flag).
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {
    private final AppService appService;

    public DevController(AppService appService) {
        this.appService = appService;
    }

    @PostMapping("/grant-coins")
    public ResponseEntity<?> grantCoins(@RequestBody(required = false) Map<String, Integer> body) {
        int amt = body == null || body.get("amount") == null ? 1000 : Math.max(0, body.get("amount"));
        return ResponseEntity.ok(appService.devGrantCoins(CurrentUser.get(), amt));
    }

    @PostMapping("/refill-stats")
    public ResponseEntity<?> refill() {
        return ResponseEntity.ok(appService.devRefillStats(CurrentUser.get()));
    }

    @PostMapping("/set-stats")
    public ResponseEntity<?> setStats(@RequestBody Map<String, Double> body) {
        double h = body.getOrDefault("hungerPercent", 1.0);
        double ha = body.getOrDefault("happinessPercent", 1.0);
        double e = body.getOrDefault("energyPercent", 1.0);
        return ResponseEntity.ok(appService.devSetStats(CurrentUser.get(), h, ha, e));
    }
}
