package de.htw.studyplanner.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.ResultRequest;
import de.htw.studyplanner.backend.dto.ResultResponse;
import de.htw.studyplanner.backend.exception.FieldValidationException;
import de.htw.studyplanner.backend.exception.NotFoundException;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.AngebotsGruppe;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.model.Wahlangebot;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.repository.WahlangebotRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

/**
 * Leistungen der angemeldeten Person lesen, eintragen, ändern und löschen.
 *
 * <p>Die Person kommt immer aus dem Token ({@link AktuellePerson#id()}). Der
 * {@code moduleCode} im Pfad bezeichnet nur das Modul, fremde Leistungen sind darüber
 * nicht erreichbar.
 *
 * <p>Pro Person und Modul gibt es genau eine Zeile in {@code leistung} (UNIQUE). Ein
 * neues Ergebnis, z. B. bei einer Wiederholung, ändert diese Zeile.
 *
 * <p>Alle Regeln werden hier geprüft, bevor gespeichert wird. Die Datenbank hat für
 * einige davon zusätzlich CHECKs, die würden aber nur einen 500 statt 400 liefern.
 */
@Service
public class ResultService {

    /** Erlaubte Noten nach RStPO §17 Abs. 1. */
    private static final List<BigDecimal> ERLAUBTE_NOTEN = List.of(
            new BigDecimal("1.0"), new BigDecimal("1.3"), new BigDecimal("1.7"),
            new BigDecimal("2.0"), new BigDecimal("2.3"), new BigDecimal("2.7"),
            new BigDecimal("3.0"), new BigDecimal("3.3"), new BigDecimal("3.7"),
            new BigDecimal("4.0"), new BigDecimal("5.0"));

    private static final BigDecimal BESTANDEN_MAX = new BigDecimal("4.0");
    private static final BigDecimal NICHT_BESTANDEN_NOTE = new BigDecimal("5.0");

    private static final int MIN_VERSUCH = 1;
    private static final int MAX_VERSUCH = 4;

    /** Module, bei denen ein Wahlangebot gewählt werden muss. */
    private static final Set<String> MODULE_MIT_WP_ANGEBOT = Set.of("B41", "B54");
    private static final Set<String> MODULE_MIT_SPRACHANGEBOT = Set.of("B16", "B26");

    /** AWE-Module ohne Angebotsliste: Veranstaltung wird frei eingetragen (E10). */
    private static final Set<String> AWE_MODULE_MIT_KURS = Set.of("B63", "B64");

    private final LeistungRepository leistungRepository;
    private final StudierendeRepository studierendeRepository;
    private final ModulRepository modulRepository;
    private final WahlangebotRepository wahlangebotRepository;
    private final SemesterService semesterService;

    /**
     * Erstellt den Service.
     *
     * @param leistungRepository    Zugriff auf die Tabelle leistung
     * @param studierendeRepository Zugriff auf die Tabelle studierende (Startsemester)
     * @param modulRepository       Zugriff auf den Modulkatalog
     * @param wahlangebotRepository Zugriff auf die Wahlangebote
     * @param semesterService       liest Semester-Strings und legt Semester bei Bedarf an
     */
    public ResultService(LeistungRepository leistungRepository,
                         StudierendeRepository studierendeRepository,
                         ModulRepository modulRepository,
                         WahlangebotRepository wahlangebotRepository,
                         SemesterService semesterService) {
        this.leistungRepository = leistungRepository;
        this.studierendeRepository = studierendeRepository;
        this.modulRepository = modulRepository;
        this.wahlangebotRepository = wahlangebotRepository;
        this.semesterService = semesterService;
    }

    /**
     * Liefert alle Leistungen der angemeldeten Person, sortiert nach Modulcode.
     *
     * @return die Leistungen, leer wenn es noch keine gibt
     */
    @Transactional(readOnly = true)
    public List<ResultResponse> eigeneLeistungen() {
        Studierende studierende = aktuelleStudierende();
        return leistungRepository.findByStudierende(studierende).stream()
                .sorted(Comparator.comparing(l -> l.getModul().getCode()))
                .map(this::zuAntwort)
                .toList();
    }

