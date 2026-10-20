package de.htw.studyplanner.backend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Erzeugt synthetische Studierende samt Studienverlauf.
 *
 * Die erzeugten Daten stellen abgeschlossene Historie dar, keine Planung.
 * Anders als beim Studienplan (siehe E2, dort wird nichts blockiert und das
 * Frontend weist nur hin) gelten hier die StuPO-Regeln strikt: ein Modul wird
 * nur belegt, wenn seine NOTWENDIG-Voraussetzungen zu diesem Zeitpunkt erfüllt
 * sind.
 *
 * Ablauf je Person: Semester für Semester vom Startsemester der Kohorte bis zum
 * aktuellsten Semester in der Tabelle {@code semester}. Pro Semester werden
 * zuerst offene Wiederholungen und danach neue Module belegt, bis das LP-Ziel
 * erreicht ist. Bestandene LP werden erst am Semesterende gutgeschrieben, damit
 * ein Modul nicht ein anderes im selben Semester freischaltet.
 *
 * Der Generator ist über den festen Seed reproduzierbar und löscht zu Beginn
 * die zuvor erzeugten synthetischen Daten, damit mehrfaches Ausführen zum
 * gleichen Ergebnis führt.
 */
@Component
public class StudierendenGenerator {

    /** Erlaubte Noten für bestandene Module (RStPO § 17 Abs. 1, ohne 5,0). */
    private static final List<Double> NOTEN =
            List.of(1.0, 1.3, 1.7, 2.0, 2.3, 2.7, 3.0, 3.3, 3.7, 4.0);

    /**
     * AWE-Module, bei denen Studierende ein Modul aus einem anderen Studiengang
     * frei eintragen. Diese Zuordnung steht nicht im Schema: wahlangebot kennt
     * nur die Gruppen WP und SPRACHE, aber keine Verknüpfung "dieses Angebot
     * gehört zu diesem Modul". Deshalb hier als feste Liste.
     */
    private static final Set<String> AWE_FREITEXT = Set.of("B63", "B64");

    /** Beispielhafte Freitext-Titel für die AWE-Module. */
    private static final List<String> AWE_TITEL = List.of(
            "Technikgeschichte",
            "Rhetorik und Präsentation",
            "Einführung in die Soziologie",
            "Nachhaltigkeit und Ressourcen",
            "Wissenschaftsethik",
            "Grundlagen des Projektrechts");

    /** Platzhalter, der sich bewusst nicht zum Anmelden eignet. */
    private static final String PASSWORT_PLATZHALTER = "synthetisch-kein-login";

    private final JdbcTemplate jdbc;

    public StudierendenGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------
    // Konfiguration und Ergebnis
    // ------------------------------------------------------------------

    /**
     * @param seed            fester Startwert, gleiche Eingabe erzeugt gleiche Daten
     * @param kohortenJahre   Startjahre der Kohorten, jeweils Wintersemester
     * @param minProKohorte   kleinste Kohortengröße
     * @param maxProKohorte   größte Kohortengröße
     * @param durchfallquote  Anteil nicht bestandener Versuche bei benoteten Modulen
     * @param maxVersuche     Versuche je Modul, danach endgültig nicht bestanden
     * @param lpZielProSemester LP, die pro Semester angestrebt werden
     */
    public record Konfiguration(
            long seed,
            List<Integer> kohortenJahre,
            int minProKohorte,
            int maxProKohorte,
            double durchfallquote,
            int maxVersuche,
            int lpZielProSemester) {

        public static Konfiguration standard() {
            return new Konfiguration(
                    42L,
                    List.of(2021, 2022, 2023, 2024, 2025),
                    30, 60,
                    0.15,
                    4,
                    30);
        }
    }

    public record Ergebnis(int studierende, int leistungen, int bestanden, int nichtBestanden) {}

    // ------------------------------------------------------------------
    // Datensätze aus der Datenbank
    // ------------------------------------------------------------------

    private record Semester(int id, int jahr, String art) {}

    private record Modul(String code, int fachsemester, int lp, String turnus,
                         boolean mitAuswahl, String bereich, String niveaustufe,
                         boolean benotet) {}

    /** Entweder vorausgesetztesModul oder minLp ist gesetzt, nie beides. */
    private record Voraussetzung(String vorausgesetztesModul, Integer minLp) {}

