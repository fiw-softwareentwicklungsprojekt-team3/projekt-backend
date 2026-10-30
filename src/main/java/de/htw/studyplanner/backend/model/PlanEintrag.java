package de.htw.studyplanner.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ein Modul im Studienplan einer Studierenden (Tabelle {@code plan_eintrag}):
 * welches Modul in welchem Semester geplant ist.
 *
 * <p>Jedes Modul steht höchstens einmal im Plan einer Person (UNIQUE auf
 * {@code nutzende_id} und {@code modul_code}). Der Plan gehört zu
 * {@link Studierende}, nicht zu {@link Nutzende} (E27). Alle Beziehungen sind
 * LAZY, damit beim Laden eines Plans nicht automatisch Module, Semester und
 * Personen mitgeladen werden.
 */
@Entity
@Table(name = "plan_eintrag")
public class PlanEintrag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_eintrag_id")
    private Integer planEintragId;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    /** Wird von JPA benötigt. */
    protected PlanEintrag() {
    }

    /**
     * Legt einen neuen Planeintrag an. Die ID setzt die Datenbank beim Speichern.
     *
     * @param studierende Person, zu deren Plan der Eintrag gehört
     * @param modul       geplantes Modul
     * @param semester    Semester, in dem das Modul geplant ist
     */
    public PlanEintrag(Studierende studierende, Modul modul, Semester semester) {
        this.studierende = studierende;
        this.modul = modul;
        this.semester = semester;
    }

    public Integer getPlanEintragId() {
        return planEintragId;
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

    /** Verschiebt das Modul in ein anderes Semester. */
    public void setSemester(Semester semester) {
        this.semester = semester;
    }
}
