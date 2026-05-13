package com.poe.backend.sql.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.poe.backend.model.Wallet;
import com.poe.backend.repo.WalletRepo;
import com.poe.backend.sql.model.DailyChallengeDefinition;
import com.poe.backend.sql.model.UserDailyChallengeProgress;
import com.poe.backend.sql.repo.DailyChallengeDefinitionRepo;
import com.poe.backend.sql.repo.UserDailyChallengeProgressRepo;

class DailyChallengeServiceTest {
    private final DailyChallengeDefinitionRepo dailyChallengeDefinitionRepo = org.mockito.Mockito.mock(DailyChallengeDefinitionRepo.class);
    private final UserDailyChallengeProgressRepo userDailyChallengeProgressRepo =
            org.mockito.Mockito.mock(UserDailyChallengeProgressRepo.class);
    private final WalletRepo walletRepo = org.mockito.Mockito.mock(WalletRepo.class);
    private final DailyChallengeService service = new DailyChallengeService(
            dailyChallengeDefinitionRepo,
            userDailyChallengeProgressRepo,
            walletRepo);

    @Test
    void getTodayForUserMergesDefinitionsWithCurrentProgress() {
        DailyChallengeDefinition definition = definition(1L, "Puzzle refresh", "MINIGAME_FINISHED", "puzzle_swap", 20);
        UserDailyChallengeProgress progress = new UserDailyChallengeProgress();
        progress.userId = "u1";
        progress.challengeDefinitionId = 1L;
        progress.progressCount = 1;
        progress.completedAt = Instant.parse("2026-05-12T12:00:00Z");
        progress.rewardGranted = true;
        progress.rewardGrantedAt = Instant.parse("2026-05-12T12:00:01Z");

        when(dailyChallengeDefinitionRepo.findByChallengeDateOrderBySlotOrderAsc(any(LocalDate.class)))
                .thenReturn(todayDefinitions(definition));
        when(userDailyChallengeProgressRepo.findByUserIdAndChallengeDefinitionIdIn(anyString(), anyList()))
                .thenReturn(List.of(progress));

        var views = service.getTodayForUser("u1");

        assertEquals(3, views.size());
        assertEquals("Puzzle refresh", views.get(0).title());
        assertTrue(views.get(0).completed());
        assertTrue(views.get(0).rewardGranted());
    }

    @Test
    void recordEventUpdatesMatchingChallengeAndGrantsCoins() {
        DailyChallengeDefinition definition = definition(1L, "Puzzle refresh", "MINIGAME_FINISHED", "puzzle_swap", 20);
        Wallet wallet = new Wallet();
        wallet.userId = "u1";
        wallet.coins = 100;

        when(dailyChallengeDefinitionRepo.findByChallengeDateOrderBySlotOrderAsc(any(LocalDate.class)))
                .thenReturn(todayDefinitions(definition));
        when(userDailyChallengeProgressRepo.findByUserIdAndChallengeDefinitionId("u1", 1L))
                .thenReturn(Optional.empty());
        when(userDailyChallengeProgressRepo.save(any(UserDailyChallengeProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(walletRepo.findByUserId("u1")).thenReturn(Optional.of(wallet));

        service.recordEvent("u1", "MINIGAME_FINISHED", Instant.parse("2026-05-12T12:00:00Z"), Map.of("gameCode", "puzzle_swap"));

        assertEquals(120, wallet.coins);
        verify(walletRepo).save(wallet);

        ArgumentCaptor<UserDailyChallengeProgress> captor = ArgumentCaptor.forClass(UserDailyChallengeProgress.class);
        verify(userDailyChallengeProgressRepo, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        UserDailyChallengeProgress lastSaved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(1, lastSaved.progressCount);
        assertTrue(lastSaved.rewardGranted);
        assertTrue(lastSaved.completedAt != null);
    }

    @Test
    void recordEventIgnoresNonMatchingMetadata() {
        DailyChallengeDefinition definition = definition(1L, "Puzzle refresh", "MINIGAME_FINISHED", "puzzle_swap", 20);

        when(dailyChallengeDefinitionRepo.findByChallengeDateOrderBySlotOrderAsc(any(LocalDate.class)))
                .thenReturn(todayDefinitions(definition));

        service.recordEvent("u1", "MINIGAME_FINISHED", Instant.parse("2026-05-12T12:00:00Z"), Map.of("gameCode", "unknown_game"));

        verify(userDailyChallengeProgressRepo, org.mockito.Mockito.never()).save(any(UserDailyChallengeProgress.class));
    }

    private DailyChallengeDefinition definition(Long id, String title, String triggerEventType, String matchValue, int rewardCoins) {
        DailyChallengeDefinition definition = new DailyChallengeDefinition();
        definition.id = id;
        definition.challengeDate = LocalDate.parse("2026-05-12");
        definition.slotOrder = 1;
        definition.challengeType = "FINISH_MINIGAME";
        definition.triggerEventType = triggerEventType;
        definition.matchValue = matchValue;
        definition.title = title;
        definition.description = "Finish Puzzle Swap once today.";
        definition.requiredCount = 1;
        definition.rewardCoins = rewardCoins;
        definition.createdAt = Instant.parse("2026-05-12T00:00:00Z");
        definition.updatedAt = Instant.parse("2026-05-12T00:00:00Z");
        return definition;
    }

    private List<DailyChallengeDefinition> todayDefinitions(DailyChallengeDefinition first) {
        DailyChallengeDefinition second = definition(2L, "Meal time", "CONSUMABLE_USED", "food_small_50", 12);
        second.challengeType = "USE_CONSUMABLE";
        second.description = "Use Meal Pack +50 once today.";
        second.slotOrder = 2;

        DailyChallengeDefinition third = definition(3L, "Checkerboard clash", "MINIGAME_FINISHED", "checkers_ai", 20);
        third.description = "Finish Checkers vs AI once today.";
        third.slotOrder = 3;

        return List.of(first, second, third);
    }
}
