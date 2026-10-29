package de.htw.studyplanner.backend.service;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.repository.SemesterRepository;

/**
 * Zentrale Fachlogik rund um Semester: Berechnung des aktuellen Semesters aus einem Datum (E18),
 * Parsen von Semester-Strings, chronologischer Vergleich über {@link Semester#compareTo(Semester)}
 * (E23) sowie das Suchen und automatische Anlegen in der Datenbank (E19).
 */
@Service
public class SemesterService {

    private static final Pattern SEMESTER_PATTERN = Pattern.compile("^(WS|SS) (\\d{4})$");

    private final SemesterRepository semesterRepository;

    /**
     * Erstellt den Service.
     *
     * @param semesterRepository Repository zum Suchen und Anlegen von Semestern
     */
    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    /**
     * Bestimmt das Semester zu einem gegebenen Datum (E18):
     * <ul>
     *   <li>01.10.–31.03. → {@code WS} mit dem Startjahr vom Oktober (z. B. 15.02.2027 → WS 2026)</li>
     *   <li>01.04.–30.09. → {@code SS} mit dem Kalenderjahr</li>
     * </ul>
     *
     * @param datum das zu prüfende Datum
     * @return das zugehörige {@link Semester} (ohne Datenbankzugriff)
     */
    public Semester aktuellesSemester(LocalDate datum) {
        int monat = datum.getMonthValue();
        int jahr = datum.getYear();

        if (monat >= 4 && monat <= 9) {
            return new Semester(jahr, SemesterArt.SS);
        }
        if (monat <= 3) {
            return new Semester(jahr - 1, SemesterArt.WS);
        }
        return new Semester(jahr, SemesterArt.WS);
    }

    /**
     * Prüft anhand von {@link Semester#compareTo(Semester)}, ob Semester {@code a}
     * zeitlich vor Semester {@code b} liegt (nie über {@code semester_id}).
     *
     * @param a erstes Semester
     * @param b zweites Semester
     * @return {@code true}, wenn {@code a} früher als {@code b} ist
     */
    public boolean istFrueher(Semester a, Semester b) {
        return a.compareTo(b) < 0;
    }

    /**
     * Prüft anhand von {@link Semester#compareTo(Semester)}, ob zwei Semester
     * fachlich dasselbe Jahr und dieselbe Art haben (nie über {@code semester_id}).
     *
     * @param a erstes Semester
     * @param b zweites Semester
     * @return {@code true}, wenn Jahr und Art übereinstimmen
     */
    public boolean istGleich(Semester a, Semester b) {
        return a.compareTo(b) == 0;
    }

    /**
     * Zerlegt einen Semester-String im Format {@code "WS JJJJ"} oder {@code "SS JJJJ"}
     * in Jahr und Art.
     *
     * @param text Semester-String, z. B. {@code "SS 2027"} oder {@code "WS 2026"}
     * @return ein {@link Semester}-Objekt mit dem gelesenen Jahr und der Art
     * @throws InvalidSemesterFormatException wenn das Format ungültig ist
     */
    public Semester parse(String text) {
        if (text == null) {
            throw new InvalidSemesterFormatException(
                    "semester muss das Format 'WS JJJJ' oder 'SS JJJJ' haben");
        }
        Matcher matcher = SEMESTER_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new InvalidSemesterFormatException(
                    "semester muss das Format 'WS JJJJ' oder 'SS JJJJ' haben");
        }
        SemesterArt art = SemesterArt.valueOf(matcher.group(1));
        int jahr = Integer.parseInt(matcher.group(2));
        return new Semester(jahr, art);
    }

    /**
     * Sucht ein Semester über {@link SemesterRepository#findByJahrAndArt(int, SemesterArt)}
     * und legt es in der Datenbank an, falls es noch nicht existiert (E19).
     *
     * @param jahr das vierstellige Jahr (bei WS das Startjahr)
     * @param art  {@code WS} oder {@code SS}
     * @return die persistierte {@link Semester}-Entität
     */
    @Transactional
    public Semester findenOderNeuAnlegen(int jahr, SemesterArt art) {
        return semesterRepository.findByJahrAndArt(jahr, art)
                .orElseGet(() -> semesterRepository.save(new Semester(jahr, art)));
    }
}

