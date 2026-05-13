package com.poe.backend.sql.view;

import java.time.Instant;

/** Player-facing notification settings payload returned by the backend. */
public record NotificationPreferenceView(
        boolean lowHungerEnabled,
        boolean dailyAiSummaryEnabled,
        Instant updatedAt) {
}
