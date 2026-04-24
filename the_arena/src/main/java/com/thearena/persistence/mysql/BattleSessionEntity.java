package com.thearena.persistence.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "battle_sessions")
public class BattleSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String battleId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private long seed;

    @Column(nullable = false)
    private int roundNumber;

    @Column(nullable = false)
    private String nextTurn;

    @Column(nullable = false)
    private String stateJson;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant endedAt;

    protected BattleSessionEntity() {
    }

    public BattleSessionEntity(
            String battleId,
            Long accountId,
            long seed,
            int roundNumber,
            String nextTurn,
            String stateJson,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.battleId = battleId;
        this.accountId = accountId;
        this.seed = seed;
        this.roundNumber = roundNumber;
        this.nextTurn = nextTurn;
        this.stateJson = stateJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getBattleId() {
        return battleId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public long getSeed() {
        return seed;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getNextTurn() {
        return nextTurn;
    }

    public void setNextTurn(String nextTurn) {
        this.nextTurn = nextTurn;
    }

    public String getStateJson() {
        return stateJson;
    }

    public void setStateJson(String stateJson) {
        this.stateJson = stateJson;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
