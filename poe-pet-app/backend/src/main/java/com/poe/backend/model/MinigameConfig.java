package com.poe.backend.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("minigames")
public class MinigameConfig {
    @Id
    public String id;
    /** Stable identifier used by the API and frontend (e.g. {@code puzzle_swap}). */
    public String code;
    /** Display name shown in the minigames hub. */
    public String name;
    /** Shown in the minigames hub; optional on legacy DB rows. */
    public String description;
    /** Energy required to start the minigame. */
    public int energyCost;
    public boolean active;
    /**
     * Reward rules.
     *
     * This is a flexible map for quick iteration. Over time, consider migrating to a typed DTO.
     * Common keys:
     * - {@code type}: SCORE_LINEAR / SHIFTED_FIBONACCI / CONNECT4_OUTCOME
     * - {@code maxReward}, {@code coinsPerPoint}, {@code rewards}, {@code previewScores}
     */
    public Map<String, Object> rewardStrategy;
    /**
     * Happiness impact rules.
     *
     * Currently interpreted by {@code AppService.happinessDeltaForSimpleMinigame(...)} and {@code GameMath}.
     */
    public Map<String, Object> happinessImpactStrategy;
}
