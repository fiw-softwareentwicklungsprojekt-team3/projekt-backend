package de.htw.studyplanner.backend.exception;

/**
 * Wird geworfen, wenn das Token technisch gültig ist, die Anfrage aber trotzdem
 * nicht beantwortet werden darf – zum Beispiel, weil die Person aus dem Token in
 * der Datenbank nicht mehr existiert.
 *
 * <p>Der {@link de.htw.studyplanner.backend.controller.ApiExceptionHandler} macht
 * daraus eine 401-Antwort im einheitlichen Fehlerformat. Ohne diese Exception
 * würde so ein Fall als 500 enden.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * @param message Meldung für die Antwort, in der Sprache der Nutzenden
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
