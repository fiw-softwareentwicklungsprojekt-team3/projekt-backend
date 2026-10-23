package de.htw.studyplanner.backend.model;

/** Semester, in dem ein Modul angeboten wird. Entspricht der Spalte {@code modul.turnus}. */
public enum Turnus {
    /** Nur im Wintersemester. */
    WS,
    /** Nur im Sommersemester. */
    SS,
    /** In beiden Semestern. */
    BEIDE
}
