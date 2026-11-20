package de.htw.studyplanner.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import de.htw.studyplanner.backend.dto.CohortStatisticsDto;

/**
 * Service für anonymisierte Statistiken für Lehrende.
 *
 * <p>Die Berechnung und Aggregation der tatsächlichen Kennzahlen aus den Leistungen
 * wird im Service-Ticket US-53a (#112) implementiert.
 */
@Service
public class StatisticsService {

    /**
     * Liefert die anonymisierten Kohorten-Statistiken aller Jahrgänge in chronologischer Reihenfolge.
     *
     * @return Liste der Kohorten-Statistiken oder eine leere Liste, wenn keine Studierenden vorhanden sind
     */
    public List<CohortStatisticsDto> getCohortStatistics() {
        return List.of();
    }
}
