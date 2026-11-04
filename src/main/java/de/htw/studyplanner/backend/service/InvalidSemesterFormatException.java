package de.htw.studyplanner.backend.service;

/**
 * Wird geworfen, wenn ein Semester-String nicht dem erwarteten Format {@code "WS JJJJ"}
 * oder {@code "SS JJJJ"} entspricht oder das Jahr außerhalb von 2000–2100 liegt.
 * Merkt sich, zu welchem Request-Feld der Fehler gehört, damit {@code errors[].field}
 * zum Feldnamen im Request passt (z. B. {@code startsemester} bei der Registrierung).
 */
public class InvalidSemesterFormatException extends RuntimeException {

    /** Feldname, wenn der Aufrufer keinen eigenen angibt. */
    public static final String STANDARD_FELD = "semester";

    private final String feld;

    /**
     * Erstellt den Fehler für das Standardfeld {@code semester}.
     *
     * @param message Beschreibung, was am Format falsch ist
     */
    public InvalidSemesterFormatException(String message) {
        this(STANDARD_FELD, message);
    }

    /**
     * Erstellt den Fehler für ein bestimmtes Request-Feld.
     *
     * @param feld    Name des Feldes im Request, z. B. {@code startsemester}
     * @param message Beschreibung, was am Format falsch ist
     */
    public InvalidSemesterFormatException(String feld, String message) {
        super(message);
        this.feld = feld;
    }

    /**
     * @return Name des Request-Feldes, zu dem der Fehler gehört
     */
    public String getFeld() {
        return feld;
    }
}
