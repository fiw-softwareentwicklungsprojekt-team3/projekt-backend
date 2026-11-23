package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import jakarta.persistence.EntityManagerFactory;

/**
 * Integrationstests für {@code GET /api/plan} (US-38): ein Test pro Akzeptanzkriterium.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlanAbrufenApiTest {

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

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Legt eine Person mit Rolle STUDIERENDE und Startsemester WS 2026 an und liefert ihr Token.
     */
    private String neueStudierende() {
        Nutzende person = new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende n = nutzendeRepository.save(new Nutzende(
                    "plan-get-" + UUID.randomUUID() + "@student.htw-berlin.de", "hash-egal", Rolle.STUDIERENDE));
            Semester ws2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.WS)
                    .orElseGet(() -> semesterRepository.save(new Semester(2026, SemesterArt.WS)));
            studierendeRepository.save(new Studierende(n, ws2026));
            return n;
        });
        return "Bearer " + jwtService.generateToken(person);
    }

    /** Plant ein Modul über {@code POST /api/plan/entries} ein und erwartet 201. */
    private void einplanen(String token, String moduleCode, String semester, String offerCode) throws Exception {
        String offer = offerCode == null ? "null" : "\"" + offerCode + "\"";
        mockMvc.perform(post("/api/plan/entries")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moduleCode": "%s",
                                  "semester": "%s",
                                  "offerCode": %s,
                                  "courseCode": null,
                                  "courseTitle": null
                                }""".formatted(moduleCode, semester, offer)))
                .andExpect(status().isCreated());
    }

    /** Ruft {@code GET /api/plan} mit dem Token auf. */
    private ResultActions planAbrufen(String token) throws Exception {
        return mockMvc.perform(get("/api/plan").header("Authorization", token));
    }

    @Test
    void planIstNachSemesternGruppiertUndSortiertMitLpSummen() throws Exception {
        String token = neueStudierende();

        // Absichtlich in falscher Reihenfolge eingeplant
        einplanen(token, "B34", "WS 2027", null);  // 10 LP, Fachsemester 3
        einplanen(token, "B41", "SS 2027", "WP1"); //  5 LP, Fachsemester 4
        einplanen(token, "B11", "WS 2027", null);  //  6 LP, Fachsemester 1
        einplanen(token, "B21", "SS 2027", null);  //  5 LP, Fachsemester 2
        einplanen(token, "B16", "WS 2026", "EN_B2.1"); // 4 LP, Fachsemester 1

        planAbrufen(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startSemester").value("WS 2026"))
                .andExpect(jsonPath("$.totalLp").value(30))
                .andExpect(jsonPath("$.maxLpPerSemester").value(30))
                .andExpect(jsonPath("$.semesters", hasSize(3)))
                // Semester zeitlich sortiert: WS 2026 < SS 2027 < WS 2027
                .andExpect(jsonPath("$.semesters[0].semester").value("WS 2026"))
                .andExpect(jsonPath("$.semesters[0].lp").value(4))
                .andExpect(jsonPath("$.semesters[0].sws").value(4))
                .andExpect(jsonPath("$.semesters[0].lpLimitExceeded").value(false))
                .andExpect(jsonPath("$.semesters[1].semester").value("SS 2027"))
                .andExpect(jsonPath("$.semesters[1].lp").value(10))
                .andExpect(jsonPath("$.semesters[1].sws").value(8))
                .andExpect(jsonPath("$.semesters[2].semester").value("WS 2027"))
                .andExpect(jsonPath("$.semesters[2].lp").value(16))
                .andExpect(jsonPath("$.semesters[2].sws").value(12))
                // Innerhalb eines Semesters nach Modulcode sortiert
                .andExpect(jsonPath("$.semesters[1].entries[0].moduleCode").value("B21"))
                .andExpect(jsonPath("$.semesters[1].entries[1].moduleCode").value("B41"))
                .andExpect(jsonPath("$.semesters[2].entries[0].moduleCode").value("B11"))
                .andExpect(jsonPath("$.semesters[2].entries[1].moduleCode").value("B34"));
    }

    @Test
    void eintragHatAlleFelder() throws Exception {
        String token = neueStudierende();
        einplanen(token, "B16", "WS 2026", "EN_B2.1");
        einplanen(token, "B34", "WS 2027", null);

        planAbrufen(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[0].entries[0].id").isNumber())
                .andExpect(jsonPath("$.semesters[0].entries[0].moduleCode").value("B16"))
                .andExpect(jsonPath("$.semesters[0].entries[0].moduleName").value("1. Fremdsprache 1"))
                .andExpect(jsonPath("$.semesters[0].entries[0].lp").value(4))
                .andExpect(jsonPath("$.semesters[0].entries[0].recommendedSemester").value(1))
                .andExpect(jsonPath("$.semesters[0].entries[0].offerCode").value("EN_B2.1"))
                .andExpect(jsonPath("$.semesters[0].entries[0].offerName")
                        .value("Englisch Fachsprache B2.1 (Technik)"))
                .andExpect(jsonPath("$.semesters[0].entries[0].courseCode").isEmpty())
                .andExpect(jsonPath("$.semesters[0].entries[0].courseTitle").isEmpty())
                .andExpect(jsonPath("$.semesters[0].entries[0].prerequisitesMet").value(true))
                .andExpect(jsonPath("$.semesters[0].entries[0].violations", hasSize(0)))
                // Kein Feld "semester" im Eintrag, um Verwechslung mit dem Plansemester zu vermeiden
                .andExpect(jsonPath("$.semesters[0].entries[0].semester").doesNotExist())
                .andExpect(jsonPath("$.semesters[1].entries[0].moduleCode").value("B34"))
                .andExpect(jsonPath("$.semesters[1].entries[0].recommendedSemester").value(3))
                .andExpect(jsonPath("$.semesters[1].entries[0].offerCode").isEmpty())
                .andExpect(jsonPath("$.semesters[1].entries[0].offerName").isEmpty());
    }

    @Test
    void semesterOhneEintraegeErscheinenNicht() throws Exception {
        String token = neueStudierende();

        // SS 2027 bleibt leer: Die Lücke füllt das Frontend, nicht das Backend
        einplanen(token, "B11", "WS 2026", null);
        einplanen(token, "B34", "WS 2027", null);

        planAbrufen(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters", hasSize(2)))
                .andExpect(jsonPath("$.semesters[0].semester").value("WS 2026"))
                .andExpect(jsonPath("$.semesters[1].semester").value("WS 2027"));
    }

    @Test
    void leererPlanGibt200MitLeererListe() throws Exception {
        String token = neueStudierende();

        planAbrufen(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startSemester").value("WS 2026"))
                .andExpect(jsonPath("$.totalLp").value(0))
                .andExpect(jsonPath("$.maxLpPerSemester").value(30))
                .andExpect(jsonPath("$.semesters", hasSize(0)));
    }

    @Test
    void genauDreißigLpSindErlaubtUndSwsWerdenSummiert() throws Exception {
        String token = neueStudierende();

        einplanen(token, "B11", "WS 2026", null); // 6 LP, 6 SWS
        einplanen(token, "B12", "WS 2026", null); // 5 LP, 4 SWS
        einplanen(token, "B13", "WS 2026", null); // 5 LP, 4 SWS
        einplanen(token, "B14", "WS 2026", null); // 5 LP, 4 SWS
        einplanen(token, "B15", "WS 2026", null); // 5 LP, 4 SWS
        einplanen(token, "B16", "WS 2026", "EN_B2.1"); // 4 LP, 4 SWS

        planAbrufen(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxLpPerSemester").value(30))
                .andExpect(jsonPath("$.semesters[0].lp").value(30))
                .andExpect(jsonPath("$.semesters[0].sws").value(26))
                .andExpect(jsonPath("$.semesters[0].lpLimitExceeded").value(false));
    }

    @Test
    void einunddreißigLpLoesenWarnungAus() throws Exception {
        String token = neueStudierende();

        einplanen(token, "B11", "WS 2026", null); // 6 LP
        einplanen(token, "B12", "WS 2026", null); // 5 LP
        einplanen(token, "B13", "WS 2026", null); // 5 LP
        einplanen(token, "B14", "WS 2026", null); // 5 LP
        einplanen(token, "B15", "WS 2026", null); // 5 LP
        einplanen(token, "B31", "WS 2026", null); // 5 LP

        planAbrufen(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semesters[0].lp").value(31))
                .andExpect(jsonPath("$.semesters[0].lpLimitExceeded").value(true));
    }

    @Test
    void jedePersonSiehtNurIhrenEigenenPlan() throws Exception {
        String anna = neueStudierende();
        String ben = neueStudierende();

        einplanen(anna, "B11", "WS 2026", null);
        einplanen(ben, "B21", "SS 2027", null);
        einplanen(ben, "B34", "WS 2027", null);

        planAbrufen(anna)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLp").value(6))
                .andExpect(jsonPath("$.semesters", hasSize(1)))
                .andExpect(jsonPath("$.semesters[0].entries", hasSize(1)))
                .andExpect(jsonPath("$.semesters[0].entries[0].moduleCode").value("B11"));

        planAbrufen(ben)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLp").value(15))
                .andExpect(jsonPath("$.semesters", hasSize(2)))
                .andExpect(jsonPath("$.semesters[0].entries[0].moduleCode").value("B21"))
                .andExpect(jsonPath("$.semesters[1].entries[0].moduleCode").value("B34"));
    }

    @Test
    void professorinBekommt403() throws Exception {
        Nutzende prof = nutzendeRepository.save(new Nutzende(
                "plan-get-prof-" + UUID.randomUUID() + "@htw-berlin.de", "hash-egal", Rolle.PROFESSOR_IN));

        planAbrufen("Bearer " + jwtService.generateToken(prof))
                .andExpect(status().isForbidden());
    }

    @Test
    void ohneTokenGibtEs401() throws Exception {
        mockMvc.perform(get("/api/plan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void keineNPlusEinsAbfragen() throws Exception {
        String klein = neueStudierende();
        einplanen(klein, "B11", "WS 2026", null);

        String gross = neueStudierende();
        einplanen(gross, "B11", "WS 2026", null);
        einplanen(gross, "B16", "WS 2026", "EN_B2.1");
        einplanen(gross, "B21", "SS 2027", null);
        einplanen(gross, "B41", "SS 2027", "WP1");
        einplanen(gross, "B34", "WS 2027", null);
        einplanen(gross, "B54", "WS 2027", "WP2");

        long abfragenKlein = zaehleAbfragen(klein);
        long abfragenGross = zaehleAbfragen(gross);

        // Feste Anzahl, unabhängig von der Größe des Plans: Person mit Startsemester,
        // alle Einträge mit Modul, Semester und Wahlangebot (JOIN FETCH) sowie die
        // Abfragen des PlanPruefers (Plan, Leistungen, Voraussetzungen).
        assertThat(abfragenKlein).isLessThanOrEqualTo(5);
        assertThat(abfragenGross).isEqualTo(abfragenKlein);
    }

    /** Ruft den Plan ab und zählt die dabei vorbereiteten SQL-Statements. */
    private long zaehleAbfragen(String token) throws Exception {
        Statistics statistik = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistik.setStatisticsEnabled(true);
        try {
            statistik.clear();
            planAbrufen(token).andExpect(status().isOk());
            return statistik.getPrepareStatementCount();
        } finally {
            statistik.setStatisticsEnabled(false);
        }
    }
}
