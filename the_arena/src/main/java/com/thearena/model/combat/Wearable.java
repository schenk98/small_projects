package com.thearena.model.combat;

public class Wearable extends Item {
    private final WearableType wearableType;
    private final int physicalReduction;
    private final int magicalReduction;
    private final String specialDefense;

    public Wearable(
            String name,
            String description,
            String image,
            WearableType wearableType,
            int physicalReduction,
            int magicalReduction,
            String specialDefense
    ) {
        super(name, description, image, "", ItemType.WEARABLE);
        this.wearableType = wearableType;
        this.physicalReduction = physicalReduction;
        this.magicalReduction = magicalReduction;
        this.specialDefense = specialDefense;
    }

    // Backward-compatible constructor.
    public Wearable(String name, WearableType wearableType, int defenseBonus) {
        this(name, "", "", wearableType, defenseBonus, 0, "");
    }

    public WearableType getWearableType() {
        return wearableType;
    }

    public int getDefenseBonus() {
        return physicalReduction + magicalReduction;
    }

    public int getPhysicalReduction() {
        return physicalReduction;
    }

    public int getMagicalReduction() {
        return magicalReduction;
    }

    public String getSpecialDefense() {
        return specialDefense;
    }
}
