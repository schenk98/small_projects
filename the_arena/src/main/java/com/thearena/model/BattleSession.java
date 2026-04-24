package com.thearena.model;

import com.thearena.model.combat.Contestant;
import java.util.Map;

public class BattleSession {
    private final String sessionId;
    private final Contestant player;
    private final Contestant enemy;
    private final long seed;
    /** Frozen at battle start for history/replay; not updated when live HP changes. */
    private final Map<String, Object> initialPlayerEquipment;
    private final Map<String, Object> initialEnemyEquipment;
    private int roundNumber;
    private boolean finished;

    public BattleSession(
            String sessionId,
            Contestant player,
            Contestant enemy,
            long seed,
            Map<String, Object> initialPlayerEquipment,
            Map<String, Object> initialEnemyEquipment
    ) {
        this.sessionId = sessionId;
        this.player = player;
        this.enemy = enemy;
        this.seed = seed;
        this.initialPlayerEquipment = Map.copyOf(initialPlayerEquipment);
        this.initialEnemyEquipment = Map.copyOf(initialEnemyEquipment);
        this.roundNumber = 0;
        this.finished = false;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Contestant getPlayer() {
        return player;
    }

    public Contestant getEnemy() {
        return enemy;
    }

    public long getSeed() {
        return seed;
    }

    public Map<String, Object> getInitialPlayerEquipment() {
        return initialPlayerEquipment;
    }

    public Map<String, Object> getInitialEnemyEquipment() {
        return initialEnemyEquipment;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void incrementRound() {
        this.roundNumber++;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
