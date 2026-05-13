package com.poe.backend.sql.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.NotificationPreference;

/** Relational repository for user email/notification toggles. */
public interface NotificationPreferenceRepo extends JpaRepository<NotificationPreference, Long> {
    /** Load a user's single preferences row. */
    Optional<NotificationPreference> findByUserId(String userId);

    /** Load all users who opted into low-hunger reminders. */
    List<NotificationPreference> findByLowHungerEnabledTrue();

    /** Load all users who opted into daily AI summaries. */
    List<NotificationPreference> findByDailyAiSummaryEnabledTrue();
}
