package de.htw.studyplanner.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.htw.studyplanner.backend.model.Studierende;

/**
 * Repository für den Datenzugriff auf {@link Studierende}-Datensätze.
 */
public interface StudierendeRepository extends JpaRepository<Studierende, Integer> {

    /**
     * Lädt alle Studierenden inklusive ihres Startsemesters in einer einzigen Abfrage,
     * um N+1-Abfragen bei der Kohorten-Aggregation zu vermeiden.
     *
     * @return Liste aller Studierenden mit geladenem Startsemester
     */
    @Query("SELECT s FROM Studierende s JOIN FETCH s.startsemester")
    List<Studierende> findAllWithStartsemester();

    // Das Repository bietet die Standard-JPA-Methoden wie save(), findById() und
    // delete() automatisch an. Die fachliche Bedeutung liegt hier darin, dass eine
    // Studierende als Erweiterung einer Nutzenden modelliert wird und deshalb genau
    // diese zusätzlichen Daten für die studentischen Prozesse verwaltet werden.

    /**
     * Lädt eine Studierende zusammen mit ihrem Startsemester in einer Abfrage.
     *
     * @param nutzendeId ID der Person
     * @return die Studierende mit geladenem Startsemester, leer wenn es sie nicht gibt
     */
    @Query("select s from Studierende s join fetch s.startsemester where s.nutzendeId = :nutzendeId")
    Optional<Studierende> findMitStartsemester(@Param("nutzendeId") Integer nutzendeId);
}
