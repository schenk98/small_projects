package com.poe.backend.sql.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.poe.backend.sql.model.NotificationPreference;
import com.poe.backend.sql.repo.NotificationPreferenceRepo;
import com.poe.backend.sql.view.NotificationPreferenceView;

class NotificationPreferenceServiceTest {
    private final NotificationPreferenceRepo notificationPreferenceRepo = org.mockito.Mockito.mock(NotificationPreferenceRepo.class);
    private final NotificationPreferenceService service = new NotificationPreferenceService(notificationPreferenceRepo);

    @Test
    void getForUserCreatesDefaultsWhenMissing() {
        NotificationPreference created = new NotificationPreference();
        created.userId = "u1";
        created.lowHungerEnabled = false;
        created.dailyAiSummaryEnabled = false;
        created.updatedAt = Instant.parse("2026-05-12T12:00:00Z");

        when(notificationPreferenceRepo.findByUserId("u1")).thenReturn(Optional.empty());
        when(notificationPreferenceRepo.save(any(NotificationPreference.class))).thenReturn(created);

        NotificationPreferenceView view = service.getForUser("u1");

        verify(notificationPreferenceRepo).save(any(NotificationPreference.class));
        assertFalse(view.lowHungerEnabled());
        assertFalse(view.dailyAiSummaryEnabled());
        assertNotNull(view.updatedAt());
    }

    @Test
    void updateForUserOverwritesExistingFlags() {
        NotificationPreference existing = new NotificationPreference();
        existing.userId = "u1";
        existing.lowHungerEnabled = false;
        existing.dailyAiSummaryEnabled = false;
        existing.createdAt = Instant.parse("2026-05-12T11:00:00Z");
        existing.updatedAt = Instant.parse("2026-05-12T11:00:00Z");

        when(notificationPreferenceRepo.findByUserId("u1")).thenReturn(Optional.of(existing));
        when(notificationPreferenceRepo.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceView view = service.updateForUser("u1", true, true);

        assertTrue(view.lowHungerEnabled());
        assertTrue(view.dailyAiSummaryEnabled());
        assertNotNull(view.updatedAt());
        verify(notificationPreferenceRepo).save(existing);
    }
}
