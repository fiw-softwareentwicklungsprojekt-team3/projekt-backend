package de.htw.studyplanner.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.AngebotsGruppe;
import de.htw.studyplanner.backend.model.ModulTeil;
import de.htw.studyplanner.backend.model.ModulTeilId;
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import de.htw.studyplanner.backend.model.Wahlangebot;

/**
 * Prüft, dass die neuen Lese-Entitäten die Seed-Daten aus V2 und V3 richtig laden:
 * {@link Wahlangebot}, {@link ModulTeil} und das Feld {@code gleichesSemester} in
 * {@link Voraussetzung}.
 *
 * <p>Die Tests prüfen bewusst feste Werte aus dem Seed (z. B. WP1 = "Machine Learning"
 * mit Empfehlung B31). Schlägt dieser Test fehl, wurde vermutlich das Seed in V2 oder
 * V3 geändert. Dann die erwarteten Werte hier an die neue Migration anpassen.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StammdatenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private WahlangebotRepository wahlangebotRepository;

    @Autowired
    private VoraussetzungRepository voraussetzungRepository;

    @Test
    void wahlangebotWp1HatGruppeUndEmpfohlenesModul() {
        Wahlangebot wp1 = wahlangebotRepository.findById("WP1").orElseThrow();

        assertThat(wp1.getBezeichnung()).isEqualTo("Machine Learning");
        assertThat(wp1.getGruppe()).isEqualTo(AngebotsGruppe.WP);
        assertThat(wp1.getSws()).isEqualTo(4);
        assertThat(wp1.getEmpfohlenesModul().getCode()).isEqualTo("B31");
    }

    @Test
    void sprachangebotHatKeinEmpfohlenesModul() {
        Wahlangebot englisch = wahlangebotRepository.findById("EN_B2.1").orElseThrow();

        assertThat(englisch.getGruppe()).isEqualTo(AngebotsGruppe.SPRACHE);
        assertThat(englisch.getNiveaustufe()).isEqualTo("1a");
        assertThat(englisch.getEmpfohlenesModul()).isNull();
    }

    @Test
    void modulTeilWirdUeberZusammengesetztenSchluesselGefunden() {
        ModulTeil teil = em.find(ModulTeil.class, new ModulTeilId("B34", "B34.2"));

        assertThat(teil).isNotNull();
        assertThat(teil.getModul().getCode()).isEqualTo("B34");
        assertThat(teil.getTeilCode()).isEqualTo("B34.2");
        assertThat(teil.getBezeichnung()).isEqualTo("Arbeitswelt IT");
        assertThat(teil.getSws()).isEqualTo(2);
    }

    @Test
    void nurNotwendigeVoraussetzungenVonB65GeltenImSelbenSemester() {
        assertThat(voraussetzungRepository.findByModul_Code("B65"))
                .filteredOn(v -> v.getArt() == VoraussetzungsArt.NOTWENDIG)
                .isNotEmpty()
                .allMatch(Voraussetzung::isGleichesSemester);

        assertThat(voraussetzungRepository.findAll())
                .filteredOn(v -> !v.getModul().getCode().equals("B65"))
                .noneMatch(Voraussetzung::isGleichesSemester);
    }
}
