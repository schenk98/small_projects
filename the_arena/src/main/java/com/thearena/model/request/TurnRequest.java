package com.thearena.model.request;

import jakarta.validation.constraints.NotBlank;

public record TurnRequest(
        @NotBlank(message = "sessionId is required")
        String sessionId,
        @NotBlank(message = "action is required")
        String action
) {
}
