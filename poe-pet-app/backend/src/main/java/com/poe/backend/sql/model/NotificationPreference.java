package com.poe.backend.sql.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Email/notification toggles that fit relational storage better than the main
 * pet state document.
 *
 * The first planned toggles are low-hunger reminders and daily AI summaries.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Owning user id from Mongo auth/user documents. */
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    public String userId;

    /** Whether the user wants low-hunger email reminders. */
    @Column(name = "low_hunger_enabled", nullable = false)
    public boolean lowHungerEnabled;

    /** Whether the user wants a daily AI-generated summary email. */
    @Column(name = "daily_ai_summary_enabled", nullable = false)
    public boolean dailyAiSummaryEnabled;

    /** Row creation timestamp. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /** Last update timestamp. */
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
