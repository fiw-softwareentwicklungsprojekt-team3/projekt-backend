package de.htw.studyplanner.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.Voraussetzung;

/** Datenzugriff auf die Tabelle {@code voraussetzung}. */
public interface VoraussetzungRepository extends JpaRepository<Voraussetzung, Integer> {

    /**
     * Liefert alle Voraussetzungen eines Moduls (notwendige und empfohlene).
     *
     * @param modulCode Code des Moduls, für das die Voraussetzungen gelten, z. B. "B34"
     * @return die Voraussetzungen, leer wenn das Modul keine hat
     */
    List<Voraussetzung> findByModul_Code(String modulCode);
}
