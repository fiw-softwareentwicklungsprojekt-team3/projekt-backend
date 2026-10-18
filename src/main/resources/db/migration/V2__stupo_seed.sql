-- V2: Echte Moduldaten der FIW-StuPO (E1.2)
--
-- Quellen:
--   StuPO Informatik und Wirtschaft vom 10.01.2024 (AMBl. HTW Berlin Nr. 08/24)
--     Anlage 2  Studienplanübersicht (Vollzeit)      -> Module, LP, SWS, NSt, NV, EV
--     Anlage 4  Wahlpflichtmodule                     -> WP1-WP15
--     Anlage 5  AWE-Module/Fremdsprachen (Variante 1) -> Sprachangebote
--     § 11 Abs. 4  Fachpraktikum: mindestens 60 LP
--     § 12         Bachelorarbeit: mindestens 151 LP
--     § 13         Kolloquium: 177 LP einschließlich Bachelorarbeit
--     § 15         Gewichtungstabelle -> zaehlt_in_x1
--   RStPO - Ba/Ma (AMBl. HTW Berlin Nr. 23/25)
--     § 25 Abs. 1 Satz 3  Toleranz von 10 LP
--     § 25 Abs. 1 Satz 4  Module der ersten drei Studienplansemester
--
-- Entscheidungen aus BD-modell-klassenentwurf-v2.md:
--   E9   Modulart folgt der Realität: nur B41 und B54 sind WP, alle anderen P
--   E10  mit_auswahl bei B16, B26, B41, B54, B63, B64.
--        B41/B54 wählen aus gruppe = WP, B16/B26 aus gruppe = SPRACHE,
--        B63/B64 haben keine Liste und tragen nur kurs_code/kurs_titel ein
--   E12  PS- und PÜ-Stunden zählen als Übung
--   E13  turnus aus der Parität des Fachsemesters (ungerade = WS, gerade = SS)
--   E16  bei Widersprüchen gilt Anlage 2
--
-- Abweichungen vom Ticket-Text, im Team entschieden:
--   E23  B43 setzt 60 LP voraus (im Ticket und in v2 des Modells übersehen)
--   E24  zaehlt_in_x1 ist zusätzlich bei B43, B65 und B66 false
--   E25  SWS von B43 und B66 werden als 0 geführt, nicht als NULL
--   E26  Sprachangebote werden je Niveau gespeichert, nicht je Sprache


-- ---------------------------------------------------------------------------
-- 1. Module (32 Zeilen, zusammen 180 LP)
-- ---------------------------------------------------------------------------
-- zaehlt_in_x1 = false bei B11-B16 (1. Fachsemester, RStPO § 17 Abs. 6),
-- bei B43 (unbenotet), bei B66 (Größe X2) und bei B65 (Größe X3).
-- Die verbleibenden Module ergeben die 115 LP der Tabelle in StuPO § 15.

INSERT INTO modul (code, bezeichnung, fachsemester, art, bereich, mit_auswahl, lp,
                   sws_vorlesung, sws_uebung, niveaustufe, turnus, zaehlt_in_x1, benotet) VALUES
