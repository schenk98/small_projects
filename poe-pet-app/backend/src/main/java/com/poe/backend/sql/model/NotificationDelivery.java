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
 * SQL record of one notification delivery attempt.
 *
 * This provides two useful behaviors:
 * - dedupe keys prevent repeated sends within the same logical delivery window
 * - audit rows make it easier to debug why a notification did or did not arrive
 */
@Entity
@Table(name = "notification_deliveries", indexes = {
        @Index(name = "idx_notification_deliveries_user_kind_created", columnList = "user_id, kind, created_at")
})
public class NotificationDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /** Owning user id from the main app. */
    @Column(name = "user_id", nullable = false, length = 100)
    public String userId;

    /** Notification kind such as LOW_HUNGER or DAILY_AI_SUMMARY. */
    @Column(nullable = false, length = 100)
    public String kind;

    /** Stable dedupe key for this logical delivery window. */
    @Column(name = "delivery_key", nullable = false, unique = true, length = 180)
    public String deliveryKey;

    /** Final email target used for the attempt. */
    @Column(name = "target_email", nullable = false, length = 255)
    public String targetEmail;

    /** Subject line sent to the SOAP service. */
    @Column(nullable = false, length = 255)
    public String subject;

    /** Short preview of the body, useful in logs/admin tooling. */
    @Column(name = "body_preview", nullable = false, length = 1000)
    public String bodyPreview;

    /** Whether the downstream SOAP service accepted the request. */
    @Column(nullable = false)
    public boolean success;

    /** Optional human-readable response or error message. */
    @Column(name = "response_message", length = 1000)
    public String responseMessage;

    /** When the attempt was recorded. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
