package de.htw.studyplanner.backend;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prüft, dass die Anwendung auf einer leeren Datenbank startet
 * und Flyway dabei alle Tabellen aus V1 anlegt.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional // jede Testmethode wird am Ende zurückgerollt
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void alleTabellenAusV1SindAngelegt() {
        List<String> tabellen = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(tabellen).contains(
                "modul", "modul_teil", "voraussetzung", "wahlangebot", "semester",
                "nutzende", "studierende", "plan_eintrag", "leistung");
    }

    @Test
    void migrationV1WurdeErfolgreichAusgefuehrt() {
        Boolean erfolgreich = jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class);

        assertThat(erfolgreich).isTrue();
    }

    /**
     * Legt ein Testmodul an. Eigene Codes statt echter Modulcodes, damit die
     * Tests nicht mit den Seed-Daten aus V2 kollidieren. sws_uebung ist in V1
     * NOT NULL und hat keinen Default, muss also immer mitgegeben werden.
     */
    private void gibTestmodul(String code, int fachsemester, int lp) {
        jdbc.update("INSERT INTO modul (code, bezeichnung, fachsemester, art, bereich, lp, "
                + "sws_vorlesung, sws_uebung, turnus) "
                + "VALUES (?, ?, ?, 'P', 'FACH', ?, 2, 2, 'WS')",
                code, "Testmodul " + code, fachsemester, lp);
    }

    @Test
    void voraussetzungsArtIstNurNotwendigOderEmpfohlen() {
        gibTestmodul("TST1", 1, 5);
        gibTestmodul("TST2", 3, 10);

        // gültig
        jdbc.update("INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code) "
                + "VALUES ('TST2', 'NOTWENDIG', 'TST1')");

        // ungültig: 'NV' ist kein erlaubter Wert
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code) "
                        + "VALUES ('TST2', 'NV', 'TST1')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nurErlaubteNotenWerdenGespeichert() {
        gibTestmodul("TST1", 1, 5);
        jdbc.update("INSERT INTO nutzende (nutzende_id, email, passwort_hash) "
                + "VALUES (1, 'test@htw-berlin.de', 'hash')");
        Integer semesterId = jdbc.queryForObject(
        "INSERT INTO semester (jahr, art) VALUES (2099, 'WS') RETURNING semester_id",
        Integer.class);
        jdbc.update("INSERT INTO studierende (nutzende_id, startsemester_id) VALUES (1, ?)", semesterId);

        // gültig
        jdbc.update("INSERT INTO leistung (nutzende_id, modul_code, status, note) "
                + "VALUES (1, 'TST1', 'BESTANDEN', 1.7)");
        jdbc.update("DELETE FROM leistung");

        // ungültig: 2,5 ist keine erlaubte Note
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO leistung (nutzende_id, modul_code, status, note) "
                        + "VALUES (1, 'TST1', 'BESTANDEN', 2.5)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
