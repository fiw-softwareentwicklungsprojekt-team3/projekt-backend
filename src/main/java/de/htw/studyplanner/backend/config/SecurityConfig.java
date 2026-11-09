package de.htw.studyplanner.backend.config;

import de.htw.studyplanner.backend.security.JwtAuthenticationFilter;
import de.htw.studyplanner.backend.security.RestAccessDeniedHandler;
import de.htw.studyplanner.backend.security.RestAuthenticationEntryPoint;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Legt fest, wer welche Pfade aufrufen darf.
 *
 * <p>Die Regeln werden von oben nach unten geprüft, die erste passende gilt.
 * Die App arbeitet ohne Sitzung (stateless): Jede Anfrage bringt ihr Token mit.
 * Fehlende Anmeldung (401) und fehlende Berechtigung (403) werden im einheitlichen
 * Fehlerformat beantwortet.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Baut die Regeln für alle Anfragen.
     *
     * <p>Offen für alle: Registrierung und Login (POST unter {@code /api/auth}), Health
     * und Swagger. Nur Studierende: {@code /api/plan} und {@code /api/results}.
     * Nur Professor_innen: {@code /api/statistics}. Alle Angemeldeten: {@code /api/modules}
     * und {@code /api/me}. Alles andere braucht mindestens eine Anmeldung.
     *
     * @param http                     Konfiguration von Spring Security
     * @param authenticationEntryPoint antwortet bei fehlender Anmeldung mit 401
     * @param accessDeniedHandler      antwortet bei fehlender Berechtigung mit 403
     * @return die Filterkette
     * @throws Exception wenn die Konfiguration nicht aufgebaut werden kann
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                // Einheitliches Fehlerformat für 401 und 403.
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        )
                        .permitAll()
                        // hasRole("X") prüft die Authority "ROLE_X" aus dem JwtAuthenticationFilter.
                        .requestMatchers("/api/plan/**", "/api/results/**")
                        .hasRole("STUDIERENDE")
                        .requestMatchers("/api/statistics/**")
                        .hasRole("PROFESSOR_IN")
                        // Alle angemeldeten Rollen.
                        .requestMatchers("/api/modules/**", "/api/auth/me")
                        .authenticated()
                        .anyRequest()
                        .authenticated())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}