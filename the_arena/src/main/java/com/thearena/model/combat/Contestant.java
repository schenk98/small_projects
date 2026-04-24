package com.thearena.model.combat;

import java.util.List;
import java.util.Objects;

/**
 * Shared combat object used for both player and enemy.
 */
public class Contestant {
    private final String name;
    private final ContestantType type;
    private final String description;
    private int health;
    private final Weapon leftHand;
    private final Weapon rightHand;
    private final Wearable bodyWear;
    private final Accessory accessory;
    private final List<Item> carriedItems;

    public Contestant(
            String name,
            ContestantType type,
            String description,
            int health,
            Weapon leftHand,
            Weapon rightHand,
            Wearable bodyWear,
            Accessory accessory,
            List<Item> carriedItems
    ) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.health = health;
        this.leftHand = leftHand;
        this.rightHand = rightHand;
        this.bodyWear = bodyWear;
        this.accessory = accessory;
        this.carriedItems = List.copyOf(carriedItems.stream().limit(3).toList());
    }

    // Backward-compatible constructor for existing callers.
    public Contestant(String name, int health, Weapon weapon, List<Wearable> wearables, List<Consumable> consumables) {
        this(
                name,
                ContestantType.ENEMY,
                "",
                health,
                weapon,
                null,
                wearables.isEmpty() ? null : wearables.getFirst(),
                null,
                consumables.stream().map(c -> (Item) c).toList()
        );
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public ContestantType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Weapon getLeftHand() {
        return leftHand;
    }

    public Weapon getRightHand() {
        return rightHand;
    }

    public Wearable getBodyWear() {
        return bodyWear;
    }

    public Accessory getAccessory() {
        return accessory;
    }

    public List<Item> getCarriedItems() {
        return carriedItems;
    }

    public Weapon primaryWeapon() {
        return leftHand != null ? leftHand : rightHand;
    }

    public int totalPhysicalReduction() {
        return bodyWear == null ? 0 : bodyWear.getPhysicalReduction();
    }

    public int totalMagicalReduction() {
        return bodyWear == null ? 0 : bodyWear.getMagicalReduction();
    }

    public boolean hasAccessoryToken(String token) {
        return accessory != null && Objects.equals(accessory.getSpecialAccessory(), token);
    }
}
