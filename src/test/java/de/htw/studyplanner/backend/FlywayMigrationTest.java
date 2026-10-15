package de.htw.studyplanner.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prüft, dass die Anwendung auf einer leeren Datenbank startet
 * und Flyway dabei alle Tabellen aus V1 anlegt.
 */
@SpringBootTest
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

    @Test
    void voraussetzungsArtIstNurNotwendigOderEmpfohlen() {
        jdbc.update("INSERT INTO modul (code, bezeichnung, fachsemester, art, bereich, lp, turnus) "
                + "VALUES ('B11', 'Programmierung 1', 1, 'P', 'FACH', 5, 'WS'), "
                + "('B34', 'Softwareentwicklungsprojekt', 3, 'P', 'FACH', 10, 'WS')");

        // gültig
        jdbc.update("INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code) "
                + "VALUES ('B34', 'NOTWENDIG', 'B11')");

        // ungültig: 'NV' ist kein erlaubter Wert
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code) "
                        + "VALUES ('B34', 'NV', 'B11')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nurErlaubteNotenWerdenGespeichert() {
        jdbc.update("INSERT INTO modul (code, bezeichnung, fachsemester, art, bereich, lp, turnus) "
                + "VALUES ('B11', 'Programmierung 1', 1, 'P', 'FACH', 5, 'WS')");
        jdbc.update("INSERT INTO nutzende (nutzende_id, email, passwort_hash) "
                + "VALUES (1, 'test@htw-berlin.de', 'hash')");
        jdbc.update("INSERT INTO semester (semester_id, jahr, art) VALUES (1, 2026, 'WS')");
        jdbc.update("INSERT INTO studierende (nutzende_id, startsemester_id) VALUES (1, 1)");

        // gültig
        jdbc.update("INSERT INTO leistung (nutzende_id, modul_code, status, note) "
                + "VALUES (1, 'B11', 'BESTANDEN', 1.7)");
        jdbc.update("DELETE FROM leistung");

        // ungültig: 2,5 ist keine erlaubte Note
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO leistung (nutzende_id, modul_code, status, note) "
                        + "VALUES (1, 'B11', 'BESTANDEN', 2.5)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
