package de.htw.studyplanner.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Antwort von {@code GET /api/plan}: der komplette Studienplan (US-38, US-60).
 *
 * @param startSemester    Startsemester der Person
 * @param totalLp          Summe der LP über den gesamten Plan
 * @param maxLpPerSemester Konfiguriertes LP-Limit pro Semester
 * @param semesters        Semester mit Einträgen, zeitlich sortiert
 */
@Schema(description = "Plan der angemeldeten Person, nach Semestern gruppiert")
public record PlanResponse(

        @Schema(description = "Startsemester der Person", example = "WS 2026")
        String startSemester,

        @Schema(description = "Summe der LP über den gesamten Plan", example = "61")
        int totalLp,

        @Schema(
                description = "Konfiguriertes LP-Limit pro Semester",
                example = "30")
        int maxLpPerSemester,

        @Schema(description = "Semester mit Einträgen, zeitlich sortiert")
        List<PlanSemesterDto> semesters
) {
}