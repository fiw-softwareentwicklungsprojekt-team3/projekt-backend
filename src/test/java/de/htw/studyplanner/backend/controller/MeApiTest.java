package de.htw.studyplanner.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
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
 * Prüft GET /api/me: Studierende, Professor:in, ohne Token, ungültiges Token und
 * den Fall "Token gültig, Person gelöscht" (muss 401 sein, nicht 500).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MeApiTest {

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

    private String bearer(Nutzende person) {
        return "Bearer " + jwtService.generateToken(person);
    }

    /**
     * Legt eine Person mit Rolle STUDIERENDE samt Eintrag in {@code studierende} an.
     *
     * <p>Beides muss in einer Transaktion passieren: {@code Studierende} teilt sich die
     * ID mit {@code Nutzende} ({@code @MapsId}). Ohne Transaktion ist die gespeicherte
     * Person schon "detached", und das Speichern von {@code Studierende} schlägt mit
     * "detached entity passed to persist" fehl.
     */
    private Nutzende studierendeAnlegen(String email, String passwortHash) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende person = nutzendeRepository.save(
                    new Nutzende(email, passwortHash, Rolle.STUDIERENDE));
            studierendeRepository.save(new Studierende(person, semester(2026, SemesterArt.WS)));
            return person;
        });
    }

    /** Legt das Semester nur an, wenn es es noch nicht gibt (UNIQUE auf jahr + art). */
    private Semester semester(int jahr, SemesterArt art) {
        return semesterRepository.findByJahrAndArt(jahr, art)
                .orElseGet(() -> semesterRepository.save(new Semester(jahr, art)));
    }

    @Test
    void studierendeBekommenIdEmailRolleUndStartsemester() throws Exception {
        Nutzende person = studierendeAnlegen("me-studentin@example.com", "hash-egal");

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(person.getNutzendeId()))
                .andExpect(jsonPath("$.email").value("me-studentin@example.com"))
                .andExpect(jsonPath("$.role").value("STUDIERENDE"))
                .andExpect(jsonPath("$.startSemester").value("WS 2026"));
    }

    @Test
    void antwortEnthaeltNiemalsDenPasswortHash() throws Exception {
        Nutzende person = studierendeAnlegen("me-kein-hash@example.com", "streng-geheim");

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwortHash").doesNotExist())
                .andExpect(jsonPath("$.passwort_hash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void professorinBekommtDieselbenFelderMitStartsemesterNull() throws Exception {
        Nutzende person = nutzendeRepository.save(
                new Nutzende("me-prof@example.com", "hash-egal", Rolle.PROFESSOR_IN));

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(person.getNutzendeId()))
                .andExpect(jsonPath("$.email").value("me-prof@example.com"))
                .andExpect(jsonPath("$.role").value("PROFESSOR_IN"))
                .andExpect(jsonPath("$.startSemester").isEmpty());
    }

    @Test
    void ohneTokenGibtEs401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mitUngueltigemTokenGibtEs401() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer kaputtes.token.hier"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void geloeschtePersonGibt401UndNicht500() throws Exception {
        Nutzende person = nutzendeRepository.save(
                new Nutzende("me-geloescht@example.com", "hash-egal", Rolle.PROFESSOR_IN));
        String token = bearer(person);

        // Das Token bleibt bis zu einer Stunde gültig, die Person ist aber weg.
        nutzendeRepository.delete(person);

        mockMvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isString());
    }
}