-- 1. Fachsemester (30 LP)
('B11', 'Programmierung 1',                          1, 'P',  'FACH', FALSE,  6, 4, 2, '1a', 'WS', FALSE, TRUE),
('B12', 'Rechnerarchitektur/Betriebssysteme',        1, 'P',  'FACH', FALSE,  5, 2, 2, '1a', 'WS', FALSE, TRUE),
('B13', 'Grundlegende Konzepte der Informatik',      1, 'P',  'FACH', FALSE,  5, 4, 0, '1a', 'WS', FALSE, TRUE),
('B14', 'BWL 1: Grundlagen der BWL',                 1, 'P',  'FACH', FALSE,  5, 4, 0, '1a', 'WS', FALSE, TRUE),
('B15', 'Mathematik 1',                              1, 'P',  'FACH', FALSE,  5, 2, 2, '1a', 'WS', FALSE, TRUE),
('B16', '1. Fremdsprache 1',                         1, 'P',  'AWE',  TRUE,   4, 0, 4, '1a', 'WS', FALSE, TRUE),
-- 2. Fachsemester (30 LP)
('B21', 'Programmierung 2',                          2, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'SS', TRUE,  TRUE),
('B22', 'Mathematik 2',                              2, 'P',  'FACH', FALSE,  6, 2, 2, '1b', 'SS', TRUE,  TRUE),
('B23', 'Software-Engineering 1',                    2, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'SS', TRUE,  TRUE),
('B24', 'Webtechnologien',                           2, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'SS', TRUE,  TRUE),
('B25', 'BWL 2: Rechnungswesen',                     2, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'SS', TRUE,  TRUE),
('B26', '1. Fremdsprache 2',                         2, 'P',  'AWE',  TRUE,   4, 0, 4, '1b', 'SS', TRUE,  TRUE),
-- 3. Fachsemester (30 LP)
('B31', 'Data Science',                              3, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'WS', TRUE,  TRUE),
('B32', 'Software-Engineering 2',                    3, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'WS', TRUE,  TRUE),
('B33', 'Datenbanksysteme',                          3, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'WS', TRUE,  TRUE),
('B34', 'Softwareentwicklungsprojekt',               3, 'P',  'FACH', FALSE, 10, 0, 6, '1b', 'WS', TRUE,  TRUE),
('B35', 'Projektmanagement',                         3, 'P',  'FACH', FALSE,  5, 0, 4, '1b', 'WS', TRUE,  TRUE),
-- 4. Fachsemester (30 LP)
('B41', 'Wahlpflichtmodul (Wirtschafts-)Informatik 1', 4, 'WP', 'FACH', TRUE,  5, 0, 4, NULL, 'SS', TRUE,  TRUE),
('B42', 'Wissenschaftliches Schreiben',              4, 'P',  'FACH', FALSE,  5, 0, 2, '1b', 'SS', TRUE,  TRUE),
('B43', 'Fachpraktikum',                             4, 'P',  'FACH', FALSE, 20, 0, 0, NULL, 'SS', FALSE, FALSE),
-- 5. Fachsemester (31 LP, Mobilitätsfenster)
('B51', 'Human Computer Interaction',                5, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'WS', TRUE,  TRUE),
('B52', 'Verteilte Systeme',                         5, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'WS', TRUE,  TRUE),
('B53', 'Datenschutz und Datensicherheit',           5, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'WS', TRUE,  TRUE),
('B54', 'Wahlpflichtmodul (Wirtschafts-)Informatik 2', 5, 'WP', 'FACH', TRUE,  5, 0, 4, NULL, 'WS', TRUE,  TRUE),
('B55', 'Informatik und Gesellschaft',               5, 'P',  'FACH', FALSE,  5, 4, 0, '1b', 'WS', TRUE,  TRUE),
('B56', 'Projekte in der Wirtschaft',                5, 'P',  'FACH', FALSE,  6, 0, 4, '1b', 'WS', TRUE,  TRUE),
-- 6. Fachsemester (29 LP)
('B61', 'Modellierung von Informationssystemen',     6, 'P',  'FACH', FALSE,  5, 2, 2, '1b', 'SS', TRUE,  TRUE),
('B62', 'BWL 3: Berufspraxis IT-Wirtschaft',         6, 'P',  'FACH', FALSE,  5, 0, 2, '1b', 'SS', TRUE,  TRUE),
('B63', 'AWE-Modul 1',                               6, 'P',  'AWE',  TRUE,   2, 0, 2, '1a', 'SS', TRUE,  TRUE),
('B64', 'AWE-Modul 2',                               6, 'P',  'AWE',  TRUE,   2, 0, 2, '1a', 'SS', TRUE,  TRUE),
('B65', 'Bachelorseminar und Abschlusskolloquium',   6, 'P',  'FACH', FALSE,  3, 0, 1, '1b', 'SS', FALSE, TRUE),
('B66', 'Bachelorarbeit',                            6, 'P',  'FACH', FALSE, 12, 0, 0, '1b', 'SS', FALSE, TRUE);


