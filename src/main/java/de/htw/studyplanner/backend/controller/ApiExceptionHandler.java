package de.htw.studyplanner.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.htw.studyplanner.backend.dto.FieldErrorResponse;
import de.htw.studyplanner.backend.service.EmailAlreadyExistsException;
import de.htw.studyplanner.backend.service.InvalidSemesterFormatException;

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

    /**
     * Antwortet mit 400, wenn ein Semester-String nicht dem Format "WS JJJJ" oder "SS JJJJ" entspricht.
     *
     * @param exception der Fehler aus dem SemesterService
     * @return Status 400 mit Feld {@code semester} und Fehlermeldung
     */
    @ExceptionHandler(InvalidSemesterFormatException.class)
    public ResponseEntity<Map<String, String>> invalidSemesterFormat(
            InvalidSemesterFormatException exception) {
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "field", "semester",
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
