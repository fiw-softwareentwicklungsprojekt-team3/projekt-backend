package de.htw.studyplanner.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import de.htw.studyplanner.backend.dto.LoginRequest;
import de.htw.studyplanner.backend.dto.LoginResponse;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.security.JwtService;

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

    public LoginResponse login(LoginRequest request) {
        Nutzende nutzende = nutzendeRepository
                .findByEmail(request.email())
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