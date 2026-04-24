package com.thearena.model.response;

import java.util.List;

public record InventoryOverviewResponse(
        List<InventoryItemResponse> stash,
        List<InventoryItemResponse> equipped
) {
}
