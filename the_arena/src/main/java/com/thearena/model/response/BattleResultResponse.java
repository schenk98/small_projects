package com.thearena.model.response;

import java.util.Map;

public record BattleResultResponse(
        String battleId,
        String status,
        String winner,
        int finalTurnNumber,
        Map<String, Object> finalState
) {
}
