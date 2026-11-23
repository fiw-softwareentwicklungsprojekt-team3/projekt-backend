package de.htw.studyplanner.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Status;

/**
 * Gemeinsame LP-Berechnung für Planprüfung, Fortschritt (US-58) und Statistik (US-53a).
 *
 * <p>Erreichte LP sind immer die Summe der LP aller Module mit {@link Status#BESTANDEN}.
 * Belegte oder nicht bestandene Module zählen nicht. Jedes Modul zählt höchstens einmal,
 * weil es pro Person und Modul nur eine Leistung gibt (UNIQUE).
 */
public final class LpRechner {

    /** Leistungspunkte des ganzen Bachelorstudiums (StuPO, Anlage 2). */
    public static final int GESAMT_LP = 180;

    /** Nur statische Methoden, keine Instanzen. */
    private LpRechner() {
    }

    /**
     * Ist die Leistung bestanden?
     *
     * @param leistung die Leistung
     * @return {@code true} bei {@link Status#BESTANDEN}
     */
    public static boolean istBestanden(Leistung leistung) {
        return leistung.getStatus() == Status.BESTANDEN;
    }

    /**
     * Summiert die LP aller bestandenen Leistungen.
     *
     * @param leistungen Leistungen einer Person
     * @return erreichte LP, 0 wenn keine bestanden ist
     */
    public static int bestandeneLp(Collection<Leistung> leistungen) {
        return leistungen.stream()
                .filter(LpRechner::istBestanden)
                .mapToInt(leistung -> leistung.getModul().getLp())
                .sum();
    }

    /**
     * Rechnet LP in Prozent von {@link #GESAMT_LP} um, kaufmännisch auf eine Nachkommastelle gerundet.
     *
     * @param lp erreichte LP
     * @return Fortschritt in Prozent, z. B. 36.1 bei 65 LP
     */
    public static double fortschrittProzent(int lp) {
        return BigDecimal.valueOf(lp * 100L)
                .divide(BigDecimal.valueOf(GESAMT_LP), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