    /**
     * Legt die Leistung für ein Modul an oder ändert die bestehende.
     *
     * @param moduleCode Modulcode aus dem Pfad
     * @param request    das neue Ergebnis
     * @return die gespeicherte Leistung
     * @throws NotFoundException        wenn es das Modul nicht gibt
     * @throws FieldValidationException wenn eine Regel verletzt ist (400)
     */
    @Transactional
    public ResultResponse speichern(String moduleCode, ResultRequest request) {
        Studierende studierende = aktuelleStudierende();
        Modul modul = modulOderFehler(moduleCode);

        BigDecimal note = pruefeNote(modul, request.status(), request.grade());
        pruefeVersuch(request.status(), request.attempt());
        Semester semester = pruefeSemester(studierende, request.semester());
        Wahlangebot wahlangebot = pruefeWahlangebot(modul, request.offerCode());
        String kursCode = pruefeKurs(modul, request.courseCode(), request.courseTitle());

        Leistung leistung = leistungRepository.findByStudierendeAndModul(studierende, modul)
                .orElseGet(() -> new Leistung(studierende, modul, request.status()));

        leistung.setStatus(request.status());
        leistung.setNote(note);
        leistung.setSemester(semester);
        leistung.setWahlangebot(wahlangebot);
        leistung.setKursCode(kursCode);
        leistung.setKursTitel(leer(request.courseTitle()) ? null : request.courseTitle().trim());
        // Nur bei NICHT_BESTANDEN wird die Versuchszahl gesetzt. Bei BELEGT und
        // BESTANDEN bleibt der gespeicherte Wert unverändert.
        if (request.status() == Status.NICHT_BESTANDEN) {
            leistung.setAnzahlVersuche(request.attempt());
        }

        return zuAntwort(leistungRepository.save(leistung));
    }

    /**
     * Löscht die Leistung für ein Modul. Gibt es keine, passiert nichts.
     *
     * @param moduleCode Modulcode aus dem Pfad
     * @throws NotFoundException wenn es das Modul nicht gibt
     */
    @Transactional
    public void loeschen(String moduleCode) {
        Studierende studierende = aktuelleStudierende();
        Modul modul = modulOderFehler(moduleCode);
        leistungRepository.findByStudierendeAndModul(studierende, modul)
                .ifPresent(leistungRepository::delete);
    }

    /**
     * Prüft, ob ein Wahlangebot zum Modul passt.
     *
     * <ul>
     *   <li>B41/B54: Angebot der Gruppe {@code WP}</li>
     *   <li>B16/B26: Angebot der Gruppe {@code SPRACHE} mit derselben Niveaustufe wie das
     *       Modul ({@code 1a} bei B16, {@code 1b} bei B26, E26)</li>
     * </ul>
     *
     * <p>Öffentlich, damit der Plan dieselbe Regel verwenden kann.
     *
     * @param modul       das Modul
     * @param wahlangebot das gewählte Angebot
     * @return true, wenn das Angebot für dieses Modul gewählt werden darf
     */
    public boolean angebotPasstZumModul(Modul modul, Wahlangebot wahlangebot) {
        if (MODULE_MIT_WP_ANGEBOT.contains(modul.getCode())) {
            return wahlangebot.getGruppe() == AngebotsGruppe.WP;
        }
        if (MODULE_MIT_SPRACHANGEBOT.contains(modul.getCode())) {
            return wahlangebot.getGruppe() == AngebotsGruppe.SPRACHE
                    && Objects.equals(wahlangebot.getNiveaustufe(), modul.getNiveaustufe());
        }
        return false;
    }

    // ---------------------------------------------------------------- Regeln

    /**
     * Prüft die Note gegen Status und Modul und gibt sie mit einer Nachkommastelle zurück.
     *
     * <p>Vergleiche immer mit {@code compareTo}: Das Frontend schickt 4,0 als {@code 4}
     * und 5,0 als {@code 5}, und {@code new BigDecimal("5").equals(new BigDecimal("5.0"))}
     * ist {@code false}.
     */
    private BigDecimal pruefeNote(Modul modul, Status status, BigDecimal note) {
        if (status == Status.BELEGT) {
            if (note != null) {
                throw new FieldValidationException("grade", "Bei BELEGT gibt es keine Note.");
            }
            return null;
        }
        if (!modul.isBenotet()) {
            if (note != null) {
                throw new FieldValidationException("grade",
                        "Modul " + modul.getCode() + " ist unbenotet und hat keine Note.");
            }
            return null;
        }
        if (note == null) {
            throw new FieldValidationException("grade", "Bei " + status + " ist eine Note Pflicht.");
        }
        if (ERLAUBTE_NOTEN.stream().noneMatch(erlaubt -> erlaubt.compareTo(note) == 0)) {
            throw new FieldValidationException("grade",
                    "Erlaubt sind nur die Noten 1,0 bis 4,0 und 5,0.");
        }
        if (status == Status.BESTANDEN && note.compareTo(BESTANDEN_MAX) > 0) {
            throw new FieldValidationException("grade", "Bei BESTANDEN ist die Note höchstens 4,0.");
        }
        if (status == Status.NICHT_BESTANDEN && note.compareTo(NICHT_BESTANDEN_NOTE) != 0) {
            throw new FieldValidationException("grade", "Bei NICHT_BESTANDEN ist die Note 5,0.");
        }
        // Einheitlich mit einer Nachkommastelle speichern (4 → 4.0).
        return note.setScale(1, RoundingMode.UNNECESSARY);
    }

