package com.poe.backend.sql.view;

import java.time.Instant;
import java.time.LocalDate;

/** Player-facing projection of one shared daily challenge plus user progress. */
public record DailyChallengeView(
        long id,
        LocalDate challengeDate,
        int slotOrder,
        String challengeType,
        String title,
        String description,
        String matchValue,
        int requiredCount,
        int progressCount,
        int progressPercent,
        boolean completed,
        Instant completedAt,
        int rewardCoins,
        boolean rewardGranted,
        Instant rewardGrantedAt,
        Instant lastEventAt) {
}
