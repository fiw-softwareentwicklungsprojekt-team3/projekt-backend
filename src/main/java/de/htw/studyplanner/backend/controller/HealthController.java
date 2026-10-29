package de.htw.studyplanner.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Einfacher Endpunkt, um zu prüfen, ob das Backend läuft.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Erreichbarkeit des Backends")
public class HealthController {

    /**
     * Meldet, dass das Backend läuft. Die Datenbank wird dabei nicht geprüft.
     *
     * @return immer {@code {"status": "UP"}}
     */
    @GetMapping("/health")
    @Operation(summary = "Prüfen, ob das Backend läuft",
            description = "Antwortet immer mit Status UP, solange der Server erreichbar ist. Die Datenbank wird nicht geprüft.")
    @ApiResponse(responseCode = "200", description = "Backend läuft",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            { "status": "UP" }""")))
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
