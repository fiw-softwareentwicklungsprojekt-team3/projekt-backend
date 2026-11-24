package de.htw.studyplanner.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.ApiError;
import de.htw.studyplanner.backend.dto.PlanEntryRequest;
import de.htw.studyplanner.backend.dto.PlanEntryResponse;
import de.htw.studyplanner.backend.dto.PlanResponse;
import de.htw.studyplanner.backend.service.PlanQueryService;
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
 * Studienplan der angemeldeten Person: Plan abrufen und Module für ein Semester einplanen.
 *
 * <p>Nur für {@code STUDIERENDE}, das regelt die SecurityConfig ({@code /api/plan/**}).
 * Die Person kommt aus dem Token. Der Controller leitet nur weiter, die Logik steht
 * in {@link PlanService} (schreiben) und {@link PlanQueryService} (lesen).
 */
@RestController
@RequestMapping("/api/plan")
@Tag(name = "Plan", description = "Studienplan der angemeldeten Person")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final PlanService planService;
    private final PlanQueryService planQueryService;

    /**
     * Erstellt den Controller.
     *
     * @param planService      Service mit den Regeln für den Studienplan
     * @param planQueryService Service zum Laden des Plans inklusive Verstößen
     */
    public PlanController(
            PlanService planService,
            PlanQueryService planQueryService) {
        this.planService = planService;
        this.planQueryService = planQueryService;
    }

    /**
     * Liefert den ganzen Plan der angemeldeten Person, nach Semestern gruppiert, mit den
     * aktuell berechneten Verstößen je Eintrag (US-38, E30).
     *
     * @return Startsemester, LP-Summe und die Semester mit ihren Einträgen
     */
    @GetMapping
    @Operation(summary = "Eigenen Plan abrufen",
            description = """
                    Liefert den ganzen Plan der angemeldeten Person, nach Semestern gruppiert.
                    Semester sind zeitlich sortiert, Einträge innerhalb eines Semesters nach Modulcode.
                    Semester ohne Einträge fehlen, Lücken füllt das Frontend.
                    Ein leerer Plan liefert `200` mit `semesters: []` und `totalLp: 0`.

                    Jeder Eintrag enthält `prerequisitesMet` und `violations` (E30). `severity` ist:
                    - `FEHLER`: notwendige Voraussetzung fehlt oder zu wenige LP
                    - `WARNUNG`: empfohlene Voraussetzung fehlt, oder LP-Lücke innerhalb der Toleranz
                      (nur mit Zulassung durch den Prüfungsausschuss)
                    - `HINWEIS`: Voraussetzung liegt in einem vergangenen Semester, das Ergebnis fehlt noch

                    `prerequisitesMet` ist `false`, sobald ein `FEHLER` vorliegt;
                    `WARNUNG` und `HINWEIS` ändern daran nichts.
                    LP-Summen (`lp`, `totalLp`), `sws` und `lpLimitExceeded` sowie Verstöße werden
                    bei jeder Anfrage berechnet, nichts davon wird gespeichert.
                    `maxLpPerSemester` nennt das konfigurierte Limit (z. B. 30 LP).

                    `recommendedSemester` ist das empfohlene Fachsemester laut StuPO,
                    nicht das Plansemester.""")
    @ApiResponse(responseCode = "200", description = "Plan der angemeldeten Person",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PlanResponse.class),
                    examples = {
                            @ExampleObject(name = "Plan ohne Verstöße",
                                    description = "Normalfall: Semester zeitlich sortiert, Einträge nach "
                                            + "Modulcode. B21 empfiehlt B11 und B13, beide liegen früher "
                                            + "im Plan, deshalb keine Verstöße.",
                                    value = """
                                    {
                                      "startSemester": "WS 2026",
                                      "totalLp": 16,
                                      "maxLpPerSemester": 30,
                                      "semesters": [
                                        {
                                          "semester": "WS 2026",
                                          "lp": 11,
                                          "sws": 10,
                                          "lpLimitExceeded": false,
                                          "entries": [
                                            {
                                              "id": 1,
                                              "moduleCode": "B11",
                                              "moduleName": "Programmierung 1",
                                              "lp": 6,
                                              "recommendedSemester": 1,
                                              "offerCode": null,
                                              "offerName": null,
                                              "courseCode": null,
                                              "courseTitle": null,
                                              "prerequisitesMet": true,
                                              "violations": []
                                            },
                                            {
                                              "id": 2,
                                              "moduleCode": "B13",
                                              "moduleName": "Grundlegende Konzepte der Informatik",
                                              "lp": 5,
                                              "recommendedSemester": 1,
                                              "offerCode": null,
                                              "offerName": null,
                                              "courseCode": null,
                                              "courseTitle": null,
                                              "prerequisitesMet": true,
                                              "violations": []
                                            }
                                          ]
                                        },
                                        {
                                          "semester": "SS 2027",
                                          "lp": 5,
                                          "sws": 4,
                                          "lpLimitExceeded": false,
                                          "entries": [
                                            {
                                              "id": 3,
                                              "moduleCode": "B21",
                                              "moduleName": "Programmierung 2",
                                              "lp": 5,
                                              "recommendedSemester": 2,
                                              "offerCode": null,
                                              "offerName": null,
                                              "courseCode": null,
                                              "courseTitle": null,
                                              "prerequisitesMet": true,
                                              "violations": []
                                            }
                                          ]
                                        }
                                      ]
                                    }"""),
                            @ExampleObject(name = "Plan mit Fehler und Warnungen",
                                    description = "B34 liegt im WS 2027, die notwendige Voraussetzung B11 "
                                            + "fehlt im Plan: FEHLER, prerequisitesMet = false. Die empfohlenen "
                                            + "B21 und B24 fehlen ebenfalls: nur WARNUNG. Das Speichern war "
                                            + "trotzdem erlaubt (E2).",
                                    value = """
                                    {
                                      "startSemester": "WS 2026",
                                      "totalLp": 14,
                                      "maxLpPerSemester": 30,
                                      "semesters": [
                                        {
                                          "semester": "WS 2026",
                                          "lp": 4,
                                          "sws": 4,
                                          "lpLimitExceeded": false,
                                          "entries": [
                                            {
                                              "id": 1,
                                              "moduleCode": "B16",
                                              "moduleName": "1. Fremdsprache 1",
                                              "lp": 4,
                                              "recommendedSemester": 1,
                                              "offerCode": "EN_B2.1",
                                              "offerName": "Englisch Fachsprache B2.1 (Technik)",
                                              "courseCode": null,
                                              "courseTitle": null,
                                              "prerequisitesMet": true,
                                              "violations": []
                                            }
                                          ]
                                        },
                                        {
                                          "semester": "WS 2027",
                                          "lp": 10,
                                          "sws": 6,
                                          "lpLimitExceeded": false,
                                          "entries": [
                                            {
                                              "id": 2,
                                              "moduleCode": "B34",
                                              "moduleName": "Softwareentwicklungsprojekt",
                                              "lp": 10,
                                              "recommendedSemester": 3,
                                              "offerCode": null,
                                              "offerName": null,
                                              "courseCode": null,
                                              "courseTitle": null,
                                              "prerequisitesMet": false,
                                              "violations": [
                                                {
                                                  "severity": "FEHLER",
                                                  "prerequisiteCode": "B11",
                                                  "message": "Voraussetzung B11 für B34 ist nicht erfüllt."
                                                },
                                                {
                                                  "severity": "WARNUNG",
                                                  "prerequisiteCode": "B21",
                                                  "message": "Voraussetzung B21 für B34 ist nicht erfüllt."
                                                },
                                                {
                                                  "severity": "WARNUNG",
                                                  "prerequisiteCode": "B24",
                                                  "message": "Voraussetzung B24 für B34 ist nicht erfüllt."
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                      ]
                                    }"""),
                            @ExampleObject(name = "Leerer Plan",
                                    description = "Noch nichts eingeplant: 200 mit leerer Liste, kein 404.",
                                    value = """
                                    {
                                      "startSemester": "WS 2026",
                                      "totalLp": 0,
                                      "maxLpPerSemester": 30,
                                      "semesters": []
                                    }""")
                    }))
    @ApiResponse(responseCode = "401",
            description = "Kein Token, Token ungültig bzw. abgelaufen, oder die Person existiert nicht mehr",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class),
                    examples = {
                            @ExampleObject(name = "Nicht angemeldet", value = """
                                    {
                                      "timestamp": "2026-11-21T12:00:00Z",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "Bitte melde dich an, um fortzufahren.",
                                      "path": "/api/plan"
                                    }"""),
                            @ExampleObject(name = "Konto gelöscht", value = """
                                    {
                                      "timestamp": "2026-11-21T12:00:00Z",
                                      "status": 401,
                                      "error": "Unauthorized",
                                      "message": "Dein Konto existiert nicht mehr. Bitte melde dich erneut an.",
                                      "path": "/api/plan"
                                    }""")
                    }))
    @ApiResponse(responseCode = "403", description = "Nur für Studierende (z. B. Token einer Professorin)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Falsche Rolle", value = """
                            {
                              "timestamp": "2026-11-21T12:00:00Z",
                              "status": 403,
                              "error": "Forbidden",
                              "message": "Dafür hast du keine Berechtigung.",
                              "path": "/api/plan"
                            }""")))
    public PlanResponse getPlan() {
        return planQueryService.eigenerPlan();
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
                    das Speichern nicht.

                    **Hinweis zu `offerCode`, `courseCode` und `courseTitle`:**
                    Bei festen Pflichtmodulen (`mit_auswahl = false`, z. B. B11, B33, B34) steht der
                    Modultitel bereits in der Datenbank fest – dort müssen alle drei Felder `null` sein.
                    Nur bei den sechs Auswahl-Modulen (`mit_auswahl = true`: B16, B26, B41, B54, B63, B64)
                    dürfen sie optional angegeben werden.""")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = PlanEntryRequest.class),
            examples = {
                    @ExampleObject(name = "Pflichtmodul (mit_auswahl = false)", value = """
                            {
                              "moduleCode": "B34",
                              "semester": "WS 2027",
                              "offerCode": null,
                              "courseCode": null,
                              "courseTitle": null
                            }"""),
                    @ExampleObject(name = "Wahlpflichtmodul mit Angebot (B41)", value = """
                            {
                              "moduleCode": "B41",
                              "semester": "SS 2027",
                              "offerCode": "WP1",
                              "courseCode": null,
                              "courseTitle": null
                            }"""),
                    @ExampleObject(name = "AWE-Modul mit freier Kurswahl (B63)", value = """
                            {
                              "moduleCode": "B63",
                              "semester": "SS 2027",
                              "offerCode": null,
                              "courseCode": "AWE-123",
                              "courseTitle": "Rhetorik und Kommunikation"
                            }""")
            }))
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
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class),
                    examples = {
                            @ExampleObject(name = "Vor dem Startsemester", value = """
                                    {
                                      "timestamp": "2026-11-14T12:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.",
                                      "path": "/api/plan/entries",
                                      "errors": [
                                        {
                                          "field": "semester",
                                          "message": "Das Semester liegt vor deinem Startsemester WS 2026."
                                        }
                                      ]
                                    }"""),
                            @ExampleObject(name = "Turnus-Verstoß", value = """
                                    {
                                      "timestamp": "2026-11-14T12:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.",
                                      "path": "/api/plan/entries",
                                      "errors": [
                                        {
                                          "field": "semester",
                                          "message": "B21 wird nur im Sommersemester angeboten, geplant ist WS 2027."
                                        }
                                      ]
                                    }"""),
                            @ExampleObject(name = "Auswahl-Angabe bei Pflichtmodul ohne Auswahl", value = """
                                    {
                                      "timestamp": "2026-11-14T12:00:00Z",
                                      "status": 400,
                                      "error": "Bad Request",
                                      "message": "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.",
                                      "path": "/api/plan/entries",
                                      "errors": [
                                        {
                                          "field": "courseTitle",
                                          "message": "Für das Modul B33 gibt es keine Auswahl."
                                        }
                                      ]
                                    }""")
                    }))
    @ApiResponse(responseCode = "401", description = "Kein oder ungültiges Token", content = @Content)
    @ApiResponse(responseCode = "403", description = "Nur für Studierende", content = @Content)
    @ApiResponse(responseCode = "404", description = "Modul wurde nicht gefunden",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Unbekanntes Modul", value = """
                            {
                              "timestamp": "2026-11-14T12:00:00Z",
                              "status": 404,
                              "error": "Not Found",
                              "message": "Modul mit dem Code B99 wurde nicht gefunden.",
                              "path": "/api/plan/entries"
                            }""")))
    @ApiResponse(responseCode = "409", description = "Modul ist bereits im Plan (bitte verschieben)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ApiError.class),
                    examples = @ExampleObject(name = "Bereits im Plan", value = """
                            {
                              "timestamp": "2026-11-14T12:00:00Z",
                              "status": 409,
                              "error": "Conflict",
                              "message": "Das Modul B34 ist bereits im Plan eingeplant. Bitte nutze das Verschieben, um das Semester zu ändern.",
                              "path": "/api/plan/entries"
                            }""")))
    public PlanEntryResponse createEntry(@Valid @RequestBody PlanEntryRequest request) {
        return planService.eintragAnlegen(request);
    }
}
