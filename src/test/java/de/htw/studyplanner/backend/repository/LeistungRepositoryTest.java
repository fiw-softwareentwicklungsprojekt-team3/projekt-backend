package de.htw.studyplanner.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;

/**
 * Prüft das Mapping von {@link Leistung} gegen die echte Tabelle aus V1,
 * inklusive der CHECK- und UNIQUE-Bedingungen.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LeistungRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LeistungRepository leistungRepository;

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
    private Modul b11;

    @BeforeEach
    void legeStudierendeAn() {
        ws2025 = semesterRepository.findByJahrAndArt(2025, SemesterArt.WS).orElseThrow();
        Nutzende nutzende = nutzendeRepository.saveAndFlush(
                new Nutzende("leistung.test@htw-berlin.de", "hash", Rolle.STUDIERENDE));
        studierende = studierendeRepository.saveAndFlush(new Studierende(nutzende, ws2025));
        b11 = modulRepository.findById("B11").orElseThrow();
    }

    private Leistung bestanden(Modul modul, String note) {
        Leistung leistung = new Leistung(studierende, modul, Status.BESTANDEN);
        leistung.setNote(new BigDecimal(note));
        leistung.setSemester(ws2025);
        leistung.setAnzahlVersuche(1);
        return leistung;
    }

    @Test
    void speichertLeistungUndLiestSieAusDerDatenbank() {
        Integer id = leistungRepository.saveAndFlush(bestanden(b11, "1.3")).getLeistungId();
        em.clear();

        Leistung gelesen = leistungRepository.findById(id).orElseThrow();

        assertThat(gelesen.getModul().getCode()).isEqualTo("B11");
        assertThat(gelesen.getStatus()).isEqualTo(Status.BESTANDEN);
        assertThat(gelesen.getNote()).isEqualByComparingTo("1.3");
        assertThat(gelesen.getAnzahlVersuche()).isEqualTo(1);
        assertThat(gelesen.getSemester().getJahr()).isEqualTo(2025);
    }

    @Test
    void semesterUndNoteDuerfenNullSein() {
        // z. B. eine belegte Leistung ohne Semesterangabe
        Integer id = leistungRepository.saveAndFlush(new Leistung(studierende, b11, Status.BELEGT)).getLeistungId();
        em.clear();

        Leistung gelesen = leistungRepository.findById(id).orElseThrow();

        assertThat(gelesen.getSemester()).isNull();
        assertThat(gelesen.getNote()).isNull();
        assertThat(gelesen.getAnzahlVersuche()).isZero();
    }

    @Test
    void findByStudierendeUndFindByStudierendeAndModul() {
        Modul b12 = modulRepository.findById("B12").orElseThrow();
        Modul b13 = modulRepository.findById("B13").orElseThrow();
        leistungRepository.save(bestanden(b11, "2.0"));
        leistungRepository.save(bestanden(b12, "3.7"));
        em.flush();
        em.clear();

        assertThat(leistungRepository.findByStudierende(studierende))
                .extracting(l -> l.getModul().getCode())
                .containsExactlyInAnyOrder("B11", "B12");
        assertThat(leistungRepository.findByStudierendeAndModul(studierende, b12))
                .hasValueSatisfying(l -> assertThat(l.getNote()).isEqualByComparingTo("3.7"));
        assertThat(leistungRepository.findByStudierendeAndModul(studierende, b13)).isEmpty();
    }

    @Test
    void wiederholungAendertDieselbeZeile() {
        Leistung erster = new Leistung(studierende, b11, Status.NICHT_BESTANDEN);
        erster.setNote(new BigDecimal("5.0"));
        erster.setAnzahlVersuche(1);
        leistungRepository.saveAndFlush(erster);
        em.clear();

        Leistung leistung = leistungRepository.findByStudierendeAndModul(studierende, b11).orElseThrow();
        leistung.setStatus(Status.BESTANDEN);
        leistung.setNote(new BigDecimal("2.7"));
        leistung.setAnzahlVersuche(leistung.getAnzahlVersuche() + 1);
        em.flush();
        em.clear();

        assertThat(leistungRepository.findByStudierende(studierende)).singleElement().satisfies(l -> {
            assertThat(l.getStatus()).isEqualTo(Status.BESTANDEN);
            assertThat(l.getNote()).isEqualByComparingTo("2.7");
            assertThat(l.getAnzahlVersuche()).isEqualTo(2);
        });
    }

    @Test
    void zweiteLeistungFuerDasselbeModulScheitertAnUnique() {
        leistungRepository.saveAndFlush(bestanden(b11, "2.0"));

        assertThatThrownBy(() -> leistungRepository.saveAndFlush(bestanden(b11, "1.0")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ungueltigeNoteScheitertAmCheck() {
        // Status BESTANDEN, damit nur chk_leistung_note_erlaubt greift
        // (bei BELEGT wäre jede Note schon durch einen anderen CHECK verboten)
        assertThatThrownBy(() -> leistungRepository.saveAndFlush(bestanden(b11, "2.5")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_leistung_note_erlaubt");
    }
}
