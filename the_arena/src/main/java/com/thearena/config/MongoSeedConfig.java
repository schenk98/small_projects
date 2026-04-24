package com.thearena.config;

import com.thearena.persistence.mongo.ItemDefinitionDocument;
import com.thearena.persistence.mongo.MonsterTemplateDocument;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Configuration
public class MongoSeedConfig {

    @Bean
    CommandLineRunner seedMongo(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                upsertItem(
                        mongoTemplate,
                        "weapon",
                        "Training Sword",
                        "Starter balanced weapon.",
                        "",
                        "",
                        Map.of("handMode", "SINGLE_HAND", "physicalDamage", 12, "magicalDamage", 3, "rarity", "common")
                );
                upsertItem(
                        mongoTemplate,
                        "weapon",
                        "Rusty Dagger",
                        "Light blade often used by goblins.",
                        "",
                        "critical",
                        Map.of("handMode", "SINGLE_HAND", "physicalDamage", 8, "magicalDamage", 0, "rarity", "common")
                );
                upsertItem(
                        mongoTemplate,
                        "weapon",
                        "Bone Spear",
                        "Undead-crafted spear.",
                        "",
                        "",
                        Map.of("handMode", "TWO_HAND", "physicalDamage", 10, "magicalDamage", 1, "rarity", "uncommon")
                );
                upsertItem(
                        mongoTemplate,
                        "weapon",
                        "Heavy Axe",
                        "Brutal chopping weapon.",
                        "",
                        "burning",
                        Map.of("handMode", "TWO_HAND", "physicalDamage", 14, "magicalDamage", 0, "rarity", "rare")
                );
                upsertItem(
                        mongoTemplate,
                        "wearable",
                        "Leather Armor",
                        "Flexible starter armor.",
                        "",
                        "",
                        Map.of("physicalReduction", 3, "magicalReduction", 1, "specialDefense", "")
                );
                upsertItem(
                        mongoTemplate,
                        "wearable",
                        "Tough Hide",
                        "Natural monster defense.",
                        "",
                        "",
                        Map.of("physicalReduction", 2, "magicalReduction", 0, "specialDefense", "")
                );
                upsertItem(
                        mongoTemplate,
                        "wearable",
                        "Iron Plating",
                        "Heavy body protection.",
                        "",
                        "",
                        Map.of("physicalReduction", 4, "magicalReduction", 1, "specialDefense", "thorns")
                );
                upsertItem(
                        mongoTemplate,
                        "accessory",
                        "Lucky Charm",
                        "Slightly boosts luck in battle.",
                        "",
                        "lucky",
                        Map.of()
                );
                upsertItem(
                        mongoTemplate,
                        "accessory",
                        "Ring of Focus",
                        "Improves combat rhythm.",
                        "",
                        "",
                        Map.of()
                );
                upsertItem(
                        mongoTemplate,
                        "consumable",
                        "Minor Elixir",
                        "Common battle draught.",
                        "",
                        "",
                        Map.of("effect", "heal", "potency", 10)
                );
                upsertItem(
                        mongoTemplate,
                        "consumable",
                        "Small Potion",
                        "Restores a chunk of health.",
                        "",
                        "",
                        Map.of("effect", "heal", "potency", 20)
                );

                String rustyDaggerId = itemIdByName(mongoTemplate, "Rusty Dagger");
                String boneSpearId = itemIdByName(mongoTemplate, "Bone Spear");
                String heavyAxeId = itemIdByName(mongoTemplate, "Heavy Axe");
                String toughHideId = itemIdByName(mongoTemplate, "Tough Hide");
                String leatherArmorId = itemIdByName(mongoTemplate, "Leather Armor");
                String ironPlatingId = itemIdByName(mongoTemplate, "Iron Plating");
                String luckyCharmId = itemIdByName(mongoTemplate, "Lucky Charm");
                String ringOfFocusId = itemIdByName(mongoTemplate, "Ring of Focus");

                upsertMonster(mongoTemplate, "Goblin", "Fast melee raider.", 70, rustyDaggerId, null, toughHideId, luckyCharmId);
                upsertMonster(mongoTemplate, "Skeleton", "Undead with disciplined strikes.", 80, boneSpearId, null, leatherArmorId, null);
                upsertMonster(mongoTemplate, "Orc", "Heavy frontline bruiser.", 95, heavyAxeId, null, ironPlatingId, ringOfFocusId);
            } catch (RuntimeException ignored) {
                // Mongo is optional for local quick start; app should still boot.
            }
        };
    }

    private void upsertMonster(
            MongoTemplate mongoTemplate,
            String name,
            String description,
            int baseHealth,
            String leftHandItemId,
            String rightHandItemId,
            String bodyWearItemId,
            String accessoryItemId
    ) {
        Query query = Query.query(Criteria.where("name").is(name));
        Update update = new Update()
                .set("name", name)
                .set("description", description)
                .set("baseHealth", baseHealth)
                .set("leftHandItemId", leftHandItemId)
                .set("rightHandItemId", rightHandItemId)
                .set("bodyWearItemId", bodyWearItemId)
                .set("accessoryItemId", accessoryItemId)
                .unset("weaponName")
                .unset("weaponDamage")
                .unset("leftHandItem")
                .unset("rightHandItem")
                .unset("bodyWearItem")
                .unset("accessoryItem");
        mongoTemplate.upsert(query, update, MonsterTemplateDocument.class);
    }

    private String itemIdByName(MongoTemplate mongoTemplate, String name) {
        ItemDefinitionDocument found = mongoTemplate.findOne(Query.query(Criteria.where("name").is(name)), ItemDefinitionDocument.class);
        return found == null ? null : found.getId();
    }

    private void upsertItem(
            MongoTemplate mongoTemplate,
            String itemType,
            String name,
            String description,
            String image,
            String specialEffect,
            Map<String, Object> attributes
    ) {
        Query query = Query.query(Criteria.where("name").is(name));
        Update update = new Update()
                .set("itemType", itemType)
                .set("name", name)
                .set("description", description)
                .set("image", image)
                .set("specialEffect", specialEffect)
                .set("attributes", attributes);
        mongoTemplate.upsert(query, update, ItemDefinitionDocument.class);
    }
}
