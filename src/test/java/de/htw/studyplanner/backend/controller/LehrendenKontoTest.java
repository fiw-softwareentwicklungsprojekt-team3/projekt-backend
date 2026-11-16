package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.service.LehrendenKontoInitialisierer;

/**
 * Tests für das Lehrenden-Konto, das beim Start aus Umgebungsvariablen angelegt wird.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LehrendenKontoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private StudierendeRepository studierendeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void kontoWirdAngelegtUndBeiVorhandenerEmailUebersprungen() {
        String email = "lehrende-" + UUID.randomUUID() + "@example.com";
        LehrendenKontoInitialisierer init = new LehrendenKontoInitialisierer(
                nutzendeRepository, passwordEncoder, email, "geheimesPasswort");

        assertThat(init.kontoAnlegen()).isTrue();
        assertThat(init.kontoAnlegen()).isFalse();

        Nutzende konto = nutzendeRepository.findByEmail(email).orElseThrow();
        assertThat(konto.getRolle()).isEqualTo(Rolle.PROFESSOR_IN);
        assertThat(konto.isSynthetic()).isFalse();
        assertThat(studierendeRepository.findById(konto.getNutzendeId())).isEmpty();
    }

    @Test
    void ohneWerteWirdNichtsAngelegt() {
        LehrendenKontoInitialisierer init = new LehrendenKontoInitialisierer(
                nutzendeRepository, passwordEncoder, "", "");

        assertThat(init.kontoAnlegen()).isFalse();
    }

    @Test
    void loginMitLehrendenKontoLiefertRollePROFESSORIN() throws Exception {
        String email = "lehrende-" + UUID.randomUUID() + "@example.com";
        new LehrendenKontoInitialisierer(
                nutzendeRepository, passwordEncoder, email, "geheimesPasswort").kontoAnlegen();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "geheimesPasswort"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSOR_IN"));
    }
}