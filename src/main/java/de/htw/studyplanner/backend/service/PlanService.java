package de.htw.studyplanner.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.PlanEntryRequest;
import de.htw.studyplanner.backend.dto.PlanEntryResponse;
import de.htw.studyplanner.backend.exception.ConflictException;
import de.htw.studyplanner.backend.exception.FieldValidationException;
import de.htw.studyplanner.backend.exception.NotFoundException;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Turnus;
import de.htw.studyplanner.backend.model.Wahlangebot;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.repository.WahlangebotRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

/**
 * Fachlogik für den Studienplan einer Studierenden: Module für ein Semester einplanen.
 *
 * <p>Die Person kommt immer aus dem Token ({@link AktuellePerson#id()}).
 * Voraussetzungen blockieren das Speichern nicht (E2), wohl aber das Startsemester (E31)
 * und der Turnus des Moduls (E32).
 */
@Service
public class PlanService {

    private final PlanEintragRepository planEintragRepository;
    private final StudierendeRepository studierendeRepository;
    private final ModulRepository modulRepository;
    private final WahlangebotRepository wahlangebotRepository;
    private final SemesterService semesterService;

    /**
     * Erstellt den Service.
     *
     * @param planEintragRepository Zugriff auf die Tabelle plan_eintrag
     * @param studierendeRepository Zugriff auf die Tabelle studierende (Startsemester)
     * @param modulRepository       Zugriff auf den Modulkatalog
     * @param wahlangebotRepository Zugriff auf die Wahlangebote
     * @param semesterService       liest, vergleicht und legt Semester an
     */
    public PlanService(PlanEintragRepository planEintragRepository,
                       StudierendeRepository studierendeRepository,
                       ModulRepository modulRepository,
                       WahlangebotRepository wahlangebotRepository,
                       SemesterService semesterService) {
        this.planEintragRepository = planEintragRepository;
        this.studierendeRepository = studierendeRepository;
        this.modulRepository = modulRepository;
        this.wahlangebotRepository = wahlangebotRepository;
        this.semesterService = semesterService;
    }

    /**
     * Plant ein Modul für ein Semester ein und speichert den Eintrag.
     *
     * @param request Modulcode, Semester und optionale Auswahl-Angaben
     * @return der gespeicherte Planeintrag inklusive {@code id}
     * @throws NotFoundException              wenn es das Modul nicht gibt (404)
     * @throws ConflictException              wenn das Modul bereits im Plan steht (409)
     * @throws InvalidSemesterFormatException wenn das Semesterformat ungültig ist (400)
     * @throws FieldValidationException       wenn eine fachliche Regel verletzt ist (400)
     */
    @Transactional
    public PlanEntryResponse eintragAnlegen(PlanEntryRequest request) {
        Studierende studierende = studierendeRepository.findById(AktuellePerson.id())
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        Modul modul = modulRepository.findById(request.moduleCode())
                .orElseThrow(() -> new NotFoundException(
                        "Modul mit dem Code " + request.moduleCode() + " wurde nicht gefunden."));

        Semester gelesen = semesterService.parse(request.semester());
        Wahlangebot wahlangebot = pruefeEintrag(studierende, modul, gelesen, request);

        Semester semester = semesterService.findenOderNeuAnlegen(gelesen.getJahr(), gelesen.getArt());

        PlanEintrag eintrag = new PlanEintrag(studierende, modul, semester);
        eintrag.setWahlangebot(wahlangebot);
        eintrag.setKursCode(leer(request.courseCode()) ? null : request.courseCode().trim());
        eintrag.setKursTitel(leer(request.courseTitle()) ? null : request.courseTitle().trim());

        PlanEintrag gespeichert = planEintragRepository.save(eintrag);
        return zuAntwort(gespeichert);
    }

