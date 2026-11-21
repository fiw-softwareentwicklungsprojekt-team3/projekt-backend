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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Passwort-Regeln für alle Registrierungen (E2.2, E2.5): nicht leer, mindestens 8 Zeichen.
 *
 * <p>Die Regeln stehen nur hier, damit Studierende und Professor:innen immer
 * dieselben Regeln haben.
 */
@NotBlank(message = "password darf nicht leer sein")
@Size(min = 8, message = "password muss mindestens 8 Zeichen haben")
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface GueltigesPasswort {

    /** @return Meldung (wird nicht genutzt, die Meldungen kommen von den einzelnen Regeln) */
    String message() default "password ist ungültig";

    /** @return Validierungsgruppen */
    Class<?>[] groups() default {};

    /** @return Payload */
    Class<? extends Payload>[] payload() default {};
}