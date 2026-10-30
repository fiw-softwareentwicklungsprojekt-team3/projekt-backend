package de.htw.studyplanner.backend.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Ein Teil eines Moduls (Tabelle {@code modul_teil}), z. B. B34.1
 * "Softwareentwicklungsprojekt" als Teil von B34.
 *
 * <p>Der Schlüssel besteht aus Modulcode und Teilcode ({@link ModulTeilId}).
 * {@code @MapsId("modulCode")} sorgt dafür, dass der Modulcode im Schlüssel und
 * das Feld {@link #getModul()} dieselbe Spalte {@code modul_code} benutzen.
 * Die Klasse ist nur lesbar, die Daten kommen aus der Migration V2.
 */
@Entity
@Table(name = "modul_teil")
public class ModulTeil {

    @EmbeddedId
    private ModulTeilId id;

    @MapsId("modulCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modul_code")
    private Modul modul;

    @Column(name = "bezeichnung", nullable = false, length = 150)
    private String bezeichnung;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "sws")
    private Integer sws;

    /** Wird von JPA benötigt. */
    protected ModulTeil() {
    }

    public ModulTeilId getId() {
        return id;
    }

    public Modul getModul() {
        return modul;
    }

    /** Teilcode, z. B. {@code B34.1}. */
    public String getTeilCode() {
        return id.getTeilCode();
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public Integer getSws() {
        return sws;
    }
}
