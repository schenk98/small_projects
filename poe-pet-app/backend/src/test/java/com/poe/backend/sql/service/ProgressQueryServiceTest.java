package com.poe.backend.sql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe.backend.sql.model.ActivityEvent;
import com.poe.backend.sql.model.AchievementDefinition;
import com.poe.backend.sql.model.UserAchievement;
import com.poe.backend.sql.repo.ActivityEventRepo;
import com.poe.backend.sql.repo.AchievementDefinitionRepo;
import com.poe.backend.sql.repo.UserAchievementRepo;
import com.poe.backend.sql.view.DailyChallengeView;
import com.poe.backend.sql.view.ProgressSummaryView;

class ProgressQueryServiceTest {
    private final ActivityEventRepo activityEventRepo = org.mockito.Mockito.mock(ActivityEventRepo.class);
    private final AchievementDefinitionRepo achievementDefinitionRepo = org.mockito.Mockito.mock(AchievementDefinitionRepo.class);
    private final UserAchievementRepo userAchievementRepo = org.mockito.Mockito.mock(UserAchievementRepo.class);
    private final DailyChallengeService dailyChallengeService = org.mockito.Mockito.mock(DailyChallengeService.class);
    private final ProgressQueryService service = new ProgressQueryService(
            activityEventRepo,
            achievementDefinitionRepo,
            userAchievementRepo,
            dailyChallengeService,
            new ObjectMapper());

    @Test
    void getSummaryMergesDefinitionProgressAndRecentActivity() {
        AchievementDefinition unlockedDefinition = definition("pet_namer", "Name Tag", 1);
        AchievementDefinition inProgressDefinition = definition("minigame_regular", "Playful Start", 3);

        UserAchievement unlockedProgress = new UserAchievement();
        unlockedProgress.userId = "u1";
        unlockedProgress.achievementCode = "pet_namer";
        unlockedProgress.progressCount = 1;
        unlockedProgress.unlockedAt = Instant.parse("2026-05-12T12:00:00Z");
        unlockedProgress.lastEventAt = Instant.parse("2026-05-12T12:00:00Z");

        ActivityEvent event = new ActivityEvent();
        event.id = 7L;
        event.eventType = "SHOP_PURCHASED";
        event.source = "shop";
        event.happenedAt = Instant.parse("2026-05-12T12:05:00Z");
        event.petName = "Miki";
        event.coinBalance = 200;
        event.metadataJson = "{\"itemCode\":\"apple\",\"priceCoins\":10}";

        when(achievementDefinitionRepo.findByActiveTrueOrderBySortOrderAscCodeAsc())
                .thenReturn(List.of(unlockedDefinition, inProgressDefinition));
        when(userAchievementRepo.findByUserIdOrderByUpdatedAtDesc("u1"))
                .thenReturn(List.of(unlockedProgress));
        when(activityEventRepo.findTop20ByUserIdOrderByHappenedAtDescIdDesc("u1"))
                .thenReturn(List.of(event));
        when(dailyChallengeService.getTodayForUser("u1"))
                .thenReturn(List.of(new DailyChallengeView(
                        1L,
                        java.time.LocalDate.parse("2026-05-12"),
                        1,
                        "FINISH_MINIGAME",
                        "Puzzle refresh",
                        "Finish Puzzle Swap once today.",
                        "puzzle_swap",
                        1,
                        0,
                        0,
                        false,
                        null,
                        20,
                        false,
                        null,
                        null)));

        ProgressSummaryView summary = service.getSummary("u1");

        assertEquals(1, summary.dailyChallenges().size());
        assertEquals("Puzzle refresh", summary.dailyChallenges().get(0).title());
        assertEquals(2, summary.achievements().size());
        assertTrue(summary.achievements().get(0).unlocked());
        assertEquals(100, summary.achievements().get(0).progressPercent());
        assertFalse(summary.achievements().get(1).unlocked());
        assertEquals(0, summary.achievements().get(1).progressCount());
        assertEquals(1, summary.recentActivity().size());
        assertEquals("SHOP_PURCHASED", summary.recentActivity().get(0).eventType());
        assertEquals("apple", summary.recentActivity().get(0).details().get("itemCode"));
        assertEquals(200, summary.recentActivity().get(0).coinBalance());
    }

    private AchievementDefinition definition(String code, String title, int requiredCount) {
        AchievementDefinition definition = new AchievementDefinition();
        definition.code = code;
        definition.title = title;
        definition.description = "desc";
        definition.category = "general";
        definition.requiredCount = requiredCount;
        definition.active = true;
        return definition;
    }
}
