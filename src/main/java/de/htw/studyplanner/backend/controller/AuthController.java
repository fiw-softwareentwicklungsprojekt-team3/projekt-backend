package de.htw.studyplanner.backend.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.htw.studyplanner.backend.dto.FieldErrorResponse;
import de.htw.studyplanner.backend.dto.LoginRequest;
import de.htw.studyplanner.backend.dto.LoginResponse;
import de.htw.studyplanner.backend.dto.RegisterRequest;
import de.htw.studyplanner.backend.dto.RegisterResponse;
import de.htw.studyplanner.backend.service.LoginService;
import de.htw.studyplanner.backend.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Registrierung neuer Studierender.
 * Der Controller leitet nur weiter, die Logik steht in {@link RegistrationService}.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registrierung und Login")
public class AuthController {

    private final RegistrationService registrationService;
    /**
     * Antwortet mit 401, wenn E-Mail oder Passwort beim Login falsch sind.
     * Die Meldung verrät nicht, welches von beiden falsch ist.
     *
     * @param exception der Fehler aus dem LoginService
     * @return Status 401 mit der Meldung im Feld {@code message}
     */
    @ExceptionHandler(InvalidCredentialsException.class)

    /**
     * Erstellt den Controller.
     *
     * @param registrationService Service, der neue Studierende anlegt
     * @param loginService        Service, der den Login prüft und das Token erzeugt
     */
    public AuthController(RegistrationService registrationService, LoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    /**
     * Registriert eine neue Person mit der Rolle STUDIERENDE.
     * Antwortet mit 400 bei ungültigen Eingaben und mit 409, wenn die E-Mail schon vergeben ist.
     *
     * @param request E-Mail, Passwort (mindestens 8 Zeichen) und Startsemester im Format "WS JJJJ"
     * @return die angelegte Person ohne Passwort
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Neue Studierende registrieren",
            description = """
                    Legt eine Person mit der Rolle STUDIERENDE an. Die Rolle setzt immer das Backend.
                    Das Studium beginnt immer im Wintersemester, deshalb ist nur `WS JJJJ` erlaubt.
                    Gibt es das Startsemester noch nicht, wird es angelegt.""")
    @ApiResponse(responseCode = "201", description = "Person wurde angelegt")
    @ApiResponse(responseCode = "400",
            description = "Eingabe ungültig, z. B. E-Mail falsch, Passwort zu kurz oder Startsemester nicht im Format WS JJJJ. "
                    + "Es wird nur das erste fehlerhafte Feld gemeldet.",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FieldErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "field": "password",
                              "message": "password muss mindestens 8 Zeichen haben"
                            }""")))
    @ApiResponse(responseCode = "409", description = "Die E-Mail ist bereits vergeben",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FieldErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "field": "email",
                              "message": "E-Mail ist bereits vergeben"
                            }""")))
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return registrationService.register(request);
    }

    /**
     * Meldet eine Person an und liefert ein JWT.
     * Antwortet mit 401, wenn E-Mail oder Passwort falsch sind.
     *
     * @param request E-Mail und Passwort
     * @return Token, Gültigkeit in Sekunden und Rolle
     */
    @PostMapping("/login")
    @Operation(summary = "Anmelden", description = "Prüft E-Mail und Passwort und liefert ein JWT.")
    @ApiResponse(responseCode = "200", description = "Login erfolgreich")
    @ApiResponse(responseCode = "401", description = "E-Mail oder Passwort falsch")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {

        return loginService.login(request);
    }
}