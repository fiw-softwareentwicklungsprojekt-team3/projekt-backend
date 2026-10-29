package de.htw.studyplanner.backend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Allgemeine Angaben für die OpenAPI-Spezifikation (Titel, Version, Beschreibung).
 *
 * <p>Die Spezifikation selbst erzeugt springdoc aus den Controllern. Sie ist unter
 * {@code /v3/api-docs} als JSON abrufbar, die Swagger-UI unter {@code /swagger-ui.html}.
 * Die Beschreibungen der einzelnen Endpunkte stehen direkt an den Controller-Methoden.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Studyplanner API",
        version = "0.0.1",
        description = """
                REST-API des Studyplanners für den Studiengang Informatik und Wirtschaft (HTW Berlin).

                Alle Endpunkte beginnen mit `/api` und tauschen JSON aus.

                **Fehlerformate**
                - Validierungsfehler (400) und Konflikte (409) bei der Registrierung: `{ "field", "message" }`
                - Alle anderen Fehler: Standardformat mit `timestamp`, `status`, `error`, `message`, `path`

                **Authentifizierung:** Es gibt noch keinen Login. Alle Endpunkte sind ohne Token erreichbar,
                deshalb kommen 401 und 403 aktuell nicht vor. Sobald JWT eingebaut ist, werden diese
                Statuscodes bei den geschützten Endpunkten ergänzt.
                """))
public class OpenApiConfig {
}
