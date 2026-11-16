package de.htw.studyplanner.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.MeResponse;
import de.htw.studyplanner.backend.exception.UnauthorizedException;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;
import de.htw.studyplanner.backend.security.AktuellePerson;

/**
 * Liefert die Kontodaten der Person, die die aktuelle Anfrage stellt.
 *
 * <p>Die ID kommt immer aus dem Token ({@link AktuellePerson#id()}) und nie aus
 * Pfad, Query oder Body. Sonst könnte jede angemeldete Person mit {@code ?id=5}
 * fremde Konten auslesen.
 *
 * <p>Gelesen wird trotzdem aus der Datenbank und nicht nur aus dem Token: Ein Token
 * ist bis zu einer Stunde gültig und kann eine Person beschreiben, die
 * inzwischen gelöscht wurde. Dieser Fall muss 401 ergeben und nicht 500.
 */
@Service
public class MeService {

    private final NutzendeRepository nutzendeRepository;
    private final StudierendeRepository studierendeRepository;

    /**
     * Erstellt den Service.
     *
     * @param nutzendeRepository    Zugriff auf die Tabelle nutzende
     * @param studierendeRepository Zugriff auf die Tabelle studierende (Startsemester)
     */
    public MeService(NutzendeRepository nutzendeRepository,
                     StudierendeRepository studierendeRepository) {
        this.nutzendeRepository = nutzendeRepository;
        this.studierendeRepository = studierendeRepository;
    }

    /**
     * Liest die angemeldete Person aus der Datenbank und baut die Antwort.
     *
     * <p>{@code @Transactional(readOnly = true)} ist nötig, weil das Startsemester
     * eine Lazy-Beziehung ist: Ohne offene Transaktion würde
     * {@code getStartsemester().label()} fehlschlagen ({@code open-in-view} ist aus).
     *
     * @return ID, E-Mail, Rolle und (nur bei Studierenden) das Startsemester
     * @throws UnauthorizedException wenn es die Person aus dem Token nicht mehr gibt
     */
    @Transactional(readOnly = true)
    public MeResponse aktuellesKonto() {
        Integer id = AktuellePerson.id();

        Nutzende nutzende = nutzendeRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        "Dein Konto existiert nicht mehr. Bitte melde dich erneut an."));

        String startSemester = null;
        if (nutzende.getRolle() == Rolle.STUDIERENDE) {
            startSemester = studierendeRepository.findById(id)
                    .map(studierende -> studierende.getStartsemester().label())
                    .orElse(null);
        }

        return new MeResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name(),
                startSemester);
    }
}
