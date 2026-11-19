package de.htw.studyplanner.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.PlanEntryPlanResponse;
import de.htw.studyplanner.backend.dto.PlanViolationResponse;
import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;
import de.htw.studyplanner.backend.exception.UnauthorizedException;

/**
 * Liefert den Studienplan mit aktuell berechneten Verstößen.
 */
@Service
public class PlanQueryService {

    private final PlanEintragRepository planEintragRepository;
    private final StudierendeRepository studierendeRepository;
    private final PlanPruefer planPruefer;

    public PlanQueryService(
            PlanEintragRepository planEintragRepository,
            StudierendeRepository studierendeRepository,
            PlanPruefer planPruefer) {
        this.planEintragRepository = planEintragRepository;
        this.studierendeRepository = studierendeRepository;
        this.planPruefer = planPruefer;
    }

    /**
     * Liefert den Plan der angemeldeten Person mit aktuellen Verst��en.
     *
     * @return Planeintr�ge mit Voraussetzungen und Verst��en
     */
    @Transactional(readOnly = true)
    public List<PlanEntryPlanResponse> eigenerPlan() {
        Studierende studierende = studierendeRepository.findById(AktuellePerson.id())
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        List<PlanEintrag> eintraege =
                planEintragRepository.findByStudierende(studierende);

        List<PlanVerstoss> verstoesse =
                planPruefer.pruefe(studierende, LocalDate.now());

        Map<String, List<PlanVerstoss>> verstoesseNachModul =
                verstoesse.stream()
                        .collect(Collectors.groupingBy(
                                PlanVerstoss::geprueftesModul));

        return eintraege.stream()
                .map(eintrag -> antwort(
                        eintrag,
                        verstoesseNachModul.getOrDefault(
                                eintrag.getModul().getCode(), List.of())))
                .toList();
    }

    private PlanEntryPlanResponse antwort(
            PlanEintrag eintrag,
            List<PlanVerstoss> verstoesse) {

        List<PlanViolationResponse> violations = verstoesse.stream()
                .map(verstoss -> new PlanViolationResponse(
                        verstoss.schwere(),
                        verstoss.fehlendeVoraussetzung(),
                        verstoss.meldung()))
                .toList();

        boolean prerequisitesMet = verstoesse.stream()
                .noneMatch(verstoss ->
                        verstoss.schwere().name().equals("FEHLER"));

        return new PlanEntryPlanResponse(
                eintrag.getPlanEintragId(),
                eintrag.getModul().getCode(),
                eintrag.getSemester().label(),
                eintrag.getWahlangebot() == null
                        ? null
                        : eintrag.getWahlangebot().getCode(),
                eintrag.getKursCode(),
                eintrag.getKursTitel(),
                prerequisitesMet,
                violations);
    }
}
