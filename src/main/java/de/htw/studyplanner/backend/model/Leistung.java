package de.htw.studyplanner.backend.model;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Das Ergebnis einer Studierenden in einem Modul (Tabelle {@code leistung}).
 *
 * <p>Es gibt höchstens eine Zeile pro Person und Modul (UNIQUE). Bei einer
 * Wiederholung wird diese Zeile geändert ({@code note}, {@code status},
 * {@code anzahlVersuche}), es entsteht keine neue Zeile (RStPO §18 Abs. 1).
 *
 * <p>Die Datenbank prüft die Note per CHECK: nur Werte nach RStPO §17 Abs. 1,
 * keine Note bei {@link Status#BELEGT}, höchstens 4,0 bei
 * {@link Status#BESTANDEN}. Die Note ist {@link BigDecimal}, weil {@code double}
 * Werte wie 1,3 nicht exakt speichern kann.
 */
@Entity
@Table(name = "leistung")
public class Leistung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leistung_id")
    private Integer leistungId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nutzende_id", nullable = false)
    private Studierende studierende;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modul_code", nullable = false)
    private Modul modul;

    /** Gewähltes Angebot bei Modulen mit Auswahl (B16, B26, B41, B54); sonst {@code null}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wahlangebot_code")
    private Wahlangebot wahlangebot;

    /** Freie Veranstaltungsnummer, z. B. bei AWE-Modulen (B63, B64). */
    @Column(name = "kurs_code", length = 30)
    private String kursCode;

    /** Freier Veranstaltungstitel, gehört zu {@link #getKursCode()}. */
    @Column(name = "kurs_titel", length = 150)
    private String kursTitel;

    /** Semester der Leistung; {@code null} erlaubt, z. B. bei anerkannten Leistungen. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private Status status;

    /**
     * Note, z. B. 1.3; {@code null} bei {@link Status#BELEGT} oder unbenoteten Modulen.
     *
     * <p>Erlaubte Werte stehen im CHECK {@code chk_leistung_note_erlaubt} in V1
     * (1.0, 1.3, 1.7, 2.0, 2.3, 2.7, 3.0, 3.3, 3.7, 4.0, 5.0). Die Prüfung erfolgt
     * nur in der Datenbank: Ein falscher Wert fällt erst beim Speichern auf.
     */
    @Column(name = "note", precision = 2, scale = 1)
    private BigDecimal note;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "anzahl_versuche", nullable = false)
    private int anzahlVersuche = 0;

    /** Wird von JPA benötigt. */
    protected Leistung() {
    }

    /**
     * Legt eine neue Leistung ohne Note und mit 0 Versuchen an. Die ID setzt die
     * Datenbank beim Speichern.
     *
     * @param studierende Person, zu der die Leistung gehört
     * @param modul       Modul der Leistung
     * @param status      aktueller Status
     */
    public Leistung(Studierende studierende, Modul modul, Status status) {
        this.studierende = studierende;
        this.modul = modul;
        this.status = status;
    }

    public Integer getLeistungId() {
        return leistungId;
    }

    public Studierende getStudierende() {
        return studierende;
    }

    public Modul getModul() {
        return modul;
    }

    public Wahlangebot getWahlangebot() {
        return wahlangebot;
    }

    public void setWahlangebot(Wahlangebot wahlangebot) {
        this.wahlangebot = wahlangebot;
    }

    public String getKursCode() {
        return kursCode;
    }

    public void setKursCode(String kursCode) {
        this.kursCode = kursCode;
    }

    public String getKursTitel() {
        return kursTitel;
    }

    public void setKursTitel(String kursTitel) {
        this.kursTitel = kursTitel;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(Semester semester) {
        this.semester = semester;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BigDecimal getNote() {
        return note;
    }

    public void setNote(BigDecimal note) {
        this.note = note;
    }

    public int getAnzahlVersuche() {
        return anzahlVersuche;
    }

    public void setAnzahlVersuche(int anzahlVersuche) {
        this.anzahlVersuche = anzahlVersuche;
    }
}
