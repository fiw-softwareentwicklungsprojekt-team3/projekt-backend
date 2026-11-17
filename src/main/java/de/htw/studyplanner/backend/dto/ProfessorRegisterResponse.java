package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Antwort nach erfolgreicher Registrierung einer Professor:in. Das Passwort ist nicht enthalten.
 */
@Schema(description = "Das angelegte Konto")
public record ProfessorRegisterResponse(
        @Schema(description = "ID des angelegten Kontos", example = "42")
        Integer id,

        @Schema(description = "E-Mail-Adresse", example = "professorin@htw-berlin.de")
        String email,

        @Schema(description = "Rolle, hier immer PROFESSOR_IN", example = "PROFESSOR_IN")
        String role
) {
}