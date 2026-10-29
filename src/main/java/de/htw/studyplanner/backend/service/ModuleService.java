package de.htw.studyplanner.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.htw.studyplanner.backend.dto.ModuleDetailDto;
import de.htw.studyplanner.backend.dto.ModuleDto;
import de.htw.studyplanner.backend.dto.PrerequisiteDto;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.ModulArt;
import de.htw.studyplanner.backend.model.Turnus;
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.VoraussetzungRepository;

/**
 * Liest den Modulkatalog: Modulliste mit Filtern und ein einzelnes Modul mit Voraussetzungen.
 *
 * <p>Das Studium hat nur etwa 30 Module. Deshalb werden alle Module geladen und im Speicher
 * gefiltert. Das ist einfacher zu lesen als eine Abfrage mit optionalen Parametern und
 * funktioniert unabhängig von der Datenbank.
 */
@Service
@Transactional(readOnly = true)
public class ModuleService {

    private static final Sort SORTIERUNG = Sort.by("fachsemester", "code");

    private static final int ERSTES_SEMESTER = 1;
    private static final int LETZTES_SEMESTER = 6;

    private final ModulRepository modulRepository;
    private final VoraussetzungRepository voraussetzungRepository;

    /**
     * Erstellt den Service.
     *
     * @param modulRepository         Zugriff auf die Module
     * @param voraussetzungRepository Zugriff auf die Voraussetzungen
     */
    public ModuleService(ModulRepository modulRepository,
            VoraussetzungRepository voraussetzungRepository) {
        this.modulRepository = modulRepository;
        this.voraussetzungRepository = voraussetzungRepository;
    }

    /**
     * Liefert die Module, sortiert nach Fachsemester und danach nach Code.
     * Alle Filter sind optional und werden mit UND verknüpft.
     *
     * @param fachsemester Fachsemester 1 bis 6, oder {@code null} für alle
     * @param art          "P" oder "WP", oder {@code null} für alle
     * @param turnus       "WS" oder "SS", oder {@code null} für alle; Module mit Turnus BEIDE
     *                     erscheinen bei beiden Werten
     * @return die passenden Module, leer wenn keines passt
     * @throws ResponseStatusException mit Status 400, wenn ein Filterwert ungültig ist
     */
    public List<ModuleDto> list(Integer fachsemester, String art, String turnus) {
        if (fachsemester != null
                && (fachsemester < ERSTES_SEMESTER || fachsemester > LETZTES_SEMESTER)) {
            throw badRequest("Ungültiger Wert für semester: erlaubt sind 1 bis 6.");
        }
        ModulArt gesuchterType = parseType(art);
        Turnus gesuchterTerm = parseTerm(turnus);

        return modulRepository.findAll(SORTIERUNG).stream()
                .filter(m -> fachsemester == null || m.getFachsemester() == fachsemester)
                .filter(m -> gesuchterType == null || m.getArt() == gesuchterType)
                .filter(m -> gesuchterTerm == null
                        || m.getTurnus() == gesuchterTerm || m.getTurnus() == Turnus.BEIDE)
                .map(ModuleDto::of)
                .toList();
    }

    /**
     * Liefert ein Modul mit seinen Voraussetzungen. Notwendige stehen vor empfohlenen,
     * danach folgt die Sortierung nach Modulcode, LP-Voraussetzungen kommen zuletzt.
     *
     * @param code Modulcode, z. B. "B34"
     * @return das Modul mit Voraussetzungen
     * @throws ResponseStatusException mit Status 404, wenn es den Code nicht gibt
     */
    public ModuleDetailDto get(String code) {
        Modul modul = modulRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Modul mit dem Code " + code + " wurde nicht gefunden."));

        List<PrerequisiteDto> voraussetzungen = voraussetzungRepository.findByModul_Code(code).stream()
                .map(ModuleService::toDto)
                .sorted(Comparator.comparing(PrerequisiteDto::kind)
                        .thenComparing(PrerequisiteDto::moduleCode,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(PrerequisiteDto::minLp,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return ModuleDetailDto.of(modul, voraussetzungen);
    }

    private static PrerequisiteDto toDto(Voraussetzung v) {
        Modul voraus = v.getVorausgesetztesModul();
        if (voraus != null) {
            return new PrerequisiteDto(v.getArt(), voraus.getCode(), voraus.getBezeichnung(), null, null);
        }
        return new PrerequisiteDto(v.getArt(), null, null, v.getMinLp(), v.getToleranzLp());
    }

    private static ModulArt parseType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return ModulArt.valueOf(type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw badRequest("Ungültiger Wert für type: erlaubt sind P und WP.");
        }
    }

    /** BEIDE ist als Filterwert nicht erlaubt; es ist nur ein Wert der Module selbst. */
    private static Turnus parseTerm(String term) {
        if (term == null) {
            return null;
        }
        if (term.equalsIgnoreCase("WS")) {
            return Turnus.WS;
        }
        if (term.equalsIgnoreCase("SS")) {
            return Turnus.SS;
        }
        throw badRequest("Ungültiger Wert für term: erlaubt sind WS und SS.");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
