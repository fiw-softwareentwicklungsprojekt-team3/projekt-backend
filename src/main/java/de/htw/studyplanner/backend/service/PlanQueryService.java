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
 * Liefert den Studienplan der angemeldeten Person.
 */
@Service
public class PlanQueryService {

    private final PlanEintragRepository planEintragRepository;
    private final StudierendeRepository studierendeRepository;
    private final PlanPruefer planPruefer;
    private final SemesterService semesterService;
    private final int maxLpPerSemester;

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
     * Liefert den vollständigen Studienplan nach Semestern gruppiert.
     *
     * @return Studienplan mit LP-, SWS- und Grenzwertinformationen
     */
    @Transactional(readOnly = true)
    public PlanResponse eigenerPlan() {
        Integer personId = AktuellePerson.id();

        Studierende studierende = studierendeRepository
                .findMitStartsemester(personId)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        List<PlanEintrag> eintraege =
                planEintragRepository.findPlanMitDetails(personId);

        Map<String, List<PlanVerstoss>> verstoesseNachModul =
                planPruefer.pruefe(studierende, LocalDate.now())
                        .stream()
                        .collect(Collectors.groupingBy(
                                PlanVerstoss::geprueftesModul));

        Map<Semester, List<PlanEintrag>> nachSemester =
                new TreeMap<>(semesterService::vergleiche);

        for (PlanEintrag eintrag : eintraege) {
            nachSemester
                    .computeIfAbsent(eintrag.getSemester(), ignored -> new ArrayList<>())
                    .add(eintrag);
        }

        List<PlanSemesterDto> semester = new ArrayList<>();
        int gesamtLp = 0;

        for (Map.Entry<Semester, List<PlanEintrag>> gruppe
                : nachSemester.entrySet()) {

            List<PlanEntryDto> dtos = gruppe.getValue()
                    .stream()
                    .sorted(Comparator.comparing(
                            eintrag -> eintrag.getModul().getCode()))
                    .map(eintrag -> zuPlanEintrag(
                            eintrag,
                            verstoesseNachModul.getOrDefault(
                                    eintrag.getModul().getCode(),
                                    List.of())))
                    .toList();

            int lp = dtos
                    .stream()
                    .mapToInt(PlanEntryDto::lp)
                    .sum();

            int sws = gruppe.getValue()
                    .stream()
                    .mapToInt(eintrag -> {
                        Integer vorlesungsSws =
                                eintrag.getModul().getSwsVorlesung();

                        int swsVorlesung =
                                vorlesungsSws == null ? 0 : vorlesungsSws;

                        return swsVorlesung
                                + eintrag.getModul().getSwsUebung();
                    })
                    .sum();

            boolean lpLimitExceeded = lp > maxLpPerSemester;

            gesamtLp += lp;

            semester.add(new PlanSemesterDto(
                    gruppe.getKey().label(),
                    lp,
                    sws,
                    lpLimitExceeded,
                    dtos));
        }

        return new PlanResponse(
                studierende.getStartsemester().label(),
                gesamtLp,
                maxLpPerSemester,
                semester);
    }

    private static PlanEntryDto zuPlanEintrag(
            PlanEintrag eintrag,
            List<PlanVerstoss> verstoesse) {

        Modul modul = eintrag.getModul();
        Wahlangebot angebot = eintrag.getWahlangebot();

        List<PlanViolationResponse> violations = verstoesse
                .stream()
                .map(verstoss -> new PlanViolationResponse(
                        verstoss.schwere(),
                        verstoss.fehlendeVoraussetzung(),
                        verstoss.meldung()))
                .toList();

        boolean prerequisitesMet = verstoesse
                .stream()
                .noneMatch(verstoss ->
                        verstoss.schwere() == PruefSchwere.FEHLER);

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