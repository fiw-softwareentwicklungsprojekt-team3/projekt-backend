package de.htw.studyplanner.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.Status;
import de.htw.studyplanner.backend.model.Studierende;
import de.htw.studyplanner.backend.repository.LeistungRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;

/**
 * Service für die Berechnung und Aggregation anonymisierter Kohorten-Statistiken für Lehrende (US-53a).
 *
 * <p>Berechnet pro Startsemester (Kohorte) aggregierte Kennzahlen ausschließlich auf Basis
 * tatsächlich bestandener Leistungen ({@code status = 'BESTANDEN'}). Geplante Module
 * aus {@code plan_eintrag} werden nicht berücksichtigt.
 *
 * <p>Um N+1-Abfragen zu verhindern, werden alle Daten in genau zwei Sammelabfragen über
 * {@link StudierendeRepository#findAllWithStartsemester()} und
 * {@link LeistungRepository#findByStatusWithModulAndStudierende(Status)} geladen.
 */
@Service
public class StatisticsService {

    /**
     * Die 17 Pflichtmodule der Fachsemester 1–3 laut FIW-StuPO (Anlage 2):
     * Fachsemester 1: B11–B16 (6 Module)
     * Fachsemester 2: B21–B26 (6 Module)
     * Fachsemester 3: B31–B35 (5 Module)
     */
    public static final Set<String> REQUIRED_MODULES_SEM_1_TO_3 = Set.of(
            "B11", "B12", "B13", "B14", "B15", "B16",
            "B21", "B22", "B23", "B24", "B25", "B26",
            "B31", "B32", "B33", "B34", "B35"
    );

    /** Mindestanzahl an bestandenen Leistungspunkten für die Zulassung zur Bachelorarbeit (StuPO § 12). */
    public static final int MIN_LP_BACHELOR_THESIS = 151;

    /** Gesamt-Leistungspunkte des Bachelorstudiengangs für die Prozentrechnung (180 LP). */
    public static final double TOTAL_DEGREE_LP = 180.0;

    /** Mindestgröße einer Kohorte zur Wahrung der Anonymität. Kleinere Kohorten werden übersprungen. */
    public static final int MIN_COHORT_SIZE = 5;

    private final StudierendeRepository studierendeRepository;
    private final LeistungRepository leistungRepository;

    /**
     * Erstellt den Service mit den benötigten Repositories.
     *
     * @param studierendeRepository Repository für Studierendendaten
     * @param leistungRepository   Repository für Leistungsdaten
     */
    public StatisticsService(StudierendeRepository studierendeRepository, LeistungRepository leistungRepository) {
        this.studierendeRepository = studierendeRepository;
        this.leistungRepository = leistungRepository;
    }

    /**
     * Liefert die anonymisierten Kohorten-Statistiken aller Jahrgänge in chronologischer Reihenfolge.
     *
     * <p>Die Sortierung der Kohorten erfolgt über {@link Semester#compareTo(Semester)} nach Jahr
     * und Semesterart (Sommersemester vor Wintersemester, nie über {@code semester_id}).
     * Kohorten mit weniger als 5 Studierenden werden zum Schutz der Anonymität nicht ausgeliefert.
     * Bei leerer Datenbank wird eine leere Liste zurückgegeben.
     *
     * @return Liste der Kohorten-Statistiken oder leere Liste, wenn keine auswertbaren Kohorten vorhanden sind
     */
    @Transactional(readOnly = true)
    public List<CohortStatisticsDto> getCohortStatistics() {
        List<Studierende> alleStudierende = studierendeRepository.findAllWithStartsemester();
        if (alleStudierende.isEmpty()) {
            return List.of();
        }

        // Alle bestandenen Leistungen mit initialisiertem Modul und Studierenden in einer Abfrage laden (kein N+1)
        List<Leistung> bestandeneLeistungen = leistungRepository.findByStatusWithModulAndStudierende(Status.BESTANDEN);

        // Nach Studierenden gruppieren: nutzendeId -> Liste bestandener Leistungen
        Map<Integer, List<Leistung>> leistungenNachStudierendem = bestandeneLeistungen.stream()
                .collect(Collectors.groupingBy(l -> l.getStudierende().getNutzendeId()));

        // Studierende nach Startsemester (Kohorte) gruppieren.
        // TreeMap mit natürlicher Ordnung von Semester sortiert automatisch chronologisch nach Jahr und Art (E23)
        Map<Semester, List<Studierende>> kohortenMap = new TreeMap<>();
        for (Studierende s : alleStudierende) {
            kohortenMap.computeIfAbsent(s.getStartsemester(), k -> new ArrayList<>()).add(s);
        }

        List<CohortStatisticsDto> ergebnis = new ArrayList<>();

        for (Map.Entry<Semester, List<Studierende>> entry : kohortenMap.entrySet()) {
            Semester startsemester = entry.getKey();
            List<Studierende> studierendeDerKohorte = entry.getValue();
            int studentCount = studierendeDerKohorte.size();

            // Kohorten mit weniger als 5 Studierenden zum Schutz der Anonymität überspringen
            if (studentCount < MIN_COHORT_SIZE) {
                continue;
            }

            int summeLp = 0;
            int completedSemesters1To3Count = 0;
            int bachelorThesisEligibleCount = 0;

            for (Studierende s : studierendeDerKohorte) {
                List<Leistung> leistungen = leistungenNachStudierendem.getOrDefault(s.getNutzendeId(), List.of());

                int studentLp = 0;
                Set<String> bestandeneModulCodes = new HashSet<>();

                for (Leistung l : leistungen) {
                    studentLp += l.getModul().getLp();
                    bestandeneModulCodes.add(l.getModul().getCode());
                }

                summeLp += studentLp;

                // Alle 17 Pflichtmodule aus Fachsemester 1–3 bestanden?
                boolean sem1To3Completed = bestandeneModulCodes.containsAll(REQUIRED_MODULES_SEM_1_TO_3);
                if (sem1To3Completed) {
                    completedSemesters1To3Count++;
                    // Zulassungsvoraussetzung Bachelorarbeit: Sem 1–3 vollständig UND mind. 151 LP (StuPO § 12)
                    if (studentLp >= MIN_LP_BACHELOR_THESIS) {
                        bachelorThesisEligibleCount++;
                    }
                }
            }

            double rawAverageLp = (double) summeLp / studentCount;
            double averageLp = roundToOneDecimal(rawAverageLp);
            double averageProgressPercent = roundToOneDecimal((rawAverageLp / TOTAL_DEGREE_LP) * 100.0);

            ergebnis.add(new CohortStatisticsDto(
                    startsemester.label(),
                    studentCount,
                    averageLp,
                    averageProgressPercent,
                    completedSemesters1To3Count,
                    bachelorThesisEligibleCount
            ));
        }

        return ergebnis;
    }

    /**
     * Rundet einen Gleitkommawert kaufmännisch auf genau eine Nachkommastelle (z. B. 62.4 oder 34.7).
     *
     * @param value der ungerundete Wert
     * @return auf eine Nachkommastelle gerundeter Wert
     */
    private double roundToOneDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
