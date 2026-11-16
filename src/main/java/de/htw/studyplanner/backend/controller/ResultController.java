package de.htw.studyplanner.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ApiError;
import de.htw.studyplanner.backend.dto.ResultRequest;
import de.htw.studyplanner.backend.dto.ResultResponse;
import de.htw.studyplanner.backend.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Leistungen der angemeldeten Person: eintragen, ändern, lesen, löschen.
 *
 * <p>Nur für {@code STUDIERENDE}, das regelt die SecurityConfig ({@code /api/results/**}).
 * Die Person kommt aus dem Token, {@code moduleCode} im Pfad ist nur das Modul.
 *
 * <p>Der Controller leitet nur weiter, die Regeln stehen in {@link ResultService}.
 */
@RestController
@RequestMapping("/api/results")
@Tag(name = "Leistungen", description = "Eigene Ergebnisse pro Modul")
@SecurityRequirement(name = "bearerAuth")
public class ResultController {

    private static final String BEISPIEL_ANTWORT = """
            {
              "moduleCode": "B11",
              "status": "NICHT_BESTANDEN",
              "semester": "WS 2026",
              "grade": 5.0,
              "attempts": 1,
              "offerCode": null,
              "courseCode": null,
              "courseTitle": null
            }""";

    private final ResultService resultService;

    /**
     * Erstellt den Controller.
     *
     * @param resultService Service mit den Regeln für Leistungen
     */
    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /**
     * Liefert alle eigenen Leistungen, sortiert nach Modulcode.
     *
     * @return die Leistungen, leer wenn es noch keine gibt
     */
    @GetMapping
    @Operation(summary = "Eigene Leistungen auflisten", description = "Sortiert nach Modulcode.")
    @ApiResponse(responseCode = "200", description = "Liste der eigenen Leistungen",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "Eine Leistung", value = "[" + BEISPIEL_ANTWORT + "]")))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Studierende", content = @Content)
    public List<ResultResponse> list() {
        return resultService.eigeneLeistungen();
    }

    /**
     * Legt die Leistung für ein Modul an oder ändert sie.
     *
     * @param moduleCode Modulcode, z. B. B11
     * @param request    das Ergebnis
     * @return die gespeicherte Leistung
     */
    @PutMapping("/{moduleCode}")
    @Operation(summary = "Leistung eintragen oder ändern",
            description = """
                    Pro Modul gibt es eine Zeile, eine Wiederholung ändert sie.
                    Noten: 1.0 … 4.0 und 5.0. BELEGT und unbenotete Module (B43) ohne Note,
                    BESTANDEN mit Note ≤ 4.0, NICHT_BESTANDEN mit 5.0.
                    attempt nur bei NICHT_BESTANDEN (1–4, Pflicht); bei BELEGT und BESTANDEN
                    bleibt die gespeicherte Versuchszahl unverändert.
                    semester darf leer sein, sonst nicht vor dem Startsemester.
                    B16/B26/B41/B54: offerCode Pflicht und passend zum Modul. B63/B64: courseCode Pflicht.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ResultRequest.class),
            examples = {
                    @ExampleObject(name = "Belegt", value = """
                            { "status": "BELEGT", "semester": "WS 2026" }"""),
                    @ExampleObject(name = "Bestanden", value = """
                            { "status": "BESTANDEN", "semester": "WS 2026", "grade": 2.3 }"""),
                    @ExampleObject(name = "Nicht bestanden", value = """
                            { "status": "NICHT_BESTANDEN", "semester": "WS 2026", "grade": 5.0, "attempt": 1 }"""),
                    @ExampleObject(name = "Wahlpflicht (B41)", value = """
                            { "status": "BESTANDEN", "semester": "SS 2028", "grade": 1.7, "offerCode": "WP1" }"""),
                    @ExampleObject(name = "AWE (B63)", value = """
                            { "status": "BESTANDEN", "semester": "SS 2029", "grade": 2.0, "courseCode": "AWE-123", "courseTitle": "Rhetorik" }""")
            }))
    @ApiResponse(responseCode = "200", description = "Gespeicherte Leistung",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResultResponse.class),
                    examples = @ExampleObject(name = "Nicht bestanden", value = BEISPIEL_ANTWORT)))
    @ApiResponse(responseCode = "400", description = "Eine Regel ist verletzt, das Feld steht in errors[].field",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Studierende", content = @Content)
    @ApiResponse(responseCode = "404", description = "Modul gibt es nicht",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    public ResultResponse save(
            @Parameter(description = "Modulcode", example = "B11") @PathVariable String moduleCode,
            @Valid @RequestBody ResultRequest request) {
        return resultService.speichern(moduleCode, request);
    }

    /**
     * Löscht die eigene Leistung für ein Modul.
     *
     * @param moduleCode Modulcode, z. B. B11
     */
    @DeleteMapping("/{moduleCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Leistung löschen", description = "Gibt es keine Leistung für das Modul, passiert nichts.")
    @ApiResponse(responseCode = "204", description = "Gelöscht")
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Studierende", content = @Content)
    @ApiResponse(responseCode = "404", description = "Modul gibt es nicht",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    public void delete(@Parameter(description = "Modulcode", example = "B11") @PathVariable String moduleCode) {
        resultService.loeschen(moduleCode);
    }
}
