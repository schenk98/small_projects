package com.thearena.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thearena.exception.ForbiddenException;
import com.thearena.model.BattleSession;
import com.thearena.persistence.mysql.BattleSessionEntity;
import com.thearena.persistence.mysql.BattleSessionRepository;
import com.thearena.persistence.mysql.UserAccountEntity;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleSessionPersistenceService {
    private final BattleSessionRepository battleSessionRepository;
    private final ObjectMapper objectMapper;
    private final AccountService accountService;

    public BattleSessionPersistenceService(
            BattleSessionRepository battleSessionRepository,
            ObjectMapper objectMapper,
            AccountService accountService
    ) {
        this.battleSessionRepository = battleSessionRepository;
        this.objectMapper = objectMapper;
        this.accountService = accountService;
    }

    @Transactional
    public void create(String battleId, Long accountId, long seed, BattleSession session) {
        String snapshot = snapshotJson(session);
        BattleSessionEntity entity = new BattleSessionEntity(
                battleId, accountId, seed, 0, "PLAYER", snapshot, Instant.now(), Instant.now()
        );
        battleSessionRepository.save(entity);
    }

    @Transactional
    public void updateTurn(String battleId, int roundNumber, String nextTurn, BattleSession session) {
        BattleSessionEntity entity = battleSessionRepository.findByBattleId(battleId)
                .orElseThrow(() -> new IllegalArgumentException("Battle session metadata not found: " + battleId));
        entity.setRoundNumber(roundNumber);
        entity.setNextTurn(nextTurn);
        entity.setStateJson(snapshotJson(session));
        entity.setUpdatedAt(Instant.now());
        if (session.isFinished()) {
            entity.setEndedAt(Instant.now());
        }
        battleSessionRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public void assertOwnership(String battleId, String username) {
        BattleSessionEntity entity = battleSessionRepository.findByBattleId(battleId)
                .orElseThrow(() -> new IllegalArgumentException("Battle session metadata not found: " + battleId));
        UserAccountEntity account = accountService.getByUsername(username);
        if (!entity.getAccountId().equals(account.getId())) {
            throw new ForbiddenException("BATTLE_ACCESS_DENIED", "Access denied for battle: " + battleId);
        }
    }

    private String snapshotJson(BattleSession session) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "playerName", session.getPlayer().getName(),
                    "playerHealth", session.getPlayer().getHealth(),
                    "enemyName", session.getEnemy().getName(),
                    "enemyHealth", session.getEnemy().getHealth(),
                    "round", session.getRoundNumber(),
                    "finished", session.isFinished(),
                    "initialPlayerEquipment", session.getInitialPlayerEquipment(),
                    "initialEnemyEquipment", session.getInitialEnemyEquipment()
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize battle snapshot", e);
        }
    }
}
