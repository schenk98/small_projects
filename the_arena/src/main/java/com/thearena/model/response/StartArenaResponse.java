package com.thearena.model.response;

public record StartArenaResponse(
        String sessionId,
        String message,
        String playerName,
        int playerHealth,
        String playerWeapon,
        String enemyName,
        int enemyHealth,
        String enemyWeapon
) {
}
