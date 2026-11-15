package de.htw.studyplanner.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Logik zu {@link HtwEmail}: vergleicht die Domain der Adresse mit den erlaubten Domains.
 */
public class HtwEmailValidator implements ConstraintValidator<HtwEmail, String> {

    private String[] domains;

    @Override
    public void initialize(HtwEmail annotation) {
        this.domains = annotation.domains();
    }

    /**
     * Prüft die Domain der Adresse.
     *
     * <p>Adressen ohne erkennbare Domain (leer, kein {@code @}, {@code @} ganz vorne oder
     * ganz hinten) gelten hier als gültig. Für sie melden {@code @NotBlank} bzw.
     * {@code @Email} den Fehler, sonst kämen zwei Meldungen für dasselbe Problem.
     *
     * @param value   die eingegebene E-Mail-Adresse
     * @param context wird nicht benutzt
     * @return {@code false} nur, wenn eine Domain vorhanden, aber nicht erlaubt ist
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int at = value.lastIndexOf('@');
        if (at <= 0 || at == value.length() - 1) {
            return true;
        }

        // Alles hinter dem letzten @ muss exakt passen. Deshalb fallen
        // "x@fakestudent.htw-berlin.de" und "student.htw-berlin.de@gmail.com" durch.
        String domain = value.substring(at + 1);
        for (String erlaubt : domains) {
            if (domain.equalsIgnoreCase(erlaubt)) {
                return true;
            }
        }
        return false;
    }
}
