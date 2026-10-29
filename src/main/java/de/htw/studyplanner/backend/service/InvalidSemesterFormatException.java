package de.htw.studyplanner.backend.service;

/**
 * Wird geworfen, wenn ein Semester-String nicht dem erwarteten Format {@code "WS JJJJ"}
 * oder {@code "SS JJJJ"} entspricht.
 */
public class InvalidSemesterFormatException extends RuntimeException {

    public InvalidSemesterFormatException(String message) {
        super(message);
    }
}

