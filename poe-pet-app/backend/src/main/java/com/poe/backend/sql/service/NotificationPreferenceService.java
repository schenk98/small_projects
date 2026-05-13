package com.poe.backend.sql.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.poe.backend.sql.model.NotificationPreference;
import com.poe.backend.sql.repo.NotificationPreferenceRepo;
import com.poe.backend.sql.view.NotificationPreferenceView;

/**
 * Owns SQL-backed notification toggle rows.
 *
 * The first product surface is intentionally small: two toggles that map to the
 * first notification types in the roadmap.
 */
@Service
public class NotificationPreferenceService {
    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepo notificationPreferenceRepo;

    public NotificationPreferenceService(NotificationPreferenceRepo notificationPreferenceRepo) {
        this.notificationPreferenceRepo = notificationPreferenceRepo;
    }

    /** Create a default preferences row once per user. */
    public void ensureDefaults(String userId) {
        try {
            getOrCreate(userId);
        } catch (Exception e) {
            log.warn("Notification preference bootstrap failed for user={}: {}", userId, e.getMessage());
        }
    }

    /** Read current notification toggles, creating default values if needed. */
    public NotificationPreferenceView getForUser(String userId) {
        NotificationPreference preference = getOrCreate(userId);
        return toView(preference);
    }

    /** Update both first-version notification toggles for a user. */
    public NotificationPreferenceView updateForUser(String userId, boolean lowHungerEnabled, boolean dailyAiSummaryEnabled) {
        NotificationPreference preference = getOrCreate(userId);
        preference.lowHungerEnabled = lowHungerEnabled;
        preference.dailyAiSummaryEnabled = dailyAiSummaryEnabled;
        preference.updatedAt = Instant.now();
        notificationPreferenceRepo.save(preference);
        return toView(preference);
    }

    private NotificationPreference getOrCreate(String userId) {
        return notificationPreferenceRepo.findByUserId(userId).orElseGet(() -> createDefault(userId));
    }

    private NotificationPreference createDefault(String userId) {
        Instant now = Instant.now();
        NotificationPreference preference = new NotificationPreference();
        preference.userId = userId;
        preference.lowHungerEnabled = false;
        preference.dailyAiSummaryEnabled = false;
        preference.createdAt = now;
        preference.updatedAt = now;
        return notificationPreferenceRepo.save(preference);
    }

    private NotificationPreferenceView toView(NotificationPreference preference) {
        return new NotificationPreferenceView(
                preference.lowHungerEnabled,
                preference.dailyAiSummaryEnabled,
                preference.updatedAt);
    }
}
