package de.htw.studyplanner.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import de.htw.studyplanner.backend.service.EmailAlreadyExistsException;
import de.htw.studyplanner.backend.service.InvalidSemesterFormatException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> emailAlreadyExists() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "field", "email",
                        "message", "E-Mail ist bereits vergeben"
                ));
    }

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validationError(
            MethodArgumentNotValidException exception) {

        var error = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .orElseThrow();

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                ));
    }
}