-- ---------------------------------------------------------------------------
-- 2. Modulteile (5 Zeilen)
-- ---------------------------------------------------------------------------
-- Nur die Module, die in Anlage 2 in Teile zerfallen. Die SWS der Teile
-- ergeben zusammen die sws_uebung des Moduls (E12).

INSERT INTO modul_teil (modul_code, teil_code, bezeichnung, sws) VALUES
('B34', 'B34.1', 'Softwareentwicklungsprojekt',        4),
('B34', 'B34.2', 'Arbeitswelt IT',                     2),
('B35', 'B35.1', 'Agiles Projektmanagement',           2),
('B35', 'B35.2', 'Konfliktmanagement und Mediation',   2),
('B65', 'B65.1', 'Bachelorseminar',                    1);


-- ---------------------------------------------------------------------------
-- 3. Wahlangebote (25 Zeilen)
-- ---------------------------------------------------------------------------
-- gruppe = 'WP'      : Auswahl für B41 und B54 (Anlage 4)
-- gruppe = 'SPRACHE' : Auswahl für B16 und B26 (Anlage 5, Variante 1)
--
-- Die Sprachen stehen je Niveau in einer eigenen Zeile, weil dieselbe Sprache
-- in B16 und B26 auf unterschiedlichen GER-Niveaus unterrichtet wird.
-- niveaustufe trennt dabei die beiden Module: '1a' gehört zu B16, '1b' zu B26.
-- Das GER-Niveau selbst steht nur in code und bezeichnung.
-- Für B63/B64 gibt es bewusst keine Zeilen: dort wird frei eingetragen (E10).

INSERT INTO wahlangebot (code, bezeichnung, gruppe, niveaustufe, sws, empfohlen_modul_code) VALUES
-- Wahlpflichtangebote für B41 und B54 (Anlage 4)
('WP1',  'Machine Learning',                                'WP', '1b', 4, 'B31'),
('WP2',  'Deep Learning',                                   'WP', '1b', 4, 'B31'),
('WP3',  'Vertiefung in Datenbanken und Datenstrukturen',   'WP', '1b', 4, 'B33'),
('WP4',  'Vertiefung in Datenschutz und Datensicherheit',   'WP', '1a', 4, NULL),
('WP5',  'IT-Security',                                     'WP', '1a', 4, NULL),
('WP6',  'Programmierung mit Python',                       'WP', '1b', 4, 'B24'),
('WP7',  'DevOps',                                          'WP', '1b', 4, 'B32'),
('WP8',  'Cloud Computing',                                 'WP', '1b', 4, 'B24'),
('WP9',  'Micro-Service-Architekturen',                     'WP', '1b', 4, 'B24'),
('WP10', 'Ethik in Informatik',                             'WP', '1a', 4, NULL),
('WP11', 'Business Intelligence',                           'WP', '1b', 4, 'B33'),
('WP12', 'Design Patterns',                                 'WP', '1b', 4, 'B22'),
('WP13', 'Progressive Web Apps',                            'WP', '1b', 4, 'B24'),
('WP14', 'Besondere Kapitel der Informatik',                'WP', '1a', 4, NULL),
('WP15', 'Besondere Kapitel der Wirtschaftsinformatik',     'WP', '1a', 4, NULL),
-- Sprachangebote für B16 (niveaustufe 1a)
('EN_B2.1',  'Englisch Fachsprache B2.1 (Technik)',                        'SPRACHE', '1a', 4, NULL),
('FR_B1.2',  'Französisch Fachsprache B1.2 (Wirtschaft)',                  'SPRACHE', '1a', 4, NULL),
('RU_B1.2',  'Russisch Fachsprache B1.2 (Wirtschaft)',                     'SPRACHE', '1a', 4, NULL),
('ES_B1.2',  'Spanisch Fachsprache B1.2 (Wirtschaft)',                     'SPRACHE', '1a', 4, NULL),
('DAF_B2.2', 'Deutsch als Fremdsprache B2.2 (Technik/Wirtschaft)',         'SPRACHE', '1a', 4, NULL),
-- Sprachangebote für B26 (niveaustufe 1b)
('EN_B2.2',  'Englisch Fachsprache B2.2 (Technik)',                        'SPRACHE', '1b', 4, NULL),
('FR_B2.1',  'Französisch Fachsprache B2.1 (Wirtschaft)',                  'SPRACHE', '1b', 4, NULL),
('RU_B2.1',  'Russisch Fachsprache B2.1 (Wirtschaft)',                     'SPRACHE', '1b', 4, NULL),
('ES_B2.1',  'Spanisch Fachsprache B2.1 (Wirtschaft)',                     'SPRACHE', '1b', 4, NULL),
('DAF_C1.1', 'Deutsch als Fremdsprache C1.1 (Technik/Wirtschaft)',         'SPRACHE', '1b', 4, NULL);


