package com.poe.backend.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("minigame_sessions")
public class MinigameSession {
    @Id
    public String id;
    public String userId;
    public String gameCode;
    public int currentNumber;
    public int streak;
    public boolean active;
    public Instant startedAt;
}
