package de.htw.studyplanner.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.PlanEntryDto;
import de.htw.studyplanner.backend.dto.PlanResponse;
import de.htw.studyplanner.backend.dto.PlanSemesterDto;
import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.PlanViolationResponse;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Wahlangebot;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

/**
 * Lesende Seite des Studienplans: liefert {@code GET /api/plan} (US-38, US-60, E30).
 *
 * <p>Schreibende Operationen stehen in {@link PlanService}. Die Person kommt immer aus
 * dem Token ({@link AktuellePerson#id()}).
 */
@Service
public class PlanQueryService {

    private final PlanEintragRepository planEintragRepository;
    private final StudierendeRepository studierendeRepository;
    private final PlanPruefer planPruefer;
    private final SemesterService semesterService;
    private final int maxLpPerSemester;

    /**
     * Erstellt den Service.
     *
     * @param planEintragRepository Zugriff auf die Tabelle plan_eintrag
     * @param studierendeRepository Zugriff auf die Tabelle studierende (Startsemester)
     * @param planPruefer           berechnet die Verstöße gegen Voraussetzungen
     * @param semesterService       vergleicht Semester zeitlich
     * @param maxLpPerSemester      konfiguriertes LP-Limit pro Semester (Standard: 30)
     */
    public PlanQueryService(
            PlanEintragRepository planEintragRepository,
            StudierendeRepository studierendeRepository,
            PlanPruefer planPruefer,
            SemesterService semesterService,
            @Value("${studyplanner.plan.max-lp-per-semester:30}") int maxLpPerSemester) {
        this.planEintragRepository = planEintragRepository;
        this.studierendeRepository = studierendeRepository;
        this.planPruefer = planPruefer;
        this.semesterService = semesterService;
        this.maxLpPerSemester = maxLpPerSemester;
    }

    /**
     * Liefert den ganzen Plan der angemeldeten Person, nach Semestern gruppiert,
     * mit den aktuell berechneten Verstößen je Eintrag.
     *
     * <ul>
     *   <li>Semester zeitlich sortiert über {@link Semester#compareTo(Semester)},
     *       innerhalb eines Semesters nach Modulcode</li>
     *   <li>Nur Semester mit Einträgen; Lücken füllt das Frontend</li>
     *   <li>LP- und SWS-Summen sowie Verstöße werden hier berechnet, nicht gespeichert (E30, US-60)</li>
     *   <li>{@code lpLimitExceeded} ist {@code true}, wenn die Semester-LP das Limit übersteigen</li>
     *   <li>{@code prerequisitesMet} ist {@code false}, sobald ein {@code FEHLER} vorliegt;
     *       {@code WARNUNG}en ändern daran nichts</li>
     *   <li>Leerer Plan: {@code semesters = []} und {@code totalLp = 0}</li>
     * </ul>
     *
     * <p>Die Einträge werden mit Modul, Semester und Wahlangebot in einer Abfrage geladen
     * (JOIN FETCH), damit weder die Antwort noch die Planprüfung N+1-Abfragen auslöst.
     *
     * @return der Plan der angemeldeten Person
     */
    @Transactional(readOnly = true)
    public PlanResponse eigenerPlan() {
        Integer personId = AktuellePerson.id();
        Studierende studierende = studierendeRepository.findMitStartsemester(personId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        // Zuerst laden: So liegen Modul, Semester und Wahlangebot schon im
        // Persistenzkontext, wenn der PlanPruefer den Plan erneut liest.
        List<PlanEintrag> eintraege = planEintragRepository.findPlanMitDetails(personId);

        Map<String, List<PlanVerstoss>> verstoesseNachModul =
                planPruefer.pruefe(studierende, LocalDate.now()).stream()
                        .collect(Collectors.groupingBy(PlanVerstoss::geprueftesModul));

        Map<Semester, List<PlanEintrag>> nachSemester = new TreeMap<>(semesterService::vergleiche);
        for (PlanEintrag eintrag : eintraege) {
            nachSemester.computeIfAbsent(eintrag.getSemester(), s -> new ArrayList<>()).add(eintrag);
        }

        List<PlanSemesterDto> semester = new ArrayList<>();
        int gesamtLp = 0;
        for (Map.Entry<Semester, List<PlanEintrag>> gruppe : nachSemester.entrySet()) {
            List<PlanEntryDto> dtos = gruppe.getValue().stream()
                    .sorted(Comparator.comparing(e -> e.getModul().getCode()))
                    .map(e -> zuPlanEintrag(e,
                            verstoesseNachModul.getOrDefault(e.getModul().getCode(), List.of())))
                    .toList();
            int lp = dtos.stream().mapToInt(PlanEntryDto::lp).sum();
            int sws = gruppe.getValue().stream().mapToInt(e -> modulSws(e.getModul())).sum();
            boolean lpLimitExceeded = lp > maxLpPerSemester;
            gesamtLp += lp;
            semester.add(new PlanSemesterDto(gruppe.getKey().label(), lp, sws, lpLimitExceeded, dtos));
        }

        return new PlanResponse(studierende.getStartsemester().label(), gesamtLp, maxLpPerSemester, semester);
    }

    /**
     * Berechnet die SWS eines Moduls aus Vorlesung und Übung.
     * Fehlende Vorlesungs-SWS (null) werden als 0 gewertet.
     */
    private static int modulSws(Modul modul) {
        Integer vorlesung = modul.getSwsVorlesung();
        int swsVorlesung = vorlesung != null ? vorlesung : 0;
        return swsVorlesung + modul.getSwsUebung();
    }

    /**
     * Baut aus einem Planeintrag und seinen Verstößen den Eintrag für die Antwort.
     *
     * @param eintrag    Planeintrag mit geladenem Modul und Wahlangebot
     * @param verstoesse Verstöße des PlanPruefers für genau dieses Modul, leer wenn keine
     * @return der Eintrag mit {@code prerequisitesMet} und {@code violations}
     */
    private static PlanEntryDto zuPlanEintrag(PlanEintrag eintrag, List<PlanVerstoss> verstoesse) {
        Modul modul = eintrag.getModul();
        Wahlangebot angebot = eintrag.getWahlangebot();

        List<PlanViolationResponse> violations = verstoesse.stream()
                .map(v -> new PlanViolationResponse(v.schwere(), v.fehlendeVoraussetzung(), v.meldung()))
                .toList();
        boolean prerequisitesMet = verstoesse.stream()
                .noneMatch(v -> v.schwere() == PruefSchwere.FEHLER);

        return new PlanEntryDto(
                eintrag.getPlanEintragId(),
                modul.getCode(),
                modul.getBezeichnung(),
                modul.getLp(),
                modul.getFachsemester(),
                angebot == null ? null : angebot.getCode(),
                angebot == null ? null : angebot.getBezeichnung(),
                eintrag.getKursCode(),
                eintrag.getKursTitel(),
                prerequisitesMet,
                violations);
    }
}
