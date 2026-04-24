package com.thearena.model.request;

import jakarta.validation.constraints.NotBlank;

public record SessionSnapshotRequest(
        @NotBlank String sessionId
) {
}
