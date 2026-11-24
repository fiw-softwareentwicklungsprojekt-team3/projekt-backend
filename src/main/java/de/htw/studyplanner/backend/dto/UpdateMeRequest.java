package de.htw.studyplanner.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Body von {@code PUT /api/me}: Startsemester der angemeldeten Studierenden ändern.
 *
 * <p>Nur Studierende können ihr Startsemester ändern; Professor_innen haben keins.
 * Das Format muss exakt {@code WS JJJJ} entsprechen, da Studierende laut Prüfungsordnung
 * nur im Wintersemester beginnen dürfen.
 *
 * @param startSemester neues Startsemester im Format "WS JJJJ"
 */
@Schema(description = "Anfrage zum Ändern des Startsemesters")
public record UpdateMeRequest(

        @NotBlank(message = "startSemester darf nicht leer sein")
        @Pattern(regexp = "WS \\d{4}", message = "startSemester muss das Format \"WS JJJJ\" haben")
        @Schema(description = "Neues Startsemester im Format \"WS JJJJ\"", example = "WS 2025")
        String startSemester
) {
}
