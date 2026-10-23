package de.htw.studyplanner.backend.dto;

public record LoginResponse(
        String token,
        long exiresIn,
        String email
) {
}