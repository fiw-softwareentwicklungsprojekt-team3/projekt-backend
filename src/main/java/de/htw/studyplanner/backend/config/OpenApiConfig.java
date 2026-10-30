package de.htw.studyplanner.backend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Allgemeine Angaben für die OpenAPI-Spezifikation (Titel, Version, Beschreibung).
 * Die Spezifikation erzeugt springdoc aus den Controllern: {@code /v3/api-docs},
 * Swagger-UI unter {@code /swagger-ui.html}.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
@OpenAPIDefinition(info = @Info(
        title = "Studyplanner API",
        version = "0.0.1",
        description = """
                REST-API des Studyplanners für den Studiengang Informatik und Wirtschaft (HTW Berlin).

                Alle Endpunkte beginnen mit `/api` und tauschen JSON aus.

                **Authentifizierung:** Login über `POST /api/auth/login`. Das Token wird als
                `Authorization: Bearer <token>` mitgeschickt. Offen sind nur Registrierung, Login,
                `GET /api/health` und Swagger.
                """))
public class OpenApiConfig {
}