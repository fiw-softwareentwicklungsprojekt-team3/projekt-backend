package de.htw.studyplanner.backend.model;

/**
 * Rollen der Nutzenden, wie sie in der V1-Migration definiert sind.
 *
 * Diese Enum-Werte bilden die fachliche Bedeutung der Nutzer:innen im System ab.
 * Eine Rolle entscheidet später, welche Rechte, Views und Services für eine
 * Person zutreffen. Dadurch kann die Anwendung ohne harte Codierung auf Rollen
 * reagieren, sondern nur auf die Enum-Werte prüfen.
 */
public enum Rolle {
    /**
     * Eine Studierende oder ein Student im System.
     */
    STUDIERENDE,

    /**
     * Person mit Lehr- oder Prüfungsfunktion.
     */
    PROFESSOR_IN
}
