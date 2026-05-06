package com.poe.backend.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("minigame_sessions")
public class MinigameSession {
    @Id
    public String id;
    public String userId;
    /** Minigame code, e.g. {@code higher_lower}. */
    public String gameCode;
    /** Higher/Lower: current card/number shown to the user. */
    public int currentNumber;
    /** Higher/Lower: consecutive correct guesses. */
    public int streak;
    public boolean active;
    public Instant startedAt;
}
