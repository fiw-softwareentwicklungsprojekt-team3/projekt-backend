package de.htw.studyplanner.backend.dto;

import java.math.BigDecimal;

import de.htw.studyplanner.backend.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body von {@code PUT /api/results/{moduleCode}}: das Ergebnis in einem Modul.
 *
 * <p>Formale Prüfungen (Pflichtfeld, Länge) stehen hier. Die fachlichen Regeln
 * (Note passt zum Status, Angebot passt zum Modul usw.) prüft der
 * {@link de.htw.studyplanner.backend.service.ResultService}.
 *
 * @param status      BELEGT, BESTANDEN oder NICHT_BESTANDEN
 * @param semester    Semester im Format "WS JJJJ" / "SS JJJJ"; leer bei anerkannten Leistungen
 * @param grade       Note; bei BELEGT und unbenoteten Modulen leer
 * @param attempt     im wievielten Versuch nicht bestanden (1–4); nur bei NICHT_BESTANDEN
 * @param offerCode   Wahlangebot bei B16, B26, B41, B54
 * @param courseCode  Veranstaltungsnummer bei B63, B64
 * @param courseTitle Veranstaltungstitel bei B63, B64 (optional)
 */
@Schema(description = "Ergebnis in einem Modul")
public record ResultRequest(

        @NotNull(message = "status darf nicht leer sein")
        @Schema(description = "BELEGT, BESTANDEN oder NICHT_BESTANDEN", example = "BESTANDEN")
        Status status,

        @Schema(description = "Semester der Leistung; leer bei anerkannten Leistungen",
                example = "WS 2026", nullable = true)
        String semester,

        @Schema(description = "Note: 1.0 … 4.0 oder 5.0; leer bei BELEGT und unbenoteten Modulen",
                example = "2.3", nullable = true)
        BigDecimal grade,

        @Schema(description = "Nur bei NICHT_BESTANDEN: im wievielten Versuch nicht bestanden (1–4)",
                example = "1", nullable = true)
        Integer attempt,

        @Schema(description = "Wahlangebot, Pflicht bei B16, B26, B41, B54", example = "WP1", nullable = true)
        String offerCode,

        @Size(max = 30, message = "courseCode darf höchstens 30 Zeichen haben")
        @Schema(description = "Veranstaltungsnummer, Pflicht bei B63, B64", example = "AWE-123", nullable = true)
        String courseCode,

        @Size(max = 150, message = "courseTitle darf höchstens 150 Zeichen haben")
        @Schema(description = "Veranstaltungstitel bei B63, B64", example = "Rhetorik", nullable = true)
        String courseTitle
) {
}
