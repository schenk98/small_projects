package com.poe.backend.model;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("minigames")
public class MinigameConfig {
    @Id
    public String id;
    public String code;
    public String name;
    /** Shown in the minigames hub; optional on legacy DB rows. */
    public String description;
    public int energyCost;
    public boolean active;
    public Map<String, Object> rewardStrategy;
    public Map<String, Object> happinessImpactStrategy;
}
