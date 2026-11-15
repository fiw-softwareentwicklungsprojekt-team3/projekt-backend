package de.htw.studyplanner.backend.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.htw.studyplanner.backend.model.Einladungscode;

/**
 * Legt beim Start neue Einladungscodes an und gibt sie aus, aber nur im Profil "einladungscode":
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=einladungscode</pre>
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=einladungscode \
 *     -Dspring-boot.run.arguments="--einladungscode.anzahl=3 --einladungscode.gueltig-tage=14"</pre>
 */
@Component
@Profile("einladungscode")
public class EinladungscodeRunner implements CommandLineRunner {

    private final EinladungscodeService service;
    private final int anzahl;
    private final int gueltigTage;

    public EinladungscodeRunner(
            EinladungscodeService service,
            @Value("${einladungscode.anzahl:1}") int anzahl,
            @Value("${einladungscode.gueltig-tage:30}") int gueltigTage) {
        this.service = service;
        this.anzahl = anzahl;
        this.gueltigTage = gueltigTage;
    }

    @Override
    public void run(String... args) {
        List<Einladungscode> codes = service.legeAn(anzahl, Duration.ofDays(gueltigTage));
        System.out.println("--- Neue Einladungscodes (nur jetzt sichtbar, nicht ins Repo!) ---");
        for (Einladungscode c : codes) {
            System.out.printf("%s   gültig bis %s%n", c.getCode(), c.getGueltigBis());
        }
        System.out.println("--- Ende ---");
    }
}