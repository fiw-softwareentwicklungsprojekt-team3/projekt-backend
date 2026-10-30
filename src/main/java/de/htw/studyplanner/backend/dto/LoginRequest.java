package de.htw.studyplanner.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email darf nicht leer sein ")
        @Email(message = "Email ist ungültig ")
        String email,

        @NotBlank(message = "Password darf nicht leer sein ")
        String password
) {
}