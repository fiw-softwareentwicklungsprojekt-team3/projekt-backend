package de.htw.studyplanner.backend.dto;

/**
 * DTO für die Antwort auf eine Anmeldung eines Benutzers.
 */
public record LoginResponse(
        String token,
        long expiresIn,
        String role
) {
}