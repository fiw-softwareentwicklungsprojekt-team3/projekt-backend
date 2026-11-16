package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void erfolgreicheRegistrierungLiefert201OhnePasswortHash()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "erfolg@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("erfolg@student.htw-berlin.de"))
                .andExpect(jsonPath("$.role")
                        .value("STUDIERENDE"))
                .andExpect(jsonPath("$.passwortHash")
                        .doesNotExist());
    }

    @Test
    void passwortWirdAlsBcryptHashGespeichert()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bcrypt@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isCreated());

        var nutzende = nutzendeRepository
                .findByEmail("bcrypt@student.htw-berlin.de")
                .orElseThrow();

        assertThat(nutzende.getPasswortHash())
                .startsWith("$2");

        assertThat(nutzende.getPasswortHash())
                .isNotEqualTo("geheimesPasswort");

        assertThat(passwordEncoder.matches(
                "geheimesPasswort",
                nutzende.getPasswortHash()
        )).isTrue();
    }

    @Test
    void doppelteEmailLiefert409()
            throws Exception {

        String request = """
                {
                  "email": "doppelt@student.htw-berlin.de",
                  "password": "geheimesPasswort",
                  "startsemester": "WS 2025"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Diese E-Mail-Adresse ist bereits vergeben."));
    }

    @Test
    void emailWirdKleinGespeichertUndAndereSchreibweiseLiefert409()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Mischung@Student.HTW-Berlin.DE",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("mischung@student.htw-berlin.de"));

        // Das Konto ist in Kleinbuchstaben gespeichert.
        assertThat(nutzendeRepository.findByEmail("mischung@student.htw-berlin.de"))
                .isPresent();

        // Dieselbe Adresse in anderer Schreibweise ist ein Duplikat.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "mischung@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void ungueltigeEmailLiefert400()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "keine-email",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isBadRequest())
                // Nur der Format-Fehler, nicht zusätzlich der Domain-Fehler.
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("email ist ungültig"));
    }

    @Test
    void htwAdresseWirdUnabhaengigVonGrossUndKleinschreibungAkzeptiert()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Name@Student.HTW-Berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("name@student.htw-berlin.de"));
    }

    @Test
    void gueltigeEmailOhneStudierendenDomainLiefert400()
            throws Exception {

        // Alle Adressen sind als E-Mail gültig, gehören aber nicht zu @student.htw-berlin.de:
        // fremde Domain, Lehrenden-Domain und drei Tricks mit ähnlichen Domains.
        String[] emails = {
                "domain-fremd@gmail.com",
                "domain-lehrende@htw-berlin.de",
                "domain-trick1@fakestudent.htw-berlin.de",
                "student.htw-berlin.de@gmail.com",
                "domain-trick2@student.htw-berlin.de.example.com"
        };

        for (String email : emails) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "%s",
                                      "password": "geheimesPasswort",
                                      "startsemester": "WS 2025"
                                    }
                                    """.formatted(email)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].message")
                            .value("email muss auf @student.htw-berlin.de enden"));

            // Es wurde niemand angelegt.
            assertThat(nutzendeRepository.findByEmail(email)).isEmpty();
        }
    }

    @Test
    void zuKurzesPasswortLiefert400()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "kurz@student.htw-berlin.de",
                                  "password": "abc",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void sommersemesterLiefert400()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sommer@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "SS 2025"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startsemester"));
    }

    @Test
    void startsemesterAusserhalbVon2000Bis2100Liefert400StattServerfehler()
            throws Exception {

        // "WS 1999" passt zum @Pattern, das Jahr prüft erst der SemesterService.
        // Ohne diese Prüfung käme der CHECK der Datenbank und damit ein 500.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jahr1999@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 1999"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startsemester"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "jahr2101@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2101"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startsemester"));

        // Es wurde niemand angelegt.
        assertThat(nutzendeRepository.findByEmail("jahr1999@student.htw-berlin.de")).isEmpty();
        assertThat(nutzendeRepository.findByEmail("jahr2101@student.htw-berlin.de")).isEmpty();
    }

    @Test
    void rolleWirdImmerAufStudierendeGesetzt()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "rolle@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025",
                                  "rolle": "PROFESSOR_IN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role")
                        .value("STUDIERENDE"));

        var nutzende = nutzendeRepository
                .findByEmail("rolle@student.htw-berlin.de")
                .orElseThrow();

        assertThat(nutzende.getRolle())
                .isEqualTo(Rolle.STUDIERENDE);
    }

    @Test
    void fehlendesSemesterWirdAngelegt()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "semester@student.htw-berlin.de",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2099"
                                }
                                """))
                .andExpect(status().isCreated());

        assertThat(semesterRepository
                .findByJahrAndArt(2099, SemesterArt.WS))
                .isPresent();
    }
}