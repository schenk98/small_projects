package com.thearena.service;

import com.thearena.model.combat.Accessory;
import com.thearena.model.combat.Consumable;
import com.thearena.model.combat.Contestant;
import com.thearena.model.combat.Item;
import com.thearena.model.combat.Weapon;
import com.thearena.model.combat.Wearable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Produces immutable JSON-friendly maps of a contestant's equipment at a point in time
 * (used at battle start so catalog edits do not rewrite history).
 */
@Service
public class ContestantLoadoutSnapshotService {

    /**
     * Deep snapshot of identity + equipment + starting HP. Live HP on the {@link Contestant}
     * may change later; this map stays fixed for the lifetime of the battle session.
     */
    public Map<String, Object> freezeAtBattleStart(Contestant contestant) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", contestant.getName());
        root.put("type", contestant.getType().name());
        root.put("description", contestant.getDescription());
        root.put("startingHealth", contestant.getHealth());
        Map<String, Object> slots = new LinkedHashMap<>();
        slots.put("leftHand", itemSnapshot(contestant.getLeftHand()));
        slots.put("rightHand", itemSnapshot(contestant.getRightHand()));
        slots.put("bodyWear", itemSnapshot(contestant.getBodyWear()));
        slots.put("accessory", itemSnapshot(contestant.getAccessory()));
        root.put("slots", slots);
        root.put(
                "carriedItems",
                contestant.getCarriedItems().stream().map(this::itemSnapshot).collect(Collectors.toList())
        );
        return Map.copyOf(root);
    }

    private Map<String, Object> itemSnapshot(Item item) {
        if (item == null) {
            return null;
        }
        if (item instanceof Weapon weapon) {
            Map<String, Object> m = baseItemFields(weapon);
            m.put("kind", "WEAPON");
            m.put("handMode", weapon.getHandMode().name());
            m.put("physicalDamage", weapon.getPhysicalDamage());
            m.put("magicalDamage", weapon.getMagicalDamage());
            return Map.copyOf(m);
        }
        if (item instanceof Wearable wearable) {
            Map<String, Object> m = baseItemFields(wearable);
            m.put("kind", "WEARABLE");
            m.put("wearableType", wearable.getWearableType().name());
            m.put("physicalReduction", wearable.getPhysicalReduction());
            m.put("magicalReduction", wearable.getMagicalReduction());
            m.put("specialDefense", wearable.getSpecialDefense());
            return Map.copyOf(m);
        }
        if (item instanceof Accessory accessory) {
            Map<String, Object> m = baseItemFields(accessory);
            m.put("kind", "ACCESSORY");
            m.put("specialAccessory", accessory.getSpecialAccessory());
            return Map.copyOf(m);
        }
        if (item instanceof Consumable consumable) {
            Map<String, Object> m = baseItemFields(consumable);
            m.put("kind", "CONSUMABLE");
            m.put("effect", consumable.getEffect());
            m.put("potency", consumable.getPotency());
            return Map.copyOf(m);
        }
        Map<String, Object> m = baseItemFields(item);
        m.put("kind", item.getItemType().name());
        return Map.copyOf(m);
    }

    private Map<String, Object> baseItemFields(Item item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemType", item.getItemType().name());
        m.put("name", item.getName());
        m.put("description", item.getDescription());
        m.put("image", item.getImage());
        m.put("specialEffect", item.getSpecialEffect());
        return m;
    }
}
