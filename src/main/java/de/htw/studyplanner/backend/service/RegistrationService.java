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

@Service
public class RegistrationService {

    private final NutzendeRepository nutzendeRepository;
    private final StudierendeRepository studierendeRepository;
    private final SemesterService semesterService;
    private final PasswordEncoder passwordEncoder;

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
