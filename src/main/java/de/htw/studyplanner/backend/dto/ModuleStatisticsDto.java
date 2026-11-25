package de.htw.studyplanner.backend.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Anonymisierte Statistiken zu einem Modul für Lehrende (US-55).
 *
 * <p>Enthält Kennzahlen über bestandene, belegte und nicht bestandene Leistungen,
 * die Wiederholungs- und kritische Fehlversuchsanzahl sowie die Durchschnittsnote.
 *
 * @param code                 Modulcode (z. B. "B11")
 * @param name                 Modulbezeichnung (z. B. "Programmierung 1")
 * @param semester             Fachsemester laut StuPO (1–6)
 * @param passedCount          Anzahl der Leistungen mit Status BESTANDEN
 * @param enrolledCount        Anzahl der Leistungen mit Status BELEGT
 * @param failedCount          Anzahl der Leistungen mit Status NICHT_BESTANDEN
 * @param repeatCount          Anzahl der Leistungen mit anzahl_versuche > 1
 * @param criticalAttemptCount Anzahl der Leistungen mit status = NICHT_BESTANDEN und anzahl_versuche >= 3
 * @param averageGrade         Durchschnittsnote der bestandenen Leistungen (auf 1 Nachkommastelle gerundet),
 *                             oder {@code null} bei unbenoteten Modulen oder wenn noch keine Note vorliegt
 */
@Schema(description = "Anonymisierte Statistik eines Moduls")
public record ModuleStatisticsDto(

        @Schema(description = "Modulcode", example = "B11")
        String code,

        @Schema(description = "Modulbezeichnung", example = "Programmierung 1")
        String name,

        @Schema(description = "Fachsemester laut StuPO", example = "1")
        int semester,

        @Schema(description = "Anzahl der Leistungen mit Status BESTANDEN", example = "140")
        int passedCount,

        @Schema(description = "Anzahl der Leistungen mit Status BELEGT", example = "15")
        int enrolledCount,

        @Schema(description = "Anzahl der Leistungen mit Status NICHT_BESTANDEN", example = "8")
        int failedCount,

        @Schema(description = "Anzahl der Personen mit Wiederholungsversuchen (anzahl_versuche > 1)", example = "22")
        int repeatCount,

        @Schema(description = "Anzahl kritischer Fehlversuche (NICHT_BESTANDEN ab 3. Versuch)", example = "3")
        int criticalAttemptCount,

        @Schema(description = "Durchschnittsnote der bestandenen Leistungen (null bei unbenoteten Modulen oder ohne Noten)",
                example = "2.3", nullable = true)
        BigDecimal averageGrade
) {
}
