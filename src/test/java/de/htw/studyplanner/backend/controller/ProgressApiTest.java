package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
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
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Integrationstests für {@code GET /api/progress} (US-58): ein Test pro Fall aus den
 * technischen Akzeptanzkriterien. Den Rollenzugriff (401/403) prüft {@link RollenZugriffTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProgressApiTest {

    /** Module der Fachsemester 1–3 (90 LP), Voraussetzung für die Bachelorarbeit. */
    private static final List<String> SEMESTER_1_BIS_3 = List.of(
            "B11", "B12", "B13", "B14", "B15", "B16",
            "B21", "B22", "B23", "B24", "B25", "B26",
            "B31", "B32", "B33", "B34", "B35");

    /** Module des 5. Fachsemesters (31 LP). */
    private static final List<String> SEMESTER_5 = List.of("B51", "B52", "B53", "B54", "B55", "B56");

    /** Anzahl der Pflichtmodule (art = P): alle 32 Module außer B41 und B54. */
    private static final int PFLICHTMODULE = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NutzendeRepository nutzendeRepository;

    @Autowired
    private StudierendeRepository studierendeRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private LeistungRepository leistungRepository;

    @Autowired
    private ModulRepository modulRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Eine angelegte Studierende: ID und fertiger Authorization-Header. */
    private record Konto(int id, String token) {
    }

    /** Legt eine Person mit Rolle STUDIERENDE und Startsemester WS 2026 an. */
    private Konto neueStudierende() {
        Nutzende person = new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende n = nutzendeRepository.save(new Nutzende(
                    "progress-" + UUID.randomUUID() + "@student.htw-berlin.de", "hash-egal", Rolle.STUDIERENDE));
            Semester ws2026 = semesterRepository.findByJahrAndArt(2026, SemesterArt.WS)
                    .orElseGet(() -> semesterRepository.save(new Semester(2026, SemesterArt.WS)));
            studierendeRepository.save(new Studierende(n, ws2026));
            return n;
        });
        return new Konto(person.getNutzendeId(), "Bearer " + jwtService.generateToken(person));
    }

    /** Trägt eine Leistung über {@code PUT /api/results/{moduleCode}} ein und erwartet 200. */
    private void leistung(Konto konto, String moduleCode, String body) throws Exception {
        mockMvc.perform(put("/api/results/" + moduleCode)
                        .header("Authorization", konto.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    /**
     * Speichert viele bestandene Leistungen direkt über das Repository (benotete mit 2,0).
     * Das ist kürzer als ein PUT pro Modul und umgeht die Pflichtfelder für Wahlangebote.
     */
    private void bestanden(Konto konto, List<String> moduleCodes) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Studierende studierende = studierendeRepository.findById(konto.id()).orElseThrow();
            for (String code : moduleCodes) {
                Modul modul = modulRepository.findById(code).orElseThrow();
                Leistung leistung = new Leistung(studierende, modul, Status.BESTANDEN);
                if (modul.isBenotet()) {
                    leistung.setNote(new BigDecimal("2.0"));
                }
                leistungRepository.save(leistung);
            }
        });
    }

    /** Ruft {@code GET /api/progress} auf, erwartet 200 und liefert die Antwort als JSON. */
    private JsonNode fortschritt(Konto konto) throws Exception {
        String json = mockMvc.perform(get("/api/progress").header("Authorization", konto.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json);
    }

    /** Sucht ein Modul in {@code openModules}; {@code null}, wenn es nicht offen ist. */
    private static JsonNode offenesModul(JsonNode antwort, String moduleCode) {
        for (JsonNode modul : antwort.get("openModules")) {
            if (modul.get("moduleCode").asText().equals(moduleCode)) {
                return modul;
            }
        }
        return null;
    }

    /** Die Sperrgründe eines offenen Moduls als Liste. */
    private static List<String> gruende(JsonNode antwort, String moduleCode) {
        List<String> gruende = new ArrayList<>();
        offenesModul(antwort, moduleCode).get("lockReasons").forEach(g -> gruende.add(g.asText()));
        return gruende;
    }

    /** Alle Codes aus {@code openModules} in der gelieferten Reihenfolge. */
    private static List<String> offeneCodes(JsonNode antwort) {
        List<String> codes = new ArrayList<>();
        antwort.get("openModules").forEach(m -> codes.add(m.get("moduleCode").asText()));
        return codes;
    }

    @Test
    void ohneLeistungen_nullLpKeinSchnittAllePflichtmoduleOffen() throws Exception {
        JsonNode antwort = fortschritt(neueStudierende());

        assertThat(antwort.get("earnedLp").asInt()).isZero();
        assertThat(antwort.get("totalLp").asInt()).isEqualTo(180);
        assertThat(antwort.get("progressPercent").asDouble()).isZero();
        assertThat(antwort.get("averageGrade").isNull()).isTrue();
        assertThat(antwort.get("bachelorThesisEligible").asBoolean()).isFalse();

        List<String> codes = offeneCodes(antwort);
        assertThat(codes).hasSize(PFLICHTMODULE)
                .doesNotContain("B41", "B54")
                .startsWith("B11", "B12", "B13", "B14", "B15", "B16", "B21")
                .endsWith("B65", "B66");

        JsonNode b11 = offenesModul(antwort, "B11");
        assertThat(b11.get("moduleName").asText()).isEqualTo("Programmierung 1");
        assertThat(b11.get("recommendedSemester").asInt()).isEqualTo(1);
        assertThat(b11.get("lp").asInt()).isEqualTo(6);
        assertThat(b11.get("resultStatus").isNull()).isTrue();
        assertThat(b11.get("locked").asBoolean()).isFalse();
        assertThat(gruende(antwort, "B11")).isEmpty();
    }

    @Test
    void gesperrtesModul_hatVerstaendlichenGrund() throws Exception {
        JsonNode antwort = fortschritt(neueStudierende());

        assertThat(offenesModul(antwort, "B34").get("locked").asBoolean()).isTrue();
        assertThat(gruende(antwort, "B34")).containsExactly("Programmierung 1 fehlt noch");

        assertThat(offenesModul(antwort, "B43").get("locked").asBoolean()).isTrue();
        assertThat(gruende(antwort, "B43")).containsExactly("Mindestens 60 LP nötig, du hast 0 LP");

        // B66: 17 Module aus Semester 1–3 und die LP-Grenze mit Toleranz.
        assertThat(gruende(antwort, "B66")).hasSize(18)
                .contains("Programmierung 1 fehlt noch",
                        "Mindestens 141 LP nötig (151 LP, ab 141 LP mit Zulassung durch den "
                                + "Prüfungsausschuss), du hast 0 LP");
    }

    @Test
    void nurBelegt_zaehltNichtUndBleibtOffen() throws Exception {
        Konto konto = neueStudierende();
        leistung(konto, "B11", """
                { "status": "BELEGT", "semester": "WS 2026" }""");

        JsonNode antwort = fortschritt(konto);

        assertThat(antwort.get("earnedLp").asInt()).isZero();
        assertThat(antwort.get("averageGrade").isNull()).isTrue();
        assertThat(offenesModul(antwort, "B11").get("resultStatus").asText()).isEqualTo("BELEGT");
        assertThat(offenesModul(antwort, "B11").get("locked").asBoolean()).isFalse();
        // Belegt ist nicht bestanden: B34 bleibt gesperrt.
        assertThat(gruende(antwort, "B34")).containsExactly("Programmierung 1 fehlt noch");
    }

    @Test
    void gemischteNoten_lpGewichtetUndAbgeschnitten() throws Exception {
        Konto konto = neueStudierende();
        leistung(konto, "B11", """
                { "status": "BESTANDEN", "grade": 1.3 }""");   // 6 LP
        leistung(konto, "B12", """
                { "status": "BESTANDEN", "grade": 2.7 }""");   // 5 LP
        leistung(konto, "B13", """
                { "status": "BESTANDEN", "grade": 4.0 }""");   // 5 LP
        leistung(konto, "B43", """
                { "status": "BESTANDEN" }""");                 // 20 LP, unbenotet
        leistung(konto, "B15", """
                { "status": "NICHT_BESTANDEN", "grade": 5.0, "attempt": 1 }""");

        JsonNode antwort = fortschritt(konto);

        // LP: 6 + 5 + 5 + 20 = 36 (B15 ist nicht bestanden), 36 / 180 = 20,0 %
        assertThat(antwort.get("earnedLp").asInt()).isEqualTo(36);
        assertThat(antwort.get("progressPercent").asDouble()).isEqualTo(20.0);
        // Schnitt: (6·1,3 + 5·2,7 + 5·4,0) / 16 = 41,3 / 16 = 2,58125 → abgeschnitten 2,5
        // (gerundet wäre es 2,6). B43 hat keine Note, B15 ist nicht bestanden.
        assertThat(antwort.get("averageGrade").decimalValue()).isEqualByComparingTo("2.5");

        assertThat(offeneCodes(antwort)).doesNotContain("B11", "B12", "B13", "B43").contains("B15");
        assertThat(offenesModul(antwort, "B15").get("resultStatus").asText()).isEqualTo("NICHT_BESTANDEN");
        // B11 ist bestanden: B34 ist jetzt frei.
        assertThat(offenesModul(antwort, "B34").get("locked").asBoolean()).isFalse();
    }

    @Test
    void b43OhneNote_zaehltLpAberNichtImSchnitt() throws Exception {
        Konto konto = neueStudierende();
        leistung(konto, "B43", """
                { "status": "BESTANDEN" }""");

        JsonNode antwort = fortschritt(konto);

        assertThat(antwort.get("earnedLp").asInt()).isEqualTo(20);
        assertThat(antwort.get("progressPercent").asDouble()).isEqualTo(11.1);
        assertThat(antwort.get("averageGrade").isNull()).isTrue();
    }

    @Test
    void bachelorarbeit_imToleranzbereichErlaubt() throws Exception {
        Konto konto = neueStudierende();
        // 90 (Sem. 1–3) + 20 (B43) + 31 (Sem. 5) = 141 LP: untere Grenze des Toleranzbereichs
        bestanden(konto, Stream.of(SEMESTER_1_BIS_3, List.of("B43"), SEMESTER_5).flatMap(List::stream).toList());

        JsonNode antwort = fortschritt(konto);

        assertThat(antwort.get("earnedLp").asInt()).isEqualTo(141);
        assertThat(antwort.get("bachelorThesisEligible").asBoolean()).isTrue();
        assertThat(offenesModul(antwort, "B66").get("locked").asBoolean()).isFalse();
        // Kolloquium braucht B66 und 177 LP.
        assertThat(gruende(antwort, "B65")).containsExactly(
                "Bachelorarbeit fehlt noch", "Mindestens 177 LP nötig, du hast 141 LP");
    }

    @Test
    void bachelorarbeit_unterToleranzGesperrt() throws Exception {
        Konto konto = neueStudierende();
        // wie oben, aber ohne B51: 136 LP
        bestanden(konto, Stream.of(SEMESTER_1_BIS_3, List.of("B43"),
                SEMESTER_5.subList(1, SEMESTER_5.size())).flatMap(List::stream).toList());

        JsonNode antwort = fortschritt(konto);

        assertThat(antwort.get("earnedLp").asInt()).isEqualTo(136);
        assertThat(antwort.get("bachelorThesisEligible").asBoolean()).isFalse();
        assertThat(gruende(antwort, "B66")).containsExactly(
                "Mindestens 141 LP nötig (151 LP, ab 141 LP mit Zulassung durch den "
                        + "Prüfungsausschuss), du hast 136 LP");
    }

    @Test
    void bachelorarbeit_genugLpAberModulAusSemester1bis3Fehlt() throws Exception {
        Konto konto = neueStudierende();
        // 84 (Sem. 1–3 ohne B11) + 5 (B41) + 20 (B43) + 31 (Sem. 5) + 14 (B61–B64) = 154 LP
        bestanden(konto, Stream.of(SEMESTER_1_BIS_3.subList(1, SEMESTER_1_BIS_3.size()),
                List.of("B41", "B43"), SEMESTER_5, List.of("B61", "B62", "B63", "B64"))
                .flatMap(List::stream).toList());

        JsonNode antwort = fortschritt(konto);

        assertThat(antwort.get("earnedLp").asInt()).isEqualTo(154);
        assertThat(antwort.get("bachelorThesisEligible").asBoolean()).isFalse();
        assertThat(gruende(antwort, "B66")).containsExactly("Programmierung 1 fehlt noch");
    }

    @Test
    void zweiKonten_sehenNurIhreEigenenDaten() throws Exception {
        Konto anna = neueStudierende();
        Konto ben = neueStudierende();
        leistung(anna, "B11", """
                { "status": "BESTANDEN", "grade": 1.0 }""");
        leistung(ben, "B12", """
                { "status": "BELEGT" }""");

        JsonNode fuerAnna = fortschritt(anna);
        JsonNode fuerBen = fortschritt(ben);

        assertThat(fuerAnna.get("earnedLp").asInt()).isEqualTo(6);
        assertThat(fuerAnna.get("averageGrade").decimalValue()).isEqualByComparingTo("1.0");
        assertThat(offeneCodes(fuerAnna)).doesNotContain("B11");
        assertThat(offenesModul(fuerAnna, "B12").get("resultStatus").isNull()).isTrue();

        assertThat(fuerBen.get("earnedLp").asInt()).isZero();
        assertThat(fuerBen.get("averageGrade").isNull()).isTrue();
        assertThat(offenesModul(fuerBen, "B11").get("resultStatus").isNull()).isTrue();
        assertThat(offenesModul(fuerBen, "B12").get("resultStatus").asText()).isEqualTo("BELEGT");
    }
}
