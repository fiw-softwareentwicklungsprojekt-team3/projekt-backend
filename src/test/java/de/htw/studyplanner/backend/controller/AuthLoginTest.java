package de.htw.studyplanner.backend.controller;

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
                "login@example.com",
                passwordEncoder.encode("geheimesPasswort"),
                Rolle.STUDIERENDE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "geheimesPasswort"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.role").value("STUDIERENDE"))
                .andExpect(jsonPath("$.exiresIn").doesNotExist())
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
                "falsch@example.com",
                passwordEncoder.encode("richtigesPasswort"),
                Rolle.STUDIERENDE));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "falsch@example.com",
                                  "password": "falschesPasswort"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthOhneTokenLiefert200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}