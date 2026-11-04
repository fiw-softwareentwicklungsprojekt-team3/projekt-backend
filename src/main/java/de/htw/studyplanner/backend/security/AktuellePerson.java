package de.htw.studyplanner.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Hilfsklasse für Controller: liefert die ID der angemeldeten Person.
 * Die ID stammt immer aus dem Token und nie aus Pfad oder Body.
 */
public final class AktuellePerson {

    private AktuellePerson() {
    }

    /**
     * Liefert die nutzende_id der Person, die die aktuelle Anfrage stellt.
     *
     * @return nutzende_id aus dem Token
     * @throws IllegalStateException wenn keine angemeldete Person vorhanden ist
     */
    public static Integer id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AngemeldetePerson person)) {
            throw new IllegalStateException("Keine angemeldete Person im SecurityContext");
        }
        return person.id();
    }
}