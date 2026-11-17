package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.exception.UngueltigerEinladungscodeException;
import de.htw.studyplanner.backend.model.Einladungscode;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.repository.EinladungscodeRepository;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.service.EinladungscodeService;

/**
 * Tests für {@code POST /api/auth/register/professor} (E2.5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfessorRegistrationTest {

    private static final String URL = "/api/auth/register/professor";

    @Autowired private MockMvc mockMvc;
    @Autowired private EinladungscodeService einladungscodeService;
    @Autowired private EinladungscodeRepository einladungscodeRepository;
    @Autowired private NutzendeRepository nutzendeRepository;
    @Autowired private JdbcTemplate jdbc;

    private String neuerCode() {
        return einladungscodeService.legeAn(1, Duration.ofDays(1)).get(0).getCode();
    }

    private ResultActions registriereProfessorIn(String email, String password, String code) throws Exception {
        return mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "code": "%s"}
                        """.formatted(email, password, code)));
    }

    private Map<String, Object> codeZeile(String code) {
        return jdbc.queryForMap(
                "SELECT eingeloest_von, eingeloest_am FROM einladungscode WHERE code = ?", code);
    }

    private void erwarteUngueltigenCode(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(UngueltigerEinladungscodeException.MELDUNG))
                .andExpect(jsonPath("$.errors[0].field").value("code"))
                .andExpect(jsonPath("$.errors[0].message").value(UngueltigerEinladungscodeException.MELDUNG));
    }

    @Test
    void gueltigerCodeLiefert201MitRolleProfessorIn() throws Exception {
        String code = neuerCode();

        registriereProfessorIn("prof.erfolg@htw-berlin.de", "geheimesPasswort", code)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("prof.erfolg@htw-berlin.de"))
                .andExpect(jsonPath("$.role").value("PROFESSOR_IN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwortHash").doesNotExist());

        var nutzende = nutzendeRepository.findByEmail("prof.erfolg@htw-berlin.de").orElseThrow();
        assertThat(nutzende.getRolle()).isEqualTo(Rolle.PROFESSOR_IN);
        assertThat(nutzende.getPasswortHash()).startsWith("$2");
    }

    @Test
    void codeIstNachEinloesenMitNutzendeIdUndDatumMarkiert() throws Exception {
        String code = neuerCode();
        assertThat(codeZeile(code).get("eingeloest_von")).isNull();
        Instant vorher = Instant.now().minusSeconds(1);

        registriereProfessorIn("prof.markiert@htw-berlin.de", "geheimesPasswort", code)
                .andExpect(status().isCreated());

        Integer neueId = nutzendeRepository.findByEmail("prof.markiert@htw-berlin.de")
                .orElseThrow().getNutzendeId();
        Map<String, Object> zeile = codeZeile(code);
        assertThat(zeile.get("eingeloest_von")).isEqualTo(neueId);
        assertThat(zeile.get("eingeloest_am")).isNotNull();
        Instant eingeloestAm = ((java.sql.Timestamp) zeile.get("eingeloest_am")).toInstant();
        assertThat(eingeloestAm).isAfter(vorher);
    }

    @Test
    void codeWirdOhneBeachtungVonGrossKleinschreibungUndLeerzeichenAkzeptiert() throws Exception {
        String code = neuerCode();

        registriereProfessorIn("prof.klein@htw-berlin.de", "geheimesPasswort", "  " + code.toLowerCase() + " ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PROFESSOR_IN"));
    }

    @Test
    void unbekannterCodeLiefert400MitAllgemeinerMeldung() throws Exception {
        erwarteUngueltigenCode(
                registriereProfessorIn("prof.unbekannt@htw-berlin.de", "geheimesPasswort", "GIBTESNICHT2"));

        assertThat(nutzendeRepository.findByEmail("prof.unbekannt@htw-berlin.de")).isEmpty();
    }

    @Test
    void abgelaufenerCodeLiefert400MitAllgemeinerMeldung() throws Exception {
        String code = einladungscodeService.erzeugeCode();
        einladungscodeRepository.save(new Einladungscode(code, Instant.now().minus(Duration.ofDays(1))));

        erwarteUngueltigenCode(
                registriereProfessorIn("prof.abgelaufen@htw-berlin.de", "geheimesPasswort", code));

        assertThat(nutzendeRepository.findByEmail("prof.abgelaufen@htw-berlin.de")).isEmpty();
        assertThat(codeZeile(code).get("eingeloest_von")).isNull();
    }

    @Test
    void schonEingeloesterCodeLiefert400MitAllgemeinerMeldung() throws Exception {
        String code = neuerCode();
        registriereProfessorIn("prof.erste@htw-berlin.de", "geheimesPasswort", code)
                .andExpect(status().isCreated());

        erwarteUngueltigenCode(
                registriereProfessorIn("prof.zweite@htw-berlin.de", "geheimesPasswort", code));

        assertThat(nutzendeRepository.findByEmail("prof.zweite@htw-berlin.de")).isEmpty();
        Integer ersteId = nutzendeRepository.findByEmail("prof.erste@htw-berlin.de")
                .orElseThrow().getNutzendeId();
        assertThat(codeZeile(code).get("eingeloest_von")).isEqualTo(ersteId);
    }

    @Test
    void fehlenderCodeLiefert400MitFeldCode() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "prof.ohnecode@htw-berlin.de", "password": "geheimesPasswort"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("code"));

        assertThat(nutzendeRepository.findByEmail("prof.ohnecode@htw-berlin.de")).isEmpty();
    }

    @Test
    void ungueltigeEmailLiefert400() throws Exception {
        registriereProfessorIn("keine-email", "geheimesPasswort", neuerCode())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("email ist ungültig"));
    }

    @Test
    void zuKurzesPasswortLiefert400() throws Exception {
        registriereProfessorIn("prof.kurz@htw-berlin.de", "abc", neuerCode())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message").value("password muss mindestens 8 Zeichen haben"));
    }

    @Test
    void vergebeneEmailLiefert409UndCodeWirdNichtVerbraucht() throws Exception {
        // Die E-Mail gehört schon einer Professor:in (nur @htw-berlin.de ist erlaubt).
        registriereProfessorIn("vergeben.prof@htw-berlin.de", "geheimesPasswort", neuerCode())
                .andExpect(status().isCreated());

        String code = neuerCode();
        registriereProfessorIn("vergeben.prof@htw-berlin.de", "geheimesPasswort", code)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Diese E-Mail-Adresse ist bereits vergeben."));

        // Der zweite Code ist nicht verbraucht.
        assertThat(codeZeile(code).get("eingeloest_von")).isNull();
    }

    @Test
    void emailOhneHtwLehrendenDomainLiefert400UndCodeBleibtGueltig() throws Exception {
        // Nur @htw-berlin.de, sonst ist später kein Login möglich (LoginRequest erlaubt nur HTW-Adressen).
        String code = neuerCode();

        registriereProfessorIn("prof@gmail.com", "geheimesPasswort", code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("email muss auf @htw-berlin.de enden"));

        registriereProfessorIn("prof@student.htw-berlin.de", "geheimesPasswort", code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        assertThat(codeZeile(code).get("eingeloest_von")).isNull();
    }

    @Test
    void vergebeneEmailLiefert409AuchBeiUnbekanntemCode() throws Exception {
        String code = neuerCode();
        registriereProfessorIn("prof.orakel@htw-berlin.de", "geheimesPasswort", code)
                .andExpect(status().isCreated());

        registriereProfessorIn("prof.orakel@htw-berlin.de", "geheimesPasswort", "GIBTESNICHT3")
                .andExpect(status().isConflict());
    }

    @Test
    void normaleRegistrierungMitCodeFeldLegtTrotzdemStudierendeAnUndVerbrauchtKeinenCode() throws Exception {
        String code = neuerCode();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "trick@student.htw-berlin.de", "password": "geheimesPasswort",
                                 "startsemester": "WS 2025", "code": "%s", "role": "PROFESSOR_IN"}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDIERENDE"));

        assertThat(nutzendeRepository.findByEmail("trick@student.htw-berlin.de").orElseThrow().getRolle())
                .isEqualTo(Rolle.STUDIERENDE);
        assertThat(codeZeile(code).get("eingeloest_von")).isNull();
    }
}