    /**
     * Prüft alle fachlichen Regeln für das Einplanen eines Moduls:
     * <ul>
     *   <li>Modul darf noch nicht im Plan stehen (sonst 409 mit Hinweis aufs Verschieben)</li>
     *   <li>Semester darf nicht vor dem Startsemester liegen (E31)</li>
     *   <li>Turnus des Moduls muss zur Art des Plansemesters passen (E32)</li>
     *   <li>Bei {@code mit_auswahl = false} (feste Pflichtmodule wie B11, B33, B34, deren
     *       Titel bereits in {@code modul.bezeichnung} steht) müssen {@code offerCode},
     *       {@code courseCode} und {@code courseTitle} leer ({@code null}) bleiben (E10)</li>
     *   <li>Nur bei {@code mit_auswahl = true} (B16, B26, B41, B54, B63, B64) dürfen
     *       {@code offerCode}, {@code courseCode} und {@code courseTitle} optional angegeben
     *       werden; ein angegebenes {@code offerCode} muss in {@code wahlangebot} existieren</li>
     * </ul>
     */
    private Wahlangebot pruefeEintrag(Studierende studierende, Modul modul,
                                      Semester gelesen, PlanEntryRequest request) {
        if (planEintragRepository.existsByStudierendeAndModul(studierende, modul)) {
            throw new ConflictException(
                    "Das Modul " + modul.getCode() + " ist bereits im Plan eingeplant. "
                            + "Bitte nutze das Verschieben, um das Semester zu ändern.");
        }

        Semester startsemester = studierende.getStartsemester();
        if (semesterService.istFrueher(gelesen, startsemester)) {
            throw new FieldValidationException("semester",
                    "Das Semester liegt vor deinem Startsemester " + startsemester.label() + ".");
        }

        if (modul.getTurnus() == Turnus.WS && gelesen.getArt() != SemesterArt.WS) {
            throw new FieldValidationException("semester",
                    modul.getCode() + " wird nur im Wintersemester angeboten, geplant ist " + gelesen.label() + ".");
        }
        if (modul.getTurnus() == Turnus.SS && gelesen.getArt() != SemesterArt.SS) {
            throw new FieldValidationException("semester",
                    modul.getCode() + " wird nur im Sommersemester angeboten, geplant ist " + gelesen.label() + ".");
        }

        // Auswahl-Prüfung (E10):
        // Normale Pflichtmodule (z. B. B11, B33, B34) haben mit_auswahl = false. Ihr Modultitel
        // steht bereits fest in modul.bezeichnung – offerCode, courseCode und courseTitle sind
        // nur für die konkrete Kurswahl innerhalb der 6 Auswahl-Module (B16, B26, B41, B54, B63, B64)
        // gedacht und müssen bei allen anderen Modulen null sein.
        if (!modul.isMitAuswahl()) {
            if (!leer(request.offerCode())) {
                throw new FieldValidationException("offerCode",
                        "Für das Modul " + modul.getCode() + " gibt es keine Auswahl.");
            }
            if (!leer(request.courseCode())) {
                throw new FieldValidationException("courseCode",
                        "Für das Modul " + modul.getCode() + " gibt es keine Auswahl.");
            }
            if (!leer(request.courseTitle())) {
                throw new FieldValidationException("courseTitle",
                        "Für das Modul " + modul.getCode() + " gibt es keine Auswahl.");
            }
            return null;
        }

        // Bei Auswahl-Modulen (mit_auswahl = true: B16, B26, B41, B54, B63, B64) sind
        // offerCode, courseCode und courseTitle beim Planen optional. Wird ein offerCode
        // übergeben (z. B. WP1..WP15 oder EN_B2.1), muss er in wahlangebot existieren.
        if (!leer(request.offerCode())) {
            return wahlangebotRepository.findById(request.offerCode().trim())
                    .orElseThrow(() -> new FieldValidationException("offerCode",
                            "Das Wahlangebot " + request.offerCode() + " gibt es nicht."));
        }

        return null;
    }

    private PlanEntryResponse zuAntwort(PlanEintrag eintrag) {
        return new PlanEntryResponse(
                eintrag.getPlanEintragId(),
                eintrag.getModul().getCode(),
                eintrag.getSemester().label(),
                eintrag.getWahlangebot() == null ? null : eintrag.getWahlangebot().getCode(),
                eintrag.getKursCode(),
                eintrag.getKursTitel());
    }

    private static boolean leer(String text) {
        return text == null || text.isBlank();
    }
}
