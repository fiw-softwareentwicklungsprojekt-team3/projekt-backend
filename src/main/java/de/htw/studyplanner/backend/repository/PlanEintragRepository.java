package de.htw.studyplanner.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Studierende;

/** Datenzugriff auf die Tabelle {@code plan_eintrag}. */
public interface PlanEintragRepository extends JpaRepository<PlanEintrag, Integer> {

    /**
     * Liefert den ganzen Plan einer Person.
     *
     * @param studierende Person, deren Plan geladen wird
     * @return alle Planeinträge der Person, leer wenn sie noch nichts geplant hat
     */
    List<PlanEintrag> findByStudierende(Studierende studierende);
}
