package de.htw.studyplanner.backend.dto;

import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.ModulArt;
import de.htw.studyplanner.backend.model.Turnus;

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
public record ModuleDto(
        String code,
        String name,
        int semester,
        ModulArt type,
        int lp,
        Integer lectureHours,
        int exerciseHours,
        Turnus term) {

    /** Wandelt ein {@link Modul} in die Kurzfassung um. */
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
