package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.TestcontainersConfiguration;
import de.htw.studyplanner.backend.exception.NotFoundException;
import de.htw.studyplanner.backend.model.Modul;
import de.htw.studyplanner.backend.model.Wahlangebot;
import de.htw.studyplanner.backend.repository.ModulRepository;
import de.htw.studyplanner.backend.repository.WahlangebotRepository;

/**
 * Prüft die Zuordnung Modul → passende Angebote im {@link OfferService} mit den echten
 * Seed-Daten. Diese Methode nutzen sowohl der Modulkatalog als auch die Leistungen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class OfferServiceTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private ModulRepository modulRepository;

    @Autowired
    private WahlangebotRepository wahlangebotRepository;

    private Modul modul(String code) {
        return modulRepository.findById(code).orElseThrow();
    }

    private Wahlangebot angebot(String code) {
        return wahlangebotRepository.findById(code).orElseThrow();
    }

    private List<String> codes(String modulCode) {
        return offerService.passendeAngebote(modulCode).stream().map(Wahlangebot::getCode).toList();
    }

    @Test
    void passendeAngeboteNachModul() {
        assertThat(codes("B41")).hasSize(15).startsWith("WP1", "WP2").endsWith("WP15");
        assertThat(codes("B54")).hasSize(15);
        assertThat(codes("B16")).contains("EN_B2.1").doesNotContain("EN_B2.2").hasSize(5);
        assertThat(codes("B26")).contains("EN_B2.2").doesNotContain("EN_B2.1").hasSize(5);
        assertThat(codes("B63")).isEmpty();
        assertThat(codes("B64")).isEmpty();
        assertThat(codes("B34")).isEmpty();
    }

    @Test
    void unbekanntesModulWirft404() {
        assertThatThrownBy(() -> offerService.passendeAngebote("B99"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void passtZumModulNutztDieselbeRegel() {
        assertThat(offerService.passtZumModul(modul("B16"), angebot("EN_B2.1"))).isTrue();
        assertThat(offerService.passtZumModul(modul("B16"), angebot("EN_B2.2"))).isFalse();
        assertThat(offerService.passtZumModul(modul("B26"), angebot("EN_B2.2"))).isTrue();
        assertThat(offerService.passtZumModul(modul("B41"), angebot("WP7"))).isTrue();
        // ein WP-Angebot mit Niveaustufe 1a passt trotzdem nicht zu B16 (falsche Gruppe)
        assertThat(offerService.passtZumModul(modul("B16"), angebot("WP4"))).isFalse();
        assertThat(offerService.passtZumModul(modul("B41"), angebot("EN_B2.1"))).isFalse();
        assertThat(offerService.passtZumModul(modul("B63"), angebot("WP1"))).isFalse();
    }

    @Test
    void festeAuswahlNurBeiB16B26B41B54() {
        assertThat(offerService.hatFesteAuswahl(modul("B16"))).isTrue();
        assertThat(offerService.hatFesteAuswahl(modul("B26"))).isTrue();
        assertThat(offerService.hatFesteAuswahl(modul("B41"))).isTrue();
        assertThat(offerService.hatFesteAuswahl(modul("B54"))).isTrue();
        assertThat(offerService.hatFesteAuswahl(modul("B63"))).isFalse();
        assertThat(offerService.hatFesteAuswahl(modul("B34"))).isFalse();
    }

    @Test
    void codesWerdenWieGelesenSortiert() {
        List<String> sortiert = List.of("WP10", "WP2", "WP1", "EN_B2.2", "EN_B2.1", "DAF_B2.2").stream()
                .sorted(OfferService.CODE_REIHENFOLGE)
                .toList();
        assertThat(sortiert).containsExactly("DAF_B2.2", "EN_B2.1", "EN_B2.2", "WP1", "WP2", "WP10");
    }
}
