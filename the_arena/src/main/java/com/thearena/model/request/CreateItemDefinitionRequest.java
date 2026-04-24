package com.thearena.model.request;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CreateItemDefinitionRequest(
        @NotBlank String itemType,
        @NotBlank String name,
        String description,
        String image,
        String specialEffect,
        Map<String, Object> attributes
) {
}
