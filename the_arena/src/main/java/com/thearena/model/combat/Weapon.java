package com.thearena.model.combat;

public class Weapon extends Item {
    private final WeaponHandMode handMode;
    private final int physicalDamage;
    private final int magicalDamage;

    public Weapon(
            String name,
            String description,
            String image,
            String specialEffect,
            WeaponHandMode handMode,
            int physicalDamage,
            int magicalDamage
    ) {
        super(name, description, image, specialEffect, ItemType.WEAPON);
        this.handMode = handMode;
        this.physicalDamage = physicalDamage;
        this.magicalDamage = magicalDamage;
    }

    // Backward-compatible constructor for existing code paths.
    public Weapon(String name, int damage) {
        this(name, "", "", "", WeaponHandMode.SINGLE_HAND, damage, 0);
    }

    public int getDamage() {
        return physicalDamage + magicalDamage;
    }

    public WeaponHandMode getHandMode() {
        return handMode;
    }

    public int getPhysicalDamage() {
        return physicalDamage;
    }

    public int getMagicalDamage() {
        return magicalDamage;
    }
}
