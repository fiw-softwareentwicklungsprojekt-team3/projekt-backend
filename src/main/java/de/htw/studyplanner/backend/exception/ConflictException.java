package de.htw.studyplanner.backend.exception;

/**
 * Wird geworfen, wenn ein Konflikt bei der Verarbeitung einer Anfrage auftritt.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}