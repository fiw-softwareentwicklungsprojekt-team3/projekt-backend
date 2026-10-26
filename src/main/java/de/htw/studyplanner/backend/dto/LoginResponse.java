package de.htw.studyplanner.backend.dto;

public record LoginResponse(
        String token,
        long expiresIn,
        String role
) {
}