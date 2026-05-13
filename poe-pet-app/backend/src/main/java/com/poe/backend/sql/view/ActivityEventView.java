package com.poe.backend.sql.view;

import java.time.Instant;
import java.util.Map;

/**
 * Player-facing recent activity row returned by the progress API.
 *
 * It keeps the important fixed fields explicit while still allowing event-type
 * specific metadata to flow through as a generic map.
 */
public record ActivityEventView(
        long id,
        String eventType,
        String source,
        Instant happenedAt,
        String petName,
        String speciesCode,
        Double hunger,
        Double happiness,
        Double energy,
        Integer coinBalance,
        Map<String, Object> details) {
}
