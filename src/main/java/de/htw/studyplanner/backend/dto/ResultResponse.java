package de.htw.studyplanner.backend.dto;

import java.math.BigDecimal;

import de.htw.studyplanner.backend.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eine Leistung in der Antwort von {@code GET /api/results} und {@code PUT /api/results/{moduleCode}}.
 *
 * @param moduleCode  Modulcode, z. B. B11
 * @param status      BELEGT, BESTANDEN oder NICHT_BESTANDEN
 * @param semester    Semester als "WS JJJJ" / "SS JJJJ", oder null
 * @param grade       Note oder null
 * @param attempts    gespeicherte Versuchszahl ({@code anzahl_versuche})
 * @param offerCode   gewähltes Wahlangebot oder null
 * @param courseCode  Veranstaltungsnummer oder null
 * @param courseTitle Veranstaltungstitel oder null
 */
@Schema(description = "Leistung in einem Modul")
public record ResultResponse(

        @Schema(example = "B11")
        String moduleCode,

        @Schema(example = "NICHT_BESTANDEN")
        Status status,

        @Schema(example = "WS 2026", nullable = true)
        String semester,

        @Schema(example = "5.0", nullable = true)
        BigDecimal grade,

        @Schema(description = "Versuchszahl; bei NICHT_BESTANDEN der Versuch, in dem nicht bestanden wurde",
                example = "1")
        int attempts,

        @Schema(nullable = true)
        String offerCode,

        @Schema(nullable = true)
        String courseCode,

        @Schema(nullable = true)
        String courseTitle
) {
}
