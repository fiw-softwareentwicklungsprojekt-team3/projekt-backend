package de.htw.studyplanner.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;

/**
 * Repository für den Datenzugriff auf {@link Semester}-Datensätze.
 */
public interface SemesterRepository extends JpaRepository<Semester, Integer> {

    /**
     * Sucht ein Semester anhand des Schlüssels Jahr + Art.
     *
     * Dieses Muster ist sinnvoll, weil ein Semester im Studium nicht nur über eine
     * technische ID identifiziert wird, sondern über die reale Bedeutung: zum
     * Beispiel „2026 WS" oder „2027 SS". Dadurch kann die Anwendung fachliche
     * Semesterlogik sauber und lesbar ausdrücken.
     */
    Optional<Semester> findByJahrAndArt(int jahr, SemesterArt art);
}
