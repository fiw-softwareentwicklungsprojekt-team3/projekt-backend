package de.htw.studyplanner.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.jayway.jsonpath.JsonPath;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Integrationstests für US-39: Planeintrag verschieben und entfernen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanAendernApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NutzendeRepository nutzendeRepository;
    @Autowired private StudierendeRepository studierendeRepository;
    @Autowired private SemesterRepository semesterRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;

    /** Legt eine Person mit Rolle STUDIERENDE und Startsemester WS 2026 an und liefert ihr Token. */
    private String neueStudierende() {
        Nutzende person = new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende n = nutzendeRepository.save(new Nutzende(
                    "plan-aendern-" + UUID.randomUUID() + "@student.htw-berlin.de", "hash-egal",
                    Rolle.STUDIERENDE));
            Semester ws2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.WS)
                    .orElseGet(() -> semesterRepository.save(new Semester(2026, SemesterArt.WS)));
            studierendeRepository.save(new Studierende(n, ws2026));
            return n;
        });
        return "Bearer " + jwtService.generateToken(person);
    }

    /** Plant B34 im WS 2027 ein und liefert die ID des Eintrags. */
    private int b34Einplanen(String token) throws Exception {
        String antwort = mockMvc.perform(post("/api/plan/entries")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "moduleCode": "B34", "semester": "WS 2027" }"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(antwort, "$.id");
    }

    private ResultActions putEntry(String token, int id, String json) throws Exception {
        return mockMvc.perform(put("/api/plan/entries/" + id)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

        private ResultActions deleteEntry(String token, int id) throws Exception {
        return mockMvc.perform(delete("/api/plan/entries/" + id)
                .header("Authorization", token));
    }

    @Test
    void verschiebenAntwortetMit200UndAendertDasSemester() throws Exception {
        String token = neueStudierende();
        int id = b34Einplanen(token);

        putEntry(token, id, """
                { "moduleCode": "B34", "semester": "WS 2028" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.semester").value("WS 2028"));

        mockMvc.perform(get("/api/plan").header("Authorization", token))
                .andExpect(jsonPath("$.semesters", hasSize(1)))
                .andExpect(jsonPath("$.semesters[0].semester").value("WS 2028"));
    }

    @Test
    void verschiebenVorDasStartsemesterGibt400() throws Exception {
        String token = neueStudierende();
        int id = b34Einplanen(token);

        putEntry(token, id, """
                { "moduleCode": "B34", "semester": "WS 2025" }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("semester"));
    }

    @Test
    void anderesModulImBodyGibt400() throws Exception {
        String token = neueStudierende();
        int id = b34Einplanen(token);

        putEntry(token, id, """
                { "moduleCode": "B33", "semester": "WS 2027" }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("moduleCode"));
    }

        @Test
    void loeschenAntwortetMit204UndEntferntDenEintrag() throws Exception {
        String token = neueStudierende();
        int id = b34Einplanen(token);

        deleteEntry(token, id).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/plan").header("Authorization", token))
                .andExpect(jsonPath("$.semesters", hasSize(0)));
    }

        @Test
    void fremdenEintragAendernOderLoeschenGibt404() throws Exception {
        String besitzerin = neueStudierende();
        String andere = neueStudierende();
        int id = b34Einplanen(besitzerin);

        putEntry(andere, id, """
                { "moduleCode": "B34", "semester": "WS 2028" }""")
                .andExpect(status().isNotFound());
        deleteEntry(andere, id).andExpect(status().isNotFound());

        // Der Eintrag der Besitzerin ist unverändert
        mockMvc.perform(get("/api/plan").header("Authorization", besitzerin))
                .andExpect(jsonPath("$.semesters[0].semester").value("WS 2027"));
    }

    @Test
    void nichtVorhandenerEintragGibtDieselbe404Meldung() throws Exception {
        String token = neueStudierende();

        deleteEntry(token, 999_999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Planeintrag mit der ID 999999 wurde nicht gefunden."));
    }
}