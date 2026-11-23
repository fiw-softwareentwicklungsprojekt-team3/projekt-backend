package de.htw.studyplanner.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.OpenModuleDto;
import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.ProgressResponse;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.ModulArt;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.repository.VoraussetzungRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

/**
 * Fortschritts-Übersicht der angemeldeten Person (US-58).
 *
 * <p>Es zählen nur Leistungen, nicht der Plan. Ein Pflichtmodul ist gesperrt, wenn eine
 * notwendige Voraussetzung nicht bestanden ist oder die LP unter {@code min_lp − toleranz_lp}
 * liegen. Im Toleranzbereich (z. B. 141–150 LP bei der Bachelorarbeit) ist es frei, weil eine
 * Zulassung durch den Prüfungsausschuss möglich ist. Die einzelne Voraussetzung prüft der
 * {@link PlanPruefer}, damit Plan und Fortschritt dieselben Regeln haben. Die Zulassung zur
 * Bachelorarbeit bestimmt der {@link BachelorarbeitPruefer}, genau wie in der Statistik.
 */
@Service
public class ProgressService {

    /** Sortierung der offenen Module: nach Fachsemester, dann nach Code. */
    private static final Comparator<Modul> STUDIENPLAN_REIHENFOLGE =
            Comparator.comparingInt(Modul::getFachsemester).thenComparing(Modul::getCode);

    private final StudierendeRepository studierendeRepository;
    private final LeistungRepository leistungRepository;
    private final ModulRepository modulRepository;
    private final VoraussetzungRepository voraussetzungRepository;
    private final PlanPruefer planPruefer;
    private final BachelorarbeitPruefer bachelorarbeitPruefer;

    /**
     * Erstellt den Service.
     *
     * @param studierendeRepository   Zugriff auf die Tabelle studierende
     * @param leistungRepository      Zugriff auf die Leistungen
     * @param modulRepository         Zugriff auf den Modulkatalog
     * @param voraussetzungRepository Zugriff auf die Voraussetzungen
     * @param planPruefer             prüft eine einzelne Voraussetzung
     * @param bachelorarbeitPruefer   bestimmt die Stufe der Zulassung zur Bachelorarbeit
     */
    public ProgressService(StudierendeRepository studierendeRepository,
                           LeistungRepository leistungRepository,
                           ModulRepository modulRepository,
                           VoraussetzungRepository voraussetzungRepository,
                           PlanPruefer planPruefer,
                           BachelorarbeitPruefer bachelorarbeitPruefer) {
        this.studierendeRepository = studierendeRepository;
        this.leistungRepository = leistungRepository;
        this.modulRepository = modulRepository;
        this.voraussetzungRepository = voraussetzungRepository;
        this.planPruefer = planPruefer;
        this.bachelorarbeitPruefer = bachelorarbeitPruefer;
    }

    /**
     * Berechnet den Fortschritt der angemeldeten Person.
     *
     * @return LP, Prozent, Notenschnitt, Stufe der Zulassung zur Bachelorarbeit und offene Pflichtmodule
     * @throws UnauthorizedException wenn es das Konto aus dem Token nicht mehr gibt
     */
    @Transactional(readOnly = true)
    public ProgressResponse fortschritt() {
        Studierende studierende = aktuelleStudierende();

        // Module zuerst laden: Danach liegen sie im Persistenzkontext, und
        // leistung.getModul() bzw. voraussetzung.getModul() brauchen keine weiteren Abfragen.
        List<Modul> module = modulRepository.findAll();
        Map<String, Leistung> leistungNachModul = leistungRepository.findByStudierende(studierende).stream()
                .collect(Collectors.toMap(l -> l.getModul().getCode(), Function.identity()));
        // Nach ID sortiert, damit die Sperrgründe immer in derselben Reihenfolge kommen.
        Map<String, List<Voraussetzung>> notwendigNachModul = voraussetzungRepository.findAll(Sort.by("id")).stream()
                .filter(v -> v.getArt() == VoraussetzungsArt.NOTWENDIG)
                .collect(Collectors.groupingBy(v -> v.getModul().getCode()));

        int earnedLp = LpRechner.bestandeneLp(leistungNachModul.values());

        List<OpenModuleDto> offen = module.stream()
                .filter(m -> m.getArt() == ModulArt.P)
                .filter(m -> !bestanden(m.getCode(), leistungNachModul))
                .sorted(STUDIENPLAN_REIHENFOLGE)
                .map(m -> zuOffenemModul(m, sperrgruende(m.getCode(), notwendigNachModul, leistungNachModul, earnedLp),
                        leistungNachModul.get(m.getCode())))
                .toList();

        BachelorarbeitPruefer.Stufe bachelorarbeit = bachelorarbeitPruefer.stufe(
                notwendigNachModul.getOrDefault(BachelorarbeitPruefer.BACHELORARBEIT, List.of()),
                leistungNachModul);

        return new ProgressResponse(
                earnedLp,
                LpRechner.GESAMT_LP,
                LpRechner.fortschrittProzent(earnedLp),
                notenschnitt(leistungNachModul.values()),
                bachelorarbeit == BachelorarbeitPruefer.Stufe.REGULAER,
                bachelorarbeit == BachelorarbeitPruefer.Stufe.MIT_ZULASSUNG,
                offen);
    }

