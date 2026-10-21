package de.htw.studyplanner.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.Nutzende;

/**
 * Repository für den Datenzugriff auf {@link Nutzende}-Datensätze.
 *
 * Spring Data JPA erzeugt hier automatisch alle Standardmethoden für CRUD und
 * Persistenz, z. B. {@code save}, {@code findById}, {@code delete},
 * {@code findAll}. Das Repository ist die Schicht, über die die Anwendung mit
 * der Tabelle {@code nutzende} kommuniziert.
 */
public interface NutzendeRepository extends JpaRepository<Nutzende, Integer> {

    /**
     * Sucht eine Nutzende anhand ihrer E-Mail-Adresse.
     *
     * Diese Methode ist für den Login- und Registrierungsfluss zentral, weil die
     * E-Mail die eindeutige Identität der Person darstellt.
     * Spring Data JPA bildet diese Signatur automatisch auf ein SQL-Query ab.
     */
    Optional<Nutzende> findByEmail(String email);
}
