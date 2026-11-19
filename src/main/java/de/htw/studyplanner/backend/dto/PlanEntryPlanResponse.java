package de.htw.studyplanner.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Planeintrag inklusive aktuell berechneter Voraussetzungsergebnisse.
 */
@Schema(description = "Planeintrag mit Voraussetzungen")
public record PlanEntryPlanResponse(

        @Schema(example = "1")
        Integer id,

        @Schema(example = "B34")
        String moduleCode,

        @Schema(example = "WS 2027")
        String semester,

        String offerCode,

        String courseCode,

        String courseTitle,

        @Schema(description = "false, sobald mindestens ein FEHLER vorhanden ist")
        boolean prerequisitesMet,

        List<PlanViolationResponse> violations) {
}
