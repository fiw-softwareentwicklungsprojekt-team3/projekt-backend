package de.htw.studyplanner.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.service.StudierendenGenerator;

/**
 * Prüft die Konsistenz der synthetischen Daten.
 *
 * Die Tests laufen mit einer kleinen Konfiguration (zwei Kohorten, wenige
 * Personen), weil die Regeln dieselben sind und der Lauf so schnell bleibt.
 * Die Standardkonfiguration wird separat nur auf ihre Werte geprüft.
 *
 * Die Startjahre 2021 und 2022 sind bewusst gewählt: diese Kohorten haben
 * genug Semester, um bis zur Bachelorarbeit zu kommen, damit auch die
 * LP-Voraussetzungen von B43, B65 und B66 im Test vorkommen.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class StudierendenGeneratorTest {

    private static final StudierendenGenerator.Konfiguration KLEIN =
            new StudierendenGenerator.Konfiguration(
                    42L, List.of(2021, 2022), 6, 10, 0.15, 4, 30);

    @Autowired
    private StudierendenGenerator generator;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void generiere() {
        generator.generiere(KLEIN);
    }

    private int zaehle(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void esWerdenStudierendeUndLeistungenAngelegt() {
        assertThat(zaehle("SELECT count(*) FROM studierende")).isBetween(12, 20);
        assertThat(zaehle("SELECT count(*) FROM nutzende")).isBetween(12, 20);
        assertThat(zaehle("SELECT count(*) FROM leistung")).isGreaterThan(100);
    }

    @Test
    void esGibtZweiKohorten() {
        assertThat(zaehle("SELECT count(DISTINCT startsemester_id) FROM studierende")).isEqualTo(2);
    }

    @Test
    void alleErzeugtenNutzendenSindMarkiertUndTragenKeineEchtenDaten() {
        assertThat(zaehle("SELECT count(*) FROM nutzende WHERE NOT is_synthetic")).isZero();
        assertThat(zaehle("SELECT count(*) FROM nutzende "
                + "WHERE email NOT LIKE 'dummy\\_%@example.invalid'")).isZero();
    }

    @Test
    void benoteteModuleHabenNurErlaubteNoten() {
        assertThat(zaehle("SELECT count(*) FROM leistung l JOIN modul m ON m.code = l.modul_code "
                + "WHERE m.benotet AND l.status = 'BESTANDEN' "
                + "  AND (l.note IS NULL OR l.note > 4.0 OR l.note < 1.0)")).isZero();
        assertThat(zaehle("SELECT count(*) FROM leistung l JOIN modul m ON m.code = l.modul_code "
                + "WHERE m.benotet AND l.status = 'NICHT_BESTANDEN' "
                + "  AND (l.note IS NULL OR l.note <> 5.0)")).isZero();
    }

    @Test
    void unbenoteteModuleHabenNiemalsEineNote() {
        assertThat(zaehle("SELECT count(*) FROM leistung l JOIN modul m ON m.code = l.modul_code "
                + "WHERE NOT m.benotet AND l.note IS NOT NULL")).isZero();
    }

    @Test
    void keinModulOhneBestandenesVorausgesetztesModul() {
        assertThat(zaehle("SELECT count(*) FROM leistung l "
                + "JOIN voraussetzung v ON v.modul_code = l.modul_code "
                + "     AND v.art = 'NOTWENDIG' AND v.vorausgesetzt_modul_code IS NOT NULL "
                + "WHERE NOT EXISTS ( "
                + "  SELECT 1 FROM leistung l2 "
                + "  WHERE l2.nutzende_id = l.nutzende_id "
                + "    AND l2.modul_code = v.vorausgesetzt_modul_code "
                + "    AND l2.status = 'BESTANDEN' "
                + "    AND l2.semester_id < l.semester_id)")).isZero();
    }

    @Test
    void keinModulUnterhalbSeinerMindestLeistungspunkte() {
        assertThat(zaehle("SELECT count(*) FROM leistung l "
                + "JOIN voraussetzung v ON v.modul_code = l.modul_code "
                + "     AND v.art = 'NOTWENDIG' AND v.min_lp IS NOT NULL "
                + "WHERE ( SELECT COALESCE(sum(m.lp), 0) FROM leistung l2 "
                + "        JOIN modul m ON m.code = l2.modul_code "
                + "        WHERE l2.nutzende_id = l.nutzende_id "
                + "          AND l2.status = 'BESTANDEN' "
                + "          AND l2.semester_id < l.semester_id ) < v.min_lp")).isZero();
    }

    @Test
    void keinModulWirdOefterAlsViermalVersucht() {
        assertThat(zaehle("SELECT count(*) FROM leistung WHERE anzahl_versuche NOT BETWEEN 1 AND 4"))
                .isZero();
    }

    @Test
    void wiederholungenStehenInDerSelbenZeile() {
        // Eine Person hat je Modul höchstens eine Zeile; dafür sorgt bereits
        // uq_leistung_nutzende_modul. Hier wird geprüft, dass es überhaupt
        // Wiederholungen gibt, sonst wäre die Quote wirkungslos.
        assertThat(zaehle("SELECT count(*) FROM leistung WHERE anzahl_versuche > 1"))
                .isPositive();
    }

    @Test
    void auswahlmoduleHabenEinAngebotOderEinenFreitext() {
        // B41 und B54 (Wahlpflicht) sowie B16 und B26 (Sprachen) brauchen ein
        // konkretes Wahlangebot.
        assertThat(zaehle("SELECT count(*) FROM leistung "
                + "WHERE modul_code IN ('B41', 'B54', 'B16', 'B26') "
                + "  AND wahlangebot_code IS NULL")).isZero();
        // B63 und B64 werden frei eingetragen.
        assertThat(zaehle("SELECT count(*) FROM leistung "
                + "WHERE modul_code IN ('B63', 'B64') AND kurs_titel IS NULL")).isZero();
        // Eine Person belegt nicht zweimal dasselbe Wahlpflichtangebot.
        assertThat(zaehle("SELECT count(*) FROM ( "
                + "  SELECT nutzende_id, wahlangebot_code FROM leistung "
                + "  WHERE wahlangebot_code IS NOT NULL "
                + "  GROUP BY nutzende_id, wahlangebot_code HAVING count(*) > 1) d")).isZero();
    }

    @Test
    void gleicherSeedErzeugtGleicheDaten() {
        String vorher = signatur();
        generator.generiere(KLEIN);
        assertThat(signatur()).isEqualTo(vorher);
    }

    @Test
    void dieStandardkonfigurationEntsprichtDemTicket() {
        StudierendenGenerator.Konfiguration k = StudierendenGenerator.Konfiguration.standard();
        assertThat(k.kohortenJahre()).hasSize(5);
        assertThat(k.minProKohorte()).isEqualTo(30);
        assertThat(k.maxProKohorte()).isEqualTo(60);
        assertThat(k.maxVersuche()).isEqualTo(4);
        assertThat(k.seed()).isEqualTo(42L);
    }

    private String signatur() {
        return jdbc.queryForObject(
                "SELECT md5(string_agg(n.email || ':' || l.modul_code || ':' || l.status || ':' "
                        + "|| COALESCE(l.note::text, '-') || ':' || l.anzahl_versuche, '|' "
                        + "ORDER BY n.email, l.modul_code)) "
                        + "FROM leistung l JOIN nutzende n ON n.nutzende_id = l.nutzende_id",
                String.class);
    }
}
