package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Fehlerantwort, die sich auf ein einzelnes Eingabefeld bezieht.
 * Wird bei Validierungsfehlern (400) und bei einer bereits vergebenen E-Mail (409) gesendet,
 * damit das Frontend die Meldung direkt am passenden Feld anzeigen kann.
 *
 * @param field   Name des betroffenen Feldes im Request
 * @param message verständliche Fehlermeldung auf Deutsch
 */
@Schema(description = "Fehler zu einem einzelnen Eingabefeld")
public record FieldErrorResponse(
        @Schema(description = "Name des betroffenen Feldes", example = "email")
        String field,

        @Schema(description = "Fehlermeldung auf Deutsch", example = "E-Mail ist bereits vergeben")
        String message) {
}
