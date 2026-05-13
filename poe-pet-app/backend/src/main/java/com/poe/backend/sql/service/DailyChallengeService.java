package com.poe.backend.sql.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.poe.backend.model.Wallet;
import com.poe.backend.repo.WalletRepo;
import com.poe.backend.sql.model.DailyChallengeDefinition;
import com.poe.backend.sql.model.UserDailyChallengeProgress;
import com.poe.backend.sql.repo.DailyChallengeDefinitionRepo;
import com.poe.backend.sql.repo.UserDailyChallengeProgressRepo;
import com.poe.backend.sql.view.DailyChallengeView;

/**
 * Generates and tracks the three shared daily challenges.
 *
 * Current product rules:
 * - the same three challenges are shared by all users for a given backend-local day
 * - the set is generated lazily on first access rather than by a separate cron
 * - rewards are granted automatically on first completion
 */
@Service
public class DailyChallengeService {
    private static final ZoneId CHALLENGE_ZONE = ZoneId.systemDefault();

    private final DailyChallengeDefinitionRepo dailyChallengeDefinitionRepo;
    private final UserDailyChallengeProgressRepo userDailyChallengeProgressRepo;
    private final WalletRepo walletRepo;

    public DailyChallengeService(
            DailyChallengeDefinitionRepo dailyChallengeDefinitionRepo,
            UserDailyChallengeProgressRepo userDailyChallengeProgressRepo,
            WalletRepo walletRepo) {
        this.dailyChallengeDefinitionRepo = dailyChallengeDefinitionRepo;
        this.userDailyChallengeProgressRepo = userDailyChallengeProgressRepo;
        this.walletRepo = walletRepo;
    }

    /** Load today's shared challenges plus the current user's progress rows. */
    public List<DailyChallengeView> getTodayForUser(String userId) {
        List<DailyChallengeDefinition> definitions = ensureChallengesForDate(LocalDate.now(CHALLENGE_ZONE));
        List<Long> ids = definitions.stream()
                .map(definition -> definition.id)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, UserDailyChallengeProgress> progressByDefinitionId = new HashMap<>();
        for (UserDailyChallengeProgress progress : userDailyChallengeProgressRepo.findByUserIdAndChallengeDefinitionIdIn(userId, ids)) {
            progressByDefinitionId.put(progress.challengeDefinitionId, progress);
        }
        return definitions.stream()
                .map(definition -> toView(definition, progressByDefinitionId.get(definition.id)))
                .toList();
    }

    /** Advance today's relevant challenge rows for one freshly-recorded activity event. */
    public void recordEvent(String userId, String eventType, Instant happenedAt, Map<String, Object> metadata) {
        List<DailyChallengeDefinition> definitions = ensureChallengesForDate(LocalDate.ofInstant(happenedAt, CHALLENGE_ZONE));
        for (DailyChallengeDefinition definition : definitions) {
            if (!matches(definition, eventType, metadata)) {
                continue;
            }
            updateProgress(userId, definition, happenedAt);
        }
    }

    private List<DailyChallengeDefinition> ensureChallengesForDate(LocalDate date) {
        List<DailyChallengeDefinition> existing = dailyChallengeDefinitionRepo.findByChallengeDateOrderBySlotOrderAsc(date);
        if (existing.size() == 3) {
            return existing;
        }
        if (!existing.isEmpty()) {
            dailyChallengeDefinitionRepo.deleteAll(existing);
        }

        Instant now = Instant.now();
        List<DailyChallengeDefinition> generated = new ArrayList<>();
        int slot = 1;
        for (ChallengeTemplate template : templatesForDate(date)) {
            DailyChallengeDefinition definition = new DailyChallengeDefinition();
            definition.challengeDate = date;
            definition.slotOrder = slot++;
            definition.challengeType = template.challengeType();
            definition.triggerEventType = template.triggerEventType();
            definition.matchValue = template.matchValue();
            definition.title = template.title();
            definition.description = template.description();
            definition.requiredCount = template.requiredCount();
            definition.rewardCoins = template.rewardCoins();
            definition.createdAt = now;
            definition.updatedAt = now;
            generated.add(dailyChallengeDefinitionRepo.save(definition));
        }
        return generated;
    }

    private void updateProgress(String userId, DailyChallengeDefinition definition, Instant happenedAt) {
        UserDailyChallengeProgress progress = userDailyChallengeProgressRepo
                .findByUserIdAndChallengeDefinitionId(userId, definition.id)
                .orElseGet(() -> createProgress(userId, definition.id, happenedAt));

        progress.lastEventAt = happenedAt;
        progress.updatedAt = Instant.now();
        progress.progressCount = Math.min(definition.requiredCount, progress.progressCount + 1);
        if (progress.completedAt == null && progress.progressCount >= definition.requiredCount) {
            progress.completedAt = happenedAt;
        }
        userDailyChallengeProgressRepo.save(progress);

        if (progress.completedAt != null && !progress.rewardGranted) {
            Wallet wallet = walletRepo.findByUserId(userId).orElseThrow();
            wallet.coins += definition.rewardCoins;
            walletRepo.save(wallet);
            progress.rewardGranted = true;
            progress.rewardGrantedAt = Instant.now();
            progress.updatedAt = progress.rewardGrantedAt;
            userDailyChallengeProgressRepo.save(progress);
        }
    }

