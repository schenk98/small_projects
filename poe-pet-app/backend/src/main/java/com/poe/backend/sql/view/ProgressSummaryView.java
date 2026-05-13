package com.poe.backend.sql.view;

import java.util.List;

/** Combined achievements + recent history payload for the first progress screen. */
public record ProgressSummaryView(
        List<DailyChallengeView> dailyChallenges,
        List<AchievementProgressView> achievements,
        List<ActivityEventView> recentActivity) {
}
