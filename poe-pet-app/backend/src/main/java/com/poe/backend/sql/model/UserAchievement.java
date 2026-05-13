package com.poe.backend.sql.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Per-user progress row for a permanent achievement definition.
 *
 * This stays separate from raw activity events so the future UI can load a
 * compact progress view without replaying the entire event history each time.
 */
@Entity
@Table(name = "user_achievements", indexes = {
        @Index(name = "idx_user_achievements_user_updated", columnList = "user_id, updated_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_achievements_user_code", columnNames = { "user_id", "achievement_code" })
})
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Owning user id from Mongo auth/user documents. */
    @Column(name = "user_id", nullable = false, length = 100)
    public String userId;

    /** Foreign-like reference to {@link AchievementDefinition#code}. */
    @Column(name = "achievement_code", nullable = false, length = 100)
    public String achievementCode;

    /** Current count accumulated toward unlock. */
    @Column(name = "progress_count", nullable = false)
    public int progressCount;

    /** Set once the achievement becomes permanently unlocked. */
    @Column(name = "unlocked_at")
    public Instant unlockedAt;

    /** Most recent event time that affected the progress counter. */
    @Column(name = "last_event_at")
    public Instant lastEventAt;

    /** Row creation timestamp. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /** Row update timestamp. */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
