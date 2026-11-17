package de.htw.studyplanner.backend.dto;

import java.util.List;

import de.htw.studyplanner.backend.model.Bereich;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.ModulArt;
import de.htw.studyplanner.backend.model.Turnus;
import io.swagger.v3.oas.annotations.media.Schema;

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
 * @param offers               wählbare Angebote, sortiert nach Code; nur bei B16, B26, B41 und B54,
 *                             sonst leer (auch bei B63/B64, dort gibt es keine feste Auswahl)
 */
@Schema(description = "Ein Modul mit allen Angaben und Voraussetzungen")
public record ModuleDetailDto(
        @Schema(description = "Modulcode", example = "B34")
        String code,
        @Schema(description = "Bezeichnung", example = "Softwareentwicklungsprojekt")
        String name,
        @Schema(description = "Fachsemester laut Studienplan (1 bis 6)", example = "3")
        int semester,
        @Schema(description = "Pflicht (P) oder Wahlpflicht (WP)", example = "P")
        ModulArt type,
        @Schema(description = "FACH oder AWE", example = "FACH")
        Bereich area,
        @Schema(description = "true, wenn bei diesem Modul ein Angebot gewählt wird", example = "false")
        boolean hasChoice,
        @Schema(description = "Leistungspunkte", example = "10")
        int lp,
        @Schema(description = "SWS Vorlesung; null, wenn keine Angabe", example = "0", nullable = true)
        Integer lectureHours,
        @Schema(description = "SWS Übung, Projekt und Seminar", example = "6")
        int exerciseHours,
        @Schema(description = "Niveaustufe 1a oder 1b; null bei Wahlpflichtmodulen", example = "1b", nullable = true)
        String level,
        @Schema(description = "Angebotssemester", example = "WS")
        Turnus term,
        @Schema(description = "true, wenn das Modul in die Gesamtnote eingeht", example = "true")
        boolean countsForFinalGrade,
        @Schema(description = "false bei unbenoteten Modulen (Fachpraktikum)", example = "true")
        boolean graded,
        @Schema(description = "Voraussetzungen, notwendige zuerst; leer, wenn es keine gibt")
        List<PrerequisiteDto> prerequisites,
        @Schema(description = "Wählbare Angebote (Sprachen oder Wahlpflicht-Bereiche), sortiert nach Code. "
                + "Nur bei B16, B26, B41 und B54; sonst leer, auch bei B63/B64 (freie Wahl).")
        List<OfferDto> offers) {

    /**
     * Wandelt ein {@link Modul} samt bereits umgewandelter Voraussetzungen und Angebote
     * in die Detailfassung um.
     *
     * @param m             das Modul aus der Datenbank
     * @param prerequisites die Voraussetzungen, bereits sortiert
     * @param offers        die wählbaren Angebote, bereits sortiert; leer, wenn es keine gibt
     * @return die Detailfassung des Moduls
     */
    public static ModuleDetailDto of(Modul m, List<PrerequisiteDto> prerequisites, List<OfferDto> offers) {
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
                prerequisites,
                offers);
    }
}
