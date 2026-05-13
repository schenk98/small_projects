package com.poe.backend.sql.service;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe.backend.model.PetState;
import com.poe.backend.model.Wallet;
import com.poe.backend.sql.model.ActivityEvent;
import com.poe.backend.sql.repo.ActivityEventRepo;

/**
 * Persists verbose activity history into PostgreSQL.
 *
 * The writes are intentionally best-effort for this phase. If history storage
 * fails, the main gameplay flow should still succeed while the team is
 * gradually rolling SQL-backed features into an existing Mongo-first project.
 */
@Service
public class ActivityHistoryService {
    private static final Logger log = LoggerFactory.getLogger(ActivityHistoryService.class);

    private final ActivityEventRepo activityEventRepo;
    private final AchievementProgressService achievementProgressService;
    private final DailyChallengeService dailyChallengeService;
    private final ObjectMapper objectMapper;

    public ActivityHistoryService(
            ActivityEventRepo activityEventRepo,
            AchievementProgressService achievementProgressService,
            DailyChallengeService dailyChallengeService,
            ObjectMapper objectMapper) {
        this.activityEventRepo = activityEventRepo;
        this.achievementProgressService = achievementProgressService;
        this.dailyChallengeService = dailyChallengeService;
        this.objectMapper = objectMapper;
    }

    /** Record one append-only activity row and fan it out into achievement progress. */
    public void record(String userId, String eventType, String source, PetState pet, Wallet wallet, Map<String, Object> metadata) {
        Instant happenedAt = Instant.now();
        try {
            ActivityEvent event = new ActivityEvent();
            event.userId = userId;
            event.eventType = eventType;
            event.source = source;
            event.happenedAt = happenedAt;
            if (pet != null) {
                event.petName = pet.name;
                event.speciesCode = pet.speciesCode;
                event.hunger = pet.hunger;
                event.happiness = pet.happiness;
                event.energy = pet.energy;
            }
            if (wallet != null) {
                event.coinBalance = wallet.coins;
            }
            event.metadataJson = toJson(metadata);
            activityEventRepo.save(event);
            achievementProgressService.recordEvent(userId, eventType, happenedAt);
            dailyChallengeService.recordEvent(userId, eventType, happenedAt, metadata);
        } catch (Exception e) {
            log.warn("Activity history write failed for user={} eventType={}: {}", userId, eventType, e.getMessage());
        }
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException e) {
            return "{\"serializationError\":true}";
        }
    }
}
