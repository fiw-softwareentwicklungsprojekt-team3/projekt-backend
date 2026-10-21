package de.htw.studyplanner.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.Modul;

/**
 * Datenzugriff auf die Tabelle {@code modul}. Der Schlüssel ist der Modulcode (z. B. "B34").
 * {@code findAll(Sort)} und {@code findById} aus {@link JpaRepository} reichen für die Modulliste.
 */
public interface ModulRepository extends JpaRepository<Modul, String> {
}
