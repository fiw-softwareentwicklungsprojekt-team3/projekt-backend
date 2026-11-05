package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Prüft den Zugriff nach Rolle: 401 ohne Token, 403 bei falscher Rolle.
 * Endpunkte, die es noch nicht gibt, antworten bei erlaubtem Zugriff mit 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RollenZugriffTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private String bearer(Rolle rolle) {
        Nutzende person = new Nutzende("rollen-test@example.com", "unused", rolle);
        person.setNutzendeId(1);
        return "Bearer " + jwtService.generateToken(person);
    }

    private void assertDurchgekommen(String pfad, Rolle rolle) throws Exception {
        int status = mockMvc.perform(get(pfad).header("Authorization", bearer(rolle)))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void studierendeKommenAufPlanDurch() throws Exception {
        assertDurchgekommen("/api/plan", Rolle.STUDIERENDE);
    }

    @Test
    void professorBekommtAufPlan403() throws Exception {
        mockMvc.perform(get("/api/plan").header("Authorization", bearer(Rolle.PROFESSOR_IN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ohneTokenBekommtAufPlan401() throws Exception {
        mockMvc.perform(get("/api/plan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studierendeBekommenAufStatistics403() throws Exception {
        mockMvc.perform(get("/api/statistics").header("Authorization", bearer(Rolle.STUDIERENDE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void professorKommtAufStatisticsDurch() throws Exception {
        assertDurchgekommen("/api/statistics", Rolle.PROFESSOR_IN);
    }

    @Test
    void beideRollenDuerfenModuleLesen() throws Exception {
        mockMvc.perform(get("/api/modules").header("Authorization", bearer(Rolle.STUDIERENDE)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/modules").header("Authorization", bearer(Rolle.PROFESSOR_IN)))
                .andExpect(status().isOk());
    }

    @Test
    void authMeBrauchtTokenAberKeineBestimmteRolle() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        assertDurchgekommen("/api/auth/me", Rolle.STUDIERENDE);
        assertDurchgekommen("/api/auth/me", Rolle.PROFESSOR_IN);
    }
}