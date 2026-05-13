package com.poe.backend.sql.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Per-user progress row for a generated daily challenge.
 *
 * A separate progress table keeps query-side rendering easy and lets the app
 * remember which rewards were already granted.
 */
@Entity
@Table(name = "user_daily_challenge_progress", indexes = {
        @Index(name = "idx_user_daily_challenge_progress_user_updated", columnList = "user_id, updated_at")
})
public class UserDailyChallengeProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Owning user id from the Mongo auth subsystem. */
    @Column(name = "user_id", nullable = false, length = 100)
    public String userId;

    /** Foreign-key-like reference to the generated daily challenge definition row. */
    @Column(name = "challenge_definition_id", nullable = false)
    public Long challengeDefinitionId;

    /** How many matching actions the player performed today. */
    @Column(name = "progress_count", nullable = false)
    public int progressCount;

    /** First time the challenge reached its required count. */
    @Column(name = "completed_at")
    public Instant completedAt;

    /** Whether the coin reward was already applied to the wallet. */
    @Column(name = "reward_granted", nullable = false)
    public boolean rewardGranted;

    /** When the reward was granted to the wallet. */
    @Column(name = "reward_granted_at")
    public Instant rewardGrantedAt;

    /** Last time a matching event advanced the challenge. */
    @Column(name = "last_event_at")
    public Instant lastEventAt;

    /** Audit timestamps. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
