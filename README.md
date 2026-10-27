# projekt-backend

[Zur Architektur-Dokumentation](docs/architektur.md)

![Backend CI](https://github.com/fiw-softwareentwicklungsprojekt-team3/projekt-backend/actions/workflows/backend.yml/badge.svg)

# Studyplanner Backend

Dieses Projekt nutzt Java 21 und Spring Boot 3.3.4.

## Anwendung lokal starten

Öffne ein Terminal im `backend`-Ordner und führe aus:
`./mvnw spring-boot:run`

## API Dokumentation
Sobald der Server läuft:

| Was | Link |
|---|---|
| Swagger-UI (Endpunkte ansehen und ausprobieren) | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI-Spezifikation als JSON (für das Frontend) | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

Das Frontend baut gegen die OpenAPI-Spezifikation. Daraus lassen sich z. B. TypeScript-Typen erzeugen.

Jeder Endpunkt ist in Swagger mit Beschreibung, Beispielwerten und den möglichen Statuscodes dokumentiert.
Die Annotationen stehen direkt an den Controller-Methoden (`@Operation`, `@ApiResponse`), die Beispielwerte an den DTOs (`@Schema`).
401 und 403 kommen noch nicht vor, weil es noch keinen Login gibt.

### JavaDoc
Öffentliche Controller- und Service-Klassen und ihre öffentlichen Methoden haben JavaDoc. Erzeugen mit:

```bash
./mvnw javadoc:javadoc
```

Ergebnis: `target/reports/apidocs/index.html` (bei älteren Plugin-Versionen `target/site/apidocs/index.html`).

Bei neuen Endpunkten gilt laut Definition of Done: JavaDoc und Swagger mit aktualisieren.

## Paketstruktur
| Schicht | Package |
|---|---|
| REST-API | `de.htw.studyplanner.backend.controller` |
| Service | `de.htw.studyplanner.backend.service` |
| Repository | `de.htw.studyplanner.backend.repository` |
| Entities | `de.htw.studyplanner.backend.model` |
| DTOs | `de.htw.studyplanner.backend.dto` |
| Konfiguration | `de.htw.studyplanner.backend.config` |

## Datenbank lokal starten

Voraussetzung: Docker Desktop läuft.

```bash
cp .env.example .env
docker compose up -d
./mvnw spring-boot:run
```

Details, HTW-Server und Regeln für Migrationen: [docs/datenbank.md](docs/datenbank.md)


