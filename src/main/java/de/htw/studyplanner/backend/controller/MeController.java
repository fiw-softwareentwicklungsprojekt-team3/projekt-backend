package de.htw.studyplanner.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.MeResponse;
import de.htw.studyplanner.backend.service.MeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Auskunft über die angemeldete Person.
 *
 * <p>Der Endpunkt liegt bewusst unter {@code /api/me} und nicht unter
 * {@code /api/auth/me}: Unter {@code /api/auth/**} ist POST ohne Token erlaubt
 * (siehe SecurityConfig). Ein eigener Pfad kann dort nicht versehentlich
 * mitfreigegeben werden.
 *
 * <p>Der Controller leitet nur weiter, die Logik steht in {@link MeService}.
 */
@RestController
@RequestMapping("/api/me")
@Tag(name = "Me", description = "Die angemeldete Person")
public class MeController {

    private final MeService meService;

    /**
     * Erstellt den Controller.
     *
     * @param meService Service, der die Person aus dem Token und der DB liest
     */
    public MeController(MeService meService) {
        this.meService = meService;
    }

    /**
     * Liefert ID, E-Mail, Rolle und Startsemester der angemeldeten Person.
     *
     * <p>Das Frontend ruft das nach einem Reload auf, um die Sitzung wiederherzustellen.
     *
     * @return die angemeldete Person ohne Passwort-Hash
     */
    @GetMapping
    @Operation(summary = "Angemeldete Person abfragen",
            description = """
                    Liefert die Kontodaten der Person, der das Token gehört.
                    Die Person wird aus dem Token gelesen (Subject = nutzende_id),
                    nie aus einem Parameter. Bei PROFESSOR_IN ist startSemester null.
                    Der Passwort-Hash ist nie Teil der Antwort.""")
    @ApiResponse(responseCode = "200", description = "Die angemeldete Person",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MeResponse.class),
                    examples = @ExampleObject(name = "Studierende", value = """
                            {
                              "id": 12,
                              "email": "studentin@example.com",
                              "role": "STUDIERENDE",
                              "startSemester": "WS 2026"
                            }""")))
    @ApiResponse(responseCode = "401",
            description = "Kein Token, Token ungültig bzw. abgelaufen, oder die Person existiert nicht mehr",
            content = @Content)
    public MeResponse me() {
        return meService.aktuellesKonto();
    }
}
