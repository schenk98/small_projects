package com.thearena.persistence.mongo;

import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "item_definitions")
public class ItemDefinitionDocument {
    @Id
    private String id;
    private String itemType;
    private String name;
    private String description;
    private String image;
    private String specialEffect;
    private Map<String, Object> attributes;

    public ItemDefinitionDocument() {
    }

    public ItemDefinitionDocument(
            String itemType,
            String name,
            String description,
            String image,
            String specialEffect,
            Map<String, Object> attributes
    ) {
        this.itemType = itemType;
        this.name = name;
        this.description = description;
        this.image = image;
        this.specialEffect = specialEffect;
        this.attributes = attributes;
    }

    public String getId() { return id; }
    public String getItemType() { return itemType; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImage() { return image; }
    public String getSpecialEffect() { return specialEffect; }
    public Map<String, Object> getAttributes() { return attributes; }
}
