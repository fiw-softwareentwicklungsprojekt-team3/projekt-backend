package de.htw.studyplanner.backend.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import de.htw.studyplanner.backend.dto.LoginRequest;
import de.htw.studyplanner.backend.dto.LoginResponse;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Meldet eine Person an: prüft E-Mail und Passwort und stellt bei Erfolg ein Token aus.
 */
@Service
public class LoginService {

    private final NutzendeRepository nutzendeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(
            NutzendeRepository nutzendeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.nutzendeRepository = nutzendeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Prüft die Zugangsdaten und stellt ein Token aus.
     *
     * <p>Die E-Mail wird ohne Unterscheidung von Groß- und Kleinschreibung gesucht.
     * Bei unbekannter E-Mail und bei falschem Passwort kommt dieselbe Exception, damit
     * von außen nicht erkennbar ist, ob es ein Konto gibt.
     *
     * @param request E-Mail und Passwort
     * @return Token, Gültigkeit in Sekunden und Rolle
     * @throws InvalidCredentialsException wenn E-Mail oder Passwort nicht stimmen
     */
    public LoginResponse login(LoginRequest request) {
        Nutzende nutzende = nutzendeRepository
                .findByEmail(request.email().toLowerCase(Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                nutzende.getPasswortHash());

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(nutzende);

        return new LoginResponse(
                token,
                jwtService.getExpirationSeconds(),
                nutzende.getRolle().name());
    }
}