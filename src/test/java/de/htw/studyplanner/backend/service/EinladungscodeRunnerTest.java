package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.htw.studyplanner.backend.model.Einladungscode;

/**
 * Prüft, dass der Runner die Codes anlegt und ausgibt.
 */
class EinladungscodeRunnerTest {

    @Test
    void legtCodesAnUndGibtSieAus() {
        EinladungscodeService service = mock(EinladungscodeService.class);
        when(service.legeAn(2, Duration.ofDays(14))).thenReturn(List.of(
                new Einladungscode("AAAABBBBCCCC", Instant.parse("2026-12-01T00:00:00Z")),
                new Einladungscode("DDDDEEEEFFFF", Instant.parse("2026-12-01T00:00:00Z"))));

        PrintStream alt = System.out;
        ByteArrayOutputStream ausgabe = new ByteArrayOutputStream();
        System.setOut(new PrintStream(ausgabe, true, StandardCharsets.UTF_8));
        try {
            new EinladungscodeRunner(service, 2, 14).run();
        } finally {
            System.setOut(alt);
        }

        verify(service).legeAn(2, Duration.ofDays(14));
        assertThat(ausgabe.toString(StandardCharsets.UTF_8))
                .contains("AAAABBBBCCCC", "DDDDEEEEFFFF");
    }
}
