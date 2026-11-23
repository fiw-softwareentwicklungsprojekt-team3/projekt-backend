package de.htw.studyplanner.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Semester im Plan mit seinen Einträgen (US-38, US-60).
 *
 * @param semester        Plansemester als "WS JJJJ" oder "SS JJJJ"
 * @param lp              Summe der LP aller Einträge in diesem Semester
 * @param sws             Summe der SWS aus Vorlesung und Übung
 * @param lpLimitExceeded true, wenn das konfigurierte LP-Limit überschritten wurde
 * @param entries         Einträge, sortiert nach Modulcode
 */
@Schema(description = "Semester im Plan mit seinen Einträgen und LP/SWS-Summen")
public record PlanSemesterDto(

        @Schema(description = "Plansemester", example = "WS 2026")
        String semester,

        @Schema(description = "Summe der LP in diesem Semester", example = "31")
        int lp,

        @Schema(description = "Summe der SWS in diesem Semester", example = "20")
        int sws,

        @Schema(
                description = "Gibt an, ob das LP-Limit pro Semester überschritten wurde",
                example = "true")
        boolean lpLimitExceeded,

        @Schema(description = "Einträge, sortiert nach Modulcode")
        List<PlanEntryDto> entries
) {
}