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
  * Fachlogik für den Studienplan einer Studierenden: Module einplanen und verschieben.
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
    private final OfferService offerService;

    /**
     * Erstellt den Service.
     *
     * @param planEintragRepository Zugriff auf die Tabelle plan_eintrag
     * @param studierendeRepository Zugriff auf die Tabelle studierende (Startsemester)
     * @param modulRepository       Zugriff auf den Modulkatalog
     * @param wahlangebotRepository Zugriff auf die Wahlangebote
     * @param semesterService       liest, vergleicht und legt Semester an
     * @param offerService          weiß, welche Wahlangebote zu welchem Modul passen
     */
    public PlanService(PlanEintragRepository planEintragRepository,
                       StudierendeRepository studierendeRepository,
                       ModulRepository modulRepository,
                       WahlangebotRepository wahlangebotRepository,
                       SemesterService semesterService,
                       OfferService offerService) {
        this.planEintragRepository = planEintragRepository;
        this.studierendeRepository = studierendeRepository;
        this.modulRepository = modulRepository;
        this.wahlangebotRepository = wahlangebotRepository;
        this.semesterService = semesterService;
        this.offerService = offerService;
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
        Studierende studierende = aktuelleStudierende();

        Modul modul = modulRepository.findById(request.moduleCode())
                .orElseThrow(() -> new NotFoundException(
                        "Modul mit dem Code " + request.moduleCode() + " wurde nicht gefunden."));

        if (planEintragRepository.existsByStudierendeAndModul(studierende, modul)) {
            throw new ConflictException(
                    "Das Modul " + modul.getCode() + " ist bereits im Plan eingeplant. "
                            + "Bitte nutze das Verschieben, um das Semester zu ändern.");
        }

        Semester gelesen = semesterService.parse(request.semester());
        Wahlangebot wahlangebot = pruefeEintrag(studierende, modul, gelesen, request);

        PlanEintrag eintrag = new PlanEintrag(studierende, modul,
                semesterService.findenOderNeuAnlegen(gelesen.getJahr(), gelesen.getArt()));
        uebernehmeAuswahl(eintrag, wahlangebot, request);

        PlanEintrag gespeichert = planEintragRepository.save(eintrag);
        return zuAntwort(gespeichert);
    }

       /**
     * Ändert Semester und/oder Auswahl eines eigenen Planeintrags (US-39).
     *
     * <p>Es gelten dieselben Regeln wie beim Einplanen, geprüft über dieselbe Methode
     * ({@code pruefeEintrag}). Das Modul selbst lässt sich nicht ändern; dafür den Eintrag
     * löschen und das neue Modul einplanen.
     *
     * @throws NotFoundException        wenn der Eintrag nicht existiert oder einer anderen Person gehört
     * @throws FieldValidationException wenn der Modulcode abweicht oder eine Regel verletzt ist (400)
     */
    @Transactional
    public PlanEntryResponse eintragAendern(Integer id, PlanEntryRequest request) {
        Studierende studierende = aktuelleStudierende();
        PlanEintrag eintrag = eigenerEintrag(id, studierende);
        Modul modul = eintrag.getModul();

        if (!modul.getCode().equals(request.moduleCode().trim())) {
            throw new FieldValidationException("moduleCode",
                    "Das Modul eines Eintrags kann nicht geändert werden. "
                            + "Bitte lösche den Eintrag und plane " + request.moduleCode().trim() + " neu ein.");
        }

        Semester gelesen = semesterService.parse(request.semester());
        Wahlangebot wahlangebot = pruefeEintrag(studierende, modul, gelesen, request);

        eintrag.setSemester(semesterService.findenOderNeuAnlegen(gelesen.getJahr(), gelesen.getArt()));
        uebernehmeAuswahl(eintrag, wahlangebot, request);

        return zuAntwort(planEintragRepository.save(eintrag));
    } 

        /**
     * Entfernt einen eigenen Planeintrag (US-39).
     *
     * <p>Abhängige Einträge bleiben stehen: Wird z. B. B16 gelöscht, bleibt B26 im Plan.
     * Die Regeln werden erst beim nächsten Ändern wieder geprüft.
     */
    @Transactional
    public void eintragLoeschen(Integer id) {
        PlanEintrag eintrag = eigenerEintrag(id, aktuelleStudierende());
        planEintragRepository.delete(eintrag);
    }

    private Studierende aktuelleStudierende() {
        return studierendeRepository.findById(AktuellePerson.id())
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));
    }

    private static void uebernehmeAuswahl(PlanEintrag eintrag, Wahlangebot wahlangebot,
                                          PlanEntryRequest request) {
        eintrag.setWahlangebot(wahlangebot);
        eintrag.setKursCode(leer(request.courseCode()) ? null : request.courseCode().trim());
        eintrag.setKursTitel(leer(request.courseTitle()) ? null : request.courseTitle().trim());
    }

        /**
     * Lädt einen Eintrag der angemeldeten Person. Fremde und nicht vorhandene Einträge ergeben
     * dieselbe 404-Meldung, damit niemand herausfinden kann, welche IDs es gibt (D3).
     */
    private PlanEintrag eigenerEintrag(Integer id, Studierende studierende) {
        return planEintragRepository.findByPlanEintragIdAndStudierende(id, studierende)
                .orElseThrow(() -> new NotFoundException(
                        "Planeintrag mit der ID " + id + " wurde nicht gefunden."));
    }

    /**
     * Prüft die fachlichen Regeln für einen Eintrag, beim Einplanen und beim Ändern gleich:
     * <ul>
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
        // übergeben, muss er existieren und zum Modul passen. Ob er passt, entscheidet
        // der OfferService, also dieselbe Regel wie im Modulkatalog und bei den Leistungen.
        if (!leer(request.offerCode())) {
            if (!offerService.hatFesteAuswahl(modul)) {
                // B63/B64: freie Wahl, keine Angebotsliste, nur eigene Kursnummer
                throw new FieldValidationException("offerCode",
                        "Für das Modul " + modul.getCode()
                                + " gibt es keine Angebotsliste, nur eine eigene Kursnummer.");
            }
            Wahlangebot wahlangebot = wahlangebotRepository.findById(request.offerCode().trim())
                    .orElseThrow(() -> new FieldValidationException("offerCode",
                            "Das Wahlangebot " + request.offerCode() + " gibt es nicht."));
            if (!offerService.passtZumModul(modul, wahlangebot)) {
                throw new FieldValidationException("offerCode",
                        "Das Wahlangebot " + wahlangebot.getCode() + " passt nicht zu Modul "
                                + modul.getCode() + ".");
            }
            return wahlangebot;
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
