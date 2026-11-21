package de.htw.studyplanner.backend.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.exception.UngueltigerEinladungscodeException;
import de.htw.studyplanner.backend.model.Einladungscode;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.repository.EinladungscodeRepository;

/**
 * Erzeugt, prüft und löst Einladungscodes für Professor:innen ein.
 *
 * <p>Codes sind 12 Zeichen lang und werden mit {@link SecureRandom} aus 31 Zeichen
 * gewählt (ohne 0/O, 1/I/L). Das sind 31^12 ≈ 7,9 · 10^17 Möglichkeiten.
 */
@Service
public class EinladungscodeService {

    /** Erlaubte Zeichen: ohne 0, O, 1, I, L, damit man den Code gut abtippen kann. */
    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /** Länge eines Codes. */
    public static final int CODE_LAENGE = 12;

    private final EinladungscodeRepository repository;
    private final SecureRandom random = new SecureRandom();

    public EinladungscodeService(EinladungscodeRepository repository) {
        this.repository = repository;
    }

    /** Erzeugt einen zufälligen Code, ohne ihn zu speichern. */
    public String erzeugeCode() {
        StringBuilder sb = new StringBuilder(CODE_LAENGE);
        for (int i = 0; i < CODE_LAENGE; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Legt neue Codes an und speichert sie.
     *
     * @param anzahl      wie viele Codes
     * @param gueltigkeit wie lange die Codes ab jetzt gültig sind
     */
    @Transactional
    public List<Einladungscode> legeAn(int anzahl, Duration gueltigkeit) {
        if (anzahl < 1) {
            throw new IllegalArgumentException("anzahl muss mindestens 1 sein");
        }
        Instant gueltigBis = Instant.now().plus(gueltigkeit);
        List<Einladungscode> codes = new ArrayList<>(anzahl);
        for (int i = 0; i < anzahl; i++) {
            String code;
            do {
                code = erzeugeCode();
            } while (repository.existsById(code)); // praktisch nie, aber sicher ist sicher
            codes.add(repository.save(new Einladungscode(code, gueltigBis)));
        }
        return codes;
    }

    /**
     * Sucht einen einlösbaren Code und sperrt ihn bis zum Ende der Transaktion.
     * Groß-/Kleinschreibung und Leerzeichen am Rand sind egal.
     *
     * @throws UngueltigerEinladungscodeException wenn der Code unbekannt, abgelaufen oder
     *                                            schon eingelöst ist (immer dieselbe Meldung)
     */
    @Transactional
    public Einladungscode sperreEinloesbarenCode(String eingabe) {
        String code = normalisiere(eingabe);
        if (code.isEmpty() || code.length() > 32) {
            throw new UngueltigerEinladungscodeException();
        }
        return repository.findByCodeForUpdate(code)
                .filter(e -> e.istEinloesbar(Instant.now()))
                .orElseThrow(UngueltigerEinladungscodeException::new);
    }

    /** Markiert einen zuvor gesperrten Code als eingelöst. */
    @Transactional
    public void einloesen(Einladungscode code, Nutzende nutzende) {
        code.einloesen(nutzende, Instant.now());
        repository.save(code);
    }

    private static String normalisiere(String eingabe) {
        return eingabe == null ? "" : eingabe.strip().toUpperCase(Locale.ROOT);
    }
}