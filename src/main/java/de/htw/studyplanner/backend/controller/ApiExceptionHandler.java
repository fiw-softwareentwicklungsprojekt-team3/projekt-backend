package de.htw.studyplanner.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.htw.studyplanner.backend.dto.FieldErrorResponse;
import de.htw.studyplanner.backend.service.EmailAlreadyExistsException;
import de.htw.studyplanner.backend.service.InvalidCredentialsException;

/**
 * Wandelt Fehler aus den Controllern in eine {@link FieldErrorResponse} um,
 * damit das Frontend die Meldung direkt am passenden Eingabefeld anzeigen kann.
 *
 * <p>Fehler, die hier nicht behandelt werden (z. B. 404 aus dem {@code ModuleService}),
 * beantwortet Spring Boot mit seinem Standard-Fehlerformat.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Antwortet mit 409, wenn die E-Mail bei der Registrierung schon vergeben ist.
     *
     * @return Status 409 mit Feld {@code email} und Fehlermeldung
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<FieldErrorResponse> emailAlreadyExists() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new FieldErrorResponse("email", "E-Mail ist bereits vergeben"));
    }

 feature/E2.3-login-jwt
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> invalidCredentials(
            InvalidCredentialsException exception) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "message", exception.getMessage()
                ));
    }


    /**
     * Antwortet mit 400, wenn ein Request-Body die Validierung nicht besteht.
     * Zurückgegeben wird nur der erste fehlerhafte Wert.
     *
     * @param exception der Validierungsfehler von Spring
     * @return Status 400 mit dem Namen des Feldes und der Fehlermeldung
     */
main
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FieldErrorResponse> validationError(
            MethodArgumentNotValidException exception) {

        var error = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .orElseThrow();

        return ResponseEntity
                .badRequest()
                .body(new FieldErrorResponse(error.getField(), error.getDefaultMessage()));
    }
}