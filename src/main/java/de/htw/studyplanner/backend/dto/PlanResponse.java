package de.htw.studyplanner.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Antwort von {@code GET /api/plan}: der ganze Plan der angemeldeten Person (US-38, US-60).
 *
 * <p>Nur Semester mit mindestens einem Eintrag sind enthalten, zeitlich sortiert.
 * Lücken füllt das Frontend. Alle LP-Summen werden bei jeder Anfrage berechnet.
 *
 * @param startSemester    Startsemester der Person, z. B. "WS 2026"
 * @param totalLp          Summe der LP über alle Einträge
 * @param maxLpPerSemester konfiguriertes LP-Limit pro Semester (US-60)
 * @param semesters        Semester mit Einträgen, zeitlich sortiert; leer bei leerem Plan
 */
@Schema(description = "Plan der angemeldeten Person, nach Semestern gruppiert")
public record PlanResponse(

        @Schema(description = "Startsemester der Person", example = "WS 2026")
        String startSemester,

        @Schema(description = "Summe der LP über den ganzen Plan", example = "61")
        int totalLp,

        @Schema(description = "Konfiguriertes LP-Limit pro Semester", example = "30")
        int maxLpPerSemester,

        @Schema(description = "Semester mit Einträgen, zeitlich sortiert")
        List<PlanSemesterDto> semesters
) {
}
