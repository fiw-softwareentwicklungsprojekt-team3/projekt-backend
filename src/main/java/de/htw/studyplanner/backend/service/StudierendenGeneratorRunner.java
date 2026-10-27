package de.htw.studyplanner.backend.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Startet den Generator beim Hochfahren der Anwendung, aber nur im Profil
 * "generator":
 *
 * <pre>./mvnw spring-boot:run -Dspring-boot.run.profiles=generator</pre>
 *
 * Ohne dieses Profil passiert nichts. Das ist bewusst so: sonst würde der
 * Generator bei jedem normalen Start laufen, auch auf dem HTW-Server, und dort
 * Testdaten anlegen.
 */
@Component
@Profile("generator")
public class StudierendenGeneratorRunner implements CommandLineRunner {

    private final StudierendenGenerator generator;

    /**
     * Erstellt den Runner.
     *
     * @param generator der Generator, der beim Start ausgeführt wird
     */
    public StudierendenGeneratorRunner(StudierendenGenerator generator) {
        this.generator = generator;
    }

    /**
     * Führt den Generator mit der Standard-Konfiguration aus und gibt das Ergebnis aus.
     *
     * @param args Startargumente der Anwendung, werden nicht genutzt
     */
    @Override
    public void run(String... args) {
        System.out.println("--- Studierendengenerator startet ---");
        StudierendenGenerator.Ergebnis e = generator.generiere();
        System.out.printf("%d Studierende, %d Leistungen (%d bestanden, %d nicht bestanden)%n",
                e.studierende(), e.leistungen(), e.bestanden(), e.nichtBestanden());
        System.out.println("--- Studierendengenerator fertig ---");
    }
}
