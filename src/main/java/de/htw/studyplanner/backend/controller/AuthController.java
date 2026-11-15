package de.htw.studyplanner.backend.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import de.htw.studyplanner.backend.dto.LoginRequest;
import de.htw.studyplanner.backend.dto.LoginResponse;
import de.htw.studyplanner.backend.dto.ProfessorRegisterRequest;
import de.htw.studyplanner.backend.dto.ProfessorRegisterResponse;
import de.htw.studyplanner.backend.dto.RegisterRequest;
import de.htw.studyplanner.backend.dto.RegisterResponse;
import de.htw.studyplanner.backend.service.LoginService;
import de.htw.studyplanner.backend.service.ProfessorRegistrationService;
import de.htw.studyplanner.backend.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Registrierung (Studierende und Professor:innen) und Login.
 * Der Controller leitet nur weiter, die Logik steht in den Services.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registrierung und Login")
public class AuthController {

    private final RegistrationService registrationService;
    private final ProfessorRegistrationService professorRegistrationService;
    private final LoginService loginService;

    /**
     * Erstellt den Controller.
     *
     * @param registrationService          Service, der neue Studierende anlegt
     * @param professorRegistrationService Service, der Professor:innen mit Einladungscode anlegt
     * @param loginService                 Service, der den Login prüft und das Token erzeugt
     */
    public AuthController(RegistrationService registrationService,
                          ProfessorRegistrationService professorRegistrationService,
                          LoginService loginService) {
        this.registrationService = registrationService;
        this.professorRegistrationService = professorRegistrationService;
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
    @ApiResponse(responseCode = "400", description = "Eingabe ungültig, z. B. E-Mail falsch oder Passwort zu kurz. Alle fehlerhaften Felder werden in errors gelistet.")
    @ApiResponse(responseCode = "409", description = "Die E-Mail ist bereits vergeben")
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return registrationService.register(request);
    }

    /**
     * Registriert eine Professor:in mit Einladungscode.
     * Antwortet mit 400 bei ungültigen Eingaben oder ungültigem Code und mit 409,
     * wenn die E-Mail schon vergeben ist.
     *
     * @param request E-Mail, Passwort (mindestens 8 Zeichen) und Einladungscode
     * @return das angelegte Konto ohne Passwort
     */
    @PostMapping("/register/professor")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Professor:in mit Einladungscode registrieren",
            description = """
                    Legt ein Konto mit der Rolle PROFESSOR_IN an. Dafür ist ein Einladungscode nötig,
                    den das Team ausgibt (siehe Wiki „Einladungscodes anlegen“). Jeder Code kann genau
                    einmal eingelöst werden.

                    E-Mail- und Passwort-Regeln sind dieselben wie bei `POST /api/auth/register`.
                    Ist die E-Mail schon vergeben, kommt 409, egal welcher Code mitgeschickt wird;
                    der Code wird dann nicht verbraucht.

                    Ist der Code unbekannt, abgelaufen oder schon eingelöst, kommt immer dieselbe
                    Meldung, damit man gültige Codes nicht erraten kann.""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProfessorRegisterRequest.class),
                            examples = @ExampleObject(
                                    name = "Registrierung mit Code",
                                    value = """
                                            {
                                              "email": "professorin@htw-berlin.de",
                                              "password": "geheimesPasswort",
                                              "code": "K7MPX4QRT9WZ"
                                            }"""))))
    @ApiResponse(responseCode = "201", description = "Konto mit Rolle PROFESSOR_IN wurde angelegt",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ProfessorRegisterResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 42,
                              "email": "professorin@htw-berlin.de",
                              "rolle": "PROFESSOR_IN"
                            }""")))
    @ApiResponse(responseCode = "400", description = """
            Eingabe ungültig (E-Mail, Passwort, Code fehlt) oder der Einladungscode ist unbekannt,
            abgelaufen oder schon eingelöst. Beim Code lautet die Meldung immer
            „Der Einladungscode ist ungültig.“""",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(name = "Code ungültig", value = """
                            {
                              "status": 400,
                              "error": "Bad Request",
                              "message": "Der Einladungscode ist ungültig.",
                              "path": "/api/auth/register/professor",
                              "errors": [
                                { "field": "code", "message": "Der Einladungscode ist ungültig." }
                              ]
                            }""")))
    @ApiResponse(responseCode = "409", description = "Die E-Mail ist bereits vergeben", content = @Content)
    public ProfessorRegisterResponse registerProfessor(
            @Valid @RequestBody ProfessorRegisterRequest request) {

        return professorRegistrationService.register(request);
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