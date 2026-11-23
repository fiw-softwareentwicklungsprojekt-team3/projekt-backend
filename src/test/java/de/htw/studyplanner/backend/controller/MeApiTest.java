package de.htw.studyplanner.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.PlanEintrag;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.PlanEintragRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.JwtService;

/**
 * Prüft GET /api/me und PUT /api/me: Studierende, Professor:in, ohne Token,
 * ungültiges Token, gelöschte Person sowie Startsemester-Änderung inkl. Prüfungen.
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
    private ModulRepository modulRepository;

    @Autowired
    private PlanEintragRepository planEintragRepository;

    @Autowired
    private LeistungRepository leistungRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String bearer(Nutzende person) {
        return "Bearer " + jwtService.generateToken(person);
    }

    private Nutzende studierendeAnlegen(String email, String passwortHash) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Nutzende person = nutzendeRepository.save(
                    new Nutzende(email, passwortHash, Rolle.STUDIERENDE));
            studierendeRepository.save(new Studierende(person, semester(2026, SemesterArt.WS)));
            return person;
        });
    }

    private Semester semester(int jahr, SemesterArt art) {
        return semesterRepository.findByJahrAndArt(jahr, art)
                .orElseGet(() -> semesterRepository.save(new Semester(jahr, art)));
    }

    private Modul irgendeinModul() {
        return modulRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Keine Module in Test-DB gefunden"));
    }

    @Test
    void studierendeBekommenIdEmailRolleUndStartsemester() throws Exception {
        Nutzende person = studierendeAnlegen("me-studentin@student.htw-berlin.de", "hash-egal");

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(person.getNutzendeId()))
                .andExpect(jsonPath("$.email").value("me-studentin@student.htw-berlin.de"))
                .andExpect(jsonPath("$.role").value("STUDIERENDE"))
                .andExpect(jsonPath("$.startSemester").value("WS 2026"));
    }

    @Test
    void antwortEnthaeltNiemalsDenPasswortHash() throws Exception {
        Nutzende person = studierendeAnlegen("me-kein-hash@student.htw-berlin.de", "streng-geheim");

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwortHash").doesNotExist())
                .andExpect(jsonPath("$.passwort_hash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void professorinBekommtDieselbenFelderMitStartsemesterNull() throws Exception {
        Nutzende person = nutzendeRepository.save(
                new Nutzende("me-prof@htw-berlin.de", "hash-egal", Rolle.PROFESSOR_IN));

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(person.getNutzendeId()))
                .andExpect(jsonPath("$.email").value("me-prof@htw-berlin.de"))
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
                new Nutzende("me-geloescht@htw-berlin.de", "hash-egal", Rolle.PROFESSOR_IN));
        String token = bearer(person);

        nutzendeRepository.delete(person);

        mockMvc.perform(get("/api/me").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void startsemesterAendernKlapptFuerStudierende() throws Exception {
        Nutzende person = studierendeAnlegen("me-put-ok@student.htw-berlin.de", "hash-egal");

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(person))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "WS 2024"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(person.getNutzendeId()))
                .andExpect(jsonPath("$.startSemester").value("WS 2024"));

        mockMvc.perform(get("/api/me").header("Authorization", bearer(person)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startSemester").value("WS 2024"));
    }

    @Test
    void startsemesterNachFruehestemPlaneintragGibt400() throws Exception {
        Nutzende person = studierendeAnlegen("me-put-plan@student.htw-berlin.de", "hash-egal");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Studierende s = studierendeRepository.findById(person.getNutzendeId()).orElseThrow();
            s.setStartsemester(semester(2024, SemesterArt.WS));
            studierendeRepository.save(s);
            planEintragRepository.save(new PlanEintrag(s, irgendeinModul(), semester(2024, SemesterArt.WS)));
        });

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(person))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "WS 2025"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startSemester"));
    }

    @Test
    void startsemesterNachFruehesterLeistungGibt400() throws Exception {
        Nutzende person = studierendeAnlegen("me-put-leistung@student.htw-berlin.de", "hash-egal");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Studierende s = studierendeRepository.findById(person.getNutzendeId()).orElseThrow();
            s.setStartsemester(semester(2024, SemesterArt.WS));
            studierendeRepository.save(s);
            Leistung l = new Leistung(s, irgendeinModul(), Status.BESTANDEN);
            l.setSemester(semester(2024, SemesterArt.WS));
            leistungRepository.save(l);
        });

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(person))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "WS 2025"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startSemester"));
    }

    @Test
    void anerkannteLeistungOhneSemesterBlockiertNichtUndGleichesSemesterIstErlaubt() throws Exception {
        Nutzende person = studierendeAnlegen("me-put-gleich@student.htw-berlin.de", "hash-egal");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Studierende s = studierendeRepository.findById(person.getNutzendeId()).orElseThrow();
            s.setStartsemester(semester(2023, SemesterArt.WS));
            studierendeRepository.save(s);

            // Anerkannte Leistung ohne Semester (semester == null)
            Leistung anerkannt = new Leistung(s, irgendeinModul(), Status.BESTANDEN);
            leistungRepository.save(anerkannt);

            // Planeintrag genau im Ziel-Startsemester WS 2024
            planEintragRepository.save(new PlanEintrag(s, irgendeinModul(), semester(2024, SemesterArt.WS)));
        });

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(person))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "WS 2024"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startSemester").value("WS 2024"));
    }

    @Test
    void sommersemesterOderFalschesFormatGibt400() throws Exception {
        Nutzende person = studierendeAnlegen("me-put-format@student.htw-berlin.de", "hash-egal");

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(person))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "SS 2025"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startSemester"));

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(person))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "2025"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startSemester"));
    }

    @Test
    void professorinWirdMit403Abgelehnt() throws Exception {
        Nutzende prof = nutzendeRepository.save(
                new Nutzende("me-put-prof@htw-berlin.de", "hash-egal", Rolle.PROFESSOR_IN));

        mockMvc.perform(put("/api/me")
                        .header("Authorization", bearer(prof))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startSemester": "WS 2025"}"""))
                .andExpect(status().isForbidden());
    }
}
