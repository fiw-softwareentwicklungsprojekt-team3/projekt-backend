package de.htw.studyplanner.backend.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Antwort von {@code GET /api/progress} (US-58): Studienfortschritt der angemeldeten Person.
 *
 * @param earnedLp               Summe der LP aller bestandenen Leistungen
 * @param totalLp                LP des ganzen Studiums (180)
 * @param progressPercent        {@code earnedLp} in Prozent von {@code totalLp}, eine Nachkommastelle
 * @param averageGrade           LP-gewichteter Notenschnitt, auf eine Nachkommastelle abgeschnitten;
 *                               {@code null}, wenn es noch keine Note gibt
 * @param bachelorThesisEligible {@code true}, wenn die Bachelorarbeit (B66) regulär angemeldet werden darf:
 *                               Sem. 1–3 bestanden und mindestens 151 LP (StuPO § 12)
 * @param bachelorThesisNeedsApproval {@code true} bei Sem. 1–3 bestanden und 141–150 LP: Anmeldung nur mit
 *                               Zulassung durch den Prüfungsausschuss (RStPO § 25 Abs. 1)
 * @param openModules            noch nicht bestandene Pflichtmodule, sortiert nach Fachsemester und Code
 */
@Schema(description = "Studienfortschritt der angemeldeten Person")
public record ProgressResponse(
        @Schema(example = "65") int earnedLp,
        @Schema(example = "180") int totalLp,
        @Schema(example = "36.1") double progressPercent,
        @Schema(description = "LP-gewichtet, abgeschnitten (RStPO); null ohne Noten",
                nullable = true, example = "2.3") BigDecimal averageGrade,
        @Schema(description = "Bachelorarbeit (B66) regulär möglich: Sem. 1–3 bestanden und mindestens 151 LP")
        boolean bachelorThesisEligible,
        @Schema(description = "Bachelorarbeit nur mit Zulassung durch den Prüfungsausschuss: "
                + "Sem. 1–3 bestanden und 141–150 LP")
        boolean bachelorThesisNeedsApproval,
        List<OpenModuleDto> openModules) {
}
