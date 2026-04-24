package com.thearena.model.response;

import java.time.Instant;
import java.util.Map;

public record BattleEventResponse(
        int turnNumber,
        String eventType,
        long seed,
        Map<String, Object> payload,
        Instant createdAt
) {
}
