package de.htw.studyplanner.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entität für die studienbezogenen Zusatzdaten einer Nutzenden.
 *
 * Diese Klasse bildet die Tabelle {@code studierende} ab. Die Besonderheit ist,
 * dass sie keinen eigenen Primärschlüssel besitzt, sondern denselben Schlüssel wie
 * {@link Nutzende} verwendet. Das ist ein typisches 1:1-Mapping mit gemeinsamem
 * Schlüssel: Eine Studierende ist immer genau eine Nutzende und umgekehrt.
 *
 * Warum ist das sinnvoll? Weil die allgemeine Identität und das fachliche Profil
 * sauber getrennt werden sollen: {@link Nutzende} verwaltet allgemeine
 * Authentifizierungs- und Rolleninformationen, {@link Studierende} ergänzt die
 * studienbezogenen Daten wie Startsemester und weitere Fachdaten. In der
 * Datenbank werden diese Informationen trotzdem in zwei Tabellen gespeichert,
 * aber in der Java-Welt werden sie als zusammengehörige Einheit modelliert.
 *
 * Das Mapping mit {@link MapsId} und {@link OneToOne} übernimmt die gemeinsame ID
 * automatisch. Dadurch wird der Fremdschlüssel {@code nutzende_id} aus der
 * {@link Nutzende}-Entität übernommen und die beiden Entitäten sind dauerhaft
 * logisch miteinander verknüpft.
 *
 * Das Feld {@code startsemester} verweist auf das Studiensemester, in dem die
 * Person begonnen hat. Diese Information ist später für Studienplanung,
 * Prüfungslogik, Semesterfilter und fachliche Abfragen zentral relevant.
 */
@Entity
@Table(name = "studierende")
public class Studierende {

    /**
     * Der gemeinsame Primärschlüssel mit {@link Nutzende}. Dadurch ist jede
     * Studierende immer an genau eine Nutzende gebunden.
     */
    @Id
    @Column(name = "nutzende_id")
    private Integer nutzendeId;

    /**
     * Eine Studierende ist in der Anwendung eine Erweiterung der Basis-Entität
     * {@link Nutzende}. Die Lazy-Relation verhindert, dass bei jedem Zugriff auf
     * das Profil automatisch das gesamte Nutzenden-Objekt geladen wird.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "nutzende_id")
    private Nutzende nutzende;

    /**
     * Startsemester der Studierenden. Viele Studierende können auf dasselbe
     * Semester verweisen, aber jedes Profil hat genau ein Startsemester.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "startsemester_id", nullable = false)
    private Semester startsemester;

    /**
     * JPA benötigt einen parameterlosen Konstruktor, damit Hibernate Instanzen beim
     * Laden aus der Datenbank erzeugen kann.
     */
    protected Studierende() {
        // JPA benötigt einen no-args Konstruktor.
    }

    /**
     * Praktischer Konstruktor für die Erstellung eines Studentenprofils.
     *
     * Ein neues Profil wird immer mit der zugehörigen {@link Nutzende}-Entität und
     * dem Startsemester verbunden. Die gemeinsame ID wird durch JPA aus der
     * zugehörigen Nutzenden-Entität übernommen.
     */
    public Studierende(Nutzende nutzende, Semester startsemester) {
        this.nutzende = nutzende;
        this.startsemester = startsemester;
    }

    /**
     * Liefert die gemeinsame ID mit {@link Nutzende}.
     */
    public Integer getNutzendeId() {
        return nutzendeId;
    }

    /**
     * Setzt die gemeinsame ID.
     */
    public void setNutzendeId(Integer nutzendeId) {
        this.nutzendeId = nutzendeId;
    }

    /**
     * Liefert die zugehörige Nutzende-Entität.
     *
     * Diese Beziehung ist fachlich wichtig, weil ein Student immer auch eine
     * Person im System ist, die zugleich allgemeine Authentifizierungsdaten hat.
     */
    public Nutzende getNutzende() {
        return nutzende;
    }

    /**
     * Setzt die zugehörige Nutzende.
     */
    public void setNutzende(Nutzende nutzende) {
        this.nutzende = nutzende;
    }

    /**
     * Liefert das Startsemester der Studierenden.
     */
    public Semester getStartsemester() {
        return startsemester;
    }

    /**
     * Setzt das Startsemester.
     */
    public void setStartsemester(Semester startsemester) {
        this.startsemester = startsemester;
    }

    @Override
    public String toString() {
        return "Studierende{" +
                "nutzendeId=" + nutzendeId +
                ", startsemester=" + startsemester +
                '}';
    }
}
