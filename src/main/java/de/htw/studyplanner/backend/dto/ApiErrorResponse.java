package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Beschreibt das Standard-Fehlerformat von Spring Boot, z. B. bei 404 oder ungültigen Filterwerten.
 *
 * <p>Diese Klasse wird nur für die API-Dokumentation genutzt: Spring Boot erzeugt die Antwort
 * selbst. Dass {@code message} gefüllt ist, liegt an {@code server.error.include-message=always}
 * in den {@code application.properties}.
 *
 * @param timestamp Zeitpunkt des Fehlers (ISO 8601)
 * @param status    HTTP-Statuscode
 * @param error     Standard-Bezeichnung des Statuscodes
 * @param message   verständliche Fehlerbeschreibung auf Deutsch
 * @param path      Pfad der Anfrage ohne Host
 */
@Schema(description = "Standard-Fehlerformat")
public record ApiErrorResponse(
        @Schema(example = "2026-10-27T09:00:00.000+00:00")
        String timestamp,

        @Schema(example = "404")
        int status,

        @Schema(example = "Not Found")
        String error,

        @Schema(example = "Modul mit dem Code B99 wurde nicht gefunden.")
        String message,

        @Schema(example = "/api/modules/B99")
        String path) {
}
