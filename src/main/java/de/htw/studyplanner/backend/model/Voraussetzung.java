package de.htw.studyplanner.backend.model;

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
 * Eine Voraussetzung eines Moduls (Tabelle {@code voraussetzung}).
 *
 * <p>Es ist immer genau eines von beiden gesetzt (das erzwingt die Datenbank):
 * <ul>
 *   <li>{@link #getVorausgesetztesModul()}: ein anderes Modul wird vorausgesetzt, oder</li>
 *   <li>{@link #getMinLp()}: eine Mindestzahl an Leistungspunkten wird vorausgesetzt.</li>
 * </ul>
 * Bei einer LP-Voraussetzung gibt {@link #getToleranzLp()} an, um wie viele
 * Leistungspunkte die Grenze unterschritten werden darf.
 */
@Entity
@Table(name = "voraussetzung")
public class Voraussetzung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voraussetzung_id")
    private Integer id;

    /** Das Modul, für das die Voraussetzung gilt. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modul_code", nullable = false)
    private Modul modul;

    @Enumerated(EnumType.STRING)
    @Column(name = "art", nullable = false)
    private VoraussetzungsArt art;

    /** Das vorausgesetzte Modul; {@code null} bei einer LP-Voraussetzung. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vorausgesetzt_modul_code")
    private Modul vorausgesetztesModul;

    /** Mindestzahl an Leistungspunkten; {@code null} bei einer Modul-Voraussetzung. */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "min_lp")
    private Integer minLp;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "toleranz_lp", nullable = false)
    private int toleranzLp;

    /**
     * {@code true}, wenn die Voraussetzung auch im selben Semester erfüllt werden darf
     * (E28, betrifft nur die NOTWENDIG-Zeilen von B65). Sonst gilt: früheres Semester.
     */
    @Column(name = "gleiches_semester", nullable = false)
    private boolean gleichesSemester;

    /** Wird von JPA benötigt. */
    protected Voraussetzung() {
    }

    public Integer getId() {
        return id;
    }

    public Modul getModul() {
        return modul;
    }

    public VoraussetzungsArt getArt() {
        return art;
    }

    public Modul getVorausgesetztesModul() {
        return vorausgesetztesModul;
    }

    public Integer getMinLp() {
        return minLp;
    }

    public int getToleranzLp() {
        return toleranzLp;
    }

    public boolean isGleichesSemester() {
        return gleichesSemester;
    }
}
