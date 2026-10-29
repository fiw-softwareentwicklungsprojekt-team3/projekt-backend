package de.htw.studyplanner.backend.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ein Wahlangebot für Module mit Auswahl (Tabelle {@code wahlangebot}), z. B. ein
 * Wahlpflichtfach für B41/B54 oder ein Sprachkurs für B16/B26.
 *
 * <p>Die Klasse ist nur lesbar wie {@link Modul}: Die Angebote kommen aus der
 * Migration V2 und werden von der Anwendung nicht verändert.
 */
@Entity
@Table(name = "wahlangebot")
public class Wahlangebot {

    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "bezeichnung", nullable = false, length = 150)
    private String bezeichnung;

    @Enumerated(EnumType.STRING)
    @Column(name = "gruppe", nullable = false, length = 10)
    private AngebotsGruppe gruppe;

    /** Niveaustufe {@code 1a} oder {@code 1b}; bei Sprachen trennt sie B16 ({@code 1a}) und B26 ({@code 1b}). */
    @Column(name = "niveaustufe", length = 2)
    private String niveaustufe;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "sws")
    private Integer sws;

    /** Empfohlenes Modul vor diesem Angebot; {@code null}, wenn es keine Empfehlung gibt. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empfohlen_modul_code")
    private Modul empfohlenesModul;

    /** Wird von JPA benötigt. */
    protected Wahlangebot() {
    }

    public String getCode() {
        return code;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public AngebotsGruppe getGruppe() {
        return gruppe;
    }

    public String getNiveaustufe() {
        return niveaustufe;
    }

    public Integer getSws() {
        return sws;
    }

    public Modul getEmpfohlenesModul() {
        return empfohlenesModul;
    }
}
