package de.htw.studyplanner.backend.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Liest bei jeder Anfrage das Token aus dem Header {@code Authorization: Bearer ...}.
 *
 * <p>Ohne Header läuft die Anfrage unverändert weiter. Ob der Pfad ein Token braucht,
 * entscheidet die {@link de.htw.studyplanner.backend.config.SecurityConfig}.
 * Ist das Token ungültig, abgelaufen oder ohne Rolle, antwortet der Filter sofort mit 401.
 * Bei gültigem Token legt er die Person mit ihrer Rolle im SecurityContext ab
 * ({@code ROLE_} plus die Rolle aus dem Token).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

private final JwtService jwtService;
private final ApiErrorWriter apiErrorWriter;

public JwtAuthenticationFilter(JwtService jwtService, ApiErrorWriter apiErrorWriter) {
    this.jwtService = jwtService;
    this.apiErrorWriter = apiErrorWriter;
    
}

    /**
     * Prüft das Token der Anfrage und meldet die Person im SecurityContext an.
     *
     * @param request     die eingehende Anfrage
     * @param response    die Antwort; bei ungültigem Token wird hier 401 geschrieben
     * @param filterChain der Rest der Filterkette
     * @throws ServletException bei Fehlern in der Filterkette
     * @throws IOException      bei Fehlern beim Schreiben der Antwort
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring("Bearer ".length());

        if (!jwtService.isValid(token)) {
    apiErrorWriter.writeError(
            request,
            response, 
            HttpStatus.UNAUTHORIZED,
            "Dein Token ist ungültig oder abgelaufen. Bitte melde dich erneut an."
    );
    return;
}

        Claims claims = jwtService.parseClaims(token);

        String email = claims.get("email", String.class);
        String rolle = claims.get("rolle", String.class);

        // Ein Token ohne Rolle wird abgelehnt, sonst entstünde die Rolle "ROLE_null".
        if (rolle == null) {
            apiErrorWriter.writeError(
                    request,
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Dein Token ist ungültig oder abgelaufen. Bitte melde dich erneut an.");
            return;
        }

        var authority = new SimpleGrantedAuthority("ROLE_" + rolle);

                Integer id = Integer.valueOf(claims.getSubject());

        var authentication = new UsernamePasswordAuthenticationToken(
                new AngemeldetePerson(id, email),
                null,
                List.of(authority));

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
    /**
     * Legt fest, für welche Pfade der Filter nicht läuft.
     *
     * <p>Login, Registrierung, Health, Swagger und die API-Dokumentation brauchen kein Token.
     * Alle anderen Pfade werden gefiltert.
     *
     * @param request die eingehende Anfrage
     * @return true, wenn der Pfad ohne Token erreichbar ist
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String pfad = request.getServletPath();
        // Nur Login und Registrierung brauchen kein Token. /api/me muss gefiltert werden.
        return pfad.equals("/api/auth/login")
                || pfad.equals("/api/auth/register")
                || pfad.startsWith("/api/health")
                || pfad.startsWith("/swagger-ui")
                || pfad.startsWith("/v3/api-docs");
    }
}