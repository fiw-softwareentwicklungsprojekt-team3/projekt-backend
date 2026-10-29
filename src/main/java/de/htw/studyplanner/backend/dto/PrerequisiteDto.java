package de.htw.studyplanner.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Eine Voraussetzung eines Moduls: entweder ein Modul (moduleCode, moduleName)
 * oder Mindest-Leistungspunkte (minLp). Felder ohne Wert fehlen in der JSON-Antwort.
 *
 * @param kind        NOTWENDIG oder EMPFOHLEN
 * @param moduleCode  Code des vorausgesetzten Moduls
 * @param moduleName  Bezeichnung des vorausgesetzten Moduls
 * @param minLp       geforderte Mindest-Leistungspunkte
 * @param toleranceLp erlaubte Unterschreitung der Mindest-LP (nur mit minLp)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Voraussetzung: entweder ein Modul oder Mindest-Leistungspunkte")
public record PrerequisiteDto(
        @Schema(description = "NOTWENDIG oder EMPFOHLEN", example = "NOTWENDIG")
        VoraussetzungsArt kind,
        @Schema(description = "Code des vorausgesetzten Moduls; fehlt bei LP-Voraussetzungen", example = "B11")
        String moduleCode,
        @Schema(description = "Bezeichnung des vorausgesetzten Moduls; fehlt bei LP-Voraussetzungen",
                example = "Programmierung 1")
        String moduleName,
        @Schema(description = "Mindest-Leistungspunkte; fehlt bei Modul-Voraussetzungen", example = "151")
        Integer minLp,
        @Schema(description = "Erlaubte Unterschreitung der Mindest-LP; nur zusammen mit minLp", example = "10")
        Integer toleranceLp) {
}
