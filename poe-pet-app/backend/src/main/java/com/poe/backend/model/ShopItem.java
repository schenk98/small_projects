package com.poe.backend.model;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("shop_items")
public class ShopItem {
    @Id
    public String id;
    public String code;
    public String type;
    public String shopSection;
    public String name;
    public String description;
    public int priceCoins;
    public boolean oneTimePurchase;
    public boolean active;
    /**
     * When false, item is hidden from the shop catalog (still in DB for staging). When null or true, shown if {@link #active}.
     */
    public Boolean playerVisible;
    public String effectKey;
    public List<Map<String, Object>> effects;
}
