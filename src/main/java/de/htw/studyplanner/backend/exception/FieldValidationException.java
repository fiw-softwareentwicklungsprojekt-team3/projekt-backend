package de.htw.studyplanner.backend.exception;

/**
 * Wird geworfen, wenn ein einzelnes Feld im Request fachlich ungültig ist, z. B. eine
 * Note, die nicht zum Status passt.
 *
 * <p>Der {@link de.htw.studyplanner.backend.controller.ApiExceptionHandler} macht daraus
 * eine 400-Antwort im einheitlichen Fehlerformat. Das Feld steht in {@code errors[].field},
 * damit das Frontend die Meldung am richtigen Eingabefeld anzeigen kann.
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    /**
     * @param field   Name des Feldes im Request, z. B. {@code grade}
     * @param message Meldung für die Antwort, in der Sprache der Nutzenden
     */
    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * @return Name des ungültigen Feldes im Request
     */
    public String getField() {
        return field;
    }
}
