package de.htw.studyplanner.backend.config;
feature/E2.3-login-jwt
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI backendOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Studyplanner Backend API")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(
                                BEARER_AUTH,
                                new SecurityScheme()

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
