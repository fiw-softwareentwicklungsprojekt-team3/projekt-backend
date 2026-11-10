package de.htw.studyplanner.backend.controller;

import de.htw.studyplanner.backend.dto.ApiError;
import de.htw.studyplanner.backend.dto.ApiFieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import de.htw.studyplanner.backend.exception.ConflictException;
import de.htw.studyplanner.backend.exception.FieldValidationException;
import de.htw.studyplanner.backend.exception.NotFoundException;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.service.InvalidCredentialsException;
import de.htw.studyplanner.backend.service.InvalidSemesterFormatException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // 400: @Valid, alle Feldfehler kommen in "errors"
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiFieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST,
                "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.", req, errors);
    }

    // 400: Validierung von Parametern (@Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<ApiFieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ApiFieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST,
                "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.", req, errors);
    }

    // 400: ungültiges JSON im Body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Die Anfrage enthält ungültige oder unvollständige Daten.", req, null);
    }

    // 400: z. B. /api/modules/abc statt Zahl
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Der Wert für '" + ex.getName() + "' hat ein ungültiges Format.", req, null);
    }

    // 400: Semester mit semester in errors
    @ExceptionHandler(InvalidSemesterFormatException.class)
    public ResponseEntity<ApiError> handleInvalidSemester(InvalidSemesterFormatException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Das Semester hat ein ungültiges Format.", req,
                List.of(new ApiFieldError(ex.getFeld(), ex.getMessage())));
    }

    // 400: fachliche Regel verletzt, das Feld kommt in "errors"
    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiError> handleFieldValidation(FieldValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.", req,
                List.of(new ApiFieldError(ex.getField(), ex.getMessage())));
    }

    // 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    // 404: unbekannte URL (sonst würde sie als 500 enden)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Die angeforderte Ressource wurde nicht gefunden.", req, null);
    }

    // 405
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Diese HTTP-Methode wird hier nicht unterstützt.", req, null);
    }

    // 409: eigene Konflikte (E-Mail vergeben)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    // 409: UNIQUE-Verletzung in der DB (ohne SQL in der Antwort)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Datenbank-Konflikt bei {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Dieser Eintrag existiert bereits oder verletzt eine Bedingung der Datenbank.", req, null);
    }

    // 401/403 aus Controllern bzw. Method-Security
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Bitte melde dich an, um fortzufahren.", req, null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    // 401: Token gültig, aber die Person existiert nicht mehr (sonst wäre das eine 500)
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Dafür hast du keine Berechtigung.", req, null);
    }

    // Alte Lösungen mit ResponseStatusException bleiben im selben Format
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : "Die Anfrage konnte nicht verarbeitet werden.";
        return build(status, message, req, null);
    }

    // 500: alles andere, kein Stacktrace in der Antwort, aber im Log
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unerwarteter Fehler bei {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es später erneut.", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                           List<ApiFieldError> errors) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, req.getRequestURI(), errors));
    }
}
