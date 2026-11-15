package de.htw.studyplanner.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Prüft die E-Mail-Regeln von {@link RegisterRequest} und {@link LoginRequest} ohne Spring
 * und ohne Datenbank. Es gibt zwei getrennte Fehlerfälle: Die Adresse ist keine gültige
 * E-Mail, oder sie ist gültig, gehört aber nicht zu einer erlaubten HTW-Domain.
 */
class EmailDomainValidationTest {

    private static final String FORMAT_REGISTER = "email ist ungültig";
    private static final String DOMAIN_REGISTER = "email muss auf @student.htw-berlin.de enden";
    private static final String FORMAT_LOGIN = "email ist ungültig";
    private static final String DOMAIN_LOGIN =
            "email muss auf @student.htw-berlin.de oder @htw-berlin.de enden";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void validatorErstellen() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void validatorSchliessen() {
        factory.close();
    }

    /** Fehlermeldungen, die beim Registrieren für diese E-Mail entstehen. */
    private List<String> registerFehler(String email) {
        return validator.validate(new RegisterRequest(email, "geheimesPasswort", "WS 2025"))
                .stream().map(v -> v.getMessage()).toList();
    }

    /** Fehlermeldungen, die beim Login für diese E-Mail entstehen. */
    private List<String> loginFehler(String email) {
        return validator.validate(new LoginRequest(email, "geheimesPasswort"))
                .stream().map(v -> v.getMessage()).toList();
    }

    // ---------- Registrierung: nur @student.htw-berlin.de ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "name@student.htw-berlin.de",
            "Name@Student.HTW-Berlin.de",
            "VORNAME.NACHNAME@STUDENT.HTW-BERLIN.DE"
    })
    void registrierungAkzeptiertStudierendenAdressen(String email) {
        assertThat(registerFehler(email)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"max.mustermann", "max@", "@student.htw-berlin.de"})
    void registrierungMeldetBeiUngueltigemFormatNurDenFormatFehler(String email) {
        assertThat(registerFehler(email)).containsExactly(FORMAT_REGISTER);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name@gmail.com",
            "name@htw-berlin.de",
            "name@fakestudent.htw-berlin.de",
            "name@sub.student.htw-berlin.de",
            "name@student.htw-berlin.de.example.com",
            "student.htw-berlin.de@gmail.com"
    })
    void registrierungMeldetBeiFalscherDomainNurDenDomainFehler(String email) {
        assertThat(registerFehler(email)).containsExactly(DOMAIN_REGISTER);
    }

    // ---------- Login: @student.htw-berlin.de und @htw-berlin.de ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "name@student.htw-berlin.de",
            "Name@Student.HTW-Berlin.de",
            "name@htw-berlin.de",
            "Name@HTW-Berlin.DE"
    })
    void loginAkzeptiertStudierendenUndLehrendenAdressen(String email) {
        assertThat(loginFehler(email)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"max.mustermann", "max@", "@htw-berlin.de"})
    void loginMeldetBeiUngueltigemFormatNurDenFormatFehler(String email) {
        assertThat(loginFehler(email)).containsExactly(FORMAT_LOGIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name@gmail.com",
            "name@fakestudent.htw-berlin.de",
            "name@fakehtw-berlin.de",
            "name@htw-berlin.de.example.com",
            "htw-berlin.de@gmail.com"
    })
    void loginMeldetBeiFalscherDomainNurDenDomainFehler(String email) {
        assertThat(loginFehler(email)).containsExactly(DOMAIN_LOGIN);
    }
}
