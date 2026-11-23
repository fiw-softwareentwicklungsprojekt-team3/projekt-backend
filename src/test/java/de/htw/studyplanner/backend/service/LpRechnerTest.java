package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit-Tests für die Prozentrechnung in {@link LpRechner}. */
class LpRechnerTest {

    @Test
    void fortschrittProzent_wirdAufEineStelleGerundet() {
        assertThat(LpRechner.fortschrittProzent(0)).isEqualTo(0.0);
        assertThat(LpRechner.fortschrittProzent(20)).isEqualTo(11.1);
        assertThat(LpRechner.fortschrittProzent(65)).isEqualTo(36.1);
        assertThat(LpRechner.fortschrittProzent(1)).isEqualTo(0.6);
        assertThat(LpRechner.fortschrittProzent(180)).isEqualTo(100.0);
    }
}
