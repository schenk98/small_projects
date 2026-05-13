package com.poe.backend.sql.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe.backend.sql.model.ActivityEvent;
import com.poe.backend.sql.model.AchievementDefinition;
import com.poe.backend.sql.model.UserAchievement;
import com.poe.backend.sql.repo.ActivityEventRepo;
import com.poe.backend.sql.repo.AchievementDefinitionRepo;
import com.poe.backend.sql.repo.UserAchievementRepo;
import com.poe.backend.sql.view.ActivityEventView;
import com.poe.backend.sql.view.AchievementProgressView;
import com.poe.backend.sql.view.ProgressSummaryView;

/**
 * Read-only query service for the first achievements/history player UI.
 *
 * The write side is already event-based. This service assembles a UI-friendly
 * projection from the SQL tables without pulling more responsibilities into the
 * already-large {@code AppService}.
 */
@Service
public class ProgressQueryService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ActivityEventRepo activityEventRepo;
    private final AchievementDefinitionRepo achievementDefinitionRepo;
    private final UserAchievementRepo userAchievementRepo;
    private final DailyChallengeService dailyChallengeService;
    private final ObjectMapper objectMapper;

    public ProgressQueryService(
            ActivityEventRepo activityEventRepo,
            AchievementDefinitionRepo achievementDefinitionRepo,
            UserAchievementRepo userAchievementRepo,
            DailyChallengeService dailyChallengeService,
            ObjectMapper objectMapper) {
        this.activityEventRepo = activityEventRepo;
        this.achievementDefinitionRepo = achievementDefinitionRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.dailyChallengeService = dailyChallengeService;
        this.objectMapper = objectMapper;
    }

    /** Return the current user's achievement progress plus their newest activity rows. */
    public ProgressSummaryView getSummary(String userId) {
        return new ProgressSummaryView(
                dailyChallengeService.getTodayForUser(userId),
                loadAchievements(userId),
                loadRecentActivity(userId));
    }

    private List<AchievementProgressView> loadAchievements(String userId) {
        Map<String, UserAchievement> progressByCode = new HashMap<>();
        for (UserAchievement progress : userAchievementRepo.findByUserIdOrderByUpdatedAtDesc(userId)) {
            progressByCode.put(progress.achievementCode, progress);
        }
        return achievementDefinitionRepo.findByActiveTrueOrderBySortOrderAscCodeAsc().stream()
                .map(definition -> toAchievementView(definition, progressByCode.get(definition.code)))
                .toList();
    }

    private AchievementProgressView toAchievementView(AchievementDefinition definition, UserAchievement progress) {
        int requiredCount = Math.max(1, definition.requiredCount);
        boolean unlocked = progress != null && progress.unlockedAt != null;
        int rawProgressCount = progress != null ? progress.progressCount : 0;
        int progressCount = unlocked ? Math.min(requiredCount, rawProgressCount) : rawProgressCount;
        int progressPercent = Math.min(100, (int) Math.round(progressCount * 100.0 / requiredCount));
        return new AchievementProgressView(
                definition.code,
                definition.title,
                definition.description,
                definition.category,
                requiredCount,
                progressCount,
                progressPercent,
                unlocked,
                progress != null ? progress.unlockedAt : null,
                progress != null ? progress.lastEventAt : null);
    }

    private List<ActivityEventView> loadRecentActivity(String userId) {
        return activityEventRepo.findTop20ByUserIdOrderByHappenedAtDescIdDesc(userId).stream()
                .map(this::toActivityView)
                .toList();
    }

    private ActivityEventView toActivityView(ActivityEvent event) {
        return new ActivityEventView(
                event.id == null ? 0 : event.id,
                event.eventType,
                event.source,
                event.happenedAt,
                event.petName,
                event.speciesCode,
                event.hunger,
                event.happiness,
                event.energy,
                event.coinBalance,
                parseMetadata(event.metadataJson));
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("raw", metadataJson);
        }
    }
}
