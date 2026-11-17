package de.htw.studyplanner.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.htw.studyplanner.backend.model.AngebotsGruppe;
import de.htw.studyplanner.backend.model.Wahlangebot;

/**
 * Zugriff auf die Tabelle {@code wahlangebot}.
 */
public interface WahlangebotRepository extends JpaRepository<Wahlangebot, String> {

    /**
     * Alle Angebote einer Gruppe.
     *
     * @param gruppe {@code WP} oder {@code SPRACHE}
     * @return die Angebote, unsortiert
     */
    List<Wahlangebot> findByGruppe(AngebotsGruppe gruppe);

    /**
     * Alle Angebote einer Gruppe mit einer bestimmten Niveaustufe.
     *
     * @param gruppe      {@code WP} oder {@code SPRACHE}
     * @param niveaustufe {@code 1a} oder {@code 1b}
     * @return die Angebote, unsortiert
     */
    List<Wahlangebot> findByGruppeAndNiveaustufe(AngebotsGruppe gruppe, String niveaustufe);
}

