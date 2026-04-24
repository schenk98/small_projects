package com.thearena.service;

import com.thearena.exception.BadRequestException;
import com.thearena.model.combat.Accessory;
import com.thearena.model.combat.Contestant;
import com.thearena.model.combat.ContestantType;
import com.thearena.model.combat.Consumable;
import com.thearena.model.combat.Item;
import com.thearena.model.combat.Weapon;
import com.thearena.model.combat.WeaponHandMode;
import com.thearena.model.combat.Wearable;
import com.thearena.model.combat.WearableType;
import com.thearena.persistence.mongo.ItemDefinitionDocument;
import com.thearena.persistence.mongo.ItemDefinitionRepository;
import com.thearena.persistence.mongo.MonsterTemplateDocument;
import com.thearena.persistence.mongo.MonsterTemplateRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class MonsterCatalogService {
    private final MonsterTemplateRepository monsterTemplateRepository;
    private final ItemDefinitionRepository itemDefinitionRepository;

    public MonsterCatalogService(
            MonsterTemplateRepository monsterTemplateRepository,
            ItemDefinitionRepository itemDefinitionRepository
    ) {
        this.monsterTemplateRepository = monsterTemplateRepository;
        this.itemDefinitionRepository = itemDefinitionRepository;
    }

    public Contestant randomMonster(long seed) {
        Random random = new Random(seed);
        List<MonsterTemplateDocument> templates;
        try {
            templates = monsterTemplateRepository.findAll();
        } catch (RuntimeException ex) {
            return fallbackMonster(random);
        }
        if (templates.isEmpty()) {
            return fallbackMonster(random);
        }

        MonsterTemplateDocument template = templates.get(random.nextInt(templates.size()));
        return buildContestantFromTemplate(template, template.getName(), ContestantType.ENEMY, template.getBaseHealth());
    }

    /**
     * Random monster template with equipment slots rolled from {@code item_definitions} where possible
     * (falls back to template ids when the catalog is empty).
     */
    public Contestant randomMonsterWithRandomizedLoadout(long seed) {
        Random rng = new Random(seed);
        List<MonsterTemplateDocument> templates;
        try {
            templates = monsterTemplateRepository.findAll();
        } catch (RuntimeException ex) {
            return fallbackMonster(rng);
        }
        if (templates.isEmpty()) {
            return fallbackMonster(rng);
        }
        MonsterTemplateDocument template = templates.get(rng.nextInt(templates.size()));
        List<ItemDefinitionDocument> all = safeAllItemDefs();
        List<ItemDefinitionDocument> weapons = all.stream().filter(d -> "weapon".equalsIgnoreCase(d.getItemType())).toList();
        List<ItemDefinitionDocument> wearables = all.stream().filter(d -> "wearable".equalsIgnoreCase(d.getItemType())).toList();
        List<ItemDefinitionDocument> accessories = all.stream().filter(d -> "accessory".equalsIgnoreCase(d.getItemType())).toList();
        List<ItemDefinitionDocument> consumables = all.stream().filter(d -> "consumable".equalsIgnoreCase(d.getItemType())).toList();

        Weapon left = !weapons.isEmpty()
                ? toWeapon(weapons.get(rng.nextInt(weapons.size())))
                : resolveWeapon(template.getLeftHandItemId()).orElse(new Weapon("Claws", 7));
        Weapon right = !weapons.isEmpty() && rng.nextBoolean()
                ? toWeapon(weapons.get(rng.nextInt(weapons.size())))
                : resolveWeapon(template.getRightHandItemId()).orElse(null);
        Wearable body = !wearables.isEmpty()
                ? toWearable(wearables.get(rng.nextInt(wearables.size())))
                : resolveWearable(template.getBodyWearItemId()).orElse(null);
        Accessory accessory = !accessories.isEmpty()
                ? toAccessory(accessories.get(rng.nextInt(accessories.size())))
                : resolveAccessory(template.getAccessoryItemId()).orElse(null);

        List<Item> carried = new ArrayList<>();
        for (int i = 0; i < 3 && !consumables.isEmpty(); i++) {
            carried.add(toConsumableFromDef(consumables.get(rng.nextInt(consumables.size()))));
        }
        if (carried.isEmpty()) {
            carried.add(new Consumable("Minor Elixir", "heal", 10));
        }
        return new Contestant(
                template.getName(),
                ContestantType.ENEMY,
                template.getDescription() == null ? "" : template.getDescription(),
                template.getBaseHealth(),
                left,
                right,
                body,
                accessory,
                carried
        );
    }

    public Contestant contestantFromTemplateId(String templateMongoId, String overrideName, ContestantType type, int healthOrZeroForTemplate) {
        MonsterTemplateDocument template = monsterTemplateRepository.findById(templateMongoId)
                .orElseThrow(() -> new BadRequestException("TEMPLATE_NOT_FOUND", "Contestant template not found: " + templateMongoId));
        String name = overrideName != null && !overrideName.isBlank() ? overrideName : template.getName();
        int hp = healthOrZeroForTemplate > 0 ? healthOrZeroForTemplate : template.getBaseHealth();
        return buildContestantFromTemplate(template, name, type, hp);
    }

    private Contestant buildContestantFromTemplate(
            MonsterTemplateDocument template,
            String name,
            ContestantType type,
            int health
    ) {
        Weapon left = resolveWeapon(template.getLeftHandItemId()).orElse(new Weapon("Claws", 7));
        Weapon right = resolveWeapon(template.getRightHandItemId()).orElse(null);
        Wearable body = resolveWearable(template.getBodyWearItemId()).orElse(null);
        Accessory accessory = resolveAccessory(template.getAccessoryItemId()).orElse(null);
        return new Contestant(
                name,
                type,
                template.getDescription() == null ? "" : template.getDescription(),
                health,
                left,
                right,
                body,
                accessory,
                List.of(new Consumable("Minor Elixir", "heal", 10))
        );
    }

    private List<ItemDefinitionDocument> safeAllItemDefs() {
        try {
            return itemDefinitionRepository.findAll();
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private Consumable toConsumableFromDef(ItemDefinitionDocument doc) {
        Map<String, Object> a = doc.getAttributes() == null ? Map.of() : doc.getAttributes();
        return new Consumable(
                doc.getName(),
                doc.getDescription() == null ? "" : doc.getDescription(),
                doc.getImage() == null ? "" : doc.getImage(),
                doc.getSpecialEffect() == null ? "" : doc.getSpecialEffect(),
                String.valueOf(a.getOrDefault("effect", "heal")),
                asInt(a, "potency", 10)
        );
    }

    private Contestant fallbackMonster(Random random) {
        List<Contestant> defaults = List.of(
                new Contestant("Goblin", ContestantType.ENEMY, "Sneaky cave creature.", 70, new Weapon("Rusty Dagger", 8), null, null, null, List.of()),
                new Contestant("Skeleton", ContestantType.ENEMY, "Undead fighter.", 80, new Weapon("Bone Spear", 10), null, null, null, List.of()),
                new Contestant(
                        "Orc",
                        ContestantType.ENEMY,
                        "Brutal frontline warrior.",
                        95,
                        new Weapon("Heavy Axe", 12),
                        null,
                        new Wearable("Hide Vest", WearableType.ARMOR, 1),
                        null,
                        List.of()
                )
        );
        return defaults.get(random.nextInt(defaults.size()));
    }

    private java.util.Optional<Weapon> resolveWeapon(String itemRef) {
        return resolveItem(itemRef)
                .filter(doc -> "weapon".equalsIgnoreCase(doc.getItemType()))
                .map(this::toWeapon);
    }

    private java.util.Optional<Wearable> resolveWearable(String itemRef) {
        return resolveItem(itemRef)
                .filter(doc -> "wearable".equalsIgnoreCase(doc.getItemType()))
                .map(this::toWearable);
    }

    private java.util.Optional<Accessory> resolveAccessory(String itemRef) {
        return resolveItem(itemRef)
                .filter(doc -> "accessory".equalsIgnoreCase(doc.getItemType()))
                .map(this::toAccessory);
    }

    /**
     * Resolves {@code item_definitions} by Mongo id (24 hex) or, for legacy documents, by unique name.
     */
    private java.util.Optional<ItemDefinitionDocument> resolveItem(String ref) {
        if (ref == null || ref.isBlank()) {
            return java.util.Optional.empty();
        }
        String trimmed = ref.trim();
        if (trimmed.length() == 24 && trimmed.matches("[a-fA-F0-9]+")) {
            return itemDefinitionRepository.findById(trimmed);
        }
        return itemDefinitionRepository.findByName(trimmed);
    }

    private Weapon toWeapon(ItemDefinitionDocument doc) {
        Map<String, Object> a = doc.getAttributes() != null ? doc.getAttributes() : Map.of();
        int physical = asInt(a, "physicalDamage", 8);
        int magical = asInt(a, "magicalDamage", 0);
        WeaponHandMode handMode = WeaponHandMode.valueOf(String.valueOf(a.getOrDefault("handMode", "SINGLE_HAND")));
        return new Weapon(doc.getName(), doc.getDescription(), doc.getImage(), doc.getSpecialEffect(), handMode, physical, magical);
    }

    private Wearable toWearable(ItemDefinitionDocument doc) {
        Map<String, Object> a = doc.getAttributes() != null ? doc.getAttributes() : Map.of();
        return new Wearable(
                doc.getName(),
                doc.getDescription(),
                doc.getImage(),
                WearableType.ARMOR,
                asInt(a, "physicalReduction", 2),
                asInt(a, "magicalReduction", 1),
                String.valueOf(a.getOrDefault("specialDefense", ""))
        );
    }

    private Accessory toAccessory(ItemDefinitionDocument doc) {
        return new Accessory(
                doc.getName(),
                doc.getDescription() == null ? "" : doc.getDescription(),
                doc.getImage() == null ? "" : doc.getImage(),
                doc.getSpecialEffect() == null ? "" : doc.getSpecialEffect()
        );
    }

    private int asInt(Map<String, Object> attributes, String key, int fallback) {
        Object value = attributes.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }
}
