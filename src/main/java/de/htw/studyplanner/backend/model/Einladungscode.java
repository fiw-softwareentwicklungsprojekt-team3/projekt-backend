package de.htw.studyplanner.backend.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Einladungscode für die Registrierung von Professor:innen (Tabelle {@code einladungscode}, V4).
 *
 * <p>Ein Code ist gültig, solange er nicht abgelaufen und noch nicht eingelöst ist.
 * Nach dem Einlösen stehen das angelegte Konto und der Zeitpunkt im Datensatz.
 */
@Entity
@Table(name = "einladungscode")
public class Einladungscode {

    @Id
    @Column(name = "code", length = 32)
    private String code;

    @Column(name = "gueltig_bis", nullable = false, columnDefinition = "timestamptz")
    private Instant gueltigBis;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eingeloest_von")
    private Nutzende eingeloestVon;

    @Column(name = "eingeloest_am", columnDefinition = "timestamptz")
    private Instant eingeloestAm;

    protected Einladungscode() {
        // JPA benötigt einen no-args Konstruktor.
    }

    /**
     * Legt einen neuen, noch nicht eingelösten Code an.
     *
     * @param code       der Code selbst
     * @param gueltigBis Zeitpunkt, bis zu dem der Code eingelöst werden kann
     */
    public Einladungscode(String code, Instant gueltigBis) {
        this.code = code;
        this.gueltigBis = gueltigBis;
    }

    /**
     * Prüft, ob der Code zum angegebenen Zeitpunkt noch eingelöst werden kann.
     *
     * @param jetzt aktueller Zeitpunkt
     * @return true, wenn der Code nicht abgelaufen und noch nicht eingelöst ist
     */
    public boolean istEinloesbar(Instant jetzt) {
        return eingeloestVon == null && jetzt.isBefore(gueltigBis);
    }

    /**
     * Markiert den Code als eingelöst.
     *
     * @param nutzende das Konto, das mit dem Code angelegt wurde
     * @param jetzt    Zeitpunkt der Einlösung
     */
    public void einloesen(Nutzende nutzende, Instant jetzt) {
        this.eingeloestVon = nutzende;
        this.eingeloestAm = jetzt;
    }

    public String getCode() {
        return code;
    }

    public Instant getGueltigBis() {
        return gueltigBis;
    }

    public Nutzende getEingeloestVon() {
        return eingeloestVon;
    }

    public Instant getEingeloestAm() {
        return eingeloestAm;
    }

    @Override
    public String toString() {
        // den Code selbst nicht ausgeben, damit er nicht in Logs landet
        return "Einladungscode{gueltigBis=" + gueltigBis + ", eingeloestAm=" + eingeloestAm + '}';
    }
}