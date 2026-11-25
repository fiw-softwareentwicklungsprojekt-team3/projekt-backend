package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;

/**
 * Integrationstests für {@link StatisticsService} (Ticket US-53a):
 * <ul>
 *   <li>Leere Datenbank liefert leere Liste</li>
 *   <li>Berechnung mit bekannter Test-Kohorte (mind. 5 Personen)</li>
 *   <li>Grenzfall 150 vs. 151 LP für Bachelorarbeits-Zulassung (StuPO § 12): 150 LP zählt als
 *       „mit Zulassung durch den Prüfungsausschuss“ (141–150 LP, RStPO § 25)</li>
 *   <li>Unvollständige Semester 1–3 (z. B. 16 von 17 Modulen)</li>
 *   <li>Ignorieren von {@link Status#NICHT_BESTANDEN} und {@link Status#BELEGT}</li>
 *   <li>Ignorieren von geplanten Modulen aus {@code plan_eintrag}</li>
 *   <li>Mindest-Kohortengröße von 5 Personen zur Anonymisierung</li>
 *   <li>Chronologische Sortierung über {@link Semester#compareTo(Semester)} nach Jahr und Art</li>
 * </ul>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class StatisticsServiceTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private StudierendeRepository studierendeRepository;

    @Autowired
    private LeistungRepository leistungRepository;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private ModulRepository modulRepository;

    @Autowired
    private PlanEintragRepository planEintragRepository;

    private Semester ws2024;
    private Semester ss2025;
    private Semester ws2025;

    @BeforeEach
    void setUpSemester() {
        ws2024 = semesterRepository.findByJahrAndArt(2024, SemesterArt.WS).orElseThrow();
        ss2025 = semesterRepository.findByJahrAndArt(2025, SemesterArt.SS).orElseThrow();
        ws2025 = semesterRepository.findByJahrAndArt(2025, SemesterArt.WS).orElseThrow();
    }

    private Studierende erstelleStudierende(Semester startsemester) {
        Nutzende n = nutzendeRepository.save(new Nutzende(
                "stats-" + UUID.randomUUID() + "@student.htw-berlin.de", "pw", Rolle.STUDIERENDE));
        return studierendeRepository.save(new Studierende(n, startsemester));
    }

    private Leistung fuegeLeistungHinzu(Studierende s, String modulCode, Status status) {
        Modul m = modulRepository.findById(modulCode).orElseThrow();
        return leistungRepository.save(new Leistung(s, m, status));
    }

    @Test
    void leereDatenbankLiefertLeereListe() {
        leistungRepository.deleteAll();
        planEintragRepository.deleteAll();
        studierendeRepository.deleteAll();

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).isEmpty();
    }

    @Test
    void berechnungMitBekannterTestKohorteUndAllenGrenzfaellen() {
        leistungRepository.deleteAll();
        planEintragRepository.deleteAll();
        studierendeRepository.deleteAll();

        // -----------------------------------------------------------------
        // Student 1: 150 LP, Sem 1–3 komplett
        // Sem 1–3 (90 LP) + B43 (20 LP) + B41, B42, B51, B52, B53, B54, B55, B61 (8 * 5 = 40 LP) = 150 LP
        // Grenzfall: Genau 150 LP (< 151 LP) -> nicht regulär, aber mit Zulassung (141–150 LP)
        // -----------------------------------------------------------------
        Studierende s1 = erstelleStudierende(ws2024);
        for (String code : StatisticsService.REQUIRED_MODULES_SEM_1_TO_3) {
            fuegeLeistungHinzu(s1, code, Status.BESTANDEN);
        }
        fuegeLeistungHinzu(s1, "B41", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B42", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B43", Status.BESTANDEN); // 20
        fuegeLeistungHinzu(s1, "B51", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B52", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B53", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B54", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B55", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s1, "B61", Status.BESTANDEN); // 5
        // Summe s1: 90 + 20 + 40 = 150 LP

        // -----------------------------------------------------------------
        // Student 2: 151 LP, Sem 1–3 komplett
        // Sem 1–3 (90 LP) + B43 (20 LP) + 7 * 5 LP (35 LP) + B56 (6 LP) = 151 LP
        // Grenzfall: Genau 151 LP UND alle 17 Module -> bachelorThesisEligible = true!
        // -----------------------------------------------------------------
        Studierende s2 = erstelleStudierende(ws2024);
        for (String code : StatisticsService.REQUIRED_MODULES_SEM_1_TO_3) {
            fuegeLeistungHinzu(s2, code, Status.BESTANDEN);
        }
        fuegeLeistungHinzu(s2, "B41", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B42", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B43", Status.BESTANDEN); // 20
        fuegeLeistungHinzu(s2, "B51", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B52", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B53", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B54", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B55", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s2, "B56", Status.BESTANDEN); // 6
        // Summe s2: 90 + 20 + 35 + 6 = 151 LP

        // -----------------------------------------------------------------
        // Student 3: 150 LP, aber B34 fehlt -> Sem 1–3 unvollständig (completedSemesters1To3 = false, bachelor = false)
        // Sem 1–3 ohne B34 (10 LP) = 80 LP + B43 (20 LP) + 7 * 5 LP (35 LP) + B65 (3 LP) + B66 (12 LP) = 150 LP
        // -----------------------------------------------------------------
        Studierende s3 = erstelleStudierende(ws2024);
        for (String code : StatisticsService.REQUIRED_MODULES_SEM_1_TO_3) {
            if (!code.equals("B34")) {
                fuegeLeistungHinzu(s3, code, Status.BESTANDEN);
            }
        }
        fuegeLeistungHinzu(s3, "B41", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B42", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B43", Status.BESTANDEN); // 20
        fuegeLeistungHinzu(s3, "B51", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B52", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B53", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B54", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B55", Status.BESTANDEN); // 5
        fuegeLeistungHinzu(s3, "B65", Status.BESTANDEN); // 3
        fuegeLeistungHinzu(s3, "B66", Status.BESTANDEN); // 12
        // Summe s3: 80 + 20 + 35 + 15 = 150 LP

        // -----------------------------------------------------------------
        // Student 4: 11 LP bestanden. BELEGT, NICHT_BESTANDEN und plan_eintrag werden ignoriert
        // -----------------------------------------------------------------
        Studierende s4 = erstelleStudierende(ws2024);
        fuegeLeistungHinzu(s4, "B11", Status.BESTANDEN);       // 6
        fuegeLeistungHinzu(s4, "B12", Status.BESTANDEN);       // 5
        fuegeLeistungHinzu(s4, "B13", Status.BELEGT);          // ignoriert
        fuegeLeistungHinzu(s4, "B14", Status.NICHT_BESTANDEN);  // ignoriert
        Modul b15 = modulRepository.findById("B15").orElseThrow();
        planEintragRepository.save(new PlanEintrag(s4, b15, ws2024)); // ignoriert

        // -----------------------------------------------------------------
        // Student 5: 0 LP (ohne Leistungen), damit Kohortengröße >= 5 erreicht wird
        // -----------------------------------------------------------------
        Studierende s5 = erstelleStudierende(ws2024);

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).hasSize(1);
        CohortStatisticsDto ws24Stats = ergebnis.getFirst();

        assertThat(ws24Stats.cohort()).isEqualTo("WS 2024");
        assertThat(ws24Stats.studentCount()).isEqualTo(5);
        // LP-Summe = 150 + 151 + 150 + 11 + 0 = 462 -> 462 / 5 = 92.4
        assertThat(ws24Stats.averageLp()).isEqualTo(92.4);
        // Fortschritt = (92.4 / 180.0) * 100 = 51.333... -> 51.3%
        assertThat(ws24Stats.averageProgressPercent()).isEqualTo(51.3);
        // Sem 1–3 komplett: nur s1 und s2
        assertThat(ws24Stats.completedSemesters1To3Count()).isEqualTo(2);
        // Bachelor-Zulassung regulär (ab 151 LP): nur s2
        assertThat(ws24Stats.bachelorThesisEligibleCount()).isEqualTo(1);
        // mit Zulassung durch den Prüfungsausschuss (141–150 LP): nur s1; s3 fehlt B34
        assertThat(ws24Stats.bachelorThesisWithApprovalCount()).isEqualTo(1);
    }

    @Test
    void kohorteMitWenigerAlsFuenfStudierendenWirdWegenAnonymisierungUebersprungen() {
        leistungRepository.deleteAll();
        planEintragRepository.deleteAll();
        studierendeRepository.deleteAll();

        // Kohorte mit 4 Studierenden (< 5) -> wird nicht angezeigt
        for (int i = 0; i < 4; i++) {
            erstelleStudierende(ws2024);
        }
        // Kohorte mit 5 Studierenden (>= 5) -> wird angezeigt
        for (int i = 0; i < 5; i++) {
            erstelleStudierende(ss2025);
        }

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        assertThat(ergebnis).hasSize(1);
        assertThat(ergebnis.getFirst().cohort()).isEqualTo("SS 2025");
        assertThat(ergebnis.getFirst().studentCount()).isEqualTo(5);
    }

    @Test
    void chronologischeSortierungDerKohortenNachJahrUndArt() {
        leistungRepository.deleteAll();
        planEintragRepository.deleteAll();
        studierendeRepository.deleteAll();

        // Je Kohorte mindestens 5 Personen anlegen, damit sie nicht übersprungen werden
        for (int i = 0; i < 5; i++) {
            erstelleStudierende(ws2025);
            erstelleStudierende(ws2024);
            erstelleStudierende(ss2025);
        }

        List<CohortStatisticsDto> ergebnis = statisticsService.getCohortStatistics();

        // Sortierung: WS 2024 -> SS 2025 -> WS 2025
        assertThat(ergebnis).extracting(CohortStatisticsDto::cohort)
                .containsExactly("WS 2024", "SS 2025", "WS 2025");
    }

    // -------------------------------------------------------------------------
    // Integrationstests für Modulstatistiken (US-55)
    // -------------------------------------------------------------------------

    @Test
    void modulStatistikenIntegrationstestMitLeistungenUndFilter() {
        leistungRepository.deleteAll();
        planEintragRepository.deleteAll();
        studierendeRepository.deleteAll();

        // 5 Studierende im WS 2024 (erfüllt MIN_COHORT_SIZE = 5 beim Filtern)
        Studierende s1 = erstelleStudierende(ws2024);
        Studierende s2 = erstelleStudierende(ws2024);
        for (int i = 0; i < 3; i++) {
            Studierende extra = erstelleStudierende(ws2024);
            Leistung le = fuegeLeistungHinzu(extra, "B12", Status.BELEGT);
            le.setAnzahlVersuche(1);
            leistungRepository.save(le);
        }
        // 1 Studierende im SS 2025 (unter MIN_COHORT_SIZE = 5)
        Studierende s3 = erstelleStudierende(ss2025);

        // s1: B11 bestanden mit 2.0 (2 Versuche)
        Leistung l1 = fuegeLeistungHinzu(s1, "B11", Status.BESTANDEN);
        l1.setNote(new java.math.BigDecimal("2.0"));
        l1.setAnzahlVersuche(2);
        leistungRepository.save(l1);

        // s2: B11 bestanden mit 3.0 (1 Versuch)
        Leistung l2 = fuegeLeistungHinzu(s2, "B11", Status.BESTANDEN);
        l2.setNote(new java.math.BigDecimal("3.0"));
        l2.setAnzahlVersuche(1);
        leistungRepository.save(l2);

        // s3: B11 nicht bestanden mit 5.0 (3 Versuche -> kritisch)
        Leistung l3 = fuegeLeistungHinzu(s3, "B11", Status.NICHT_BESTANDEN);
        l3.setNote(new java.math.BigDecimal("5.0"));
        l3.setAnzahlVersuche(3);
        leistungRepository.save(l3);

        // s1: B43 (unbenotetes Fachpraktikum) bestanden
        Leistung l4 = fuegeLeistungHinzu(s1, "B43", Status.BESTANDEN);
        l4.setAnzahlVersuche(1);
        leistungRepository.save(l4);

        // 1. Ohne Filter: alle Module der PO enthalten
        List<de.htw.studyplanner.backend.dto.ModuleStatisticsDto> alle =
                statisticsService.getModuleStatistics(null);
        assertThat(alle).isNotEmpty();

        var b11Stats = alle.stream().filter(m -> m.code().equals("B11")).findFirst().orElseThrow();
        assertThat(b11Stats.name()).isEqualTo("Programmierung 1");
        assertThat(b11Stats.semester()).isEqualTo(1);
        assertThat(b11Stats.passedCount()).isEqualTo(2);
        assertThat(b11Stats.enrolledCount()).isEqualTo(0);
        assertThat(b11Stats.failedCount()).isEqualTo(1);
        assertThat(b11Stats.repeatCount()).isEqualTo(2);
        assertThat(b11Stats.criticalAttemptCount()).isEqualTo(1);
        assertThat(b11Stats.averageGrade()).isEqualByComparingTo("2.5");

        var b43Stats = alle.stream().filter(m -> m.code().equals("B43")).findFirst().orElseThrow();
        assertThat(b43Stats.passedCount()).isEqualTo(1);
        assertThat(b43Stats.averageGrade()).isNull(); // unbenotet!

        // 2. Mit Kohorten-Filter "WS 2024": s3 (SS 2025) ist nicht enthalten
        List<de.htw.studyplanner.backend.dto.ModuleStatisticsDto> gefiltert =
                statisticsService.getModuleStatistics("WS 2024");
        var b11Gefiltert = gefiltert.stream().filter(m -> m.code().equals("B11")).findFirst().orElseThrow();
        assertThat(b11Gefiltert.passedCount()).isEqualTo(2);
        assertThat(b11Gefiltert.failedCount()).isEqualTo(0);
        assertThat(b11Gefiltert.criticalAttemptCount()).isEqualTo(0);
        assertThat(b11Gefiltert.repeatCount()).isEqualTo(1);
        assertThat(b11Gefiltert.averageGrade()).isEqualByComparingTo("2.5");
    }

    @Test
    void modulStatistikMitWenigerAlsFuenfStudierendenInKohorteGibtLeereListeZurueck() {
        Semester ws2025 = semesterRepository.findByJahrAndArt(2025, SemesterArt.WS).orElseGet(() -> semesterRepository.save(new Semester(2025, SemesterArt.WS)));
        // Nur 4 Studierende in WS 2025 (< MIN_COHORT_SIZE = 5)
        for (int i = 0; i < 4; i++) {
            Studierende s = erstelleStudierende(ws2025);
            Leistung l = fuegeLeistungHinzu(s, "B11", Status.BESTANDEN);
            l.setNote(new java.math.BigDecimal("2.0"));
            l.setAnzahlVersuche(1);
            leistungRepository.save(l);
        }

        // Ohne Filter sind die Leistungen in der Gesamtsicht enthalten
        assertThat(statisticsService.getModuleStatistics(null)).isNotEmpty();

        // Mit Kohorten-Filter "WS 2025" (< 5 Studierende) muss zum Schutz der Anonymität leer zurückkommen
        assertThat(statisticsService.getModuleStatistics("WS 2025")).isEmpty();
    }
}
