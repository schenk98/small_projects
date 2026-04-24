package com.thearena.persistence.mongo;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "battle_events")
public class BattleEventDocument {
    @Id
    private String id;
    private String battleId;
    private int turnNumber;
    private String eventType;
    private long seed;
    private Map<String, Object> payload;
    private Instant createdAt;

    public BattleEventDocument() {
    }

    public BattleEventDocument(
            String battleId,
            int turnNumber,
            String eventType,
            long seed,
            Map<String, Object> payload,
            Instant createdAt
    ) {
        this.battleId = battleId;
        this.turnNumber = turnNumber;
        this.eventType = eventType;
        this.seed = seed;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public String getBattleId() {
        return battleId;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public long getSeed() {
        return seed;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
