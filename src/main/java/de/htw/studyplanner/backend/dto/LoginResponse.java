package de.htw.studyplanner.backend.dto;

/**
 * DTO für die Antwort auf eine Anmeldung eines Benutzers.
 *
 * @param token     ausgestelltes JWT
 * @param expiresIn Gültigkeit des Tokens in Sekunden
 * @param role      Rolle der angemeldeten Person
 */
public record LoginResponse(
        String token,
        long expiresIn,
        String role
) {
}