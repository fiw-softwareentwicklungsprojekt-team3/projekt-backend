package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Daten für die Registrierung ({@code POST /api/auth/register}).
 *
 * @param email         E-Mail-Adresse, muss eindeutig sein
 * @param password      Passwort im Klartext, mindestens 8 Zeichen; gespeichert wird nur der Hash
 * @param startsemester Startsemester im Format "WS JJJJ", z. B. "WS 2025"
 */
@Schema(description = "Daten für die Registrierung")
public record RegisterRequest(
        @Schema(description = "E-Mail-Adresse, muss eindeutig sein", example = "studentin@example.com")
        @NotBlank(message = "email darf nicht leer sein")
        @Email(message = "email ist ungültig")
        String email,

        @Schema(description = "Passwort, mindestens 8 Zeichen", example = "geheimesPasswort", minLength = 8)
        @NotBlank(message = "password darf nicht leer sein")
        @Size(min = 8, message = "password muss mindestens 8 Zeichen haben")
        String password,

        @Schema(description = "Startsemester, nur Wintersemester erlaubt", example = "WS 2025",
                pattern = "^WS \\d{4}$")
        @NotBlank(message = "startsemester darf nicht leer sein")
        @Pattern(
                regexp = "^WS \\d{4}$",
                message = "startsemester muss das Format WS JJJJ haben"
        )
        String startsemester
) {
}
