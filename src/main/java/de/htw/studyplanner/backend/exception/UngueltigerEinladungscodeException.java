package de.htw.studyplanner.backend.exception;

/**
 * Der Einladungscode ist unbekannt, abgelaufen oder schon eingelöst.
 *
 * <p>Alle drei Fälle haben bewusst dieselbe Meldung. Sonst könnte man an der
 * Antwort ablesen, ob es einen Code gibt, und so gültige Codes erraten.
 * Der {@code ApiExceptionHandler} macht daraus eine Antwort mit Status 400.
 */
public class UngueltigerEinladungscodeException extends RuntimeException {

    /** Die eine Meldung für alle Fälle. */
    public static final String MELDUNG = "Der Einladungscode ist ungültig.";

    /** Erstellt die Exception mit der allgemeinen Meldung. */
    public UngueltigerEinladungscodeException() {
        super(MELDUNG);
    }
}
