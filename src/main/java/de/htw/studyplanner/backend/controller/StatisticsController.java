package de.htw.studyplanner.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ApiError;
import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.dto.ModuleStatisticsDto;
import de.htw.studyplanner.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
                    Kohorten mit weniger als 5 Studierenden werden zum Schutz der Anonymität nicht ausgeliefert.
                    Enthält Studierendenanzahl, durchschnittliche LP, mittleren Fortschritt sowie
                    Abschlussstände für Sem. 1–3 und die Bachelorarbeit: bachelorThesisEligibleCount zählt
                    regulär (ab 151 LP), bachelorThesisWithApprovalCount mit Zulassung durch den
                    Prüfungsausschuss (141–150 LP). Beides setzt bestandene Sem. 1–3 voraus.
                    Nur für Professor_innen abrufbar.""")
    @ApiResponse(
            responseCode = "200",
            description = "Liste der Kohorten-Statistiken ab mindestens 5 Studierenden (leer, wenn keine entsprechenden Kohorten vorhanden sind)",
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
                                "bachelorThesisEligibleCount": 0,
                                "bachelorThesisWithApprovalCount": 0
                              }
                            ]
                            """)))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Professor_innen", content = @Content)
    public List<CohortStatisticsDto> getCohortStatistics() {
        return statisticsService.getCohortStatistics();
    }

    /**
     * Liefert die anonymisierten Statistiken aller Module für Lehrende (US-55).
     *
     * <p>Optional kann nach einer Kohorte (Startsemester der Studierenden) gefiltert werden.
     *
     * @param cohort optionales Startsemester zur Filterung (z. B. "WS 2023")
     * @return Liste der Modulstatistiken sortiert nach Fachsemester und Modulcode
     */
    @GetMapping("/modules")
    @Operation(
            summary = "Anonymisierte Modul- und Wiederholungsstatistiken abrufen",
            description = """
                    Liefert aggregierte und anonymisierte Kennzahlen pro Modul:
                    Anzahl bestandener, belegter und nicht bestandener Leistungen, Anzahl der
                    Personen mit Wiederholungsversuchen (Versuche > 1), Anzahl kritischer
                    Fehlversuche (NICHT_BESTANDEN ab dem 3. Versuch) und Durchschnittsnote.
                    Beim unbenoteten Modul B43 oder wenn noch keine Noten vorliegen, ist averageGrade null.
                    Optional filterbar nach Kohorte (Startsemester der Studierenden, z. B. 'WS 2023').
                    Die Liste ist sortiert nach Fachsemester und Modulcode.
                    Nur für Professor_innen abrufbar.""")
    @ApiResponse(
            responseCode = "200",
            description = "Liste der Modul-Statistiken, sortiert nach Fachsemester und Code",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ModuleStatisticsDto.class)),
                    examples = @ExampleObject(name = "Modul-Statistiken", value = """
                            [
                              {
                                "code": "B11",
                                "name": "Programmierung 1",
                                "semester": 1,
                                "passedCount": 140,
                                "enrolledCount": 15,
                                "failedCount": 8,
                                "repeatCount": 22,
                                "criticalAttemptCount": 3,
                                "averageGrade": 2.3
                              },
                              {
                                "code": "B43",
                                "name": "Fachpraktikum",
                                "semester": 4,
                                "passedCount": 95,
                                "enrolledCount": 10,
                                "failedCount": 0,
                                "repeatCount": 0,
                                "criticalAttemptCount": 0,
                                "averageGrade": null
                              }
                            ]
                            """)))
    @ApiResponse(
            responseCode = "400",
            description = "Ungültiges Format für den Parameter cohort (z. B. kein WS/SS oder Jahr außerhalb 2000–2100)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Professor_innen", content = @Content)
    public List<ModuleStatisticsDto> getModuleStatistics(
            @Parameter(description = "Optionaler Filter nach Kohorte (Startsemester, z. B. 'WS 2023')", example = "WS 2023")
            @RequestParam(required = false) String cohort) {
        return statisticsService.getModuleStatistics(cohort);
    }
}
