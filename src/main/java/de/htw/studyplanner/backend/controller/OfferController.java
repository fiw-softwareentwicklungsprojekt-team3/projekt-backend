package de.htw.studyplanner.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ApiError;
import de.htw.studyplanner.backend.dto.OfferDto;
import de.htw.studyplanner.backend.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Alle Wahlangebote, unabhängig vom Modul. Für das Frontend, falls es die Liste ohne
 * Modulbezug braucht. Welche Angebote zu einem Modul passen, liefert
 * {@code GET /api/modules/{code}} im Feld {@code offers}.
 */
@RestController
@RequestMapping("/api/offers")
@Tag(name = "Angebote", description = "Sprachen und Wahlpflicht-Bereiche")
public class OfferController {

    private final OfferService offerService;

    /**
     * Erstellt den Controller.
     *
     * @param offerService liefert die Angebote
     */
    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Liefert alle Angebote, optional nach Gruppe gefiltert, sortiert nach Code.
     *
     * @param group {@code WP} oder {@code SPRACHE}; ohne Angabe alle
     * @return die Angebote
     */
    @GetMapping
    @Operation(summary = "Alle Angebote auflisten",
            description = "Sortiert nach Code (WP1, WP2, …, WP15). Ohne group kommen alle 25 Angebote.")
    @ApiResponse(responseCode = "200", description = "Liste der Angebote")
    @ApiResponse(responseCode = "400", description = "group hat keinen erlaubten Wert",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(value = """
                            {
                              "timestamp": "2026-11-15T09:00:00Z",
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.",
                              "path": "/api/offers",
                              "errors": [ { "field": "group", "message": "Erlaubt sind nur WP und SPRACHE." } ]
                            }""")))
    @ApiResponse(responseCode = "401", description = "Nicht angemeldet", content = @Content)
    public List<OfferDto> list(
            @Parameter(name = "group", description = "WP oder SPRACHE", example = "WP")
            @RequestParam(name = "group", required = false) String group) {
        return offerService.alleAngebote(group);
    }
}
