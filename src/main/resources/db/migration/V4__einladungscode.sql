-- V4: Einladungscodes für die Registrierung von Professor:innen (E2.5)
--
-- Eine Professor:in registriert sich mit einem Code, den das Team vorher anlegt.
-- Jeder Code kann genau einmal eingelöst werden. Danach stehen hier die ID des
-- angelegten Kontos und der Zeitpunkt der Einlösung.
--
-- Hier stehen bewusst KEINE Codes: Das Repo ist öffentlich, ein Code im Repo
-- wäre für alle sichtbar. Wie das Team Codes anlegt, steht im Wiki
-- („Einladungscodes anlegen“) und in docs/einladungscodes.md.

CREATE TABLE einladungscode (
    code            VARCHAR(32)  PRIMARY KEY,
    gueltig_bis     TIMESTAMPTZ  NOT NULL,
    eingeloest_von  INTEGER      REFERENCES nutzende (nutzende_id),
    eingeloest_am   TIMESTAMPTZ,
    -- ein Konto kann höchstens einen Code eingelöst haben
    CONSTRAINT uq_einladungscode_eingeloest_von UNIQUE (eingeloest_von),
    -- eingelöst heißt: beide Spalten gesetzt, sonst beide leer
    CONSTRAINT chk_einladungscode_einloesung
        CHECK ((eingeloest_von IS NULL) = (eingeloest_am IS NULL))
);