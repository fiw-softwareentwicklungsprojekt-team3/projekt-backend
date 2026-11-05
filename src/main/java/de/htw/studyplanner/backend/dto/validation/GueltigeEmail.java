package de.htw.studyplanner.backend.dto.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * E-Mail-Regeln für alle Registrierungen (E2.2, E2.5): nicht leer, gültiges Format.
 *
 * <p>Die Regeln stehen nur hier. Wer sie ändert, ändert sie für Studierende und
 * Professor:innen gleichzeitig. Jede verletzte Regel erscheint mit ihrer eigenen
 * Meldung in {@code errors}.
 */
@NotBlank(message = "email darf nicht leer sein")
@Email(message = "email ist ungültig")
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface GueltigeEmail {

    /** @return Meldung (wird nicht genutzt, die Meldungen kommen von den einzelnen Regeln) */
    String message() default "email ist ungültig";

    /** @return Validierungsgruppen */
    Class<?>[] groups() default {};

    /** @return Payload */
    Class<? extends Payload>[] payload() default {};
}