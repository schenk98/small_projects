package com.thearena.service;

import com.thearena.persistence.mongo.CombatLogDocument;
import com.thearena.persistence.mongo.CombatLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class CombatLogService {
    private final CombatLogRepository combatLogRepository;

    public CombatLogService(CombatLogRepository combatLogRepository) {
        this.combatLogRepository = combatLogRepository;
    }

    public void log(String sessionId, String event) {
        try {
            combatLogRepository.save(new CombatLogDocument(sessionId, event, Instant.now()));
        } catch (RuntimeException ignored) {
            // For local PoC we keep the battle flow alive even when Mongo is unavailable.
        }
    }
}
