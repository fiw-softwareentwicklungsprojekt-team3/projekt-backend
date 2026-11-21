package de.htw.studyplanner.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Semester im Plan mit seinen Einträgen (US-38).
 *
 * @param semester Plansemester als "WS JJJJ" oder "SS JJJJ"
 * @param lp       Summe der LP aller Einträge in diesem Semester (bei jeder Anfrage berechnet)
 * @param entries  Einträge, sortiert nach Modulcode
 */
@Schema(description = "Semester im Plan mit seinen Einträgen")
public record PlanSemesterDto(

        @Schema(description = "Plansemester", example = "WS 2026")
        String semester,

        @Schema(description = "Summe der LP in diesem Semester", example = "31")
        int lp,

        @Schema(description = "Einträge, sortiert nach Modulcode")
        List<PlanEntryDto> entries
) {
}
