package com.thearena.model.request;

import com.thearena.persistence.mysql.EquipSlot;
import jakarta.validation.constraints.NotNull;

public record EquipItemRequest(
        @NotNull Long inventoryItemId,
        @NotNull EquipSlot equipSlot
) {
}