    // ------------------------------------------------------------------
    // Einstieg
    // ------------------------------------------------------------------

    @Transactional
    public Ergebnis generiere() {
        return generiere(Konfiguration.standard());
    }

    @Transactional
    public Ergebnis generiere(Konfiguration k) {
        loescheFrueherErzeugteDaten();

        List<Semester> semester = ladeSemester();
        List<Modul> module = ladeModule();
        Map<String, List<Voraussetzung>> voraussetzungen = ladeNotwendigeVoraussetzungen();
        List<String> wpAngebote = ladeWahlangebote("WP", null);
        Map<String, List<String>> sprachen = Map.of(
                "1a", ladeWahlangebote("SPRACHE", "1a"),
                "1b", ladeWahlangebote("SPRACHE", "1b"));

        Random random = new Random(k.seed());
        int anzahlStudierende = 0;

        for (int jahr : k.kohortenJahre()) {
            Semester start = findeSemester(semester, jahr, "WS");
            int groesse = k.minProKohorte()
                    + random.nextInt(k.maxProKohorte() - k.minProKohorte() + 1);

            for (int nr = 1; nr <= groesse; nr++) {
                int nutzendeId = legeStudierendeAn(jahr, nr, start.id());
                simuliereStudienverlauf(nutzendeId, start, semester, module,
                        voraussetzungen, wpAngebote, sprachen, k, random);
                anzahlStudierende++;
            }
        }

        return new Ergebnis(
                anzahlStudierende,
                zaehle("SELECT count(*) FROM leistung"),
                zaehle("SELECT count(*) FROM leistung WHERE status = 'BESTANDEN'"),
                zaehle("SELECT count(*) FROM leistung WHERE status = 'NICHT_BESTANDEN'"));
    }

    // ------------------------------------------------------------------
    // Simulation einer Person
    // ------------------------------------------------------------------

