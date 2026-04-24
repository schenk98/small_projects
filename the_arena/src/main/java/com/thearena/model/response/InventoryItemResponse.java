package com.thearena.model.response;

import com.thearena.persistence.mysql.EquipSlot;
import com.thearena.persistence.mysql.InventoryBucket;
import java.time.Instant;
import java.util.Map;

public record InventoryItemResponse(
        Long id,
        String itemType,
        String itemName,
        InventoryBucket bucket,
        EquipSlot equipSlot,
        Map<String, Object> payload,
        Instant acquiredAt
) {
}
