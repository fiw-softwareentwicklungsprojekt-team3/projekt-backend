-- Markierung für generierte Testdaten (Ticket E1.5/E1.6).
-- DEFAULT false: echte Accounts, die später von Hand angelegt werden,
-- sind damit automatisch nicht als synthetisch markiert.
ALTER TABLE nutzende
    ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT false;

-- Voraussetzungen, die im selben Semester erfüllt werden dürfen (E28).
--
-- Betrifft nur B65 (Bachelorseminar und Abschlusskolloquium). Dessen beide
-- NOTWENDIG-Zeilen sind "B66 bestanden" und "177 LP". Die 177 LP sind
-- 180 - 3, enthalten also die 12 LP der Bachelorarbeit. Beides ist nach
-- Anlage 2 erst im selben Semester wie B65 zu erreichen: Bachelorarbeit und
-- Kolloquium schließen gemeinsam das 6. Fachsemester ab.
--
-- Mit gleiches_semester = true dürfen Module zählen, die im selben Semester
-- bestanden oder geplant sind. Für alle anderen Zeilen bleibt es bei der
-- Regel aus E2: ein früheres Semester.
ALTER TABLE voraussetzung
    ADD COLUMN gleiches_semester BOOLEAN NOT NULL DEFAULT false;

-- nur bei notwendigen Voraussetzungen sinnvoll; EMPFOHLEN ist ohnehin nur
-- eine Warnung
ALTER TABLE voraussetzung
    ADD CONSTRAINT chk_voraussetzung_gleiches_semester
        CHECK (NOT gleiches_semester OR art = 'NOTWENDIG');

UPDATE voraussetzung
   SET gleiches_semester = true
 WHERE modul_code = 'B65'
   AND art = 'NOTWENDIG';

-- Semester als Referenzdaten. Konvention:
--   WS wird mit seinem Startjahr geführt (WS 2024/25 -> jahr 2024, art 'WS')
--   SS wird mit seinem Kalenderjahr geführt (SS 2025  -> jahr 2025, art 'SS')
--
-- Eingefügt wird chronologisch, das ist aber nur Kosmetik: die semester_id
-- sagt nichts über die Zeit aus, weil die App später weitere Semester
-- anlegt, sobald sie gebraucht werden (E19). "Früher" und "später" werden
-- immer über jahr und art bestimmt, nie über die ID (siehe Abschnitt 3 im
-- Modelldokument).
INSERT INTO semester (jahr, art) VALUES
    (2021, 'WS'),
    (2022, 'SS'),
    (2022, 'WS'),
    (2023, 'SS'),
    (2023, 'WS'),
    (2024, 'SS'),
    (2024, 'WS'),
    (2025, 'SS'),
    (2025, 'WS'),
    (2026, 'SS'),
    (2026, 'WS');