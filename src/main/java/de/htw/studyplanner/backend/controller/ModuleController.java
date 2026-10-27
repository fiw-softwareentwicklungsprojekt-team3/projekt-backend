package de.htw.studyplanner.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ApiErrorResponse;
import de.htw.studyplanner.backend.dto.ModuleDetailDto;
import de.htw.studyplanner.backend.dto.ModuleDto;
import de.htw.studyplanner.backend.service.ModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Modulkatalog: liefert die Module des Studiengangs.
 * Der Controller leitet nur weiter, die Logik steht in {@link ModuleService}.
 */
@RestController
@RequestMapping("/api/modules")
@Tag(name = "Module", description = "Modulkatalog des Studiengangs")
public class ModuleController {

    private final ModuleService moduleService;

    /**
     * Erstellt den Controller.
     *
     * @param moduleService Service, der den Modulkatalog liest
     */
    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    /**
     * Liefert alle Module, sortiert nach Fachsemester und Code, optional gefiltert.
     * Ein ungültiger Filterwert führt zu Status 400.
     *
     * @param fachsemester Fachsemester 1 bis 6
     * @param art          P (Pflicht) oder WP (Wahlpflicht)
     * @param turnus       WS oder SS; Module, die in beiden Semestern angeboten werden, sind immer dabei
     * @return die passenden Module
     */
    @GetMapping
    @Operation(summary = "Module auflisten",
            description = "Sortiert nach Fachsemester und Code. Filter sind optional und werden kombiniert.")
    @ApiResponse(responseCode = "200",
            description = "Liste der passenden Module; leer, wenn keines passt")
    @ApiResponse(responseCode = "400",
            description = "Ungültiger Filterwert, z. B. fachsemester=7, art=X oder turnus=BEIDE",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "timestamp": "2026-10-27T09:00:00.000+00:00",
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Ungültiger Wert für fachsemester: erlaubt sind 1 bis 6.",
                              "path": "/api/modules"
                            }""")))
    public List<ModuleDto> list(
            @Parameter(description = "Fachsemester, 1 bis 6", example = "3")
            @RequestParam(required = false) Integer fachsemester,
            @Parameter(description = "P (Pflicht) oder WP (Wahlpflicht)", example = "P")
            @RequestParam(required = false) String art,
            @Parameter(description = "WS oder SS; Module mit Turnus BEIDE sind bei beiden enthalten", example = "WS")
            @RequestParam(required = false) String turnus) {
        return moduleService.list(fachsemester, art, turnus);
    }

    /**
     * Liefert ein Modul mit seinen Voraussetzungen. Gibt es den Code nicht, antwortet die API mit 404.
     *
     * @param code Modulcode, z. B. B34
     * @return das Modul mit Voraussetzungen
     */
    @GetMapping("/{code}")
    @Operation(summary = "Ein Modul mit Voraussetzungen abrufen",
            description = "Notwendige Voraussetzungen stehen vor empfohlenen. LP-Voraussetzungen kommen zuletzt.")
    @ApiResponse(responseCode = "200", description = "Das Modul mit Voraussetzungen")
    @ApiResponse(responseCode = "404", description = "Es gibt kein Modul mit diesem Code",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "timestamp": "2026-10-27T09:00:00.000+00:00",
                              "status": 404,
                              "error": "Not Found",
                              "message": "Modul mit dem Code B99 wurde nicht gefunden.",
                              "path": "/api/modules/B99"
                            }""")))
    public ModuleDetailDto get(
            @Parameter(description = "Modulcode", example = "B34") @PathVariable String code) {
        return moduleService.get(code);
    }
}
