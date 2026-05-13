package com.poe.backend.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.poe.backend.model.MinigameConfig;
import com.poe.backend.repo.MinigameRepo;

/**
 * Inserts minigame definitions that exist in code but may be missing from older MongoDB seeds.
 * Matches {@code mongodb/seed-minigames.json}. Skips if a document with the same {@code code} already exists.
 */
@Component
@Order(50)
@ConditionalOnProperty(name = "app.bootstrap.minigames.enabled", havingValue = "true", matchIfMissing = true)
public class MinigameCatalogSeeder implements ApplicationRunner {
    private final MinigameRepo minigameRepo;

    public MinigameCatalogSeeder(MinigameRepo minigameRepo) {
        this.minigameRepo = minigameRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (MinigameConfig cfg : defaultMinigamesToEnsure()) {
            Optional<MinigameConfig> existing = minigameRepo.findByCode(cfg.code);
            if (existing.isEmpty()) {
                minigameRepo.save(cfg);
            }
        }
    }

    private static List<MinigameConfig> defaultMinigamesToEnsure() {
        return List.of(higherLower(), puzzleSwap(), connect4(), minesweep(), checkers());
    }

    private static MinigameConfig higherLower() {
        MinigameConfig m = new MinigameConfig();
        m.code = "higher_lower";
        m.name = "Higher or Lower";
        m.description = "Guess if the next random number from 1 to 100 will be higher or lower.";
        m.energyCost = 3;
        m.active = true;
        m.rewardStrategy = Map.of(
                "type", "SHIFTED_FIBONACCI",
                "sequenceStart", List.of(1, 2),
                "maxReward", 48,
                "futureExtensions", List.of("GLOBAL_MULTIPLIER", "EVENT_MULTIPLIER"));
        Map<String, Object> happiness = new LinkedHashMap<>();
        happiness.put("type", "SCORE_THRESHOLDS");
        happiness.put("thresholds", List.of(
                Map.of("minScore", 0, "maxScore", 0, "happinessDeltaPercent", -0.1),
                Map.of("minScore", 1, "maxScore", 1, "happinessDeltaPercent", 0.0),
                Map.of(
                        "minScore", 2,
                        "maxScore", 999,
                        "happinessDeltaPercentPerPoint", 0.125,
                        "maxPositivePercent", 0.625)));
        m.happinessImpactStrategy = happiness;
        return m;
    }

    private static MinigameConfig puzzleSwap() {
        MinigameConfig m = new MinigameConfig();
        m.code = "puzzle_swap";
        m.name = "Puzzle Swap";
        m.description = "Rebuild shuffled image by swapping two tiles.";
        m.energyCost = 4;
        m.active = true;
        m.rewardStrategy = Map.of(
                "type", "SCORE_LINEAR",
                "coinsPerPoint", 2,
                "maxReward", 96);
        m.happinessImpactStrategy = Map.of("type", "SCORE_THRESHOLDS");
        return m;
    }

    private static MinigameConfig connect4() {
        MinigameConfig m = new MinigameConfig();
        m.code = "connect4_ai";
        m.name = "Connect 4 AI";
        m.description = "Play Connect 4 against AI with multiple difficulty levels.";
        m.energyCost = 5;
        m.active = true;
        m.rewardStrategy = Map.of(
                "type", "CONNECT4_OUTCOME",
                "rewards", Map.of("win", 9, "draw", 4, "loss", 1));
        m.happinessImpactStrategy = Map.of("type", "SCORE_THRESHOLDS");
        return m;
    }

    private static MinigameConfig minesweep() {
        MinigameConfig m = new MinigameConfig();
        m.code = "minesweep_ai";
        m.name = "Minesweeper";
        m.description = "Clear the field without hitting a mine. Mines are placed after your first reveal (classic safe first click).";
        m.energyCost = 4;
        m.active = true;
        m.rewardStrategy = Map.of(
                "type", "CONNECT4_OUTCOME",
                "rewards", Map.of("win", 10, "draw", 0, "loss", 1));
        m.happinessImpactStrategy = Map.of("type", "SCORE_THRESHOLDS");
        return m;
    }

    private static MinigameConfig checkers() {
        MinigameConfig m = new MinigameConfig();
        m.code = "checkers_ai";
        m.name = "Checkers vs AI";
        m.description = "American checkers on an 8×8 board. Capture when available. Beat the AI on easy, medium, or hard.";
        m.energyCost = 5;
        m.active = true;
        m.rewardStrategy = Map.of(
                "type", "CONNECT4_OUTCOME",
                "rewards", Map.of("win", 8, "draw", 3, "loss", 1));
        m.happinessImpactStrategy = Map.of("type", "SCORE_THRESHOLDS");
        return m;
    }
}
