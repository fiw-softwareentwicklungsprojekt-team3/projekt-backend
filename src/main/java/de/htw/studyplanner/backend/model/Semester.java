package de.htw.studyplanner.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entität für ein Studiensemester.
 *
 * Ein Semester wird durch das Jahr ({@code jahr}) und die Art
 * ({@code WS} oder {@code SS}) identifiziert. Die Kombination aus beiden Werten
 * ist in der Datenbank eindeutig.
 *
 * Diese Klasse ist für alle semesterbezogenen Abfragen relevant, z. B. um das
 * Startsemester einer Studierenden zu speichern, nach einem Semester zu suchen
 * oder Semester chronologisch zu sortieren.
 *
 * Das Interface {@link Comparable} ist hier wichtig, um Listen einfach zu
 * sortieren, weil Semester im Backend
 * oft in Reihenfolge benötigt werden: Jahr aufsteigend, und bei gleichem Jahr
 * Sommersemester vor Wintersemester.
 */
@Entity
@Table(name = "semester", uniqueConstraints = {
        @UniqueConstraint(name = "uq_semester", columnNames = { "jahr", "art" })
})
public class Semester implements Comparable<Semester> {

    /**
     * Technische ID des Semesters. Sie ist für die Datenbank notwendig, wird aber
     * nicht als Schlüssel verwendet, da Jahr + Art bereits jedes Semester
     * eindeutig identifizieren.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id")
    private Integer semesterId;

    /**
     * Das Studienjahr, z. B. 2026. Es ist als SMALLINT gespeichert, weil die
     * Werte klein sind und die Datenbank so eine kompakte Darstellung erhält.
     */
    @Column(name = "jahr", nullable = false, columnDefinition = "SMALLINT")
    private short jahr;

    /**
     * Semesterart, als Enum mit STRING-Ausprägung wird der Wert lesbar gespeichert
     * und leicht in Java
     * verarbeitet.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "art", nullable = false, length = 2)
    private SemesterArt art;

    /**
     * JPA benötigt einen parameterlosen Konstruktor, damit Hibernate Instanzen
     * dieser Entität beim Laden aus der Datenbank erzeugen kann.
     *
     * Dieser Konstruktor dient allein der Framework-Initialisierung.
     */
    protected Semester() {
    }

    /**
     * Praktischer Konstruktor für die Anwendung.
     *
     * Ein neues Semester wird immer mit einem Jahr und einer Art erstellt.
     * Das Jahr wird dabei in ein short umgewandelt, weil die
     * Datenbank-Spalte als SMALLINT definiert ist und damit Platz spart.
     */
    public Semester(int jahr, SemesterArt art) {
        this.jahr = (short) jahr;
        this.art = art;
    }

    /**
     * Liefert die technische Datenbank-ID
     */
    public Integer getSemesterId() {
        return semesterId;
    }

    /**
     * Setzt die technische ID, typischerweise beim Laden aus der Datenbank.
     */
    public void setSemesterId(Integer semesterId) {
        this.semesterId = semesterId;
    }

    /**
     * Liefert das Jahr als normalen int-Wert zurück.
     *
     * Das interne Feld {@code jahr} ist als short gespeichert. Deshalb muss der
     * Wert hier wieder in den größeren int-Typ zurückkonvertiert werden.
     * {@link Short#toUnsignedInt(short)} sorgt dafür, dass das Jahr sauber als
     * positive Zahl interpretiert wird.
     */
    public int getJahr() {
        return Short.toUnsignedInt(jahr);
    }

    /**
     * Setzt das Jahr.
     *
     * Da die Basis in der Datenbank als SMALLINT gespeichert ist, wird der Wert
     * hier wieder auf short gecastet, damit das Feld konsistent bleibt.
     */
    public void setJahr(int jahr) {
        this.jahr = (short) jahr;
    }

    /**
     * Liefert die Semesterart, z. B. Winter- oder Sommersemester.
     */
    public SemesterArt getArt() {
        return art;
    }

    /**
     * Setzt die Semesterart.
     */
    public void setArt(SemesterArt art) {
        this.art = art;
    }

    /**
     * Vergleicht zwei Semester nach ihrer fachlichen Reihenfolge.
     *
     * Das Verfahren funktioniert so:
     * 1. Vergleiche erst das Jahr.
     * 2. Wenn das Jahr gleich ist, vergleiche die Art.
     *
     * Dadurch kann eine Liste von Semestern in chronologischer Reihenfolge sortiert
     * werden.
     */
    @Override
    public int compareTo(Semester other) {
        if (other == null) {
            return 1;
        }

        int yearComparison = Integer.compare(getJahr(), other.getJahr());
        if (yearComparison != 0) {
            return yearComparison;
        }

        return Integer.compare(semesterOrder(this.art), semesterOrder(other.art));
    }

    /**
     * Ordnet den Typ des Semesters einer numerischen Reihenfolge zu.
     *
     * Sommersemester hat hier die niedrigere Reihenfolge, da es im selben
     * Kalenderjahr zeitlich vor dem Wintersemester liegt.
     */
    private int semesterOrder(SemesterArt art) {
        if (art == null) {
            return Integer.MAX_VALUE;
        }
        return switch (art) {
            case SS -> 0;
            case WS -> 1;
        };
    }

    /**
     * Gibt ein lesbares String-Objekt für Debugging und Logging zurück.
     */
    @Override
    public String toString() {
        return "Semester{" +
                "semesterId=" + semesterId +
                ", jahr=" + jahr +
                ", art=" + art +
                '}';
    }
}
