package de.htw.studyplanner.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import de.htw.studyplanner.backend.model.Modul;
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

    /**
     * Liefert den ganzen Plan einer Person mit Modul, Semester und Wahlangebot in
     * einer einzigen Abfrage (JOIN FETCH), damit es keine N+1-Abfragen gibt (US-38).
     *
     * @param nutzendeId ID der Person
     * @return alle Planeinträge der Person, leer wenn sie noch nichts geplant hat
     */
    @Query("""
            select e from PlanEintrag e
            join fetch e.modul
            join fetch e.semester
            left join fetch e.wahlangebot
            where e.studierende.nutzendeId = :nutzendeId
            """)
    List<PlanEintrag> findPlanMitDetails(@Param("nutzendeId") Integer nutzendeId);

    /**
     * Prüft, ob eine Person ein Modul bereits im Plan hat (UNIQUE auf Person und Modul).
     *
     * @param studierende Person
     * @param modul       Modul
     * @return {@code true}, wenn das Modul bereits im Plan steht
     */
    boolean existsByStudierendeAndModul(Studierende studierende, Modul modul);
}
