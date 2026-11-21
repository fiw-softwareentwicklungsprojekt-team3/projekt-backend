package de.htw.studyplanner.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Anonymisierte Statistiken für Lehrende.
 *
 * <p>Nur für {@code PROFESSOR_IN}, das regelt die {@code SecurityConfig} ({@code /api/statistics/**}).
 * Das Dashboard-Frontend ruft diese Daten ab, um Übersichten über Jahrgänge anzuzeigen.
 */
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistiken", description = "Anonymisierte Auswertungen für Lehrende")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * Erstellt den Controller.
     *
     * @param statisticsService Service zur Ermittlung der Statistiken
     */
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Liefert die Liste aller Kohorten-Statistiken in chronologischer Reihenfolge.
     *
     * @return anonymisierte Statistiken pro Kohorte (leer wenn keine Studierenden vorhanden)
     */
    @GetMapping("/cohorts")
    @Operation(
            summary = "Anonymisierte Kohorten-Statistiken abrufen",
            description = """
                    Liefert aggregierte und vollständig anonymisierte Kennzahlen pro Kohorte (Startsemester).
                    Enthält Studierendenanzahl, durchschnittliche LP, mittleren Fortschritt sowie
                    Abschlussstände für Sem. 1–3 und die Bachelorarbeit.
                    Nur für Professor_innen abrufbar.""")
    @ApiResponse(
            responseCode = "200",
            description = "Liste der Kohorten-Statistiken (leer, wenn keine Studierenden in der Datenbank sind)",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = CohortStatisticsDto.class)),
                    examples = @ExampleObject(name = "Kohorten-Übersicht", value = """
                            [
                              {
                                "cohort": "WS 2024",
                                "studentCount": 45,
                                "averageLp": 62.4,
                                "averageProgressPercent": 34.7,
                                "completedSemesters1To3Count": 28,
                                "bachelorThesisEligibleCount": 0
                              }
                            ]
                            """)))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Professor_innen", content = @Content)
    public List<CohortStatisticsDto> getCohortStatistics() {
        return statisticsService.getCohortStatistics();
    }
}
