package com.poe.backend.controller;

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

@RestController
@RequestMapping("/api")
public class GameController {
    private final AppService appService;

    public GameController(AppService appService) {
        this.appService = appService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        return ResponseEntity.ok(appService.getDashboard(CurrentUser.get()));
    }

    @GetMapping("/shop/items")
    public ResponseEntity<?> shopItems() {
        return ResponseEntity.ok(appService.shopItems());
    }

    @PostMapping("/shop/purchase")
    public ResponseEntity<?> purchase(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.purchase(CurrentUser.get(), payload.get("itemCode")));
    }

    @GetMapping("/inventory")
    public ResponseEntity<?> inventory() {
        return ResponseEntity.ok(appService.inventory(CurrentUser.get()));
    }

    /** Full visual catalog: PET_MOOD, BACKGROUND, FOREGROUND (sorted). */
    @GetMapping("/pet-visuals/catalog")
    public ResponseEntity<?> petVisualCatalog() {
        return ResponseEntity.ok(appService.petVisualCatalog());
    }

    @PostMapping("/pet-visuals/species")
    public ResponseEntity<?> setSpecies(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(appService.setSpecies(CurrentUser.get(), payload.get("speciesCode")));
    }

    @PostMapping("/pet-visuals/mood-assets")
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
    public ResponseEntity<?> use(@RequestBody Map<String, Object> payload) {
        boolean confirm = payload.get("confirmOverwrite") instanceof Boolean b && b;
        return ResponseEntity.ok(appService.useConsumable(CurrentUser.get(), String.valueOf(payload.get("itemCode")), confirm));
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
