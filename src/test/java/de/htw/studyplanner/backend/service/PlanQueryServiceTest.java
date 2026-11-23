package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import de.htw.studyplanner.backend.dto.PlanEntryDto;
import de.htw.studyplanner.backend.dto.PlanResponse;
import de.htw.studyplanner.backend.dto.PlanSemesterDto;
import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Wahlangebot;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.AngemeldetePerson;

/**
 * Unit-Tests für {@link PlanQueryService}: Gruppierung, Sortierung, LP-Summen und die
 * Zuordnung der Verstöße zu den Einträgen. Der PlanPruefer ist durch eine Attrappe ersetzt.
 */
class PlanQueryServiceTest {

    private final PlanEintragRepository planEintragRepository = mock(PlanEintragRepository.class);
    private final StudierendeRepository studierendeRepository = mock(StudierendeRepository.class);
    private final FakePlanPruefer planPruefer = new FakePlanPruefer();

    private final PlanQueryService planQueryService = new PlanQueryService(
            planEintragRepository,
            studierendeRepository,
            planPruefer,
            new SemesterService(null),
            30);

    private final Studierende studierende = new Studierende(
            new Nutzende("student@student.htw-berlin.de", "hash", Rolle.STUDIERENDE),
            new Semester(2026, SemesterArt.WS));

    /** PlanPruefer-Attrappe, die die vorgegebenen Verstöße zurückgibt. */
    static class FakePlanPruefer extends PlanPruefer {
        List<PlanVerstoss> ergebnis = List.of();

        FakePlanPruefer() {
            super(null, null, null, null);
        }

        /** Liefert {@link #ergebnis}, unabhängig von Person und Datum. */
        @Override
        public List<PlanVerstoss> pruefe(Studierende studierende, LocalDate heute) {
            return ergebnis;
        }
    }

    @BeforeEach
    void setUpSecurity() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                new AngemeldetePerson(42, "student@student.htw-berlin.de"), null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    /** Modul mit Code und 5 LP; der Konstruktor ist nicht öffentlich. */
    private static Modul modul(String code) {
        return modul(code, 5);
    }

    /** Modul mit Code und LP; der Konstruktor ist nicht öffentlich. */
    private static Modul modul(String code, int lp) {
        try {
            java.lang.reflect.Constructor<Modul> c = Modul.class.getDeclaredConstructor();
            c.setAccessible(true);
            Modul m = c.newInstance();
            ReflectionTestUtils.setField(m, "code", code);
            ReflectionTestUtils.setField(m, "lp", lp);
            return m;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /** Wahlangebot mit Code; der Konstruktor ist nicht öffentlich. */
    private static Wahlangebot wahlangebot(String code) {
        try {
            java.lang.reflect.Constructor<Wahlangebot> c = Wahlangebot.class.getDeclaredConstructor();
            c.setAccessible(true);
            Wahlangebot w = c.newInstance();
            ReflectionTestUtils.setField(w, "code", code);
            return w;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void eigenerPlan_personNichtGefunden_wirftUnauthorizedException() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.empty());

        assertThatThrownBy(planQueryService::eigenerPlan)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Dein Konto existiert nicht mehr");
    }

    @Test
    void eigenerPlan_ohneVerstoesse_prerequisitesMetIsTrue() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        Modul b11 = modul("B11");
        PlanEintrag eintrag = new PlanEintrag(studierende, b11, new Semester(2026, SemesterArt.WS));

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of();

        PlanEntryDto antwort = einzigerEintrag(planQueryService.eigenerPlan());

        assertThat(antwort.moduleCode()).isEqualTo("B11");
        assertThat(antwort.prerequisitesMet()).isTrue();
        assertThat(antwort.violations()).isEmpty();
    }

    @Test
    void eigenerPlan_mitFehler_prerequisitesMetIsFalse() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        Modul b34 = modul("B34");
        PlanEintrag eintrag = new PlanEintrag(studierende, b34, new Semester(2027, SemesterArt.WS));

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of(
                new PlanVerstoss(PruefSchwere.FEHLER, "B34", "B11", "Voraussetzung B11 fehlt."));

        PlanEntryDto antwort = einzigerEintrag(planQueryService.eigenerPlan());

        assertThat(antwort.moduleCode()).isEqualTo("B34");
        assertThat(antwort.prerequisitesMet()).isFalse();
        assertThat(antwort.violations()).hasSize(1);
        assertThat(antwort.violations().get(0).severity()).isEqualTo(PruefSchwere.FEHLER);
        assertThat(antwort.violations().get(0).prerequisiteCode()).isEqualTo("B11");
    }

    @Test
    void eigenerPlan_mitWarnung_prerequisitesMetBleibtTrue() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        Modul b34 = modul("B34");
        PlanEintrag eintrag = new PlanEintrag(studierende, b34, new Semester(2027, SemesterArt.WS));

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of(
                new PlanVerstoss(PruefSchwere.WARNUNG, "B34", "B21", "Voraussetzung B21 empfohlen."));

