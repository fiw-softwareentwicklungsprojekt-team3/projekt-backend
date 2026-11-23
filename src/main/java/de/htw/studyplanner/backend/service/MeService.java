package de.htw.studyplanner.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.MeResponse;
import de.htw.studyplanner.backend.dto.UpdateMeRequest;
import de.htw.studyplanner.backend.exception.FieldValidationException;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

import java.util.Comparator;
import java.util.Optional;

/**
 * Liefert und aktualisiert die Kontodaten der Person, die die aktuelle Anfrage stellt.
 *
 * <p>Die ID kommt immer aus dem Token ({@link AktuellePerson#id()}) und nie aus
 * Pfad, Query oder Body. Sonst könnte jede angemeldete Person mit {@code ?id=5}
 * fremde Konten auslesen.
 *
 * <p>Gelesen wird trotzdem aus der Datenbank und nicht nur aus dem Token: Ein Token
 * ist bis zu einer Stunde gültig und kann eine Person beschreiben, die
 * inzwischen gelöscht wurde. Dieser Fall muss 401 ergeben und nicht 500.
 */
@Service
public class MeService {

    private final NutzendeRepository nutzendeRepository;
    private final StudierendeRepository studierendeRepository;
    private final SemesterService semesterService;
    private final PlanEintragRepository planEintragRepository;

    /**
     * Erstellt den Service.
     *
     * @param nutzendeRepository      Zugriff auf die Tabelle nutzende
     * @param studierendeRepository   Zugriff auf die Tabelle studierende (Startsemester)
     * @param semesterService         Hilfsdienst zum Parsen und Anlegen von Semestern
     * @param planEintragRepository   Zugriff auf Planeinträge (für Konflikterkennung)
     */
    public MeService(NutzendeRepository nutzendeRepository,
                     StudierendeRepository studierendeRepository,
                     SemesterService semesterService,
                     PlanEintragRepository planEintragRepository) {
        this.nutzendeRepository = nutzendeRepository;
        this.studierendeRepository = studierendeRepository;
        this.semesterService = semesterService;
        this.planEintragRepository = planEintragRepository;
    }

    /**
     * Liest die angemeldete Person aus der Datenbank und baut die Antwort.
     *
     * <p>{@code @Transactional(readOnly = true)} ist nötig, weil das Startsemester
     * eine Lazy-Beziehung ist: Ohne offene Transaktion würde
     * {@code getStartsemester().label()} fehlschlagen ({@code open-in-view} ist aus).
     *
     * @return ID, E-Mail, Rolle und (nur bei Studierenden) das Startsemester
     * @throws UnauthorizedException wenn es die Person aus dem Token nicht mehr gibt
     */
    @Transactional(readOnly = true)
    public MeResponse aktuellesKonto() {
        Integer id = AktuellePerson.id();

        var nutzende = nutzendeRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        String startSemester = null;
        if (nutzende.getRolle() == Rolle.STUDIERENDE) {
            startSemester = studierendeRepository.findById(id)
                    .map(studierende -> studierende.getStartsemester().label())
                    .orElse(null);
        }

        return new MeResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name(),
                startSemester);
    }

    /**
     * Ändert das Startsemester der angemeldeten Studierenden.
     *
     * <p>Nur Studierende können ihr Startsemester ändern.
     *
     * <p>Das neue Startsemester darf nicht nach dem frühesten Planeintrag liegen,
     * damit keine Einträge „vor dem Studium" entstehen.
     *
     * @param request neues Startsemester im Format "WS JJJJ"
     * @return aktualisierte Kontodaten
     * @throws UnauthorizedException    wenn die Person nicht mehr existiert
     * @throws FieldValidationException wenn die Rolle keine Änderung erlaubt oder
     *                                  Planeinträge das neue Semester verbieten
     */
    @Transactional
    public MeResponse startsemesterAendern(UpdateMeRequest request) {
        Integer id = AktuellePerson.id();

        var nutzende = nutzendeRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        if (nutzende.getRolle() != Rolle.STUDIERENDE) {
            throw new FieldValidationException("startSemester",
                    "Nur Studierende können ihr Startsemester ändern.");
        }

        // Semester parsen und ggf. anlegen (nur WS dank @Pattern im DTO).
        Semester geparst = semesterService.parse(request.startSemester(), "startSemester");
        Semester neuesSemester = semesterService.findenOderNeuAnlegen(geparst.getJahr(), SemesterArt.WS);

        // Prüfen: Gibt es Planeinträge, die vor dem neuen Startsemester liegen?
        Optional<Semester> fruehesterEintrag = planEintragRepository
                .findPlanMitDetails(id)
                .stream()
                .map(PlanEintrag::getSemester)
                .min(Comparator.naturalOrder());

        if (fruehesterEintrag.isPresent()
                && semesterService.istFrueher(fruehesterEintrag.get(), neuesSemester)) {
            throw new FieldValidationException("startSemester",
                    "Das neue Startsemester darf nicht nach einem vorhandenen Planeintrag ("
                    + fruehesterEintrag.get().label() + ") liegen.");
        }

        Studierende studierende = studierendeRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));
        studierende.setStartsemester(neuesSemester);
        studierendeRepository.save(studierende);

        return new MeResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name(),
                neuesSemester.label());
    }
}
