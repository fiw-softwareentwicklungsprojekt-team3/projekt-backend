package de.htw.studyplanner.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.OfferDto;
import de.htw.studyplanner.backend.exception.FieldValidationException;
import de.htw.studyplanner.backend.exception.NotFoundException;
import de.htw.studyplanner.backend.model.AngebotsGruppe;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Wahlangebot;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.WahlangebotRepository;

/**
 * Welche Wahlangebote zu welchem Modul passen (E10, E26).
 *
 * <p>Die Zuordnung steht nicht als Fremdschlüssel in der Datenbank, sondern ergibt sich
 * aus {@code gruppe} und {@code niveaustufe}:
 * <ul>
 *   <li>B41 und B54: alle Angebote der Gruppe {@code WP}</li>
 *   <li>B16 und B26: Angebote der Gruppe {@code SPRACHE} mit derselben Niveaustufe wie das
 *       Modul ({@code 1a} bei B16, {@code 1b} bei B26)</li>
 *   <li>alle anderen Module, auch B63 und B64 (AWE, „freie Wahl“): keine Angebote</li>
 * </ul>
 *
 * <p>Diese Klasse ist die <b>einzige</b> Stelle mit dieser Regel. Der Modulkatalog
 * ({@code GET /api/modules/{code}}) zeigt damit die Angebote an, und die Leistungen
 * ({@link ResultService}) prüfen damit ein gewähltes Angebot.
 */
@Service
@Transactional(readOnly = true)
public class OfferService {

    /** Module, die aus der Gruppe WP wählen (Wahlpflichtmodule). */
    private static final Set<String> MODULE_MIT_WP_ANGEBOT = Set.of("B41", "B54");

    /** Module, die eine Sprache mit ihrer eigenen Niveaustufe wählen. */
    private static final Set<String> MODULE_MIT_SPRACHANGEBOT = Set.of("B16", "B26");

    /** Teilt einen Code in Text- und Zahlteile, z. B. "WP12" in "WP" und "12". */
    private static final Pattern TEIL = Pattern.compile("\\d+|\\D+");

    /**
     * Sortiert Codes so, wie man sie liest: WP1, WP2, …, WP10 statt WP1, WP10, WP2.
     * Zahlteile werden als Zahl verglichen, Textteile als Text.
     */
    static final Comparator<String> CODE_REIHENFOLGE = OfferService::vergleicheCodes;

    private final WahlangebotRepository wahlangebotRepository;
    private final ModulRepository modulRepository;

    /**
     * Erstellt den Service.
     *
     * @param wahlangebotRepository Zugriff auf die Wahlangebote
     * @param modulRepository       Zugriff auf den Modulkatalog
     */
    public OfferService(WahlangebotRepository wahlangebotRepository, ModulRepository modulRepository) {
        this.wahlangebotRepository = wahlangebotRepository;
        this.modulRepository = modulRepository;
    }

    /**
     * Liefert die Angebote, die bei diesem Modul gewählt werden können, sortiert nach Code.
     *
     * @param modulCode Code des Moduls, z. B. {@code B16}
     * @return die passenden Angebote; leer, wenn das Modul keine feste Auswahl hat
     * @throws NotFoundException wenn es das Modul nicht gibt
     */
    public List<Wahlangebot> passendeAngebote(String modulCode) {
        Modul modul = modulRepository.findById(modulCode)
                .orElseThrow(() -> new NotFoundException(
                        "Modul mit dem Code " + modulCode + " wurde nicht gefunden."));
        return passendeAngebote(modul);
    }

    /**
     * Liefert die Angebote, die bei diesem Modul gewählt werden können, sortiert nach Code.
     *
     * @param modul das Modul
     * @return die passenden Angebote; leer, wenn das Modul keine feste Auswahl hat
     */
    public List<Wahlangebot> passendeAngebote(Modul modul) {
        List<Wahlangebot> angebote;
        if (MODULE_MIT_WP_ANGEBOT.contains(modul.getCode())) {
            angebote = wahlangebotRepository.findByGruppe(AngebotsGruppe.WP);
        } else if (MODULE_MIT_SPRACHANGEBOT.contains(modul.getCode())) {
            angebote = wahlangebotRepository.findByGruppeAndNiveaustufe(
                    AngebotsGruppe.SPRACHE, modul.getNiveaustufe());
        } else {
            return List.of();
        }
        return angebote.stream()
                .sorted(Comparator.comparing(Wahlangebot::getCode, CODE_REIHENFOLGE))
                .toList();
    }

    /**
     * Gibt an, ob bei diesem Modul ein Angebot aus einer festen Liste gewählt wird
     * (B16, B26, B41, B54). B63/B64 zählen nicht dazu: Dort gibt es keine Liste.
     *
     * @param modul das Modul
     * @return true, wenn es eine feste Auswahl gibt
     */
    public boolean hatFesteAuswahl(Modul modul) {
        return MODULE_MIT_WP_ANGEBOT.contains(modul.getCode())
                || MODULE_MIT_SPRACHANGEBOT.contains(modul.getCode());
    }

    /**
     * Prüft, ob ein Angebot bei diesem Modul gewählt werden darf. Nutzt dieselbe Regel wie
     * {@link #passendeAngebote(Modul)}, damit Anzeige und Prüfung nie auseinanderlaufen.
     *
     * @param modul       das Modul
     * @param wahlangebot das gewählte Angebot
     * @return true, wenn das Angebot zu den passenden Angeboten des Moduls gehört
     */
    public boolean passtZumModul(Modul modul, Wahlangebot wahlangebot) {
        return passendeAngebote(modul).stream()
                .anyMatch(angebot -> angebot.getCode().equals(wahlangebot.getCode()));
    }

    /**
     * Alle Angebote als DTO, optional nach Gruppe gefiltert, sortiert nach Code.
     *
     * @param gruppe {@code WP}, {@code SPRACHE} (Groß-/Kleinschreibung egal) oder null für alle
     * @return die Angebote
     * @throws FieldValidationException wenn die Gruppe keinen erlaubten Wert hat (400)
     */
    public List<OfferDto> alleAngebote(String gruppe) {
        List<Wahlangebot> angebote = gruppe == null
                ? wahlangebotRepository.findAll()
                : wahlangebotRepository.findByGruppe(parseGruppe(gruppe));
        return angebote.stream()
                .sorted(Comparator.comparing(Wahlangebot::getCode, CODE_REIHENFOLGE))
                .map(OfferDto::of)
                .toList();
    }

    private static AngebotsGruppe parseGruppe(String gruppe) {
        try {
            return AngebotsGruppe.valueOf(gruppe.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new FieldValidationException("group", "Erlaubt sind nur WP und SPRACHE.");
        }
    }

    private static int vergleicheCodes(String a, String b) {
        Matcher ma = TEIL.matcher(a);
        Matcher mb = TEIL.matcher(b);
        while (ma.find() && mb.find()) {
            String ta = ma.group();
            String tb = mb.group();
            int ergebnis;
            if (Character.isDigit(ta.charAt(0)) && Character.isDigit(tb.charAt(0))) {
                ergebnis = Long.compare(Long.parseLong(ta), Long.parseLong(tb));
            } else {
                ergebnis = ta.compareTo(tb);
            }
            if (ergebnis != 0) {
                return ergebnis;
            }
        }
        return Integer.compare(a.length(), b.length());
    }
}
