package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.repository.VoraussetzungRepository;

/**
 * Schneller Unit-Test für {@link StatisticsService} ohne Docker und Spring-Kontext.
 * Testet die Berechnungslogik, Rundung und Sortierung isoliert über Mocks.
 *
 * <p>Die Regeln der Bachelorarbeit (B66) sind wie in der DB nachgebaut: die 17 Module aus
 * Sem. 1–3 und 151 LP mit 10 LP Toleranz. Geprüft werden sie vom echten
 * {@link BachelorarbeitPruefer} mit echtem {@link PlanPruefer}.
 */
class StatisticsServiceUnitTest {

    private final StudierendeRepository studierendeRepository = mock(StudierendeRepository.class);
    private final LeistungRepository leistungRepository = mock(LeistungRepository.class);
    private final VoraussetzungRepository voraussetzungRepository = b66Regeln();
    private final PlanPruefer planPruefer = new PlanPruefer(
            mock(PlanEintragRepository.class), leistungRepository, voraussetzungRepository,
            new SemesterService(mock(SemesterRepository.class)));
    private final ModulRepository modulRepository = mock(ModulRepository.class);
    private final SemesterService semesterService = new SemesterService(mock(SemesterRepository.class));
    private final StatisticsService statisticsService = new StatisticsService(
            studierendeRepository, leistungRepository,
            new BachelorarbeitPruefer(voraussetzungRepository, planPruefer),
            modulRepository, semesterService);

    /** Baut die notwendigen Voraussetzungen von B66 wie in V2__stupo_seed.sql nach. */
    private static VoraussetzungRepository b66Regeln() {
        AtomicInteger id = new AtomicInteger();
        List<Voraussetzung> regeln = new ArrayList<>();
        regeln.add(lpRegel(id.incrementAndGet(), 151, 10));
        for (String code : StatisticsService.REQUIRED_MODULES_SEM_1_TO_3) {
            regeln.add(modulRegel(id.incrementAndGet(), code));
        }
        VoraussetzungRepository repository = mock(VoraussetzungRepository.class);
        when(repository.findByModul_Code("B66")).thenReturn(regeln);
        return repository;
    }

    /** Eine LP-Voraussetzung (NOTWENDIG). */
    private static Voraussetzung lpRegel(int id, int minLp, int toleranzLp) {
        Voraussetzung v = mock(Voraussetzung.class);
        when(v.getId()).thenReturn(id);
        when(v.getArt()).thenReturn(VoraussetzungsArt.NOTWENDIG);
        when(v.getMinLp()).thenReturn(minLp);
        when(v.getToleranzLp()).thenReturn(toleranzLp);
        return v;
    }

    /** Eine Modul-Voraussetzung (NOTWENDIG). */
    private static Voraussetzung modulRegel(int id, String modulCode) {
        Modul modul = mock(Modul.class);
        when(modul.getCode()).thenReturn(modulCode);
        Voraussetzung v = mock(Voraussetzung.class);
        when(v.getId()).thenReturn(id);
        when(v.getArt()).thenReturn(VoraussetzungsArt.NOTWENDIG);
        when(v.getVorausgesetztesModul()).thenReturn(modul);
        // Mockito liefert für Integer sonst 0 statt null, dann wäre es eine LP-Regel
        when(v.getMinLp()).thenReturn(null);
        return v;
    }

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
        Studierende s5 = erstelleStudierende(5, ws2024); // 0 LP (Person ohne Leistungen für Mindestgröße 5)

        when(studierendeRepository.findAllWithStartsemester()).thenReturn(List.of(s1, s2, s3, s4, s5));

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

        // s5: 0 LP (keine Leistungen)

