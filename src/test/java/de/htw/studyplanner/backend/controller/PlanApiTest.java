package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 * Integrationstests für die Plan-API ({@code POST /api/plan/entries} und {@code GET /api/plan}):
 * ein Test pro Regel aus dem Ticket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private StudierendeRepository studierendeRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Legt eine Person mit Rolle STUDIERENDE und Startsemester WS 2026 an und liefert ihr Token.
     */
    private String neueStudierende() {
        Nutzende person = new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende n = nutzendeRepository.save(new Nutzende(
                    "plan-" + UUID.randomUUID() + "@student.htw-berlin.de", "hash-egal", Rolle.STUDIERENDE));
            Semester ws2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.WS)
                    .orElseGet(() -> semesterRepository.save(new Semester(2026, SemesterArt.WS)));
            studierendeRepository.save(new Studierende(n, ws2026));
            return n;
        });
        return "Bearer " + jwtService.generateToken(person);
    }

    private ResultActions postEntry(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/plan/entries")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions getPlan(String token) throws Exception {
        return mockMvc.perform(get("/api/plan")
                .header("Authorization", token));
    }

    @Test
    void modulEinplanenAntwortetMit201UndGespeichertemEintragInklId() throws Exception {
        String token = neueStudierende();

        // B34 hat notwendige Voraussetzung B11; Voraussetzungen blockieren das Speichern nicht
        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.moduleCode").value("B34"))
                .andExpect(jsonPath("$.semester").value("WS 2027"))
                .andExpect(jsonPath("$.offerCode").isEmpty())
                .andExpect(jsonPath("$.courseCode").isEmpty())
                .andExpect(jsonPath("$.courseTitle").isEmpty());
    }

    @Test
    void professorinBekommt403() throws Exception {
        Nutzende prof = nutzendeRepository.save(new Nutzende(
                "plan-prof-" + UUID.randomUUID() + "@htw-berlin.de", "hash-egal", Rolle.PROFESSOR_IN));
        String token = "Bearer " + jwtService.generateToken(prof);

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isForbidden());
    }

    @Test
    void ohneTokenGibtEs401() throws Exception {
        mockMvc.perform(post("/api/plan/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleCode": "B34",
                                  "semester": "WS 2027"
                                }"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void falschesSemesterFormatGibt400MitSemesterInErrors() throws Exception {
        String token = neueStudierende();

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "Winter 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("semester"));
    }

    @Test
    void neuesSemesterWirdUeberFindenOderNeuAnlegenGespeichert() throws Exception {
        String token = neueStudierende();

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2035",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semester").value("WS 2035"));

        assertThat(semesterRepository.findByJahrAndArt(2035, SemesterArt.WS)).isPresent();
    }

    @Test
    void semesterVorStartsemesterGibt400MitMeldung() throws Exception {
        String token = neueStudierende();

        // Startsemester ist WS 2026 -> SS 2026 und WS 2025 liegen davor
        postEntry(token, """
                {
                  "moduleCode": "B11",
                  "semester": "WS 2025",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("semester"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Das Semester liegt vor deinem Startsemester WS 2026."));

        // Das Startsemester selbst ist planbar
        postEntry(token, """
                {
                  "moduleCode": "B11",
                  "semester": "WS 2026",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semester").value("WS 2026"));
    }

    @Test
    void unbekannterModuleCodeGibt404() throws Exception {
        String token = neueStudierende();

        postEntry(token, """
                {
                  "moduleCode": "B99",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isNotFound());
    }

    @Test
    void modulSchonImPlanGibt409MitHinweisAufsVerschieben() throws Exception {
        String token = neueStudierende();

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated());

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2028",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("verschieb")));
    }

    @Test
    void modulOhneAuswahlMitOfferCodeCourseCodeOderCourseTitleGibt400() throws Exception {
        String token = neueStudierende();

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": "WP1",
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": "KURS-1",
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("courseCode"));

        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": "Freier Titel"
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("courseTitle"));
    }

    @Test
    void unbekannterOfferCodeGibt400MitOfferCodeInErrors() throws Exception {
        String token = neueStudierende();

        postEntry(token, """
                {
                  "moduleCode": "B41",
                  "semester": "SS 2027",
                  "offerCode": "GIBTSNICHT",
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));
    }

    @Test
    void offerCodeMussZumModulPassen() throws Exception {
        String token = neueStudierende();

        // EN_B2.2 ist eine 1b-Sprache und gehört zu B26, nicht zu B16
        postEntry(token, """
                {
                  "moduleCode": "B16",
                  "semester": "WS 2026",
                  "offerCode": "EN_B2.2",
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));

        // Eine Sprache passt nicht zu einem Wahlpflichtmodul
        postEntry(token, """
                {
                  "moduleCode": "B41",
                  "semester": "SS 2027",
                  "offerCode": "EN_B2.1",
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));

        // B63 hat keine Angebotsliste, nur eine eigene Kursnummer
        postEntry(token, """
                {
                  "moduleCode": "B63",
                  "semester": "SS 2027",
                  "offerCode": "WP1",
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("offerCode"));

        // Das passende Angebot wird gespeichert
        postEntry(token, """
                {
                  "moduleCode": "B16",
                  "semester": "WS 2026",
                  "offerCode": "EN_B2.1",
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offerCode").value("EN_B2.1"));
    }

    @Test
    void beiAuswahlModulenSindOfferCodeCourseCodeUndCourseTitleOptional() throws Exception {
        String token = neueStudierende();

        // Komplett ohne Auswahl-Angaben einplanbar (B41 hat mit_auswahl = true, turnus = SS)
        postEntry(token, """
                {
                  "moduleCode": "B41",
                  "semester": "SS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleCode").value("B41"))
                .andExpect(jsonPath("$.offerCode").isEmpty())
                .andExpect(jsonPath("$.courseCode").isEmpty())
                .andExpect(jsonPath("$.courseTitle").isEmpty());

        // Mit offerCode und Kursangaben einplanbar (B54 hat mit_auswahl = true, turnus = WS)
        postEntry(token, """
                {
                  "moduleCode": "B54",
                  "semester": "WS 2027",
                  "offerCode": "WP1",
                  "courseCode": "WP-01",
                  "courseTitle": "Machine Learning"
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleCode").value("B54"))
                .andExpect(jsonPath("$.offerCode").value("WP1"))
                .andExpect(jsonPath("$.courseCode").value("WP-01"))
                .andExpect(jsonPath("$.courseTitle").value("Machine Learning"));
    }

    @Test
    void turnusVerstossGibt400MitSemesterInErrors() throws Exception {
        String token = neueStudierende();

        // B21 hat Turnus SS -> in WS 2027 verboten
        postEntry(token, """
                {
                  "moduleCode": "B21",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("semester"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("B21 wird nur im Sommersemester angeboten, geplant ist WS 2027."));

        // B34 hat Turnus WS -> in SS 2027 verboten
        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "SS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("semester"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("B34 wird nur im Wintersemester angeboten, geplant ist SS 2027."));

        // Das Jahr spielt keine Rolle: WS-Modul darf in WS 2026, WS 2027 oder WS 2028 liegen
        postEntry(token, """
                {
                  "moduleCode": "B11",
                  "semester": "WS 2028",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semester").value("WS 2028"));
    }

    @Test
    void b34OhneB11HatFehler_mitB11InFrueheremSemesterFehlerWeg() throws Exception {
        String token = neueStudierende();

        // 1. B34 für WS 2027 ohne B11 einplanen
        postEntry(token, """
                {
                  "moduleCode": "B34",
                  "semester": "WS 2027",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated());

        // 2. GET /api/plan aufrufen: B34 hat prerequisitesMet = false und FEHLER für B11
        getPlan(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].moduleCode").value("B34"))
                .andExpect(jsonPath("$[0].prerequisitesMet").value(false))
                .andExpect(jsonPath("$[0].violations[?(@.prerequisiteCode == 'B11')].severity")
                        .value(hasItem("FEHLER")))
                .andExpect(jsonPath("$[0].violations[?(@.prerequisiteCode == 'B11')].message")
                        .value(hasItem("Voraussetzung B11 für B34 ist nicht erfüllt.")));

        // 3. B11 in ein früheres Semester (WS 2026) einplanen
        postEntry(token, """
                {
                  "moduleCode": "B11",
                  "semester": "WS 2026",
                  "offerCode": null,
                  "courseCode": null,
                  "courseTitle": null
                }""")
                .andExpect(status().isCreated());

        // 4. GET /api/plan erneut aufrufen: FEHLER für B11 verschwunden, prerequisitesMet bei B34 jetzt true
        // (empfohlene Voraussetzungen B21 und B24 bleiben als WARNUNG in violations erhalten)
        getPlan(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.moduleCode == 'B34')].prerequisitesMet")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.moduleCode == 'B34')].violations[?(@.prerequisiteCode == 'B11')]")
                        .isEmpty())
                .andExpect(jsonPath("$[?(@.moduleCode == 'B34')].violations[?(@.severity == 'FEHLER')]")
                        .isEmpty())
                .andExpect(jsonPath("$[?(@.moduleCode == 'B34')].violations[?(@.prerequisiteCode == 'B21')].severity")
                        .value(hasItem("WARNUNG")))
                .andExpect(jsonPath("$[?(@.moduleCode == 'B34')].violations[?(@.prerequisiteCode == 'B24')].severity")
                        .value(hasItem("WARNUNG")));
    }
}
