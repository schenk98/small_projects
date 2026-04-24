package com.thearena.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateContestantTemplateRequest(
        @NotBlank String name,
        String description,
        @Min(1) int baseHealth,
        /** Mongo {@code item_definitions._id} (24-char hex) or null to leave slot empty. */
        String leftHandItemId,
        String rightHandItemId,
        String bodyWearItemId,
        String accessoryItemId
) {
}
