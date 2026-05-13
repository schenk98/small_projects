package com.poe.backend.model;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("shop_items")
public class ShopItem {
    @Id
    public String id;
    /** Stable identifier used by purchase/inventory (e.g. {@code COIN_MULT_20P}). */
    public String code;
    /** Item kind: CONSUMABLE, COSMETIC, or SPECIES. */
    public String type;
    /** Optional UI grouping label for the shop page (frontend may use it for sections). */
    public String shopSection;
    /** Display name. */
    public String name;
    /** Display description shown in shop UI. */
    public String description;
    /** Coin price charged on purchase. */
    public int priceCoins;
    /** Legacy flag; cosmetics are enforced as one-time by ownership checks. */
    public boolean oneTimePurchase;
    public boolean active;
    /**
     * When false, item is hidden from the shop catalog (still in DB for staging). When null or true, shown if {@link #active}.
     */
    public Boolean playerVisible;
    /** Groups timed effects so subsequent uses can reset/overwrite them safely. */
    public String effectKey;
    /** List of effects interpreted by {@code AppService.applyEffects/applyNonTimedEffects}. */
    public List<Map<String, Object>> effects;
}
