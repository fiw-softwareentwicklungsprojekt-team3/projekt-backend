package de.htw.studyplanner.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;

@Schema(description = "Einheitliches Fehlerformat aller Backend-Fehler")
@JsonInclude(JsonInclude.Include.NON_NULL) 
public record ApiError(
@JsonInclude(JsonInclude.Include.NON_NULL) // 
        @Schema(example = "2026-10-15T09:00:00Z") String timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Die Eingabe ist ungültig. Bitte prüfe die markierten Felder.") String message,
        @Schema(example = "/api/auth/register") String path,
        @Schema(description = "Nur bei Validierungsfehlern") List<ApiFieldError> errors) {

    public static ApiError of(HttpStatus status, String message, String path, List<ApiFieldError> errors) {
        return new ApiError(
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                errors);
    }
}