    private void simuliereStudienverlauf(int nutzendeId, Semester start,
            List<Semester> alleSemester, List<Modul> module,
            Map<String, List<Voraussetzung>> voraussetzungen,
            List<String> wpAngebote, Map<String, List<String>> sprachen,
            Konfiguration k, Random random) {

        Set<String> bestanden = new HashSet<>();
        Map<String, Integer> versuche = new HashMap<>();
        Set<String> schonGewaehlteWp = new HashSet<>();
        String sprachePraefix = null;
        int lp = 0;

        for (Semester s : alleSemester) {
            if (s.id() < start.id()) {
                continue;
            }
            int studiensemester = s.id() - start.id() + 1;

            List<Modul> kandidaten = new ArrayList<>();
            // Wiederholungen haben Vorrang: ohne sie bleibt die Person hängen.
            for (Modul m : module) {
                int v = versuche.getOrDefault(m.code(), 0);
                if (v > 0 && v < k.maxVersuche()
                        && !bestanden.contains(m.code())
                        && passtInsSemester(m, s)) {
                    kandidaten.add(m);
                }
            }
            // Danach neue Module, in Fachsemester-Reihenfolge.
            for (Modul m : module) {
                if (!versuche.containsKey(m.code())
                        && m.fachsemester() <= studiensemester
                        && passtInsSemester(m, s)
                        && voraussetzungenErfuellt(m, bestanden, lp, voraussetzungen)) {
                    kandidaten.add(m);
                }
            }

            // LP erst am Semesterende gutschreiben, damit ein Modul nicht ein
            // anderes im selben Semester freischaltet.
            int lpDiesesSemester = 0;
            Set<String> neuBestanden = new HashSet<>();

            for (Modul m : kandidaten) {
                if (lpDiesesSemester >= k.lpZielProSemester()) {
                    break;
                }
                int versuchNr = versuche.merge(m.code(), 1, Integer::sum);
                boolean geschafft;
                Double note;

                if (!m.benotet()) {
                    // Unbenotete Leistung (Fachpraktikum): gilt als erbracht,
                    // eine Note gibt es hier in keinem Fall.
                    geschafft = true;
                    note = null;
                } else if (random.nextDouble() < k.durchfallquote()) {
                    geschafft = false;
                    note = 5.0;
                } else {
                    geschafft = true;
                    note = NOTEN.get(random.nextInt(NOTEN.size()));
                }

                String status = geschafft ? "BESTANDEN" : "NICHT_BESTANDEN";

                if (versuchNr == 1) {
                    String wahlangebot = null;
                    String kursTitel = null;
                    if (m.mitAuswahl()) {
                        if (AWE_FREITEXT.contains(m.code())) {
                            kursTitel = AWE_TITEL.get(random.nextInt(AWE_TITEL.size()));
                        } else if ("AWE".equals(m.bereich())) {
                            wahlangebot = waehleSprache(
                                    sprachen.get(m.niveaustufe()), sprachePraefix, random);
                            sprachePraefix = wahlangebot.substring(0, wahlangebot.indexOf('_'));
                        } else {
                            wahlangebot = waehleOhneWiederholung(wpAngebote, schonGewaehlteWp, random);
                        }
                    }
                    jdbc.update("INSERT INTO leistung (nutzende_id, modul_code, wahlangebot_code, "
                            + "kurs_titel, semester_id, status, note, anzahl_versuche) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            nutzendeId, m.code(), wahlangebot, kursTitel, s.id(), status, note, 1);
                } else {
                    // Wiederholung überschreibt die bestehende Zeile, die alte
                    // Note wird nicht aufbewahrt (Team-Entscheidung).
                    jdbc.update("UPDATE leistung SET status = ?, note = ?, anzahl_versuche = ?, "
                            + "semester_id = ? WHERE nutzende_id = ? AND modul_code = ?",
                            status, note, versuchNr, s.id(), nutzendeId, m.code());
                }

                // Ein belegtes Modul kostet Arbeitszeit, auch wenn es nicht
                // bestanden wird. Deshalb zählt es in jedem Fall auf das
                // LP-Ziel dieses Semesters.
                lpDiesesSemester += m.lp();
                if (geschafft) {
                    neuBestanden.add(m.code());
                }
            }

            for (String code : neuBestanden) {
                bestanden.add(code);
                lp += lpVon(module, code);
            }
        }
    }

    /** Ein Modul wird nur in einem Semester belegt, das zu seinem Turnus passt. */
    private boolean passtInsSemester(Modul m, Semester s) {
        return "BEIDE".equals(m.turnus()) || m.turnus().equals(s.art());
    }

    /**
     * Prüft die NOTWENDIG-Voraussetzungen. Bei LP-Grenzen wird {@code min_lp}
     * streng angewendet und die Toleranz nicht ausgereizt: wer die strenge
     * Grenze erfüllt, erfüllt auch die tolerierte, damit sind die erzeugten
     * Daten unter jeder Lesart der StuPO gültig.
     */
    private boolean voraussetzungenErfuellt(Modul m, Set<String> bestanden, int lp,
            Map<String, List<Voraussetzung>> alle) {
        for (Voraussetzung v : alle.getOrDefault(m.code(), List.of())) {
            if (v.vorausgesetztesModul() != null) {
                if (!bestanden.contains(v.vorausgesetztesModul())) {
                    return false;
                }
            } else if (lp < v.minLp()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Datenbankzugriff
    // ------------------------------------------------------------------

    private void loescheFrueherErzeugteDaten() {
        jdbc.update("DELETE FROM leistung WHERE nutzende_id IN "
                + "(SELECT nutzende_id FROM nutzende WHERE is_synthetic)");
        jdbc.update("DELETE FROM plan_eintrag WHERE nutzende_id IN "
                + "(SELECT nutzende_id FROM nutzende WHERE is_synthetic)");
        jdbc.update("DELETE FROM studierende WHERE nutzende_id IN "
                + "(SELECT nutzende_id FROM nutzende WHERE is_synthetic)");
        jdbc.update("DELETE FROM nutzende WHERE is_synthetic");
    }

    private int legeStudierendeAn(int jahr, int nr, int startsemesterId) {
        // example.invalid ist laut RFC 2606 dauerhaft nicht vergeben, die
        // Adresse kann also mit keiner echten Adresse kollidieren.
        String email = "dummy_%d_%d@example.invalid".formatted(jahr, nr);
        Integer id = jdbc.queryForObject(
                "INSERT INTO nutzende (email, passwort_hash, rolle, is_synthetic) "
                        + "VALUES (?, ?, 'STUDIERENDE', TRUE) RETURNING nutzende_id",
                Integer.class, email, PASSWORT_PLATZHALTER);
        jdbc.update("INSERT INTO studierende (nutzende_id, startsemester_id) VALUES (?, ?)",
                id, startsemesterId);
        return id;
    }

    private List<Semester> ladeSemester() {
        return jdbc.query("SELECT semester_id, jahr, art FROM semester ORDER BY semester_id",
                (rs, i) -> new Semester(rs.getInt("semester_id"), rs.getInt("jahr"), rs.getString("art")));
    }

    private List<Modul> ladeModule() {
        return jdbc.query("SELECT code, fachsemester, lp, turnus, mit_auswahl, bereich, "
                + "niveaustufe, benotet FROM modul ORDER BY fachsemester, code",
                (rs, i) -> new Modul(
                        rs.getString("code"),
                        rs.getInt("fachsemester"),
                        rs.getInt("lp"),
                        rs.getString("turnus"),
                        rs.getBoolean("mit_auswahl"),
                        rs.getString("bereich"),
                        rs.getString("niveaustufe"),
                        rs.getBoolean("benotet")));
    }

    private Map<String, List<Voraussetzung>> ladeNotwendigeVoraussetzungen() {
        record Zeile(String modulCode, Voraussetzung voraussetzung) {}

        List<Zeile> zeilen = jdbc.query(
                "SELECT modul_code, vorausgesetzt_modul_code, min_lp FROM voraussetzung "
                        + "WHERE art = 'NOTWENDIG'",
                (rs, i) -> {
                    Integer minLp = rs.getObject("min_lp") == null ? null : rs.getInt("min_lp");
                    return new Zeile(rs.getString("modul_code"),
                            new Voraussetzung(rs.getString("vorausgesetzt_modul_code"), minLp));
                });

        Map<String, List<Voraussetzung>> ergebnis = new HashMap<>();
        for (Zeile z : zeilen) {
            ergebnis.computeIfAbsent(z.modulCode(), c -> new ArrayList<>()).add(z.voraussetzung());
        }
        return ergebnis;
    }

    private List<String> ladeWahlangebote(String gruppe, String niveaustufe) {
        if (niveaustufe == null) {
            return jdbc.queryForList(
                    "SELECT code FROM wahlangebot WHERE gruppe = ? ORDER BY code",
                    String.class, gruppe);
        }
        return jdbc.queryForList(
                "SELECT code FROM wahlangebot WHERE gruppe = ? AND niveaustufe = ? ORDER BY code",
                String.class, gruppe, niveaustufe);
    }

    private int zaehle(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    // ------------------------------------------------------------------
    // Kleine Helfer
    // ------------------------------------------------------------------

    private Semester findeSemester(List<Semester> alle, int jahr, String art) {
        return alle.stream()
                .filter(s -> s.jahr() == jahr && s.art().equals(art))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Semester %d %s fehlt in der Tabelle semester".formatted(jahr, art)));
    }

    private int lpVon(List<Modul> module, String code) {
        return module.stream()
                .filter(m -> m.code().equals(code))
                .findFirst()
                .orElseThrow()
                .lp();
    }

    /**
     * E26: B26 führt dieselbe Sprache fort wie B16. Verglichen wird das Präfix
     * vor dem Unterstrich, nicht der ganze Code: EN_B2.1 und EN_B2.2 gehören
     * zusammen, EN_B2.1 und FR_B2.1 nicht.
     */
    private String waehleSprache(List<String> angebote, String praefix, Random random) {
        if (praefix != null) {
            for (String a : angebote) {
                if (a.startsWith(praefix + "_")) {
                    return a;
                }
            }
        }
        return angebote.get(random.nextInt(angebote.size()));
    }

    /** Dasselbe Wahlpflichtangebot soll eine Person nicht zweimal belegen. */
    private String waehleOhneWiederholung(List<String> angebote, Set<String> schonGewaehlt,
            Random random) {
        List<String> offen = angebote.stream().filter(a -> !schonGewaehlt.contains(a)).toList();
        String gewaehlt = offen.get(random.nextInt(offen.size()));
        schonGewaehlt.add(gewaehlt);
        return gewaehlt;
    }
}
