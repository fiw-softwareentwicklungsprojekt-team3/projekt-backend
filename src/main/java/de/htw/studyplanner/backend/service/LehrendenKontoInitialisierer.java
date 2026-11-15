package de.htw.studyplanner.backend.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.repository.NutzendeRepository;

/**
 * Legt beim Start der Anwendung ein Lehrenden-Konto mit der Rolle PROFESSOR_IN an.
 *
 * <p>E-Mail und Passwort kommen aus den Einstellungen {@code professor.email} und
 * {@code professor.password} (Umgebungsvariablen PROFESSOR_EMAIL und PROFESSOR_PASSWORD).
 * Sind sie nicht gesetzt oder gibt es die E-Mail schon, passiert nichts. Es entsteht
 * kein Eintrag in {@code studierende}.
 */
@Component
public class LehrendenKontoInitialisierer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LehrendenKontoInitialisierer.class);

    private final NutzendeRepository nutzendeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String passwort;

    /**
     * Erstellt den Initialisierer.
     *
     * @param nutzendeRepository Zugriff auf die Konten
     * @param passwordEncoder    erzeugt den Passwort-Hash (BCrypt)
     * @param email              E-Mail des Lehrenden-Kontos, leer wenn nicht gesetzt
     * @param passwort           Passwort des Lehrenden-Kontos, leer wenn nicht gesetzt
     */
    public LehrendenKontoInitialisierer(
            NutzendeRepository nutzendeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${professor.email:}") String email,
            @Value("${professor.password:}") String passwort) {
        this.nutzendeRepository = nutzendeRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.passwort = passwort;
    }

    @Override
    public void run(String... args) {
        kontoAnlegen();
    }

    /**
     * Legt das Konto an, wenn beide Werte gesetzt sind und die E-Mail noch nicht existiert.
     *
     * @return {@code true}, wenn ein neues Konto angelegt wurde
     */
    public boolean kontoAnlegen() {
        if (email.isBlank() || passwort.isBlank()) {
            return false;
        }
        // E-Mail in Kleinbuchstaben, damit der Login unabhängig von der Schreibweise funktioniert.
        String kleineEmail = email.trim().toLowerCase(Locale.ROOT);
        if (nutzendeRepository.findByEmail(kleineEmail).isPresent()) {
            return false;
        }
        nutzendeRepository.save(new Nutzende(
                kleineEmail,
                passwordEncoder.encode(passwort),
                Rolle.PROFESSOR_IN,
                false));
        LOG.info("Lehrenden-Konto {} wurde angelegt.", kleineEmail);
        return true;
    }
}