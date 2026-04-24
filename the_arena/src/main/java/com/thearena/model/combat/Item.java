package com.thearena.model.combat;

public abstract class Item {
    private final String name;
    private final String description;
    private final String image;
    private final String specialEffect;
    private final ItemType itemType;

    protected Item(String name, String description, String image, String specialEffect, ItemType itemType) {
        this.name = name;
        this.description = description;
        this.image = image;
        this.specialEffect = specialEffect;
        this.itemType = itemType;
    }

    public String getName() {
        return name;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public String getSpecialEffect() {
        return specialEffect;
    }
}
