package de.htw.studyplanner.backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.exception.ConflictException;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.repository.NutzendeRepository;

/**
 * Gemeinsame Logik für alle Registrierungen: E-Mail prüfen, Passwort hashen,
 * Konto in {@code nutzende} anlegen. E-Mails werden immer in Kleinbuchstaben
 * gespeichert und verglichen, wie beim Login.
 *
 * <p>Wird von {@link RegistrationService} (Studierende, E2.2) und
 * {@link ProfessorRegistrationService} (Professor:innen, E2.5) genutzt, damit die
 * 409-Regel und das Hashing nur an einer Stelle stehen.
 */
@Service
public class KontoService {

    /** Meldung bei 409, gleich für alle Registrierungen. */
    public static final String EMAIL_VERGEBEN = "Diese E-Mail-Adresse ist bereits vergeben.";

    private final NutzendeRepository nutzendeRepository;
    private final PasswordEncoder passwordEncoder;

    public KontoService(NutzendeRepository nutzendeRepository, PasswordEncoder passwordEncoder) {
        this.nutzendeRepository = nutzendeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Prüft, ob die E-Mail noch frei ist.
     *
     * @throws ConflictException wenn es die E-Mail schon gibt (führt zu Status 409)
     */
    public void pruefeEmailFrei(String email) {
        if (nutzendeRepository.findByEmail(normalisiere(email)).isPresent()) {
            throw new ConflictException(EMAIL_VERGEBEN);
        }
    }

    /**
     * Legt ein Konto mit der angegebenen Rolle an. Gespeichert wird nur der BCrypt-Hash.
     * Die Rolle bestimmt immer der aufrufende Service, nie der Request.
     *
     * @throws ConflictException wenn es die E-Mail schon gibt (führt zu Status 409)
     */
    @Transactional
    public Nutzende legeKontoAn(String email, String password, Rolle rolle) {
        String normalisiert = normalisiere(email);
        pruefeEmailFrei(normalisiert);
        String passwortHash = passwordEncoder.encode(password);
        return nutzendeRepository.save(new Nutzende(normalisiert, passwortHash, rolle));
    }

    /**
     * Macht aus einer E-Mail die gespeicherte Form: Kleinbuchstaben.
     * {@code Max@HTW-Berlin.de} und {@code max@htw-berlin.de} sind damit dieselbe Adresse.
     *
     * @param email die E-Mail aus dem Request
     * @return die E-Mail in Kleinbuchstaben
     */
    static String normalisiere(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}