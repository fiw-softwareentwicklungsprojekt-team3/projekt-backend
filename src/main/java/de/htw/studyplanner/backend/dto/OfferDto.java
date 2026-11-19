package de.htw.studyplanner.backend.dto;

import de.htw.studyplanner.backend.model.AngebotsGruppe;
import de.htw.studyplanner.backend.model.Wahlangebot;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ein Wahlangebot (Sprache oder Wahlpflicht-Bereich), wie es die API ausgibt.
 *
 * <p>Die Entity {@link Wahlangebot} geht nie direkt in eine Antwort, sonst würden
 * interne Felder und die Lazy-Relation zum empfohlenen Modul nach außen gelangen.
 *
 * @param code                  Code des Angebots, z. B. {@code WP7} oder {@code EN_B2.1}
 * @param name                  Bezeichnung
 * @param group                 {@code WP} oder {@code SPRACHE}
 * @param level                 Niveaustufe {@code 1a} oder {@code 1b}; null, wenn keine Angabe
 * @param sws                   Semesterwochenstunden; null, wenn keine Angabe
 * @param recommendedModuleCode empfohlene Voraussetzung (EV laut Anlage 4); null, wenn keine
 */
@Schema(description = "Ein Wahlangebot (Sprache oder Wahlpflicht-Bereich)")
public record OfferDto(
        @Schema(description = "Code des Angebots", example = "EN_B2.1")
        String code,

        @Schema(description = "Bezeichnung", example = "Englisch Fachsprache B2.1 (Technik)")
        String name,

        @Schema(description = "WP (Wahlpflicht) oder SPRACHE", example = "SPRACHE")
        AngebotsGruppe group,

        @Schema(description = "Niveaustufe 1a oder 1b; null, wenn keine Angabe", example = "1a", nullable = true)
        String level,

        @Schema(description = "Semesterwochenstunden; null, wenn keine Angabe", example = "4", nullable = true)
        Integer sws,

        @Schema(description = "Empfohlene Voraussetzung laut Anlage 4; null, wenn keine",
                example = "B31", nullable = true)
        String recommendedModuleCode) {

    /**
     * Baut das DTO aus der Entity. Muss innerhalb einer Transaktion aufgerufen werden,
     * weil das empfohlene Modul lazy geladen wird.
     *
     * @param w das Wahlangebot
     * @return das DTO
     */
    public static OfferDto of(Wahlangebot w) {
        return new OfferDto(
                w.getCode(),
                w.getBezeichnung(),
                w.getGruppe(),
                w.getNiveaustufe(),
                w.getSws(),
                w.getEmpfohlenesModul() == null ? null : w.getEmpfohlenesModul().getCode());
    }
}
