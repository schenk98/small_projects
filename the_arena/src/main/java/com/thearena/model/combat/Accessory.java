package com.thearena.model.combat;

public class Accessory extends Item {
    private final String specialAccessory;

    public Accessory(String name, String description, String image, String specialAccessory) {
        super(name, description, image, "", ItemType.ACCESSORY);
        this.specialAccessory = specialAccessory;
    }

    public String getSpecialAccessory() {
        return specialAccessory;
    }
}
