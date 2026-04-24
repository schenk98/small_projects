package com.thearena.model.request;

import jakarta.validation.constraints.NotNull;

public record ScrapItemRequest(
        @NotNull Long inventoryItemId
) {
}
