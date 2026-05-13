package com.poe.backend.sql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.poe.backend.sql.model.AchievementDefinition;
import com.poe.backend.sql.model.UserAchievement;
import com.poe.backend.sql.repo.AchievementDefinitionRepo;
import com.poe.backend.sql.repo.UserAchievementRepo;

class AchievementProgressServiceTest {
    private final AchievementDefinitionRepo achievementDefinitionRepo = org.mockito.Mockito.mock(AchievementDefinitionRepo.class);
    private final UserAchievementRepo userAchievementRepo = org.mockito.Mockito.mock(UserAchievementRepo.class);
    private final AchievementProgressService service =
            new AchievementProgressService(achievementDefinitionRepo, userAchievementRepo);

    @Test
    void recordEventCreatesNewProgressRowWhenMissing() {
        AchievementDefinition definition = definition("minigame_regular", 3, "MINIGAME_FINISHED");
        Instant happenedAt = Instant.parse("2026-05-12T11:00:00Z");

        when(achievementDefinitionRepo.findByActiveTrueAndTriggerEventTypeOrderBySortOrderAscCodeAsc("MINIGAME_FINISHED"))
                .thenReturn(List.of(definition));
        when(userAchievementRepo.findByUserIdAndAchievementCode("u1", "minigame_regular"))
                .thenReturn(Optional.empty());
        when(userAchievementRepo.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordEvent("u1", "MINIGAME_FINISHED", happenedAt);

        ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepo).save(captor.capture());
        UserAchievement saved = captor.getValue();
        assertEquals("u1", saved.userId);
        assertEquals("minigame_regular", saved.achievementCode);
        assertEquals(1, saved.progressCount);
        assertSame(happenedAt, saved.createdAt);
        assertSame(happenedAt, saved.updatedAt);
        assertSame(happenedAt, saved.lastEventAt);
        assertNull(saved.unlockedAt);
    }

    @Test
    void recordEventUnlocksExistingProgressAtThreshold() {
        AchievementDefinition definition = definition("pet_chatter", 3, "AI_CHAT_SENT");
        Instant happenedAt = Instant.parse("2026-05-12T11:15:00Z");
        UserAchievement existing = new UserAchievement();
        existing.userId = "u1";
        existing.achievementCode = "pet_chatter";
        existing.progressCount = 2;

        when(achievementDefinitionRepo.findByActiveTrueAndTriggerEventTypeOrderBySortOrderAscCodeAsc("AI_CHAT_SENT"))
                .thenReturn(List.of(definition));
        when(userAchievementRepo.findByUserIdAndAchievementCode("u1", "pet_chatter"))
                .thenReturn(Optional.of(existing));

        service.recordEvent("u1", "AI_CHAT_SENT", happenedAt);

        ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepo).save(captor.capture());
        UserAchievement saved = captor.getValue();
        assertEquals(3, saved.progressCount);
        assertSame(happenedAt, saved.updatedAt);
        assertSame(happenedAt, saved.lastEventAt);
        assertSame(happenedAt, saved.unlockedAt);
    }

    @Test
    void recordEventDoesNothingWhenNoDefinitionsMatch() {
        when(achievementDefinitionRepo.findByActiveTrueAndTriggerEventTypeOrderBySortOrderAscCodeAsc("NOOP"))
                .thenReturn(List.of());

        service.recordEvent("u1", "NOOP", Instant.parse("2026-05-12T11:30:00Z"));

        verify(userAchievementRepo, never()).save(any(UserAchievement.class));
    }

    private AchievementDefinition definition(String code, int requiredCount, String triggerEventType) {
        AchievementDefinition definition = new AchievementDefinition();
        definition.code = code;
        definition.requiredCount = requiredCount;
        definition.triggerEventType = triggerEventType;
        definition.active = true;
        return definition;
    }
}
