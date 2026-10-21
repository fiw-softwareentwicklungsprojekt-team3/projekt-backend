package de.htw.studyplanner.backend.repository;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NutzendeRepositoryTest {

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private StudierendeRepository studierendeRepository;

    @Test
    void findetNutzendePerEmailUndSpeichertMitStudentenDaten() {
        Semester semester = new Semester(2026, SemesterArt.WS);
        Semester gespeichertesSemester = semesterRepository.saveAndFlush(semester);

        Nutzende nutzende = new Nutzende("max.mustermann@htw-berlin.de", "geheimesPasswort", Rolle.STUDIERENDE);
        Nutzende gespeicherteNutzende = nutzendeRepository.saveAndFlush(nutzende);

        Studierende studierende = new Studierende(gespeicherteNutzende, gespeichertesSemester);
        studierendeRepository.saveAndFlush(studierende);

        Nutzende gefunden = nutzendeRepository.findByEmail("max.mustermann@htw-berlin.de").orElseThrow();

        assertThat(gefunden.getEmail()).isEqualTo("max.mustermann@htw-berlin.de");
        assertThat(gefunden.getRolle()).isEqualTo(Rolle.STUDIERENDE);
        assertThat(gefunden.getPasswortHash()).isEqualTo("geheimesPasswort");
    }

    @Test
    void doppelteEmailWirdVonDerDbAbgelehnt() {
        nutzendeRepository.saveAndFlush(new Nutzende("duplikat@htw-berlin.de", "hash1", Rolle.STUDIERENDE));

        assertThatThrownBy(() -> {
            nutzendeRepository.saveAndFlush(new Nutzende("duplikat@htw-berlin.de", "hash2", Rolle.PROFESSOR_IN));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void semesterVergleichtChronologischNachJahrUndArt() {
        Semester ss2025 = new Semester(2025, SemesterArt.SS);
        Semester ws2025 = new Semester(2025, SemesterArt.WS);
        Semester ws2026 = new Semester(2026, SemesterArt.WS);

        List<Semester> semesters = List.of(ss2025, ws2025, ws2026);

        assertThat(semesters).isSortedAccordingTo(Comparator.naturalOrder());
        assertThat(ss2025.compareTo(ws2025)).isNegative();
        assertThat(ws2025.compareTo(ws2026)).isNegative();
    }

    @Test
    void findByJahrAndArtFindetDasGewuenschteSemester() {
        semesterRepository.saveAndFlush(new Semester(2027, SemesterArt.SS));

        Semester gefunden = semesterRepository.findByJahrAndArt(2027, SemesterArt.SS).orElseThrow();

        assertThat(gefunden.getJahr()).isEqualTo(2027);
        assertThat(gefunden.getArt()).isEqualTo(SemesterArt.SS);
    }
}
