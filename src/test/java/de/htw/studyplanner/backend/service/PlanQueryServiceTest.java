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

import de.htw.studyplanner.backend.dto.PlanEntryPlanResponse;
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

class PlanQueryServiceTest {

    private final PlanEintragRepository planEintragRepository = mock(PlanEintragRepository.class);
    private final StudierendeRepository studierendeRepository = mock(StudierendeRepository.class);
    private final FakePlanPruefer planPruefer = new FakePlanPruefer();

    private final PlanQueryService planQueryService =
            new PlanQueryService(planEintragRepository, studierendeRepository, planPruefer);

    private final Studierende studierende = new Studierende(
            new Nutzende("student@student.htw-berlin.de", "hash", Rolle.STUDIERENDE),
            new Semester(2026, SemesterArt.WS));

    static class FakePlanPruefer extends PlanPruefer {
        List<PlanVerstoss> ergebnis = List.of();

        FakePlanPruefer() {
            super(null, null, null, null);
        }

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

    private static Modul modul(String code) {
        try {
            java.lang.reflect.Constructor<Modul> c = Modul.class.getDeclaredConstructor();
            c.setAccessible(true);
            Modul m = c.newInstance();
            ReflectionTestUtils.setField(m, "code", code);
            return m;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

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
        when(studierendeRepository.findById(42)).thenReturn(Optional.empty());

        assertThatThrownBy(planQueryService::eigenerPlan)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Dein Konto existiert nicht mehr");
    }

    @Test
    void eigenerPlan_ohneVerstoesse_prerequisitesMetIsTrue() {
        when(studierendeRepository.findById(42)).thenReturn(Optional.of(studierende));

        Modul b11 = modul("B11");
        PlanEintrag eintrag = new PlanEintrag(studierende, b11, new Semester(2026, SemesterArt.WS));

        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of();

        List<PlanEntryPlanResponse> antwort = planQueryService.eigenerPlan();

        assertThat(antwort).hasSize(1);
        assertThat(antwort.get(0).moduleCode()).isEqualTo("B11");
        assertThat(antwort.get(0).prerequisitesMet()).isTrue();
        assertThat(antwort.get(0).violations()).isEmpty();
    }

    @Test
    void eigenerPlan_mitFehler_prerequisitesMetIsFalse() {
        when(studierendeRepository.findById(42)).thenReturn(Optional.of(studierende));

        Modul b34 = modul("B34");
        PlanEintrag eintrag = new PlanEintrag(studierende, b34, new Semester(2027, SemesterArt.WS));

        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of(
                new PlanVerstoss(PruefSchwere.FEHLER, "B34", "B11", "Voraussetzung B11 fehlt."));

        List<PlanEntryPlanResponse> antwort = planQueryService.eigenerPlan();

        assertThat(antwort).hasSize(1);
        assertThat(antwort.get(0).moduleCode()).isEqualTo("B34");
        assertThat(antwort.get(0).prerequisitesMet()).isFalse();
        assertThat(antwort.get(0).violations()).hasSize(1);
        assertThat(antwort.get(0).violations().get(0).severity()).isEqualTo(PruefSchwere.FEHLER);
        assertThat(antwort.get(0).violations().get(0).prerequisiteCode()).isEqualTo("B11");
    }

    @Test
    void eigenerPlan_mitWarnung_prerequisitesMetBleibtTrue() {
        when(studierendeRepository.findById(42)).thenReturn(Optional.of(studierende));

        Modul b34 = modul("B34");
        PlanEintrag eintrag = new PlanEintrag(studierende, b34, new Semester(2027, SemesterArt.WS));

        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of(
                new PlanVerstoss(PruefSchwere.WARNUNG, "B34", "B21", "Voraussetzung B21 empfohlen."));

        List<PlanEntryPlanResponse> antwort = planQueryService.eigenerPlan();

        assertThat(antwort).hasSize(1);
        assertThat(antwort.get(0).prerequisitesMet()).isTrue();
        assertThat(antwort.get(0).violations()).hasSize(1);
        assertThat(antwort.get(0).violations().get(0).severity()).isEqualTo(PruefSchwere.WARNUNG);
    }

    @Test
    void eigenerPlan_mitWahlangebotCode() {
        when(studierendeRepository.findById(42)).thenReturn(Optional.of(studierende));

        Modul b41 = modul("B41");
        PlanEintrag eintrag = new PlanEintrag(studierende, b41, new Semester(2027, SemesterArt.SS));
        Wahlangebot wp1 = wahlangebot("WP1");
        eintrag.setWahlangebot(wp1);
        eintrag.setKursCode("K-123");
        eintrag.setKursTitel("Web Security");

        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(eintrag));
        planPruefer.ergebnis = List.of();

        List<PlanEntryPlanResponse> antwort = planQueryService.eigenerPlan();

        assertThat(antwort).hasSize(1);
        assertThat(antwort.get(0).offerCode()).isEqualTo("WP1");
        assertThat(antwort.get(0).courseCode()).isEqualTo("K-123");
        assertThat(antwort.get(0).courseTitle()).isEqualTo("Web Security");
    }
}
