package de.htw.studyplanner.backend.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Ein Modul aus der Studienplanübersicht (Tabelle {@code modul}).
 *
 * <p>Die Klasse ist nur lesbar: Die Modulliste wird per Flyway-Migration eingespielt
 * und von der Anwendung nicht verändert. Deshalb gibt es keine Setter.
 *
 * <p>Die Spalten vom Typ {@code SMALLINT} brauchen {@code @JdbcTypeCode(SqlTypes.SMALLINT)},
 * sonst meldet Hibernate bei {@code ddl-auto=validate} einen Typfehler (int statt smallint).
 */
@Entity
@Table(name = "modul")
public class Modul {

    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "bezeichnung", nullable = false)
    private String bezeichnung;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "fachsemester", nullable = false)
    private int fachsemester;

    @Enumerated(EnumType.STRING)
    @Column(name = "art", nullable = false)
    private ModulArt art;

    @Enumerated(EnumType.STRING)
    @Column(name = "bereich", nullable = false)
    private Bereich bereich;

    @Column(name = "mit_auswahl", nullable = false)
    private boolean mitAuswahl;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "lp", nullable = false)
    private int lp;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "sws_vorlesung")
    private Integer swsVorlesung;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "sws_uebung", nullable = false)
    private int swsUebung;

    /** Niveaustufe {@code 1a} oder {@code 1b}; bei Wahlpflichtmodulen {@code null}. */
    @Column(name = "niveaustufe", length = 2)
    private String niveaustufe;

    @Enumerated(EnumType.STRING)
    @Column(name = "turnus", nullable = false)
    private Turnus turnus;

    @Column(name = "zaehlt_in_x1", nullable = false)
    private boolean zaehltInX1;

    @Column(name = "benotet", nullable = false)
    private boolean benotet;

    /** Wird von JPA benötigt. */
    protected Modul() {
    }

    public String getCode() {
        return code;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public int getFachsemester() {
        return fachsemester;
    }

    public ModulArt getArt() {
        return art;
    }

    public Bereich getBereich() {
        return bereich;
    }

    public boolean isMitAuswahl() {
        return mitAuswahl;
    }

    public int getLp() {
        return lp;
    }

    public Integer getSwsVorlesung() {
        return swsVorlesung;
    }

    public int getSwsUebung() {
        return swsUebung;
    }

    public String getNiveaustufe() {
        return niveaustufe;
    }

    public Turnus getTurnus() {
        return turnus;
    }

    public boolean isZaehltInX1() {
        return zaehltInX1;
    }

    public boolean isBenotet() {
        return benotet;
    }
}
