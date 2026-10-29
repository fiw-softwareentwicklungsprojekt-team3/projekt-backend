package de.htw.studyplanner.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import de.htw.studyplanner.backend.controller.ApiExceptionHandler;
import de.htw.studyplanner.backend.dto.RegisterRequest;
import de.htw.studyplanner.backend.dto.RegisterResponse;
import de.htw.studyplanner.backend.model.Semester;
import de.htw.studyplanner.backend.model.SemesterArt;
import de.htw.studyplanner.backend.repository.NutzendeRepository;
import de.htw.studyplanner.backend.repository.SemesterRepository;
import de.htw.studyplanner.backend.repository.StudierendeRepository;

@SuppressWarnings("null")
class SemesterServiceTest {

    private final SemesterRepository semesterRepository = mock(SemesterRepository.class);
    private final SemesterService semesterService = new SemesterService(semesterRepository);

    // ------------------------------------------------------------------
    // aktuellesSemester(LocalDate) – Grenzen und Beispielwerte
    // ------------------------------------------------------------------

    @Test
    void aktuellesSemesterGrenzeMaerzAufApril() {
        Semester am31Maerz = semesterService.aktuellesSemester(LocalDate.of(2027, 3, 31));
        assertThat(am31Maerz.getArt()).isEqualTo(SemesterArt.WS);
        assertThat(am31Maerz.getJahr()).isEqualTo(2026);
        assertThat(am31Maerz.label()).isEqualTo("WS 2026");

        Semester am01April = semesterService.aktuellesSemester(LocalDate.of(2027, 4, 1));
        assertThat(am01April.getArt()).isEqualTo(SemesterArt.SS);
        assertThat(am01April.getJahr()).isEqualTo(2027);
        assertThat(am01April.label()).isEqualTo("SS 2027");
    }

    @Test
    void aktuellesSemesterGrenzeSeptemberAufOktober() {
        Semester am30September = semesterService.aktuellesSemester(LocalDate.of(2026, 9, 30));
        assertThat(am30September.getArt()).isEqualTo(SemesterArt.SS);
        assertThat(am30September.getJahr()).isEqualTo(2026);
        assertThat(am30September.label()).isEqualTo("SS 2026");

        Semester am01Oktober = semesterService.aktuellesSemester(LocalDate.of(2026, 10, 1));
        assertThat(am01Oktober.getArt()).isEqualTo(SemesterArt.WS);
        assertThat(am01Oktober.getJahr()).isEqualTo(2026);
        assertThat(am01Oktober.label()).isEqualTo("WS 2026");
    }

    @Test
    void aktuellesSemesterImFebruarLiefertWsVomVorjahr() {
        Semester imFebruar = semesterService.aktuellesSemester(LocalDate.of(2027, 2, 15));
        assertThat(imFebruar.getArt()).isEqualTo(SemesterArt.WS);
        assertThat(imFebruar.getJahr()).isEqualTo(2026);
        assertThat(imFebruar.label()).isEqualTo("WS 2026");
    }

    // ------------------------------------------------------------------
    // parse(String) – gültige und ungültige Eingaben
    // ------------------------------------------------------------------