    /** {@code attempt} ist nur bei NICHT_BESTANDEN erlaubt, dann Pflicht und 1–4. */
    private void pruefeVersuch(Status status, Integer versuch) {
        if (status != Status.NICHT_BESTANDEN) {
            if (versuch != null) {
                throw new FieldValidationException("attempt",
                        "attempt wird nur bei NICHT_BESTANDEN angegeben.");
            }
            return;
        }
        if (versuch == null) {
            throw new FieldValidationException("attempt",
                    "Bei NICHT_BESTANDEN ist anzugeben, im wievielten Versuch.");
        }
        if (versuch < MIN_VERSUCH || versuch > MAX_VERSUCH) {
            throw new FieldValidationException("attempt",
                    "Es gibt höchstens " + MAX_VERSUCH + " Versuche (1–" + MAX_VERSUCH + ").");
        }
    }

    /**
     * Semester darf leer sein (anerkannte Leistung). Sonst muss es gültig sein und darf
     * nicht vor dem Startsemester liegen (E31). Fehlt es in der DB, wird es angelegt (E19).
     */
    private Semester pruefeSemester(Studierende studierende, String text) {
        if (leer(text)) {
            return null;
        }
        Semester gelesen = semesterService.parse(text.trim(), "semester");
        Semester start = studierende.getStartsemester();
        if (semesterService.istFrueher(gelesen, start)) {
            throw new FieldValidationException("semester",
                    "Das Semester darf nicht vor deinem Startsemester (" + start.label() + ") liegen.");
        }
        return semesterService.findenOderNeuAnlegen(gelesen.getJahr(), gelesen.getArt());
    }

    /** B16/B26/B41/B54 brauchen ein passendes Angebot, alle anderen Module keins. */
    private Wahlangebot pruefeWahlangebot(Modul modul, String offerCode) {
        boolean brauchtAngebot = MODULE_MIT_WP_ANGEBOT.contains(modul.getCode())
                || MODULE_MIT_SPRACHANGEBOT.contains(modul.getCode());

        if (!brauchtAngebot) {
            if (!leer(offerCode)) {
                throw new FieldValidationException("offerCode",
                        "Für Modul " + modul.getCode() + " gibt es keine Wahlangebote.");
            }
            return null;
        }
        if (leer(offerCode)) {
            throw new FieldValidationException("offerCode",
                    "Für Modul " + modul.getCode() + " ist ein Wahlangebot Pflicht.");
        }
        Wahlangebot wahlangebot = wahlangebotRepository.findById(offerCode.trim())
                .orElseThrow(() -> new FieldValidationException("offerCode",
                        "Das Wahlangebot " + offerCode + " gibt es nicht."));
        if (!angebotPasstZumModul(modul, wahlangebot)) {
            throw new FieldValidationException("offerCode",
                    "Das Wahlangebot " + wahlangebot.getCode() + " passt nicht zu Modul " + modul.getCode() + ".");
        }
        return wahlangebot;
    }

    /** B63/B64 brauchen eine Veranstaltungsnummer, alle anderen Module keine. */
    private String pruefeKurs(Modul modul, String courseCode, String courseTitle) {
        if (!AWE_MODULE_MIT_KURS.contains(modul.getCode())) {
            if (!leer(courseCode) || !leer(courseTitle)) {
                throw new FieldValidationException(leer(courseCode) ? "courseTitle" : "courseCode",
                        "Für Modul " + modul.getCode() + " wird keine Veranstaltung eingetragen.");
            }
            return null;
        }
        if (leer(courseCode)) {
            throw new FieldValidationException("courseCode",
                    "Für Modul " + modul.getCode() + " ist die Veranstaltungsnummer Pflicht.");
        }
        return courseCode.trim();
    }

    // ---------------------------------------------------------------- Hilfen

    private Studierende aktuelleStudierende() {
        return studierendeRepository.findById(AktuellePerson.id())
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));
    }

    private Modul modulOderFehler(String moduleCode) {
        return modulRepository.findById(moduleCode)
                .orElseThrow(() -> new NotFoundException("Das Modul " + moduleCode + " gibt es nicht."));
    }

    private ResultResponse zuAntwort(Leistung leistung) {
        return new ResultResponse(
                leistung.getModul().getCode(),
                leistung.getStatus(),
                leistung.getSemester() == null ? null : leistung.getSemester().label(),
                leistung.getNote(),
                leistung.getAnzahlVersuche(),
                leistung.getWahlangebot() == null ? null : leistung.getWahlangebot().getCode(),
                leistung.getKursCode(),
                leistung.getKursTitel());
    }

    private static boolean leer(String text) {
        return text == null || text.isBlank();
    }
}
