package com.thearena.model.response;

public record TurnResponse(
        String sessionId,
        String action,
        String message,
        int playerHealth,
        int enemyHealth,
        boolean finished,
        String winner
) {
}
