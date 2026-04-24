package com.thearena.model.combat;

public class Consumable extends Item {
    private final String effect;
    private final int potency;

    public Consumable(String name, String description, String image, String specialEffect, String effect, int potency) {
        super(name, description, image, specialEffect, ItemType.CONSUMABLE);
        this.effect = effect;
        this.potency = potency;
    }

    public Consumable(String name, String effect, int potency) {
        this(name, "", "", "", effect, potency);
    }

    public String getEffect() {
        return effect;
    }

    public int getPotency() {
        return potency;
    }
}
