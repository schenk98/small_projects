package com.thearena.controller;

import com.thearena.model.response.BattleEventResponse;
import com.thearena.model.response.BattleReplayResponse;
import com.thearena.model.response.BattleResultResponse;
import com.thearena.service.BattleHistoryService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/arena/history")
public class BattleHistoryController {
    private final BattleHistoryService battleHistoryService;

    public BattleHistoryController(BattleHistoryService battleHistoryService) {
        this.battleHistoryService = battleHistoryService;
    }

    @GetMapping("/{battleId}/result")
    public BattleResultResponse quickResult(@PathVariable("battleId") String battleId, Authentication authentication) {
        return battleHistoryService.quickResult(battleId, authentication.getName());
    }

    @PostMapping("/{battleId}/replay")
    public BattleReplayResponse fullReplay(@PathVariable("battleId") String battleId, Authentication authentication) {
        return battleHistoryService.fullReplay(battleId, authentication.getName());
    }

    @GetMapping("/{battleId}/turn/{turnNumber}")
    public List<BattleEventResponse> turnPoint(
            @PathVariable("battleId") String battleId,
            @PathVariable("turnNumber") int turnNumber,
            Authentication authentication
    ) {
        return battleHistoryService.eventsAtTurn(battleId, turnNumber, authentication.getName());
    }
}