    private UserDailyChallengeProgress createProgress(String userId, Long challengeDefinitionId, Instant happenedAt) {
        UserDailyChallengeProgress progress = new UserDailyChallengeProgress();
        progress.userId = userId;
        progress.challengeDefinitionId = challengeDefinitionId;
        progress.progressCount = 0;
        progress.rewardGranted = false;
        progress.createdAt = happenedAt;
        progress.updatedAt = happenedAt;
        return progress;
    }

    private boolean matches(DailyChallengeDefinition definition, String eventType, Map<String, Object> metadata) {
        if (!definition.triggerEventType.equals(eventType)) {
            return false;
        }
        String matchValue = definition.matchValue == null ? "" : definition.matchValue;
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        return switch (definition.challengeType) {
            case "FINISH_MINIGAME" -> matchValue.equals(asLowerString(safeMetadata.get("gameCode")));
            case "USE_CONSUMABLE" -> matchValue.equals(asLowerString(safeMetadata.get("itemCode")));
            default -> false;
        };
    }

    private DailyChallengeView toView(DailyChallengeDefinition definition, UserDailyChallengeProgress progress) {
        int requiredCount = Math.max(1, definition.requiredCount);
        int progressCount = progress != null ? progress.progressCount : 0;
        int progressPercent = Math.min(100, (int) Math.round(progressCount * 100.0 / requiredCount));
        return new DailyChallengeView(
                definition.id == null ? 0 : definition.id,
                definition.challengeDate,
                definition.slotOrder,
                definition.challengeType,
                definition.title,
                definition.description,
                definition.matchValue,
                requiredCount,
                progressCount,
                progressPercent,
                progress != null && progress.completedAt != null,
                progress != null ? progress.completedAt : null,
                definition.rewardCoins,
                progress != null && progress.rewardGranted,
                progress != null ? progress.rewardGrantedAt : null,
                progress != null ? progress.lastEventAt : null);
    }

    private List<ChallengeTemplate> templatesForDate(LocalDate date) {
        List<ChallengeTemplate> minigames = new ArrayList<>(List.of(
                new ChallengeTemplate(
                        "FINISH_MINIGAME",
                        "MINIGAME_FINISHED",
                        "higher_lower",
                        "Guessing streak",
                        "Finish Higher or Lower once today.",
                        1,
                        18),
                new ChallengeTemplate(
                        "FINISH_MINIGAME",
                        "MINIGAME_FINISHED",
                        "puzzle_swap",
                        "Puzzle refresh",
                        "Finish Puzzle Swap once today.",
                        1,
                        20),
                new ChallengeTemplate(
                        "FINISH_MINIGAME",
                        "MINIGAME_FINISHED",
                        "connect4_ai",
                        "Connect 4 duel",
                        "Finish Connect 4 AI once today.",
                        1,
                        20),
                new ChallengeTemplate(
                        "FINISH_MINIGAME",
                        "MINIGAME_FINISHED",
                        "minesweep_ai",
                        "Mine sweeper",
                        "Finish Minesweeper once today.",
                        1,
                        20),
                new ChallengeTemplate(
                        "FINISH_MINIGAME",
                        "MINIGAME_FINISHED",
                        "checkers_ai",
                        "Checkerboard clash",
                        "Finish Checkers vs AI once today.",
                        1,
                        20)));

        List<ChallengeTemplate> consumables = new ArrayList<>(List.of(
                new ChallengeTemplate(
                        "USE_CONSUMABLE",
                        "CONSUMABLE_USED",
                        "food_small_50",
                        "Meal time",
                        "Use Meal Pack +50 once today.",
                        1,
                        12),
                new ChallengeTemplate(
                        "USE_CONSUMABLE",
                        "CONSUMABLE_USED",
                        "food_boost_20_regen_10pct_10h",
                        "Boost snack",
                        "Use Boost Snack +20 once today.",
                        1,
                        12),
                new ChallengeTemplate(
                        "USE_CONSUMABLE",
                        "CONSUMABLE_USED",
                        "food_combo_10_energy_25pct",
                        "Quick bite",
                        "Use Quick Bite +10 once today.",
                        1,
                        12),
                new ChallengeTemplate(
                        "USE_CONSUMABLE",
                        "CONSUMABLE_USED",
                        "food_cozy_bites",
                        "Cozy treat",
                        "Use Cozy Bites once today.",
                        1,
                        12)));

        Random minigameRandom = new Random(date.toEpochDay());
        Random consumableRandom = new Random(date.toEpochDay() * 31 + 7);
        java.util.Collections.shuffle(minigames, minigameRandom);
        java.util.Collections.shuffle(consumables, consumableRandom);

        return List.of(minigames.get(0), minigames.get(1), consumables.get(0));
    }

    private String asLowerString(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    private record ChallengeTemplate(
            String challengeType,
            String triggerEventType,
            String matchValue,
            String title,
            String description,
            int requiredCount,
            int rewardCoins) {
    }
}
