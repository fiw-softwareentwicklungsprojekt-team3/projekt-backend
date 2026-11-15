package de.htw.studyplanner.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import de.htw.studyplanner.backend.validation.HtwEmail;

/**
 * DTO für die Anmeldung eines Benutzers.
 *
 * @param email    E-Mail-Adresse des Kontos: @student.htw-berlin.de (Studierende)
 *                 oder @htw-berlin.de (Lehrende)
 * @param password Passwort im Klartext
 */
public record LoginRequest(
        @NotBlank(message = "Email darf nicht leer sein ")
        @Email(message = "Email ist ungültig ")
        @HtwEmail(domains = {HtwEmail.STUDIERENDE, HtwEmail.LEHRENDE},
                message = "Email muss auf @student.htw-berlin.de oder @htw-berlin.de enden")
        String email,

        @NotBlank(message = "Password darf nicht leer sein ")
        String password
) {
}