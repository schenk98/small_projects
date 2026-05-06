package com.poe.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("inventory_items")
public class InventoryItem {
    @Id
    public String id;
    public String userId;
    /** References {@link com.poe.backend.model.ShopItem#code}. */
    public String itemCode;
    /** Owned quantity for this user+itemCode pair. */
    public int quantity;
}
