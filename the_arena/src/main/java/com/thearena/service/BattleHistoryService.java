package com.thearena.service;

import com.thearena.exception.BadRequestException;
import com.thearena.model.BattleSession;
import com.thearena.model.response.BattleEventResponse;
import com.thearena.model.response.BattleReplayResponse;
import com.thearena.model.response.BattleResultResponse;
import com.thearena.persistence.mongo.BattleEventDocument;
import com.thearena.persistence.mongo.BattleEventRepository;
import com.thearena.persistence.mongo.BattleProjectionDocument;
import com.thearena.persistence.mongo.BattleProjectionRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BattleHistoryService {
    private final BattleEventRepository battleEventRepository;
    private final BattleProjectionRepository battleProjectionRepository;
    private final BattleSessionPersistenceService battleSessionPersistenceService;

    public BattleHistoryService(
            BattleEventRepository battleEventRepository,
            BattleProjectionRepository battleProjectionRepository,
            BattleSessionPersistenceService battleSessionPersistenceService
    ) {
        this.battleEventRepository = battleEventRepository;
        this.battleProjectionRepository = battleProjectionRepository;
        this.battleSessionPersistenceService = battleSessionPersistenceService;
    }

    public void recordInitialSnapshot(BattleSession session) {
        Map<String, Object> initialPayload = new LinkedHashMap<>();
        initialPayload.put("playerName", session.getPlayer().getName());
        initialPayload.put("playerHealth", session.getPlayer().getHealth());
        initialPayload.put("enemyName", session.getEnemy().getName());
        initialPayload.put("enemyHealth", session.getEnemy().getHealth());
        initialPayload.put("initialPlayerEquipment", session.getInitialPlayerEquipment());
        initialPayload.put("initialEnemyEquipment", session.getInitialEnemyEquipment());
        save(session, 0, "INITIAL_SNAPSHOT", initialPayload);

        Map<String, Object> projectionSnapshot = new LinkedHashMap<>();
        projectionSnapshot.put("playerHealth", session.getPlayer().getHealth());
        projectionSnapshot.put("enemyHealth", session.getEnemy().getHealth());
        projectionSnapshot.put("initialPlayerEquipment", session.getInitialPlayerEquipment());
        projectionSnapshot.put("initialEnemyEquipment", session.getInitialEnemyEquipment());
        saveProjection(session.getSessionId(), "IN_PROGRESS", null, 0, projectionSnapshot);
    }

    public void recordTurn(BattleSession session, String action, String message) {
        save(session, session.getRoundNumber(), "TURN", Map.of(
                "action", action,
                "message", message,
                "playerHealth", session.getPlayer().getHealth(),
                "enemyHealth", session.getEnemy().getHealth()
        ));
        saveProjection(session.getSessionId(), "IN_PROGRESS", null, session.getRoundNumber(), Map.of(
                "playerHealth", session.getPlayer().getHealth(),
                "enemyHealth", session.getEnemy().getHealth()
        ));
    }

    public void recordFinalSnapshot(BattleSession session, String winner) {
        save(session, session.getRoundNumber(), "FINAL_SNAPSHOT", Map.of(
                "winner", winner,
                "playerHealth", session.getPlayer().getHealth(),
                "enemyHealth", session.getEnemy().getHealth()
        ));
        saveProjection(session.getSessionId(), "FINISHED", winner, session.getRoundNumber(), Map.of(
                "winner", winner,
                "playerHealth", session.getPlayer().getHealth(),
                "enemyHealth", session.getEnemy().getHealth()
        ));
    }

    public BattleResultResponse quickResult(String battleId, String username) {
        battleSessionPersistenceService.assertOwnership(battleId, username);
        var projection = battleProjectionRepository.findByBattleId(battleId).orElse(null);
        if (projection != null) {
            return new BattleResultResponse(
                    battleId,
                    projection.getStatus(),
                    projection.getWinner(),
                    projection.getLatestTurn(),
                    projection.getSnapshot()
            );
        }
        List<BattleEventDocument> events = battleEventRepository.findByBattleIdOrderByTurnNumberAsc(battleId);
        if (events.isEmpty()) {
            throw new BadRequestException("BATTLE_HISTORY_NOT_FOUND", "No battle history found for id: " + battleId);
        }

        BattleEventDocument finalSnapshot = events.stream()
                .filter(e -> "FINAL_SNAPSHOT".equals(e.getEventType()))
                .max(Comparator.comparingInt(BattleEventDocument::getTurnNumber))
                .orElse(null);

        if (finalSnapshot == null) {
            return new BattleResultResponse(battleId, "IN_PROGRESS", null, latestTurn(events), Map.of());
        }

        Object winner = finalSnapshot.getPayload().get("winner");
        return new BattleResultResponse(
                battleId,
                "FINISHED",
                winner == null ? null : winner.toString(),
                finalSnapshot.getTurnNumber(),
                finalSnapshot.getPayload()
        );
    }

    public BattleReplayResponse fullReplay(String battleId, String username) {
        battleSessionPersistenceService.assertOwnership(battleId, username);
        List<BattleEventDocument> events = battleEventRepository.findByBattleIdOrderByTurnNumberAsc(battleId);
        if (events.isEmpty()) {
            throw new BadRequestException("BATTLE_HISTORY_NOT_FOUND", "No battle history found for id: " + battleId);
        }

        BattleEventResponse initial = events.stream()
                .filter(e -> "INITIAL_SNAPSHOT".equals(e.getEventType()))
                .map(this::toResponse)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("INITIAL_SNAPSHOT_MISSING", "Initial snapshot missing for battle: " + battleId));

        List<BattleEventResponse> turns = events.stream()
                .filter(e -> "TURN".equals(e.getEventType()))
                .map(this::toResponse)
                .toList();

        BattleEventResponse finalSnapshot = events.stream()
                .filter(e -> "FINAL_SNAPSHOT".equals(e.getEventType()))
                .map(this::toResponse)
                .findFirst()
                .orElse(null);

        return new BattleReplayResponse(battleId, initial.seed(), initial, turns, finalSnapshot);
    }

    public List<BattleEventResponse> eventsAtTurn(String battleId, int turnNumber, String username) {
        battleSessionPersistenceService.assertOwnership(battleId, username);
        List<BattleEventDocument> events = battleEventRepository.findByBattleIdAndTurnNumberOrderByCreatedAtAsc(battleId, turnNumber);
        if (events.isEmpty()) {
            throw new BadRequestException("TURN_EVENTS_NOT_FOUND", "No events found for battleId=" + battleId + " turnNumber=" + turnNumber);
        }
        return events.stream().map(this::toResponse).toList();
    }

    private BattleEventResponse toResponse(BattleEventDocument event) {
        return new BattleEventResponse(
                event.getTurnNumber(),
                event.getEventType(),
                event.getSeed(),
                event.getPayload(),
                event.getCreatedAt()
        );
    }

    private int latestTurn(List<BattleEventDocument> events) {
        return events.stream().map(BattleEventDocument::getTurnNumber).max(Integer::compareTo).orElse(0);
    }

    private void save(BattleSession session, int turn, String type, Map<String, Object> payload) {
        try {
            battleEventRepository.save(new BattleEventDocument(
                    session.getSessionId(), turn, type, session.getSeed(), payload, Instant.now()
            ));
        } catch (RuntimeException ignored) {
            // Keep gameplay alive if Mongo is temporarily unavailable.
        }
    }

    private void saveProjection(String battleId, String status, String winner, int latestTurn, Map<String, Object> snapshot) {
        try {
            battleProjectionRepository.save(new BattleProjectionDocument(
                    battleId,
                    status,
                    winner,
                    latestTurn,
                    snapshot,
                    Instant.now()
            ));
        } catch (RuntimeException ignored) {
            // Projection storage is optimization only.
        }
    }
}
