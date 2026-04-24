package com.thearena.controller;

import com.thearena.model.request.StartArenaRequest;
import com.thearena.model.request.SessionSnapshotRequest;
import com.thearena.model.request.TurnRequest;
import com.thearena.model.response.StartArenaResponse;
import com.thearena.model.response.TurnResponse;
import com.thearena.exception.BadRequestException;
import com.thearena.service.BattleService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/arena")
public class ArenaController {
    private final BattleService battleService;

    public ArenaController(BattleService battleService) {
        this.battleService = battleService;
    }

    @PostMapping("/start")
    public StartArenaResponse start(@Valid @RequestBody(required = false) StartArenaRequest request, Authentication authentication) {
        String authenticatedUser = authentication.getName();
        String requested = request == null ? null : request.playerName();
        if (requested != null && !requested.isBlank() && !requested.equals(authenticatedUser)) {
            throw new BadRequestException("PLAYER_NAME_MISMATCH", "playerName must match authenticated user.");
        }
        return battleService.startBattle(authenticatedUser, request);
    }

    @PatchMapping("/turn")
    public TurnResponse turn(@Valid @RequestBody TurnRequest request, Authentication authentication) {
        return battleService.processTurn(request.sessionId(), request.action(), authentication.getName());
    }

    @PostMapping("/snapshot")
    public Map<String, String> snapshot(@Valid @RequestBody SessionSnapshotRequest request, Authentication authentication) {
        battleService.snapshotAndCloseSession(request.sessionId(), authentication.getName());
        return Map.of("status", "snapshot_recorded");
    }

}
