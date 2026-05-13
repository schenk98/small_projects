package com.poe.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poe.backend.security.CurrentUser;
import com.poe.backend.service.AppService;
import com.poe.backend.sql.service.NotificationPreferenceService;
import com.poe.backend.sql.service.ProgressQueryService;

/**
 * Main game API consumed by the Vite/React frontend.
 *
 * Convention:
 * - controllers stay thin (HTTP parsing + delegation)
 * - most business rules live in {@link AppService}
 */
@RestController
@RequestMapping("/api")
public class GameController {
    private final AppService appService;
    private final ProgressQueryService progressQueryService;
    private final NotificationPreferenceService notificationPreferenceService;

    public GameController(
            AppService appService,
            ProgressQueryService progressQueryService,
            NotificationPreferenceService notificationPreferenceService) {
        this.appService = appService;
        this.progressQueryService = progressQueryService;
        this.notificationPreferenceService = notificationPreferenceService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(appService.getDashboard(CurrentUser.get()));
    }

    /** Return all active shop catalog items that are player-visible. */
    @GetMapping("/shop/items")
    public ResponseEntity<?> shopItems() {
        return ResponseEntity.ok(appService.shopItems());
    }

    /** Purchase a shop item by code (coins are charged server-side). */
    @PostMapping("/shop/purchase")
    public ResponseEntity<?> purchase(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.purchase(CurrentUser.get(), payload.get("itemCode")));
    }

    /** List inventory entries owned by the current user. */
    @GetMapping("/inventory")
    public ResponseEntity<?> inventory() {
        return ResponseEntity.ok(appService.inventory(CurrentUser.get()));
    }

    /** Return achievements progress plus recent activity rows for the current user. */
    @GetMapping("/progress/summary")
    public ResponseEntity<?> progressSummary() {
        return ResponseEntity.ok(progressQueryService.getSummary(CurrentUser.get()));
    }

    /** Return the current user's first-version notification toggles. */
    @GetMapping("/notification-preferences")
    public ResponseEntity<?> notificationPreferences() {
        return ResponseEntity.ok(notificationPreferenceService.getForUser(CurrentUser.get()));
    }

    /** Update low-hunger and daily-AI-summary notification toggles. */
    @PostMapping("/notification-preferences")
    public ResponseEntity<?> updateNotificationPreferences(@RequestBody Map<String, Object> payload) {
        boolean lowHungerEnabled = payload.get("lowHungerEnabled") instanceof Boolean b && b;
        boolean dailyAiSummaryEnabled = payload.get("dailyAiSummaryEnabled") instanceof Boolean b && b;
        return ResponseEntity.ok(notificationPreferenceService.updateForUser(
                CurrentUser.get(),
                lowHungerEnabled,
                dailyAiSummaryEnabled));
    }

    /** Full visual catalog: PET_MOOD, BACKGROUND, FOREGROUND (sorted). */
    @GetMapping("/pet-visuals/catalog")
    public ResponseEntity<?> petVisualCatalog() {
        return ResponseEntity.ok(appService.petVisualCatalog());
    }

    @PostMapping("/pet-visuals/species")
    /** Set base pet species. Non-starter species must be unlocked through the shop first. */
    public ResponseEntity<?> setSpecies(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.setSpecies(CurrentUser.get(), payload.get("speciesCode")));
    }

    @PostMapping("/pet-visuals/mood-assets")
    /** Set per-mood image overrides by asset code (requires ownership or starter). */
    public ResponseEntity<?> setMoodAssets(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, String> moodAssetCodes = (Map<String, String>) payload.get("moodAssetCodes");
        return ResponseEntity.ok(appService.setMoodAssets(CurrentUser.get(), moodAssetCodes));
    }

    /** Equip scene layers; pass {@code none} or omit to clear. Requires starter or owned asset codes. */
    @PostMapping("/pet-visuals/equip-layers")
    public ResponseEntity<?> equipLayers(@RequestBody Map<String, String> payload) {
        String bg = payload != null ? payload.get("backgroundAssetCode") : null;
        String fg = payload != null ? payload.get("foregroundAssetCode") : null;
        return ResponseEntity.ok(appService.equipVisualLayers(CurrentUser.get(), bg, fg));
    }

    @PostMapping("/inventory/use")
    /** Use a consumable from inventory (supports confirmOverwrite flow). */
    public ResponseEntity<?> use(@RequestBody Map<String, Object> payload) {
        boolean confirm = payload.get("confirmOverwrite") instanceof Boolean b && b;
        return ResponseEntity.ok(appService.useConsumable(CurrentUser.get(), String.valueOf(payload.get("itemCode")), confirm));
    }

    /** Update user-facing pet name (used by AI prefix and some UI labels). */
    @PostMapping("/pet/name")
    public ResponseEntity<?> setPetName(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.setPetName(CurrentUser.get(), payload != null ? payload.get("name") : null));
    }

    /** AI chat endpoint: backend calls the Local SLM Gateway and returns assistant text. */
    @PostMapping("/ai/chat")
    public ResponseEntity<?> aiChat(@RequestBody Map<String, Object> payload) {
        String message = payload != null && payload.get("message") != null ? String.valueOf(payload.get("message")) : "";
        @SuppressWarnings("unchecked")
        List<Map<String, String>> conversation = payload != null && payload.get("conversation") instanceof List l
                ? (List<Map<String, String>>) l
                : List.of();
        return ResponseEntity.ok(appService.aiChat(CurrentUser.get(), conversation, message));
    }

    /** Public AI integration summary (gateway reachability, guardrail sizes). */
    @GetMapping("/ai/info")
    public ResponseEntity<?> aiInfo() {
        return ResponseEntity.ok(appService.getAiChatInfo());
    }

    @GetMapping("/minigames")
    public ResponseEntity<?> minigames() {
        return ResponseEntity.ok(appService.minigames());
    }

    /** Same payload as `dashboard.rewardPreview` — for clients or when dashboard JSON omits nested preview. */
    @GetMapping("/minigames/reward-preview")
    public ResponseEntity<?> rewardPreview() {
        return ResponseEntity.ok(appService.buildRewardPreview(CurrentUser.get()));
    }

    @PostMapping("/minigames/higher-lower/start")
    public ResponseEntity<?> start() {
        return ResponseEntity.ok(appService.startHigherLower(CurrentUser.get()));
    }

    @PostMapping("/minigames/higher-lower/guess")
    public ResponseEntity<?> guess(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.guessHigherLower(CurrentUser.get(), payload.get("guess")));
    }

    @PostMapping("/minigames/higher-lower/quit")
    public ResponseEntity<?> quit() {
        return ResponseEntity.ok(appService.quitHigherLower(CurrentUser.get()));
    }

    @PostMapping("/minigames/{code}/start-simple")
    public ResponseEntity<?> startSimple(@PathVariable String code) {
        return ResponseEntity.ok(appService.startSimpleMinigame(CurrentUser.get(), code));
    }

    @PostMapping("/minigames/{code}/abandon-simple")
    public ResponseEntity<?> abandonSimple(@PathVariable String code) {
        return ResponseEntity.ok(appService.abandonSimpleMinigame(CurrentUser.get(), code));
    }

    @PostMapping("/minigames/{code}/finish-simple")
    public ResponseEntity<?> finishSimple(@PathVariable String code, @RequestBody Map<String, Object> payload) {
        Object scoreObj = payload.get("score");
        int score = scoreObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(scoreObj != null ? scoreObj : 0));
        String connectDifficulty = payload.get("connectDifficulty") != null ? String.valueOf(payload.get("connectDifficulty"))
                : (payload.get("difficulty") != null ? String.valueOf(payload.get("difficulty")) : null);
        Integer connectHumanMoves = payload.get("connectHumanMoves") instanceof Number n ? n.intValue() : null;
        return ResponseEntity.ok(appService.finishSimpleMinigame(CurrentUser.get(), code, score, connectDifficulty, connectHumanMoves));
    }
}
