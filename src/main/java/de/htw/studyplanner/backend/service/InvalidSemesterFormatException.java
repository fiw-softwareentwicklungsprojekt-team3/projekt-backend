package de.htw.studyplanner.backend.service;

/**
 * Wird geworfen, wenn ein Semester-String nicht dem erwarteten Format {@code "WS JJJJ"}
 * oder {@code "SS JJJJ"} entspricht.
 */
public class InvalidSemesterFormatException extends RuntimeException {

    /**
     * Erstellt den Fehler mit einer Meldung für die API.
     *
     * @param message Beschreibung, was am Format falsch ist
     */
    public InvalidSemesterFormatException(String message) {
        super(message);
    }
}

