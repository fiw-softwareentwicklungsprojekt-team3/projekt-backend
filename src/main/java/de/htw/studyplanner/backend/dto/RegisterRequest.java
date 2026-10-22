package de.htw.studyplanner.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email darf nicht leer sein")
        @Email(message = "email ist ungültig")
        String email,

        @NotBlank(message = "password darf nicht leer sein")
        @Size(min = 8, message = "password muss mindestens 8 Zeichen haben")
        String password,

        @NotBlank(message = "startsemester darf nicht leer sein")
        @Pattern(
                regexp = "^WS \\d{4}$",
                message = "startsemester muss das Format WS JJJJ haben"
        )
        String startsemester
) {
}
