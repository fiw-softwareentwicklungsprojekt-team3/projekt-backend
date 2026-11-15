package de.htw.studyplanner.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.repository.NutzendeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void erfolgreicherLoginLiefertDieExakteResponseStruktur() throws Exception {
        nutzendeRepository.save(new Nutzende(
                "login@student.htw-berlin.de",
                passwordEncoder.encode("geheimesPasswort"),
                Rolle.STUDIERENDE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@student.htw-berlin.de",
                                  "password": "geheimesPasswort"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.role").value("STUDIERENDE"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void geschuetzterEndpunktOhneTokenLiefert401() throws Exception {
        mockMvc.perform(get("/api/modules"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void ungueltigerTokenGibt401() throws Exception {
        mockMvc.perform(get("/api/modules")
                .header("Authorization", "Bearer ungueltiger.token.string"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    void loginMitFalschemPasswortGibt401() throws Exception {
        nutzendeRepository.save(new Nutzende(
                "falsch@student.htw-berlin.de",
                passwordEncoder.encode("richtigesPasswort"),
                Rolle.STUDIERENDE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "falsch@student.htw-berlin.de",
                                  "password": "falschesPasswort"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIgnoriertGrossUndKleinschreibungDerEmail() throws Exception {
        nutzendeRepository.save(new Nutzende(
                "gross@student.htw-berlin.de",
                passwordEncoder.encode("geheimesPasswort"),
                Rolle.STUDIERENDE));

        // Das Konto ist klein gespeichert, der Login kommt mit Großbuchstaben.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Gross@Student.HTW-Berlin.DE",
                                  "password": "geheimesPasswort"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void lehrendeMeldenSichMitHtwAdresseAn() throws Exception {
        nutzendeRepository.save(new Nutzende(
                "login-lehrende@htw-berlin.de",
                passwordEncoder.encode("geheimesPasswort"),
                Rolle.PROFESSOR_IN));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "Login-Lehrende@HTW-Berlin.de",
                                  "password": "geheimesPasswort"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSOR_IN"));
    }

    @Test
    void loginMitUngueltigerEmailLiefert400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "keine-email",
                                  "password": "geheimesPasswort"
                                }
                                """))
                .andExpect(status().isBadRequest())
                // Nur der Format-Fehler, nicht zusätzlich der Domain-Fehler.
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("ungültig")));
    }

    @Test
    void loginMitGueltigerEmailOhneHtwDomainLiefert400() throws Exception {
        // Dieses Konto gibt es sogar mit richtigem Passwort. Trotzdem kommt 400 und nicht 200,
        // weil die Domain schon vor der Suche nach dem Konto geprüft wird.
        nutzendeRepository.save(new Nutzende(
                "login-fremd@gmail.com",
                passwordEncoder.encode("geheimesPasswort"),
                Rolle.STUDIERENDE));

        String[] emails = {
                "login-fremd@gmail.com",
                "login-trick@fakestudent.htw-berlin.de",
                "htw-berlin.de@gmail.com"
        };

        for (String email : emails) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "%s",
                                      "password": "geheimesPasswort"
                                    }
                                    """.formatted(email)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].message")
                            .value("Email muss auf @student.htw-berlin.de oder @htw-berlin.de enden"));
        }
    }

    @Test
    void healthOhneTokenLiefert200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}