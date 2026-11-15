package de.htw.studyplanner.backend.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Prüft, dass eine E-Mail-Adresse zu einer erlaubten HTW-Domain gehört.
 *
 * <p>Geprüft wird nur der Teil hinter dem {@code @}, und zwar exakt und ohne Unterscheidung
 * von Groß- und Kleinschreibung. Ob die Adresse überhaupt wie eine E-Mail aussieht, prüft
 * weiterhin {@code @Email}. So gibt es zwei getrennte Fehlermeldungen: "E-Mail ungültig"
 * und "keine HTW-Adresse".
 *
 * <p>Beispiel: {@code @HtwEmail(domains = {HtwEmail.STUDIERENDE, HtwEmail.LEHRENDE})}
 */
@Documented
@Constraint(validatedBy = HtwEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface HtwEmail {

    /** Domain der Studierenden-Adressen. */
    String STUDIERENDE = "student.htw-berlin.de";

    /** Domain der Lehrenden-Adressen. */
    String LEHRENDE = "htw-berlin.de";

    /**
     * Erlaubte Domains, ohne {@code @}.
     *
     * @return die erlaubten Domains
     */
    String[] domains();

    /**
     * Fehlermeldung, wenn die Domain nicht erlaubt ist.
     *
     * @return die Fehlermeldung
     */
    String message() default "email muss eine HTW-Adresse sein";

    /**
     * Von Bean Validation vorgeschrieben, wird hier nicht benutzt.
     *
     * @return die Gruppen
     */
    Class<?>[] groups() default {};

    /**
     * Von Bean Validation vorgeschrieben, wird hier nicht benutzt.
     *
     * @return die Payload
     */
    Class<? extends Payload>[] payload() default {};
}
