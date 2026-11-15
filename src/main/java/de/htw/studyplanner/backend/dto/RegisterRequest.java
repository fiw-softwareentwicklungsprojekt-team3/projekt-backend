package de.htw.studyplanner.backend.dto;

import de.htw.studyplanner.backend.dto.validation.GueltigeEmail;
import de.htw.studyplanner.backend.dto.validation.GueltigesPasswort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import de.htw.studyplanner.backend.validation.HtwEmail;

/**
 * Daten für die Registrierung ({@code POST /api/auth/register}).
 *
<<<<<<< HEAD
 * <p>E-Mail- und Passwort-Regeln kommen aus {@link GueltigeEmail} und
 * {@link GueltigesPasswort}, damit sie für alle Registrierungen gleich sind.
 *
 * @param email         E-Mail-Adresse, muss eindeutig sein
=======
 * @param email         HTW-Adresse der Studierenden (@student.htw-berlin.de), muss eindeutig sein
>>>>>>> 34d57f9d165490ee6450df99fba80ab3a79835c0
 * @param password      Passwort im Klartext, mindestens 8 Zeichen; gespeichert wird nur der Hash
 * @param startsemester Startsemester im Format "WS JJJJ", z. B. "WS 2025"
 */
@Schema(description = "Daten für die Registrierung")
public record RegisterRequest(
<<<<<<< HEAD
        @Schema(description = "E-Mail-Adresse, muss eindeutig sein", example = "studentin@example.com")
        @GueltigeEmail
=======
        @Schema(description = "E-Mail-Adresse, muss eindeutig sein und auf @student.htw-berlin.de enden",
                example = "studentin@student.htw-berlin.de")
        @NotBlank(message = "email darf nicht leer sein")
        @Email(message = "email ist ungültig")
        @HtwEmail(domains = HtwEmail.STUDIERENDE,
                message = "email muss auf @student.htw-berlin.de enden")
>>>>>>> 34d57f9d165490ee6450df99fba80ab3a79835c0
        String email,

        @Schema(description = "Passwort, mindestens 8 Zeichen", example = "geheimesPasswort", minLength = 8)
        @GueltigesPasswort
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
