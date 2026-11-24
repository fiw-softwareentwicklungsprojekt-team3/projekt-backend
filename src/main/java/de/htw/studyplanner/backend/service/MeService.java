package de.htw.studyplanner.backend.service;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.MeResponse;
import de.htw.studyplanner.backend.dto.UpdateMeRequest;
import de.htw.studyplanner.backend.exception.FieldValidationException;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

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
    private final LeistungRepository leistungRepository;

    /**
     * Erstellt den Service.
     *
     * @param nutzendeRepository    Zugriff auf die Tabelle nutzende
     * @param studierendeRepository Zugriff auf die Tabelle studierende (Startsemester)
     * @param semesterService       Hilfsdienst zum Parsen und Anlegen von Semestern
     * @param planEintragRepository Zugriff auf Planeinträge (für Konflikterkennung)
     * @param leistungRepository    Zugriff auf Leistungen (für Konflikterkennung)
     */
    public MeService(NutzendeRepository nutzendeRepository,
                     StudierendeRepository studierendeRepository,
                     SemesterService semesterService,
                     PlanEintragRepository planEintragRepository,
                     LeistungRepository leistungRepository) {
        this.nutzendeRepository = nutzendeRepository;
        this.studierendeRepository = studierendeRepository;
        this.semesterService = semesterService;
        this.planEintragRepository = planEintragRepository;
        this.leistungRepository = leistungRepository;
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
        Nutzende nutzende = ladeNutzende(id);
        Studierende studierende = nutzende.getRolle() == Rolle.STUDIERENDE
                ? studierendeRepository.findById(id).orElse(null)
                : null;
        return baueResponse(nutzende, studierende);
    }

    /**
     * Ändert das Startsemester der angemeldeten Studierenden.
     *
     * <p>Nur Studierende können ihr Startsemester ändern.
     *
     * <p>Das neue Startsemester darf nicht nach dem frühesten Planeintrag oder der
     * frühesten Leistung liegen, damit keine Einträge „vor dem Studium“ entstehen (E31).
     * Anerkannte Leistungen ohne Semester ({@code semester == null}) werden ignoriert.
     *
     * @param request neues Startsemester im Format "WS JJJJ"
     * @return aktualisierte Kontodaten
     * @throws UnauthorizedException    wenn die Person nicht mehr existiert
     * @throws FieldValidationException wenn die Rolle keine Änderung erlaubt oder
     *                                  Planeinträge bzw. Leistungen das neue Semester verbieten
     */
    @Transactional
    public MeResponse startsemesterAendern(UpdateMeRequest request) {
        Integer id = AktuellePerson.id();
        Nutzende nutzende = ladeNutzende(id);

        if (nutzende.getRolle() != Rolle.STUDIERENDE) {
            throw new FieldValidationException("startSemester",
                    "Nur Studierende können ihr Startsemester ändern.");
        }

        Studierende studierende = studierendeRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        // Semester zunächst nur parsen, noch nicht in der DB anlegen.
        Semester geparst = semesterService.parse(request.startSemester(), "startSemester");

        // Prüfen: Gibt es Planeinträge, die vor dem neuen Startsemester liegen?
        Optional<Semester> fruehesterPlanEintrag = planEintragRepository
                .findPlanMitDetails(id)
                .stream()
                .map(PlanEintrag::getSemester)
                .min(Comparator.naturalOrder());

        if (fruehesterPlanEintrag.isPresent()
                && semesterService.istFrueher(fruehesterPlanEintrag.get(), geparst)) {
            throw new FieldValidationException("startSemester",
                    "Das neue Startsemester darf nicht nach einem vorhandenen Planeintrag ("
                    + fruehesterPlanEintrag.get().label() + ") liegen.");
        }

        // Prüfen: Gibt es Leistungen mit Semester, die vor dem neuen Startsemester liegen?
        Optional<Semester> fruehesteLeistung = leistungRepository
                .findByStudierende(studierende)
                .stream()
                .map(Leistung::getSemester)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder());

        if (fruehesteLeistung.isPresent()
                && semesterService.istFrueher(fruehesteLeistung.get(), geparst)) {
            throw new FieldValidationException("startSemester",
                    "Das neue Startsemester darf nicht nach einer vorhandenen Leistung (Leistung in "
                    + fruehesteLeistung.get().label() + ") liegen.");
        }

        // Erst nach erfolgreicher Prüfung in der Datenbank anlegen und speichern.
        Semester neuesSemester = semesterService.findenOderNeuAnlegen(geparst.getJahr(), SemesterArt.WS);
        studierende.setStartsemester(neuesSemester);
        studierendeRepository.save(studierende);

        return baueResponse(nutzende, studierende);
    }

    private Nutzende ladeNutzende(Integer id) {
        return nutzendeRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));
    }

    private MeResponse baueResponse(Nutzende nutzende, Studierende studierende) {
        String startSemester = studierende != null
                ? studierende.getStartsemester().label()
                : null;
        return new MeResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name(),
                startSemester);
    }
}
