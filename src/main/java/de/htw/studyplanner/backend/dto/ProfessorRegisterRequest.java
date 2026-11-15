package de.htw.studyplanner.backend.dto;

import de.htw.studyplanner.backend.dto.validation.GueltigeEmail;
import de.htw.studyplanner.backend.dto.validation.GueltigesPasswort;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import de.htw.studyplanner.backend.validation.HtwEmail;

/**
 * Daten für die Registrierung von Professor:innen ({@code POST /api/auth/register/professor}).
 *
 * <p>E-Mail- und Passwort-Regeln sind dieselben wie bei Studierenden
 * ({@link GueltigeEmail}, {@link GueltigesPasswort}). Die E-Mail muss auf
 * {@code @htw-berlin.de} enden, denn nur damit ist später der Login möglich.
 *
 * @param email    HTW-Adresse der Lehrenden ({@code @htw-berlin.de}), muss eindeutig sein
 * @param password Passwort im Klartext, mindestens 8 Zeichen; gespeichert wird nur der Hash
 * @param code     Einladungscode, den das Team ausgegeben hat
 */
@Schema(description = "Daten für die Registrierung von Professor:innen")
public record ProfessorRegisterRequest(
        @Schema(description = "E-Mail-Adresse, muss eindeutig sein und auf @htw-berlin.de enden",
                example = "professorin@htw-berlin.de")
        @GueltigeEmail
        @HtwEmail(domains = HtwEmail.LEHRENDE,
                message = "email muss auf @htw-berlin.de enden")
        String email,

        @Schema(description = "Passwort, mindestens 8 Zeichen", example = "geheimesPasswort", minLength = 8)
        @GueltigesPasswort
        String password,

        @Schema(description = "Einladungscode (12 Zeichen, Groß-/Kleinschreibung egal)", example = "K7MPX4QRT9WZ")
        @NotBlank(message = "code darf nicht leer sein")
        String code
) {
}
