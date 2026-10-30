package de.htw.studyplanner.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.Wahlangebot;

/** Datenzugriff auf die Tabelle {@code wahlangebot}. Der Schlüssel ist der Angebotscode (z. B. "WP1"). */
public interface WahlangebotRepository extends JpaRepository<Wahlangebot, String> {
}
