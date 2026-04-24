package com.thearena.persistence.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "monster_templates")
public class MonsterTemplateDocument {
    @Id
    private String id;
    private String name;
    private String description;
    private int baseHealth;
    /** References {@link ItemDefinitionDocument#getId()} (Mongo ObjectId as hex string). */
    private String leftHandItemId;
    private String rightHandItemId;
    private String bodyWearItemId;
    private String accessoryItemId;

    public MonsterTemplateDocument() {
    }

    public MonsterTemplateDocument(
            String name,
            String description,
            int baseHealth,
            String leftHandItemId,
            String rightHandItemId,
            String bodyWearItemId,
            String accessoryItemId
    ) {
        this.name = name;
        this.description = description;
        this.baseHealth = baseHealth;
        this.leftHandItemId = leftHandItemId;
        this.rightHandItemId = rightHandItemId;
        this.bodyWearItemId = bodyWearItemId;
        this.accessoryItemId = accessoryItemId;
    }

    public String getName() {
        return name;
    }

    public int getBaseHealth() {
        return baseHealth;
    }

    public String getLeftHandItemId() {
        return leftHandItemId;
    }

    public String getRightHandItemId() {
        return rightHandItemId;
    }

    public String getBodyWearItemId() {
        return bodyWearItemId;
    }

    public String getAccessoryItemId() {
        return accessoryItemId;
    }

    public String getDescription() {
        return description;
    }
}
