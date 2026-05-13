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
 * Immutable-style activity history row for the relational/event side of the app.
 *
 * The document database still owns the live gameplay state. This table stores
 * append-only history snapshots that are easier to query for activity feeds,
 * achievements, audits, and later analytics.
 */
@Entity
@Table(name = "activity_events", indexes = {
        @Index(name = "idx_activity_events_user_happened_at", columnList = "user_id, happened_at"),
        @Index(name = "idx_activity_events_type_happened_at", columnList = "event_type, happened_at")
})
public class ActivityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Owning user id from Mongo auth/user documents. */
    @Column(name = "user_id", nullable = false, length = 100)
    public String userId;

    /** Stable domain event code such as SHOP_PURCHASED or AI_CHAT_SENT. */
    @Column(name = "event_type", nullable = false, length = 100)
    public String eventType;

    /** Human-ish source bucket: auth, pet, shop, minigame, ai, etc. */
    @Column(nullable = false, length = 100)
    public String source;

    /** When the event happened on the backend. */
    @Column(name = "happened_at", nullable = false)
    public Instant happenedAt;

    /** Optional denormalized pet name snapshot for easier history rendering. */
    @Column(name = "pet_name", length = 64)
    public String petName;

    /** Optional denormalized species snapshot for later filtering/reporting. */
    @Column(name = "species_code", length = 32)
    public String speciesCode;

    /** Pet hunger after the action completed. */
    public Double hunger;

    /** Pet happiness after the action completed. */
    public Double happiness;

    /** Pet energy after the action completed. */
    public Double energy;

    /** Wallet coin balance after the action completed. */
    @Column(name = "coin_balance")
    public Integer coinBalance;

    /** Verbose JSON payload with action-specific details that do not fit fixed columns. */
    @Column(name = "metadata_json", nullable = false, columnDefinition = "text")
    public String metadataJson;
}
