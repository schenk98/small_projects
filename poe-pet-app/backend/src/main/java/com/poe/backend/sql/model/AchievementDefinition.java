package com.poe.backend.sql.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalog row describing a permanent achievement.
 *
 * Definitions live in SQL because they are relational reference data: the same
 * achievement can be reused across many users and progressed via event counts.
 */
@Entity
@Table(name = "achievement_definitions")
public class AchievementDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Stable code used by code, migrations, and future frontend payloads. */
    @Column(nullable = false, unique = true, length = 100)
    public String code;

    /** Short player-facing title. */
    @Column(nullable = false, length = 120)
    public String title;

    /** Slightly longer explanation shown in achievements UI. */
    @Column(nullable = false, length = 500)
    public String description;

    /** High-level group such as pet, economy, minigame, or ai. */
    @Column(nullable = false, length = 60)
    public String category;

    /** Event code that should increment this achievement's progress. */
    @Column(name = "trigger_event_type", nullable = false, length = 100)
    public String triggerEventType;

    /** Number of matching events required to unlock it. */
    @Column(name = "required_count", nullable = false)
    public int requiredCount;

    /** Soft on/off flag so the catalog can evolve without deleting history. */
    @Column(nullable = false)
    public boolean active;

    /** UI-friendly ordering hint for stable lists. */
    @Column(name = "sort_order", nullable = false)
    public int sortOrder;

    /** Creation timestamp for debugging and seed visibility. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /** Last change timestamp for admin/seed updates. */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
