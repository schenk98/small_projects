package com.poe.backend.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.poe.backend.model.MinigameConfig;
import com.poe.backend.repo.MinigameRepo;

/**
 * Inserts minigame definitions that exist in code but may be missing from older MongoDB seeds
 * (e.g. Minesweeper, Checkers). Skips if a document with the same {@code code} already exists.
 */
@Component
@Order(50)
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
        return List.of(minesweep(), checkers());
    }

    private static MinigameConfig minesweep() {
        MinigameConfig m = new MinigameConfig();
        m.code = "minesweep_ai";
        m.name = "Minesweeper";
        m.description = "Clear the field without hitting a mine. Mines are placed after your first reveal (safe first click).";
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
        m.description = "American checkers on an 8×8 board. Mandatory capture; longest jump sequence; multi-jump chains. Three AI strengths.";
        m.energyCost = 5;
        m.active = true;
        m.rewardStrategy = Map.of(
                "type", "CONNECT4_OUTCOME",
                "rewards", Map.of("win", 8, "draw", 3, "loss", 1));
        m.happinessImpactStrategy = Map.of("type", "SCORE_THRESHOLDS");
        return m;
    }
}
