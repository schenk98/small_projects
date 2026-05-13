package com.poe.backend.sql.model;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Shared daily challenge generated once per date and reused by every player.
 *
 * The current product choice is intentionally simple:
 * - exactly three challenges per day
 * - same three challenges for all users
 * - generated from a small template pool suitable for the current app scope
 */
@Entity
@Table(name = "daily_challenge_definitions", indexes = {
        @Index(name = "idx_daily_challenge_definitions_date", columnList = "challenge_date, slot_order")
})
public class DailyChallengeDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Local day bucket used by the backend when picking the current challenge set. */
    @Column(name = "challenge_date", nullable = false)
    public LocalDate challengeDate;

    /** Slot order from 1..3 for stable rendering. */
    @Column(name = "slot_order", nullable = false)
    public int slotOrder;

    /** Template type such as FINISH_MINIGAME or USE_CONSUMABLE. */
    @Column(name = "challenge_type", nullable = false, length = 60)
    public String challengeType;

    /** Activity event type that advances the challenge. */
    @Column(name = "trigger_event_type", nullable = false, length = 100)
    public String triggerEventType;

    /** Event metadata value used to match a specific minigame or item code. */
    @Column(name = "match_value", nullable = false, length = 120)
    public String matchValue;

    /** Player-facing challenge title. */
    @Column(nullable = false, length = 160)
    public String title;

    /** Short player-facing description. */
    @Column(nullable = false, length = 500)
    public String description;

    /** Number of matching actions required to finish the challenge. */
    @Column(name = "required_count", nullable = false)
    public int requiredCount;

    /** Coin reward granted automatically on first completion. */
    @Column(name = "reward_coins", nullable = false)
    public int rewardCoins;

    /** Audit timestamps for generation/debugging. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
