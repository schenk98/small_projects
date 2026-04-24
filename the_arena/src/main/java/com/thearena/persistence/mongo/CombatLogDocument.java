package com.thearena.persistence.mongo;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "combat_logs")
public class CombatLogDocument {
    @Id
    private String id;
    private String sessionId;
    private String event;
    private Instant createdAt;

    public CombatLogDocument() {
    }

    public CombatLogDocument(String sessionId, String event, Instant createdAt) {
        this.sessionId = sessionId;
        this.event = event;
        this.createdAt = createdAt;
    }
}
