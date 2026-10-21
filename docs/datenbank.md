# Datenbank (PostgreSQL + Flyway)

## Überblick

- Die Tabellen werden **nur** über Flyway-Migrationen angelegt und geändert: `src/main/resources/db/migration`.
- Flyway läuft **automatisch beim Start** der Anwendung und führt alle noch fehlenden Migrationen aus.
- Welche Migrationen schon gelaufen sind, steht in der Tabelle `flyway_schema_history`.
- Zugangsdaten stehen in der `.env` im Projektordner (Vorlage: `.env.example`). Die `.env` ist in `.gitignore` und wird nie committet.

## Lokal entwickeln (Standard für alle)

Voraussetzung: [Docker Desktop](https://www.docker.com/products/docker-desktop/) ist installiert und gestartet.

```bash
cp .env.example .env          # einmalig, danach Passwort in .env ändern
docker compose up -d          # startet PostgreSQL auf localhost:5432
./mvnw spring-boot:run        # startet die App, Flyway legt die Tabellen an
```

Im Log steht dann z. B. `Successfully applied 1 migration to schema "public", now at version v1`.

- Datenbank stoppen: `docker compose down` (Daten bleiben erhalten)
- Datenbank komplett leeren: `docker compose down -v` (löscht alle Daten, beim nächsten Start legt Flyway alles neu an)
- Tabellen ansehen: `docker compose exec db psql -U studyplanner -d studyplanner -c '\dt'` oder in pgAdmin mit `localhost:5432`

Die Tests (`./mvnw test`) starten automatisch eine eigene, leere PostgreSQL im Docker-Container. Docker muss dafür laufen.

## HTW-Server

| | |
|---|---|
| Zugang | nur vor Ort an der HTW oder mit **VPN** |
| Datenbank anlegen | https://ocean.f4.htw-berlin.de/ → Login mit Moodle-Zugang → „Create Database“ → PostgreSQL |
| Host | `psql.f4.htw-berlin.de` |
| Port | `5432` |
| Benutzer | `s0` + Matrikelnummer |
| Passwort | Moodle-Passwort |
| Werkzeug | pgAdmin 4 |

**Wichtig:**
- Für das Projekt eine **neue, leere** Datenbank anlegen, nicht die aus dem Datenbanken-Kurs (`unischema`). Flyway bricht auf einer Datenbank ab, die schon Tabellen ohne Flyway-Historie enthält.
- Die Zugangsdaten sind die persönlichen Moodle-Daten. **Nie weitergeben**, nie committen, nicht im Chat teilen.

**Wer führt Migrationen auf dem HTW-Server aus?**
Die Projekt-Datenbank auf dem HTW-Server gehört <Name>. Nur diese Person startet die App gegen den HTW-Server, Flyway führt dabei die Migrationen aus. Alle anderen entwickeln lokal mit Docker. Wer selbst gegen den HTW-Server testen will, legt sich auf ocean eine eigene Datenbank mit dem eigenen Zugang an.

**App gegen den HTW-Server starten:** In der `.env` die HTW-Werte eintragen (siehe Kommentar in `.env.example`), VPN verbinden, dann `./mvnw spring-boot:run`. Zurück zu lokal: die lokalen Werte wieder eintragen.

## Regeln für Migrationen

1. **Eine bereits gemergte Migration wird nie geändert.** Flyway speichert eine Prüfsumme jeder Datei und bricht beim Start ab, wenn sich eine gelaufene Datei geändert hat.
2. Jede Änderung an der Datenbank kommt als **neue Datei** mit der nächsten Nummer: `V2__beschreibung.sql`, `V3__beschreibung.sql`, …
3. Dateiname: großes `V`, Nummer, **zwei** Unterstriche, Beschreibung in Kleinbuchstaben mit `_`, z. B. `V2__modul_seed.sql`.
4. Vor dem Anlegen einer neuen Migration `main` pullen, damit nicht zwei Personen dieselbe Nummer vergeben.
5. Tabellen und Spalten heißen deutsch wie im Datenmodell (Entscheidung E7).

**Was tun bei „Validate failed: Migration checksum mismatch“?** Lokal: `docker compose down -v` und neu starten. Danach die Änderung als neue Migration schreiben statt die alte Datei zu ändern.

## Synthetische Studierendendaten (Generator)

Für das Professor:innen-Dashboard und die Prognose brauchen wir Studierende mit
Noten. Echte Daten gibt es nicht, deshalb erzeugt ein Generator sie.

**Aufruf:**

```
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=generator
```

Ohne das Profil `generator` läuft der Generator **nicht**. Das ist Absicht:
sonst würde bei jedem normalen Start Testdaten angelegt, auch auf dem
HTW-Server.

**Was erzeugt wird:** fünf Kohorten (Startjahre 2021 bis 2025, jeweils
Wintersemester) mit je 30 bis 60 Personen, dazu ein Studienverlauf Semester für
Semester mit Noten, Wiederholungen und Wahlangeboten.

**Parameter** stehen in `StudierendenGenerator.Konfiguration`:

| Parameter | Standard | Bedeutung |
|---|---|---|
| `seed` | `42` | fester Startwert, gleicher Seed erzeugt gleiche Daten |
| `kohortenJahre` | 2021–2025 | Startjahre der Kohorten (immer WS) |
| `minProKohorte` / `maxProKohorte` | 30 / 60 | Kohortengröße |
| `durchfallquote` | `0.10` | Anteil nicht bestandener Versuche bei benoteten Modulen |
| `maxVersuche` | `4` | Versuche je Modul (RStPO) |
| `lpZielProSemester` | `30` | LP, die pro Semester angestrebt werden |

**Mehrfaches Ausführen** ist unproblematisch: der Generator löscht zu Beginn die
zuvor erzeugten Daten (alles mit `nutzende.is_synthetic = true`) und legt sie neu
an. Von Hand angelegte Accounts bleiben dabei erhalten.

**Welche Regeln gelten:** Die erzeugten Daten sind abgeschlossene Historie, keine
Planung. Beim Studienplan darf laut E2 auch ohne erfüllte Voraussetzung geplant
werden (das Frontend weist nur darauf hin) — hier dagegen gelten die
StuPO-Regeln strikt: ein Modul wird nur belegt, wenn seine NOTWENDIG-Vor­aus­set­zun­gen
zu diesem Zeitpunkt erfüllt sind, LP zählen nur aus bestandenen Modulen, und der
Turnus (WS/SS) wird beachtet.

**Hinweis zur Durchfallquote im Dashboard:** Weil eine Wiederholung dieselbe
Zeile überschreibt, steht in `leistung.status` immer nur der aktuelle Stand. Eine
Durchfallquote je Modul lässt sich deshalb nicht aus `status` ablesen, sondern
nur über `anzahl_versuche > 1`.
