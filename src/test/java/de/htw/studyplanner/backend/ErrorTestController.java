package de.htw.studyplanner.backend;

import de.htw.studyplanner.backend.exception.ConflictException;
import de.htw.studyplanner.backend.exception.NotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-errors")
@Profile("test")
public class ErrorTestController {

    @GetMapping("/not-found")
    public void notFound() { throw new NotFoundException("Modul mit der ID 42 wurde nicht gefunden."); }

    @GetMapping("/conflict")
    public void conflict() { throw new ConflictException("Diese E-Mail-Adresse ist bereits vergeben."); }

    @GetMapping("/forbidden")
    public void forbidden() { throw new AccessDeniedException("verboten"); }

    @GetMapping("/boom")
    public void boom() { throw new IllegalStateException("SELECT * FROM users, com.example.Geheim"); }
  
}
