package de.htw.studyplanner.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.Studierende;

/**
 * Repository für den Datenzugriff auf {@link Studierende}-Datensätze.
 */
public interface StudierendeRepository extends JpaRepository<Studierende, Integer> {
    // Das Repository bietet die Standard-JPA-Methoden wie save(), findById() und
    // delete() automatisch an. Die fachliche Bedeutung liegt hier darin, dass eine
    // Studierende als Erweiterung einer Nutzenden modelliert wird und deshalb genau
    // diese zusätzlichen Daten für die studentischen Prozesse verwaltet werden.
}
