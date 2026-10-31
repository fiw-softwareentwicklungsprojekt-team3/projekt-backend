package de.htw.studyplanner.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.RegisterRequest;
import de.htw.studyplanner.backend.dto.RegisterResponse;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;

/**
 * Legt neue Studierende an: Konto in {@code nutzende}, Eintrag in {@code studierende}
 * und, falls nötig, das Startsemester in {@code semester}.
 */
@Service
public class RegistrationService {

    private final NutzendeRepository nutzendeRepository;
    private final StudierendeRepository studierendeRepository;
    private final SemesterService semesterService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Erstellt den Service.
     *
     * @param nutzendeRepository    Zugriff auf die Konten
     * @param studierendeRepository Zugriff auf die Studierenden-Einträge
     * @param semesterRepository    Zugriff auf die Semester
     * @param passwordEncoder       erzeugt den Passwort-Hash (BCrypt)
     */
    public RegistrationService(
            NutzendeRepository nutzendeRepository,
            StudierendeRepository studierendeRepository,
            SemesterService semesterService,
            PasswordEncoder passwordEncoder) {
        this.nutzendeRepository = nutzendeRepository;
        this.studierendeRepository = studierendeRepository;
        this.semesterService = semesterService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registriert eine neue Person mit der Rolle STUDIERENDE.
     *
     * <p>Gespeichert wird nur der BCrypt-Hash des Passworts. Gibt es das Startsemester
     * noch nicht in der Tabelle {@code semester}, wird es angelegt. Alles passiert in
     * einer Transaktion: schlägt ein Schritt fehl, wird nichts gespeichert.
     *
     * @param request bereits validierte Eingaben; startsemester hat das Format "WS JJJJ"
     * @return die angelegte Person ohne Passwort
     * @throws EmailAlreadyExistsException wenn es die E-Mail schon gibt (führt zu Status 409)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (nutzendeRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        Semester parsed = semesterService.parse(request.startsemester());
        Semester semester = semesterService.findenOderNeuAnlegen(parsed.getJahr(), parsed.getArt());

        String passwortHash = passwordEncoder.encode(request.password());

        // Die Rolle wird immer vom Backend gesetzt.
        Nutzende nutzende = nutzendeRepository.save(
                new Nutzende(
                        request.email(),
                        passwortHash,
                        Rolle.STUDIERENDE
                ));

        studierendeRepository.save(
                new Studierende(nutzende, semester)
        );

        return new RegisterResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name(),
                semester.label()
        );
    }
}
