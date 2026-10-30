package de.htw.studyplanner.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;

/**
 * Prüft das Mapping von {@link PlanEintrag} gegen die echte Tabelle aus V1.
 *
 * <p>Vor jedem Lesen wird {@code flush()} und {@code clear()} aufgerufen. Sonst
 * liefert JPA das Objekt aus seinem Cache und der Test liest gar nicht aus der Datenbank.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanEintragRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PlanEintragRepository planEintragRepository;

    @Autowired
    private WahlangebotRepository wahlangebotRepository;

    @Autowired
    private ModulRepository modulRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private StudierendeRepository studierendeRepository;

    private Studierende studierende;
    private Semester ws2025;

    @BeforeEach
    void legeStudierendeAn() {
        // Semester 2021-2026 sind schon durch V3 in der Datenbank
        ws2025 = semesterRepository.findByJahrAndArt(2025, SemesterArt.WS).orElseThrow();
        Nutzende nutzende = nutzendeRepository.saveAndFlush(
                new Nutzende("plan.test@htw-berlin.de", "hash", Rolle.STUDIERENDE));
        studierende = studierendeRepository.saveAndFlush(new Studierende(nutzende, ws2025));
    }

    @Test
    void speichertPlanEintragUndLiestIhnAusDerDatenbank() {
        Modul b41 = modulRepository.findById("B41").orElseThrow();
        PlanEintrag eintrag = new PlanEintrag(studierende, b41, ws2025);
        eintrag.setWahlangebot(wahlangebotRepository.findById("WP7").orElseThrow());
        Integer id = planEintragRepository.saveAndFlush(eintrag).getPlanEintragId();
        em.clear();

        PlanEintrag gelesen = planEintragRepository.findById(id).orElseThrow();

        assertThat(gelesen.getModul().getCode()).isEqualTo("B41");
        assertThat(gelesen.getWahlangebot().getCode()).isEqualTo("WP7");
        assertThat(gelesen.getSemester().getJahr()).isEqualTo(2025);
        assertThat(gelesen.getSemester().getArt()).isEqualTo(SemesterArt.WS);
        assertThat(gelesen.getStudierende().getNutzendeId()).isEqualTo(studierende.getNutzendeId());
        assertThat(gelesen.getKursCode()).isNull();
    }

    @Test
    void findByStudierendeLiefertNurDenPlanDieserPerson() {
        Modul b11 = modulRepository.findById("B11").orElseThrow();
        Modul b12 = modulRepository.findById("B12").orElseThrow();
        planEintragRepository.save(new PlanEintrag(studierende, b11, ws2025));
        planEintragRepository.save(new PlanEintrag(studierende, b12, ws2025));

        Nutzende andere = nutzendeRepository.save(
                new Nutzende("andere.person@htw-berlin.de", "hash", Rolle.STUDIERENDE));
        Studierende andereStudierende = studierendeRepository.save(new Studierende(andere, ws2025));
        planEintragRepository.save(new PlanEintrag(andereStudierende, b11, ws2025));
        em.flush();
        em.clear();

        List<PlanEintrag> plan = planEintragRepository.findByStudierende(studierende);

        assertThat(plan).extracting(e -> e.getModul().getCode()).containsExactlyInAnyOrder("B11", "B12");
    }

    @Test
    void zweiterEintragFuerDasselbeModulScheitertAnUnique() {
        Modul b11 = modulRepository.findById("B11").orElseThrow();
        Semester ss2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.SS).orElseThrow();
        planEintragRepository.saveAndFlush(new PlanEintrag(studierende, b11, ws2025));

        // anderes Semester, aber dasselbe Modul: verboten durch uq_plan_eintrag_nutzende_modul
        assertThatThrownBy(() -> planEintragRepository.saveAndFlush(new PlanEintrag(studierende, b11, ss2026)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
