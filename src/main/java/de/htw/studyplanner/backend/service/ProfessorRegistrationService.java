package de.htw.studyplanner.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.htw.studyplanner.backend.dto.ProfessorRegisterRequest;
import de.htw.studyplanner.backend.dto.ProfessorRegisterResponse;
import de.htw.studyplanner.backend.exception.ConflictException;
import de.htw.studyplanner.backend.exception.UngueltigerEinladungscodeException;
import de.htw.studyplanner.backend.model.Einladungscode;
import de.htw.studyplanner.backend.model.Nutzende;
import de.htw.studyplanner.backend.model.Rolle;

/**
 * Registriert Professor:innen mit einem Einladungscode (E2.5).
 */
@Service
public class ProfessorRegistrationService {

    private final KontoService kontoService;
    private final EinladungscodeService einladungscodeService;

    public ProfessorRegistrationService(KontoService kontoService,
                                        EinladungscodeService einladungscodeService) {
        this.kontoService = kontoService;
        this.einladungscodeService = einladungscodeService;
    }

    /**
     * Legt ein Konto mit der Rolle PROFESSOR_IN an und löst den Code ein.
     *
     * <p>Reihenfolge mit Absicht: Zuerst wird die E-Mail geprüft, dann der Code. Andersherum
     * könnte man mit einer bekannten, vergebenen E-Mail Codes durchprobieren: 409 hieße
     * „Code gültig“, 400 „Code ungültig“.
     *
     * <p>Alles läuft in einer Transaktion. Der Code bleibt bis zum Ende gesperrt, deshalb
     * entsteht auch bei zwei gleichzeitigen Anfragen mit demselben Code nur ein Konto.
     *
     * @throws ConflictException                  wenn die E-Mail schon vergeben ist (409)
     * @throws UngueltigerEinladungscodeException wenn der Code nicht einlösbar ist (400)
     */
    @Transactional
    public ProfessorRegisterResponse register(ProfessorRegisterRequest request) {
        kontoService.pruefeEmailFrei(request.email());

        Einladungscode code = einladungscodeService.sperreEinloesbarenCode(request.code());

        Nutzende nutzende = kontoService.legeKontoAn(
                request.email(), request.password(), Rolle.PROFESSOR_IN);

        einladungscodeService.einloesen(code, nutzende);

        return new ProfessorRegisterResponse(
                nutzende.getNutzendeId(),
                nutzende.getEmail(),
                nutzende.getRolle().name());
    }
}