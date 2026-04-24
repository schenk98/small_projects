package com.thearena.persistence.mongo;

import java.time.Instant;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("battle_projections")
public class BattleProjectionDocument {
    @Id
    private String id;
    private String battleId;
    private String status;
    private String winner;
    private int latestTurn;
    private Map<String, Object> snapshot;
    private Instant updatedAt;

    protected BattleProjectionDocument() {}

    public BattleProjectionDocument(
            String battleId,
            String status,
            String winner,
            int latestTurn,
            Map<String, Object> snapshot,
            Instant updatedAt
    ) {
        this.battleId = battleId;
        this.status = status;
        this.winner = winner;
        this.latestTurn = latestTurn;
        this.snapshot = snapshot;
        this.updatedAt = updatedAt;
    }

    public String getBattleId() { return battleId; }
    public String getStatus() { return status; }
    public String getWinner() { return winner; }
    public int getLatestTurn() { return latestTurn; }
    public Map<String, Object> getSnapshot() { return snapshot; }
}
