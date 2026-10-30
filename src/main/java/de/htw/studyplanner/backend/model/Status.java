package de.htw.studyplanner.backend.model;

/** Status einer Leistung. Entspricht der Spalte {@code leistung.status}. */
public enum Status {
    /** Modul ist belegt, es gibt noch keine Note. */
    BELEGT,
    /** Modul ist bestanden (Note höchstens 4,0 oder unbenotet). */
    BESTANDEN,
    /** Modul ist nicht bestanden (Note 5,0). */
    NICHT_BESTANDEN
}
