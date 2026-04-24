package com.thearena.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thearena.exception.BadRequestException;
import com.thearena.model.combat.Accessory;
import com.thearena.model.combat.Contestant;
import com.thearena.model.combat.ContestantType;
import com.thearena.model.combat.Consumable;
import com.thearena.model.combat.Item;
import com.thearena.model.combat.ItemType;
import com.thearena.model.combat.Weapon;
import com.thearena.model.combat.WeaponHandMode;
import com.thearena.model.combat.Wearable;
import com.thearena.model.combat.WearableType;
import com.thearena.model.response.InventoryItemResponse;
import com.thearena.model.response.InventoryOverviewResponse;
import com.thearena.persistence.mysql.EquipSlot;
import com.thearena.persistence.mysql.InventoryBucket;
import com.thearena.persistence.mysql.InventoryItemEntity;
import com.thearena.persistence.mysql.InventoryItemRepository;
import com.thearena.persistence.mysql.UserAccountEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final AccountService accountService;
    private final InventoryItemRepository inventoryItemRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(
            AccountService accountService,
            InventoryItemRepository inventoryItemRepository,
            ObjectMapper objectMapper
    ) {
        this.accountService = accountService;
        this.inventoryItemRepository = inventoryItemRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Seeds default equipped gear + stash consumable when the account has no inventory rows yet.
     */
    @Transactional
    public void ensureStarterGear(String username) {
        UserAccountEntity account = accountService.getByUsername(username);
        if (inventoryItemRepository.countByAccountId(account.getId()) > 0) {
            return;
        }
        Instant now = Instant.now();
        Weapon sword = new Weapon("Training Sword", "Basic steel sword.", "", "", WeaponHandMode.SINGLE_HAND, 12, 3);
        Wearable armor = new Wearable("Leather Armor", "Starter body armor.", "", WearableType.ARMOR, 3, 1, "");
        Accessory charm = new Accessory("Lucky Charm", "Simple charm.", "", "lucky");
        Consumable potion = new Consumable("Small Potion", "heal", 20);

        saveEquipped(account.getId(), sword, EquipSlot.LEFT_HAND, now);
        saveEquipped(account.getId(), armor, EquipSlot.BODY, now);
        saveEquipped(account.getId(), charm, EquipSlot.ACCESSORY, now);
        saveStash(account.getId(), potion, now);
    }

    private void saveEquipped(Long accountId, Item item, EquipSlot slot, Instant now) {
        try {
            String json = objectMapper.writeValueAsString(toPayload(item));
            inventoryItemRepository.save(new InventoryItemEntity(
                    accountId,
                    item.getItemType().name(),
                    item.getName(),
                    json,
                    now,
                    InventoryBucket.EQUIPPED,
                    slot
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save starter item", e);
        }
    }

    private void saveStash(Long accountId, Item item, Instant now) {
        try {
            String json = objectMapper.writeValueAsString(toPayload(item));
            inventoryItemRepository.save(new InventoryItemEntity(
                    accountId,
                    item.getItemType().name(),
                    item.getName(),
                    json,
                    now,
                    InventoryBucket.STASH,
                    null
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save starter stash item", e);
        }
    }

    public void storeLoot(String username, Item item) {
        UserAccountEntity account = accountService.getByUsername(username);
        Map<String, Object> payload = toPayload(item);
        try {
            String json = objectMapper.writeValueAsString(payload);
            inventoryItemRepository.save(new InventoryItemEntity(
                    account.getId(),
                    item.getItemType().name(),
                    item.getName(),
                    json,
                    Instant.now(),
                    InventoryBucket.STASH,
                    null
            ));
        } catch (Exception ignored) {
            // Loot persistence should never break combat flow.
        }
    }

    @Transactional(readOnly = true)
    public InventoryOverviewResponse overview(String username) {
        UserAccountEntity account = accountService.getByUsername(username);
        List<InventoryItemResponse> stash = inventoryItemRepository
                .findByAccountIdAndBucketOrderByAcquiredAtDesc(account.getId(), InventoryBucket.STASH)
                .stream()
                .map(this::toResponse)
                .toList();
        List<InventoryItemResponse> equipped = inventoryItemRepository
                .findByAccountIdAndBucketOrderByAcquiredAtDesc(account.getId(), InventoryBucket.EQUIPPED)
                .stream()
                .map(this::toResponse)
                .toList();
        return new InventoryOverviewResponse(stash, equipped);
    }

    public List<InventoryItemResponse> listForUser(String username) {
        UserAccountEntity account = accountService.getByUsername(username);
        return inventoryItemRepository.findByAccountIdOrderByAcquiredAtDesc(account.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Builds the in-battle player from MySQL equipped rows. Missing slots use sensible defaults.
     */
    @Transactional(readOnly = true)
    public Contestant buildContestantFromEquipped(String username, int health) {
        UserAccountEntity account = accountService.getByUsername(username);
        Long aid = account.getId();
        Weapon left = findEquippedItem(aid, EquipSlot.LEFT_HAND).map(this::toWeapon).orElse(new Weapon("Bare Fists", "", "", "", WeaponHandMode.SINGLE_HAND, 1, 0));
        Weapon right = findEquippedItem(aid, EquipSlot.RIGHT_HAND).map(this::toWeapon).orElse(null);
        Wearable body = findEquippedItem(aid, EquipSlot.BODY).map(this::toWearable).orElse(null);
        Accessory accessory = findEquippedItem(aid, EquipSlot.ACCESSORY).map(this::toAccessory).orElse(null);
        List<Item> carried = inventoryItemRepository.findByAccountIdAndBucketOrderByAcquiredAtDesc(aid, InventoryBucket.STASH).stream()
                .filter(e -> ItemType.CONSUMABLE.name().equals(e.getItemType()))
                .limit(3)
                .map(this::toConsumable)
                .map(c -> (Item) c)
                .toList();
        return new Contestant(
                username,
                ContestantType.PLAYER,
                "Arena challenger",
                health,
                left,
                right,
                body,
                accessory,
                carried
        );
    }

    @Transactional
    public void equipFromStash(String username, long inventoryItemId, EquipSlot equipSlot) {
        UserAccountEntity account = accountService.getByUsername(username);
        InventoryItemEntity row = inventoryItemRepository.findByIdAndAccountId(inventoryItemId, account.getId())
                .orElseThrow(() -> new BadRequestException("ITEM_NOT_FOUND", "Inventory item not found."));
        if (row.getBucket() != InventoryBucket.STASH) {
            throw new BadRequestException("NOT_IN_STASH", "Only stash items can be equipped this way.");
        }
        validateSlotForItemType(row.getItemType(), equipSlot);

        inventoryItemRepository.findByAccountIdAndBucketAndEquipSlot(account.getId(), InventoryBucket.EQUIPPED, equipSlot)
                .ifPresent(previous -> {
                    previous.setBucket(InventoryBucket.STASH);
                    previous.setEquipSlot(null);
                    inventoryItemRepository.save(previous);
                });

        row.setBucket(InventoryBucket.EQUIPPED);
        row.setEquipSlot(equipSlot);
        inventoryItemRepository.save(row);
    }

    @Transactional
    public void scrap(String username, long inventoryItemId) {
        UserAccountEntity account = accountService.getByUsername(username);
        InventoryItemEntity row = inventoryItemRepository.findByIdAndAccountId(inventoryItemId, account.getId())
                .orElseThrow(() -> new BadRequestException("ITEM_NOT_FOUND", "Inventory item not found."));
        inventoryItemRepository.delete(row);
    }

    private void validateSlotForItemType(String itemType, EquipSlot slot) {
        ItemType type = ItemType.valueOf(itemType);
        switch (slot) {
            case LEFT_HAND, RIGHT_HAND -> {
                if (type != ItemType.WEAPON) {
                    throw new BadRequestException("INVALID_SLOT", "Hands only accept WEAPON items.");
                }
            }
            case BODY -> {
                if (type != ItemType.WEARABLE) {
                    throw new BadRequestException("INVALID_SLOT", "Body slot only accepts WEARABLE items.");
                }
            }
            case ACCESSORY -> {
                if (type != ItemType.ACCESSORY) {
                    throw new BadRequestException("INVALID_SLOT", "Accessory slot only accepts ACCESSORY items.");
                }
            }
            default -> throw new BadRequestException("INVALID_SLOT", "Unknown slot.");
        }
    }

    private Optional<InventoryItemEntity> findEquippedItem(Long accountId, EquipSlot slot) {
        return inventoryItemRepository.findByAccountIdAndBucketAndEquipSlot(accountId, InventoryBucket.EQUIPPED, slot);
    }

    private Weapon toWeapon(InventoryItemEntity entity) {
        Map<String, Object> p = readPayload(entity);
        WeaponHandMode hand = WeaponHandMode.valueOf(String.valueOf(p.getOrDefault("handMode", "SINGLE_HAND")));
        int phys = asInt(p, "physicalDamage", 1);
        int mag = asInt(p, "magicalDamage", 0);
        return new Weapon(
                entity.getItemName(),
                String.valueOf(p.getOrDefault("description", "")),
                String.valueOf(p.getOrDefault("image", "")),
                String.valueOf(p.getOrDefault("specialEffect", "")),
                hand,
                phys,
                mag
        );
    }

    private Wearable toWearable(InventoryItemEntity entity) {
        Map<String, Object> p = readPayload(entity);
        WearableType wt = WearableType.valueOf(String.valueOf(p.getOrDefault("wearableType", "ARMOR")));
        return new Wearable(
                entity.getItemName(),
                String.valueOf(p.getOrDefault("description", "")),
                String.valueOf(p.getOrDefault("image", "")),
                wt,
                asInt(p, "physicalReduction", 0),
                asInt(p, "magicalReduction", 0),
                String.valueOf(p.getOrDefault("specialDefense", ""))
        );
    }

    private Accessory toAccessory(InventoryItemEntity entity) {
        Map<String, Object> p = readPayload(entity);
        return new Accessory(
                entity.getItemName(),
                String.valueOf(p.getOrDefault("description", "")),
                String.valueOf(p.getOrDefault("image", "")),
                String.valueOf(p.getOrDefault("specialAccessory", p.getOrDefault("specialEffect", "")))
        );
    }

    private Consumable toConsumable(InventoryItemEntity entity) {
        Map<String, Object> p = readPayload(entity);
        return new Consumable(
                entity.getItemName(),
                String.valueOf(p.getOrDefault("description", "")),
                String.valueOf(p.getOrDefault("image", "")),
                String.valueOf(p.getOrDefault("specialEffect", "")),
                String.valueOf(p.getOrDefault("effect", "heal")),
                asInt(p, "potency", 10)
        );
    }

    private Map<String, Object> readPayload(InventoryItemEntity entity) {
        try {
            return objectMapper.readValue(entity.getPayloadJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private int asInt(Map<String, Object> map, String key, int fallback) {
        Object v = map.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        return fallback;
    }

    private InventoryItemResponse toResponse(InventoryItemEntity entity) {
        try {
            Map<String, Object> payload = objectMapper.readValue(entity.getPayloadJson(), new TypeReference<>() {});
            return new InventoryItemResponse(
                    entity.getId(),
                    entity.getItemType(),
                    entity.getItemName(),
                    entity.getBucket(),
                    entity.getEquipSlot(),
                    payload,
                    entity.getAcquiredAt()
            );
        } catch (Exception ex) {
            return new InventoryItemResponse(
                    entity.getId(),
                    entity.getItemType(),
                    entity.getItemName(),
                    entity.getBucket(),
                    entity.getEquipSlot(),
                    Map.of(),
                    entity.getAcquiredAt()
            );
        }
    }

    private Map<String, Object> toPayload(Item item) {
        if (item instanceof Weapon weapon) {
            return Map.of(
                    "description", weapon.getDescription(),
                    "image", weapon.getImage(),
                    "specialEffect", weapon.getSpecialEffect(),
                    "physicalDamage", weapon.getPhysicalDamage(),
                    "magicalDamage", weapon.getMagicalDamage(),
                    "handMode", weapon.getHandMode().name()
            );
        }
        if (item instanceof Wearable wearable) {
            return Map.of(
                    "description", wearable.getDescription(),
                    "image", wearable.getImage(),
                    "wearableType", wearable.getWearableType().name(),
                    "physicalReduction", wearable.getPhysicalReduction(),
                    "magicalReduction", wearable.getMagicalReduction(),
                    "specialDefense", wearable.getSpecialDefense()
            );
        }
        if (item instanceof Accessory accessory) {
            return Map.of(
                    "description", accessory.getDescription(),
                    "image", accessory.getImage(),
                    "specialAccessory", accessory.getSpecialAccessory()
            );
        }
        if (item instanceof Consumable consumable) {
            return Map.of(
                    "description", consumable.getDescription(),
                    "image", consumable.getImage(),
                    "specialEffect", consumable.getSpecialEffect(),
                    "effect", consumable.getEffect(),
                    "potency", consumable.getPotency()
            );
        }
        return Map.of();
    }
}
