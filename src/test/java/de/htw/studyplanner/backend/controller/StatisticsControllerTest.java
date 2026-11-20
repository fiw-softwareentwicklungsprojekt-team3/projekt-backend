package de.htw.studyplanner.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.htw.studyplanner.backend.dto.CohortStatisticsDto;
import de.htw.studyplanner.backend.service.StatisticsService;

class StatisticsControllerTest {

    static class TestStatisticsService extends StatisticsService {
        List<CohortStatisticsDto> daten = List.of();

        @Override
        public List<CohortStatisticsDto> getCohortStatistics() {
            return daten;
        }
    }

    private final TestStatisticsService statisticsService = new TestStatisticsService();
    private final StatisticsController controller = new StatisticsController(statisticsService);

    @Test
    void getCohortStatisticsLiefertErgebnisAusService() {
        List<CohortStatisticsDto> mockData = List.of(
                new CohortStatisticsDto("WS 2024", 45, 62.4, 34.7, 28, 0)
        );
        statisticsService.daten = mockData;

        List<CohortStatisticsDto> ergebnis = controller.getCohortStatistics();

        assertThat(ergebnis).isEqualTo(mockData);
    }

    @Test
    void getCohortStatisticsLiefertLeereListe() {
        statisticsService.daten = List.of();

        List<CohortStatisticsDto> ergebnis = controller.getCohortStatistics();

        assertThat(ergebnis).isEmpty();
    }
}
