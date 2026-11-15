package de.htw.studyplanner.backend.service;

/**
 * Wird geworfen, wenn bei der Anmeldung die E-Mail oder das Passwort falsch ist.
 * Der {@code ApiExceptionHandler} macht daraus eine Antwort mit Status 401.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-Mail oder Passwort ist falsch.");
    }
}