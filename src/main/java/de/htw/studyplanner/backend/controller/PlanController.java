package de.htw.studyplanner.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ApiError;
import de.htw.studyplanner.backend.dto.PlanEntryRequest;
import de.htw.studyplanner.backend.dto.PlanEntryResponse;
import de.htw.studyplanner.backend.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Studienplan der angemeldeten Person: Module für ein Semester einplanen.
 *
 * <p>Nur für {@code STUDIERENDE}, das regelt die SecurityConfig ({@code /api/plan/**}).
 * Die Person kommt aus dem Token. Der Controller leitet nur weiter, die Logik steht
 * in {@link PlanService}.
 */
@RestController
@RequestMapping("/api/plan")
@Tag(name = "Plan", description = "Studienplan der angemeldeten Person")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final PlanService planService;

    /**
     * Erstellt den Controller.
     *
     * @param planService Service mit den Regeln für den Studienplan
     */
    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /**
     * Plant ein Modul für ein Semester ein.
     *
     * @param request Modulcode, Semester und optionale Auswahl-Angaben
     * @return der gespeicherte Planeintrag inklusive ID
     */
    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Modul für ein Semester einplanen",
            description = """
                    Legt einen neuen Planeintrag für die angemeldete Person an.
                    Planbar ist jedes Semester ab dem Startsemester, sofern der Turnus des Moduls
                    zur Semesterart passt (WS/SS/BEIDE). Nicht erfüllte Voraussetzungen blockieren
                    das Speichern nicht.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = PlanEntryRequest.class),
            examples = @ExampleObject(name = "Pflichtmodul einplanen", value = """
                    {
                      "moduleCode": "B34",
                      "semester": "WS 2027",
                      "offerCode": null,
                      "courseCode": null,
                      "courseTitle": null
                    }""")))
    @ApiResponse(responseCode = "201", description = "Modul wurde eingeplant",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PlanEntryResponse.class),
                    examples = @ExampleObject(name = "Gespeicherter Planeintrag", value = """
                            {
                              "id": 1,
                              "moduleCode": "B34",
                              "semester": "WS 2027",
                              "offerCode": null,
                              "courseCode": null,
                              "courseTitle": null
                            }""")))
    @ApiResponse(responseCode = "400",
            description = "Ungültige Eingabe, z. B. falsches Semesterformat, vor dem Startsemester, "
                    + "Turnus-Verstoß oder unerlaubte Auswahl-Angaben",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Studierende", content = @Content)
    @ApiResponse(responseCode = "404", description = "Modul wurde nicht gefunden",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Modul ist bereits im Plan (bitte verschieben)",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    public PlanEntryResponse createEntry(@Valid @RequestBody PlanEntryRequest request) {
        return planService.eintragAnlegen(request);
    }
}
