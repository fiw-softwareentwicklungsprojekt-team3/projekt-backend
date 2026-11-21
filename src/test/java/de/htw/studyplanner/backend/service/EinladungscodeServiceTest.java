package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.htw.studyplanner.backend.exception.UngueltigerEinladungscodeException;
import de.htw.studyplanner.backend.model.Einladungscode;
import de.htw.studyplanner.backend.repository.EinladungscodeRepository;

/**
 * Unit-Tests ohne Datenbank für {@link EinladungscodeService}.
 */
class EinladungscodeServiceTest {

    private final EinladungscodeRepository repository = mock(EinladungscodeRepository.class);
    private final EinladungscodeService service = new EinladungscodeService(repository);

    @Test
    void codeHat12ZeichenAusDemErlaubtenAlphabet() {
        for (int i = 0; i < 200; i++) {
            String code = service.erzeugeCode();
            assertThat(code).hasSize(EinladungscodeService.CODE_LAENGE);
            for (char c : code.toCharArray()) {
                assertThat(EinladungscodeService.ALPHABET).contains(String.valueOf(c));
            }
        }
    }

    @Test
    void codesSindZufaellig() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            codes.add(service.erzeugeCode());
        }
        assertThat(codes).hasSize(10_000);
    }

    @Test
    void alphabetEnthaeltKeineVerwechselbarenZeichen() {
        assertThat(EinladungscodeService.ALPHABET).doesNotContain("0", "O", "1", "I", "L");
    }

    @Test
    void legeAnSpeichertDieGewuenschteAnzahl() {
        when(repository.existsById(anyString())).thenReturn(false);
        when(repository.save(any(Einladungscode.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Einladungscode> codes = service.legeAn(3, Duration.ofDays(7));

        assertThat(codes).hasSize(3);
        assertThat(codes).allSatisfy(c -> {
            assertThat(c.getGueltigBis()).isAfter(Instant.now().plus(Duration.ofDays(6)));
            assertThat(c.getEingeloestVon()).isNull();
        });
    }

    @Test
    void legeAnErzeugtNeuenCodeBeiKollision() {
        when(repository.existsById(anyString())).thenReturn(true, false);
        when(repository.save(any(Einladungscode.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.legeAn(1, Duration.ofDays(1))).hasSize(1);
    }

    @Test
    void legeAnOhneAnzahlIstFehler() {
        assertThatThrownBy(() -> service.legeAn(0, Duration.ofDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void leererOderZuLangerCodeGehtNichtAnDieDatenbank() {
        assertThatThrownBy(() -> service.sperreEinloesbarenCode(null))
                .isInstanceOf(UngueltigerEinladungscodeException.class);
        assertThatThrownBy(() -> service.sperreEinloesbarenCode("   "))
                .isInstanceOf(UngueltigerEinladungscodeException.class);
        assertThatThrownBy(() -> service.sperreEinloesbarenCode("X".repeat(33)))
                .isInstanceOf(UngueltigerEinladungscodeException.class);

        verify(repository, never()).findByCodeForUpdate(anyString());
    }

    @Test
    void einladungscodeIstNurVorAblaufUndOhneEinloesungEinloesbar() {
        Instant jetzt = Instant.now();
        Einladungscode code = new Einladungscode("ABCDEFGHJKMN", jetzt.plusSeconds(60));

        assertThat(code.istEinloesbar(jetzt)).isTrue();
        assertThat(code.istEinloesbar(jetzt.plusSeconds(61))).isFalse();
        assertThat(code.toString()).doesNotContain("ABCDEFGHJKMN");
    }
}
