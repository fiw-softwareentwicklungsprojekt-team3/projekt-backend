package de.htw.studyplanner.backend.dto;

public record PlanVerstoss(
        PruefSchwere schwere,
        String geprueftesModul,
        String fehlendeVoraussetzung,
        String meldung) {
}
