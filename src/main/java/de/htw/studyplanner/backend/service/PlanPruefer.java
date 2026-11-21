package de.htw.studyplanner.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
                PlanVerstoss verstoss = null;

                if (voraussetzung.getMinLp() != null) {
                    verstoss = pruefeLp(
                            geplanterEintrag,
                            voraussetzung,
                            planNachModul,
                            leistungNachModul);
                } else if (voraussetzung.getVorausgesetztesModul() != null) {
                    verstoss = pruefeVoraussetzung(
                            geplanterEintrag,
                            voraussetzung,
                            planNachModul,
                            leistungNachModul,
                            heute);
                }

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
            Map<String, Leistung> leistungNachModul,
            LocalDate heute) {

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

            if (vorausgesetzteLeistung != null
                    && vorausgesetzteLeistung.getStatus() == Status.NICHT_BESTANDEN) {
                if (istNachDurchgefallenNeuGeplant(
                        vorausgesetzteLeistung,
                        vorausgesetzterPlan)) {
                    return null;
                }
            } else {
                if (semesterService.istVorbei(vorausgesetzterPlan.getSemester(), heute)) {
                    return new PlanVerstoss(
                            PruefSchwere.HINWEIS,
                            geprueftesModul,
                            vorausgesetztesModul,
                            "Ergebnis ausstehend");
                }
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

    /**
     * Prüft eine LP-Voraussetzung (min_lp mit toleranz_lp).
     *
     * <p>Ab min_lp ist alles in Ordnung. Zwischen min_lp - toleranz_lp und min_lp - 1
     * gibt es eine Warnung (nur mit Zulassung durch den Prüfungsausschuss). Darunter
     * ist es ein Fehler, bei einer empfohlenen Voraussetzung nur eine Warnung.
     *
     * @return der Verstoß oder null, wenn die Voraussetzung erfüllt ist
     */
    private PlanVerstoss pruefeLp(
            PlanEintrag geplanterEintrag,
            Voraussetzung voraussetzung,
            Map<String, PlanEintrag> planNachModul,
            Map<String, Leistung> leistungNachModul) {

        String geprueftesModul = geplanterEintrag.getModul().getCode();
        int soll = voraussetzung.getMinLp();
                int ist = zaehleLp(
                geplanterEintrag,
                voraussetzung.isGleichesSemester(),
                planNachModul,
                leistungNachModul);

        if (ist >= soll) {
            return null;
        }

        String zahlen = geprueftesModul + " braucht " + soll
                + " LP, bis dahin sind " + ist + " LP erreicht oder geplant";

        if (ist >= soll - voraussetzung.getToleranzLp()) {
            return new PlanVerstoss(
                    PruefSchwere.WARNUNG,
                    geprueftesModul,
                    null,
                    zahlen + ", nur mit Zulassung durch den Prüfungsausschuss");
        }

        return new PlanVerstoss(
                schweregrad(voraussetzung.getArt()),
                geprueftesModul,
                null,
                zahlen);
    }

    /**
     * Zählt die LP, die bis zum Semester des geplanten Moduls erreicht oder geplant sind.
     *
     * <p>Gezählt werden alle bestandenen Module und alle geplanten Module, die laut
     * {@link #darfAlsGeplantZaehlen} zählen dürfen. Mit {@code gleicheSemesterErlaubt}
     * (E28, z. B. bei B65) zählen auch Module aus demselben Semester. Das geprüfte Modul
     * zählt nie mit. Ein durchgefallenes Modul zählt nur, wenn es nach dem Durchfallen
     * neu geplant wurde. Jedes Modul zählt höchstens einmal.
     */
    private int zaehleLp(
            PlanEintrag geplanterEintrag,
            boolean gleicheSemesterErlaubt,
            Map<String, PlanEintrag> planNachModul,
            Map<String, Leistung> leistungNachModul) {

        Set<String> gezaehlt = new HashSet<>();
        int summe = 0;

        for (Leistung leistung : leistungNachModul.values()) {
            if (leistung.getStatus() == Status.BESTANDEN) {
                gezaehlt.add(leistung.getModul().getCode());
                summe += leistung.getModul().getLp();
            }
        }

        String geprueftesModul = geplanterEintrag.getModul().getCode();

        for (PlanEintrag plan : planNachModul.values()) {
            String code = plan.getModul().getCode();

            if (gezaehlt.contains(code)
                    || code.equals(geprueftesModul)
                    || !darfAlsGeplantZaehlen(
                            plan, geplanterEintrag, gleicheSemesterErlaubt)) {
                continue;
            }

            Leistung leistung = leistungNachModul.get(code);
            boolean durchgefallen = leistung != null
                    && leistung.getStatus() == Status.NICHT_BESTANDEN
                    && !istNachDurchgefallenNeuGeplant(leistung, plan);

            if (!durchgefallen) {
                summe += plan.getModul().getLp();
            }
        }

        return summe;
    }

    private PruefSchwere schweregrad(VoraussetzungsArt art) {
        return art == VoraussetzungsArt.NOTWENDIG
                ? PruefSchwere.FEHLER
                : PruefSchwere.WARNUNG;
    }
}