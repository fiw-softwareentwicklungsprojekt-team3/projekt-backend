# projekt-backend

[Zur Architektur-Dokumentation](docs/architektur.md)

![Backend CI](https://github.com/fiw-softwareentwicklungsprojekt-team3/projekt-backend/actions/workflows/backend.yml/badge.svg)

# Studyplanner Backend

Dieses Projekt nutzt Java 21 und Spring Boot 3.3.4.

## Anwendung lokal starten

Öffne ein Terminal im `backend`-Ordner und führe aus:
`./mvnw spring-boot:run`

## API Dokumentation
Sobald der Server läuft, ist die Swagger-UI erreichbar unter:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

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


