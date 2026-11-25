package de.htw.studyplanner.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.security.JwtService;
import de.htw.studyplanner.backend.service.StatisticsService;

/**
 * Integrationstests für {@code GET /api/statistics/cohorts}:
 * - Professor_in erhält 200 mit erwartetem JSON-Schema
 * - Leere Datenbank / keine Daten liefert 200 mit []
 * - Studierende erhalten 403 Forbidden
 * - Ohne Token gibt es 401 Unauthorized
 * - Vollständige Anonymisierung (keine personenbezogenen Daten)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StatisticsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private StatisticsService statisticsService;

    private String tokenFuerRolle(Rolle rolle) {
        String domain = (rolle == Rolle.PROFESSOR_IN) ? "htw-berlin.de" : "student.htw-berlin.de";
        Nutzende person = nutzendeRepository.save(new Nutzende(
                "stats-" + UUID.randomUUID() + "@" + domain, "hash-egal", rolle));
        return "Bearer " + jwtService.generateToken(person);
    }
// Prüft, dass Professor_innen 200 OK mit dem erwarteten JSON-Schema erhalten
    @Test
    void professorinErhaelt200MitErwartetemJsonSchema() throws Exception {
        when(statisticsService.getCohortStatistics()).thenReturn(List.of(
                new CohortStatisticsDto("WS 2024", 45, 62.4, 34.7, 28, 0, 3)
        ));

        String token = tokenFuerRolle(Rolle.PROFESSOR_IN);

        mockMvc.perform(get("/api/statistics/cohorts")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cohort").value("WS 2024"))
                .andExpect(jsonPath("$[0].studentCount").value(45))
                .andExpect(jsonPath("$[0].averageLp").value(62.4))
                .andExpect(jsonPath("$[0].averageProgressPercent").value(34.7))
                .andExpect(jsonPath("$[0].completedSemesters1To3Count").value(28))
                .andExpect(jsonPath("$[0].bachelorThesisEligibleCount").value(0))
                .andExpect(jsonPath("$[0].bachelorThesisWithApprovalCount").value(3));
    }
// Prüft, dass eine leere Datenbank oder keine Daten 200 OK mit einer leeren Liste liefert
    @Test
    void keineStudierendenInDbLiefert200MitLeererListe() throws Exception {
        when(statisticsService.getCohortStatistics()).thenReturn(List.of());

        String token = tokenFuerRolle(Rolle.PROFESSOR_IN);

        mockMvc.perform(get("/api/statistics/cohorts")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
// Prüft, dass Studierende keinen Zugriff auf die Statistiken haben (403 Forbidden)
    @Test
    void studierendeErhalten403Forbidden() throws Exception {
        String token = tokenFuerRolle(Rolle.STUDIERENDE);

        mockMvc.perform(get("/api/statistics/cohorts")
                        .header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/statistics/cohorts"));
    }
// Prüft, dass ohne Token ein 401 Unauthorized zurückgegeben wird
    @Test
    void ohneTokenGibtEs401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/statistics/cohorts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/statistics/cohorts"));
    }
// Prüft, dass die Antwort vollständig anonymisiert ist (keine personenbezogenen Daten enthalten)
    @Test
    void antwortIstVollstaendigAnonymisiert() throws Exception {
        when(statisticsService.getCohortStatistics()).thenReturn(List.of(
                new CohortStatisticsDto("WS 2024", 45, 62.4, 34.7, 28, 0, 3)
        ));

        String token = tokenFuerRolle(Rolle.PROFESSOR_IN);

        mockMvc.perform(get("/api/statistics/cohorts")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nutzendeId").doesNotExist())
                .andExpect(jsonPath("$[0].studentId").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].vorname").doesNotExist())
                .andExpect(jsonPath("$[0].nachname").doesNotExist())
                .andExpect(jsonPath("$[0].leistungen").doesNotExist())
                .andExpect(jsonPath("$[0].einzelleistungen").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // Tests für GET /api/statistics/modules (US-55)
    // -------------------------------------------------------------------------

    @Test
    void professorinErhaelt200FuerModulstatistiken() throws Exception {
        when(statisticsService.getModuleStatistics(null)).thenReturn(List.of(
                new de.htw.studyplanner.backend.dto.ModuleStatisticsDto(
                        "B11", "Programmierung 1", 1, 140, 15, 8, 22, 3, new java.math.BigDecimal("2.3")),
                new de.htw.studyplanner.backend.dto.ModuleStatisticsDto(
                        "B43", "Fachpraktikum", 4, 95, 10, 0, 0, 0, null)
        ));

        String token = tokenFuerRolle(Rolle.PROFESSOR_IN);

        mockMvc.perform(get("/api/statistics/modules")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code").value("B11"))
                .andExpect(jsonPath("$[0].name").value("Programmierung 1"))
                .andExpect(jsonPath("$[0].semester").value(1))
                .andExpect(jsonPath("$[0].passedCount").value(140))
                .andExpect(jsonPath("$[0].enrolledCount").value(15))
                .andExpect(jsonPath("$[0].failedCount").value(8))
                .andExpect(jsonPath("$[0].repeatCount").value(22))
                .andExpect(jsonPath("$[0].criticalAttemptCount").value(3))
                .andExpect(jsonPath("$[0].averageGrade").value(2.3))
                .andExpect(jsonPath("$[1].code").value("B43"))
                .andExpect(jsonPath("$[1].averageGrade").doesNotExist());
    }

    @Test
    void professorinKannModulstatistikenNachKohorteFiltern() throws Exception {
        when(statisticsService.getModuleStatistics("WS 2023")).thenReturn(List.of(
                new de.htw.studyplanner.backend.dto.ModuleStatisticsDto(
                        "B11", "Programmierung 1", 1, 20, 2, 1, 3, 0, new java.math.BigDecimal("2.1"))
        ));

        String token = tokenFuerRolle(Rolle.PROFESSOR_IN);

        mockMvc.perform(get("/api/statistics/modules?cohort=WS 2023")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("B11"))
                .andExpect(jsonPath("$[0].passedCount").value(20));
    }

    @Test
    void modulstatistikenMitUngueltigemSemesterParameterLiefert400() throws Exception {
        when(statisticsService.getModuleStatistics("Ungueltig"))
                .thenThrow(new de.htw.studyplanner.backend.service.InvalidSemesterFormatException(
                        "cohort", "cohort muss das Format 'WS JJJJ' oder 'SS JJJJ' haben"));

        String token = tokenFuerRolle(Rolle.PROFESSOR_IN);

        mockMvc.perform(get("/api/statistics/modules?cohort=Ungueltig")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("cohort"))
                .andExpect(jsonPath("$.errors[0].message").isString());
    }

    @Test
    void studierendeErhalten403FuerModulstatistiken() throws Exception {
        String token = tokenFuerRolle(Rolle.STUDIERENDE);

        mockMvc.perform(get("/api/statistics/modules")
                        .header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.path").value("/api/statistics/modules"));
    }

    @Test
    void ohneTokenGibtEs401FuerModulstatistiken() throws Exception {
        mockMvc.perform(get("/api/statistics/modules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/statistics/modules"));
    }
}
