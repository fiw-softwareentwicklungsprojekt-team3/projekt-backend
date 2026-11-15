package de.htw.studyplanner.backend.exception;

/**
 * Wird geworfen, wenn eine angeforderte Ressource nicht gefunden wird.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}