package com.thearena.model.request;

/**
 * @param playerName        optional override; must match authenticated user when set.
 * @param mode              {@code RANDOM} (default) or {@code DUEL}.
 * @param playerTemplateId  Mongo {@code monster_templates._id} for DUEL (left fighter uses account username).
 * @param enemyTemplateId   Mongo {@code monster_templates._id} for DUEL.
 */
public record StartArenaRequest(
        String playerName,
        String mode,
        String playerTemplateId,
        String enemyTemplateId
) {
}
