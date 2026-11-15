package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Antwort von {@code GET /api/me}: wer ist gerade angemeldet.
 *
 * <p>Der Passwort-Hash ist hier bewusst nicht enthalten und darf auch nie ergänzt
 * werden. Das Feld heißt {@code role} (englisch), passend zur Entscheidung E29 (D1).
 *
 * @param id            nutzende_id der angemeldeten Person
 * @param email         E-Mail-Adresse
 * @param role          Rolle, STUDIERENDE oder PROFESSOR_IN
 * @param startSemester Startsemester im Format "WS JJJJ"; bei PROFESSOR_IN null
 */
@Schema(description = "Die angemeldete Person")
public record MeResponse(

        @Schema(description = "ID der angemeldeten Person", example = "12")
        Integer id,

        @Schema(description = "E-Mail-Adresse", example = "studentin@example.com")
        String email,

        @Schema(description = "Rolle der Person", example = "STUDIERENDE")
        String role,

        @Schema(description = "Startsemester der Studierenden, bei PROFESSOR_IN null",
                example = "WS 2026", nullable = true)
        String startSemester
) {
}
