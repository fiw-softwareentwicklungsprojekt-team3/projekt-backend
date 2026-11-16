package de.htw.studyplanner.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.htw.studyplanner.backend.dto.PruefSchwere;

class PlanPrueferAusgabeTest {

    @Test
    void gibtAlleSchweregradeAus() {
        List<PruefSchwere> schweregrade = List.of(
                PruefSchwere.FEHLER,
                PruefSchwere.WARNUNG,
                PruefSchwere.HINWEIS);

        schweregrade.forEach(System.out::println);

        assertEquals("FEHLER", schweregrade.get(0).name());
        assertEquals("WARNUNG", schweregrade.get(1).name());
        assertEquals("HINWEIS", schweregrade.get(2).name());
    }
}