-- ---------------------------------------------------------------------------
-- 4. Voraussetzungen (47 Zeilen: 23 NOTWENDIG, 24 EMPFOHLEN)
-- ---------------------------------------------------------------------------

-- 4a) Notwendige Voraussetzungen auf einzelne Module (Anlage 2)
INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code) VALUES
('B34', 'NOTWENDIG', 'B11'),   -- Softwareentwicklungsprojekt braucht Programmierung 1
('B56', 'NOTWENDIG', 'B34'),   -- Projekte in der Wirtschaft brauchen das Softwareentwicklungsprojekt
('B65', 'NOTWENDIG', 'B66');   -- Kolloquium braucht die bestandene Bachelorarbeit (StuPO § 13)

-- 4b) Notwendige Voraussetzungen auf eine Mindestzahl an Leistungspunkten
INSERT INTO voraussetzung (modul_code, art, min_lp, toleranz_lp) VALUES
('B43', 'NOTWENDIG',  60,  0),   -- Fachpraktikum, StuPO § 11 Abs. 4 und Anlage 2
('B66', 'NOTWENDIG', 151, 10),   -- Bachelorarbeit, StuPO § 12 und RStPO § 25 Abs. 1 Satz 3
('B65', 'NOTWENDIG', 177,  0);   -- Kolloquium, StuPO § 13

-- 4c) Bachelorarbeit: zusätzlich alle Module der ersten drei Studienplansemester
--     (RStPO § 25 Abs. 1 Satz 4), ohne Toleranz. 17 Zeilen.
INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code)
SELECT 'B66', 'NOTWENDIG', code
FROM modul
WHERE fachsemester <= 3;

-- 4d) Empfohlene Voraussetzungen (Anlage 2, Spalte EV). 24 Zeilen.
INSERT INTO voraussetzung (modul_code, art, vorausgesetzt_modul_code) VALUES
('B21', 'EMPFOHLEN', 'B11'),
('B21', 'EMPFOHLEN', 'B13'),
('B22', 'EMPFOHLEN', 'B15'),
('B23', 'EMPFOHLEN', 'B11'),
('B23', 'EMPFOHLEN', 'B13'),
('B24', 'EMPFOHLEN', 'B11'),
('B25', 'EMPFOHLEN', 'B14'),
('B26', 'EMPFOHLEN', 'B16'),
('B31', 'EMPFOHLEN', 'B13'),
('B32', 'EMPFOHLEN', 'B23'),
('B33', 'EMPFOHLEN', 'B24'),
('B34', 'EMPFOHLEN', 'B21'),
('B34', 'EMPFOHLEN', 'B24'),
('B35', 'EMPFOHLEN', 'B23'),
('B51', 'EMPFOHLEN', 'B32'),
('B52', 'EMPFOHLEN', 'B24'),
('B52', 'EMPFOHLEN', 'B31'),
('B53', 'EMPFOHLEN', 'B12'),
('B55', 'EMPFOHLEN', 'B42'),
('B56', 'EMPFOHLEN', 'B21'),
('B61', 'EMPFOHLEN', 'B33'),
('B61', 'EMPFOHLEN', 'B51'),
('B62', 'EMPFOHLEN', 'B14'),
('B62', 'EMPFOHLEN', 'B25');
