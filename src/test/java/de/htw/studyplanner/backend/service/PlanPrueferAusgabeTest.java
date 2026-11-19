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
    private final Modul b43 = modul("B43");
    private final Modul b65 = modul("B65");
    private final Modul b66 = modul("B66");

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

    @Test
    void lp_genauMinLp_inOrdnung() {
        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.NOTWENDIG, 60, 0),
                List.of(new PlanEintrag(studierende, b43, ws2027)),
                List.of(leistung(modul("F1", 60), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).isEmpty();
    }

    @Test
    void lp_einsDarunter_fehlerMitIstUndSoll() {
        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.NOTWENDIG, 60, 0),
                List.of(new PlanEintrag(studierende, b43, ws2027)),
                List.of(leistung(modul("F1", 59), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.FEHLER);
        assertThat(verstoesse.get(0).fehlendeVoraussetzung()).isNull();
        assertThat(verstoesse.get(0).meldung())
                .isEqualTo("B43 braucht 60 LP, bis dahin sind 59 LP erreicht oder geplant");
    }

    @Test
    void lp_toleranzGrenze141_warnung() {
        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b66, VoraussetzungsArt.NOTWENDIG, 151, 10),
                List.of(new PlanEintrag(studierende, b66, ws2027)),
                List.of(leistung(modul("F1", 141), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.WARNUNG);
        assertThat(verstoesse.get(0).meldung()).contains("Prüfungsausschuss");
    }

    @Test
    void lp_toleranzGrenze140_fehler() {
        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b66, VoraussetzungsArt.NOTWENDIG, 151, 10),
                List.of(new PlanEintrag(studierende, b66, ws2027)),
                List.of(leistung(modul("F1", 140), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.FEHLER);
    }

    @Test
    void lp_empfohlen_nurWarnung() {
        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.EMPFOHLEN, 60, 0),
                List.of(new PlanEintrag(studierende, b43, ws2027)),
                List.of(leistung(modul("F1", 59), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.WARNUNG);
    }

    @Test
    void lp_durchgefallenesModulZaehltNicht() {
        Modul durchgefallen = modul("F1", 60);

        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.NOTWENDIG, 60, 0),
                List.of(new PlanEintrag(studierende, durchgefallen, ws2025),
                        new PlanEintrag(studierende, b43, ws2027)),
                List.of(leistung(durchgefallen, Status.NICHT_BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).meldung())
                .isEqualTo("B43 braucht 60 LP, bis dahin sind 0 LP erreicht oder geplant");
    }

    @Test
    void lp_gleichesSemesterZaehltNicht() {
        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.NOTWENDIG, 60, 0),
                List.of(new PlanEintrag(studierende, modul("F1", 60), ws2027),
                        new PlanEintrag(studierende, b43, ws2027)),
                List.of());

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).schwere()).isEqualTo(PruefSchwere.FEHLER);
    }

    @Test
    void lp_durchgefallenUndNeuGeplant_zaehlt() {
        Modul f1 = modul("F1", 60);

        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.NOTWENDIG, 60, 0),
                List.of(new PlanEintrag(studierende, f1, ws2026),
                        new PlanEintrag(studierende, b43, ws2027)),
                List.of(leistung(f1, Status.NICHT_BESTANDEN, ws2025)));

        assertThat(verstoesse).isEmpty();
    }

    @Test
    void lp_modulZaehltNurEinmal() {
        Modul f1 = modul("F1", 60);

        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lpVoraussetzung(b43, VoraussetzungsArt.NOTWENDIG, 61, 0),
                List.of(new PlanEintrag(studierende, f1, ws2025),
                        new PlanEintrag(studierende, b43, ws2027)),
                List.of(leistung(f1, Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).meldung())
                .isEqualTo("B43 braucht 61 LP, bis dahin sind 60 LP erreicht oder geplant");
    }

    @Test
    void lp_b65Mit177_b66ImSelbenSemester_inOrdnung() {
        Voraussetzung lp177 = lpVoraussetzung(b65, VoraussetzungsArt.NOTWENDIG, 177, 0);
        ReflectionTestUtils.setField(lp177, "gleichesSemester", true);

        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lp177,
                List.of(new PlanEintrag(studierende, b65, ws2027),
                        new PlanEintrag(studierende, modul("B66", 12), ws2027)),
                List.of(leistung(modul("F1", 165), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).isEmpty();
    }

    private static Modul modul(String code) {
        return instanziieren(Modul.class, modul -> {
            ReflectionTestUtils.setField(modul, "code", code);
        });
    }

    @Test
    void lp_gleichesSemester_eigenesModulZaehltNichtMit() {
        Modul b65Mit3Lp = modul("B65", 3);
        Voraussetzung lp177 = lpVoraussetzung(b65Mit3Lp, VoraussetzungsArt.NOTWENDIG, 177, 0);
        ReflectionTestUtils.setField(lp177, "gleichesSemester", true);

        List<PlanVerstoss> verstoesse = pruefeMitLp(
                lp177,
                List.of(new PlanEintrag(studierende, b65Mit3Lp, ws2027)),
                List.of(leistung(modul("F1", 174), Status.BESTANDEN, ws2025)));

        assertThat(verstoesse).hasSize(1);
        assertThat(verstoesse.get(0).meldung())
                .isEqualTo("B65 braucht 177 LP, bis dahin sind 174 LP erreicht oder geplant");
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

    private static Modul modul(String code, int lp) {
        return instanziieren(Modul.class, modul -> {
            ReflectionTestUtils.setField(modul, "code", code);
            ReflectionTestUtils.setField(modul, "lp", lp);
        });
    }

    private static Voraussetzung lpVoraussetzung(
            Modul modul, VoraussetzungsArt art, int minLp, int toleranzLp) {
        return instanziieren(Voraussetzung.class, voraussetzung -> {
            ReflectionTestUtils.setField(voraussetzung, "modul", modul);
            ReflectionTestUtils.setField(voraussetzung, "art", art);
            ReflectionTestUtils.setField(voraussetzung, "minLp", minLp);
            ReflectionTestUtils.setField(voraussetzung, "toleranzLp", toleranzLp);
        });
    }

    private List<PlanVerstoss> pruefeMitLp(
            Voraussetzung voraussetzung, List<PlanEintrag> plan, List<Leistung> leistungen) {
        when(voraussetzungRepository.findAll()).thenReturn(List.of(voraussetzung));
        when(planEintragRepository.findByStudierende(studierende)).thenReturn(plan);
        when(leistungRepository.findByStudierende(studierende)).thenReturn(leistungen);
        return planPruefer.pruefe(studierende, HEUTE_WS_2026);
    }
}
