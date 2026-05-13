package com.poe.backend.sql.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.poe.backend.sql.model.AchievementDefinition;
import com.poe.backend.sql.model.UserAchievement;
import com.poe.backend.sql.repo.AchievementDefinitionRepo;
import com.poe.backend.sql.repo.UserAchievementRepo;

/**
 * Updates permanent achievement progress from recorded activity events.
 *
 * This keeps the logic intentionally simple for the first MVP step:
 * matching events increment counters, and definitions unlock once the required
 * count is reached.
 */
@Service
public class AchievementProgressService {
    private final AchievementDefinitionRepo achievementDefinitionRepo;
    private final UserAchievementRepo userAchievementRepo;

    public AchievementProgressService(
            AchievementDefinitionRepo achievementDefinitionRepo,
            UserAchievementRepo userAchievementRepo) {
        this.achievementDefinitionRepo = achievementDefinitionRepo;
        this.userAchievementRepo = userAchievementRepo;
    }

    /** Advance all active definitions that listen to the given event type. */
    public void recordEvent(String userId, String eventType, Instant happenedAt) {
        List<AchievementDefinition> definitions =
                achievementDefinitionRepo.findByActiveTrueAndTriggerEventTypeOrderBySortOrderAscCodeAsc(eventType);
        for (AchievementDefinition definition : definitions) {
            UserAchievement progress = userAchievementRepo.findByUserIdAndAchievementCode(userId, definition.code)
                    .orElseGet(() -> newProgress(userId, definition.code, happenedAt));
            // Once unlocked, keep the record stable: do not keep incrementing progress above requiredCount.
            if (progress.unlockedAt != null) {
                progress.lastEventAt = happenedAt;
                progress.updatedAt = happenedAt;
                userAchievementRepo.save(progress);
                continue;
            }
            progress.progressCount += 1;
            progress.lastEventAt = happenedAt;
            progress.updatedAt = happenedAt;
            if (progress.unlockedAt == null && progress.progressCount >= Math.max(1, definition.requiredCount)) {
                progress.unlockedAt = happenedAt;
            }
            userAchievementRepo.save(progress);
        }
    }

    private UserAchievement newProgress(String userId, String achievementCode, Instant happenedAt) {
        UserAchievement progress = new UserAchievement();
        progress.userId = userId;
        progress.achievementCode = achievementCode;
        progress.progressCount = 0;
        progress.createdAt = happenedAt;
        progress.updatedAt = happenedAt;
        return progress;
    }
}
