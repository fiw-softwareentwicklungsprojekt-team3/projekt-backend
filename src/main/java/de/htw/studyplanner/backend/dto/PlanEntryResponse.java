package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein gespeicherter Planeintrag in der Antwort von {@code POST /api/plan/entries} (E33).
 *
 * @param id          technische ID des Planeintrags ({@code plan_eintrag_id})
 * @param moduleCode  Modulcode, z. B. B34
 * @param semester    geplantes Semester als "WS JJJJ" oder "SS JJJJ"
 * @param offerCode   gewähltes Wahlangebot oder {@code null}
 * @param courseCode  Veranstaltungsnummer oder {@code null}
 * @param courseTitle Veranstaltungstitel oder {@code null}
 */
@Schema(description = "Gespeicherter Planeintrag")
public record PlanEntryResponse(

        @Schema(description = "ID des Planeintrags", example = "1")
        Integer id,

        @Schema(description = "Modulcode", example = "B34")
        String moduleCode,

        @Schema(description = "Geplantes Semester", example = "WS 2027")
        String semester,

        @Schema(description = "Gewähltes Wahlangebot oder null", nullable = true)
        String offerCode,

        @Schema(description = "Veranstaltungsnummer oder null", nullable = true)
        String courseCode,

        @Schema(description = "Veranstaltungstitel oder null", nullable = true)
        String courseTitle
) {
}
