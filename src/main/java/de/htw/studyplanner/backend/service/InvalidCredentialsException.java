package de.htw.studyplanner.backend.service;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("E-Mail oder Passwort ist falsch.");
    }
}