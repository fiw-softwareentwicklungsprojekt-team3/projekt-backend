package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Verstoss gegen eine Modulvoraussetzung in der Planantwort.
 */
@Schema(description = "Verstoss gegen eine Modulvoraussetzung")
public record PlanViolationResponse(

        @Schema(description = "Schweregrad", example = "FEHLER")
        PruefSchwere severity,

        @Schema(description = "Code der Voraussetzung", example = "B11")
        String prerequisiteCode,

        @Schema(description = "Meldung", example = "Voraussetzung B11 fuer B34 ist nicht erfuellt.")
        String message) {
}
