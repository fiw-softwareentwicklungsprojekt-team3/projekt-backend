package de.htw.studyplanner.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htw.studyplanner.backend.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Schreibt eine Fehlerantwort im einheitlichen Format ({@link ApiError}) direkt in die Antwort.
 *
 * <p>Wird dort gebraucht, wo Filter und Handler von Spring Security antworten. Dort greift
 * der normale {@code ApiExceptionHandler} nicht.
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeError(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError apiError = ApiError.of(status, message, request.getRequestURI(), null);

        objectMapper.writeValue(response.getWriter(), apiError);
    }
}