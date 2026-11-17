package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.dto.ProfessorRegisterRequest;
import de.htw.studyplanner.backend.exception.UngueltigerEinladungscodeException;

/**
 * Zwei gleichzeitige Registrierungen mit demselben Code ergeben nur ein Konto (E2.5).
 *
 * <p>Absichtlich ohne {@code @Transactional}: Beide Threads brauchen eigene
 * Transaktionen, die wirklich committen, sonst wird die Zeilensperre nicht getestet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EinladungscodeGleichzeitigTest {

    @Autowired
    private ProfessorRegistrationService professorRegistrationService;

    @Autowired
    private EinladungscodeService einladungscodeService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void zweiGleichzeitigeVersucheErgebenNurEinKonto() throws Exception {
        String code = einladungscodeService.legeAn(1, Duration.ofDays(1)).get(0).getCode();
        List<String> emails = List.of("gleichzeitig.a@htw-berlin.de", "gleichzeitig.b@htw-berlin.de");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> ergebnisse = new ArrayList<>();
            for (String email : emails) {
                ergebnisse.add(pool.submit(() -> {
                    start.await();
                    try {
                        professorRegistrationService.register(
                                new ProfessorRegisterRequest(email, "geheimesPasswort", code));
                        return true;
                    } catch (UngueltigerEinladungscodeException e) {
                        return false;
                    }
                }));
            }
            start.countDown();

            int erfolge = 0;
            for (Future<Boolean> f : ergebnisse) {
                if (f.get(30, TimeUnit.SECONDS)) {
                    erfolge++;
                }
            }
            assertThat(erfolge).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        Integer konten = jdbc.queryForObject(
                "SELECT COUNT(*) FROM nutzende WHERE email IN (?, ?)", Integer.class,
                emails.get(0), emails.get(1));
        assertThat(konten).isEqualTo(1);

        Integer eingeloestVon = jdbc.queryForObject(
                "SELECT eingeloest_von FROM einladungscode WHERE code = ?", Integer.class, code);
        Integer kontoId = jdbc.queryForObject(
                "SELECT nutzende_id FROM nutzende WHERE email IN (?, ?)", Integer.class,
                emails.get(0), emails.get(1));
        assertThat(eingeloestVon).isEqualTo(kontoId);
    }
}
