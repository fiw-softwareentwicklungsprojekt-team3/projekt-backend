package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Anonymisierte Statistiken einer Studierenden-Kohorte (Jahrgang/Startsemester).
 *
 * <p>Enthält ausschließlich aggregierte Kennzahlen für Lehrende und das Dashboard.
 * Personenbezogene Daten (wie ID, E-Mail, Name oder Einzelleistungen) sind
 * ausdrücklich ausgeschlossen.
 *
 * @param cohort                      Startsemester der Kohorte (z. B. "WS 2024")
 * @param studentCount                Anzahl der Studierenden in dieser Kohorte
 * @param averageLp                   Durchschnittlich erreichte Leistungspunkte (LP)
 * @param averageProgressPercent      Mittlerer Studienfortschritt in Prozent (bezogen auf 180 LP)
 * @param completedSemesters1To3Count Anzahl der Studierenden mit allen 17 Pflichtmodulen der Fachsemester 1–3
 * @param bachelorThesisEligibleCount Anzahl der Studierenden, die Voraussetzungen für die Bachelorarbeit erfüllen
 */
@Schema(description = "Anonymisierte Statistik einer Studierenden-Kohorte")
public record CohortStatisticsDto(

        @Schema(description = "Startsemester der Kohorte", example = "WS 2024")
        String cohort,

        @Schema(description = "Anzahl der Studierenden in dieser Kohorte", example = "45")
        int studentCount,

        @Schema(description = "Durchschnittlich erreichte Leistungspunkte", example = "62.4")
        double averageLp,

        @Schema(description = "Mittlerer Fortschritt in Prozent bezogen auf 180 LP", example = "34.7")
        double averageProgressPercent,

        @Schema(description = "Anzahl Personen mit allen Pflichtkursen aus den Fachsemestern 1–3", example = "28")
        int completedSemesters1To3Count,

        @Schema(description = "Anzahl Personen mit Zulassungsvoraussetzung für die Bachelorarbeit", example = "0")
        int bachelorThesisEligibleCount) {
}
