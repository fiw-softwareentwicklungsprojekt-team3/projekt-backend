package de.htw.studyplanner.backend.dto;

import java.util.List;

import de.htw.studyplanner.backend.model.Bereich;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.ModulArt;
import de.htw.studyplanner.backend.model.Turnus;

/**
 * Ein Modul mit allen Angaben und seinen Voraussetzungen ({@code GET /api/modules/{code}}).
 *
 * @param code                 Modulcode, z. B. "B34"
 * @param name                 Bezeichnung des Moduls
 * @param semester             Fachsemester laut Studienplan (1 bis 6)
 * @param type                 Pflicht (P) oder Wahlpflicht (WP)
 * @param area                 FACH oder AWE
 * @param hasChoice            {@code true}, wenn bei diesem Modul ein Angebot gewählt wird
 * @param lp                   Leistungspunkte
 * @param lectureHours         Semesterwochenstunden Vorlesung; {@code null}, wenn keine Angabe
 * @param exerciseHours        Semesterwochenstunden Übung, Projekt und Seminar
 * @param level                Niveaustufe "1a" oder "1b"; {@code null} bei Wahlpflichtmodulen
 * @param term                 Angebotssemester: WS, SS oder BEIDE
 * @param countsForFinalGrade  {@code true}, wenn das Modul in die Gesamtnote eingeht
 * @param graded               {@code false} bei unbenoteten Modulen (Fachpraktikum)
 * @param prerequisites        Voraussetzungen, notwendige zuerst; leer, wenn es keine gibt
 */
public record ModuleDetailDto(
        String code,
        String name,
        int semester,
        ModulArt type,
        Bereich area,
        boolean hasChoice,
        int lp,
        Integer lectureHours,
        int exerciseHours,
        String level,
        Turnus term,
        boolean countsForFinalGrade,
        boolean graded,
        List<PrerequisiteDto> prerequisites) {

    /** Wandelt ein {@link Modul} samt bereits umgewandelter Voraussetzungen in die Detailfassung um. */
    public static ModuleDetailDto of(Modul m, List<PrerequisiteDto> prerequisites) {
        return new ModuleDetailDto(
                m.getCode(),
                m.getBezeichnung(),
                m.getFachsemester(),
                m.getArt(),
                m.getBereich(),
                m.isMitAuswahl(),
                m.getLp(),
                m.getSwsVorlesung(),
                m.getSwsUebung(),
                m.getNiveaustufe(),
                m.getTurnus(),
                m.isZaehltInX1(),
                m.isBenotet(),
                prerequisites);
    }
}
