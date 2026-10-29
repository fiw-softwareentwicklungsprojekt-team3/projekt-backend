package de.htw.studyplanner.backend.dto;

import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.ModulArt;
import de.htw.studyplanner.backend.model.Turnus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kurzfassung eines Moduls für die Modulliste ({@code GET /api/modules}).
 *
 * @param code          Modulcode, z. B. "B34"
 * @param name          Bezeichnung des Moduls
 * @param semester      Fachsemester laut Studienplan (1 bis 6)
 * @param type          Pflicht (P) oder Wahlpflicht (WP)
 * @param lp            Leistungspunkte
 * @param lectureHours  Semesterwochenstunden Vorlesung; {@code null}, wenn keine Angabe
 * @param exerciseHours Semesterwochenstunden Übung, Projekt und Seminar
 * @param term          Angebotssemester: WS, SS oder BEIDE
 */
@Schema(description = "Kurzfassung eines Moduls")
public record ModuleDto(
        @Schema(description = "Modulcode", example = "B34")
        String code,
        @Schema(description = "Bezeichnung", example = "Softwareentwicklungsprojekt")
        String name,
        @Schema(description = "Fachsemester laut Studienplan (1 bis 6)", example = "3")
        int semester,
        @Schema(description = "Pflicht (P) oder Wahlpflicht (WP)", example = "P")
        ModulArt type,
        @Schema(description = "Leistungspunkte", example = "10")
        int lp,
        @Schema(description = "SWS Vorlesung; null, wenn keine Angabe", example = "0", nullable = true)
        Integer lectureHours,
        @Schema(description = "SWS Übung, Projekt und Seminar", example = "6")
        int exerciseHours,
        @Schema(description = "Angebotssemester", example = "WS")
        Turnus term) {

    /**
     * Wandelt ein {@link Modul} in die Kurzfassung um.
     *
     * @param m das Modul aus der Datenbank
     * @return die Kurzfassung für die Modulliste
     */
    public static ModuleDto of(Modul m) {
        return new ModuleDto(
                m.getCode(),
                m.getBezeichnung(),
                m.getFachsemester(),
                m.getArt(),
                m.getLp(),
                m.getSwsVorlesung(),
                m.getSwsUebung(),
                m.getTurnus());
    }
}
