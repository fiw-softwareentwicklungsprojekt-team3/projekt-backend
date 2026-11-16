package de.htw.studyplanner.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.VoraussetzungRepository;

/**
 * Prüft die Voraussetzungen der geplanten Module.
 */
@Service
public class PlanPruefer {

    private final PlanEintragRepository planEintragRepository;
    private final LeistungRepository leistungRepository;
    private final VoraussetzungRepository voraussetzungRepository;
    private final SemesterService semesterService;

    public PlanPruefer(
            PlanEintragRepository planEintragRepository,
            LeistungRepository leistungRepository,
            VoraussetzungRepository voraussetzungRepository,
            SemesterService semesterService) {
        this.planEintragRepository = planEintragRepository;
        this.leistungRepository = leistungRepository;
        this.voraussetzungRepository = voraussetzungRepository;
        this.semesterService = semesterService;
    }

    /**
     * Prüft alle Modulvoraussetzungen einer Studierenden.
     *
     * @param studierende die zu prüfende Person
     * @param heute       aktuelles Datum
     * @return alle gefundenen Verstöße
     */
    @Transactional(readOnly = true)
    public List<PlanVerstoss> pruefe(Studierende studierende, LocalDate heute) {
        Objects.requireNonNull(studierende, "studierende");
        Objects.requireNonNull(heute, "heute");

        List<PlanEintrag> plan = planEintragRepository.findByStudierende(studierende);
        List<Leistung> leistungen = leistungRepository.findByStudierende(studierende);
        List<Voraussetzung> voraussetzungen = voraussetzungRepository.findAll();

        Map<String, PlanEintrag> planNachModul = plan.stream()
                .collect(Collectors.toMap(
                        eintrag -> eintrag.getModul().getCode(),
                        Function.identity()));

        Map<String, Leistung> leistungNachModul = leistungen.stream()
                .collect(Collectors.toMap(
                        leistung -> leistung.getModul().getCode(),
                        Function.identity()));

        Map<String, List<Voraussetzung>> voraussetzungenNachModul = voraussetzungen.stream()
                .filter(voraussetzung -> voraussetzung.getModul() != null)
                .collect(Collectors.groupingBy(
                        voraussetzung -> voraussetzung.getModul().getCode()));

        List<PlanVerstoss> verstosse = new ArrayList<>();

        for (PlanEintrag geplanterEintrag : plan) {
            String modulCode = geplanterEintrag.getModul().getCode();
            Leistung eigeneLeistung = leistungNachModul.get(modulCode);

            if (eigeneLeistung != null
                    && eigeneLeistung.getStatus() == Status.BESTANDEN) {
                continue;
            }

            List<Voraussetzung> modulVoraussetzungen =
                    voraussetzungenNachModul.getOrDefault(modulCode, List.of());

            for (Voraussetzung voraussetzung : modulVoraussetzungen) {
                if (voraussetzung.getMinLp() != null
                        || voraussetzung.getVorausgesetztesModul() == null) {
                    continue;
                }

                PlanVerstoss verstoss = pruefeVoraussetzung(
                        geplanterEintrag,
                        voraussetzung,
                        planNachModul,
                        leistungNachModul);

                if (verstoss != null) {
                    verstosse.add(verstoss);
                }
            }
        }

        return verstosse;
    }

    private PlanVerstoss pruefeVoraussetzung(
            PlanEintrag geplanterEintrag,
            Voraussetzung voraussetzung,
            Map<String, PlanEintrag> planNachModul,
            Map<String, Leistung> leistungNachModul) {

        String geprueftesModul = geplanterEintrag.getModul().getCode();
        String vorausgesetztesModul =
                voraussetzung.getVorausgesetztesModul().getCode();

        PlanEintrag vorausgesetzterPlan =
                planNachModul.get(vorausgesetztesModul);

        Leistung vorausgesetzteLeistung =
                leistungNachModul.get(vorausgesetztesModul);

        if (vorausgesetzteLeistung != null
                && vorausgesetzteLeistung.getStatus() == Status.BESTANDEN) {
            return null;
        }

        boolean gleicheSemesterErlaubt =
                voraussetzung.isGleichesSemester();

        if (vorausgesetzterPlan != null
                && darfAlsGeplantZaehlen(
                        vorausgesetzterPlan,
                        geplanterEintrag,
                        gleicheSemesterErlaubt)) {

            if (vorausgesetzteLeistung == null
                    || vorausgesetzteLeistung.getStatus() == Status.BELEGT) {
                return new PlanVerstoss(
                        PruefSchwere.HINWEIS,
                        geprueftesModul,
                        vorausgesetztesModul,
                        "Ergebnis ausstehend");
            }

            if (vorausgesetzteLeistung.getStatus() == Status.NICHT_BESTANDEN
                    && istNachDurchgefallenNeuGeplant(
                            vorausgesetzteLeistung,
                            vorausgesetzterPlan)) {
                return null;
            }
        }

        return new PlanVerstoss(
                schweregrad(voraussetzung.getArt()),
                geprueftesModul,
                vorausgesetztesModul,
                "Voraussetzung " + vorausgesetztesModul
                        + " für " + geprueftesModul
                        + " ist nicht erfüllt.");
    }

    private boolean darfAlsGeplantZaehlen(
            PlanEintrag vorausgesetzterPlan,
            PlanEintrag geplanterEintrag,
            boolean gleicheSemesterErlaubt) {

        if (gleicheSemesterErlaubt) {
            return semesterService.istFrueher(
                    vorausgesetzterPlan.getSemester(),
                    geplanterEintrag.getSemester())
                    || semesterService.istGleich(
                            vorausgesetzterPlan.getSemester(),
                            geplanterEintrag.getSemester());
        }

        return semesterService.istFrueher(
                vorausgesetzterPlan.getSemester(),
                geplanterEintrag.getSemester());
    }

    private boolean istNachDurchgefallenNeuGeplant(
            Leistung leistung,
            PlanEintrag planEintrag) {

        return leistung.getSemester() != null
                && semesterService.istFrueher(
                        leistung.getSemester(),
                        planEintrag.getSemester());
    }

    private PruefSchwere schweregrad(VoraussetzungsArt art) {
        return art == VoraussetzungsArt.NOTWENDIG
                ? PruefSchwere.FEHLER
                : PruefSchwere.WARNUNG;
    }
}