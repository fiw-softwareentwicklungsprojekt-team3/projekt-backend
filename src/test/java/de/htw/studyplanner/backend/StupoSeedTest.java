package de.htw.studyplanner.backend;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prüft die Seed-Daten aus V2 gegen die StuPO.
 *
 * Die Summenprüfungen sind wichtiger als die reinen Zeilenzahlen: 180 LP
 * insgesamt (StuPO § 7 Abs. 1) und 115 LP in der Größe X1 (Tabelle in § 15)
 * stimmen nur, wenn jeder einzelne LP-Wert richtig ist. Ein Tippfehler in
 * einem Modul lässt diese Tests scheitern.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class StupoSeedTest {

    @Autowired
    private JdbcTemplate jdbc;

    private int zaehle(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void alleModuleSindAngelegt() {
        assertThat(zaehle("SELECT count(*) FROM modul")).isEqualTo(32);
        assertThat(zaehle("SELECT count(*) FROM modul_teil")).isEqualTo(5);
    }

    @Test
    void alleVoraussetzungenSindAngelegt() {
        assertThat(zaehle("SELECT count(*) FROM voraussetzung")).isEqualTo(47);
        assertThat(zaehle("SELECT count(*) FROM voraussetzung WHERE art = 'NOTWENDIG'")).isEqualTo(23);
        assertThat(zaehle("SELECT count(*) FROM voraussetzung WHERE art = 'EMPFOHLEN'")).isEqualTo(24);
    }

    @Test
    void alleWahlangeboteSindAngelegt() {
        assertThat(zaehle("SELECT count(*) FROM wahlangebot WHERE gruppe = 'WP'")).isEqualTo(15);
        // fünf Sprachen in je zwei Niveaus (B16 und B26)
        assertThat(zaehle("SELECT count(*) FROM wahlangebot WHERE gruppe = 'SPRACHE'")).isEqualTo(10);
    }

    @Test
    void dasStudiumHat180Leistungspunkte() {
        assertThat(zaehle("SELECT sum(lp) FROM modul")).isEqualTo(180);
    }

    @Test
    void dieGesamtnoteWiegt115Leistungspunkte() {
        // StuPO § 15: Summe der Gewichtungsfaktoren
        assertThat(zaehle("SELECT sum(lp) FROM modul WHERE zaehlt_in_x1")).isEqualTo(115);
    }

    @Test
    void jedesFachsemesterHatDieLeistungspunkteAusAnlage2() {
        assertThat(jdbc.queryForList(
                "SELECT sum(lp) FROM modul GROUP BY fachsemester ORDER BY fachsemester",
                Integer.class))
                .containsExactly(30, 30, 30, 30, 31, 29);
    }

    @Test
    void nurDasFachpraktikumIstUnbenotet() {
        assertThat(jdbc.queryForList(
                "SELECT code FROM modul WHERE NOT benotet", String.class))
                .containsExactly("B43");
    }

    @Test
    void nurB41UndB54SindWahlpflicht() {
        // E9: die Modulart folgt der Realität, nicht der Anlage 2
        assertThat(jdbc.queryForList(
                "SELECT code FROM modul WHERE art = 'WP' ORDER BY code", String.class))
                .containsExactly("B41", "B54");
    }

    @Test
    void auswahlUndAweStehenAnDenRichtigenModulen() {
        assertThat(jdbc.queryForList(
                "SELECT code FROM modul WHERE mit_auswahl ORDER BY code", String.class))
                .containsExactly("B16", "B26", "B41", "B54", "B63", "B64");
        assertThat(jdbc.queryForList(
                "SELECT code FROM modul WHERE bereich = 'AWE' ORDER BY code", String.class))
                .containsExactly("B16", "B26", "B63", "B64");
    }

    @Test
    void dieBachelorarbeitSetztDieErstenDreiSemesterVoraus() {
        // RStPO § 25 Abs. 1 Satz 4
        assertThat(zaehle("SELECT count(*) FROM voraussetzung "
                + "WHERE modul_code = 'B66' AND vorausgesetzt_modul_code IS NOT NULL"))
                .isEqualTo(17);

        // StuPO § 12 und RStPO § 25 Abs. 1 Satz 3
        assertThat(jdbc.queryForMap("SELECT min_lp, toleranz_lp FROM voraussetzung "
                + "WHERE modul_code = 'B66' AND min_lp IS NOT NULL"))
                .containsEntry("min_lp", 151)
                .containsEntry("toleranz_lp", 10);
    }

    @Test
    void dasFachpraktikumSetzt60LeistungspunkteVoraus() {
        // StuPO § 11 Abs. 4, Anlage 2
        assertThat(jdbc.queryForObject("SELECT min_lp FROM voraussetzung "
                + "WHERE modul_code = 'B43' AND min_lp IS NOT NULL", Integer.class))
                .isEqualTo(60);
    }

    @Test
    void dasKolloquiumSetztBachelorarbeitUnd177LeistungspunkteVoraus() {
        // StuPO § 13
        assertThat(zaehle("SELECT count(*) FROM voraussetzung WHERE modul_code = 'B65' "
                + "AND art = 'NOTWENDIG' AND vorausgesetzt_modul_code = 'B66'")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT min_lp FROM voraussetzung "
                + "WHERE modul_code = 'B65' AND min_lp IS NOT NULL", Integer.class))
                .isEqualTo(177);
    }

    @Test
    void dieSwsDerTeileErgebenDieUebungsstundenDesModuls() {
        assertThat(zaehle("SELECT count(*) FROM modul m "
                + "JOIN (SELECT modul_code, sum(sws) AS s FROM modul_teil GROUP BY modul_code) t "
                + "  ON t.modul_code = m.code "
                + "WHERE t.s <> m.sws_uebung")).isZero();
    }

    @Test
    void derTurnusFolgtDerParitaetDesFachsemesters() {
        // E13: ungerades Fachsemester = WS, gerades = SS
        assertThat(zaehle("SELECT count(*) FROM modul WHERE turnus <> "
                + "CASE WHEN fachsemester % 2 = 1 THEN 'WS' ELSE 'SS' END")).isZero();
    }
}
