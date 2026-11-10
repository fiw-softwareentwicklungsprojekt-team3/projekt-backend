package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Integrationstests für /api/results: jede Regel aus dem Ticket, Wiederholung,
 * zwei Accounts und Zugriff nach Rolle.
 *
 * <p>Jeder Test legt eine eigene Person mit Startsemester WS 2026 an, damit sich die
 * Tests nicht gegenseitig beeinflussen. Module aus dem StuPO-Seed (V2):
 * B11 (benotet, WS), B43 (unbenotet), B16/B26 (Sprache 1a/1b), B41 (WP), B63 (AWE).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResultApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private StudierendeRepository studierendeRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private LeistungRepository leistungRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // ---------------------------------------------------------------- Hilfen

    /**
     * Legt eine Person mit Rolle STUDIERENDE und Startsemester WS 2026 an und liefert ihr Token.
     * Person und Studierende in einer Transaktion, weil Studierende die ID übernimmt (@MapsId).
     */
    private String neueStudierende() {
        Nutzende person = new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende n = nutzendeRepository.save(new Nutzende(
                    "results-" + UUID.randomUUID() + "@example.com", "hash-egal", Rolle.STUDIERENDE));
            Semester ws2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.WS)
                    .orElseGet(() -> semesterRepository.save(new Semester(2026, SemesterArt.WS)));
            studierendeRepository.save(new Studierende(n, ws2026));
            return n;
        });
        return "Bearer " + jwtService.generateToken(person);
    }

    private ResultActions put(String token, String modul, String json) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/results/" + modul)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private void erwarteFehler(ResultActions antwort, String feld) throws Exception {
        antwort.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(feld));
    }

    // ---------------------------------------------------------------- Endpunkte

    @Test
    void bestandenEintragenUndLesen() throws Exception {
        String token = neueStudierende();

        put(token, "B11", """
                { "status": "BESTANDEN", "semester": "WS 2026", "grade": 2.3 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleCode").value("B11"))
                .andExpect(jsonPath("$.status").value("BESTANDEN"))
                .andExpect(jsonPath("$.semester").value("WS 2026"))
                .andExpect(jsonPath("$.grade").value(2.3))
                .andExpect(jsonPath("$.attempts").value(0));

        mockMvc.perform(get("/api/results").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].moduleCode").value("B11"))
                .andExpect(jsonPath("$[0].grade").value(2.3));
    }

    @Test
    void ohneLeistungenIstDieListeLeer() throws Exception {
        mockMvc.perform(get("/api/results").header("Authorization", neueStudierende()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void loeschenGibt204UndDieLeistungIstWeg() throws Exception {
        String token = neueStudierende();
        put(token, "B11", """
                { "status": "BELEGT", "semester": "WS 2026" }""").andExpect(status().isOk());

        mockMvc.perform(delete("/api/results/B11").header("Authorization", token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/results").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unbekanntesModulGibt404() throws Exception {
        String token = neueStudierende();
        put(token, "XYZ", """
                { "status": "BELEGT" }""").andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/results/XYZ").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- Wiederholung und Versuche

    @Test
    void wiederholungAendertDieZeileUndSpeichertDenVersuch() throws Exception {
        String token = neueStudierende();

        put(token, "B11", """
                { "status": "BELEGT", "semester": "WS 2026" }""").andExpect(status().isOk());
        put(token, "B11", """
                { "status": "NICHT_BESTANDEN", "semester": "WS 2026", "grade": 5.0, "attempt": 1 }""")
                .andExpect(jsonPath("$.attempts").value(1));
        put(token, "B11", """
                { "status": "NICHT_BESTANDEN", "semester": "WS 2027", "grade": 5.0, "attempt": 2 }""")
                .andExpect(jsonPath("$.attempts").value(2));
        // BESTANDEN ohne attempt: Versuchszahl bleibt unverändert
        put(token, "B11", """
                { "status": "BESTANDEN", "semester": "WS 2028", "grade": 2.0 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BESTANDEN"))
                .andExpect(jsonPath("$.semester").value("WS 2028"))
                .andExpect(jsonPath("$.attempts").value(2));

        // genau eine Zeile für B11
        mockMvc.perform(get("/api/results").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void nichtBestandenOhneAttemptGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0 }"""), "attempt");
    }

    @Test
    void mehrAls4VersucheGibt400() throws Exception {
        String token = neueStudierende();
        erwarteFehler(put(token, "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0, "attempt": 5 }"""), "attempt");
        erwarteFehler(put(token, "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0, "attempt": 0 }"""), "attempt");
        put(token, "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0, "attempt": 4 }""").andExpect(status().isOk());
    }

    @Test
    void attemptBeiBestandenOderBelegtGibt400() throws Exception {
        String token = neueStudierende();
        erwarteFehler(put(token, "B11", """
                { "status": "BESTANDEN", "grade": 2.0, "attempt": 1 }"""), "attempt");
        erwarteFehler(put(token, "B11", """
                { "status": "BELEGT", "attempt": 1 }"""), "attempt");
    }

    // ---------------------------------------------------------------- Noten

    @Test
    void notenAlsGanzeZahlWerdenAkzeptiert() throws Exception {
        // JavaScript schickt 5.0 als 5 und 4.0 als 4
        String token = neueStudierende();
        put(token, "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 5, "attempt": 1 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(5.0));
        put(token, "B11", """
                { "status": "BESTANDEN", "grade": 4 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(4.0));
    }

    @Test
    void nichtErlaubteNoteGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BESTANDEN", "grade": 2.5 }"""), "grade");
    }

    @Test
    void bestandenMitNoteUeber4Gibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BESTANDEN", "grade": 5.0 }"""), "grade");
    }

    @Test
    void bestandenOhneNoteGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BESTANDEN" }"""), "grade");
    }

    @Test
    void nichtBestandenMitAndererNoteAls5Gibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 4.0, "attempt": 1 }"""), "grade");
    }

    @Test
    void belegtMitNoteGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BELEGT", "grade": 2.0 }"""), "grade");
    }

    @Test
    void unbenotetesModulHatNieEineNote() throws Exception {
        String token = neueStudierende();
        erwarteFehler(put(token, "B43", """
                { "status": "BESTANDEN", "grade": 2.0 }"""), "grade");
        erwarteFehler(put(token, "B43", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0, "attempt": 1 }"""), "grade");
        put(token, "B43", """
                { "status": "BESTANDEN", "semester": "SS 2028" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").doesNotExist());
        put(token, "B43", """
                { "status": "NICHT_BESTANDEN", "semester": "SS 2028", "attempt": 1 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").doesNotExist())
                .andExpect(jsonPath("$.attempts").value(1));
    }

    // ---------------------------------------------------------------- Semester

    @Test
    void semesterDarfLeerSein() throws Exception {
        put(neueStudierende(), "B11", """
                { "status": "BESTANDEN", "grade": 4.0 }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semester").doesNotExist());
    }

    @Test
    void semesterVorDemStartsemesterGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BESTANDEN", "semester": "SS 2026", "grade": 2.0 }"""), "semester");
    }

    @Test
    void ungueltigesSemesterFormatGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BESTANDEN", "semester": "Winter 26", "grade": 2.0 }"""), "semester");
    }

    // ---------------------------------------------------------------- Wahlangebote und Kurse

    @Test
    void wahlpflichtBrauchtEinWpAngebot() throws Exception {
        String token = neueStudierende();
        erwarteFehler(put(token, "B41", """
                { "status": "BELEGT" }"""), "offerCode");
        erwarteFehler(put(token, "B41", """
                { "status": "BELEGT", "offerCode": "EN_B2.1" }"""), "offerCode");
        erwarteFehler(put(token, "B41", """
                { "status": "BELEGT", "offerCode": "GIBTSNICHT" }"""), "offerCode");
        put(token, "B41", """
                { "status": "BELEGT", "offerCode": "WP1" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offerCode").value("WP1"));
    }

    @Test
    void spracheMussZurNiveaustufePassen() throws Exception {
        String token = neueStudierende();
        // EN_B2.1 ist Niveau 1a → passt zu B16, nicht zu B26
        put(token, "B16", """
                { "status": "BELEGT", "offerCode": "EN_B2.1" }""").andExpect(status().isOk());
        erwarteFehler(put(token, "B26", """
                { "status": "BELEGT", "offerCode": "EN_B2.1" }"""), "offerCode");
        put(token, "B26", """
                { "status": "BELEGT", "offerCode": "EN_B2.2" }""").andExpect(status().isOk());
        erwarteFehler(put(token, "B16", """
                { "status": "BELEGT", "offerCode": "WP1" }"""), "offerCode");
    }

    @Test
    void angebotBeiModulOhneAuswahlGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BELEGT", "offerCode": "WP1" }"""), "offerCode");
    }

    @Test
    void aweModulBrauchtCourseCode() throws Exception {
        String token = neueStudierende();
        erwarteFehler(put(token, "B63", """
                { "status": "BELEGT", "courseTitle": "Rhetorik" }"""), "courseCode");
        put(token, "B63", """
                { "status": "BELEGT", "courseCode": "AWE-123", "courseTitle": "Rhetorik" }""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCode").value("AWE-123"))
                .andExpect(jsonPath("$.courseTitle").value("Rhetorik"));
    }

    @Test
    void kursBeiAnderemModulGibt400() throws Exception {
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BELEGT", "courseCode": "AWE-123" }"""), "courseCode");
        erwarteFehler(put(neueStudierende(), "B11", """
                { "status": "BELEGT", "courseTitle": "Rhetorik" }"""), "courseTitle");
    }

    // ---------------------------------------------------------------- Zwei Accounts und Rollen

    @Test
    void jedePersonSiehtNurIhreLeistungen() throws Exception {
        String anna = neueStudierende();
        String ben = neueStudierende();

        put(anna, "B11", """
                { "status": "BESTANDEN", "grade": 1.3 }""").andExpect(status().isOk());
        put(ben, "B11", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0, "attempt": 1 }""").andExpect(status().isOk());

        mockMvc.perform(get("/api/results").header("Authorization", anna))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("BESTANDEN"))
                .andExpect(jsonPath("$[0].grade").value(1.3));

        // Ben löscht "seine" B11 – Annas Leistung bleibt
        mockMvc.perform(delete("/api/results/B11").header("Authorization", ben))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/results").header("Authorization", anna))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/results").header("Authorization", ben))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void professorinBekommt403() throws Exception {
        Nutzende prof = nutzendeRepository.save(new Nutzende(
                "results-prof-" + UUID.randomUUID() + "@example.com", "hash-egal", Rolle.PROFESSOR_IN));
        String token = "Bearer " + jwtService.generateToken(prof);

        mockMvc.perform(get("/api/results").header("Authorization", token))
                .andExpect(status().isForbidden());
        put(token, "B11", """
                { "status": "BELEGT" }""").andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/results/B11").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void ohneTokenGibt401() throws Exception {
        mockMvc.perform(get("/api/results")).andExpect(status().isUnauthorized());
    }

    @Test
    void leistungWirdMitEinerNachkommastelleGespeichert() throws Exception {
        String token = neueStudierende();
        put(token, "B11", """
                { "status": "BESTANDEN", "grade": 3 }""").andExpect(status().isOk());
        assertThat(leistungRepository.findAll().stream()
                .anyMatch(l -> l.getNote() != null && l.getNote().scale() == 1
                        && l.getNote().compareTo(new java.math.BigDecimal("3.0")) == 0))
                .isTrue();
    }
}
