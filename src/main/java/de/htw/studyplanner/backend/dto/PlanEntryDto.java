package de.htw.studyplanner.backend.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Eintrag im Plan in der Antwort von {@code GET /api/plan} (US-38).
 *
 * <p>Bewusst ein eigenes DTO (nicht {@link PlanEntryResponse}): Es enthält zusätzlich die
 * Moduldaten und das Ergebnis der Planprüfung ({@code prerequisitesMet}, {@code violations},
 * E30). Beides wird bei jeder Anfrage berechnet, nicht gespeichert.
 *
 * @param id                  technische ID des Planeintrags ({@code plan_eintrag_id})
 * @param moduleCode          Modulcode, z. B. B34
 * @param moduleName          Bezeichnung des Moduls
 * @param lp                  Leistungspunkte des Moduls
 * @param recommendedSemester empfohlenes Fachsemester laut StuPO; heißt nicht {@code semester},
 *                            damit es nicht mit dem Plansemester verwechselt wird
 * @param offerCode           gewähltes Wahlangebot oder {@code null}
 * @param offerName           Bezeichnung des Wahlangebots oder {@code null}
 * @param courseCode          freie Veranstaltungsnummer oder {@code null}
 * @param courseTitle         freier Veranstaltungstitel oder {@code null}
 * @param prerequisitesMet    {@code false}, sobald mindestens ein Verstoß mit Schwere
 *                            {@code FEHLER} vorliegt; Warnungen ändern daran nichts
 * @param violations          alle Verstöße gegen Voraussetzungen dieses Moduls, leer wenn keine
 */
@Schema(description = "Eintrag im Plan")
public record PlanEntryDto(

        @Schema(description = "ID des Planeintrags", example = "1")
        Integer id,

        @Schema(description = "Modulcode", example = "B34")
        String moduleCode,

        @Schema(description = "Bezeichnung des Moduls", example = "Softwareentwicklungsprojekt")
        String moduleName,

        @Schema(description = "Leistungspunkte des Moduls", example = "10")
        int lp,

        @Schema(description = "Empfohlenes Fachsemester laut StuPO (nicht das Plansemester)", example = "3")
        int recommendedSemester,

        @Schema(description = "Gewähltes Wahlangebot oder null", nullable = true, example = "WP1")
        String offerCode,

        @Schema(description = "Bezeichnung des Wahlangebots oder null", nullable = true)
        String offerName,

        @Schema(description = "Veranstaltungsnummer oder null", nullable = true)
        String courseCode,

        @Schema(description = "Veranstaltungstitel oder null", nullable = true)
        String courseTitle,

        @Schema(description = "false, sobald mindestens ein Verstoß mit Schwere FEHLER vorliegt",
                example = "true")
        boolean prerequisitesMet,

        @Schema(description = "Verstöße gegen Voraussetzungen dieses Moduls, leer wenn keine")
        List<PlanViolationResponse> violations
) {
}
