package de.htw.studyplanner.backend.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Prüft die Wahlangebote im Modulkatalog ({@code offers} in {@code GET /api/modules/{code}})
 * und {@code GET /api/offers} gegen die echten Seed-Daten aus V2 (15 WP-Angebote,
 * 5 Sprachen mit 1a, 5 Sprachen mit 1b).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ModuleOffersApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    private MockHttpServletRequestBuilder authenticatedGet(String path) {
        Nutzende testUser = new Nutzende("offers-test@student.htw-berlin.de", "unused", Rolle.STUDIERENDE);
        testUser.setNutzendeId(1);
        return get(path).header("Authorization", "Bearer " + jwtService.generateToken(testUser));
    }

    // ---------------------------------------------------------------- Wahlpflicht

    @Test
    void b41HatAlle15WpAngeboteSortiertNachCode() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasChoice").value(true))
                .andExpect(jsonPath("$.offers", hasSize(15)))
                .andExpect(jsonPath("$.offers[*].group", everyItem(is("WP"))))
                // WP2 vor WP10: Zahlen werden als Zahl sortiert
                .andExpect(jsonPath("$.offers[0].code").value("WP1"))
                .andExpect(jsonPath("$.offers[1].code").value("WP2"))
                .andExpect(jsonPath("$.offers[9].code").value("WP10"))
                .andExpect(jsonPath("$.offers[14].code").value("WP15"));
    }

    @Test
    void angebotHatAlleFelder() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers[0].code").value("WP1"))
                .andExpect(jsonPath("$.offers[0].name").value("Machine Learning"))
                .andExpect(jsonPath("$.offers[0].group").value("WP"))
                .andExpect(jsonPath("$.offers[0].level").value("1b"))
                .andExpect(jsonPath("$.offers[0].sws").value(4))
                .andExpect(jsonPath("$.offers[0].recommendedModuleCode").value("B31"))
                // WP4 hat keine empfohlene Voraussetzung
                .andExpect(jsonPath("$.offers[3].code").value("WP4"))
                .andExpect(jsonPath("$.offers[3].recommendedModuleCode").doesNotExist());
    }

    @Test
    void b54HatDieselben15WpAngebote() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B54"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers", hasSize(15)))
                .andExpect(jsonPath("$.offers[*].group", everyItem(is("WP"))));
    }

    // ---------------------------------------------------------------- Sprachen

    @Test
    void b16HatNurSprachenMitNiveaustufe1a() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers", hasSize(5)))
                .andExpect(jsonPath("$.offers[*].group", everyItem(is("SPRACHE"))))
                .andExpect(jsonPath("$.offers[*].level", everyItem(is("1a"))))
                .andExpect(jsonPath("$.offers[*].code", hasItem("EN_B2.1")))
                .andExpect(jsonPath("$.offers[*].code", not(hasItem("EN_B2.2"))))
                .andExpect(jsonPath("$.offers[*].code",
                        contains("DAF_B2.2", "EN_B2.1", "ES_B1.2", "FR_B1.2", "RU_B1.2")));
    }

    @Test
    void b26HatNurSprachenMitNiveaustufe1b() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers", hasSize(5)))
                .andExpect(jsonPath("$.offers[*].level", everyItem(is("1b"))))
                .andExpect(jsonPath("$.offers[*].code", hasItem("EN_B2.2")))
                .andExpect(jsonPath("$.offers[*].code", not(hasItem("EN_B2.1"))));
    }

    // ---------------------------------------------------------------- ohne Liste

    @Test
    void aweModuleB63UndB64HabenKeineAngebote() throws Exception {
        // „freie Wahl“: nur eigene Kursnummer, keine feste Liste
        mockMvc.perform(authenticatedGet("/api/modules/B63"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasChoice").value(true))
                .andExpect(jsonPath("$.offers", hasSize(0)));
        mockMvc.perform(authenticatedGet("/api/modules/B64"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offers", hasSize(0)));
    }

    @Test
    void modulOhneAuswahlHatLeereListe() throws Exception {
        mockMvc.perform(authenticatedGet("/api/modules/B34"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasChoice").value(false))
                .andExpect(jsonPath("$.offers", hasSize(0)));
    }

    // ---------------------------------------------------------------- GET /api/offers

    @Test
    void alleAngeboteOhneFilter() throws Exception {
        mockMvc.perform(authenticatedGet("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(25)));
    }

    @Test
    void angeboteNachGruppeGefiltert() throws Exception {
        mockMvc.perform(authenticatedGet("/api/offers").param("group", "WP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(15)))
                .andExpect(jsonPath("$[0].code").value("WP1"))
                .andExpect(jsonPath("$[*].group", everyItem(is("WP"))));

        // Groß-/Kleinschreibung egal
        mockMvc.perform(authenticatedGet("/api/offers").param("group", "sprache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].group", everyItem(is("SPRACHE"))));
    }

    @Test
    void unbekannteGruppeLiefert400() throws Exception {
        mockMvc.perform(authenticatedGet("/api/offers").param("group", "AWE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("group"));
    }

    @Test
    void ohneTokenLiefert401() throws Exception {
        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isUnauthorized());
    }
}
