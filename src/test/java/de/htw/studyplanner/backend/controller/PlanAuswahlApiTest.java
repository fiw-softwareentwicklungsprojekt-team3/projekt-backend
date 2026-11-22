package de.htw.studyplanner.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.repository.WahlangebotRepository;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Integrationstests für die Auswahl-Regeln zwischen zwei Modulen (US-41, umgesetzt in US-39):
 * gleiche Sprache bei B16/B26 und kein gemeinsames WP-Angebot bei B41/B54,
 * jeweils gegen den eigenen Plan und gegen bestandene Leistungen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanAuswahlApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NutzendeRepository nutzendeRepository;
    @Autowired private StudierendeRepository studierendeRepository;
    @Autowired private SemesterRepository semesterRepository;
    @Autowired private ModulRepository modulRepository;
    @Autowired private WahlangebotRepository wahlangebotRepository;
    @Autowired private LeistungRepository leistungRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;

    /** Eine angelegte Person: Token für die Anfragen und ID für Leistungen. */
    private record Person(String token, Integer id) {
    }

    /** Legt eine Person mit Rolle STUDIERENDE und Startsemester WS 2026 an. */
    private Person neueStudierende() {
        Nutzende person = new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende n = nutzendeRepository.save(new Nutzende(
                    "plan-auswahl-" + UUID.randomUUID() + "@student.htw-berlin.de", "hash-egal",
                    Rolle.STUDIERENDE));
            Semester ws2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.WS)
                    .orElseGet(() -> semesterRepository.save(new Semester(2026, SemesterArt.WS)));
            studierendeRepository.save(new Studierende(n, ws2026));
            return n;
        });
        return new Person("Bearer " + jwtService.generateToken(person), person.getNutzendeId());
    }

    /** Speichert eine bestandene Leistung mit Angebot, z. B. B16 mit EN_B2.1. */
    private void bestanden(Person person, String modulCode, String offerCode) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Leistung leistung = new Leistung(
                    studierendeRepository.findById(person.id()).orElseThrow(),
                    modulRepository.findById(modulCode).orElseThrow(),
                    Status.BESTANDEN);
            leistung.setWahlangebot(wahlangebotRepository.findById(offerCode).orElseThrow());
            leistung.setNote(new BigDecimal("2.0"));
            leistung.setAnzahlVersuche(1);
            leistungRepository.save(leistung);
        });
    }

    /** Plant ein Modul ein; {@code offerCode} darf null sein. */
    private ResultActions einplanen(Person person, String modulCode, String semester, String offerCode)
            throws Exception {
        String angebot = offerCode == null ? "null" : "\"" + offerCode + "\"";
        return mockMvc.perform(post("/api/plan/entries")
                .header("Authorization", person.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "moduleCode": "%s", "semester": "%s", "offerCode": %s }"""
                        .formatted(modulCode, semester, angebot)));
    }

    private int id(ResultActions antwort) throws Exception {
        return JsonPath.read(antwort.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void verschiebenMitAndererSpracheAlsB16ImPlanGibt400() throws Exception {
        Person person = neueStudierende();
        einplanen(person, "B16", "WS 2026", "FR_B1.2").andExpect(status().isCreated());
        int b26 = id(einplanen(person, "B26", "SS 2027", null).andExpect(status().isCreated()));

        mockMvc.perform(put("/api/plan/entries/" + b26)
                        .header("Authorization", person.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "moduleCode": "B26", "semester": "SS 2028", "offerCode": "EN_B2.2" }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));
    }

    @Test
    void andereSpracheAlsBestandenesB16Gibt400() throws Exception {
        Person person = neueStudierende();
        bestanden(person, "B16", "EN_B2.1");

        einplanen(person, "B26", "SS 2027", "FR_B2.1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));
    }

    @Test
    void gleicheSpracheWieB16IstErlaubt() throws Exception {
        Person person = neueStudierende();
        bestanden(person, "B16", "EN_B2.1");

        einplanen(person, "B26", "SS 2027", "EN_B2.2").andExpect(status().isCreated());
    }

        @Test
    void gleichesWpAngebotWieB41ImPlanGibt400() throws Exception {
        Person person = neueStudierende();
        einplanen(person, "B41", "SS 2027", "WP7").andExpect(status().isCreated());

        einplanen(person, "B54", "WS 2027", "WP7")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));
    }

    @Test
    void gleichesWpAngebotWieBestandenesB41Gibt400() throws Exception {
        Person person = neueStudierende();
        bestanden(person, "B41", "WP3");

        einplanen(person, "B54", "WS 2027", "WP3")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));
    }
}