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
 * <p><b>Hinweis zu {@code offerCode}, {@code courseCode} und {@code courseTitle} (E10):</b>
 * Diese drei Felder sind ausschließlich für die konkrete Kurswahl innerhalb der sechs
 * Auswahl-Module ({@code mit_auswahl = true}: B16, B26, B41, B54, B63, B64) gedacht
 * und dort beim Planen optional. Bei allen anderen Modulen ({@code mit_auswahl = false},
 * z. B. B11, B33, B34) steht der Modultitel bereits über {@code modul.bezeichnung} fest,
 * sodass alle drei Felder {@code null} sein müssen.
 *
 * @param moduleCode  Modulcode, z. B. B34
 * @param semester    geplantes Semester im Format "WS JJJJ" oder "SS JJJJ"
 * @param offerCode   gewähltes Wahlangebot aus der Liste (nur bei Auswahl-Modulen, z. B. WP1..WP15
 *                    für B41/B54 oder EN_B2.1 für B16; sonst {@code null})
 * @param courseCode  freie Veranstaltungsnummer (nur bei Auswahl-Modulen wie B63/B64; sonst {@code null})
 * @param courseTitle freier Veranstaltungstitel zum {@code courseCode}, <b>nicht</b> die Bezeichnung
 *                    des Moduls selbst (nur bei Auswahl-Modulen; sonst {@code null})
 */
@Schema(description = "Daten zum Einplanen eines Moduls")
public record PlanEntryRequest(

        @NotBlank(message = "moduleCode darf nicht leer sein")
        @Schema(description = "Modulcode, z. B. B34", example = "B34")
        String moduleCode,

        @Schema(description = "Geplantes Semester im Format 'WS JJJJ' oder 'SS JJJJ'", example = "WS 2027")
        String semester,

        @Schema(description = "Wahlangebot aus dem Katalog (optional, nur erlaubt bei mit_auswahl = true: "
                + "B16, B26, B41, B54, B63, B64; bei festen Pflichtmodulen zwingend null)",
                example = "null", nullable = true)
        String offerCode,

        @Size(max = 30, message = "courseCode darf höchstens 30 Zeichen haben")
        @Schema(description = "Freie Veranstaltungsnummer (optional, nur erlaubt bei mit_auswahl = true; "
                + "bei festen Pflichtmodulen zwingend null)",
                example = "null", nullable = true)
        String courseCode,

        @Size(max = 150, message = "courseTitle darf höchstens 150 Zeichen haben")
        @Schema(description = "Freier Veranstaltungstitel eines gewählten Kurses, nicht der Modulname "
                + "(optional, nur erlaubt bei mit_auswahl = true; bei festen Pflichtmodulen zwingend null)",
                example = "null", nullable = true)
        String courseTitle
) {
}
