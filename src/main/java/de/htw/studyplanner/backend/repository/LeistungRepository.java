package de.htw.studyplanner.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;

/** Datenzugriff auf die Tabelle {@code leistung}. */
public interface LeistungRepository extends JpaRepository<Leistung, Integer> {

    /**
     * Liefert alle Leistungen einer Person.
     *
     * @param studierende Person, deren Leistungen geladen werden
     * @return alle Leistungen, leer wenn es noch keine gibt
     */
    List<Leistung> findByStudierende(Studierende studierende);

    /**
     * Liefert die Leistung einer Person in einem Modul. Es gibt höchstens eine,
     * weil die Tabelle UNIQUE auf Person und Modul hat.
     *
     * @param studierende Person
     * @param modul       Modul
     * @return die Leistung, oder leer wenn die Person das Modul noch nicht hat
     */
    Optional<Leistung> findByStudierendeAndModul(Studierende studierende, Modul modul);

    /**
     * Lädt alle Leistungen mit dem angegebenen Status inklusive des zugehörigen Moduls
     * und der Studierenden in einer einzigen Abfrage, um N+1-Probleme bei Aggregationen zu vermeiden.
     *
     * @param status Status der gesuchten Leistungen (z. B. {@link Status#BESTANDEN})
     * @return Liste der Leistungen mit geladenem Modul und geladenen Studierenden
     */
    @Query("SELECT l FROM Leistung l JOIN FETCH l.modul JOIN FETCH l.studierende WHERE l.status = :status")
    List<Leistung> findByStatusWithModulAndStudierende(@Param("status") Status status);
}
