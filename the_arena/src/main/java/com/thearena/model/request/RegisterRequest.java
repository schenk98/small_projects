package com.thearena.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6, message = "password must have at least 6 characters") String password
) {
}
