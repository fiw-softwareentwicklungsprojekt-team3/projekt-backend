package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Antwort nach erfolgreicher Registrierung. Das Passwort ist nicht enthalten.
 *
 * @param id            ID der angelegten Person
 * @param email         E-Mail-Adresse
 * @param rolle         Rolle, bei der Registrierung immer STUDIERENDE
 * @param startsemester Startsemester wie im Request, z. B. "WS 2025"
 */
@Schema(description = "Die angelegte Person")
public record RegisterResponse(
        @Schema(description = "ID der angelegten Person", example = "1")
        Integer id,

        @Schema(description = "E-Mail-Adresse", example = "studentin@example.com")
        String email,

        @Schema(description = "Rolle, bei der Registrierung immer STUDIERENDE", example = "STUDIERENDE")
        String rolle,

        @Schema(description = "Startsemester", example = "WS 2025")
        String startsemester
) {
}
