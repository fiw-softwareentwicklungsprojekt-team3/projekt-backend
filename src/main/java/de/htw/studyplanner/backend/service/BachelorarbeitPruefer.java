package de.htw.studyplanner.backend.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.PlanVerstoss;
import de.htw.studyplanner.backend.dto.PruefSchwere;
import de.htw.studyplanner.backend.model.Leistung;
import de.htw.studyplanner.backend.model.Voraussetzung;
import de.htw.studyplanner.backend.model.VoraussetzungsArt;
import de.htw.studyplanner.backend.repository.VoraussetzungRepository;

/**
 * Entscheidet, ob eine Person die Bachelorarbeit (B66) anmelden darf.
 *
 * <p>Die Regeln kommen aus der Tabelle {@code voraussetzung} (B66: {@code min_lp} 151,
 * {@code toleranz_lp} 10 und die 17 Module der Fachsemester 1–3, E14). Daraus ergeben sich
 * drei Stufen, wie im {@link PlanPruefer} (Fehler, Warnung, in Ordnung):
 * <ul>
 *   <li>{@link Stufe#REGULAER}: Sem. 1–3 bestanden und mindestens 151 LP (StuPO § 12)</li>
 *   <li>{@link Stufe#MIT_ZULASSUNG}: Sem. 1–3 bestanden und 141–150 LP, nur mit Zulassung durch
 *       den Prüfungsausschuss (RStPO § 25 Abs. 1)</li>
 *   <li>{@link Stufe#NICHT_ZUGELASSEN}: ein Modul aus Sem. 1–3 fehlt oder weniger als 141 LP</li>
 * </ul>
 * Es zählen nur Leistungen, nicht der Plan. Die einzelne Regel prüft
 * {@link PlanPruefer#pruefeVoraussetzung}. Genutzt von {@link ProgressService} (US-58)
 * und {@link StatisticsService} (US-53a), damit beide dieselbe Antwort geben.
 */
@Service
public class BachelorarbeitPruefer {

    /** Modulcode der Bachelorarbeit. */
    public static final String BACHELORARBEIT = "B66";

    /** Die drei Stufen der Zulassung zur Bachelorarbeit. */
    public enum Stufe {
        /** Regulär zugelassen: mindestens {@code min_lp} (151 LP). */
        REGULAER,
        /** Nur mit Zulassung durch den Prüfungsausschuss: im Toleranzbereich (141–150 LP). */
        MIT_ZULASSUNG,
        /** Nicht zugelassen. */
        NICHT_ZUGELASSEN
    }

    private final VoraussetzungRepository voraussetzungRepository;
    private final PlanPruefer planPruefer;

    /**
     * Erstellt den Prüfer.
     *
     * @param voraussetzungRepository liest die Regeln der Bachelorarbeit
     * @param planPruefer             prüft eine einzelne Voraussetzung
     */
    public BachelorarbeitPruefer(VoraussetzungRepository voraussetzungRepository, PlanPruefer planPruefer) {
        this.voraussetzungRepository = voraussetzungRepository;
        this.planPruefer = planPruefer;
    }

    /**
     * Lädt die notwendigen Voraussetzungen der Bachelorarbeit, sortiert nach ID.
     * Einmal laden und für viele Personen an {@link #stufe} übergeben.
     *
     * @return die Regeln von B66
     */
    @Transactional(readOnly = true)
    public List<Voraussetzung> regeln() {
        return voraussetzungRepository.findByModul_Code(BACHELORARBEIT).stream()
                .filter(v -> v.getArt() == VoraussetzungsArt.NOTWENDIG)
                .sorted(Comparator.comparing(Voraussetzung::getId))
                .toList();
    }

    /**
     * Bestimmt die Stufe einer Person aus ihren bestandenen Leistungen.
     *
     * @param regeln            Regeln aus {@link #regeln()}
     * @param leistungNachModul Leistungen der Person nach Modulcode (nur BESTANDEN zählt)
     * @return die Stufe
     */
    public Stufe stufe(List<Voraussetzung> regeln, Map<String, Leistung> leistungNachModul) {
        LocalDate heute = LocalDate.now();
        boolean mitZulassung = false;
        for (Voraussetzung regel : regeln) {
            PlanVerstoss verstoss = planPruefer.pruefeVoraussetzung(
                    BACHELORARBEIT, null, regel, Map.of(), leistungNachModul, heute);
            if (verstoss == null) {
                continue;
            }
            if (verstoss.schwere() == PruefSchwere.FEHLER) {
                return Stufe.NICHT_ZUGELASSEN;
            }
            mitZulassung = true;
        }
        return mitZulassung ? Stufe.MIT_ZULASSUNG : Stufe.REGULAER;
    }
}
