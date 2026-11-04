package de.htw.studyplanner.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.RegisterRequest;
import de.htw.studyplanner.backend.dto.RegisterResponse;
import de.htw.studyplanner.backend.exception.ConflictException;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.StudierendeRepository;

/**
 * Legt neue Studierende an: Konto in {@code nutzende} (über {@link KontoService}),
 * Eintrag in {@code studierende} und, falls nötig, das Startsemester in {@code semester}.
 */
@Service
public class RegistrationService {

    private final KontoService kontoService;
    private final StudierendeRepository studierendeRepository;
    private final SemesterService semesterService;

    /**
     * Erstellt den Service.
     *
     * @param kontoService          legt das Konto an (gemeinsam mit der Professor:innen-Registrierung)
     * @param studierendeRepository Zugriff auf die Studierenden-Einträge
     * @param semesterService       Zugriff auf die Semester
     */
    public RegistrationService(
            KontoService kontoService,
            StudierendeRepository studierendeRepository,
            SemesterService semesterService) {
        this.kontoService = kontoService;
        this.studierendeRepository = studierendeRepository;
        this.semesterService = semesterService;
    }

    /**
     * Registriert eine neue Person mit der Rolle STUDIERENDE.
     *
     * <p>Die Rolle ist hier fest eingetragen. Über diesen Weg entsteht nie ein Konto
     * mit einer anderen Rolle, auch wenn der Request ein Feld {@code rolle} oder
     * {@code code} mitschickt. Professor:innen registrieren sich über
     * {@link ProfessorRegistrationService}.
     *
     * <p>Gibt es das Startsemester noch nicht in der Tabelle {@code semester}, wird es
     * angelegt. Alles passiert in einer Transaktion: schlägt ein Schritt fehl, wird
     * nichts gespeichert.
     *
     * @param request bereits validierte Eingaben; startsemester hat das Format "WS JJJJ"
     * @return die angelegte Person ohne Passwort
     * @throws ConflictException wenn es die E-Mail schon gibt (führt zu Status 409)
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        Nutzende nutzende = kontoService.legeKontoAn(
                request.email(), request.password(), Rolle.STUDIERENDE);

        Semester parsed = semesterService.parse(request.startsemester());
        Semester semester = semesterService.findenOderNeuAnlegen(parsed.getJahr(), parsed.getArt());

        studierendeRepository.save(new Studierende(nutzende, semester));

        return new RegisterResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name(),
                semester.label()
        );
    }
}
