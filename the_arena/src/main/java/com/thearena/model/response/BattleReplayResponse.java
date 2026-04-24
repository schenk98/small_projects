package com.thearena.model.response;

import java.util.List;

public record BattleReplayResponse(
        String battleId,
        long seed,
        BattleEventResponse initialSnapshot,
        List<BattleEventResponse> turns,
        BattleEventResponse finalSnapshot
) {
}
