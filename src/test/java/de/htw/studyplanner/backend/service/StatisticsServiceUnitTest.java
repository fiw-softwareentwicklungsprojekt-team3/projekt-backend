package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;

/**
 * Schneller Unit-Test für {@link StatisticsService} ohne Docker und Spring-Kontext.
 * Testet die Berechnungslogik, Rundung und Sortierung isoliert über Mocks.
 */
class StatisticsServiceUnitTest {

    private final StudierendeRepository studierendeRepository = mock(StudierendeRepository.class);
    private final LeistungRepository leistungRepository = mock(LeistungRepository.class);
    private final StatisticsService statisticsService =
            new StatisticsService(studierendeRepository, leistungRepository);

    private static Modul erstelleModul(String code, int lp) {
        Modul m = mock(Modul.class);
        when(m.getCode()).thenReturn(code);
        when(m.getLp()).thenReturn(lp);
        return m;
    }

    private static Studierende erstelleStudierende(int id, Semester startsemester) {
        Nutzende n = new Nutzende("s" + id + "@student.htw-berlin.de", "pw", Rolle.STUDIERENDE);
        n.setNutzendeId(id);
        Studierende s = new Studierende(n, startsemester);
        s.setNutzendeId(id);
        return s;
    }

    private static Leistung erstelleBestandeneLeistung(Studierende s, Modul m) {
        return new Leistung(s, m, Status.BESTANDEN);
    }

    @Test
    void leereStudierendeLiefertLeereListe() {
        when(studierendeRepository.findAllWithStartsemester()).thenReturn(List.of());

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).isEmpty();
    }

    @Test
    void berechnungUndGrenzfaelleErfolgreich() {
        Semester ws2024 = new Semester(2024, SemesterArt.WS);

        Studierende s1 = erstelleStudierende(1, ws2024); // 150 LP, Sem 1-3 komplett
        Studierende s2 = erstelleStudierende(2, ws2024); // 151 LP, Sem 1-3 komplett -> Bachelor-Zulassung
        Studierende s3 = erstelleStudierende(3, ws2024); // 150 LP, B34 fehlt -> Sem 1-3 unvollständig
        Studierende s4 = erstelleStudierende(4, ws2024); // 11 LP

        when(studierendeRepository.findAllWithStartsemester()).thenReturn(List.of(s1, s2, s3, s4));

        List<Leistung> alleLeistungen = new ArrayList<>();

        // 17 Pflichtmodule (B11-B16, B21-B26, B31-B35)
        List<Modul> sem1To3 = List.of(
                erstelleModul("B11", 6), erstelleModul("B12", 5), erstelleModul("B13", 5),
                erstelleModul("B14", 5), erstelleModul("B15", 5), erstelleModul("B16", 4),
                erstelleModul("B21", 5), erstelleModul("B22", 6), erstelleModul("B23", 5),
                erstelleModul("B24", 5), erstelleModul("B25", 5), erstelleModul("B26", 4),
                erstelleModul("B31", 5), erstelleModul("B32", 5), erstelleModul("B33", 5),
                erstelleModul("B34", 10), erstelleModul("B35", 5)
        ); // Summe = 90 LP

        Modul b41 = erstelleModul("B41", 5);
        Modul b42 = erstelleModul("B42", 5);
        Modul b43 = erstelleModul("B43", 20);
        Modul b51 = erstelleModul("B51", 5);
        Modul b52 = erstelleModul("B52", 5);
        Modul b53 = erstelleModul("B53", 5);
        Modul b54 = erstelleModul("B54", 5);
        Modul b55 = erstelleModul("B55", 5);
        Modul b56 = erstelleModul("B56", 6);
        Modul b61 = erstelleModul("B61", 5);
        Modul b65 = erstelleModul("B65", 3);
        Modul b66 = erstelleModul("B66", 12);

        // s1: 90 + 20 + (8 * 5) = 150 LP
        for (Modul m : sem1To3) alleLeistungen.add(erstelleBestandeneLeistung(s1, m));
        for (Modul m : List.of(b41, b42, b43, b51, b52, b53, b54, b55, b61)) {
            alleLeistungen.add(erstelleBestandeneLeistung(s1, m));
        }

        // s2: 90 + 20 + (7 * 5) + 6 = 151 LP
        for (Modul m : sem1To3) alleLeistungen.add(erstelleBestandeneLeistung(s2, m));
        for (Modul m : List.of(b41, b42, b43, b51, b52, b53, b54, b55, b56)) {
            alleLeistungen.add(erstelleBestandeneLeistung(s2, m));
        }

        // s3: 80 LP (B34 fehlt) + 20 + (7 * 5) + 3 + 12 = 150 LP
        for (Modul m : sem1To3) {
            if (!m.getCode().equals("B34")) alleLeistungen.add(erstelleBestandeneLeistung(s3, m));
        }
        for (Modul m : List.of(b41, b42, b43, b51, b52, b53, b54, b55, b65, b66)) {
            alleLeistungen.add(erstelleBestandeneLeistung(s3, m));
        }

        // s4: 11 LP
        alleLeistungen.add(erstelleBestandeneLeistung(s4, sem1To3.get(0))); // B11 (6 LP)
        alleLeistungen.add(erstelleBestandeneLeistung(s4, sem1To3.get(1))); // B12 (5 LP)

        when(leistungRepository.findByStatusWithModulAndStudierende(Status.BESTANDEN))
                .thenReturn(alleLeistungen);

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).hasSize(1);
        CohortStatisticsDto dto = ergebnis.getFirst();

        assertThat(dto.cohort()).isEqualTo("WS 2024");
        assertThat(dto.studentCount()).isEqualTo(4);
        assertThat(dto.averageLp()).isEqualTo(115.5);
        assertThat(dto.averageProgressPercent()).isEqualTo(64.2);
        assertThat(dto.completedSemesters1To3Count()).isEqualTo(2); // nur s1 und s2
        assertThat(dto.bachelorThesisEligibleCount()).isEqualTo(1); // nur s2 (>= 151 LP und Sem 1-3)
    }

    @Test
    void sortierungChronologischNachJahrUndArt() {
        Semester ws2025 = new Semester(2025, SemesterArt.WS);
        Semester ws2024 = new Semester(2024, SemesterArt.WS);
        Semester ss2025 = new Semester(2025, SemesterArt.SS);

        Studierende s1 = erstelleStudierende(1, ws2025);
        Studierende s2 = erstelleStudierende(2, ws2024);
        Studierende s3 = erstelleStudierende(3, ss2025);

        when(studierendeRepository.findAllWithStartsemester()).thenReturn(List.of(s1, s2, s3));
        when(leistungRepository.findByStatusWithModulAndStudierende(Status.BESTANDEN)).thenReturn(List.of());

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).extracting(CohortStatisticsDto::cohort)
                .containsExactly("WS 2024", "SS 2025", "WS 2025");
    }
}
