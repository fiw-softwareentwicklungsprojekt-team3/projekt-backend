# Backend Architektur

**Diagramm:** [`architektur.mmd`](architektur.mmd) (wird von GitHub direkt als Grafik angezeigt)

## Pakete und Schichten

| Schicht | Paket | Aufgabe |
|---|---|---|
| REST-API | `de.htw.studyplanner.backend.controller` | Endpunkte, nimmt Requests an, keine Logik |
| Service | `de.htw.studyplanner.backend.service` | Semesterlogik, Planprüfung, Fortschritt, Statistik |
| Repository | `de.htw.studyplanner.backend.repository` | Datenbankzugriff (Spring Data JPA) |
| Entities | `de.htw.studyplanner.backend.model` | Abbild der Tabellen |
| DTOs | `de.htw.studyplanner.backend.dto` | Daten, die über die API gehen |
| Konfiguration | `de.htw.studyplanner.backend.config` | CORS, Security, OpenAPI |



## Sprache der API-Pfade

API-Pfade sind englisch und im Plural, z. B. `GET /api/modules`.
Entities und Tabellen bleiben deutsch, die Übersetzung passiert im Controller bzw. in den DTOs.


## Zugriff nach Rolle

Es gibt nur die Rollen STUDIERENDE und PROFESSOR_IN, keine ADMIN-Rolle.
Ohne Token antwortet die API mit 401, bei falscher Rolle mit 403.

| Pfad | STUDIERENDE | PROFESSOR_IN |
|---|---|---|
| `/api/plan/**` | erlaubt | 403 |
| `/api/results/**` | erlaubt | 403 |
| `/api/statistics/**` | 403 | erlaubt |
| `/api/modules/**`, `GET /api/me` | erlaubt | erlaubt |
| `POST /api/auth/**`, `GET /api/health`, Swagger | ohne Token erlaubt | ohne Token erlaubt |

Die ID der angemeldeten Person liefert `AktuellePerson.id()` aus dem Token.
Sie wird nie aus Pfad oder Body gelesen.