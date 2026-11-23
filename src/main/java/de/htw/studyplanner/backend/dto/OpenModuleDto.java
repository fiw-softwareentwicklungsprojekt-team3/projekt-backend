package de.htw.studyplanner.backend.dto;

import java.util.List;

import de.htw.studyplanner.backend.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Pflichtmodul, das noch nicht bestanden ist (Teil von {@link ProgressResponse}).
 *
 * @param moduleCode          Modulcode, z. B. B34
 * @param moduleName          Bezeichnung des Moduls
 * @param recommendedSemester empfohlenes Fachsemester laut Studienplan
 * @param lp                  Leistungspunkte des Moduls
 * @param resultStatus        aktueller Stand der Leistung: BELEGT, NICHT_BESTANDEN oder {@code null}
 * @param locked              {@code true}, wenn eine notwendige Voraussetzung nicht erfüllt ist
 * @param lockReasons         Gründe für die Sperre, leer wenn das Modul frei ist
 */
@Schema(description = "Noch nicht bestandenes Pflichtmodul")
public record OpenModuleDto(
        @Schema(example = "B34") String moduleCode,
        @Schema(example = "Softwareentwicklungsprojekt") String moduleName,
        @Schema(description = "Empfohlenes Fachsemester", example = "3") int recommendedSemester,
        @Schema(example = "10") int lp,
        @Schema(description = "BELEGT, NICHT_BESTANDEN oder null (noch keine Leistung)",
                nullable = true, example = "BELEGT") Status resultStatus,
        @Schema(description = "Gesperrt, weil eine notwendige Voraussetzung fehlt") boolean locked,
        @Schema(description = "Warum das Modul gesperrt ist", example = "[\"Programmierung 1 fehlt noch\"]")
        List<String> lockReasons) {
}
