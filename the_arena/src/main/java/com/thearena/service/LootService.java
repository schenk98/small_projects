package com.thearena.service;

import com.thearena.model.combat.Accessory;
import com.thearena.model.combat.Consumable;
import com.thearena.model.combat.Item;
import com.thearena.model.combat.LootStorageType;
import com.thearena.model.combat.Weapon;
import com.thearena.model.combat.WeaponHandMode;
import com.thearena.model.combat.Wearable;
import com.thearena.model.combat.WearableType;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class LootService {
    private final List<Weapon> weapons = List.of(
            new Weapon("Training Sword", "Starter sword.", "", "", WeaponHandMode.SINGLE_HAND, 12, 3),
            new Weapon("Hunter Bow", "Long-range bow.", "", "critical", WeaponHandMode.TWO_HAND, 10, 2),
            new Weapon("War Hammer", "Heavy crushing weapon.", "", "", WeaponHandMode.TWO_HAND, 17, 1)
    );
    private final List<Wearable> wearables = List.of(
            new Wearable("Leather Armor", "Basic body armor.", "", WearableType.ARMOR, 3, 1, ""),
            new Wearable("Steel Cuirass", "High physical defense.", "", WearableType.ARMOR, 5, 0, ""),
            new Wearable("Mystic Robe", "Magic-resistant robe.", "", WearableType.ARMOR, 1, 4, "thorns")
    );
    private final List<Accessory> accessories = List.of(
            new Accessory("Lucky Charm", "Adds tiny attack bonus.", "", "lucky"),
            new Accessory("Ruby Ring", "Adds fire affinity.", "", "burning"),
            new Accessory("Sage Pendant", "Calms magical flow.", "", "")
    );
    private final List<Consumable> consumables = List.of(
            new Consumable("Small Potion", "heal", 20),
            new Consumable("Energy Draught", "stamina", 15),
            new Consumable("Iron Skin Tonic", "defense_boost", 2)
    );

    public Item rollLoot(LootStorageType storageType, long seed, int turnNumber) {
        Random rng = new Random(seed + turnNumber * 31L + storageType.ordinal());
        int roll = rng.nextInt(100);
        return switch (storageType) {
            case CHEST -> rollChest(roll, rng);
            case CORPSE -> rollCorpse(roll, rng);
            case BOX -> rollBox(roll, rng);
        };
    }

    private Item rollChest(int roll, Random rng) {
        if (roll < 60) {
            return weapons.get(rng.nextInt(weapons.size()));
        }
        if (roll < 85) {
            return wearables.get(rng.nextInt(wearables.size()));
        }
        if (roll < 93) {
            return accessories.get(rng.nextInt(accessories.size()));
        }
        return consumables.get(rng.nextInt(consumables.size()));
    }

    private Item rollCorpse(int roll, Random rng) {
        if (roll < 60) {
            return wearables.get(rng.nextInt(wearables.size()));
        }
        if (roll < 85) {
            return weapons.get(rng.nextInt(weapons.size()));
        }
        if (roll < 93) {
            return accessories.get(rng.nextInt(accessories.size()));
        }
        return consumables.get(rng.nextInt(consumables.size()));
    }

    private Item rollBox(int roll, Random rng) {
        if (roll < 60) {
            return consumables.get(rng.nextInt(consumables.size()));
        }
        if (roll < 85) {
            return wearables.get(rng.nextInt(wearables.size()));
        }
        if (roll < 93) {
            return accessories.get(rng.nextInt(accessories.size()));
        }
        return weapons.get(rng.nextInt(weapons.size()));
    }
}
