# Einladungscodes für Professor:innen (E2.5)

Professor:innen registrieren sich über `POST /api/auth/register/professor` mit
E-Mail, Passwort und einem **Einladungscode**. Ohne Code gibt es nur
`POST /api/auth/register`, und das legt immer ein Konto mit der Rolle
`STUDIERENDE` an.

Warum Einladungscode und nicht eine Liste erlaubter E-Mail-Adressen: siehe Wiki
→ „Entscheidungen“.

## Wie ein Code aussieht

- 12 Zeichen, zufällig mit `SecureRandom` erzeugt
- nur Großbuchstaben und Ziffern, **ohne** `0 O 1 I L` (gut abzutippen), z. B. `K7MPX4QRT9WZ`
- Groß-/Kleinschreibung und Leerzeichen am Rand sind bei der Eingabe egal
- 31^12 ≈ 7,9 · 10^17 Möglichkeiten
- jeder Code kann **genau einmal** eingelöst werden und hat ein Ablaufdatum

## Neue Codes anlegen (empfohlen)

Die Codes landen in der Datenbank, die in der `.env` eingetragen ist (lokal oder
HTW-Server).

```bash
# ein Code, 30 Tage gültig
./mvnw spring-boot:run -Dspring-boot.run.profiles=einladungscode

# drei Codes, 180 Tage gültig
./mvnw spring-boot:run -Dspring-boot.run.profiles=einladungscode \
  -Dspring-boot.run.arguments="--einladungscode.anzahl=3 --einladungscode.gueltig-tage=180"
```

Die Codes stehen im Log zwischen `--- Neue Einladungscodes ---` und `--- Ende ---`.
Danach die App mit `Strg+C` beenden.

**Codes nie committen, nie in Issues, PRs oder das öffentliche Wiki schreiben.**
Weitergabe nur direkt an die Professor:in oder im privaten Team-Kanal.

## Code für Demo und Messe

Für die Demo auf dem HTW-Server legt die Person, der die HTW-Datenbank gehört,
einen Code mit langer Gültigkeit an (z. B. `--einladungscode.gueltig-tage=180`)
und legt ihn im privaten Team-Kanal ab. Nach der Demo kann er gesperrt werden
(siehe unten). Lokal legt sich jede Person bei Bedarf selbst einen Code an.

## Nützliche SQL-Abfragen (pgAdmin / psql)

```sql
-- offene Codes
SELECT code, gueltig_bis FROM einladungscode
 WHERE eingeloest_von IS NULL AND gueltig_bis > now();

-- wer hat welchen Code eingelöst
SELECT e.code, n.email, e.eingeloest_am
  FROM einladungscode e JOIN nutzende n ON n.nutzende_id = e.eingeloest_von;

-- einen Code sperren (läuft sofort ab)
UPDATE einladungscode SET gueltig_bis = now() WHERE code = 'K7MPX4QRT9WZ';
```

Notlösung ohne laufende App (z. B. nur pgAdmin vorhanden). `random()` ist nicht
kryptografisch sicher, deshalb lieber das Profil oben nutzen:

```sql
INSERT INTO einladungscode (code, gueltig_bis)
SELECT string_agg(substr('ABCDEFGHJKMNPQRSTUVWXYZ23456789', (floor(random() * 31) + 1)::int, 1), ''),
       now() + interval '30 days'
  FROM generate_series(1, 12)
RETURNING code;
```

## Verhalten der API

| Fall | Antwort |
|---|---|
| gültiger Code, neue E-Mail | `201`, Rolle `PROFESSOR_IN`, Code wird mit Nutzende-ID und Zeitpunkt markiert |
| Code unbekannt, abgelaufen oder schon eingelöst | `400`, immer `"Der Einladungscode ist ungültig."` |
| E-Mail schon vergeben | `409`, **egal welcher Code** – der Code wird nicht verbraucht |
| E-Mail/Passwort ungültig, Code fehlt | `400` mit Feldern in `errors` (Regeln wie bei Studierenden) |
| zwei Anfragen gleichzeitig mit demselben Code | nur eine bekommt `201` (Zeilensperre `SELECT … FOR UPDATE`) |

Die E-Mail wird **vor** dem Code geprüft. Sonst könnte man mit einer bekannten,
vergebenen E-Mail Codes durchprobieren (409 hieße „Code gültig“).
