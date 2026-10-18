package de.htw.studyplanner.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Startet für Tests eine leere PostgreSQL im Docker-Container.
 * Spring verbindet sich automatisch damit, die .env wird dabei ignoriert.
 *
 * Die Version muss zum HTW-Server passen, auf dem die Anwendung am Ende
 * läuft, und damit auch zu der aus docker-compose.yml. Sonst werden die
 * Tests gegen eine andere Hauptversion ausgeführt als die, gegen die
 * entwickelt und ausgeliefert wird.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:12"));
    }
}
