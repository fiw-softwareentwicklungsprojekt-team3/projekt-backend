package de.htw.studyplanner.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Prüft den Modulkatalog gegen die echten Seed-Daten aus V2 (32 Module).
 * Jeder Test läuft in einer Transaktion, die am Ende zurückgerollt wird.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ModuleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JwtService jwtService;

    private MockHttpServletRequestBuilder authenticatedGet(String path) {
        Nutzende testUser = new Nutzende(
                "module-test@example.com",
                "unused",
                Rolle.STUDIERENDE);
        testUser.setNutzendeId(1);

        return get(path).header(
                "Authorization",
                "Bearer " + jwtService.generateToken(testUser));
    }

    // ---------------------------------------------------------------- Liste

    @Test
    void listeEnthaeltAlleModuleSortiertNachSemesterUndCode() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(32)))
                .andExpect(jsonPath("$[0].code").value("B11"))
                .andExpect(jsonPath("$[5].code").value("B16"))   // letztes Modul in Semester 1
                .andExpect(jsonPath("$[6].code").value("B21"))   // erstes Modul in Semester 2
                .andExpect(jsonPath("$[31].code").value("B66"));
    }

    @Test
    void listeZeigtLpSwsTypUndTurnus() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules").param("fachsemester", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[3].code").value("B34"))
                .andExpect(jsonPath("$[3].name").value("Softwareentwicklungsprojekt"))
                .andExpect(jsonPath("$[3].lp").value(10))
                .andExpect(jsonPath("$[3].lectureHours").value(0))
                .andExpect(jsonPath("$[3].exerciseHours").value(6))
                .andExpect(jsonPath("$[3].type").value("P"))
                .andExpect(jsonPath("$[3].term").value("WS"));
    }

    // -------------------------------------------------------------- Filter

    @Test
    void filterNachSemester() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules").param("fachsemester", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("B31", "B32", "B33", "B34", "B35")));
    }

    @Test
    void filterNachTypWahlpflicht() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules").param("art", "WP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", contains("B41", "B54")));
    }

    @Test
    void filterNachTurnus() throws Exception {
        // Ungerades Fachsemester = WS (6 + 5 + 6 Module), gerades = SS (6 + 3 + 6 Module)
        mockMvc.perform(authenticatedGet("/api/modules").param("turnus", "WS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(17)));
        mockMvc.perform(authenticatedGet("/api/modules").param("turnus", "SS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)));
    }

    @Test
    void filterWerdenKombiniert() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules").param("fachsemester", "3").param("turnus", "SS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void modulMitTurnusBeideErscheintBeiWsUndSs() throws Exception {
        // Im Seed gibt es kein Modul mit Turnus BEIDE, deshalb wird hier eines angelegt.
        jdbc.update("INSERT INTO modul (code, bezeichnung, fachsemester, art, bereich, lp, "
                + "sws_vorlesung, sws_uebung, turnus) "
                + "VALUES ('TST1', 'Testmodul', 1, 'P', 'FACH', 5, 2, 2, 'BEIDE')");

        mockMvc.perform(authenticatedGet("/api/modules").param("turnus", "WS"))
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[*].code", hasItem("TST1")));
        mockMvc.perform(authenticatedGet("/api/modules").param("turnus", "SS"))
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[*].code", hasItem("TST1")));
    }

    @Test
    void ungueltigeFilterwerteLiefern400() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules").param("fachsemester", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("fachsemester")));
        mockMvc.perform(authenticatedGet("/api/modules").param("fachsemester", "7"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(authenticatedGet("/api/modules").param("fachsemester", "abc"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(authenticatedGet("/api/modules").param("art", "X"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("art")));
        mockMvc.perform(authenticatedGet("/api/modules").param("turnus", "SOMMER"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("turnus")));
        // BEIDE ist nur ein Wert der Module, kein erlaubter Filterwert
        mockMvc.perform(authenticatedGet("/api/modules").param("turnus", "BEIDE"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- Detail

    @Test
    void b34BrauchtB11() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B34"))
                .andExpect(jsonPath("$.name").value("Softwareentwicklungsprojekt"))
                .andExpect(jsonPath("$.semester").value(3))
                .andExpect(jsonPath("$.prerequisites", hasSize(3)))
                // notwendige zuerst, danach die empfohlenen nach Code
                .andExpect(jsonPath("$.prerequisites[0].kind").value("NOTWENDIG"))
                .andExpect(jsonPath("$.prerequisites[0].moduleCode").value("B11"))
                .andExpect(jsonPath("$.prerequisites[0].moduleName").value("Programmierung 1"))
                .andExpect(jsonPath("$.prerequisites[1].kind").value("EMPFOHLEN"))
                .andExpect(jsonPath("$.prerequisites[1].moduleCode").value("B21"))
                .andExpect(jsonPath("$.prerequisites[2].moduleCode").value("B24"));
    }

    @Test
    void modulOhneVoraussetzungenHatLeereListe() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prerequisites", hasSize(0)));
    }

    @Test
    void fachpraktikumBrauchtSechzigLeistungspunkte() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B43"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graded").value(false))
                .andExpect(jsonPath("$.countsForFinalGrade").value(false))
                .andExpect(jsonPath("$.prerequisites", hasSize(1)))
                .andExpect(jsonPath("$.prerequisites[0].kind").value("NOTWENDIG"))
                .andExpect(jsonPath("$.prerequisites[0].minLp").value(60))
                .andExpect(jsonPath("$.prerequisites[0].moduleCode").doesNotExist());
    }

    @Test
    void bachelorarbeitHatModulUndLpVoraussetzungen() throws Exception {
        // 17 Module aus den ersten drei Semestern und die Grenze von 151 LP (Toleranz 10)
        mockMvc.perform(authenticatedGet("/api/modules/B66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prerequisites", hasSize(18)))
                .andExpect(jsonPath("$.prerequisites[0].moduleCode").value("B11"))
                .andExpect(jsonPath("$.prerequisites[17].minLp").value(151))
                .andExpect(jsonPath("$.prerequisites[17].toleranceLp").value(10));
    }

    @Test
    void unbekannterCodeLiefert404() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/X99"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason(containsString("X99")));
    }
}