    @Test
    void parseGueltigeSemesterStrings() {
        Semester ss = semesterService.parse("SS 2027");
        assertThat(ss.getJahr()).isEqualTo(2027);
        assertThat(ss.getArt()).isEqualTo(SemesterArt.SS);
        assertThat(ss.label()).isEqualTo("SS 2027");

        Semester ws = semesterService.parse("WS 2026");
        assertThat(ws.getJahr()).isEqualTo(2026);
        assertThat(ws.getArt()).isEqualTo(SemesterArt.WS);
        assertThat(ws.label()).isEqualTo("WS 2026");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "SoSe 27",
            "WS27",
            "WS 27",
            "SS2027",
            "WS 2026/27",
            "WiSe 2026",
            "XX 2027",
            "WS 202a",
            " WS 2026",
            "WS 2026 "
    })
    void parseUngueltigeEingabenWerfenInvalidSemesterFormatException(String eingabe) {
        assertThatThrownBy(() -> semesterService.parse(eingabe))
                .isInstanceOf(InvalidSemesterFormatException.class);
    }

    @Test
    void apiExceptionHandlerWandeltInvalidSemesterFormatIn400MitFieldSemesterUm() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        InvalidSemesterFormatException exception =
                new InvalidSemesterFormatException("semester muss das Format 'WS JJJJ' oder 'SS JJJJ' haben");

        ResponseEntity<Map<String, String>> response = handler.invalidSemesterFormat(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody())
                .containsEntry("field", "semester")
                .containsKey("message");
    }

    // ------------------------------------------------------------------
    // istFrueher / istGleich und Semester.label() / toString()
    // ------------------------------------------------------------------

    @Test
    void vergleichIgnoriertSemesterIdUndNutztJahrUndArt() {
        Semester ws2026 = new Semester(2026, SemesterArt.WS);
        ws2026.setSemesterId(99);

        Semester ss2027 = new Semester(2027, SemesterArt.SS);
        ss2027.setSemesterId(1);

        Semester ws2027 = new Semester(2027, SemesterArt.WS);
        ws2027.setSemesterId(2);

        Semester ws2026Kopie = new Semester(2026, SemesterArt.WS);
        ws2026Kopie.setSemesterId(5);

        assertThat(semesterService.istFrueher(ws2026, ss2027)).isTrue();
        assertThat(semesterService.istFrueher(ss2027, ws2027)).isTrue();
        assertThat(semesterService.istFrueher(ws2027, ss2027)).isFalse();
        assertThat(semesterService.istFrueher(ws2026, ws2026Kopie)).isFalse();

        assertThat(semesterService.istGleich(ws2026, ws2026Kopie)).isTrue();
        assertThat(semesterService.istGleich(ss2027, ws2027)).isFalse();
    }

    @Test
    void labelUndToStringSindGetrennt() {
        Semester semester = new Semester(2026, SemesterArt.WS);
        semester.setSemesterId(42);

        assertThat(semester.label()).isEqualTo("WS 2026");
        assertThat(semester.toString()).contains("semesterId=42", "jahr=2026", "art=WS");
    }

    // ------------------------------------------------------------------
    // findenOderNeuAnlegen(jahr, art)
    // ------------------------------------------------------------------

    @Test
    void findenOderNeuAnlegenGibtVorhandenesSemesterZurueck() {
        Semester vorhanden = new Semester(2026, SemesterArt.WS);
        vorhanden.setSemesterId(10);
        when(semesterRepository.findByJahrAndArt(2026, SemesterArt.WS))
                .thenReturn(Optional.of(vorhanden));

        Semester ergebnis = semesterService.findenOderNeuAnlegen(2026, SemesterArt.WS);

        assertThat(ergebnis).isSameAs(vorhanden);
        verify(semesterRepository, never()).save(any());
    }

    @Test
    void findenOderNeuAnlegenLegtFehlendesSemesterAn() {
        when(semesterRepository.findByJahrAndArt(2029, SemesterArt.SS))
                .thenReturn(Optional.empty());
        when(semesterRepository.save(any(Semester.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Semester ergebnis = semesterService.findenOderNeuAnlegen(2029, SemesterArt.SS);

        assertThat(ergebnis.getJahr()).isEqualTo(2029);
        assertThat(ergebnis.getArt()).isEqualTo(SemesterArt.SS);
        verify(semesterRepository).save(any(Semester.class));
    }

    @Test
    void registrationServiceNutztSemesterService() {
        NutzendeRepository nutzendeRepo = mock(NutzendeRepository.class);
        StudierendeRepository studierendeRepo = mock(StudierendeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(nutzendeRepo.findByEmail("neu@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("geheimesPasswort")).thenReturn("hash");
        when(nutzendeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(semesterRepository.findByJahrAndArt(2026, SemesterArt.WS))
                .thenReturn(Optional.of(new Semester(2026, SemesterArt.WS)));

        RegistrationService registrationService = new RegistrationService(
                nutzendeRepo, studierendeRepo, semesterService, passwordEncoder);

        RegisterResponse response = registrationService.register(
                new RegisterRequest(
                        "neu@example.com", "geheimesPasswort", "WS 2026"));

        assertThat(response.startsemester()).isEqualTo("WS 2026");
        verify(semesterRepository).findByJahrAndArt(2026, SemesterArt.WS);
    }
}
