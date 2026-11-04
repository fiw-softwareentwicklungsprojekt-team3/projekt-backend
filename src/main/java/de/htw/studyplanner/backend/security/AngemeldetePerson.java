package de.htw.studyplanner.backend.security;

/**
 * Die angemeldete Person, wie sie aus dem Token gelesen wird.
 *
 * @param id    nutzende_id aus dem Token-Subject
 * @param email E-Mail-Adresse aus dem Token
 */
public record AngemeldetePerson(Integer id, String email) {
}