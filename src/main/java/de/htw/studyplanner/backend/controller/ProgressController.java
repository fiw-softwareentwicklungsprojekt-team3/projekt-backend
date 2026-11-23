package de.htw.studyplanner.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ProgressResponse;
import de.htw.studyplanner.backend.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Fortschritts-Übersicht der angemeldeten Person (US-58).
 *
 * <p>Nur für {@code STUDIERENDE}, das regelt die SecurityConfig ({@code /api/progress/**}).
 * Die Person kommt aus dem Token. Die Berechnung steht in {@link ProgressService}.
 */
@RestController
@RequestMapping("/api/progress")
@Tag(name = "Fortschritt", description = "Eigener Studienfortschritt")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;

    /**
     * Erstellt den Controller.
     *
     * @param progressService Service, der den Fortschritt berechnet
     */
    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * Liefert den eigenen Studienfortschritt.
     *
     * @return LP, Prozent, Notenschnitt, Stufe der Zulassung zur Bachelorarbeit und offene Pflichtmodule
     */
    @GetMapping
    @Operation(summary = "Eigenen Fortschritt abrufen",
            description = """
                    Es zählen nur Leistungen, nicht der Plan.
                    earnedLp: Summe der LP aller BESTANDEN-Leistungen, progressPercent bezogen auf 180 LP.
                    averageGrade: LP-gewichteter Schnitt der benoteten BESTANDEN-Leistungen (B43 zählt nicht),
                    auf eine Nachkommastelle abgeschnitten; null, wenn es noch keine Note gibt.
                    openModules: alle Pflichtmodule (art = P), die nicht bestanden sind, nach Fachsemester und Code.
                    locked: eine notwendige Voraussetzung ist nicht bestanden oder die LP liegen unter
                    min_lp − toleranz_lp.
                    bachelorThesisEligible: Sem. 1–3 bestanden und mindestens 151 LP (regulär).
                    bachelorThesisNeedsApproval: Sem. 1–3 bestanden und 141–150 LP, nur mit Zulassung
                    durch den Prüfungsausschuss. Höchstens eins von beiden ist true.""")
    @ApiResponse(responseCode = "200", description = "Fortschritt der angemeldeten Person",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProgressResponse.class),
                    examples = @ExampleObject(name = "B12–B15 bestanden, B11 nicht bestanden", value = """
                            {
                              "earnedLp": 20,
                              "totalLp": 180,
                              "progressPercent": 11.1,
                              "averageGrade": 2.3,
                              "bachelorThesisEligible": false,
                              "bachelorThesisNeedsApproval": false,
                              "openModules": [
                                {
                                  "moduleCode": "B11",
                                  "moduleName": "Programmierung 1",
                                  "recommendedSemester": 1,
                                  "lp": 6,
                                  "resultStatus": "NICHT_BESTANDEN",
                                  "locked": false,
                                  "lockReasons": []
                                },
                                {
                                  "moduleCode": "B34",
                                  "moduleName": "Softwareentwicklungsprojekt",
                                  "recommendedSemester": 3,
                                  "lp": 10,
                                  "resultStatus": null,
                                  "locked": true,
                                  "lockReasons": ["Programmierung 1 fehlt noch"]
                                },
                                {
                                  "moduleCode": "B43",
                                  "moduleName": "Fachpraktikum",
                                  "recommendedSemester": 4,
                                  "lp": 20,
                                  "resultStatus": null,
                                  "locked": true,
                                  "lockReasons": ["Mindestens 60 LP nötig, du hast 20 LP"]
                                }
                              ]
                            }""")))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Studierende", content = @Content)
    public ProgressResponse get() {
        return progressService.fortschritt();
    }
}
