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
                                  "email": "erfolg@example.com",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("erfolg@example.com"))
                .andExpect(jsonPath("$.rolle")
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
                                  "email": "bcrypt@example.com",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isCreated());

        var nutzende = nutzendeRepository
                .findByEmail("bcrypt@example.com")
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
                  "email": "doppelt@example.com",
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
                .andExpect(jsonPath("$.field").value("email"));
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
                .andExpect(jsonPath("$.field").value("email"));
    }

    @Test
    void zuKurzesPasswortLiefert400()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "kurz@example.com",
                                  "password": "abc",
                                  "startsemester": "WS 2025"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("password"));
    }

    @Test
    void sommersemesterLiefert400()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "sommer@example.com",
                                  "password": "geheimesPasswort",
                                  "startsemester": "SS 2025"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("startsemester"));
    }

    @Test
    void rolleWirdImmerAufStudierendeGesetzt()
            throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "rolle@example.com",
                                  "password": "geheimesPasswort",
                                  "startsemester": "WS 2025",
                                  "rolle": "PROFESSOR_IN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rolle")
                        .value("STUDIERENDE"));

        var nutzende = nutzendeRepository
                .findByEmail("rolle@example.com")
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
                                  "email": "semester@example.com",
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
