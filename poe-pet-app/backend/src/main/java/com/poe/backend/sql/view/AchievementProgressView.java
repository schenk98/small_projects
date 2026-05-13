package com.poe.backend.sql.view;

import java.time.Instant;

/**
 * Player-facing achievement row returned by the progress API.
 *
 * This combines static definition data with user-specific progress so the
 * frontend does not need to join multiple API payloads itself.
 */
public record AchievementProgressView(
        String code,
        String title,
        String description,
        String category,
        int requiredCount,
        int progressCount,
        int progressPercent,
        boolean unlocked,
        Instant unlockedAt,
        Instant lastEventAt) {
}
