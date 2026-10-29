package de.htw.studyplanner.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.htw.studyplanner.backend.model.VoraussetzungsArt;

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
public record PrerequisiteDto(
        VoraussetzungsArt kind,
        String moduleCode,
        String moduleName,
        Integer minLp,
        Integer toleranceLp) {
}
