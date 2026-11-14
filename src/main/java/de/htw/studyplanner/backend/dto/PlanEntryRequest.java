package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body von {@code POST /api/plan/entries}: ein Modul für ein Semester einplanen.
 *
 * <p>Formale Prüfungen (Pflichtfeld, Länge) stehen hier. Die fachlichen Regeln
 * (Semesterformat, Startsemester, Turnus, Auswahl-Regeln) prüft der
 * {@link de.htw.studyplanner.backend.service.PlanService}.
 *
 * @param moduleCode  Modulcode, z. B. B34
 * @param semester    geplantes Semester im Format "WS JJJJ" oder "SS JJJJ"
 * @param offerCode   gewähltes Wahlangebot bei Auswahl-Modulen (optional)
 * @param courseCode  Veranstaltungsnummer bei Auswahl-Modulen (optional)
 * @param courseTitle Veranstaltungstitel bei Auswahl-Modulen (optional)
 */
@Schema(description = "Daten zum Einplanen eines Moduls")
public record PlanEntryRequest(

        @NotBlank(message = "moduleCode darf nicht leer sein")
        @Schema(description = "Modulcode, z. B. B34", example = "B34")
        String moduleCode,

        @Schema(description = "Geplantes Semester im Format 'WS JJJJ' oder 'SS JJJJ'", example = "WS 2027")
        String semester,

        @Schema(description = "Wahlangebot bei Modulen mit Auswahl (optional)", example = "null", nullable = true)
        String offerCode,

        @Size(max = 30, message = "courseCode darf höchstens 30 Zeichen haben")
        @Schema(description = "Veranstaltungsnummer bei Modulen mit Auswahl (optional)", example = "null", nullable = true)
        String courseCode,

        @Size(max = 150, message = "courseTitle darf höchstens 150 Zeichen haben")
        @Schema(description = "Veranstaltungstitel bei Modulen mit Auswahl (optional)", example = "null", nullable = true)
        String courseTitle
) {
}
