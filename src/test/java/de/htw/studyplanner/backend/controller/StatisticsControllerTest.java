package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.service.StatisticsService;

/**
 * Unit‑Test für {@link StatisticsController} ohne Spring‑Boot‑Test‑Umgebung.
 * Statt einer Unterklasse von {@link StatisticsService} wird ein Mock verwendet,
 * damit der Test auch nach einer zukünftigen Erweiterung des Services (z. B.
 * Repository‑Injection in US‑53a) kompiliert.
 */
class StatisticsControllerTest {

    private final StatisticsService statisticsService = mock(StatisticsService.class);
    private final StatisticsController controller = new StatisticsController(statisticsService);

    @Test
    void getCohortStatisticsLiefertErgebnisAusService() {
        List<CohortStatisticsDto> mockData = List.of(
                new CohortStatisticsDto("WS 2024", 45, 62.4, 34.7, 28, 0, 0)
        );
        when(statisticsService.getCohortStatistics()).thenReturn(mockData);

        List<CohortStatisticsDto> ergebnis = controller.getCohortStatistics();
        assertThat(ergebnis).isEqualTo(mockData);
    }

    @Test
    void getCohortStatisticsLiefertLeereListe() {
        when(statisticsService.getCohortStatistics()).thenReturn(List.of());

        List<CohortStatisticsDto> ergebnis = controller.getCohortStatistics();
        assertThat(ergebnis).isEmpty();
    }
}
