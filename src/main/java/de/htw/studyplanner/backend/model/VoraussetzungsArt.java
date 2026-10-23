package de.htw.studyplanner.backend.model;

/**
 * Art einer Voraussetzung. Die Reihenfolge ist Absicht: Notwendige Voraussetzungen
 * stehen vor den empfohlenen, wenn nach {@link Enum#ordinal()} sortiert wird.
 */
public enum VoraussetzungsArt {
    /** Notwendige Voraussetzung (NV laut Studienplan). */
    NOTWENDIG,
    /** Empfohlene Voraussetzung (EV laut Studienplan). */
    EMPFOHLEN
}
