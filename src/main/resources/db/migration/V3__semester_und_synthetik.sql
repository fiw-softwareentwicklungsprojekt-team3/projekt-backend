-- Markierung für generierte Testdaten (Ticket E1.5/E1.6).
-- DEFAULT false: echte Accounts, die später von Hand angelegt werden,
-- sind damit automatisch nicht als synthetisch markiert.
ALTER TABLE nutzende
    ADD COLUMN is_synthetic BOOLEAN NOT NULL DEFAULT false;

-- Semester als Referenzdaten. Konvention:
--   WS wird mit seinem Startjahr geführt (WS 2024/25 -> jahr 2024, art 'WS')
--   SS wird mit seinem Kalenderjahr geführt (SS 2025  -> jahr 2025, art 'SS')
-- Reihenfolge der INSERTs = chronologisch, damit die vergebenen
-- semester_id aufsteigend der Zeit folgen. Darauf baut die Prognose auf.
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