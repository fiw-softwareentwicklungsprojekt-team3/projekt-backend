package de.htw.studyplanner.backend.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import de.htw.studyplanner.backend.dto.RegisterRequest;
import de.htw.studyplanner.backend.dto.RegisterResponse;
import de.htw.studyplanner.backend.service.RegistrationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request) {

        return registrationService.register(request);
    }
}
