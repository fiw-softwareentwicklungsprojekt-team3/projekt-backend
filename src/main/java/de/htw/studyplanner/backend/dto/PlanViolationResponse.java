package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Verstoß gegen eine Modulvoraussetzung in der Planantwort ({@code GET /api/plan}, E30).
 *
 * @param severity         {@code FEHLER}: notwendige Voraussetzung fehlt oder zu wenige LP;
 *                         {@code WARNUNG}: empfohlene Voraussetzung fehlt oder LP-Lücke innerhalb der
 *                         Toleranz; {@code HINWEIS}: Voraussetzung liegt in einem vergangenen Semester,
 *                         das Ergebnis fehlt noch
 * @param prerequisiteCode Code des fehlenden Moduls, z. B. B11; {@code null} bei einer
 *                         LP-Voraussetzung (z. B. Fachpraktikum), dann nennt {@code message} die LP
 * @param message          lesbare Meldung für das Frontend
 */
@Schema(description = "Verstoß gegen eine Modulvoraussetzung")
public record PlanViolationResponse(

        @Schema(description = "FEHLER (notwendige Voraussetzung oder LP fehlen), WARNUNG (empfohlene "
                + "Voraussetzung fehlt oder LP-Lücke in der Toleranz), HINWEIS (Ergebnis ausstehend)",
                example = "FEHLER")
        PruefSchwere severity,

        @Schema(description = "Code des fehlenden Moduls; null bei einer LP-Voraussetzung",
                example = "B11", nullable = true)
        String prerequisiteCode,

        @Schema(description = "Meldung", example = "Voraussetzung B11 für B34 ist nicht erfüllt.")
        String message) {
}
