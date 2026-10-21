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
 * Basisentität für alle Nutzenden im System.
 *
 * Diese Klasse bildet die Tabelle {@code nutzende} ab und ist die gemeinsame
 * Identität für alle Benutzer:innen des Backends. Sie enthält genau die Daten,
 * die für jede Person gleich sind: E-Mail, Passwort-Hash und Rolle.
 *
 * Warum gibt es diese Entität, obwohl die Datenbank schon Tabellen hat? Weil die
 * Datenbank nur die Relation speichert, aber die Java-Anwendung mit Objekten
 * arbeitet. JPA übersetzt zwischen beiden Ebenen: Die Datenbank speichert Zeilen,
 * Java hält Objekte im Speicher. Diese Klasse ist genau diese Java-Repräsentation
 * der Datenbanktabelle.
 *
 * Die Rolle ist hier als Enum abgebildet, damit die Typen Studierende und Professorinnen einheitlich verwaltet
 * werden können, ohne für jeden Typ eine separate Haupttabelle anzulegen.
 *
 * Mit {@link JpaRepository} kann Spring diese Entität direkt speichern, laden,
 * aktualisieren und löschen. Die E-Mail ist zusätzlich als unique Constraint
 * abgesichert, damit keine zwei Nutzenden dieselbe E-Mailadresse im System haben.
 */
@Entity
@Table(name = "nutzende", uniqueConstraints = {
    @UniqueConstraint(name = "uq_nutzende_email", columnNames = "email")
})
public class Nutzende {

    /**
     * Primärschlüssel der Tabelle {@code nutzende}. Der Wert wird von der
     * Datenbank automatisch erzeugt, daher wird die Strategie IDENTITY verwendet.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nutzende_id")
    private Integer nutzendeId;

    /**
     * Eindeutige Login-/Kontaktkennung der Nutzenden. Der Unique-Constraint
     * verhindert doppelte E-Mail-Adressen im System.
     */
    @Column(name = "email", nullable = false, length = 200)
    private String email;

    /**
     * Passwort-Hash statt Klartextpasswort. Dadurch bleibt das Passwort nicht als
     * Rohwert in der Datenbank stehen und die Logik kann später sicher erweitert
     * werden (z. B. Hashing-Algorithmus oder Passwort-Policy).
     */
    @Column(name = "passwort_hash", nullable = false, length = 200)
    private String passwortHash;

    /**
     * Rolle der Nutzenden. Als String gespeichert, damit die Werte lesbar und
     * einfach in SQL-Queries oder Logs nachvollziehbar sind.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rolle", nullable = false, length = 15)
    private Rolle rolle;

    protected Nutzende() {
        // JPA benötigt einen no-args Konstruktor.
    }

    /**
     * Praktischer Konstruktor für die Erstellung einer neuen Nutzenden.
     *
     * In der Anwendung werden neue Benutzer:innen mit E-Mail, Passwort-Hash und
     * Rolle erzeugt. Die ID wird erst beim Speichern von der Datenbank gesetzt.
     */
    public Nutzende(String email, String passwortHash, Rolle rolle) {
        this.email = email;
        this.passwortHash = passwortHash;
        this.rolle = rolle;
    }

    /**
     * Liefert die technische Datenbank-ID der Nutzenden.
     */
    public Integer getNutzendeId() {
        return nutzendeId;
    }

    /**
     * Setzt die ID, z. B. nach dem Laden eines Datensatzes aus der Datenbank.
     */
    public void setNutzendeId(Integer nutzendeId) {
        this.nutzendeId = nutzendeId;
    }

    /**
     * Liefert die E-Mail-Adresse der Nutzenden.
     *
     * Diese Information ist für Login, Identifikation und Eindeutigkeit zentral.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Setzt die E-Mail-Adresse.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Liefert den Passwort-Hash.
     *
     * Der Anwendungscode sollte niemals den Klartextwert speichern oder weitergeben.
     */
    public String getPasswortHash() {
        return passwortHash;
    }

    /**
     * Setzt den Passwort-Hash.
     */
    public void setPasswortHash(String passwortHash) {
        this.passwortHash = passwortHash;
    }

    /**
     * Liefert die Rolle der Nutzenden.
     */
    public Rolle getRolle() {
        return rolle;
    }

    /**
     * Setzt die Rolle.
     */
    public void setRolle(Rolle rolle) {
        this.rolle = rolle;
    }

    @Override
    public String toString() {
        return "Nutzende{" +
                "nutzendeId=" + nutzendeId +
                ", email='" + email + '\'' +
                ", rolle=" + rolle +
                '}';
    }
}
