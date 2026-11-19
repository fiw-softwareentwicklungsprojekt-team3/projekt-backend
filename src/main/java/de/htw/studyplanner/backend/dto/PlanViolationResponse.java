package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Verstoß gegen eine Modulvoraussetzung in der Planantwort.
 */
@Schema(description = "Verstoß gegen eine Modulvoraussetzung")
public record PlanViolationResponse(

        @Schema(description = "Schweregrad", example = "FEHLER")
        PruefSchwere severity,

        @Schema(description = "Code der Voraussetzung", example = "B11")
        String prerequisiteCode,

        @Schema(description = "Meldung", example = "Voraussetzung B11 für B34 ist nicht erfüllt.")
        String message) {
}
