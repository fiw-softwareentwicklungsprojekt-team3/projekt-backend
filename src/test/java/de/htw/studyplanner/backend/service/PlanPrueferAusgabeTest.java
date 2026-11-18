package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.VoraussetzungRepository;

class PlanPrueferAusgabeTest {

    private static final LocalDate HEUTE_WS_2026 = LocalDate.of(2026, 11, 15);

    private final PlanEintragRepository planEintragRepository = mock(PlanEintragRepository.class);
    private final LeistungRepository leistungRepository = mock(LeistungRepository.class);
    private final VoraussetzungRepository voraussetzungRepository = mock(VoraussetzungRepository.class);
    private final SemesterService semesterService = new SemesterService(mock(SemesterRepository.class));

    private final PlanPruefer planPruefer = new PlanPruefer(
            planEintragRepository, leistungRepository, voraussetzungRepository, semesterService);

    private final Studierende studierende = new Studierende(
            new Nutzende("test@student.htw-berlin.de", "hash", Rolle.STUDIERENDE),
            new Semester(2026, SemesterArt.WS));
    private final Modul b11 = modul("B11");
    private final Modul b34 = modul("B34");

    private final Semester ws2025 = new Semester(2025, SemesterArt.WS);
    private final Semester ws2026 = new Semester(2026, SemesterArt.WS);
    private final Semester ws2027 = new Semester(2027, SemesterArt.WS);

    @BeforeEach
    void setUp() {
        when(voraussetzungRepository.findAll()).thenReturn(List.of(
                voraussetzung(b34, b11, VoraussetzungsArt.NOTWENDIG, false)));
    }

    @Test
    void b34MitNvB11_erfuellt() {
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(
                new PlanEintrag(studierende, b11, ws2026),
                new PlanEintrag(studierende, b34, ws2027)));
        when(leistungRepository.findByStudierende(studierende)).thenReturn(List.of());

        List<PlanVerstoss> verstoesse = planPruefer.pruefe(studierende, HEUTE_WS_2026);

        assertThat(verstoesse).isEmpty();
    }

    @Test
    void b34MitNvB11_ausstehend() {
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(
                new PlanEintrag(studierende, b11, ws2025),
                new PlanEintrag(studierende, b34, ws2027)));
        when(leistungRepository.findByStudierende(studierende)).thenReturn(List.of());

        List<PlanVerstoss> verstoesse = planPruefer.pruefe(studierende, HEUTE_WS_2026);

        assertThat(verstoesse).containsExactly(
                new PlanVerstoss(PruefSchwere.HINWEIS, "B34", "B11", "Ergebnis ausstehend"));
    }

    @Test
    void b34MitNvB11_durchgefallen() {
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(
                new PlanEintrag(studierende, b11, ws2025),
                new PlanEintrag(studierende, b34, ws2027)));
        when(leistungRepository.findByStudierende(studierende)).thenReturn(List.of(
                leistung(b11, Status.NICHT_BESTANDEN, ws2025)));

        List<PlanVerstoss> verstoesse = planPruefer.pruefe(studierende, HEUTE_WS_2026);

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.FEHLER);
        assertThat(verstoesse.get(0).geprueftesModul()).isEqualTo("B34");
        assertThat(verstoesse.get(0).fehlendeVoraussetzung()).isEqualTo("B11");
    }

    @Test
    void b34MitNvB11_durchgefallenUndNeuGeplant() {
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(
                new PlanEintrag(studierende, b11, ws2026),
                new PlanEintrag(studierende, b34, ws2027)));
        when(leistungRepository.findByStudierende(studierende)).thenReturn(List.of(
                leistung(b11, Status.NICHT_BESTANDEN, ws2025)));

        List<PlanVerstoss> verstoesse = planPruefer.pruefe(studierende, HEUTE_WS_2026);

        assertThat(verstoesse).isEmpty();
    }

    @Test
    void b34MitNvB11_fehlt() {
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(
                new PlanEintrag(studierende, b34, ws2027)));
        when(leistungRepository.findByStudierende(studierende)).thenReturn(List.of());

        List<PlanVerstoss> verstoesse = planPruefer.pruefe(studierende, HEUTE_WS_2026);

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.FEHLER);
    }

    @Test
    void b34MitNvB11_gleichesSemester() {
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(List.of(
                new PlanEintrag(studierende, b11, ws2026),
                new PlanEintrag(studierende, b34, ws2026)));
        when(leistungRepository.findByStudierende(studierende)).thenReturn(List.of());

        List<PlanVerstoss> verstoesse = planPruefer.pruefe(studierende, HEUTE_WS_2026);

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.FEHLER);
        verify(planEintragRepository).findByStudierende(studierende);
        verify(leistungRepository).findByStudierende(studierende);
        verify(voraussetzungRepository).findAll();
    }

    private static Modul modul(String code) {
        return instanziieren(Modul.class, modul -> {
            ReflectionTestUtils.setField(modul, "code", code);
        });
    }

    private static Voraussetzung voraussetzung(
            Modul modul, Modul vorausgesetzt, VoraussetzungsArt art, boolean gleichesSemester) {
        return instanziieren(Voraussetzung.class, voraussetzung -> {
            ReflectionTestUtils.setField(voraussetzung, "modul", modul);
            ReflectionTestUtils.setField(voraussetzung, "vorausgesetztesModul", vorausgesetzt);
            ReflectionTestUtils.setField(voraussetzung, "art", art);
            ReflectionTestUtils.setField(voraussetzung, "gleichesSemester", gleichesSemester);
            ReflectionTestUtils.setField(voraussetzung, "minLp", null);
        });
    }

    private static <T> T instanziieren(Class<T> type, java.util.function.Consumer<T> initializer) {
        try {
            java.lang.reflect.Constructor<T> konstruktor =
                    type.getDeclaredConstructor();
            konstruktor.setAccessible(true);
            T objekt = konstruktor.newInstance();
            initializer.accept(objekt);
            return objekt;
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private Leistung leistung(Modul modul, Status status, Semester semester) {
        Leistung l = new Leistung(studierende, modul, status);
        l.setSemester(semester);
        return l;
    }
}
