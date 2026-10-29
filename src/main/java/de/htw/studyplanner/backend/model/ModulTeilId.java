package de.htw.studyplanner.backend.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Zusammengesetzter Schlüssel von {@link ModulTeil}: Modulcode und Teilcode,
 * z. B. {@code B34} und {@code B34.1}.
 *
 * <p>JPA verlangt für zusammengesetzte Schlüssel {@link Serializable} sowie
 * {@code equals} und {@code hashCode}, damit zwei Schlüssel mit denselben Werten
 * als gleich erkannt werden.
 */
@Embeddable
public class ModulTeilId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Wird über {@code @MapsId("modulCode")} in {@link ModulTeil} aus dem Modul befüllt. */
    @Column(name = "modul_code", length = 10)
    private String modulCode;

    @Column(name = "teil_code", length = 10)
    private String teilCode;

    /** Wird von JPA benötigt. */
    protected ModulTeilId() {
    }

    public ModulTeilId(String modulCode, String teilCode) {
        this.modulCode = modulCode;
        this.teilCode = teilCode;
    }

    public String getModulCode() {
        return modulCode;
    }

    public String getTeilCode() {
        return teilCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModulTeilId other)) {
            return false;
        }
        return Objects.equals(modulCode, other.modulCode) && Objects.equals(teilCode, other.teilCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modulCode, teilCode);
    }
}