        when(leistungRepository.findByStatusWithModulAndStudierende(Status.BESTANDEN))
                .thenReturn(alleLeistungen);

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).hasSize(1);
        CohortStatisticsDto dto = ergebnis.getFirst();

        assertThat(dto.cohort()).isEqualTo("WS 2024");
        assertThat(dto.studentCount()).isEqualTo(5);
        // LP-Summe = 462 -> 462 / 5 = 92.4
        assertThat(dto.averageLp()).isEqualTo(92.4);
        // Fortschritt = (92.4 / 180.0) * 100 = 51.333... -> 51.3%
        assertThat(dto.averageProgressPercent()).isEqualTo(51.3);
        assertThat(dto.completedSemesters1To3Count()).isEqualTo(2); // nur s1 und s2
        assertThat(dto.bachelorThesisEligibleCount()).isEqualTo(1); // nur s2 (>= 151 LP und Sem 1-3)
        assertThat(dto.bachelorThesisWithApprovalCount()).isEqualTo(1); // nur s1 (141–150 LP und Sem 1-3)
    }

    @Test
    void kohorteMitWenigerAlsFuenfStudierendenWirdUebersprungen() {
        Semester ws2024 = new Semester(2024, SemesterArt.WS);
        Semester ss2025 = new Semester(2025, SemesterArt.SS);

        // ws2024: 4 Studierende (< 5)
        List<Studierende> studierende = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            studierende.add(erstelleStudierende(i, ws2024));
        }
        // ss2025: 5 Studierende (>= 5)
        for (int i = 5; i <= 9; i++) {
            studierende.add(erstelleStudierende(i, ss2025));
        }

        when(studierendeRepository.findAllWithStartsemester()).thenReturn(studierende);
        when(leistungRepository.findByStatusWithModulAndStudierende(Status.BESTANDEN)).thenReturn(List.of());

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).hasSize(1);
        assertThat(ergebnis.getFirst().cohort()).isEqualTo("SS 2025");
        assertThat(ergebnis.getFirst().studentCount()).isEqualTo(5);
    }

    @Test
    void modulStatistikenWerdenKorrektAggregiertUndSortiert() {
        Semester ws2023 = new Semester(2023, SemesterArt.WS);
        Semester ws2024 = new Semester(2024, SemesterArt.WS);

        Studierende s1 = erstelleStudierende(1, ws2023);
        Studierende s2 = erstelleStudierende(2, ws2023);
        Studierende s3 = erstelleStudierende(3, ws2024);

        // Module vorbereiten:
        // B11: Fachsemester 1, benotet
        Modul b11 = mock(Modul.class);
        when(b11.getCode()).thenReturn("B11");
        when(b11.getBezeichnung()).thenReturn("Programmierung 1");
        when(b11.getFachsemester()).thenReturn(1);
        when(b11.isBenotet()).thenReturn(true);

        // B43: Fachsemester 4, unbenotet
        Modul b43 = mock(Modul.class);
        when(b43.getCode()).thenReturn("B43");
        when(b43.getBezeichnung()).thenReturn("Fachpraktikum");
        when(b43.getFachsemester()).thenReturn(4);
        when(b43.isBenotet()).thenReturn(false);

        // B12: Fachsemester 1, benotet (aber vor B43 einsortiert)
        Modul b12 = mock(Modul.class);
        when(b12.getCode()).thenReturn("B12");
        when(b12.getBezeichnung()).thenReturn("Mathematik 1");
        when(b12.getFachsemester()).thenReturn(1);
        when(b12.isBenotet()).thenReturn(true);

        when(modulRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "fachsemester", "code")))
                .thenReturn(List.of(b11, b12, b43));

        // Leistungen:
        // s1 für B11: BESTANDEN, Note 2.0, 2 Versuche (Wiederholung)
        Leistung l1 = new Leistung(s1, b11, Status.BESTANDEN);
        l1.setNote(new java.math.BigDecimal("2.0"));
        l1.setAnzahlVersuche(2);

        // s2 für B11: BESTANDEN, Note 3.0, 1 Versuch
        Leistung l2 = new Leistung(s2, b11, Status.BESTANDEN);
        l2.setNote(new java.math.BigDecimal("3.0"));
        l2.setAnzahlVersuche(1);

        // s3 für B11: NICHT_BESTANDEN, Note 5.0, 3 Versuche (kritisch)
        Leistung l3 = new Leistung(s3, b11, Status.NICHT_BESTANDEN);
        l3.setNote(new java.math.BigDecimal("5.0"));
        l3.setAnzahlVersuche(3);

        // s1 für B43: BESTANDEN, keine Note, 1 Versuch
        Leistung l4 = new Leistung(s1, b43, Status.BESTANDEN);
        l4.setAnzahlVersuche(1);

        // s2 für B12: BELEGT, 1 Versuch
        Leistung l5 = new Leistung(s2, b12, Status.BELEGT);
        l5.setAnzahlVersuche(1);

        // 3 weitere Studierende in WS 2023 (für MIN_COHORT_SIZE = 5 bei Kohorten-Filter)
        Studierende s4 = org.mockito.Mockito.mock(Studierende.class);
        org.mockito.Mockito.when(s4.getStartsemester()).thenReturn(s1.getStartsemester());
        Studierende s5 = org.mockito.Mockito.mock(Studierende.class);
        org.mockito.Mockito.when(s5.getStartsemester()).thenReturn(s1.getStartsemester());
        Studierende s6 = org.mockito.Mockito.mock(Studierende.class);
        org.mockito.Mockito.when(s6.getStartsemester()).thenReturn(s1.getStartsemester());
        Leistung l6 = new Leistung(s4, b12, Status.BELEGT);
        l6.setAnzahlVersuche(1);
        Leistung l7 = new Leistung(s5, b12, Status.BELEGT);
        l7.setAnzahlVersuche(1);
        Leistung l8 = new Leistung(s6, b12, Status.BELEGT);
        l8.setAnzahlVersuche(1);

        when(leistungRepository.findAllWithModulAndStudierendeAndStartsemester())
                .thenReturn(List.of(l1, l2, l3, l4, l5, l6, l7, l8));

        // Ohne Filter
        List<de.htw.studyplanner.backend.dto.ModuleStatisticsDto> ergebnis =
                statisticsService.getModuleStatistics(null);

        assertThat(ergebnis).hasSize(3);

        // B11: passed=2, enrolled=0, failed=1, repeat=2 (l1 und l3), critical=1 (l3), avgGrade=2.5
        var b11Stats = ergebnis.get(0);
        assertThat(b11Stats.code()).isEqualTo("B11");
        assertThat(b11Stats.name()).isEqualTo("Programmierung 1");
        assertThat(b11Stats.semester()).isEqualTo(1);
        assertThat(b11Stats.passedCount()).isEqualTo(2);
        assertThat(b11Stats.enrolledCount()).isEqualTo(0);
        assertThat(b11Stats.failedCount()).isEqualTo(1);
        assertThat(b11Stats.repeatCount()).isEqualTo(2);
        assertThat(b11Stats.criticalAttemptCount()).isEqualTo(1);
        assertThat(b11Stats.averageGrade()).isEqualByComparingTo("2.5");

        // B12: passed=0, enrolled=1, failed=0, repeat=0, critical=0, avgGrade=null
        var b12Stats = ergebnis.get(1);
        assertThat(b12Stats.code()).isEqualTo("B12");
        assertThat(b12Stats.passedCount()).isEqualTo(0);
        assertThat(b12Stats.enrolledCount()).isEqualTo(4);
        assertThat(b12Stats.failedCount()).isEqualTo(0);
        assertThat(b12Stats.averageGrade()).isNull();

        // B43: unbenotetes Modul -> avgGrade ist null
        var b43Stats = ergebnis.get(2);
        assertThat(b43Stats.code()).isEqualTo("B43");
        assertThat(b43Stats.passedCount()).isEqualTo(1);
        assertThat(b43Stats.averageGrade()).isNull();

        // Mit Kohorten-Filter "WS 2023" -> s3 (WS 2024) wird herausgefiltert
        List<de.htw.studyplanner.backend.dto.ModuleStatisticsDto> gefiltert =
                statisticsService.getModuleStatistics("WS 2023");

        var b11Gefiltert = gefiltert.get(0);
        assertThat(b11Gefiltert.passedCount()).isEqualTo(2);
        assertThat(b11Gefiltert.failedCount()).isEqualTo(0); // l3 ist nicht mehr dabei
        assertThat(b11Gefiltert.criticalAttemptCount()).isEqualTo(0);
        assertThat(b11Gefiltert.repeatCount()).isEqualTo(1); // nur l1
        assertThat(b11Gefiltert.averageGrade()).isEqualByComparingTo("2.5");
    }

    @Test
    void modulStatistikenUngueltigesSemesterWirftException() {
        org.junit.jupiter.api.Assertions.assertThrows(
                InvalidSemesterFormatException.class,
                () -> statisticsService.getModuleStatistics("Sommer2023"));
    }
}