        PlanEntryDto antwort = einzigerEintrag(planQueryService.eigenerPlan());

        assertThat(antwort.prerequisitesMet()).isTrue();
        assertThat(antwort.violations()).hasSize(1);
        assertThat(antwort.violations().get(0).severity()).isEqualTo(PruefSchwere.WARNUNG);
    }

    @Test
    void eigenerPlan_mitWahlangebotCode() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        Modul b41 = modul("B41");
        PlanEintrag eintrag = new PlanEintrag(studierende, b41, new Semester(2027, SemesterArt.SS));
        Wahlangebot wp1 = wahlangebot("WP1");
        eintrag.setWahlangebot(wp1);
        eintrag.setKursCode("K-123");
        eintrag.setKursTitel("Web Security");

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of();

        PlanEntryDto antwort = einzigerEintrag(planQueryService.eigenerPlan());

        assertThat(antwort.offerCode()).isEqualTo("WP1");
        assertThat(antwort.courseCode()).isEqualTo("K-123");
        assertThat(antwort.courseTitle()).isEqualTo("Web Security");
    }

    @Test
    void eigenerPlan_gruppiertNachSemesterSortiertUndSummiertLp() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        // Absichtlich unsortiert, mit zwei verschiedenen Semester-Instanzen für WS 2027
        PlanEintrag b34 = new PlanEintrag(studierende, modul("B34", 10), new Semester(2027, SemesterArt.WS));
        PlanEintrag b21 = new PlanEintrag(studierende, modul("B21", 5), new Semester(2027, SemesterArt.SS));
        PlanEintrag b11 = new PlanEintrag(studierende, modul("B11", 6), new Semester(2027, SemesterArt.WS));

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(List.of(b34, b21, b11));

        PlanResponse plan = planQueryService.eigenerPlan();

        assertThat(plan.startSemester()).isEqualTo("WS 2026");
        assertThat(plan.totalLp()).isEqualTo(21);
        assertThat(plan.maxLpPerSemester()).isEqualTo(30);
        assertThat(plan.semesters()).extracting(PlanSemesterDto::semester).containsExactly("SS 2027", "WS 2027");
        assertThat(plan.semesters()).extracting(PlanSemesterDto::lp).containsExactly(5, 16);
        assertThat(plan.semesters()).extracting(PlanSemesterDto::sws).containsExactly(0, 0);
        assertThat(plan.semesters()).extracting(PlanSemesterDto::lpLimitExceeded)
                .containsExactly(false, false);
        assertThat(plan.semesters().get(1).entries()).extracting(PlanEntryDto::moduleCode)
                .containsExactly("B11", "B34");
    }

    @Test
    void eigenerPlan_leererPlan_liefertLeereListeUndNullLp() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));
        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(List.of());

        PlanResponse plan = planQueryService.eigenerPlan();

        assertThat(plan.totalLp()).isZero();
        assertThat(plan.semesters()).isEmpty();
    }

    @Test
    void genauDreißigLpErzeugenKeineWarnung() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        List<PlanEintrag> eintraege = List.of(
                new PlanEintrag(studierende, modul("B11", 6), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B12", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B13", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B14", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B15", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B16", 4), new Semester(2026, SemesterArt.WS)));

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(eintraege);

        PlanSemesterDto semester = planQueryService.eigenerPlan().semesters().get(0);

        assertThat(semester.lp()).isEqualTo(30);
        assertThat(semester.lpLimitExceeded()).isFalse();
    }

    @Test
    void einunddreißigLpErzeugenWarnung() {
        when(studierendeRepository.findMitStartsemester(42)).thenReturn(Optional.of(studierende));

        List<PlanEintrag> eintraege = List.of(
                new PlanEintrag(studierende, modul("B11", 6), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B12", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B13", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B14", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B15", 5), new Semester(2026, SemesterArt.WS)),
                new PlanEintrag(studierende, modul("B31", 5), new Semester(2026, SemesterArt.WS)));

        when(planEintragRepository.findPlanMitDetails(42)).thenReturn(eintraege);

        PlanSemesterDto semester = planQueryService.eigenerPlan().semesters().get(0);

        assertThat(semester.lp()).isEqualTo(31);
        assertThat(semester.lpLimitExceeded()).isTrue();
    }

    /** Prüft, dass der Plan genau einen Eintrag hat, und liefert ihn. */
    private static PlanEntryDto einzigerEintrag(PlanResponse plan) {
        assertThat(plan.semesters()).hasSize(1);
        assertThat(plan.semesters().get(0).entries()).hasSize(1);
        return plan.semesters().get(0).entries().get(0);
    }
}
