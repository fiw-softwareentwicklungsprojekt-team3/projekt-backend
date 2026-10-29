package de.htw.studyplanner.backend.service;

/**
 * Wird geworfen, wenn bei der Registrierung die E-Mail schon vergeben ist.
 * Der {@code ApiExceptionHandler} macht daraus eine Antwort mit Status 409.
 */
public class EmailAlreadyExistsException extends RuntimeException {
}