    /**
     * LP-gewichteter Schnitt aller bestandenen Leistungen mit Note, auf eine Nachkommastelle
     * abgeschnitten (RStPO). Unbenotete Module wie B43 haben keine Note und zählen nicht mit.
     *
     * @param leistungen Leistungen der Person
     * @return der Schnitt, oder {@code null}, wenn es noch keine Note gibt
     */
    static BigDecimal notenschnitt(Iterable<Leistung> leistungen) {
        BigDecimal summe = BigDecimal.ZERO;
        int lp = 0;
        for (Leistung leistung : leistungen) {
            if (LpRechner.istBestanden(leistung) && leistung.getModul().isBenotet() && leistung.getNote() != null) {
                int modulLp = leistung.getModul().getLp();
                summe = summe.add(leistung.getNote().multiply(BigDecimal.valueOf(modulLp)));
                lp += modulLp;
            }
        }
        if (lp == 0) {
            return null;
        }
        return summe.divide(BigDecimal.valueOf(lp), 1, RoundingMode.DOWN);
    }

    /**
     * Sammelt die Gründe, warum ein Modul gesperrt ist. Gezählt werden nur Leistungen,
     * deshalb bekommt der {@link PlanPruefer} einen leeren Plan. Nur ein Fehler sperrt;
     * eine Warnung (LP im Toleranzbereich) lässt das Modul frei.
     *
     * @return die Gründe, leer wenn das Modul frei ist
     */
    private List<String> sperrgruende(String modulCode,
                                      Map<String, List<Voraussetzung>> notwendigNachModul,
                                      Map<String, Leistung> leistungNachModul,
                                      int earnedLp) {
        List<String> gruende = new ArrayList<>();
        LocalDate heute = LocalDate.now();
        for (Voraussetzung voraussetzung : notwendigNachModul.getOrDefault(modulCode, List.of())) {
            PlanVerstoss verstoss = planPruefer.pruefeVoraussetzung(
                    modulCode, null, voraussetzung, Map.of(), leistungNachModul, heute);
            if (verstoss != null && verstoss.schwere() == PruefSchwere.FEHLER) {
                gruende.add(grund(voraussetzung, earnedLp));
            }
        }
        return gruende;
    }

    /** Text für die Studierenden, z. B. „Programmierung 1 fehlt noch“. */
    private static String grund(Voraussetzung voraussetzung, int earnedLp) {
        if (voraussetzung.getVorausgesetztesModul() != null) {
            return voraussetzung.getVorausgesetztesModul().getBezeichnung() + " fehlt noch";
        }
        int minLp = voraussetzung.getMinLp();
        int toleranz = voraussetzung.getToleranzLp();
        if (toleranz == 0) {
            return "Mindestens " + minLp + " LP nötig, du hast " + earnedLp + " LP";
        }
        return "Mindestens " + (minLp - toleranz) + " LP nötig (" + minLp
                + " LP, ab " + (minLp - toleranz) + " LP mit Zulassung durch den Prüfungsausschuss), du hast "
                + earnedLp + " LP";
    }

    /** Baut den Eintrag für ein offenes Pflichtmodul. */
    private static OpenModuleDto zuOffenemModul(Modul modul, List<String> gruende, Leistung leistung) {
        return new OpenModuleDto(
                modul.getCode(),
                modul.getBezeichnung(),
                modul.getFachsemester(),
                modul.getLp(),
                leistung == null ? null : leistung.getStatus(),
                !gruende.isEmpty(),
                gruende);
    }

    /** Hat die Person das Modul bestanden? */
    private static boolean bestanden(String modulCode, Map<String, Leistung> leistungNachModul) {
        Leistung leistung = leistungNachModul.get(modulCode);
        return leistung != null && LpRechner.istBestanden(leistung);
    }

    /** Die angemeldete Person aus dem Token; 401, wenn das Konto nicht mehr existiert. */
    private Studierende aktuelleStudierende() {
        return studierendeRepository.findById(AktuellePerson.id())
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));
    }
}
